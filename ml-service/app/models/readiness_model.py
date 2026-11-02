"""
Exam Readiness Model and Estimation Engine.
Combines a trained Random Forest Regressor with a transparent rule-based fallback
and generates actionable academic study recommendations.
"""

import os
import json
import logging
from typing import Dict, Any, List, Tuple
import joblib
import numpy as np

logger = logging.getLogger("focusflow.ml.readiness")

ARTIFACT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "artifacts")
MODEL_PATH = os.path.join(ARTIFACT_DIR, "readiness_model.joblib")
META_PATH = os.path.join(ARTIFACT_DIR, "readiness_metadata.json")


class ExamReadinessModel:
    def __init__(self):
        self.model = None
        self.metadata = {}
        self.is_loaded = False
        self._load_model()

    def _load_model(self):
        try:
            if os.path.exists(MODEL_PATH) and os.path.exists(META_PATH):
                self.model = joblib.load(MODEL_PATH)
                with open(META_PATH, "r", encoding="utf-8") as f:
                    self.metadata = json.load(f)
                self.is_loaded = True
                logger.info(f"Loaded trained Exam Readiness ML model (version: {self.metadata.get('version', '1.0')})")
            else:
                logger.info("No pre-trained Exam Readiness ML model artifact found. Using rule-based engine.")
        except Exception as e:
            logger.warning(f"Failed to load ML model artifact: {e}. Falling back to rule-based engine.")
            self.model = None
            self.is_loaded = False

    def predict(
        self,
        topic_completion_rate: float,
        average_quiz_score: float,
        weak_topic_ratio: float,
        revision_completion_rate: float,
        days_until_exam: int,
        importance: str = "MEDIUM",
        recent_quiz_trend: float = None,
        subject_id: int = None,
        subject_name: str = None
    ) -> Dict[str, Any]:
        """
        Calculates exam readiness score, classification level, component breakdown,
        and prioritized recommendations.
        """
        if recent_quiz_trend is None:
            recent_quiz_trend = average_quiz_score

        # 1. Compute Transparent Rule-Based Components
        comp_points = round(topic_completion_rate * 100.0 * 0.35, 2)
        quiz_points = round(average_quiz_score * 0.35, 2)
        rev_points = round(revision_completion_rate * 100.0 * 0.20, 2)
        trend_points = round(recent_quiz_trend * 0.10, 2)
        base_score = comp_points + quiz_points + rev_points + trend_points

        # 2. Penalties calculation
        weak_penalty = 0.0
        if weak_topic_ratio > 0:
            weak_penalty = round(min(15.0, weak_topic_ratio * 20.0), 2)

        urgency_penalty = 0.0
        if days_until_exam <= 3 and topic_completion_rate < 0.50:
            urgency_penalty = 15.0

        importance_penalty = 0.0
        if importance.upper() == "HIGH" and topic_completion_rate < 0.70:
            importance_penalty = 5.0

        total_penalty = round(weak_penalty + urgency_penalty + importance_penalty, 2)
        rule_score = max(0.0, min(100.0, round(base_score - total_penalty, 1)))

        # 3. Model prediction (if available) or Rule-Based
        prediction_source = "RULE_BASED"
        final_score = rule_score

        if self.is_loaded and self.model is not None:
            try:
                imp_num = 1.0 if importance.upper() == "HIGH" else (0.5 if importance.upper() == "MEDIUM" else 0.0)
                # Feature array matching trained model signature
                features = np.array([[
                    topic_completion_rate,
                    average_quiz_score / 100.0,
                    weak_topic_ratio,
                    revision_completion_rate,
                    min(days_until_exam, 30) / 30.0,
                    imp_num,
                    recent_quiz_trend / 100.0
                ]])
                ml_pred = float(self.model.predict(features)[0])
                final_score = max(0.0, min(100.0, round(ml_pred, 1)))
                prediction_source = "TRAINED_ML"
            except Exception as ex:
                logger.warning(f"Inference error with ML model: {ex}. Using rule-based score.")
                final_score = rule_score
                prediction_source = "RULE_BASED"

        # 4. Classify Readiness Level
        if final_score >= 75.0:
            readiness_level = "HIGH"
        elif final_score >= 50.0:
            readiness_level = "MODERATE"
        else:
            readiness_level = "LOW"

        # 5. Generate Academic Recommendations
        recommendations = self._generate_recommendations(
            final_score,
            readiness_level,
            topic_completion_rate,
            average_quiz_score,
            weak_topic_ratio,
            revision_completion_rate,
            days_until_exam,
            importance
        )

        breakdown = {
            "topicCompletionContribution": comp_points,
            "quizAverageContribution": quiz_points,
            "revisionConsistencyContribution": rev_points,
            "recentTrendContribution": trend_points,
            "weakTopicPenalty": weak_penalty,
            "urgencyPenalty": urgency_penalty,
            "importancePenalty": importance_penalty,
            "totalPenalties": total_penalty
        }

        return {
            "subjectId": subject_id,
            "subjectName": subject_name,
            "readinessScore": final_score,
            "readinessLevel": readiness_level,
            "predictionSource": prediction_source,
            "daysUntilExam": days_until_exam,
            "componentBreakdown": breakdown,
            "recommendations": recommendations
        }

    def _generate_recommendations(
        self,
        score: float,
        level: str,
        topic_comp: float,
        quiz_avg: float,
        weak_ratio: float,
        rev_comp: float,
        days_left: int,
        importance: str
    ) -> List[str]:
        recs = []

        if days_left <= 3 and topic_comp < 0.60:
            recs.append(f"⚠️ Critical: Exam is in {days_left} day(s) with {round(topic_comp * 100)}% syllabus completed! Focus exclusively on core high-yield topics and summary flashcards.")
        elif days_left <= 7:
            recs.append(f"⏰ Exam approaching in {days_left} days. Transition from learning new topics to active recall flashcards and practice quizzes.")

        if weak_ratio > 0.0:
            recs.append(f"🔍 You have unaddressed weak topics ({round(weak_ratio * 100)}% of subject). Retake topic quizzes and review flashcards before moving ahead.")

        if topic_comp < 0.70:
            recs.append(f"📚 Syllabus completion is at {round(topic_comp * 100)}%. Prioritize remaining topics in your daily schedule to eliminate blind spots.")

        if quiz_avg < 60.0:
            recs.append(f"📝 Quiz average is {round(quiz_avg, 1)}%. Take practice assessments under timed conditions to strengthen conceptual depth.")
        elif quiz_avg >= 85.0:
            recs.append("🌟 Excellent quiz performance! Maintain mastery with occasional spaced repetitions.")

        if rev_comp < 0.50:
            recs.append("🧠 Revision consistency is low. Review scheduled SM-2 flashcard decks daily to prevent memory decay.")

        if importance.upper() == "HIGH" and score < 75.0:
            recs.append("🎯 This is a HIGH importance subject. Dedicate at least 60-90 minutes daily in your study planner.")

        if not recs:
            recs.append("✅ Outstanding overall preparation! Keep reviewing upcoming flashcards to lock in retention before exam day.")

        return recs


exam_readiness_model = ExamReadinessModel()

