package poker.model.game;

import lombok.Getter;
import poker.common.cards.Card;
import poker.common.cards.Deck;
import poker.model.exceptions.*;
import poker.model.players.Player;
import poker.model.players.PlayerId;
import poker.model.players.PlayerState;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main game engine for 5-card draw poker.
 * Manages game state, validates moves, and orchestrates game flow.
 */
@Getter
public class PokerGame {
    private final GameId gameId;
    private final GameConfig config;
    private final Map<PlayerId, Player> players;
    private final HandEvaluator handEvaluator;
    
    private GameState state;
    private Deck deck;
    private int pot;
    private int currentBet;
    private PlayerId currentTurn;
    private PlayerId dealerId;
    private int dealerIndex;
    private List<PlayerId> turnOrder;
    private int actionsInRound;
    private boolean roundComplete;

    public PokerGame(GameId gameId, GameConfig config) {
        this.gameId = gameId;
        this.config = config;
        config.validate();
        
        this.players = new LinkedHashMap<>();
        this.handEvaluator = new HandEvaluator.StandardPokerEvaluator();
        this.state = GameState.LOBBY;
        this.pot = 0;
        this.currentBet = 0;
        this.turnOrder = new ArrayList<>();
    }

    /**
     * Add a player to the game (must be in LOBBY state).
     */
    public synchronized Player addPlayer(PlayerId playerId, String name) {
        if (state != GameState.LOBBY) {
            throw new StateMismatchException("LOBBY", state.name());
        }
        if (players.size() >= config.getMaxPlayers()) {
            throw new InvalidMoveException("GAME_FULL", "Game is full");
        }
        if (players.containsKey(playerId)) {
            throw new InvalidMoveException("ALREADY_JOINED", "Player already in game");
        }

        Player player = new Player(playerId, name, config.getInitialChips());
        players.put(playerId, player);
        
        // First player becomes dealer
        if (dealerId == null) {
            dealerId = playerId;
            dealerIndex = 0;
        }
        
        return player;
    }

    /**
     * Remove a player from the game.
     */
    public synchronized void removePlayer(PlayerId playerId) {
        if (!players.containsKey(playerId)) {
            throw new InvalidMoveException("PLAYER_NOT_FOUND", "Player not in game");
        }
        
        players.remove(playerId);
        
        // Reassign dealer if necessary
        if (playerId.equals(dealerId) && !players.isEmpty()) {
            dealerId = players.keySet().iterator().next();
        }
    }

    /**
     * Start the game (transition from LOBBY to ANTE).
     */
    public synchronized void startGame() {
        if (state != GameState.LOBBY) {
            throw new StateMismatchException("LOBBY", state.name());
        }
        if (players.size() < config.getMinPlayers()) {
            throw new InvalidMoveException("NOT_ENOUGH_PLAYERS", 
                "Need at least " + config.getMinPlayers() + " players");
        }

        // Initialize turn order
        turnOrder = new ArrayList<>(players.keySet());
        
        // Rotate to start after dealer
        while (!turnOrder.get(0).equals(dealerId)) {
            turnOrder.add(turnOrder.remove(0));
        }
        
        state = GameState.ANTE;
    }

    /**
     * Collect ante from all players.
     */
    public synchronized void collectAnte() {
        if (state != GameState.ANTE) {
            throw new StateMismatchException("ANTE", state.name());
        }

        for (Player player : players.values()) {
            if (player.getChips() < config.getAnte()) {
                player.setState(PlayerState.SITTING_OUT);
            } else {
                player.bet(config.getAnte());
                pot += config.getAnte();
            }
        }

        state = GameState.DEAL;
    }

    /**
     * Deal initial cards to all active players.
     */
    public synchronized void dealInitialCards() {
        if (state != GameState.DEAL) {
            throw new StateMismatchException("DEAL", state.name());
        }

        deck = Deck.createShuffledDeck(new SecureRandom());
        
        // Deal 5 cards to each active player
        for (Player player : players.values()) {
            if (player.isActive()) {
                player.clearHand();
                List<Card> cards = deck.draw(5);
                player.addCards(cards);
            }
        }

        state = GameState.BET1;
        startBettingRound();
    }

    /**
     * Start a betting round.
     */
    private void startBettingRound() {
        currentBet = 0;
        actionsInRound = 0;
        roundComplete = false;
        
        // Find first active player after dealer
        currentTurn = findNextActivePlayer(dealerId);
        
        // Reset player bets for this round
        for (Player player : players.values()) {
            player.resetForNewRound();
        }
    }

    /**
     * Player checks (bet is 0).
     */
    public synchronized void check(PlayerId playerId) {
        validateTurn(playerId);
        
        Player player = players.get(playerId);
        
        if (currentBet > player.getCurrentBet()) {
            throw new InvalidMoveException("CANNOT_CHECK", "Must call or fold");
        }

        advanceTurn();
    }

    /**
     * Player calls the current bet.
     */
    public synchronized void call(PlayerId playerId) {
        validateTurn(playerId);
        
        Player player = players.get(playerId);
        int callAmount = currentBet - player.getCurrentBet();
        
        if (callAmount > player.getChips()) {
            // All-in
            callAmount = player.getChips();
        }
        
        player.bet(callAmount);
        pot += callAmount;
        
        advanceTurn();
    }

    /**
     * Player raises the bet.
     */
    public synchronized void raise(PlayerId playerId, int amount) {
        validateTurn(playerId);
        
        Player player = players.get(playerId);
        int callAmount = currentBet - player.getCurrentBet();
        int totalAmount = callAmount + amount;
        
        if (totalAmount > player.getChips()) {
            throw new NotEnoughChipsException(totalAmount, player.getChips());
        }
        
        if (amount < config.getFixedBet()) {
            throw new InvalidMoveException("RAISE_TOO_SMALL", 
                "Minimum raise is " + config.getFixedBet());
        }
        
        player.bet(totalAmount);
        pot += totalAmount;
        currentBet = player.getCurrentBet();
        
        advanceTurn();
    }

    /**
     * Player folds.
     */
    public synchronized void fold(PlayerId playerId) {
        validateTurn(playerId);
        
        Player player = players.get(playerId);
        player.fold();
        
        // Check if only one player remains
        long activePlayers = players.values().stream()
            .filter(Player::isActive)
            .count();
        
        if (activePlayers == 1) {
            // Skip to showdown
            state = GameState.SHOWDOWN;
            return;
        }
        
        advanceTurn();
    }

    /**
     * Player draws cards (exchanges some cards for new ones).
     */
    public synchronized List<Card> draw(PlayerId playerId, List<Integer> cardIndices) {
        if (state != GameState.DRAW) {
            throw new StateMismatchException("DRAW", state.name());
        }
        
        if (!currentTurn.equals(playerId)) {
            throw new OutOfTurnException();
        }
        
        Player player = players.get(playerId);
        
        if (cardIndices.size() > config.getMaxDraw()) {
            throw new IllegalDrawException("Cannot draw more than " + config.getMaxDraw() + " cards");
        }
        
        // Validate indices
        Set<Integer> uniqueIndices = new HashSet<>(cardIndices);
        if (uniqueIndices.size() != cardIndices.size()) {
            throw new IllegalDrawException("Duplicate card indices");
        }
        
        for (int index : cardIndices) {
            if (index < 0 || index >= 5) {
                throw new IllegalDrawException("Invalid card index: " + index);
            }
        }
        
        if (deck.remaining() < cardIndices.size()) {
            throw new IllegalDrawException("Not enough cards in deck");
        }
        
        // Remove old cards and draw new ones
        player.removeCards(cardIndices);
        List<Card> newCards = deck.draw(cardIndices.size());
        player.addCards(newCards);
        
        advanceTurn();
        
        return newCards;
    }

    /**
     * Transition to next phase after draw.
     */
    public synchronized void startSecondBettingRound() {
        if (state != GameState.DRAW) {
            throw new StateMismatchException("DRAW", state.name());
        }
        
        state = GameState.BET2;
        startBettingRound();
    }

    /**
     * Evaluate all hands and determine winner(s).
     */
    public synchronized Map<PlayerId, HandRank> showdown() {
        if (state != GameState.BET2 && state != GameState.SHOWDOWN) {
            throw new StateMismatchException("BET2 or SHOWDOWN", state.name());
        }
        
        state = GameState.SHOWDOWN;
        
        Map<PlayerId, HandRank> rankings = new HashMap<>();
        
        for (Player player : players.values()) {
            if (player.isActive() || player.getState() == PlayerState.ALL_IN) {
                HandRank rank = handEvaluator.evaluate(player.getHand());
                rankings.put(player.getId(), rank);
            }
        }
        
        return rankings;
    }

    /**
     * Distribute pot to winner(s).
     */
    public synchronized List<Payout> distributePot(Map<PlayerId, HandRank> rankings) {
        if (state != GameState.SHOWDOWN) {
            throw new StateMismatchException("SHOWDOWN", state.name());
        }
        
        state = GameState.PAYOUT;
        
        // Find winner(s)
        HandRank bestRank = rankings.values().stream()
            .max(HandRank::compareTo)
            .orElseThrow();
        
        List<PlayerId> winners = rankings.entrySet().stream()
            .filter(e -> e.getValue().compareTo(bestRank) == 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Distribute pot
        int winnerShare = pot / winners.size();
        int remainder = pot % winners.size();
        
        List<Payout> payouts = new ArrayList<>();
        
        for (int i = 0; i < winners.size(); i++) {
            PlayerId winnerId = winners.get(i);
            Player winner = players.get(winnerId);
            
            int amount = winnerShare + (i == 0 ? remainder : 0);
            winner.addChips(amount);
            
            payouts.add(new Payout(winnerId, amount, winner.getChips()));
        }
        
        pot = 0;
        state = GameState.END;
        
        // Automatically transition to LOBBY for next round
        // Remove players with no chips
        players.entrySet().removeIf(entry -> entry.getValue().getChips() <= 0);
        
        if (players.size() >= config.getMinPlayers()) {
            // Move to lobby for next round
            state = GameState.LOBBY;
        }
        
        return payouts;
    }

    /**
     * Reset game for next round (called when starting from LOBBY after game ends).
     */
    public synchronized void resetForNextRound() {
        if (state != GameState.LOBBY) {
            throw new StateMismatchException("LOBBY", state.name());
        }
        
        // Rotate dealer button
        dealerIndex = (dealerIndex + 1) % players.size();
        dealerId = new ArrayList<>(players.keySet()).get(dealerIndex);
        
        // Note: Players with no chips are already removed in distributePot
    }

    private void validateTurn(PlayerId playerId) {
        if (!currentTurn.equals(playerId)) {
            throw new OutOfTurnException();
        }
        
        Player player = players.get(playerId);
        if (player == null) {
            throw new InvalidMoveException("PLAYER_NOT_FOUND", "Player not in game");
        }
        
        if (!player.isActive()) {
            throw new InvalidMoveException("PLAYER_NOT_ACTIVE", "Player is not active");
        }
    }

    private void advanceTurn() {
        actionsInRound++;
        
        // Check if betting round is complete
        long activePlayers = players.values().stream()
            .filter(Player::isActive)
            .count();
        
        if (actionsInRound >= activePlayers) {
            // Check if all active players have matched the current bet
            boolean allMatched = players.values().stream()
                .filter(Player::isActive)
                .allMatch(p -> p.getCurrentBet() == currentBet || p.getState() == PlayerState.ALL_IN);
            
            if (allMatched) {
                // Betting round complete
                if (state == GameState.BET1) {
                    state = GameState.DRAW;
                    actionsInRound = 0;  // Reset for draw phase
                    currentTurn = findNextActivePlayer(dealerId);
                } else if (state == GameState.BET2) {
                    state = GameState.SHOWDOWN;
                } else if (state == GameState.DRAW) {
                    // All players have drawn, move to BET2
                    startSecondBettingRound();
                }
                return;
            }
        }
        
        // Move to next active player
        currentTurn = findNextActivePlayer(currentTurn);
    }

    private PlayerId findNextActivePlayer(PlayerId afterThis) {
        int startIndex = turnOrder.indexOf(afterThis);
        
        for (int i = 1; i <= turnOrder.size(); i++) {
            int index = (startIndex + i) % turnOrder.size();
            PlayerId playerId = turnOrder.get(index);
            Player player = players.get(playerId);
            
            if (player != null && player.isActive()) {
                return playerId;
            }
        }
        
        throw new IllegalStateException("No active players found");
    }

    public Player getPlayer(PlayerId playerId) {
        return players.get(playerId);
    }

    public List<Player> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    public int getPlayerCount() {
        return players.size();
    }

    public long getActivePlayerCount() {
        return players.values().stream()
            .filter(Player::isActive)
            .count();
    }

    /**
     * Represents a payout to a player.
     */
    public record Payout(PlayerId playerId, int amount, int newStack) {
    }
}
