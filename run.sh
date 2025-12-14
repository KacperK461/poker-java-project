#!/bin/bash

echo "Building Poker Game..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "Build successful! Starting server..."
echo ""

# Start server in background
java -jar poker-server/target/poker-server.jar &
SERVER_PID=$!

sleep 2

echo "Starting clients..."
echo ""

# Start clients in new terminal windows
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && java -jar poker-client/target/poker-client.jar localhost 7777"'
    osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && java -jar poker-client/target/poker-client.jar localhost 7777"'
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    gnome-terminal -- bash -c "java -jar poker-client/target/poker-client.jar localhost 7777; exec bash"
    gnome-terminal -- bash -c "java -jar poker-client/target/poker-client.jar localhost 7777; exec bash"
fi

echo ""
echo "Server and clients started!"
echo "Server PID: $SERVER_PID"
echo "Press Ctrl+C to stop the server"

# Wait for Ctrl+C
trap "kill $SERVER_PID; exit" INT
wait
