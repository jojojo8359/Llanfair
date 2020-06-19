package org.fenix.llanfair.server;

public enum ServerAction {
    DO_NOTHING,
    START, // needs millis
    RESTART, // not needed? - end + reset + start
    SPLIT, // needs millis
    END,
    RESET,
    PAUSE, // needs millis
    RESUME; // needs millis
}
