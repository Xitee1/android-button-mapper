package de.xitee.buttonmapper.shizuku

/** Problems reported by the reader or the client. Localized in the UI (see `ui/Labels.kt`). */
data class ReaderError(val kind: Kind, val detail: String = "") {
    enum class Kind {
        GRAB_POWER_KEY,
        GRAB_FAILED,
        NO_DEVICES,
        READ_FAILED,
        SERVICE_INVALID,
        CONNECTION_LOST,
        BIND_FAILED,
        START_FAILED,
    }

    companion object {
        /** Parses the [kind] string sent over AIDL; unknown kinds become [Kind.READ_FAILED]. */
        fun fromWire(kind: String, detail: String): ReaderError =
            ReaderError(Kind.entries.firstOrNull { it.name == kind } ?: Kind.READ_FAILED, detail)
    }
}
