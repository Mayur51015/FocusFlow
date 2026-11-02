import os
import sys
import logging
from contextlib import asynccontextmanager

# Ensure ml-service root directory is in sys.path for direct execution
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes.prediction import router as prediction_router
from app.services.prediction_service import prediction_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("focusflow.ml")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting FocusFlow ML Microservice...")
    health = prediction_service.get_health()
    logger.info(
        f"Model status: modelLoaded={health.modelLoaded}, "
        f"version={health.modelVersion}, source={health.predictionSource}"
    )
    yield
    # Shutdown
    logger.info("Shutting down FocusFlow ML Microservice.")


app = FastAPI(
    title="FocusFlow ML Duration Prediction Microservice",
    description=(
        "Microservice for predicting realistic academic study durations using Machine Learning "
        "(Scikit-Learn Random Forest Regressor) with heuristic fallback."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS setup for Spring Boot and Angular communication
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:4200", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(prediction_router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
