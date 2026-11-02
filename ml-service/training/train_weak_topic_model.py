"""
Training script for FocusFlow Weak Topic Random Forest Classifier.
Trains on representative academic performance features and serializes the model.
"""

import os
import sys
import json
import joblib
from datetime import datetime
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, accuracy_score, confusion_matrix

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from training.prepare_dataset import generate_weak_topic_dataset

ARTIFACTS_DIR = os.path.join(BASE_DIR, "artifacts")
os.makedirs(ARTIFACTS_DIR, exist_ok=True)

MODEL_PATH = os.path.join(ARTIFACTS_DIR, "weak_topic_model.joblib")
METADATA_PATH = os.path.join(ARTIFACTS_DIR, "weak_topic_metadata.json")

FEATURE_NAMES = [
    "difficulty_num",
    "average_quiz_score",
    "attempt_count",
    "revision_count",
    "recent_study_minutes",
    "last_quiz_score"
]
CLASSES = ["MODERATE", "STRONG", "WEAK"]


def train():
    print("Generating representative performance dataset (2,500 samples)...")
    df = generate_weak_topic_dataset(num_samples=2500, random_state=42)

    X = df[FEATURE_NAMES]
    y = df["label"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )

    print(f"Training dataset size: {len(X_train)}, Test dataset size: {len(X_test)}")
    print(f"Features: {FEATURE_NAMES}")

    rf = RandomForestClassifier(
        n_estimators=100,
        max_depth=6,
        min_samples_split=5,
        min_samples_leaf=3,
        random_state=42,
        class_weight="balanced"
    )

    cv_scores = cross_val_score(rf, X_train, y_train, cv=5, scoring="accuracy")
    print(f"5-Fold Cross-Validation Accuracy: {cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")

    rf.fit(X_train, y_train)

    # Evaluate on test set
    y_pred = rf.predict(X_test)
    test_acc = accuracy_score(y_test, y_pred)
    report = classification_report(y_test, y_pred, output_dict=True)

    print(f"\nTest Accuracy: {test_acc:.4f}")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    feature_importances = {
        name: round(float(imp), 4)
        for name, imp in zip(FEATURE_NAMES, rf.feature_importances_)
    }
    print("Feature Importances:")
    for k, v in feature_importances.items():
        print(f" - {k}: {v}")

    # Save model artifact
    joblib.dump(rf, MODEL_PATH)
    print(f"\nModel successfully saved to {MODEL_PATH}")

    # Save metadata
    metadata = {
        "modelName": "WeakTopicRandomForestClassifier",
        "algorithm": "RandomForestClassifier",
        "version": "1.0",
        "trainedAt": datetime.now().isoformat(),
        "features": FEATURE_NAMES,
        "classes": rf.classes_.tolist(),
        "crossValidationAccuracy": round(float(cv_scores.mean()), 4),
        "testAccuracy": round(float(test_acc), 4),
        "metrics": {
            "macroAvgF1": round(float(report["macro avg"]["f1-score"]), 4),
            "weightedAvgF1": round(float(report["weighted avg"]["f1-score"]), 4)
        },
        "featureImportances": feature_importances,
        "note": "Trained on synthetic representative academic data. Evaluates WEAK, MODERATE, STRONG topic mastery."
    }

    with open(METADATA_PATH, "w") as f:
        json.dump(metadata, f, indent=2)

    print(f"Metadata saved to {METADATA_PATH}")
    return rf, metadata


if __name__ == "__main__":
    train()

