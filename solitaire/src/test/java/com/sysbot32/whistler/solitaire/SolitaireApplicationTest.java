package com.sysbot32.whistler.solitaire;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SolitaireApplicationTest {
    @Test
    void mainEntryPointExists() throws Exception {
        final Method main = SolitaireApplication.class.getMethod("main", String[].class);
        assertTrue(Modifier.isPublic(main.getModifiers()));
        assertTrue(Modifier.isStatic(main.getModifiers()));
    }
}
