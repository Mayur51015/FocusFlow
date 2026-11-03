"""
Dataset preparation for FocusFlow Weak Topic Classification.
Generates representative academic performance records across difficulty levels,
quiz attempts, scores, revision frequencies, and study durations.
"""

import numpy as np
import pandas as pd


def generate_weak_topic_dataset(num_samples: int = 1500, random_state: int = 42) -> pd.DataFrame:
    np.random.seed(random_state)
    records = []

    difficulties = ["EASY", "MEDIUM", "HARD"]
    difficulty_weights = [0.35, 0.40, 0.25]

    for _ in range(num_samples):
        difficulty = np.random.choice(difficulties, p=difficulty_weights)
        difficulty_val = 1 if difficulty == "EASY" else (2 if difficulty == "MEDIUM" else 3)

        # Whether student has attempted quizzes
        has_attempts = np.random.rand() > 0.10
        if has_attempts:
            attempts = np.random.randint(1, 8)
            # Base quiz score influenced by difficulty and revision
            base_score = np.random.normal(loc=65 - (difficulty_val - 2) * 12, scale=18)
            revisions = np.random.randint(0, 5)
            study_minutes = np.random.randint(20, 360) + (revisions * 30)

            # Score improvement with revisions and study
            score_boost = (revisions * 4.5) + min(study_minutes / 30.0, 10.0)
            avg_score = np.clip(base_score + score_boost, 0.0, 100.0)
            last_score = np.clip(avg_score + np.random.normal(0, 7), 0.0, 100.0)
        else:
            attempts = 0
            avg_score = 0.0
            last_score = 0.0
            revisions = np.random.choice([0, 1], p=[0.85, 0.15])
            study_minutes = np.random.randint(0, 120)

        # Ground truth classification based on multi-factor criteria
        if attempts == 0:
            if study_minutes >= 60:
                label = "MODERATE"  # Studied but unassessed
            else:
                label = "WEAK"  # Little study and unassessed
        else:
            if avg_score >= 75.0 and (last_score >= 70.0 or revisions >= 1):
                label = "STRONG"
            elif avg_score < 50.0:
                label = "WEAK"
            elif difficulty == "HARD" and avg_score < 60.0 and attempts >= 2:
                label = "WEAK"
            elif avg_score >= 65.0 and study_minutes >= 60:
                label = "STRONG" if (avg_score >= 78.0 or revisions >= 2) else "MODERATE"
            else:
                label = "MODERATE"

        records.append({
            "difficulty": difficulty,
            "difficulty_num": difficulty_val,
            "average_quiz_score": round(float(avg_score), 2),
            "attempt_count": int(attempts),
            "revision_count": int(revisions),
            "recent_study_minutes": int(study_minutes),
            "last_quiz_score": round(float(last_score), 2),
            "label": label
        })

    df = pd.DataFrame(records)
    return df


if __name__ == "__main__":
    df = generate_weak_topic_dataset(2000)
    print("Generated dataset sample:")
    print(df.head())
    print("\nClass distribution:")
    print(df["label"].value_counts(normalize=True))

