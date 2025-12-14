@echo off
echo Building Poker Game...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo Build failed!
    exit /b %errorlevel%
)

echo.
echo Build successful! Starting server...
echo.
start "Poker Server" java -jar poker-server\target\poker-server.jar

timeout /t 2 /nobreak > nul

echo Starting clients...
echo.
start "Poker Client 1" java -jar poker-client\target\poker-client.jar localhost 7777
timeout /t 1 /nobreak > nul
start "Poker Client 2" java -jar poker-client\target\poker-client.jar localhost 7777

echo.
echo Server and clients started!
echo Check the new windows for game interaction.
