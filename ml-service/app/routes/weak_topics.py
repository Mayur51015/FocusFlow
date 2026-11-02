"""
Routes for Weak Topic Performance Prediction.
Exposes endpoints for single topic evaluation and batch topic assessment.
"""

from typing import Union, List
from fastapi import APIRouter, Body
from app.schemas.performance_schema import (
    WeakTopicRequest,
    WeakTopicResponse,
    WeakTopicBatchRequest,
    WeakTopicBatchResponse
)
from app.services.performance_service import performance_service

router = APIRouter(prefix="/ml", tags=["Performance & Weak Topics"])


@router.post(
    "/predict-weak-topics",
    response_model=Union[WeakTopicResponse, WeakTopicBatchResponse],
    summary="Predict Topic Mastery & Weakness",
    description="Evaluates student understanding on a topic or multiple topics to classify into STRONG, MODERATE, or WEAK."
)
async def predict_weak_topics(
    payload: Union[WeakTopicRequest, WeakTopicBatchRequest] = Body(...)
):
    if isinstance(payload, WeakTopicBatchRequest):
        return performance_service.predict_batch(payload)
    return performance_service.predict_weak_topic(payload)


@router.post(
    "/predict-weak-topics/batch",
    response_model=WeakTopicBatchResponse,
    summary="Batch Predict Topic Weakness",
    description="Evaluates multiple topics simultaneously."
)
async def predict_weak_topics_batch(
    payload: WeakTopicBatchRequest
):
    return performance_service.predict_batch(payload)

