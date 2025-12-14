# Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Poker Game System                        │
└─────────────────────────────────────────────────────────────┘

┌────────────────┐                              ┌────────────────┐
│  poker-client  │◄───────── TCP/IP ───────────►│ poker-server   │
│                │        Port 7777              │                │
│  PokerClient   │                               │  PokerServer   │
│  (Console UI)  │                               │  (Game Host)   │
└────────────────┘                               └────────────────┘
        │                                                 │
        │ uses                                            │ uses
        ▼                                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                      poker-model                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Protocol   │  │  Game Logic  │  │   Players    │      │
│  │  Messages    │  │  PokerGame   │  │   Player     │      │
│  │  Parser      │  │  HandEval    │  │   PlayerId   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ uses
                              ▼
                    ┌─────────────────┐
                    │  poker-common   │
                    │  Card, Deck     │
                    │  Suit, Rank     │
                    └─────────────────┘
```

## Module Dependencies

```
poker-game (parent)
├── poker-common (no dependencies)
├── poker-model (depends on poker-common)
├── poker-server (depends on poker-common, poker-model)
└── poker-client (depends on poker-common, poker-model)
```

## Game Flow State Machine

```
     ┌──────┐
     │START │
     └───┬──┘
         │
         ▼
    ┌────────┐
    │ LOBBY  │◄───────────────┐
    └────┬───┘                │
         │ 2+ players         │
         ▼                    │
    ┌────────┐                │
    │  ANTE  │                │
    └────┬───┘                │
         │ collect ante       │
         ▼                    │
    ┌────────┐                │
    │  DEAL  │                │
    └────┬───┘                │
         │ 5 cards each       │
         ▼                    │
    ┌────────┐                │
    │  BET1  │                │
    └────┬───┘                │
         │ betting round      │
         ▼                    │
    ┌────────┐                │
    │  DRAW  │                │
    └────┬───┘                │
         │ exchange 0-3 cards │
         ▼                    │
    ┌────────┐                │
    │  BET2  │                │
    └────┬───┘                │
         │ betting round      │
         ▼                    │
    ┌──────────┐              │
    │ SHOWDOWN │              │
    └────┬─────┘              │
         │ compare hands      │
         ▼                    │
    ┌────────┐                │
    │ PAYOUT │                │
    └────┬───┘                │
         │ distribute pot     │
         ▼                    │
    ┌────────┐                │
    │  END   │────────────────┘
    └────────┘      new round
```

## Class Relationships

### poker-common
```
┌─────────┐
│  Suit   │ (enum: CLUBS, DIAMONDS, HEARTS, SPADES)
└─────────┘

┌─────────┐
│  Rank   │ (enum: TWO...ACE)
└─────────┘

┌─────────────────────┐
│  Card (record)      │
├─────────────────────┤
│ - suit: Suit        │
│ - rank: Rank        │
├─────────────────────┤
│ + compareTo()       │
│ + toString()        │
│ + fromString()      │
└─────────────────────┘

┌──────────────────────┐
│  Deck                │
├──────────────────────┤
│ - cards: List<Card>  │
│ - currentIndex: int  │
├──────────────────────┤
│ + createSorted()     │
│ + createShuffled()   │
│ + draw()             │
│ + shuffle()          │
│ + remaining()        │
└──────────────────────┘
```

### poker-model (Game)
```
┌─────────────────────────┐
│  PokerGame              │
├─────────────────────────┤
│ - gameId: GameId        │
│ - config: GameConfig    │
│ - players: Map          │
│ - state: GameState      │
│ - deck: Deck            │
│ - pot: int              │
│ - currentTurn: PlayerId │
├─────────────────────────┤
│ + addPlayer()           │
│ + startGame()           │
│ + collectAnte()         │
│ + dealInitialCards()    │
│ + check()               │
│ + call()                │
│ + raise()               │
│ + fold()                │
│ + draw()                │
│ + showdown()            │
│ + distributePot()       │
└─────────────────────────┘

┌─────────────────────┐
│  HandEvaluator      │ (interface)
│  (Strategy Pattern) │
├─────────────────────┤
│ + evaluate()        │
└─────────────────────┘
         ▲
         │
         │ implements
         │
┌───────────────────────┐
│ StandardPokerEvaluator│
├───────────────────────┤
│ + evaluate()          │
│ - checkFlush()        │
│ - checkStraight()     │
│ - findNOfAKind()      │
└───────────────────────┘
```

### poker-model (Protocol)
```
┌──────────────────────┐
│  Message (abstract)  │
├──────────────────────┤
│ - gameId: String     │
│ - playerId: String   │
│ - action: String     │
│ - params: Map        │
├──────────────────────┤
│ + parse()            │
│ + toProtocolString() │
└──────────────────────┘
         ▲
         │
    ┌────┴────┐
    │         │
┌───┴────┐ ┌──┴──────┐
│ Client │ │ Server  │
│ Message│ │ Message │
└────────┘ └─────────┘
```

### poker-server
```
┌────────────────────┐
│  PokerServer       │
├────────────────────┤
│ - port: int        │
│ - gameManager      │
│ - executor         │
├────────────────────┤
│ + start()          │
│ + shutdown()       │
└────────────────────┘
         │
         │ manages
         ▼
┌────────────────────┐
│  GameManager       │
├────────────────────┤
│ - games: Map       │
├────────────────────┤
│ + createGame()     │
│ + getGame()        │
└────────────────────┘
         │
         │ handles
         ▼
┌────────────────────┐
│  ClientHandler     │
├────────────────────┤
│ - socket: Socket   │
│ - playerId         │
│ - gameId           │
├────────────────────┤
│ + run()            │
│ + handleMessage()  │
│ + broadcast()      │
└────────────────────┘
```

## Protocol Message Flow

### Game Creation & Join
```
Client                    Server
  │                         │
  ├──HELLO VERSION=1.0─────►│
  │◄──────OK────────────────┤
  │                         │
  ├──CREATE ANTE=10 BET=20─►│
  │◄──────OK────────────────┤
  │                         │
  ├──JOIN GAME=X NAME=Alice►│
  │◄──WELCOME GAME=X PID=A──┤
  │◄──LOBBY PLAYERS=Alice───┤
  │                         │
```

### Game Start & Deal
```
Client A              Server              Client B
  │                     │                     │
  ├──START─────────────►│                     │
  │◄──STARTED───────────┤─────STARTED────────►│
  │◄──ANTE_OK───────────┤─────ANTE_OK────────►│
  │◄──DEAL AS,KH,...────┤                     │
  │                     ├─────DEAL *,*,...────►│
  │◄──TURN PID=A────────┤─────TURN PID=A─────►│
  │                     │                     │
```

### Betting Round
```
Client A              Server              Client B
  │                     │                     │
  ├──CHECK─────────────►│                     │
  │                     ├──ACTION CHECK──────►│
  │                     ├──TURN PID=B────────►│
  │◄─────TURN PID=B─────┤                     │
  │                     │◄──────BET 20────────┤
  │◄───ACTION BET 20────┤                     │
  │◄───TURN PID=A───────┤─────TURN PID=A─────►│
  │                     │                     │
```

## Virtual Threads Architecture

```
┌────────────────────────────────────────┐
│         PokerServer (Main)              │
│    ServerSocket (Port 7777)             │
└───────────────┬────────────────────────┘
                │
                │ accept() loop
                │
    ┌───────────┴───────────┐
    │                       │
    ▼                       ▼
┌─────────┐           ┌─────────┐
│ Virtual │           │ Virtual │
│ Thread  │           │ Thread  │
│   #1    │           │   #2    │
└────┬────┘           └────┬────┘
     │                     │
     ▼                     ▼
┌────────────┐      ┌────────────┐
│ Client     │      │ Client     │
│ Handler #1 │      │ Handler #2 │
└────────────┘      └────────────┘
     │                     │
     │                     │
     └─────────┬───────────┘
               │
               ▼
        ┌────────────┐
        │ PokerGame  │
        │ (shared)   │
        │ synchronized│
        └────────────┘
```

## Data Flow Example - Complete Game

```
1. LOBBY Phase:
   Client -> CREATE -> Server: Creates game
   Client -> JOIN -> Server: Adds player
   Server -> WELCOME -> Client: Confirms join
   Server -> LOBBY -> All: Updates player list

2. ANTE Phase:
   Client -> START -> Server: Begins game
   Server: Collects ante from all players
   Server -> STARTED -> All: Game started
   Server -> ANTE_OK -> All: Ante collected

3. DEAL Phase:
   Server: Creates shuffled deck
   Server: Deals 5 cards to each player
   Server -> DEAL (cards) -> Player: Your cards
   Server -> DEAL (*****) -> Others: Masked

4. BET1 Phase:
   Server -> TURN -> Current player
   Client -> CHECK/CALL/BET/FOLD -> Server
   Server -> ACTION -> All: Broadcast action
   Server: Advance to next player
   (Repeat until round complete)

5. DRAW Phase:
   Server -> TURN (DRAW) -> Current player
   Client -> DRAW 0,2 -> Server: Exchange cards
   Server: Remove cards, draw new ones
   Server -> DRAWOK -> Player: New cards
   Server -> DRAWOK (count) -> Others: Masked

6. BET2 Phase:
   (Same as BET1)

7. SHOWDOWN Phase:
   Server: Evaluates all hands
   Server -> SHOWDOWN -> All: Shows hands
   Server -> WINNER -> All: Announces winner

8. PAYOUT Phase:
   Server: Distributes pot
   Server -> PAYOUT -> All: Chip updates
   Server -> END -> All: Game complete
```

## Security Model

```
┌─────────────────────────────────────────┐
│         Security Layers                  │
└─────────────────────────────────────────┘

Layer 1: Network
├─ Connection validation
├─ Message length limits (512 bytes)
└─ Rate limiting (potential)

Layer 2: Protocol
├─ Message format validation
├─ Parameter validation
└─ Action authorization

Layer 3: Game Logic
├─ Turn validation (OutOfTurnException)
├─ State validation (StateMismatchException)
├─ Chip validation (NotEnoughChipsException)
├─ Draw validation (IllegalDrawException)
└─ Card masking (server-side only)

Layer 4: Data Integrity
├─ Authoritative server state
├─ SecureRandom shuffling
├─ Immutable game history
└─ Synchronized access
```

## Extension Points (Design for Future)

```
HandEvaluator (Strategy)
├─ StandardPokerEvaluator (current)
├─ TexasHoldemEvaluator (future)
├─ OmahaEvaluator (future)
└─ CustomVariantEvaluator (future)

GameFactory (Factory)
├─ createFiveCardDraw() (current)
├─ createTexasHoldem() (future)
└─ createOmaha() (future)

TransportLayer (Strategy)
├─ BlockingIOTransport (current)
└─ NIOTransport (bonus 1)

GameManager (Singleton)
├─ Single game (current)
└─ Multiple concurrent games (bonus 2)
```
