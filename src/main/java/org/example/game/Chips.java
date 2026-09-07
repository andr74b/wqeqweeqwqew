package org.example.game;

import java.io.Serializable;

/** Exact half-chip accounting: a 5-chip blackjack wins 7.5 chips, without rounding. */
public record Chips(long halfUnits) implements Serializable {
    public static final Chips ZERO = new Chips(0);

    public Chips {
        if (halfUnits < 0) {
            throw new IllegalArgumentException("Chips cannot be negative.");
        }
    }

    public static Chips whole(long amount) {
        return new Chips(Math.multiplyExact(amount, 2));
    }

    public Chips plus(Chips amount) {
        return new Chips(Math.addExact(halfUnits, amount.halfUnits));
    }

    public Chips minus(Chips amount) {
        return new Chips(Math.subtractExact(halfUnits, amount.halfUnits));
    }
}
