package com.sysbot32.whistler.file_manager.ops;


import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferControlTest {

    @Test
    void progressAndCancel() {
        final TransferControl c = new TransferControl();
        c.begin(4);
        assertEquals(4, c.total());
        assertEquals(0, c.completed());
        assertFalse(c.isCancelled());

        c.advance("one");
        assertEquals(1, c.completed());
        assertEquals("one", c.detail());
        assertEquals(25, c.percent());

        c.cancel();
        assertTrue(c.isCancelled());
        assertThrows(CancellationException.class, c::throwIfCancelled);
        assertThrows(CancellationException.class, () -> c.advance("two"));
    }

    @Test
    void unknownTotalYieldsIndeterminatePercent() {
        final TransferControl c = new TransferControl();
        c.begin(0);
        assertEquals(-1, c.percent());
    }
}
