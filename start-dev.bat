@echo off
title FocusFlow Starter
echo ====================================================
echo           Starting FocusFlow Application
echo ====================================================
echo.
echo Launching Spring Boot Backend on http://localhost:8080 ...
start "FocusFlow Backend" cmd /k "cd backend && set JAVA_HOME=C:\Program Files\Java\jdk-21&& mvnw spring-boot:run"

echo Launching Angular Frontend on http://localhost:4200 ...
start "FocusFlow Frontend" cmd /k "cd frontend && npm start"

echo.
echo ====================================================
echo Both services are starting in separate windows:
echo - Frontend: http://localhost:4200
echo - Backend:  http://localhost:8080
echo ====================================================
pause
