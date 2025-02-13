package fi.dy.masa.litematica.util;

public enum OverlayType
{
    NONE            (0),
    MISSING         (1),
    EXTRA           (2),
    WRONG_STATE     (3),
    WRONG_BLOCK     (4),
    DIFF_BLOCK      (5);

    private final int priority;

    OverlayType(int priority)
    {
        this.priority = priority;
    }

    /**
     * Higher value means higher render priority over an overlapping lower priority
     * @return ()
     */
    public int getRenderPriority()
    {
        return this.priority;
    }
}
