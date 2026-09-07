package org.example.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.example.game.Card;
import org.example.game.Rank;

import java.util.Locale;

/** Conventional, accessible card faces built entirely from HTML and CSS. */
final class PlayingCard extends Div {
    PlayingCard(Card card) {
        this(card, new GameMessages(Locale.ENGLISH));
    }

    PlayingCard(Card card, GameMessages messages) {
        addClassName("playing-card");
        if (card.suit().isRed()) {
            addClassName("red-suit");
        }
        getElement().setAttribute("role", "img");
        getElement().setAttribute("aria-label", messages.cardLabel(card));

        var face = new Div();
        face.addClassName("card-face");
        face.getElement().setAttribute("aria-hidden", "true");
        face.add(corner(card, "top-corner"), center(card), corner(card, "bottom-corner"));
        add(face);
    }

    private PlayingCard(boolean placeholder, GameMessages messages) {
        addClassNames("playing-card", placeholder ? "card-placeholder" : "card-back");
        getElement().setAttribute("role", "img");
        getElement().setAttribute("aria-label", messages.text(placeholder ? "card.empty" : "card.faceDown"));
        var mark = new Span("♠");
        mark.getElement().setAttribute("aria-hidden", "true");
        add(mark);
    }

    static PlayingCard faceDown() { return faceDown(new GameMessages(Locale.ENGLISH)); }
    static PlayingCard placeholder() { return placeholder(new GameMessages(Locale.ENGLISH)); }
    static PlayingCard faceDown(GameMessages messages) { return new PlayingCard(false, messages); }
    static PlayingCard placeholder(GameMessages messages) { return new PlayingCard(true, messages); }

    private static Div corner(Card card, String position) {
        var corner = new Div(new Span(card.rank().symbol()), new Span(card.suit().symbol()));
        corner.addClassNames("card-corner", position);
        return corner;
    }

    private static Div center(Card card) {
        var center = new Div();
        center.addClassName("card-center");
        if (card.rank().isFaceCard()) {
            center.addClassName("face-value");
            center.add(new Span(card.rank().symbol()), new Span(card.suit().symbol()));
        } else if (card.rank() == Rank.ACE) {
            center.addClassName("ace-pip");
            center.add(new Span(card.suit().symbol()));
        } else {
            center.addClassNames("pips", "pips-" + card.rank().value());
            for (int i = 0; i < card.rank().value(); i++) {
                center.add(new Span(card.suit().symbol()));
            }
        }
        return center;
    }
}
