"""
Unit tests for Exam Readiness Estimation endpoint and model logic.
"""

import os
import sys
import unittest

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from fastapi.testclient import TestClient
from app.main import app


class TestExamReadiness(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_predict_exam_readiness_high(self):
        payload = {
            "subjectId": 1,
            "subjectName": "Data Structures",
            "topicCompletionRate": 0.90,
            "averageQuizScore": 85.0,
            "weakTopicRatio": 0.0,
            "revisionCompletionRate": 0.85,
            "daysUntilExam": 10,
            "importance": "HIGH",
            "recentQuizTrend": 90.0
        }
        response = self.client.post("/ml/predict-exam-readiness", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["subjectId"], 1)
        self.assertGreaterEqual(data["readinessScore"], 70.0)
        self.assertIn(data["readinessLevel"], ["HIGH", "MODERATE"])
        self.assertIn("componentBreakdown", data)
        self.assertGreater(len(data["recommendations"]), 0)

    def test_predict_exam_readiness_urgent_penalty(self):
        payload = {
            "subjectId": 2,
            "subjectName": "Operating Systems",
            "topicCompletionRate": 0.30,
            "averageQuizScore": 45.0,
            "weakTopicRatio": 0.40,
            "revisionCompletionRate": 0.20,
            "daysUntilExam": 2,
            "importance": "HIGH",
            "recentQuizTrend": 40.0
        }
        response = self.client.post("/ml/predict-exam-readiness", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["readinessLevel"], "LOW")
        self.assertLess(data["readinessScore"], 50.0)
        self.assertTrue(any("Critical" in r or "Exam is in" in r for r in data["recommendations"]))

    def test_predict_exam_readiness_batch(self):
        batch_payload = {
            "subjects": [
                {
                    "subjectId": 1,
                    "topicCompletionRate": 0.85,
                    "averageQuizScore": 80.0,
                    "weakTopicRatio": 0.05,
                    "revisionCompletionRate": 0.75,
                    "daysUntilExam": 12
                },
                {
                    "subjectId": 2,
                    "topicCompletionRate": 0.25,
                    "averageQuizScore": 35.0,
                    "weakTopicRatio": 0.50,
                    "revisionCompletionRate": 0.10,
                    "daysUntilExam": 3
                }
            ]
        }
        response = self.client.post("/ml/predict-exam-readiness/batch", json=batch_payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["totalSubjects"], 2)
        self.assertEqual(len(data["results"]), 2)
        self.assertTrue(0.0 <= data["overallReadinessScore"] <= 100.0)


if __name__ == "__main__":
    unittest.main()

