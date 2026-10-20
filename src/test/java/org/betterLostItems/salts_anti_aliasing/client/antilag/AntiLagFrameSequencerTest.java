package org.betterLostItems.salts_anti_aliasing.client.antilag;

import org.junit.jupiter.api.Test;

import static org.betterLostItems.salts_anti_aliasing.client.antilag.AntiLagFrameSequencer.NO_FRAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AntiLagFrameSequencerTest {
    @Test
    void inputAndPresentOfOneFrameShareAnIndex() {
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();

        long input = sequencer.beginFrame();
        long present = sequencer.endFrame();

        assertEquals(1L, input);
        assertEquals(input, present);
    }

    @Test
    void indicesIncreaseFromFrameToFrame() {
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();
        long previous = 0L;

        for (int frame = 0; frame < 5; frame++) {
            long input = sequencer.beginFrame();
            assertTrue(input > previous);
            assertEquals(input, sequencer.endFrame());
            previous = input;
        }
    }

    @Test
    void repeatedInputAnnouncementsBeforeThePresentDoNotOpenAnotherFrame() {
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();

        long first = sequencer.beginFrame();

        assertEquals(NO_FRAME, sequencer.beginFrame());
        assertEquals(NO_FRAME, sequencer.beginFrame());
        assertEquals(first, sequencer.endFrame());
        assertTrue(sequencer.beginFrame() > first);
    }

    @Test
    void theRealCallOrderPairsEveryPresentWithTheInputBeforeIt() {
        // What the swapchain hook does: PRESENT right before vkQueuePresentKHR, then INPUT for the next
        // frame right after it returns. The very first present has nothing open and announces nothing.
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();
        assertEquals(NO_FRAME, sequencer.endFrame());
        long input = sequencer.beginFrame();

        for (int frame = 0; frame < 4; frame++) {
            long present = sequencer.endFrame();
            assertEquals(input, present);
            long next = sequencer.beginFrame();
            assertEquals(input + 1, next);
            input = next;
        }
    }

    @Test
    void presentWithoutAnOpenFrameAnnouncesNothing() {
        // Anti-Lag switched on halfway through a frame, or a present with no preceding poll.
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();

        assertEquals(NO_FRAME, sequencer.endFrame());
        sequencer.beginFrame();
        sequencer.endFrame();
        assertEquals(NO_FRAME, sequencer.endFrame());
    }

    @Test
    void resetAbandonsTheOpenFrameButNeverReusesAnIndex() {
        AntiLagFrameSequencer sequencer = new AntiLagFrameSequencer();
        long abandoned = sequencer.beginFrame();
        assertTrue(sequencer.frameOpen());

        sequencer.reset();

        assertFalse(sequencer.frameOpen());
        assertEquals(NO_FRAME, sequencer.endFrame());
        assertTrue(sequencer.beginFrame() > abandoned);
    }
}
