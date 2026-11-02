"""
Unit and Integration Tests for FocusFlow ML Microservice.
Tests valid predictions, invalid inputs, edge cases, clamping, and heuristic fallback.
Works directly with standard Python unittest or pytest.
"""

import os
import sys
import unittest

# Ensure ml-service root directory is in sys.path
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from fastapi.testclient import TestClient
from app.main import app
from app.models.duration_model import DurationPredictorModel
from app.schemas.prediction_schema import PredictionRequest, DifficultyLevel


class TestMlMicroservice(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_health_endpoint(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "UP")
        self.assertIn("modelLoaded", data)
        self.assertIn("modelVersion", data)
        self.assertIn("uptimeSeconds", data)

    def test_predict_duration_valid_hard(self):
        payload = {
            "topicDifficulty": "HARD",
            "estimatedMinutes": 60,
            "historicalAverageMinutes": 75.0,
            "previousStudySessions": 2,
            "subjectCategory": "STEM"
        }
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertGreaterEqual(data["predictedMinutes"], 60)
        self.assertEqual(data["modelVersion"], "1.0")
        self.assertEqual(data["predictionSource"], "EXPERIMENTAL_ML")
        self.assertGreaterEqual(data["confidenceScore"], 0.50)
        self.assertLessEqual(data["confidenceScore"], 1.0)
        self.assertGreater(data["estimatedErrorRangeMinutes"], 0)

    def test_predict_duration_valid_easy(self):
        payload = {
            "topicDifficulty": "EASY",
            "estimatedMinutes": 60,
            "historicalAverageMinutes": 50.0,
            "previousStudySessions": 3
        }
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertLessEqual(data["predictedMinutes"], 65)
        self.assertEqual(data["predictionSource"], "EXPERIMENTAL_ML")

    def test_predict_duration_snake_case_support(self):
        # Support snake_case keys transparently
        payload = {
            "topic_difficulty": "HARD",
            "estimated_minutes": 60,
            "historical_average_minutes": 70.0,
            "previous_study_sessions": 1
        }
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertGreaterEqual(data["predictedMinutes"], 60)

    def test_predict_duration_missing_required_field(self):
        payload = {"topicDifficulty": "HARD"}
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_duration_invalid_difficulty(self):
        payload = {"topicDifficulty": "EXTREME", "estimatedMinutes": 60}
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_duration_negative_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": -30}
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_duration_zero_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": 0}
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_predict_duration_excessive_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": 9999}
        response = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_heuristic_fallback_when_model_unloaded(self):
        fallback_model = DurationPredictorModel(model_path="nonexistent_model.joblib")
        self.assertFalse(fallback_model.is_loaded)

        req = PredictionRequest(
            topicDifficulty=DifficultyLevel.HARD,
            estimatedMinutes=60,
            historicalAverageMinutes=80.0
        )

        pred, conf, err_range, ver, source, details = fallback_model.predict(req)
        self.assertEqual(source, "HEURISTIC_FALLBACK")
        self.assertEqual(ver, "fallback")
        self.assertGreaterEqual(pred, 60)
        self.assertEqual(conf, 0.70)
        self.assertIn("fallback", details.lower())


if __name__ == "__main__":
    unittest.main(verbosity=2)
