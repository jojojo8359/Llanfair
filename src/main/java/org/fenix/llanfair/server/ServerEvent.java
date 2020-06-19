package org.fenix.llanfair.server;

public class ServerEvent {
    private ServerAction action;
    private long nanoTime;

    public ServerEvent() {
        action = ServerAction.DO_NOTHING;
        nanoTime = 0L;
    }

    public ServerEvent(int i, long time) {
        nanoTime = time;
        switch(i) {
            case 1:
                action = ServerAction.START;
                break;
            case 3:
                action = ServerAction.SPLIT;
                break;
            case 4:
                action = ServerAction.END;
                nanoTime = 0L;
                break;
            case 5:
                action = ServerAction.RESET;
                nanoTime = 0L;
                break;
            case 6:
                action = ServerAction.PAUSE;
                break;
            case 7:
                action = ServerAction.RESUME;
                break;
            default:
                action = ServerAction.DO_NOTHING;
                nanoTime = 0L;
                break;
        }
    }

    public long getNanoTime() {
        return this.nanoTime;
    }

    public ServerAction getAction() {
        return this.action;
    }

    public String toString() {
        return this.getAction() + " : " + this.getNanoTime();
    }
}
