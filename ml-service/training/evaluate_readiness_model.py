"""
Evaluation script to benchmark Exam Readiness model across diverse student scenarios.
"""

import os
import joblib
import numpy as np

ARTIFACT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "artifacts")
MODEL_PATH = os.path.join(ARTIFACT_DIR, "readiness_model.joblib")


def evaluate_benchmarks():
    if not os.path.exists(MODEL_PATH):
        print("Model file does not exist. Train the model first.")
        return

    model = joblib.load(MODEL_PATH)
    print("Evaluating Scenarios against trained model:\n")

    scenarios = [
        {
            "name": "Scenario 1: High preparation (90% syllabus, 88% quiz, 0 weak, 80% rev, 10d left)",
            "vector": [0.90, 0.88, 0.0, 0.80, 10 / 30.0, 1.0, 0.90]
        },
        {
            "name": "Scenario 2: Moderate preparation (60% syllabus, 65% quiz, 10% weak, 50% rev, 7d left)",
            "vector": [0.60, 0.65, 0.10, 0.50, 7 / 30.0, 0.5, 0.65]
        },
        {
            "name": "Scenario 3: Urgent / Critical (30% syllabus, 40% quiz, 35% weak, 20% rev, 2d left)",
            "vector": [0.30, 0.40, 0.35, 0.20, 2 / 30.0, 1.0, 0.38]
        },
        {
            "name": "Scenario 4: New / Unstarted Subject (0% syllabus, 0% quiz, 0% weak, 0% rev, 25d left)",
            "vector": [0.0, 0.0, 0.0, 0.0, 25 / 30.0, 0.5, 0.0]
        }
    ]

    for s in scenarios:
        pred = model.predict(np.array([s["vector"]]))[0]
        level = "HIGH" if pred >= 75 else ("MODERATE" if pred >= 50 else "LOW")
        print(f"{s['name']}")
        print(f" -> Predicted Score: {pred:.1f}%, Level: {level}\n")


if __name__ == "__main__":
    evaluate_benchmarks()

