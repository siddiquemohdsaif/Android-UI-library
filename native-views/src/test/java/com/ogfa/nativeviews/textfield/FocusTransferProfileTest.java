package com.ogfa.nativeviews.textfield;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FocusTransferProfileTest {
    @Test
    public void sameIdReplacementConsumesPendingFocus() {
        FocusTransferProfile profile = new FocusTransferProfile();
        profile.begin("phone_number_field", 100L);

        assertTrue(profile.consume("phone_number_field", 107L));
        assertFalse(profile.hasPending());
        assertEquals(7L, profile.getLastElapsedMs());
    }

    @Test
    public void differentIdDoesNotStealPendingFocus() {
        FocusTransferProfile profile = new FocusTransferProfile();
        profile.begin("phone_number_field", 100L);

        assertFalse(profile.consume("country_field", 104L));
        assertTrue(profile.hasPending());
        assertEquals("phone_number_field", profile.getPendingFieldId());
    }

    @Test
    public void expiryReturnsRemovedFieldAndClearsPendingState() {
        FocusTransferProfile profile = new FocusTransferProfile();
        profile.begin("phone_number_field", 100L);

        assertEquals("phone_number_field", profile.expire(112L));
        assertFalse(profile.hasPending());
        assertEquals(12L, profile.getLastElapsedMs());
        assertNull(profile.expire(120L));
    }

    @Test
    public void elapsedTimeNeverBecomesNegative() {
        FocusTransferProfile profile = new FocusTransferProfile();
        profile.begin("phone_number_field", 100L);

        assertTrue(profile.consume("phone_number_field", 90L));
        assertEquals(0L, profile.getLastElapsedMs());
    }
}
