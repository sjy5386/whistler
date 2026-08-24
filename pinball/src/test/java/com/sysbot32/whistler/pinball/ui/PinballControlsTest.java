package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.PinballWorld;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinballControlsTest {
    @Test
    void leftAndRightFlipperKeysAreIndependent() {
        assertTrue(PinballControls.isLeftFlipper(KeyEvent.VK_Z, KeyEvent.KEY_LOCATION_STANDARD));
        assertTrue(PinballControls.isLeftFlipper(KeyEvent.VK_LEFT, KeyEvent.KEY_LOCATION_STANDARD));
        assertTrue(PinballControls.isLeftFlipper(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_LEFT));
        assertFalse(PinballControls.isLeftFlipper(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_RIGHT));

        assertTrue(PinballControls.isRightFlipper(KeyEvent.VK_SLASH, KeyEvent.KEY_LOCATION_STANDARD));
        assertTrue(PinballControls.isRightFlipper(KeyEvent.VK_RIGHT, KeyEvent.KEY_LOCATION_STANDARD));
        assertTrue(PinballControls.isRightFlipper(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_RIGHT));
        assertFalse(PinballControls.isRightFlipper(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_LEFT));
    }

    @Test
    void plungerAndNewGameKeysAreBound() {
        assertTrue(PinballControls.isPlunger(KeyEvent.VK_SPACE));
        assertTrue(PinballControls.isPlunger(KeyEvent.VK_DOWN));
        assertTrue(PinballControls.isPlunger(KeyEvent.VK_ENTER));
        assertTrue(PinballControls.isNewGame(KeyEvent.VK_F2));
        assertFalse(PinballControls.isPlunger(KeyEvent.VK_Z));
    }

    @Test
    void keyPressedRaisesTheMatchingFlipperOnTheShippedWorld() {
        final PinballWorld world = new PinballWorld();
        assertFalse(world.getLeftFlipper().isRaised());
        assertFalse(world.getRightFlipper().isRaised());
        PinballControls.keyPressed(world, KeyEvent.VK_Z, KeyEvent.KEY_LOCATION_STANDARD);
        assertTrue(world.getLeftFlipper().isRaised());
        assertFalse(world.getRightFlipper().isRaised());
        PinballControls.keyPressed(world, KeyEvent.VK_SLASH, KeyEvent.KEY_LOCATION_STANDARD);
        assertTrue(world.getLeftFlipper().isRaised());
        assertTrue(world.getRightFlipper().isRaised());
        PinballControls.keyReleased(world, KeyEvent.VK_Z, KeyEvent.KEY_LOCATION_STANDARD);
        assertFalse(world.getLeftFlipper().isRaised());
        assertTrue(world.getRightFlipper().isRaised());
    }
}
