"""
Weak Topic Classification Model.
Loads Scikit-Learn Random Forest Classifier from artifacts or applies
a rule-based heuristic fallback if the artifact is missing or invalid.
"""

import os
import json
import logging
import joblib
import pandas as pd
from typing import Tuple, Dict, Any, Optional

from app.schemas.performance_schema import (
    WeakTopicRequest,
    PerformanceLevel,
    TopicDifficulty
)

logger = logging.getLogger("focusflow.ml.weak_topic_model")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ARTIFACTS_DIR = os.path.join(BASE_DIR, "artifacts")
MODEL_PATH = os.path.join(ARTIFACTS_DIR, "weak_topic_model.joblib")
METADATA_PATH = os.path.join(ARTIFACTS_DIR, "weak_topic_metadata.json")

FEATURE_NAMES = [
    "difficulty_num",
    "average_quiz_score",
    "attempt_count",
    "revision_count",
    "recent_study_minutes",
    "last_quiz_score"
]


class WeakTopicClassifierModel:
    def __init__(self):
        self.model = None
        self.metadata = {}
        self.version = "1.0"
        self._load_model()

    def _load_model(self):
        try:
            if os.path.exists(MODEL_PATH):
                self.model = joblib.load(MODEL_PATH)
                logger.info(f"Weak topic model successfully loaded from {MODEL_PATH}")
            else:
                logger.warning(f"No weak topic model found at {MODEL_PATH}. Rule-based fallback active.")

            if os.path.exists(METADATA_PATH):
                with open(METADATA_PATH, "r") as f:
                    self.metadata = json.load(f)
                    self.version = self.metadata.get("version", "1.0")
        except Exception as e:
            logger.error(f"Failed to load weak topic model: {e}. Falling back to rule-based.")
            self.model = None

    @property
    def is_loaded(self) -> bool:
        return self.model is not None

    def _difficulty_to_num(self, diff: TopicDifficulty) -> int:
        if diff == TopicDifficulty.EASY:
            return 1
        elif diff == TopicDifficulty.HARD:
            return 3
        return 2

    def _rule_based_classify(self, req: WeakTopicRequest) -> Tuple[PerformanceLevel, float, str, Dict[str, float]]:
        """
        Rule-based heuristic baseline for academic mastery classification.
        """
        score = req.averageQuizScore
        attempts = req.attemptCount
        revisions = req.revisionCount
        study_min = req.recentStudyMinutes
        diff = req.topicDifficulty

        # Scenario 1: No quiz attempts conducted yet
        if attempts == 0:
            if study_min >= 60:
                explanation = f"Topic has {study_min}m study time but no quiz attempts. Classified as MODERATE pending assessment."
                return PerformanceLevel.MODERATE, 0.70, explanation, {"MODERATE": 0.70, "WEAK": 0.30, "STRONG": 0.0}
            else:
                explanation = f"Topic has insufficient assessment data ({attempts} attempts, {study_min}m study). Marked as WEAK to prioritize learning."
                return PerformanceLevel.WEAK, 0.75, explanation, {"WEAK": 0.75, "MODERATE": 0.25, "STRONG": 0.0}

        # Scenario 2: High mastery (score >= 75%)
        if score >= 75.0:
            explanation = f"Strong quiz performance ({score:.1f}% avg across {attempts} attempts with {revisions} revisions)."
            return PerformanceLevel.STRONG, 0.90, explanation, {"STRONG": 0.90, "MODERATE": 0.08, "WEAK": 0.02}

        # Scenario 3: Failing / struggling (score < 50%)
        if score < 50.0:
            explanation = f"Quiz performance ({score:.1f}% avg) is below passing threshold across {attempts} attempts. Topic requires reinforcement."
            return PerformanceLevel.WEAK, 0.88, explanation, {"WEAK": 0.88, "MODERATE": 0.10, "STRONG": 0.02}

        # Scenario 4: Borderline performance (50% - 74%)
        if diff == TopicDifficulty.HARD and score < 60.0 and attempts >= 2:
            explanation = f"Below 60% ({score:.1f}%) on a HARD topic after {attempts} attempts indicates persistent conceptual difficulty."
            return PerformanceLevel.WEAK, 0.80, explanation, {"WEAK": 0.80, "MODERATE": 0.18, "STRONG": 0.02}

        explanation = f"Average quiz performance ({score:.1f}% avg, {attempts} attempts). Topic is at moderate mastery."
        return PerformanceLevel.MODERATE, 0.75, explanation, {"MODERATE": 0.75, "WEAK": 0.15, "STRONG": 0.10}

    def predict(self, req: WeakTopicRequest) -> Tuple[PerformanceLevel, float, str, Optional[Dict[str, float]], str]:
        """
        Classifies topic mastery using Random Forest Classifier or Rule-Based Fallback.
        Returns: (level, confidence, prediction_source, probabilities, explanation)
        """
        diff_num = self._difficulty_to_num(req.topicDifficulty)
        last_score = req.lastQuizScore if req.lastQuizScore is not None else req.averageQuizScore

        # Try ML Random Forest
        if self.is_loaded:
            try:
                features_df = pd.DataFrame([{
                    "difficulty_num": diff_num,
                    "average_quiz_score": req.averageQuizScore,
                    "attempt_count": req.attemptCount,
                    "revision_count": req.revisionCount,
                    "recent_study_minutes": req.recentStudyMinutes,
                    "last_quiz_score": last_score
                }], columns=FEATURE_NAMES)

                pred_class = self.model.predict(features_df)[0]
                probs = self.model.predict_proba(features_df)[0]
                prob_dict = {cls: round(float(p), 4) for cls, p in zip(self.model.classes_, probs)}
                confidence = prob_dict.get(pred_class, float(max(probs)))

                level = PerformanceLevel(pred_class)
                explanation = (
                    f"ML Random Forest classified as {level.value} with {confidence*100:.1f}% confidence "
                    f"based on quiz score ({req.averageQuizScore:.1f}%), {req.attemptCount} attempts, and {req.recentStudyMinutes}m study."
                )
                return level, confidence, "RANDOM_FOREST", prob_dict, explanation
            except Exception as e:
                logger.error(f"Error during ML inference: {e}. Falling back to rule-based.")

        # Fallback to rule-based
        level, confidence, explanation, prob_dict = self._rule_based_classify(req)
        return level, confidence, "RULE_BASED", prob_dict, explanation

