package com.shahriarhasan.usedphoneinspector.core.model

import kotlin.math.roundToInt

data class ScorableResult(
    val category: TestCategory,
    val status: TestStatus,
    val required: Boolean = true,
)

object ScoreEngine {
    fun calculate(
        profile: InspectionProfile,
        results: List<ScorableResult>,
    ): ScoreResult {
        val required = results.filter { it.required }
        val terminal = required.count { it.status.isTerminal }
        val coverage = if (required.isEmpty()) 0 else (terminal * 100.0 / required.size).roundToInt()
        val weights = InspectionProfiles.weights(profile)
        val categoryScores = mutableMapOf<TestCategory, Int>()
        var weightedEarned = 0.0
        var availableWeight = 0.0

        weights.forEach { (category, categoryWeight) ->
            val scored = results.filter { it.category == category && it.status.isScored }
            if (scored.isNotEmpty()) {
                val ratio = scored.map { resultValue(it.status) }.average()
                categoryScores[category] = (ratio * 100).roundToInt()
                weightedEarned += ratio * categoryWeight
                availableWeight += categoryWeight
            }
        }

        val score = if (availableWeight == 0.0) 0 else (weightedEarned / availableWeight * 100).roundToInt()
        val grade = if (coverage < 60) {
            ConditionGrade.INCOMPLETE
        } else {
            gradeFor(score)
        }
        return ScoreResult(
            score = score.coerceIn(0, 100),
            coveragePercent = coverage.coerceIn(0, 100),
            grade = grade,
            categoryScores = categoryScores,
            completedCount = terminal,
            totalRequiredCount = required.size,
        )
    }

    fun gradeFor(score: Int): ConditionGrade = when (score) {
        in 90..100 -> ConditionGrade.EXCELLENT
        in 75..89 -> ConditionGrade.GOOD
        in 60..74 -> ConditionGrade.FAIR
        else -> ConditionGrade.POOR
    }

    private fun resultValue(status: TestStatus): Double = when (status) {
        TestStatus.PASS -> 1.0
        TestStatus.WARNING -> 0.5
        TestStatus.FAIL -> 0.0
        else -> error("Only scored states have a score")
    }
}

object TestResultTransitions {
    fun canTransition(from: TestStatus, to: TestStatus): Boolean = when (from) {
        TestStatus.NOT_STARTED -> to != TestStatus.NOT_STARTED
        TestStatus.IN_PROGRESS -> true
        else -> to == TestStatus.IN_PROGRESS || to.isTerminal
    }
}

