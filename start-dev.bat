@echo off
title FocusFlow Starter
echo ====================================================
echo           Starting FocusFlow Application
echo ====================================================
echo.
echo Launching Spring Boot Backend on http://localhost:8080 ...
start "FocusFlow Backend" cmd /k "cd backend && set JAVA_HOME=C:\Program Files\Java\jdk-21&& set PATH=C:\Users\mayur\maven\apache-maven-3.9.9\bin;%JAVA_HOME%\bin;%PATH%&& mvn spring-boot:run"

echo Launching Python ML Microservice on http://localhost:8000 ...
start "FocusFlow ML Microservice" cmd /k "cd ml-service && python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload"

echo Launching Angular Frontend on http://localhost:4200 ...
start "FocusFlow Frontend" cmd /k "cd frontend && npm start"

echo.
echo ====================================================
echo Services are starting in separate windows:
echo - Frontend:      http://localhost:4200
echo - Backend:       http://localhost:8080
echo - ML Microservice: http://localhost:8000 (Swagger: /docs)
echo ====================================================
pause

