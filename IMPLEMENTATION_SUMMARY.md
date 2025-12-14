# Poker Game - Implementation Summary

## Project Overview
Complete implementation of 5-card draw poker with client-server architecture for the PZ1 course assignment.

## ✅ Completed Requirements

### 1. Multi-Module Maven Project
- ✅ `poker-common`: Common utilities (Card, Deck, Suit, Rank)
- ✅ `poker-model`: Game logic and protocol
- ✅ `poker-server`: Server with virtual threads
- ✅ `poker-client`: Console client
- ✅ Parent POM with dependency management

### 2. Card System (Points 16-20)
- ✅ `Suit` enum with symbols (♣, ♦, ♥, ♠)
- ✅ `Rank` enum with values
- ✅ `Card` record implementing Comparable
- ✅ `equals()` and `hashCode()` implemented
- ✅ Tested with HashSet
- ✅ `Deck` class with factory methods
- ✅ `shuffle()` using SecureRandom
- ✅ Card dealing functionality

### 3. Game Logic
- ✅ `PokerGame` - Main game engine
- ✅ `HandEvaluator` - Strategy pattern for hand evaluation
- ✅ `Player` - Player management
- ✅ `GameState` - State machine (LOBBY → ANTE → DEAL → BET1 → DRAW → BET2 → SHOWDOWN → PAYOUT → END)
- ✅ `GameConfig` - Configurable game settings
- ✅ Hand rankings: Royal Flush to High Card

### 4. Protocol Communication
- ✅ Human-readable protocol
- ✅ Format: `GAME_ID PLAYER_ID ACTION [PARAMS...]`
- ✅ Client commands: HELLO, CREATE, JOIN, START, BET, CALL, CHECK, FOLD, DRAW, STATUS, QUIT
- ✅ Server responses: OK, ERR, WELCOME, LOBBY, STARTED, DEAL, TURN, ACTION, SHOWDOWN, WINNER, PAYOUT, END
- ✅ Message parser with validation
- ✅ 512-byte limit enforcement

### 5. Server Implementation
- ✅ JDK 21 virtual threads: `Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())`
- ✅ TCP server on port 7777
- ✅ Multiple concurrent clients
- ✅ Game management (GameManager)
- ✅ Client handler per connection
- ✅ Broadcasting to game participants

### 6. Client Implementation
- ✅ Console-based interface
- ✅ Interactive command input
- ✅ Server message display
- ✅ Card visualization
- ✅ Help system
- ✅ Connection management

### 7. Exception Handling
- ✅ `InvalidMoveException` - Base class
- ✅ `OutOfTurnException` - Turn validation
- ✅ `NotEnoughChipsException` - Chip validation
- ✅ `IllegalDrawException` - Draw validation
- ✅ `ProtocolException` - Protocol errors
- ✅ `StateMismatchException` - State validation
- ✅ `SecurityException` - Security violations

### 8. Validation & Security
- ✅ Server-authoritative game state
- ✅ Turn validation
- ✅ Chip balance validation
- ✅ Draw limit enforcement (max 3 cards)
- ✅ Card masking (others' cards hidden)
- ✅ SecureRandom for shuffling
- ✅ Message length validation
- ✅ Out-of-turn action rejection

### 9. Testing
- ✅ Unit tests for Card system (24 tests)
- ✅ Unit tests for HandEvaluator (12 tests)
- ✅ Unit tests for Player (13 tests)
- ✅ Unit tests for PokerGame (15 tests)
- ✅ Unit tests for Protocol (11 tests)
- ✅ **Total: 75 tests, all passing**
- ✅ JaCoCo integration for coverage

### 10. Build Configuration
- ✅ Maven Shade Plugin for fat JARs
- ✅ Executable JARs with main classes
- ✅ JDK 21 compilation target
- ✅ Lombok annotation processing
- ✅ SLF4J + Logback logging

### 11. Documentation
- ✅ Comprehensive README.md
- ✅ Quick Start Guide (QUICKSTART.md)
- ✅ Protocol specification
- ✅ Code comments and JavaDoc
- ✅ Run scripts (run.bat, run.sh)

## 📊 Project Statistics

### Lines of Code
- **Common**: ~200 lines (4 classes)
- **Model**: ~1,800 lines (19 classes)
- **Server**: ~600 lines (3 classes)
- **Client**: ~350 lines (1 class)
- **Tests**: ~900 lines (5 test classes)
- **Total**: ~3,850 lines

### File Structure
```
poker/
├── pom.xml (parent)
├── README.md
├── QUICKSTART.md
├── .gitignore
├── run.bat / run.sh
├── poker-common/
│   ├── pom.xml
│   └── src/
│       ├── main/java/pl/edu/poker/common/
│       │   └── cards/ (Suit, Rank, Card, Deck)
│       └── test/java/pl/edu/poker/common/cards/
│           (CardTest, DeckTest)
├── poker-model/
│   ├── pom.xml
│   └── src/
│       ├── main/java/pl/edu/poker/model/
│       │   ├── exceptions/ (7 exception classes)
│       │   ├── game/ (GameId, GameState, GameConfig, 
│       │   │          HandRank, HandEvaluator, PokerGame)
│       │   ├── players/ (PlayerId, PlayerState, Player)
│       │   └── protocol/ (Message, ClientMessage, ServerMessage)
│       └── test/java/pl/edu/poker/model/
│           (HandEvaluatorTest, PokerGameTest, 
│            PlayerTest, MessageTest)
├── poker-server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/pl/edu/poker/server/
│       │   │   (PokerServer, ClientHandler, GameManager)
│       │   └── resources/logback.xml
│       └── target/poker-server.jar (executable)
└── poker-client/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/pl/edu/poker/client/
        │   │   (PokerClient)
        │   └── resources/logback.xml
        └── target/poker-client.jar (executable)
```

## 🎯 Key Features

### Design Patterns Used
1. **Strategy Pattern**: HandEvaluator for different poker variants
2. **Factory Pattern**: Game and player creation
3. **State Pattern**: GameState enum for game flow
4. **Builder Pattern**: GameConfig
5. **Observer Pattern**: Event broadcasting

### Java 21 Features
- Virtual threads for client connections
- Records for immutable data (Card, Payout)
- Switch expressions
- Enhanced pattern matching

### Best Practices
- Immutable design where appropriate (Card, GameId, PlayerId)
- Thread-safe operations (synchronized methods)
- Comprehensive validation
- Separation of concerns
- DRY principle
- Clean code principles

## 🚀 How to Run

### Quick Start
```bash
# Build
mvn clean package

# Run server
java -jar poker-server/target/poker-server.jar

# Run clients (in separate terminals)
java -jar poker-client/target/poker-client.jar
java -jar poker-client/target/poker-client.jar
```

### Example Game Session
```
[Terminal 1 - Alice]
> create 10 20
> join GAME123 Alice
> start
> check
> draw 0,2
> bet 20

[Terminal 2 - Bob]
> join GAME123 Bob
> call
> draw none
> call
```

## ✨ Extra Features

### Implemented
- Comprehensive error messages
- Logging system
- Card display with Unicode symbols
- Help system in client
- Status command
- Graceful shutdown
- Connection cleanup

### Ready for Extension
- Multiple game support (bonus 2)
- Different poker variants (Strategy pattern ready)
- NIO implementation (bonus 1 - architecture supports it)
- Timeouts
- Player statistics
- Game history

## 📝 Testing Coverage

All core functionality is tested:
- ✅ Card creation and comparison
- ✅ Deck shuffling and dealing
- ✅ Hand evaluation (all 10 hand types)
- ✅ Player actions (bet, call, check, fold, draw)
- ✅ Game flow (LOBBY → END)
- ✅ Protocol parsing
- ✅ Exception handling
- ✅ Validation logic

## 🔒 Security & Validation

- Server-side validation of all moves
- SecureRandom for deck shuffling
- Card masking (players see only their cards)
- Turn order enforcement
- Chip balance checking
- Draw limit enforcement
- Message length limits
- Invalid action rejection

## 📚 Technologies Used

- **Java**: 21 (virtual threads, records, switch expressions)
- **Maven**: 3.9.9 (multi-module, shade plugin)
- **Lombok**: 1.18.30 (boilerplate reduction)
- **SLF4J/Logback**: 2.0.9/1.4.14 (logging)
- **JUnit**: 5.10.1 (testing)
- **Mockito**: 5.8.0 (mocking)
- **JaCoCo**: 0.8.11 (coverage)

## ✅ Requirements Checklist

### Mandatory Requirements
- [x] 5-card draw poker implementation
- [x] Client-server architecture
- [x] TCP sockets
- [x] Human-readable protocol
- [x] 2-4 players
- [x] Server validation
- [x] JDK 21
- [x] Maven multi-module
- [x] Fat JARs (maven-shade)
- [x] Card/Deck classes
- [x] Unit tests (70%+ coverage)
- [x] Lombok usage
- [x] Exception handling
- [x] Protocol specification

### Bonus Options
- [ ] java.nio (architecture ready)
- [ ] Multiple concurrent games (architecture ready)

### Extra Credit
- [x] Comprehensive documentation
- [x] Clean code
- [x] Design patterns
- [x] Security measures
- [x] Extensive testing (75 tests)
- [x] Run scripts
- [x] Quick start guide

## 🎓 Assignment Compliance

This implementation fully satisfies all requirements from the PZ1 assignment:
- Elements 16-20: Card system ✅
- Task 2: Multi-module structure ✅
- Task 3: JDK 21 with virtual threads ✅
- Task 4: Core poker classes ✅
- Task 5: Design patterns ✅
- Task 6: Suggested package structure ✅
- Task 7: Exception package ✅
- Task 8: TDD approach ✅
- Task 9: Protocol design ✅
- Task 10: Network implementation ✅

## 📧 Contact

Project completed for PZ1 course - December 2025
