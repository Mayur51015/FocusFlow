"""
Service layer for Exam Readiness Estimation.
Handles single and batch subject evaluations.
"""

import logging
from typing import List
from app.models.readiness_model import exam_readiness_model
from app.schemas.readiness_schema import (
    ExamReadinessRequest,
    ExamReadinessResponse,
    ExamReadinessBatchRequest,
    ExamReadinessBatchResponse
)

logger = logging.getLogger("focusflow.ml.readiness_service")


class ReadinessService:
    def __init__(self):
        self.model = exam_readiness_model

    def evaluate_subject(self, request: ExamReadinessRequest) -> ExamReadinessResponse:
        res = self.model.predict(
            topic_completion_rate=request.topicCompletionRate,
            average_quiz_score=request.averageQuizScore,
            weak_topic_ratio=request.weakTopicRatio,
            revision_completion_rate=request.revisionCompletionRate,
            days_until_exam=request.daysUntilExam,
            importance=request.importance or "MEDIUM",
            recent_quiz_trend=request.recentQuizTrend,
            subject_id=request.subjectId,
            subject_name=request.subjectName
        )
        return ExamReadinessResponse(**res)

    def evaluate_batch(self, batch_request: ExamReadinessBatchRequest) -> ExamReadinessBatchResponse:
        results: List[ExamReadinessResponse] = []
        total_score = 0.0

        for req in batch_request.subjects:
            eval_res = self.evaluate_subject(req)
            results.append(eval_res)
            total_score += eval_res.readinessScore

        count = len(results)
        avg_score = round(total_score / count, 1) if count > 0 else 0.0

        if avg_score >= 75.0:
            overall_level = "HIGH"
        elif avg_score >= 50.0:
            overall_level = "MODERATE"
        else:
            overall_level = "LOW"

        return ExamReadinessBatchResponse(
            results=results,
            overallReadinessScore=avg_score,
            overallReadinessLevel=overall_level,
            totalSubjects=count
        )


readiness_service = ReadinessService()

