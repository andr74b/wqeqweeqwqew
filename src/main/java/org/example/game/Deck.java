package org.example.game;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.random.RandomGenerator;

/** A finite deck without replacement. Ordered decks make rules tests deterministic. */
public final class Deck implements Serializable {
    public static final int SIZE = 52;
    private final ArrayDeque<Card> cards;

    public Deck(List<Card> drawOrder) {
        var copy = List.copyOf(drawOrder);
        if (new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException("A deck cannot contain duplicate cards.");
        }
        cards = new ArrayDeque<>(copy);
    }

    public static Deck shuffled() {
        return shuffled(RandomGenerator.getDefault());
    }

    public static Deck shuffled(RandomGenerator random) {
        var cards = new ArrayList<Card>(SIZE);
        for (var suit : Suit.values()) {
            for (var rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(cards, random);
        return new Deck(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards remain in this deck.");
        }
        return cards.removeFirst();
    }

    public int remaining() { return cards.size(); }
}
