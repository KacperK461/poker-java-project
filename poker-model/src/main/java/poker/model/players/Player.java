package poker.model.players;

import lombok.Getter;
import lombok.Setter;
import poker.common.cards.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player in the poker game.
 */
@Getter
public class Player {
    private final PlayerId id;
    private final String name;
    
    @Setter
    private int chips;
    
    @Setter
    private PlayerState state;
    
    private final List<Card> hand;
    private int currentBet;

    public Player(PlayerId id, String name, int initialChips) {
        if (id == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }
        if (initialChips < 0) {
            throw new IllegalArgumentException("Initial chips cannot be negative");
        }
        
        this.id = id;
        this.name = name;
        this.chips = initialChips;
        this.state = PlayerState.ACTIVE;
        this.hand = new ArrayList<>();
        this.currentBet = 0;
    }

    public void addCard(Card card) {
        if (hand.size() >= 5) {
            throw new IllegalStateException("Hand already has 5 cards");
        }
        hand.add(card);
    }

    public void addCards(List<Card> cards) {
        if (hand.size() + cards.size() > 5) {
            throw new IllegalStateException("Cannot add cards: would exceed hand size of 5");
        }
        hand.addAll(cards);
    }

    public void removeCards(List<Integer> indices) {
        List<Card> cardsToRemove = new ArrayList<>();
        for (int index : indices) {
            if (index < 0 || index >= hand.size()) {
                throw new IllegalArgumentException("Invalid card index: " + index);
            }
            cardsToRemove.add(hand.get(index));
        }
        hand.removeAll(cardsToRemove);
    }

    public void clearHand() {
        hand.clear();
        currentBet = 0;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public void bet(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }
        if (amount > chips) {
            throw new IllegalArgumentException("Not enough chips: have " + chips + ", need " + amount);
        }
        
        chips -= amount;
        currentBet += amount;
        
        if (chips == 0) {
            state = PlayerState.ALL_IN;
        }
    }

    public void fold() {
        state = PlayerState.FOLDED;
    }

    public void resetForNewRound() {
        currentBet = 0;
        if (state != PlayerState.SITTING_OUT) {
            state = PlayerState.ACTIVE;
        }
    }

    public boolean isActive() {
        return state == PlayerState.ACTIVE;
    }

    public boolean canAct() {
        return state == PlayerState.ACTIVE || state == PlayerState.ALL_IN;
    }

    public int getHandSize() {
        return hand.size();
    }

    public void addChips(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative chips");
        }
        chips += amount;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    @Override
    public String toString() {
        return String.format("Player{id=%s, name='%s', chips=%d, state=%s}", 
            id.getId(), name, chips, state);
    }
}
