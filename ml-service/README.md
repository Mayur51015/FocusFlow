# FocusFlow ML Microservice – Study Duration Predictor

The **FocusFlow ML Microservice** is an independently deployable Python FastAPI service that predicts realistic topic study durations using a Scikit-Learn **Random Forest Regressor** with automated heuristic fallback.

---

## 1. Architecture & Directory Layout

```
ml-service/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI application entrypoint & CORS setup
│   ├── routes/
│   │   ├── __init__.py
│   │   └── prediction.py        # REST endpoints (/ml/predict-duration, /health)
│   ├── models/
│   │   ├── __init__.py
│   │   └── duration_model.py    # Model pipeline wrapper & heuristic fallback
│   ├── schemas/
│   │   ├── __init__.py
│   │   └── prediction_schema.py # Pydantic request/response validation schemas
│   ├── services/
│   │   ├── __init__.py
│   │   └── prediction_service.py# Lifecycle, prediction & health manager
│   └── utils/
│       ├── __init__.py
│       └── preprocessing.py     # Feature featurization & safety clamping
├── training/
│   ├── __init__.py
│   ├── generate_sample_data.py  # Calibrated synthetic data generator
│   ├── train_model.py           # Preprocessing pipeline & Random Forest trainer
│   └── evaluate_model.py        # Slice evaluation (MAE, RMSE, R²) across difficulties
├── artifacts/
│   ├── duration_model.joblib    # Serialized Scikit-Learn Pipeline
│   └── model_metadata.json      # Version, feature names, and test metrics
├── requirements.txt
└── README.md
```

---

## 2. Machine Learning Model

- **Algorithm**: `RandomForestRegressor(n_estimators=100, max_depth=8, min_samples_split=5)`
- **Preprocessing**: `ColumnTransformer` with `OneHotEncoder` (categorical) + `StandardScaler` (numerical).
- **Inputs**:
  - `topicDifficulty`: `EASY`, `MEDIUM`, or `HARD`
  - `estimatedMinutes`: Planned baseline minutes ($1 \le m \le 720$)
  - `historicalAverageMinutes`: Student's historical average minutes spent on similar topics
  - `previousStudySessions`: Number of completed sessions on this topic ($0 \le s \le 100$)
  - `subjectCategory`: Optional category (`STEM`, `HUMANITIES`, `COMMERCE`, `GENERAL`)
- **Outputs**:
  - `predictedMinutes`: Bounded, integer-rounded duration
  - `confidenceScore`: Uncertainty metric ($0.50$ to $0.98$)
  - `estimatedErrorRangeMinutes`: Tree-level standard deviation margin ($\pm$ minutes)
  - `modelVersion`: Active model version (`"1.0"` or `"fallback"`)
  - `predictionSource`: `"EXPERIMENTAL_ML"` or `"HEURISTIC_FALLBACK"`

> **Empirical Data Notice**: Initial model weights are trained on calibrated synthetic data designed to model student pacing (HARD topics generally take $+15\text{--}40\%$ longer; EASY topics take $10\text{--}15\%$ less; learning curve reduces time after 3+ sessions). As real students log sessions in FocusFlow, the dataset expands to allow continuous offline retraining.

---

## 3. How to Run the Service

From the project root:

```powershell
cd ml-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive API documentation is automatically accessible at:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

---

## 4. API Endpoints

### Health Check
- **`GET /health`** or **`GET /ml/health`**
- **Response**:
```json
{
  "status": "UP",
  "modelLoaded": true,
  "modelVersion": "1.0",
  "predictionSource": "EXPERIMENTAL_ML",
  "uptimeSeconds": 42.15
}
```

### Predict Study Duration
- **`POST /ml/predict-duration`**
- **Request**:
```json
{
  "topicDifficulty": "HARD",
  "estimatedMinutes": 60,
  "historicalAverageMinutes": 75.0,
  "previousStudySessions": 2,
  "subjectCategory": "STEM"
}
```
- **Response**:
```json
{
  "predictedMinutes": 82,
  "confidenceScore": 0.88,
  "estimatedErrorRangeMinutes": 9,
  "modelVersion": "1.0",
  "predictionSource": "EXPERIMENTAL_ML",
  "details": null
}
```

---

## 5. Automated Heuristic Fallback

If the model artifact (`duration_model.joblib`) is missing, corrupt, or fails during inference:
- The service will **not crash or throw 500 errors**.
- It immediately engages the heuristic fallback model:
  - Multipliers: EASY $\times 0.90$, MEDIUM $\times 1.05$, HARD $\times 1.30$.
  - 50/50 blending with historical averages when available.
  - Returns `"predictionSource": "HEURISTIC_FALLBACK"` and `"modelVersion": "fallback"`.

