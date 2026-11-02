"""
FastAPI Routes for Exam Readiness Prediction.
Exposes endpoints for estimating readiness score, readiness level, breakdown, and recommendations.
"""

from typing import Union
from fastapi import APIRouter, Body
from app.schemas.readiness_schema import (
    ExamReadinessRequest,
    ExamReadinessResponse,
    ExamReadinessBatchRequest,
    ExamReadinessBatchResponse
)
from app.services.readiness_service import readiness_service

router = APIRouter(prefix="/ml", tags=["Exam Readiness Estimation"])


@router.post(
    "/predict-exam-readiness",
    response_model=Union[ExamReadinessResponse, ExamReadinessBatchResponse],
    summary="Predict Exam Preparation Readiness",
    description="Estimates subject exam readiness percentage (0-100%), readiness tier (LOW, MODERATE, HIGH), and recommendations."
)
async def predict_exam_readiness(
    payload: Union[ExamReadinessRequest, ExamReadinessBatchRequest] = Body(...)
):
    if isinstance(payload, ExamReadinessBatchRequest):
        return readiness_service.evaluate_batch(payload)
    return readiness_service.evaluate_subject(payload)


@router.post(
    "/predict-exam-readiness/batch",
    response_model=ExamReadinessBatchResponse,
    summary="Batch Predict Exam Readiness Across Multiple Subjects",
    description="Calculates exam readiness metrics across all enrolled courses/subjects simultaneously."
)
async def predict_exam_readiness_batch(
    payload: ExamReadinessBatchRequest
):
    return readiness_service.evaluate_batch(payload)

