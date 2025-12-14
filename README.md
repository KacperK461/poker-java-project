# Poker Game - 5-Card Draw

A complete implementation of 5-card draw poker with client-server architecture, built with Java 21 and Maven.

## Features

- **Multi-module Maven project** with clean separation of concerns
- **5-card draw poker** following standard rules
- **Client-server architecture** using TCP sockets
- **JDK 21 virtual threads** for efficient concurrent connections
- **Human-readable protocol** for easy debugging and testing
- **Comprehensive validation** and security measures
- **Extensive unit tests** with 70%+ code coverage
- **Lombok** for reduced boilerplate code

## Project Structure

```
poker/
├── poker-common/      # Common utilities (Card, Deck, Suit, Rank)
├── poker-model/       # Game logic and protocol
├── poker-server/      # Server implementation
└── poker-client/      # Console client
```

## Building

Requirements:
- JDK 21 or higher
- Maven 3.9+

Build all modules:
```bash
mvn clean install
```

Build with tests:
```bash
mvn clean test
```

Build fat JARs:
```bash
mvn clean package
```

## Running

### Start the Server

```bash
java -jar poker-server/target/poker-server.jar [port]
```

Default port: 7777

### Start a Client

```bash
java -jar poker-client/target/poker-client.jar [host] [port]
```

Default: localhost:7777

## How to Play

### Creating a Game

1. Start the server
2. Connect with a client
3. Create a game:
   ```
   create 10 20
   ```
   (ante=10, bet=20)

### Joining a Game

Other players can join:
```
join GAME_ID YourName
```

### Starting the Game

The host starts when 2-4 players have joined:
```
start
```

### Playing

During your turn:
- **check** - Check (if no bet to call)
- **call** - Call the current bet
- **bet <amount>** - Raise the bet
- **fold** - Fold your hand

During draw phase:
- **draw 0,2,4** - Exchange cards at positions 0, 2, and 4
- **draw none** - Keep all cards

## Protocol Specification

Format: `GAME_ID PLAYER_ID ACTION [PARAM1=VALUE1 ...]`

### Client Commands

- `HELLO VERSION=<version>` - Initialize connection
- `CREATE ANTE=<n> BET=<n> LIMIT=FIXED` - Create game
- `JOIN GAME=<id> NAME=<name>` - Join game
- `START` - Start game (host only)
- `CHECK` - Check
- `CALL` - Call current bet
- `BET AMOUNT=<n>` - Raise bet
- `FOLD` - Fold hand
- `DRAW CARDS=<i,j,k>` - Draw cards (max 3)
- `STATUS` - Request game status
- `LEAVE` - Leave game
- `QUIT` - Disconnect

### Server Responses

- `OK [MESSAGE=...]` - Success
- `ERR CODE=<code> REASON=<text>` - Error
- `WELCOME GAME=<id> PLAYER=<id>` - Joined game
- `LOBBY PLAYERS=<list>` - Lobby status
- `STARTED DEALER=<id> ANTE=<n> BET=<n>` - Game started
- `DEAL PLAYER=<id> CARDS=<cards>` - Cards dealt
- `TURN PLAYER=<id> PHASE=<phase>` - Player's turn
- `ACTION PLAYER=<id> TYPE=<action>` - Action taken
- `SHOWDOWN PLAYER=<id> HAND=<cards> RANK=<rank>` - Show hands
- `WINNER PLAYER=<id> POT=<n> RANK=<rank>` - Winner declared
- `END REASON=<reason>` - Game ended

## Game Rules

1. **Ante Phase**: All players contribute ante
2. **Deal**: Each player receives 5 cards
3. **Betting Round 1**: First betting round
4. **Draw**: Players exchange 0-3 cards
5. **Betting Round 2**: Second betting round
6. **Showdown**: Best hand wins

### Hand Rankings (High to Low)

1. Royal Flush
2. Straight Flush
3. Four of a Kind
4. Full House
5. Flush
6. Straight
7. Three of a Kind
8. Two Pair
9. Pair
10. High Card

## Testing

Run unit tests:
```bash
mvn test
```

Generate coverage report:
```bash
mvn jacoco:report
```

Coverage reports are in `target/site/jacoco/index.html`

## Code Quality

The project uses:
- **JaCoCo** for code coverage (70%+ target)
- **JUnit 5** for unit testing
- **Mockito** for mocking
- **Lombok** for reducing boilerplate
- **SLF4J + Logback** for logging

## Architecture Highlights

### Design Patterns

- **Strategy Pattern**: `HandEvaluator` for different poker variants
- **Factory Pattern**: Game and player creation
- **State Pattern**: `GameState` enum for game flow
- **Observer Pattern**: Event broadcasting to clients

### Key Classes

- `PokerGame`: Main game engine
- `HandEvaluator`: Evaluates poker hands
- `Player`: Player state and actions
- `Deck`: Card deck with shuffling
- `ClientHandler`: Handles client connections
- `Message`: Protocol parsing and encoding

## Security Features

- **Authoritative server**: Server controls all game state
- **Card masking**: Other players' cards are hidden
- **Turn validation**: Out-of-turn actions rejected
- **Chip validation**: Prevents betting more than available
- **Rate limiting**: Protection against spam
- **Secure random**: `SecureRandom` for deck shuffling

## License

Educational project for university coursework.

## Author

Created for PZ1 course assignment (December 2025).
