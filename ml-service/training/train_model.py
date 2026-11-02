"""
Model Training Script for FocusFlow Study Duration Predictor.

Trains a Scikit-Learn Pipeline (OneHotEncoder + StandardScaler + RandomForestRegressor)
to predict realistic study durations in minutes.
"""

import os
import json
import datetime
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from generate_sample_data import generate_synthetic_study_data

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "artifacts"))


def build_pipeline() -> Pipeline:
    categorical_features = ["difficulty", "subject_category"]
    numerical_features = ["estimated_minutes", "historical_average_minutes", "previous_study_sessions"]

    preprocessor = ColumnTransformer(
        transformers=[
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), categorical_features),
            ("num", StandardScaler(), numerical_features),
        ]
    )

    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=8,
        min_samples_split=5,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1
    )

    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("regressor", model),
        ]
    )
    return pipeline


def train_and_evaluate(data_path: str = None) -> Pipeline:
    # 1. Load data
    if data_path and os.path.exists(data_path):
        print(f"Loading training data from: {data_path}")
        df = pd.read_csv(data_path)
    else:
        print("Generating fresh synthetic dataset for model training...")
        df = generate_synthetic_study_data(n_samples=3000)

    feature_cols = [
        "difficulty",
        "estimated_minutes",
        "historical_average_minutes",
        "previous_study_sessions",
        "subject_category"
    ]
    target_col = "actual_minutes"

    X = df[feature_cols]
    y = df[target_col]

    # 2. Strict ML Best Practice: Split train/test BEFORE any preprocessing
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42
    )
    print(f"Dataset split: {len(X_train)} training samples, {len(X_test)} testing samples.")

    # 3. Build & fit pipeline
    pipeline = build_pipeline()
    print("Fitting Random Forest Regressor Pipeline...")
    pipeline.fit(X_train, y_train)

    # 4. Evaluate on test set
    y_pred_test = pipeline.predict(X_test)
    mae = float(mean_absolute_error(y_test, y_pred_test))
    rmse = float(np.sqrt(mean_squared_error(y_test, y_pred_test)))
    r2 = float(r2_score(y_test, y_pred_test))

    print("\n--- Model Evaluation Results (Test Set) ---")
    print(f"Mean Absolute Error (MAE): {mae:.2f} minutes")
    print(f"Root Mean Squared Error (RMSE): {rmse:.2f} minutes")
    print(f"R-squared Score (R²): {r2:.4f}")

    # 5. Save artifacts
    os.makedirs(ARTIFACTS_DIR, exist_ok=True)
    model_save_path = os.path.join(ARTIFACTS_DIR, "duration_model.joblib")
    metadata_save_path = os.path.join(ARTIFACTS_DIR, "model_metadata.json")

    joblib.dump(pipeline, model_save_path)
    print(f"Saved trained model artifact to: {model_save_path}")

    metadata = {
        "version": "1.0",
        "algorithm": "RandomForestRegressor",
        "feature_columns": feature_cols,
        "target_column": target_col,
        "metrics": {
            "test_mae": round(mae, 2),
            "test_rmse": round(rmse, 2),
            "test_r2": round(r2, 4),
        },
        "trained_samples_count": len(X_train),
        "test_samples_count": len(X_test),
        "trained_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "synthetic_data_note": "Model trained on synthetic calibrated data for MVP 1 development and initial deployment."
    }

    with open(metadata_save_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Saved model metadata to: {metadata_save_path}")

    return pipeline


if __name__ == "__main__":
    train_and_evaluate()

