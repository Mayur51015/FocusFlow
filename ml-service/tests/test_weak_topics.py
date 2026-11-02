"""
Unit tests for Weak Topic Prediction endpoint and service.
Uses standard Python unittest.
"""

import os
import sys
import unittest
from fastapi.testclient import TestClient

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from app.main import app


class TestWeakTopics(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_predict_weak_topics_single_weak(self):
        payload = {
            "topicId": 12,
            "topicDifficulty": "HARD",
            "averageQuizScore": 42.0,
            "attemptCount": 3,
            "revisionCount": 1,
            "recentStudyMinutes": 120,
            "lastQuizScore": 40.0
        }
        response = self.client.post("/ml/predict-weak-topics", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["topicId"], 12)
        self.assertEqual(data["performanceLevel"], "WEAK")
        self.assertIn(data["predictionSource"], ["RANDOM_FOREST", "RULE_BASED"])
        self.assertGreater(data["confidence"], 0.5)

    def test_predict_weak_topics_single_strong(self):
        payload = {
            "topicId": 5,
            "topicDifficulty": "MEDIUM",
            "averageQuizScore": 95.0,
            "attemptCount": 2,
            "revisionCount": 2,
            "recentStudyMinutes": 90,
            "lastQuizScore": 95.0
        }
        response = self.client.post("/ml/predict-weak-topics", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["topicId"], 5)
        self.assertEqual(data["performanceLevel"], "STRONG")

    def test_predict_weak_topics_batch(self):
        batch_payload = {
            "topics": [
                {
                    "topicId": 1,
                    "topicDifficulty": "HARD",
                    "averageQuizScore": 30.0,
                    "attemptCount": 2,
                    "revisionCount": 0,
                    "recentStudyMinutes": 45
                },
                {
                    "topicId": 2,
                    "topicDifficulty": "EASY",
                    "averageQuizScore": 90.0,
                    "attemptCount": 1,
                    "revisionCount": 1,
                    "recentStudyMinutes": 60
                }
            ]
        }
        response = self.client.post("/ml/predict-weak-topics/batch", json=batch_payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["totalTopics"], 2)
        self.assertGreaterEqual(data["weakCount"], 1)
        self.assertGreaterEqual(data["strongCount"], 1)
        self.assertEqual(len(data["predictions"]), 2)

    def test_unassessed_topic_fallback(self):
        payload = {
            "topicId": 99,
            "topicDifficulty": "MEDIUM",
            "averageQuizScore": 0.0,
            "attemptCount": 0,
            "revisionCount": 0,
            "recentStudyMinutes": 75
        }
        response = self.client.post("/ml/predict-weak-topics", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["topicId"], 99)
        self.assertIn(data["performanceLevel"], ["MODERATE", "WEAK"])


if __name__ == "__main__":
    unittest.main()
