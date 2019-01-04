package zuwagon.zulocationtrackelib;

/**
 * This interface must implemented by status listeners.
 */
public interface ZWStatusCallback {
    /**
     * Fires when new status received.
     * @param code {@link ZWStatus ZWStatus} code.
     */
    void onStatus(int code);
}
