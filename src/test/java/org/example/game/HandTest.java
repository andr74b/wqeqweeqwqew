package org.example.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.example.game.Rank.*;
import static org.example.game.Suit.SPADES;

class HandTest {
    @ParameterizedTest
    @CsvSource({
            "ACE KING, 21, true, true, false",
            "ACE TEN, 21, true, true, false",
            "JACK QUEEN, 20, false, false, false",
            "ACE SIX, 17, true, false, false",
            "ACE SIX TEN, 17, false, false, false",
            "ACE ACE, 12, true, false, false",
            "ACE ACE NINE, 21, true, false, false",
            "ACE ACE TEN, 12, false, false, false",
            "ACE ACE ACE EIGHT, 21, true, false, false",
            "ACE ACE ACE ACE NINE, 13, false, false, false",
            "ACE ACE ACE ACE TEN TEN, 24, false, false, true",
            "TEN FIVE SIX, 21, false, false, false",
            "KING SEVEN FIVE, 22, false, false, true",
            "TWO THREE FOUR, 9, false, false, false"
    })
    void scoresCardsAndAces(String ranks, int total, boolean soft, boolean blackjack, boolean bust) {
        var hand = new Hand(Arrays.stream(ranks.split(" "))
                .map(Rank::valueOf).map(rank -> new Card(rank, SPADES)).toList());
        assertAll(() -> assertEquals(total, hand.total()),
                () -> assertEquals(soft, hand.value().soft()),
                () -> assertEquals(blackjack, hand.isBlackjack()),
                () -> assertEquals(bust, hand.isBust()));
    }

    @Test
    void emptyHandIsNotBlackjackOrSoft() {
        var hand = Hand.empty();
        assertEquals(new Hand.Value(0, false), hand.value());
        assertFalse(hand.isBlackjack());
        assertFalse(hand.isBust());
    }

    @Test
    void handDefensivelyCopiesCardsAndAddingDoesNotMutateOldHand() {
        var cards = new ArrayList<>(List.of(new Card(ACE, SPADES)));
        var hand = new Hand(cards);
        cards.clear();
        var updated = hand.with(new Card(KING, SPADES));
        assertEquals(1, hand.cards().size());
        assertTrue(updated.isBlackjack());
        assertThrows(UnsupportedOperationException.class, () -> hand.cards().clear());
        assertThrows(NullPointerException.class, () -> hand.with(null));
    }

    @Test
    void cardRequiresRankAndSuit() {
        assertThrows(NullPointerException.class, () -> new Card(null, SPADES));
        assertThrows(NullPointerException.class, () -> new Card(ACE, null));
        assertEquals("Ace of spades", new Card(ACE, SPADES).label());
    }
}
