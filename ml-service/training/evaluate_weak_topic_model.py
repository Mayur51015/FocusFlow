"""
Evaluation script for FocusFlow Weak Topic Classifier.
Runs comprehensive diagnostics on edge cases, unassessed topics, and low/high performers.
"""

import os
import sys
import json
import joblib
import pandas as pd
import numpy as np

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

MODEL_PATH = os.path.join(BASE_DIR, "artifacts", "weak_topic_model.joblib")
METADATA_PATH = os.path.join(BASE_DIR, "artifacts", "weak_topic_metadata.json")

FEATURE_NAMES = [
    "difficulty_num",
    "average_quiz_score",
    "attempt_count",
    "revision_count",
    "recent_study_minutes",
    "last_quiz_score"
]


def evaluate():
    if not os.path.exists(MODEL_PATH):
        print(f"Model not found at {MODEL_PATH}. Run train_weak_topic_model.py first.")
        return

    model = joblib.load(MODEL_PATH)
    print("Loaded model classes:", model.classes_)

    test_scenarios = [
        {
            "desc": "Struggling student: HARD topic, 42% avg score, 3 attempts, 1 revision, 120m study",
            "features": [3, 42.0, 3, 1, 120, 40.0],
            "expected": "WEAK"
        },
        {
            "desc": "Mastered student: MEDIUM topic, 92% avg score, 2 attempts, 2 revisions, 90m study",
            "features": [2, 92.0, 2, 2, 90, 95.0],
            "expected": "STRONG"
        },
        {
            "desc": "Average student: EASY topic, 65% avg score, 1 attempt, 0 revisions, 45m study",
            "features": [1, 65.0, 1, 0, 45, 65.0],
            "expected": "MODERATE"
        },
        {
            "desc": "Unassessed student with some study: MEDIUM topic, 0% score, 0 attempts, 0 revisions, 80m study",
            "features": [2, 0.0, 0, 0, 80, 0.0],
            "expected": "MODERATE"
        },
        {
            "desc": "Neglected topic: HARD topic, 0% score, 0 attempts, 0 revisions, 10m study",
            "features": [3, 0.0, 0, 0, 10, 0.0],
            "expected": "WEAK"
        }
    ]

    print("\n--- Testing Diagnostic Scenarios ---")
    for s in test_scenarios:
        df = pd.DataFrame([s["features"]], columns=FEATURE_NAMES)
        pred = model.predict(df)[0]
        probs = model.predict_proba(df)[0]
        prob_dict = {cls: round(float(p), 3) for cls, p in zip(model.classes_, probs)}
        print(f"\nScenario: {s['desc']}")
        print(f"Prediction: {pred} (Expected: {s['expected']}) | Probabilities: {prob_dict}")


if __name__ == "__main__":
    evaluate()

