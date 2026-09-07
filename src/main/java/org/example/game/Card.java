package org.example.game;

import java.io.Serializable;
import java.util.Objects;

public record Card(Rank rank, Suit suit) implements Serializable {
    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public String label() {
        return rank.label() + " of " + suit.label();
    }
}
