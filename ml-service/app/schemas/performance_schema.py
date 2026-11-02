from enum import Enum
from typing import Optional, List, Dict
from pydantic import BaseModel, Field, field_validator, AliasChoices


class PerformanceLevel(str, Enum):
    STRONG = "STRONG"
    MODERATE = "MODERATE"
    WEAK = "WEAK"


class TopicDifficulty(str, Enum):
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"


class WeakTopicRequest(BaseModel):
    topicId: Optional[int] = Field(
        default=None,
        validation_alias=AliasChoices("topicId", "topic_id", "id"),
        description="Unique identifier for the topic"
    )
    topicDifficulty: TopicDifficulty = Field(
        default=TopicDifficulty.MEDIUM,
        validation_alias=AliasChoices("topicDifficulty", "topic_difficulty", "difficulty"),
        description="Difficulty level of the topic (EASY, MEDIUM, HARD)"
    )
    averageQuizScore: float = Field(
        default=0.0,
        ge=0.0,
        le=100.0,
        validation_alias=AliasChoices("averageQuizScore", "average_quiz_score", "quizScore", "score"),
        description="Average percentage score on quizzes for this topic (0.0 to 100.0)"
    )
    attemptCount: int = Field(
        default=0,
        ge=0,
        validation_alias=AliasChoices("attemptCount", "attempt_count", "attempts"),
        description="Number of quiz attempts conducted on this topic"
    )
    revisionCount: int = Field(
        default=0,
        ge=0,
        validation_alias=AliasChoices("revisionCount", "revision_count", "revisions"),
        description="Number of spaced revision sessions completed for this topic"
    )
    recentStudyMinutes: int = Field(
        default=0,
        ge=0,
        validation_alias=AliasChoices("recentStudyMinutes", "recent_study_minutes", "studyMinutes"),
        description="Total or recent study duration in minutes for this topic"
    )
    lastQuizScore: Optional[float] = Field(
        default=None,
        ge=0.0,
        le=100.0,
        validation_alias=AliasChoices("lastQuizScore", "last_quiz_score"),
        description="Score percentage on the most recent quiz attempt"
    )

    @field_validator("topicDifficulty", mode="before")
    @classmethod
    def normalize_difficulty(cls, v):
        if isinstance(v, str):
            v_upper = v.strip().upper()
            if v_upper in TopicDifficulty.__members__:
                return TopicDifficulty(v_upper)
        return v

    model_config = {
        "populate_by_name": True,
        "json_schema_extra": {
            "example": {
                "topicId": 12,
                "topicDifficulty": "HARD",
                "averageQuizScore": 42.0,
                "attemptCount": 3,
                "revisionCount": 1,
                "recentStudyMinutes": 120,
                "lastQuizScore": 40.0
            }
        }
    }


class WeakTopicResponse(BaseModel):
    topicId: Optional[int] = Field(default=None, description="Topic ID if provided")
    performanceLevel: PerformanceLevel = Field(..., description="Classified level: STRONG, MODERATE, or WEAK")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Model prediction confidence score")
    predictionSource: str = Field(..., description="'RANDOM_FOREST' or 'RULE_BASED'")
    probabilities: Optional[Dict[str, float]] = Field(default=None, description="Class probabilities if ML was used")
    explanation: str = Field(..., description="Actionable rationale explaining the classification")

    model_config = {
        "populate_by_name": True,
        "json_schema_extra": {
            "example": {
                "topicId": 12,
                "performanceLevel": "WEAK",
                "confidence": 0.88,
                "predictionSource": "RANDOM_FOREST",
                "probabilities": {
                    "WEAK": 0.88,
                    "MODERATE": 0.10,
                    "STRONG": 0.02
                },
                "explanation": "Low quiz score (42.0%) on HARD difficulty after 3 attempts indicates knowledge gaps."
            }
        }
    }


class WeakTopicBatchRequest(BaseModel):
    topics: List[WeakTopicRequest] = Field(..., description="List of topic metrics to classify")


class WeakTopicBatchResponse(BaseModel):
    predictions: List[WeakTopicResponse] = Field(..., description="List of classifications")
    totalTopics: int = Field(..., description="Total topics evaluated")
    weakCount: int = Field(default=0, description="Count of topics classified as WEAK")
    moderateCount: int = Field(default=0, description="Count of topics classified as MODERATE")
    strongCount: int = Field(default=0, description="Count of topics classified as STRONG")

