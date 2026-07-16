package com.shahriarhasan.usedphoneinspector.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestResultTransitionsTest {
    @Test fun notStarted_cannotTransitionToItself() {
        assertFalse(TestResultTransitions.canTransition(TestStatus.NOT_STARTED, TestStatus.NOT_STARTED))
        assertTrue(TestResultTransitions.canTransition(TestStatus.NOT_STARTED, TestStatus.IN_PROGRESS))
    }

    @Test fun completedResult_canBeRetriedOrEdited() {
        assertTrue(TestResultTransitions.canTransition(TestStatus.FAIL, TestStatus.IN_PROGRESS))
        assertTrue(TestResultTransitions.canTransition(TestStatus.FAIL, TestStatus.PASS))
    }
}

