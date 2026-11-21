package de.xitee.buttonmapper.shizuku;

oneway interface IKeyEventListener {
    /** One EV_KEY event. [value]: 0 = up, 1 = down, 2 = repeat. */
    void onKeyEvent(String devicePath, String deviceName, int code, int value, long timeMillis);

    /** Device list, one entry per device: "path\tname\tgrabbed(0|1)\tKEY_A,KEY_B,...". */
    void onDevices(in String[] devices);

    /** Reader stopped or hit a problem. */
    void onError(String message);
}
