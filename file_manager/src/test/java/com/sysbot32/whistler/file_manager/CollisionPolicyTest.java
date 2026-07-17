package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollisionPolicyTest {

    @Test
    void noCollisionAlwaysProceeds() {
        final CollisionPolicy.Decision d = CollisionPolicy.resolve(
                false,
                CollisionPolicy.Remember.NONE,
                null
        );
        assertEquals(CollisionPolicy.Action.PROCEED, d.action());
        assertEquals(CollisionPolicy.Remember.NONE, d.remember());
    }

    @Test
    void overwriteAndSkipAndCancel() {
        assertEquals(
                CollisionPolicy.Action.PROCEED,
                CollisionPolicy.resolve(true, CollisionPolicy.Remember.NONE, CollisionPolicy.UserChoice.OVERWRITE)
                        .action()
        );
        assertEquals(
                CollisionPolicy.Action.SKIP,
                CollisionPolicy.resolve(true, CollisionPolicy.Remember.NONE, CollisionPolicy.UserChoice.SKIP)
                        .action()
        );
        assertEquals(
                CollisionPolicy.Action.ABORT,
                CollisionPolicy.resolve(true, CollisionPolicy.Remember.NONE, CollisionPolicy.UserChoice.CANCEL)
                        .action()
        );
    }

    @Test
    void rememberAllChoices() {
        final CollisionPolicy.Decision allOver = CollisionPolicy.resolve(
                true, CollisionPolicy.Remember.NONE, CollisionPolicy.UserChoice.OVERWRITE_ALL);
        assertEquals(CollisionPolicy.Action.PROCEED, allOver.action());
        assertEquals(CollisionPolicy.Remember.OVERWRITE_ALL, allOver.remember());

        final CollisionPolicy.Decision rememberedOver = CollisionPolicy.resolve(
                true, CollisionPolicy.Remember.OVERWRITE_ALL, null);
        assertEquals(CollisionPolicy.Action.PROCEED, rememberedOver.action());

        final CollisionPolicy.Decision allSkip = CollisionPolicy.resolve(
                true, CollisionPolicy.Remember.NONE, CollisionPolicy.UserChoice.SKIP_ALL);
        assertEquals(CollisionPolicy.Action.SKIP, allSkip.action());
        assertEquals(CollisionPolicy.Remember.SKIP_ALL, allSkip.remember());

        final CollisionPolicy.Decision rememberedSkip = CollisionPolicy.resolve(
                true, CollisionPolicy.Remember.SKIP_ALL, null);
        assertEquals(CollisionPolicy.Action.SKIP, rememberedSkip.action());
    }

    @Test
    void sessionTracksRememberedState() {
        final CollisionPolicy.Session session = new CollisionPolicy.Session();
        assertEquals(
                CollisionPolicy.Action.PROCEED,
                session.decide(true, CollisionPolicy.UserChoice.OVERWRITE_ALL).action()
        );
        assertEquals(
                CollisionPolicy.Action.PROCEED,
                session.decide(true, null).action()
        );
    }

    @Test
    void missingChoiceWhenNeededThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                CollisionPolicy.resolve(true, CollisionPolicy.Remember.NONE, null));
    }
}
