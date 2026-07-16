package com.shahriarhasan.usedphoneinspector.core.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementPolicyTest {
    @Test fun freeUser_canCompleteFirstThreeOnly() {
        assertTrue(EntitlementPolicy.canComplete(0, false))
        assertTrue(EntitlementPolicy.canComplete(2, false))
        assertFalse(EntitlementPolicy.canComplete(3, false))
    }

    @Test fun proUser_hasNoCompletionLimit() {
        assertTrue(EntitlementPolicy.canComplete(10_000, true))
    }
}

