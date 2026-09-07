package org.example.game;

import org.example.TestDecks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.example.game.Rank.*;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackShoeTest {
    @Test
    void roundsConsumeTheSameDeckWithoutReturningDiscards() {
        var game = TestDecks.game(TEN, TEN, NINE, EIGHT, KING, QUEEN, EIGHT, NINE);
        game.newRound(25);
        game.stand();
        var first = game.snapshot();
        assertEquals(48, first.cardsRemaining());
        var seen = new HashSet<>(first.player().cards());
        seen.addAll(first.dealer().cards());
        game.newRound(25);
        game.stand();
        assertEquals(44, game.snapshot().cardsRemaining());
        assertEquals(1, game.snapshot().shoeNumber());
        assertFalse(game.snapshot().shuffledThisRound());
        game.snapshot().player().cards().forEach(card -> assertTrue(seen.add(card)));
        game.snapshot().dealer().cards().forEach(card -> assertTrue(seen.add(card)));
    }

    @Test
    void hitAndDealerDrawsReduceRemainingCountWithoutReshuffling() {
        var game = TestDecks.game(TEN, SIX, FIVE, TEN, TWO, TWO);
        game.newRound(25);
        assertEquals(48, game.snapshot().cardsRemaining()); // includes concealed hole card
        game.hit();
        assertEquals(47, game.snapshot().cardsRemaining());
        game.stand();
        assertEquals(46, game.snapshot().cardsRemaining());
        assertEquals(1, game.snapshot().shoeNumber());
    }

    @Test
    void exactlyTwentyCardsAllowsAnotherRoundAndCutShufflesOnlyBeforeFollowingDeal() {
        int[] creations = {0};
        var game = new BlackjackGame(() -> {
            creations[0]++;
            return lowPlayerHighDealerDeck();
        });
        for (int round = 0; round < 8; round++) {
            game.newRound(25);
            game.stand();
        }
        assertEquals(20, game.snapshot().cardsRemaining());
        assertFalse(game.snapshot().shuffleBeforeNextRound());
        game.newRound(25);
        assertEquals(1, creations[0]);
        assertEquals(16, game.snapshot().cardsRemaining());
        game.stand();
        assertTrue(game.snapshot().shuffleBeforeNextRound());
        assertEquals(1, game.snapshot().shoeNumber());
        var balance = game.snapshot().bankroll();
        game.newRound(25);
        assertEquals(2, creations[0]);
        assertEquals(2, game.snapshot().shoeNumber());
        assertTrue(game.snapshot().shuffledThisRound());
        assertEquals(48, game.snapshot().cardsRemaining());
        assertEquals(balance.minus(Chips.whole(25)), game.snapshot().bankroll());
        assertEquals(10, game.snapshot().round());
    }

    @Test
    void invalidBetAtCutDoesNotShuffleAndFailedReplacementDoesNotTakeStake() {
        int[] creations = {0};
        var game = new BlackjackGame(() -> ++creations[0] == 1
                ? lowPlayerHighDealerDeck() : new Deck(List.of()));
        for (int round = 0; round < 9; round++) {
            game.newRound(25);
            game.stand();
        }
        var before = game.snapshot();
        assertFalse(game.newRound(0));
        assertEquals(1, creations[0]);
        assertEquals(before, game.snapshot());
        assertThrows(IllegalArgumentException.class, () -> game.newRound(25));
        assertEquals(before, game.snapshot());
    }

    private Deck lowPlayerHighDealerDeck() {
        var lows = new ArrayList<Card>();
        var highs = new ArrayList<Card>();
        for (var suit : Suit.values()) {
            for (var rank : List.of(TWO, THREE, FOUR, FIVE)) {
                lows.add(new Card(rank, suit));
            }
            for (var rank : List.of(TEN, JACK, QUEEN, KING)) {
                highs.add(new Card(rank, suit));
            }
        }
        var ordered = new ArrayList<Card>();
        for (int i = 0; i < lows.size(); i++) {
            ordered.add(lows.get(i));
            ordered.add(highs.get(i));
        }
        return TestDecks.complete(ordered);
    }
}
