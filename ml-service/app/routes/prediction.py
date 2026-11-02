import logging
from fastapi import APIRouter, HTTPException, status
from app.schemas.prediction_schema import (
    PredictionRequest,
    PredictionResponse,
    HealthResponse,
)
from app.services.prediction_service import prediction_service

logger = logging.getLogger("focusflow.ml.router")

router = APIRouter(tags=["ML Prediction"])


@router.post(
    "/ml/predict-duration",
    response_model=PredictionResponse,
    status_code=status.HTTP_200_OK,
    summary="Predict realistic study duration for a topic",
    description="Uses a Random Forest Regressor (or calibrated fallback) to predict actual study duration based on difficulty, estimates, and history."
)
async def predict_duration(request: PredictionRequest) -> PredictionResponse:
    try:
        response = prediction_service.predict_duration(request)
        return response
    except Exception as e:
        logger.error(f"Error handling prediction request: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An error occurred while computing the study duration prediction: {str(e)}"
        )


@router.get(
    "/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Microservice health and model status"
)
async def health_check() -> HealthResponse:
    return prediction_service.get_health()


@router.get(
    "/ml/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Microservice health and model status (namespaced alias)"
)
async def health_check_namespaced() -> HealthResponse:
    return prediction_service.get_health()

