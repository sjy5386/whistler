package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.PinballWorld;

import java.awt.event.KeyEvent;

/**
 * Key mapping for flippers, plunger, and new game. Left/right flippers are
 * independent (including distinct left/right Shift and Ctrl).
 */
public final class PinballControls {
    private PinballControls() {
    }

    public static boolean isLeftFlipper(final int keyCode, final int keyLocation) {
        if (keyCode == KeyEvent.VK_Z || keyCode == KeyEvent.VK_LEFT) {
            return true;
        }
        return (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL)
                && keyLocation == KeyEvent.KEY_LOCATION_LEFT;
    }

    public static boolean isRightFlipper(final int keyCode, final int keyLocation) {
        if (keyCode == KeyEvent.VK_SLASH || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_PERIOD) {
            return true;
        }
        return (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL)
                && keyLocation == KeyEvent.KEY_LOCATION_RIGHT;
    }

    public static boolean isPlunger(final int keyCode) {
        return keyCode == KeyEvent.VK_SPACE
                || keyCode == KeyEvent.VK_DOWN
                || keyCode == KeyEvent.VK_ENTER;
    }

    public static boolean isNewGame(final int keyCode) {
        return keyCode == KeyEvent.VK_F2;
    }

    public static void keyPressed(final PinballWorld world, final int keyCode, final int keyLocation) {
        if (isNewGame(keyCode)) {
            world.reset();
            return;
        }
        if (isLeftFlipper(keyCode, keyLocation)) {
            world.setLeftFlipperRaised(true);
        }
        if (isRightFlipper(keyCode, keyLocation)) {
            world.setRightFlipperRaised(true);
        }
    }

    public static void keyReleased(final PinballWorld world, final int keyCode, final int keyLocation) {
        if (isLeftFlipper(keyCode, keyLocation)) {
            world.setLeftFlipperRaised(false);
        }
        if (isRightFlipper(keyCode, keyLocation)) {
            world.setRightFlipperRaised(false);
        }
    }
}
