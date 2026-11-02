"""
Performance Service orchestrating Weak Topic classification for single topics and batches.
"""

import logging
from typing import List
from app.models.weak_topic_model import WeakTopicClassifierModel
from app.schemas.performance_schema import (
    WeakTopicRequest,
    WeakTopicResponse,
    WeakTopicBatchRequest,
    WeakTopicBatchResponse,
    PerformanceLevel
)

logger = logging.getLogger("focusflow.ml.performance_service")


class PerformanceService:
    def __init__(self, model: WeakTopicClassifierModel = None):
        self.model = model or WeakTopicClassifierModel()

    def predict_weak_topic(self, req: WeakTopicRequest) -> WeakTopicResponse:
        level, confidence, source, probs, explanation = self.model.predict(req)
        return WeakTopicResponse(
            topicId=req.topicId,
            performanceLevel=level,
            confidence=confidence,
            predictionSource=source,
            probabilities=probs,
            explanation=explanation
        )

    def predict_batch(self, batch_req: WeakTopicBatchRequest) -> WeakTopicBatchResponse:
        predictions: List[WeakTopicResponse] = []
        weak_count = 0
        mod_count = 0
        strong_count = 0

        for topic_req in batch_req.topics:
            res = self.predict_weak_topic(topic_req)
            predictions.append(res)
            if res.performanceLevel == PerformanceLevel.WEAK:
                weak_count += 1
            elif res.performanceLevel == PerformanceLevel.MODERATE:
                mod_count += 1
            elif res.performanceLevel == PerformanceLevel.STRONG:
                strong_count += 1

        return WeakTopicBatchResponse(
            predictions=predictions,
            totalTopics=len(predictions),
            weakCount=weak_count,
            moderateCount=mod_count,
            strongCount=strong_count
        )


performance_service = PerformanceService()

