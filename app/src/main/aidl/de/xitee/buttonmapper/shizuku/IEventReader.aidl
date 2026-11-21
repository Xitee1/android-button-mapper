package de.xitee.buttonmapper.shizuku;

import de.xitee.buttonmapper.shizuku.IKeyEventListener;

/** Runs inside the Shizuku (shell uid) process and reads /dev/input/event* directly. */
interface IEventReader {
    /** Required by Shizuku: called when the user service is unbound/destroyed. */
    void destroy() = 16777114;

    /**
     * (Re)starts reading all input devices and reports key events to [listener].
     * Devices whose name is contained in [grabDeviceNames] are grabbed exclusively (EVIOCGRAB),
     * so Android no longer sees their keys.
     */
    void start(IKeyEventListener listener, in String[] grabDeviceNames) = 1;

    /** Stops reading and releases all grabs. */
    void stop() = 2;
}
