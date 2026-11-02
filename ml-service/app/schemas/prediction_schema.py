from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator, AliasChoices


class DifficultyLevel(str, Enum):
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"


class PredictionRequest(BaseModel):
    topicDifficulty: DifficultyLevel = Field(
        ...,
        validation_alias=AliasChoices("topicDifficulty", "topic_difficulty", "difficulty"),
        description="Difficulty level of the topic (EASY, MEDIUM, HARD)"
    )
    estimatedMinutes: int = Field(
        ...,
        ge=1,
        le=720,
        validation_alias=AliasChoices("estimatedMinutes", "estimated_minutes", "estimated_duration"),
        description="Planned / baseline estimated duration in minutes (1 to 720)"
    )
    historicalAverageMinutes: Optional[float] = Field(
        default=None,
        ge=0,
        validation_alias=AliasChoices("historicalAverageMinutes", "historical_average_minutes", "historical_average"),
        description="Historical average minutes spent on this subject or similar topics"
    )
    previousStudySessions: int = Field(
        default=0,
        ge=0,
        le=100,
        validation_alias=AliasChoices("previousStudySessions", "previous_study_sessions", "previous_sessions"),
        description="Number of study sessions previously completed for this topic"
    )
    subjectCategory: Optional[str] = Field(
        default="GENERAL",
        validation_alias=AliasChoices("subjectCategory", "subject_category", "category"),
        description="Subject classification category (e.g., STEM, HUMANITIES, GENERAL)"
    )

    @field_validator("topicDifficulty", mode="before")
    @classmethod
    def normalize_difficulty(cls, v):
        if isinstance(v, str):
            v_upper = v.strip().upper()
            if v_upper in DifficultyLevel.__members__:
                return DifficultyLevel(v_upper)
        return v

    model_config = {
        "populate_by_name": True,
        "json_schema_extra": {
            "example": {
                "topicDifficulty": "HARD",
                "estimatedMinutes": 60,
                "historicalAverageMinutes": 75.0,
                "previousStudySessions": 3,
                "subjectCategory": "STEM"
            }
        }
    }


class PredictionResponse(BaseModel):
    predictedMinutes: int = Field(
        ...,
        ge=1,
        description="Predicted realistic study duration in minutes"
    )
    confidenceScore: float = Field(
        default=0.85,
        ge=0.0,
        le=1.0,
        description="Model confidence score or calibration metric (0.0 to 1.0)"
    )
    estimatedErrorRangeMinutes: int = Field(
        default=10,
        ge=0,
        description="Estimated standard error margin (+/- minutes)"
    )
    modelVersion: str = Field(
        ...,
        description="Active ML model version (e.g. 1.0 or fallback)"
    )
    predictionSource: str = Field(
        ...,
        description="Origin of prediction: EXPERIMENTAL_ML or HEURISTIC_FALLBACK"
    )
    details: Optional[str] = Field(
        default=None,
        description="Diagnostic information or reason for fallback"
    )


class HealthResponse(BaseModel):
    status: str
    modelLoaded: bool
    modelVersion: str
    predictionSource: str
    uptimeSeconds: float

