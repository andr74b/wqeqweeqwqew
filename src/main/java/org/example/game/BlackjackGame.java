package org.example.game;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Single-player, single-deck blackjack. The dealer stands on all 17s.
 * Owned by one Vaadin view, whose session lock serializes commands.
 * Virtual chips use exact half-chip units. Cards persist across rounds until the cut.
 */
public final class BlackjackGame implements Serializable {
    public static final int STARTING_CHIPS = 1_000;
    public static final int MIN_BET = 5;
    public static final int MAX_BET = 500;
    public static final int BET_STEP = 5;
    // Enough reserve for both hands of this single-deck, single-player ruleset.
    // Shuffle only BETWEEN rounds, never replace cards partway through a hand.
    public static final int SHUFFLE_BELOW = 20;

    private final DeckFactory deckFactory;
    private Deck deck;
    private Hand player = Hand.empty();
    private Hand dealer = Hand.empty();
    private Phase phase = Phase.READY;
    private Outcome outcome = Outcome.NONE;
    private int round;
    private Statistics statistics = new Statistics(0, 0, 0);
    private Chips bankroll = Chips.whole(STARTING_CHIPS);
    private int wager;
    private Chips payout = Chips.ZERO;
    private int shoeNumber = 1;
    private boolean shuffledThisRound;

    public BlackjackGame() {
        this(Deck::shuffled);
    }

    public BlackjackGame(DeckFactory deckFactory) {
        this.deckFactory = Objects.requireNonNull(deckFactory, "deckFactory");
        deck = freshDeck();
    }

    public BetValidation validateBet(int bet) {
        if (phase == Phase.PLAYER_TURN) {
            return BetValidation.ROUND_IN_PROGRESS;
        }
        if (bet < MIN_BET || bet > MAX_BET || bet % BET_STEP != 0) {
            return BetValidation.INVALID_AMOUNT;
        }
        return bankroll.halfUnits() < bet * 2L ? BetValidation.INSUFFICIENT_CHIPS : BetValidation.VALID;
    }

    /** Deduct the stake exactly once, before any card is dealt. Rejected commands change nothing. */
    public boolean newRound(int bet) {
        if (validateBet(bet) != BetValidation.VALID) {
            return false;
        }
        boolean shuffle = deck.remaining() < SHUFFLE_BELOW;
        if (shuffle) {
            // Validate the replacement before changing bankroll or the existing round.
            deck = freshDeck();
            shoeNumber++;
        }
        shuffledThisRound = shuffle;
        wager = bet;
        bankroll = bankroll.minus(Chips.whole(wager));
        payout = Chips.ZERO;
        player = Hand.empty();
        dealer = Hand.empty();
        outcome = Outcome.NONE;
        phase = Phase.PLAYER_TURN;
        round++;

        // Deal clockwise: player, dealer upcard, player, dealer hole card.
        player = player.with(deck.draw());
        dealer = dealer.with(deck.draw());
        player = player.with(deck.draw());
        dealer = dealer.with(deck.draw());

        if (player.isBlackjack() && dealer.isBlackjack()) {
            finish(Outcome.PUSH);
        } else if (player.isBlackjack()) {
            finish(Outcome.PLAYER_BLACKJACK);
        } else if (dealer.isBlackjack()) {
            finish(Outcome.DEALER_BLACKJACK);
        }
        return true;
    }

    private Deck freshDeck() {
        var fresh = Objects.requireNonNull(deckFactory.create(), "deck");
        if (fresh.remaining() != Deck.SIZE) {
            throw new IllegalArgumentException("A fresh deck must contain all 52 unique cards.");
        }
        return fresh;
    }

    /** An explicit new session is allowed only after the bankroll falls below the table minimum. */
    public boolean restartSession() {
        if (!snapshot().outOfChips()) {
            return false;
        }
        var fresh = freshDeck();
        deck = fresh;
        shoeNumber = 1;
        shuffledThisRound = false;
        bankroll = Chips.whole(STARTING_CHIPS);
        wager = 0;
        payout = Chips.ZERO;
        phase = Phase.READY;
        outcome = Outcome.NONE;
        player = Hand.empty();
        dealer = Hand.empty();
        statistics = new Statistics(0, 0, 0);
        round = 0;
        return true;
    }

    public void hit() {
        if (phase != Phase.PLAYER_TURN) {
            return;
        }
        player = player.with(deck.draw());
        if (player.isBust()) {
            finish(Outcome.PLAYER_BUST);
        } else if (player.total() == 21) {
            stand();
        }
    }

    public void stand() {
        if (phase != Phase.PLAYER_TURN) {
            return;
        }
        while (dealer.total() < 17) {
            dealer = dealer.with(deck.draw());
        }
        finish(dealer.isBust() ? Outcome.DEALER_BUST
                : player.total() > dealer.total() ? Outcome.PLAYER_WIN
                : player.total() < dealer.total() ? Outcome.DEALER_WIN
                : Outcome.PUSH);
    }

    private void finish(Outcome result) {
        payout = switch (result) {
            case PLAYER_BLACKJACK -> new Chips(wager * 5L); // stake + 3:2 profit, in half-chips
            case DEALER_BUST, PLAYER_WIN -> Chips.whole(wager * 2L);
            case PUSH -> Chips.whole(wager);
            case DEALER_BLACKJACK, PLAYER_BUST, DEALER_WIN -> Chips.ZERO;
            case NONE -> throw new IllegalArgumentException("A finished round needs an outcome.");
        };
        bankroll = bankroll.plus(payout);
        outcome = result;
        phase = Phase.ROUND_OVER;
        statistics = switch (result) {
            case PLAYER_BLACKJACK, DEALER_BUST, PLAYER_WIN ->
                    new Statistics(statistics.wins() + 1, statistics.losses(), statistics.pushes());
            case DEALER_BLACKJACK, PLAYER_BUST, DEALER_WIN ->
                    new Statistics(statistics.wins(), statistics.losses() + 1, statistics.pushes());
            case PUSH -> new Statistics(statistics.wins(), statistics.losses(), statistics.pushes() + 1);
            case NONE -> throw new IllegalArgumentException("A finished round needs an outcome.");
        };
    }

    /** Never exposes the hole card or its contribution to the score during the player's turn. */
    public Snapshot snapshot() {
        boolean hidden = phase == Phase.PLAYER_TURN;
        var visibleDealer = hidden ? new Hand(List.of(dealer.cards().getFirst())) : dealer;
        return new Snapshot(phase, outcome, player, visibleDealer, hidden, round, statistics,
                bankroll, wager, payout, deck.remaining(), shoeNumber, shuffledThisRound);
    }

    @FunctionalInterface
    public interface DeckFactory extends Serializable {
        Deck create();
    }

    public enum Phase { READY, PLAYER_TURN, ROUND_OVER }

    public enum BetValidation { VALID, ROUND_IN_PROGRESS, INVALID_AMOUNT, INSUFFICIENT_CHIPS }

    public enum Outcome {
        NONE, PLAYER_BLACKJACK, DEALER_BLACKJACK, PLAYER_BUST, DEALER_BUST,
        PLAYER_WIN, DEALER_WIN, PUSH
    }

    public record Statistics(int wins, int losses, int pushes) implements Serializable {
        public int roundsPlayed() { return wins + losses + pushes; }
    }

    public record Snapshot(Phase phase, Outcome outcome, Hand player, Hand dealer,
                           boolean dealerCardHidden, int round, Statistics statistics,
                           Chips bankroll, int wager, Chips payout, int cardsRemaining,
                           int shoeNumber, boolean shuffledThisRound)
            implements Serializable {
        public boolean canPlay() { return phase == Phase.PLAYER_TURN; }
        public boolean outOfChips() { return !canPlay() && bankroll.halfUnits() < MIN_BET * 2L; }
        public int maximumBet() {
            return (int) Math.min(MAX_BET, bankroll.halfUnits() / (2 * BET_STEP) * BET_STEP);
        }
        public boolean shuffleBeforeNextRound() { return !canPlay() && cardsRemaining < SHUFFLE_BELOW; }
        public long netHalfChips() { return phase == Phase.ROUND_OVER ? payout.halfUnits() - wager * 2L : 0; }
        public long sessionProfitHalfChips() {
            return bankroll.halfUnits() + (canPlay() ? wager * 2L : 0) - STARTING_CHIPS * 2L;
        }
    }
}
