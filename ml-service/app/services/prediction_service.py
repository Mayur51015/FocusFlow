import time
import logging
from app.models.duration_model import DurationPredictorModel
from app.schemas.prediction_schema import (
    PredictionRequest,
    PredictionResponse,
    HealthResponse,
)

logger = logging.getLogger("focusflow.ml.service")


class PredictionService:
    """
    Application service managing ML duration prediction and model lifecycle.
    """

    def __init__(self, model: DurationPredictorModel = None):
        self.start_time = time.time()
        self.model = model or DurationPredictorModel()

    def predict_duration(self, request: PredictionRequest) -> PredictionResponse:
        """
        Executes prediction with full validation and fallback handling.
        """
        (
            predicted_minutes,
            confidence_score,
            error_range,
            version,
            source,
            details,
        ) = self.model.predict(request)

        return PredictionResponse(
            predictedMinutes=predicted_minutes,
            confidenceScore=confidence_score,
            estimatedErrorRangeMinutes=error_range,
            modelVersion=version,
            predictionSource=source,
            details=details,
        )

    def get_health(self) -> HealthResponse:
        """
        Retrieves microservice health status and model availability.
        """
        uptime = round(time.time() - self.start_time, 2)
        model_loaded = self.model.is_loaded
        source = "EXPERIMENTAL_ML" if model_loaded else "HEURISTIC_FALLBACK"

        return HealthResponse(
            status="UP",
            modelLoaded=model_loaded,
            modelVersion=self.model.version,
            predictionSource=source,
            uptimeSeconds=uptime,
        )


# Global singleton instance
prediction_service = PredictionService()

