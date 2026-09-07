package org.example.game;

public enum Rank {
    ACE("A", "Ace", 11),
    TWO("2", "Two", 2), THREE("3", "Three", 3), FOUR("4", "Four", 4),
    FIVE("5", "Five", 5), SIX("6", "Six", 6), SEVEN("7", "Seven", 7),
    EIGHT("8", "Eight", 8), NINE("9", "Nine", 9), TEN("10", "Ten", 10),
    JACK("J", "Jack", 10), QUEEN("Q", "Queen", 10), KING("K", "King", 10);

    private final String symbol;
    private final String label;
    private final int value;

    Rank(String symbol, String label, int value) {
        this.symbol = symbol;
        this.label = label;
        this.value = value;
    }

    public String symbol() { return symbol; }
    public String label() { return label; }
    public int value() { return value; }
    public boolean isFaceCard() { return this == JACK || this == QUEEN || this == KING; }
}
