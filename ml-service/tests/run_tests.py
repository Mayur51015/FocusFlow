import unittest
import os
import sys

# Ensure ml-service root is in sys.path
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

    def test_01_health_check(self):
        resp = self.client.get("/health")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["status"], "UP")
        self.assertTrue(data["modelLoaded"])
        self.assertEqual(data["modelVersion"], "1.0")
        self.assertEqual(data["predictionSource"], "EXPERIMENTAL_ML")

    def test_02_valid_prediction_hard(self):
        payload = {
            "topicDifficulty": "HARD",
            "estimatedMinutes": 60,
            "historicalAverageMinutes": 75.0,
            "previousStudySessions": 2,
            "subjectCategory": "STEM"
        }
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertGreaterEqual(data["predictedMinutes"], 60)
        self.assertEqual(data["modelVersion"], "1.0")
        self.assertEqual(data["predictionSource"], "EXPERIMENTAL_ML")
        self.assertGreaterEqual(data["confidenceScore"], 0.50)
        self.assertLessEqual(data["confidenceScore"], 1.0)
        self.assertGreater(data["estimatedErrorRangeMinutes"], 0)

    def test_03_valid_prediction_easy(self):
        payload = {
            "topicDifficulty": "EASY",
            "estimatedMinutes": 60,
            "historicalAverageMinutes": 50.0,
            "previousStudySessions": 3
        }
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertLessEqual(data["predictedMinutes"], 65)
        self.assertEqual(data["predictionSource"], "EXPERIMENTAL_ML")

    def test_04_missing_required_field(self):
        payload = {"topicDifficulty": "HARD"}
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 422)

    def test_05_invalid_difficulty(self):
        payload = {"topicDifficulty": "IMPOSSIBLE", "estimatedMinutes": 60}
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 422)

    def test_06_negative_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": -15}
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 422)

    def test_07_zero_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": 0}
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 422)

    def test_08_excessive_minutes(self):
        payload = {"topicDifficulty": "MEDIUM", "estimatedMinutes": 9999}
        resp = self.client.post("/ml/predict-duration", json=payload)
        self.assertEqual(resp.status_code, 422)

    def test_09_heuristic_fallback(self):
        fallback_model = DurationPredictorModel(model_path="missing_file.joblib")
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

