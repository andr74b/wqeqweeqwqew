package org.example.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.stream.Stream;

import static org.example.game.BlackjackGame.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackGameIT {
    @Test
    void playsTwoThousandReproducibleRoundsAcrossIndependentTables() {
        for (int table = 0; table < 100; table++) {
            var shuffles = new Random(table);
            var choices = new Random(table + 1000);
            var game = new BlackjackGame(() -> Deck.shuffled(shuffles));
            var seenInDeck = new HashSet<Card>();
            int shoe = 1;
            long expectedBankroll = 2_000;
            for (int round = 1; round <= 20; round++) {
                var before = game.snapshot();
                game.newRound(25);
                int actions = 0;
                while (game.snapshot().canPlay()) {
                    assertTrue(actions++ < 20, "A finite deck must terminate the round");
                    assertEquals(1, game.snapshot().dealer().cards().size());
                    int total = game.snapshot().player().total();
                    if (total < 12 || total < 19 && choices.nextBoolean()) {
                        game.hit();
                    } else {
                        game.stand();
                    }
                }
                var state = game.snapshot();
                assertEquals(round, state.round());
                assertEquals(round, state.statistics().roundsPlayed());
                assertFalse(state.dealerCardHidden());
                var cards = Stream.concat(state.player().cards().stream(), state.dealer().cards().stream()).toList();
                assertEquals(cards.size(), new HashSet<>(cards).size(), "No card may be dealt twice in one round");
                if (state.shoeNumber() != shoe) {
                    assertTrue(before.cardsRemaining() < BlackjackGame.SHUFFLE_BELOW);
                    assertEquals(shoe + 1, state.shoeNumber());
                    shoe = state.shoeNumber();
                    seenInDeck.clear();
                }
                cards.forEach(card -> assertTrue(seenInDeck.add(card), "Discards cannot reappear before reshuffling"));
                assertEquals(52 - seenInDeck.size(), state.cardsRemaining());
                var expected = expectedOutcome(state.player(), state.dealer());
                assertEquals(expected, state.outcome());
                expectedBankroll += switch (expected) {
                    case PLAYER_BLACKJACK -> 75; // 37.5 chips
                    case PLAYER_WIN, DEALER_BUST -> 50;
                    case PLAYER_BUST, DEALER_WIN, DEALER_BLACKJACK -> -50;
                    case PUSH -> 0;
                    case NONE -> throw new AssertionError("Unfinished round");
                };
                assertEquals(expectedBankroll, state.bankroll().halfUnits());
                if (!state.player().isBust() && !state.player().isBlackjack() && !state.dealer().isBlackjack()) {
                    assertTrue(independentTotal(state.dealer()) >= 17, "Dealer must finish playing");
                }
            }
        }
    }

    private BlackjackGame.Outcome expectedOutcome(Hand player, Hand dealer) {
        int p = independentTotal(player);
        int d = independentTotal(dealer);
        boolean playerNatural = player.cards().size() == 2 && p == 21;
        boolean dealerNatural = dealer.cards().size() == 2 && d == 21;
        if (playerNatural && dealerNatural) { return PUSH; }
        if (playerNatural) { return PLAYER_BLACKJACK; }
        if (dealerNatural) { return DEALER_BLACKJACK; }
        if (p > 21) { return PLAYER_BUST; }
        if (d > 21) { return DEALER_BUST; }
        return p > d ? PLAYER_WIN : p < d ? DEALER_WIN : PUSH;
    }

    // Independent scoring oracle: start every ace at one, then promote one if possible.
    private int independentTotal(Hand hand) {
        int minimum = hand.cards().stream().mapToInt(card -> switch (card.rank()) {
            case ACE -> 1;
            case JACK, QUEEN, KING -> 10;
            default -> Integer.parseInt(card.rank().symbol());
        }).sum();
        boolean ace = hand.cards().stream().anyMatch(card -> card.rank() == Rank.ACE);
        return ace && minimum <= 11 ? minimum + 10 : minimum;
    }
}
