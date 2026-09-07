package org.example.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.example.game.Rank.*;
import static org.example.game.Suit.*;
import static org.junit.jupiter.api.Assertions.*;

class DeckTest {
    @Test
    void shuffledDeckContainsEveryCardExactlyOnceAndNeverRecycles() {
        var deck = Deck.shuffled(new Random(42));
        assertEquals(52, deck.remaining());
        var seen = new HashSet<Card>();
        while (deck.remaining() > 0) {
            assertTrue(seen.add(deck.draw()), "Duplicate card");
        }
        for (var suit : Suit.values()) {
            for (var rank : Rank.values()) {
                assertTrue(seen.contains(new Card(rank, suit)));
            }
        }
        assertThrows(IllegalStateException.class, deck::draw);
    }

    @Test
    void seededShuffleIsReproducibleAndDifferentSeedsChangeTheOrder() {
        var first = drain(Deck.shuffled(new Random(42)));
        assertEquals(first, drain(Deck.shuffled(new Random(42))));
        assertNotEquals(first, drain(Deck.shuffled(new Random(43))));
    }

    @Test
    void orderedDeckDefensivelyCopiesItsInputAndRejectsDuplicates() {
        var ace = new Card(ACE, SPADES);
        var king = new Card(KING, HEARTS);
        var input = new ArrayList<>(List.of(ace, king));
        var deck = new Deck(input);
        input.clear();
        assertEquals(ace, deck.draw());
        assertEquals(king, deck.draw());
        assertThrows(IllegalArgumentException.class, () -> new Deck(List.of(ace, ace)));
    }

    private List<Card> drain(Deck deck) {
        var result = new ArrayList<Card>();
        while (deck.remaining() > 0) {
            result.add(deck.draw());
        }
        return result;
    }
}
