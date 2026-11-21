package de.xitee.buttonmapper.shizuku

import android.os.RemoteException
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.system.exitProcess

/**
 * Shizuku user service. This class is instantiated by Shizuku inside a separate process that runs
 * with the shell uid (member of the `input` group), so it can open /dev/input/event* directly.
 *
 * We read the evdev devices ourselves instead of piping `getevent`, because getevent's stdout is
 * block-buffered when it is not attached to a terminal.
 */
class EventReaderService : IEventReader.Stub() {

    private var reader: EvdevReader? = null

    @Synchronized
    override fun start(listener: IKeyEventListener, grabDeviceNames: Array<String>) {
        reader?.shutdown()
        reader = EvdevReader(listener, grabDeviceNames.toSet()).also { it.start() }
    }

    @Synchronized
    override fun stop() {
        reader?.shutdown()
        reader = null
    }

    override fun destroy() {
        stop()
        exitProcess(0)
    }
}

/** Static information about one input device, gathered via `getevent -lp`. */
private data class DeviceInfo(val path: String, val name: String, val keys: List<String>)

private class EvdevReader(
    private val listener: IKeyEventListener,
    private val grabNames: Set<String>,
) : Thread("evdev-reader") {

    private val wakePipe: Array<FileDescriptor> = Os.pipe()

    @Volatile
    private var running = true

    fun shutdown() {
        running = false
        runCatching { Os.write(wakePipe[1], byteArrayOf(1), 0, 1) }
    }

    override fun run() {
        val devices = queryDevices()
        val open = LinkedHashMap<FileDescriptor, DeviceInfo>()
        val grabbed = HashSet<String>()
        for (d in devices) {
            try {
                val fd = Os.open(d.path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK or OsConstants.O_CLOEXEC, 0)
                open[fd] = d
                if (d.name in grabNames) {
                    when {
                        // Grabbing the power key device would make the phone unusable.
                        "KEY_POWER" in d.keys -> report { onError(ReaderError.Kind.GRAB_POWER_KEY.name, d.name) }
                        grab(fd) -> grabbed += d.path
                        else -> report { onError(ReaderError.Kind.GRAB_FAILED.name, d.name) }
                    }
                }
            } catch (e: ErrnoException) {
                Log.w(TAG, "Cannot open ${d.path}", e)
            }
        }
        report {
            onDevices(devices.map { d ->
                listOf(d.path, d.name, if (d.path in grabbed) "1" else "0", d.keys.joinToString(",")).joinToString("\t")
            }.toTypedArray())
        }
        if (open.isEmpty()) {
            report { onError(ReaderError.Kind.NO_DEVICES.name, "") }
            return
        }

        val recordSize = if (android.os.Process.is64Bit()) 24 else 16
        val timeFieldSize = recordSize - 8
        val buf = ByteArray(recordSize * 64)
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder())

        try {
            while (running) {
                val fds = open.keys.toList()
                val pollFds = Array(fds.size + 1) { i ->
                    StructPollfd().apply {
                        fd = if (i == 0) wakePipe[0] else fds[i - 1]
                        events = OsConstants.POLLIN.toShort()
                    }
                }
                try {
                    Os.poll(pollFds, -1)
                } catch (e: ErrnoException) {
                    if (e.errno == OsConstants.EINTR) continue else throw e
                }
                if (!running || pollFds[0].revents.toInt() != 0) break

                for (i in 1 until pollFds.size) {
                    val revents = pollFds[i].revents.toInt()
                    if (revents == 0) continue
                    val fd = fds[i - 1]
                    val dev = open.getValue(fd)
                    if (revents and (OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL) != 0) {
                        closeQuietly(fd); open.remove(fd); continue
                    }
                    val n = try {
                        Os.read(fd, buf, 0, buf.size)
                    } catch (e: ErrnoException) {
                        if (e.errno == OsConstants.EAGAIN) 0 else { closeQuietly(fd); open.remove(fd); 0 }
                    }
                    var off = 0
                    while (off + recordSize <= n) {
                        val sec = if (recordSize == 24) bb.getLong(off) else bb.getInt(off).toLong()
                        val usec = if (recordSize == 24) bb.getLong(off + 8) else bb.getInt(off + 4).toLong()
                        val type = bb.getShort(off + timeFieldSize).toInt() and 0xffff
                        val code = bb.getShort(off + timeFieldSize + 2).toInt() and 0xffff
                        val value = bb.getInt(off + timeFieldSize + 4)
                        if (type == EV_KEY) {
                            val time = sec * 1000 + usec / 1000
                            if (!report { onKeyEvent(dev.path, dev.name, code, value, time) }) running = false
                        }
                        off += recordSize
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Reader failed", t)
            report { onError(ReaderError.Kind.READ_FAILED.name, t.message.orEmpty()) }
        } finally {
            // Closing the fd also releases an EVIOCGRAB grab.
            open.keys.forEach(::closeQuietly)
            closeQuietly(wakePipe[0]); closeQuietly(wakePipe[1])
        }
    }

    /** Calls the listener; returns false if the app side is gone. */
    private inline fun report(block: IKeyEventListener.() -> Unit): Boolean = try {
        listener.block(); true
    } catch (e: RemoteException) {
        Log.w(TAG, "Listener gone", e); false
    }

    private fun closeQuietly(fd: FileDescriptor) {
        runCatching { Os.close(fd) }
    }

    /** EVIOCGRAB via libcore's ioctlInt (passes a non-null pointer, which the kernel treats as "grab"). */
    private fun grab(fd: FileDescriptor): Boolean = try {
        val m = Os::class.java.methods.first { it.name == "ioctlInt" }
        when (m.parameterTypes.size) {
            2 -> m.invoke(null, fd, EVIOCGRAB)
            3 -> {
                val ref = m.parameterTypes[2].getConstructor(Int::class.javaPrimitiveType).newInstance(1)
                m.invoke(null, fd, EVIOCGRAB, ref)
            }
            else -> error("Unknown ioctlInt signature")
        }
        true
    } catch (t: Throwable) {
        Log.e(TAG, "EVIOCGRAB failed", t); false
    }

    /** Uses `getevent -lp` once (it exits, so buffering is no problem) to get device names and keys. */
    private fun queryDevices(): List<DeviceInfo> {
        val out = try {
            val p = ProcessBuilder("getevent", "-lp").redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().also { p.waitFor() }
        } catch (e: Exception) {
            Log.e(TAG, "getevent -lp failed", e); ""
        }
        val parsed = parseGeteventDevices(out)
        if (parsed.isNotEmpty()) return parsed
        // Fallback: at least read every event node, names unknown.
        return java.io.File("/dev/input").listFiles { f -> f.name.startsWith("event") }
            ?.sortedBy { it.name }
            ?.map { DeviceInfo(it.path, it.name, emptyList()) }
            .orEmpty()
    }

    companion object {
        private const val TAG = "ButtonMapperReader"
        private const val EV_KEY = 1
        private const val EVIOCGRAB = 0x40044590
    }
}

private fun parseGeteventDevices(text: String): List<DeviceInfo> {
    val result = ArrayList<DeviceInfo>()
    var path: String? = null
    var name = ""
    var keys = ArrayList<String>()
    var inKeySection = false
    fun flush() {
        path?.let { result += DeviceInfo(it, name, keys) }
    }
    val tokenRegex = Regex("""\b(?:KEY|BTN)_[A-Z0-9_]+\b""")
    for (line in text.lineSequence()) {
        val add = Regex("""^add device \d+: (\S+)""").find(line)
        if (add != null) {
            flush()
            path = add.groupValues[1]; name = ""; keys = ArrayList(); inKeySection = false
            continue
        }
        val nameMatch = Regex("""^\s+name:\s+"(.*)"""").find(line)
        if (nameMatch != null) { name = nameMatch.groupValues[1]; continue }
        val section = Regex("""^\s+([A-Z]{2,4}) \([0-9a-f]{4}\):""").find(line)
        if (section != null) inKeySection = section.groupValues[1] == "KEY"
        else if (line.trim().endsWith(":")) inKeySection = false
        if (inKeySection) keys += tokenRegex.findAll(line).map { it.value }
    }
    flush()
    return result
}
