"""
Sample Synthetic Data Generator for FocusFlow Study Duration Prediction.

IMPORTANT NOTICE:
Synthetic training data is generated strictly for initial model development,
pipeline calibration, testing, and architecture verification.
Synthetic data does NOT guarantee or prove real-world model accuracy.
As real students use FocusFlow, historical session tracking from Phase 1
accumulates real empirical data to continually fine-tune the model.
"""

import os
import numpy as np
import pandas as pd

np.random.seed(42)

DIFFICULTIES = ["EASY", "MEDIUM", "HARD"]
DIFFICULTY_PROBS = [0.30, 0.45, 0.25]
CATEGORIES = ["STEM", "HUMANITIES", "COMMERCE", "GENERAL"]
CATEGORY_PROBS = [0.45, 0.25, 0.15, 0.15]

ESTIMATED_DURATIONS = [15, 30, 45, 60, 75, 90, 120]


def generate_synthetic_study_data(n_samples: int = 2000) -> pd.DataFrame:
    records = []

    for _ in range(n_samples):
        difficulty = np.random.choice(DIFFICULTIES, p=DIFFICULTY_PROBS)
        category = np.random.choice(CATEGORIES, p=CATEGORY_PROBS)
        estimated_minutes = float(np.random.choice(ESTIMATED_DURATIONS))
        previous_sessions = int(np.random.choice([0, 1, 2, 3, 4, 5, 6], p=[0.25, 0.20, 0.18, 0.15, 0.10, 0.07, 0.05]))

        # Base multiplier per difficulty
        if difficulty == "EASY":
            multiplier = np.random.normal(0.90, 0.08)
        elif difficulty == "MEDIUM":
            multiplier = np.random.normal(1.08, 0.10)
        else: # HARD
            multiplier = np.random.normal(1.32, 0.14)

        # Experience effect: students with 3+ sessions are slightly faster (learning curve)
        experience_discount = max(0.90, 1.0 - (previous_sessions * 0.02))
        effective_multiplier = multiplier * experience_discount

        # Historical average minutes
        hist_noise = np.random.normal(0, estimated_minutes * 0.08)
        historical_average = max(15.0, round(estimated_minutes * effective_multiplier + hist_noise, 1))

        # Target actual minutes with stochastic noise
        actual_noise = np.random.normal(0, 6.0)
        actual_minutes = round(max(15.0, (estimated_minutes * effective_multiplier) + actual_noise))

        records.append({
            "difficulty": difficulty,
            "estimated_minutes": estimated_minutes,
            "historical_average_minutes": historical_average,
            "previous_study_sessions": previous_sessions,
            "subject_category": category,
            "actual_minutes": actual_minutes,
        })

    return pd.DataFrame(records)


def save_dataset(output_path: str = None) -> str:
    if output_path is None:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        output_path = os.path.join(base_dir, "sample_study_data.csv")

    df = generate_synthetic_study_data(n_samples=2500)
    df.to_csv(output_path, index=False)
    print(f"Generated {len(df)} synthetic study records saved to: {output_path}")
    print("\nSummary Statistics:")
    print(df.groupby("difficulty")[["estimated_minutes", "actual_minutes"]].mean())
    return output_path


if __name__ == "__main__":
    save_dataset()

