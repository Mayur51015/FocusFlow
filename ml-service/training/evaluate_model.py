"""
Model Evaluation Script for FocusFlow Study Duration Predictor.

Performs slice-based evaluation and error analysis across difficulty categories.
"""

import os
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, r2_score
from generate_sample_data import generate_synthetic_study_data

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "artifacts"))
MODEL_PATH = os.path.join(ARTIFACTS_DIR, "duration_model.joblib")


def evaluate_saved_model():
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError(f"Model artifact not found at: {MODEL_PATH}. Run train_model.py first.")

    pipeline = joblib.load(MODEL_PATH)
    print(f"Loaded model from: {MODEL_PATH}")

    # Generate fresh evaluation holdout
    eval_df = generate_synthetic_study_data(n_samples=1000)
    feature_cols = [
        "difficulty",
        "estimated_minutes",
        "historical_average_minutes",
        "previous_study_sessions",
        "subject_category"
    ]

    X = eval_df[feature_cols]
    y = eval_df["actual_minutes"]

    preds = pipeline.predict(X)
    overall_mae = mean_absolute_error(y, preds)
    overall_r2 = r2_score(y, preds)

    print("\n=======================================================")
    print("      FOCUSFLOW MODEL EVALUATION & SLICE ANALYSIS      ")
    print("=======================================================")
    print(f"Overall MAE: {overall_mae:.2f} mins")
    print(f"Overall R²:  {overall_r2:.4f}")

    eval_df["predicted"] = preds
    eval_df["abs_error"] = np.abs(y - preds)

    print("\nSlice Metrics by Topic Difficulty:")
    for diff in ["EASY", "MEDIUM", "HARD"]:
        subset = eval_df[eval_df["difficulty"] == diff]
        mae_slice = subset["abs_error"].mean()
        avg_est = subset["estimated_minutes"].mean()
        avg_act = subset["actual_minutes"].mean()
        avg_pred = subset["predicted"].mean()
        print(f"  [{diff:6s}] Samples: {len(subset):3d} | MAE: {mae_slice:5.2f}m | Avg Est: {avg_est:5.1f}m | Avg Actual: {avg_act:5.1f}m | Avg Pred: {avg_pred:5.1f}m")

    print("\nSample Predictions vs Ground Truth:")
    sample = eval_df.sample(5, random_state=42)
    for _, row in sample.iterrows():
        print(f"  Difficulty: {row['difficulty']:6s} | Est: {row['estimated_minutes']:3.0f}m | Hist: {row['historical_average_minutes']:4.1f}m | Actual: {row['actual_minutes']:3.0f}m | Pred: {row['predicted']:4.1f}m")
    print("=======================================================\n")


if __name__ == "__main__":
    evaluate_saved_model()

