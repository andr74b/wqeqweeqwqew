package org.example;

import org.example.game.BlackjackGame;
import org.example.game.Card;
import org.example.game.Deck;
import org.example.game.Rank;
import org.example.game.Suit;

import java.util.ArrayList;
import java.util.EnumMap;

public final class TestDecks {
    private TestDecks() { }

    /** Assigns distinct suits to repeated ranks; arguments are in draw order. */
    public static Deck ordered(Rank... ranks) {
        var counts = new EnumMap<Rank, Integer>(Rank.class);
        var cards = new ArrayList<Card>();
        for (var rank : ranks) {
            int occurrence = counts.merge(rank, 1, Integer::sum) - 1;
            cards.add(new Card(rank, Suit.values()[occurrence]));
        }
        return new Deck(cards);
    }

    public static BlackjackGame game(Rank... ranks) {
        return new BlackjackGame(() -> fullOrdered(ranks));
    }

    public static Deck fullOrdered(Rank... ranks) {
        var prefix = ordered(ranks);
        var cards = new ArrayList<Card>();
        while (prefix.remaining() > 0) {
            cards.add(prefix.draw());
        }
        return complete(cards);
    }

    public static Deck complete(java.util.List<Card> prefix) {
        var cards = new ArrayList<>(prefix);
        for (var suit : Suit.values()) {
            for (var rank : Rank.values()) {
                var card = new Card(rank, suit);
                if (!cards.contains(card)) {
                    cards.add(card);
                }
            }
        }
        return new Deck(cards);
    }
}
