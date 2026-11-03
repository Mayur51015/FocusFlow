"""
Training script for the Exam Readiness ML Regressor.
Generates representative synthetic academic data covering syllabus completion,
quiz averages, weak topic ratios, revision rates, and exam countdowns.
Trains a Scikit-Learn RandomForestRegressor and outputs model artifacts.
"""

import os
import json
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_squared_error, r2_score
import joblib

ARTIFACT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "artifacts")
os.makedirs(ARTIFACT_DIR, exist_ok=True)

MODEL_PATH = os.path.join(ARTIFACT_DIR, "readiness_model.joblib")
META_PATH = os.path.join(ARTIFACT_DIR, "readiness_metadata.json")


def generate_readiness_dataset(n_samples: int = 2500, random_state: int = 42):
    np.random.seed(random_state)

    # 1. Topic completion rate [0.0, 1.0]
    topic_completion = np.random.beta(a=3.0, b=2.0, size=n_samples)

    # 2. Average quiz score [0.0, 1.0] (correlated with completion + noise)
    quiz_score = np.clip(0.3 * topic_completion + 0.7 * np.random.beta(3, 2, size=n_samples), 0.0, 1.0)

    # 3. Weak topic ratio [0.0, 1.0] (inversely correlated with quiz score)
    weak_ratio = np.clip((1.0 - quiz_score) * np.random.uniform(0.2, 0.9, size=n_samples), 0.0, 1.0)

    # 4. Revision completion rate [0.0, 1.0]
    revision_rate = np.clip(0.4 * topic_completion + 0.6 * np.random.beta(2, 2, size=n_samples), 0.0, 1.0)

    # 5. Days until exam [1, 30] normalized to [0, 1]
    days_left = np.random.randint(1, 31, size=n_samples)
    days_left_norm = days_left / 30.0

    # 6. Importance: 1.0 (HIGH), 0.5 (MEDIUM), 0.0 (LOW)
    importance_num = np.random.choice([0.0, 0.5, 1.0], size=n_samples, p=[0.2, 0.5, 0.3])

    # 7. Recent quiz trend [0.0, 1.0]
    recent_trend = np.clip(quiz_score + np.random.normal(0, 0.08, size=n_samples), 0.0, 1.0)

    # Calculate target readiness score
    base_score = (
        (topic_completion * 100.0 * 0.35) +
        (quiz_score * 100.0 * 0.35) +
        (revision_rate * 100.0 * 0.20) +
        (recent_trend * 100.0 * 0.10)
    )

    # Penalties
    weak_penalties = np.minimum(15.0, weak_ratio * 20.0)
    urgency_penalties = np.where((days_left <= 3) & (topic_completion < 0.50), 15.0, 0.0)
    importance_penalties = np.where((importance_num == 1.0) & (topic_completion < 0.70), 5.0, 0.0)

    target_score = np.clip(
        base_score - weak_penalties - urgency_penalties - importance_penalties + np.random.normal(0, 1.2, size=n_samples),
        0.0,
        100.0
    )

    X = np.column_stack([
        topic_completion,
        quiz_score,
        weak_ratio,
        revision_rate,
        days_left_norm,
        importance_num,
        recent_trend
    ])

    return X, target_score


def train_and_save():
    print("Generating representative academic training data for Exam Readiness...")
    X, y = generate_readiness_dataset(n_samples=3000)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    print("Training RandomForestRegressor...")
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=12,
        min_samples_split=4,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    cv_scores = cross_val_score(model, X_train, y_train, cv=5, scoring="r2")

    print(f"Model Evaluation: R2 Score = {r2:.4f}, RMSE = {np.sqrt(mse):.4f}, CV Mean R2 = {cv_scores.mean():.4f}")

    # Persist model
    joblib.dump(model, MODEL_PATH)
    metadata = {
        "modelType": "RandomForestRegressor",
        "version": "1.0",
        "features": [
            "topicCompletionRate",
            "quizScoreNormalized",
            "weakTopicRatio",
            "revisionCompletionRate",
            "daysUntilExamNormalized",
            "importanceNumeric",
            "recentQuizTrendNormalized"
        ],
        "metrics": {
            "r2Score": round(float(r2), 4),
            "rmse": round(float(np.sqrt(mse)), 4),
            "cvMeanR2": round(float(cv_scores.mean()), 4)
        }
    }
    with open(META_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"Model artifact saved to {MODEL_PATH}")
    print(f"Metadata saved to {META_PATH}")


if __name__ == "__main__":
    train_and_save()

