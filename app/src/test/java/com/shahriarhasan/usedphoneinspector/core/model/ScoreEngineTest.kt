package com.shahriarhasan.usedphoneinspector.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreEngineTest {
    @Test
    fun passWarningFail_areWeightedTransparently() {
        val result = ScoreEngine.calculate(
            InspectionProfile.USED_PHONE,
            listOf(
                ScorableResult(TestCategory.DISPLAY, TestStatus.PASS),
                ScorableResult(TestCategory.AUDIO, TestStatus.WARNING),
                ScorableResult(TestCategory.CAMERA, TestStatus.FAIL),
            ),
        )
        assertEquals(56, result.score)
        assertEquals(100, result.coveragePercent)
        assertEquals(ConditionGrade.POOR, result.grade)
    }

    @Test
    fun unsupportedSkippedDeniedAndNotStarted_doNotAffectPossibleScore() {
        val result = ScoreEngine.calculate(
            InspectionProfile.USED_PHONE,
            listOf(
                ScorableResult(TestCategory.DISPLAY, TestStatus.PASS),
                ScorableResult(TestCategory.DISPLAY, TestStatus.UNSUPPORTED),
                ScorableResult(TestCategory.AUDIO, TestStatus.SKIPPED),
                ScorableResult(TestCategory.CAMERA, TestStatus.PERMISSION_DENIED),
                ScorableResult(TestCategory.BATTERY, TestStatus.NOT_STARTED),
            ),
        )
        assertEquals(100, result.score)
        assertEquals(80, result.coveragePercent)
        assertEquals(ConditionGrade.EXCELLENT, result.grade)
    }

    @Test
    fun lowCoverage_isIncompleteEvenWithPerfectScore() {
        val result = ScoreEngine.calculate(
            InspectionProfile.USED_PHONE,
            listOf(
                ScorableResult(TestCategory.DISPLAY, TestStatus.PASS),
                ScorableResult(TestCategory.AUDIO, TestStatus.NOT_STARTED),
                ScorableResult(TestCategory.CAMERA, TestStatus.NOT_STARTED),
            ),
        )
        assertEquals(100, result.score)
        assertEquals(33, result.coveragePercent)
        assertEquals(ConditionGrade.INCOMPLETE, result.grade)
    }

    @Test
    fun gradeBoundaries_matchSpecification() {
        assertEquals(ConditionGrade.EXCELLENT, ScoreEngine.gradeFor(90))
        assertEquals(ConditionGrade.GOOD, ScoreEngine.gradeFor(75))
        assertEquals(ConditionGrade.FAIR, ScoreEngine.gradeFor(60))
        assertEquals(ConditionGrade.POOR, ScoreEngine.gradeFor(59))
    }

    @Test
    fun tabletProfile_hasDifferentDisplayWeight() {
        assertEquals(25, InspectionProfiles.weights(InspectionProfile.USED_TABLET)[TestCategory.DISPLAY])
        assertEquals(20, InspectionProfiles.weights(InspectionProfile.USED_PHONE)[TestCategory.DISPLAY])
    }
}
