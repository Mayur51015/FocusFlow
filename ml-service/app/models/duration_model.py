import os
import json
import logging
import joblib
import numpy as np
import pandas as pd
from typing import Optional, Tuple, Dict, Any
from app.schemas.prediction_schema import PredictionRequest, DifficultyLevel
from app.utils.preprocessing import (
    to_input_dataframe,
    clamp_and_round_prediction,
    DIFFICULTY_FACTORS,
)

logger = logging.getLogger("focusflow.ml.model")

DEFAULT_ARTIFACT_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "..", "artifacts")
)
DEFAULT_MODEL_PATH = os.path.join(DEFAULT_ARTIFACT_DIR, "duration_model.joblib")
DEFAULT_METADATA_PATH = os.path.join(DEFAULT_ARTIFACT_DIR, "model_metadata.json")


class DurationPredictorModel:
    """
    Wrapper for the Scikit-Learn Study Duration Prediction Model.
    Supports graceful fallback when model artifacts are not present.
    """

    def __init__(
        self,
        model_path: str = DEFAULT_MODEL_PATH,
        metadata_path: str = DEFAULT_METADATA_PATH
    ):
        self.model_path = model_path
        self.metadata_path = metadata_path
        self.pipeline = None
        self.metadata: Dict[str, Any] = {}
        self.version = "fallback"
        self._load_model()

    def _load_model(self) -> bool:
        """
        Attempts to load serialized model pipeline and metadata from disk.
        Returns True if successful, False otherwise.
        """
        try:
            if not os.path.exists(self.model_path):
                logger.warning(
                    f"Model artifact not found at '{self.model_path}'. "
                    "Service will operate in HEURISTIC_FALLBACK mode."
                )
                return False

            self.pipeline = joblib.load(self.model_path)
            logger.info(f"Loaded trained ML pipeline from '{self.model_path}'")

            if os.path.exists(self.metadata_path):
                with open(self.metadata_path, "r", encoding="utf-8") as f:
                    self.metadata = json.load(f)
                self.version = self.metadata.get("version", "1.0")
                logger.info(f"Loaded model metadata: version={self.version}")
            else:
                self.version = "1.0"

            return True
        except Exception as e:
            logger.error(
                f"Failed to load ML model artifact from '{self.model_path}': {e}. "
                "Falling back to rule-based heuristic prediction."
            )
            self.pipeline = None
            self.version = "fallback"
            return False

    @property
    def is_loaded(self) -> bool:
        return self.pipeline is not None

    def reload(self) -> bool:
        return self._load_model()

    def predict(self, request: PredictionRequest) -> Tuple[int, float, int, str, str, Optional[str]]:
        """
        Generates study duration prediction.
        Returns:
            (predicted_minutes, confidence_score, error_range_minutes, model_version, prediction_source, details)
        """
        if self.is_loaded:
            try:
                df = to_input_dataframe(request)
                raw_pred = float(self.pipeline.predict(df)[0])

                # Extract tree-level variance for uncertainty estimation if Random Forest is present
                regressor = None
                if hasattr(self.pipeline, "named_steps") and "regressor" in self.pipeline.named_steps:
                    regressor = self.pipeline.named_steps["regressor"]
                elif hasattr(self.pipeline, "estimators_"):
                    regressor = self.pipeline

                if regressor and hasattr(regressor, "estimators_"):
                    # Calculate tree predictions after transforming features
                    preprocessor = self.pipeline.named_steps.get("preprocessor")
                    if preprocessor:
                        transformed = preprocessor.transform(df)
                    else:
                        transformed = df
                    tree_preds = np.array([tree.predict(transformed)[0] for tree in regressor.estimators_])
                    std_dev = float(np.std(tree_preds))
                    error_range = max(5, int(round(std_dev)))
                    # Confidence inversely proportional to relative variance
                    rel_std = std_dev / max(1.0, raw_pred)
                    confidence = float(np.clip(1.0 - (rel_std * 0.8), 0.50, 0.98))
                else:
                    error_range = max(8, int(round(raw_pred * 0.12)))
                    confidence = 0.85

                predicted_mins = clamp_and_round_prediction(
                    raw_pred,
                    request.estimatedMinutes
                )

                return (
                    predicted_mins,
                    round(confidence, 2),
                    error_range,
                    self.version,
                    "EXPERIMENTAL_ML",
                    None
                )
            except Exception as e:
                logger.warning(f"Inference exception encountered: {e}. Executing heuristic fallback.")
                return self.fallback_predict(request, reason=f"Inference error: {str(e)}")

        return self.fallback_predict(request, reason="Model artifact unavailable, using heuristic fallback")

    def fallback_predict(
        self,
        request: PredictionRequest,
        reason: Optional[str] = None
    ) -> Tuple[int, float, int, str, str, Optional[str]]:
        """
        Heuristic fallback model when trained artifacts are offline.
        Uses calibrated difficulty multipliers and historical weighting.
        """
        diff_factor = DIFFICULTY_FACTORS.get(request.topicDifficulty, 1.0)
        base_estimate = request.estimatedMinutes * diff_factor

        # Historical adjustment if student has logged previous sessions
        if request.historicalAverageMinutes is not None and request.historicalAverageMinutes > 0:
            # 50% planned difficulty estimate + 50% student historical average
            blended = (base_estimate * 0.5) + (request.historicalAverageMinutes * 0.5)
        else:
            blended = base_estimate

        # Slight 3% learning curve speedup if student has studied this topic multiple times
        if request.previousStudySessions >= 3:
            blended *= 0.95

        predicted_mins = clamp_and_round_prediction(blended, request.estimatedMinutes)
        error_range = max(10, int(round(request.estimatedMinutes * 0.20)))

        return (
            predicted_mins,
            0.70,
            error_range,
            "fallback",
            "HEURISTIC_FALLBACK",
            reason or "Model artifact not loaded, using heuristic fallback"
        )
