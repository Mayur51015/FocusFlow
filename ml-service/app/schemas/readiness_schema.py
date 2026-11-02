"""
Pydantic Schemas for Exam Readiness Estimation.
Defines input features, scoring breakdowns, readiness levels, and recommendations.
"""

from typing import List, Optional, Dict
from pydantic import BaseModel, Field


class ExamReadinessRequest(BaseModel):
    subjectId: Optional[int] = Field(None, description="Optional ID of the subject being evaluated")
    subjectName: Optional[str] = Field(None, description="Name of the subject")
    topicCompletionRate: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ratio of completed topics in the subject (0.0 to 1.0)"
    )
    averageQuizScore: float = Field(
        ...,
        ge=0.0,
        le=100.0,
        description="Average percentage scored on subject quizzes (0.0 to 100.0)"
    )
    weakTopicRatio: float = Field(
        0.0,
        ge=0.0,
        le=1.0,
        description="Ratio of weak topics to total topics (0.0 to 1.0)"
    )
    revisionCompletionRate: float = Field(
        0.0,
        ge=0.0,
        le=1.0,
        description="Ratio of scheduled revisions completed (0.0 to 1.0)"
    )
    daysUntilExam: int = Field(
        ...,
        description="Remaining days until examination date"
    )
    importance: Optional[str] = Field(
        "MEDIUM",
        description="Importance level: HIGH, MEDIUM, LOW"
    )
    recentQuizTrend: Optional[float] = Field(
        None,
        ge=0.0,
        le=100.0,
        description="Average score on the last 3 quizzes (0.0 to 100.0). Defaults to averageQuizScore."
    )


class ExamReadinessResponse(BaseModel):
    subjectId: Optional[int] = Field(None, description="Subject ID")
    subjectName: Optional[str] = Field(None, description="Subject Name")
    readinessScore: float = Field(..., description="Calculated Readiness Score (0.0 to 100.0)")
    readinessLevel: str = Field(..., description="Readiness Classification: LOW, MODERATE, HIGH")
    predictionSource: str = Field(..., description="Source: TRAINED_ML or RULE_BASED")
    daysUntilExam: int = Field(..., description="Days remaining until examination")
    componentBreakdown: Dict[str, float] = Field(
        ...,
        description="Breakdown of points contributed by topic completion, quizzes, revisions, and trend"
    )
    recommendations: List[str] = Field(
        default_factory=list,
        description="Actionable academic study recommendations"
    )


class ExamReadinessBatchRequest(BaseModel):
    subjects: List[ExamReadinessRequest] = Field(..., min_length=1)


class ExamReadinessBatchResponse(BaseModel):
    results: List[ExamReadinessResponse]
    overallReadinessScore: float
    overallReadinessLevel: str
    totalSubjects: int

