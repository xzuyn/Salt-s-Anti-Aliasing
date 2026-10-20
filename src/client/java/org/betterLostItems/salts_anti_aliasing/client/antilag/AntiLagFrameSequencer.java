package org.betterLostItems.salts_anti_aliasing.client.antilag;

/**
 * Pairs the two {@code vkAntiLagUpdateAMD} calls that belong to one frame.
 *
 * <p>The Vulkan spec asks for the INPUT stage immediately before the frame's input is processed, and for
 * the PRESENT stage, with the <em>same</em> frame index, right before {@code vkQueuePresentKHR}. Frame
 * indices only ever go up. This class hands out those indices and refuses to open a second frame
 * before the first one was presented, so a repeated INPUT announcement can never pair a frame with the
 * wrong index.</p>
 *
 * <p>This type is deliberately free of Minecraft and LWJGL classes so it can be unit tested on its own.</p>
 */
public final class AntiLagFrameSequencer {
    /** Returned when there is nothing to announce. Real frame indices start at 1. */
    public static final long NO_FRAME = -1L;

    private long nextFrameIndex = 1L;
    private long openFrameIndex = NO_FRAME;

    /**
     * Opens a frame at the INPUT stage.
     * @return the frame index to announce, or {@link #NO_FRAME} if the previous frame is still open
     */
    public synchronized long beginFrame() {
        if (openFrameIndex != NO_FRAME) {
            return NO_FRAME;
        }

        openFrameIndex = nextFrameIndex++;
        return openFrameIndex;
    }

    /**
     * Closes the open frame at the PRESENT stage.
     * @return the frame index to announce, or {@link #NO_FRAME} if no frame was opened (for example
     *         because Anti-Lag was switched on in the middle of a frame)
     */
    public synchronized long endFrame() {
        long closed = openFrameIndex;
        openFrameIndex = NO_FRAME;
        return closed;
    }

    /** Abandons the open frame without presenting it. Indices keep counting up. */
    public synchronized void reset() {
        openFrameIndex = NO_FRAME;
    }

    /** Whether a frame has been opened and not yet presented. */
    public synchronized boolean frameOpen() {
        return openFrameIndex != NO_FRAME;
    }
}
