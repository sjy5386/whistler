package com.sysbot32.whistler.file_manager.ops;

import org.junit.jupiter.api.Test;

import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCoachingTest {
    @Test
    void detectsAccessDeniedException() {
        assertTrue(AccessCoaching.isAccessDenied(new AccessDeniedException("/secret")));
    }

    @Test
    void detectsOperationNotPermittedMessage() {
        assertTrue(AccessCoaching.isAccessDenied(new java.io.IOException("Operation not permitted")));
        assertTrue(AccessCoaching.isAccessDenied(new RuntimeException(
                new java.io.IOException("EPERM: open failed"))));
    }

    @Test
    void ignoresOrdinaryErrors() {
        assertFalse(AccessCoaching.isAccessDenied(new java.io.IOException("No such file")));
        assertFalse(AccessCoaching.isAccessDenied(null));
    }
}
