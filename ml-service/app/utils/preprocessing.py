import numpy as np
import pandas as pd
from typing import Dict, Any
from app.schemas.prediction_schema import PredictionRequest, DifficultyLevel


DIFFICULTY_FACTORS = {
    DifficultyLevel.EASY: 0.90,
    DifficultyLevel.MEDIUM: 1.05,
    DifficultyLevel.HARD: 1.30,
}


def prepare_feature_dict(request: PredictionRequest) -> Dict[str, Any]:
    """
    Transforms a validated PredictionRequest into a standardized feature dictionary.
    Handles contextual imputation if historical values are absent.
    """
    difficulty = request.topicDifficulty.value

    # Impute historical average if not available
    if request.historicalAverageMinutes is not None and request.historicalAverageMinutes > 0:
        hist_avg = float(request.historicalAverageMinutes)
    else:
        multiplier = DIFFICULTY_FACTORS.get(request.topicDifficulty, 1.0)
        hist_avg = float(round(request.estimatedMinutes * multiplier))

    category = request.subjectCategory.strip().upper() if request.subjectCategory else "GENERAL"

    return {
        "difficulty": difficulty,
        "estimated_minutes": float(request.estimatedMinutes),
        "historical_average_minutes": hist_avg,
        "previous_study_sessions": int(request.previousStudySessions),
        "subject_category": category,
    }


def to_input_dataframe(request: PredictionRequest) -> pd.DataFrame:
    """
    Converts a single PredictionRequest into a single-row DataFrame
    matching the training schema.
    """
    features = prepare_feature_dict(request)
    return pd.DataFrame([features])


def clamp_and_round_prediction(
    raw_prediction: float,
    estimated_minutes: int,
    min_limit: int = 15,
    max_limit: int = 360
) -> int:
    """
    Safely bounds predictions to reasonable limits to avoid extreme outliers:
    - Minimum: max(15 mins, 40% of estimated duration)
    - Maximum: min(360 mins, 250% of estimated duration)
    - Rounds to nearest integer
    """
    lower_bound = max(min_limit, int(round(estimated_minutes * 0.4)))
    upper_bound = min(max_limit, int(round(estimated_minutes * 2.5)))

    bounded = min(max(raw_prediction, lower_bound), upper_bound)
    return int(round(bounded))

