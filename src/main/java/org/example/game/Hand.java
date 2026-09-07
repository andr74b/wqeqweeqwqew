package org.example.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Immutable hand; an ace counts as eleven only while that does not cause a bust. */
public record Hand(List<Card> cards) implements Serializable {
    public Hand {
        cards = List.copyOf(cards);
    }

    public static Hand empty() {
        return new Hand(List.of());
    }

    public Hand with(Card card) {
        var updated = new ArrayList<>(cards);
        updated.add(card);
        return new Hand(updated);
    }

    public Value value() {
        int total = 0;
        int aces = 0;
        for (var card : cards) {
            total += card.rank().value();
            if (card.rank() == Rank.ACE) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return new Value(total, aces > 0);
    }

    public int total() { return value().total(); }
    public boolean isBlackjack() { return cards.size() == 2 && total() == 21; }
    public boolean isBust() { return total() > 21; }

    public record Value(int total, boolean soft) implements Serializable { }
}
