package org.example.game;

public enum Suit {
    CLUBS("♣", "clubs", false),
    DIAMONDS("♦", "diamonds", true),
    HEARTS("♥", "hearts", true),
    SPADES("♠", "spades", false);

    private final String symbol;
    private final String label;
    private final boolean red;

    Suit(String symbol, String label, boolean red) {
        this.symbol = symbol;
        this.label = label;
        this.red = red;
    }

    public String symbol() { return symbol; }
    public String label() { return label; }
    public boolean isRed() { return red; }
}
