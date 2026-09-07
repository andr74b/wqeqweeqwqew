package org.example.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.example.game.Card;
import org.example.game.Rank;

/** Conventional, accessible card faces built entirely from HTML and CSS. */
final class PlayingCard extends Div {
    PlayingCard(Card card) {
        addClassName("playing-card");
        if (card.suit().isRed()) {
            addClassName("red-suit");
        }
        getElement().setAttribute("role", "img");
        getElement().setAttribute("aria-label", card.label());

        var face = new Div();
        face.addClassName("card-face");
        face.getElement().setAttribute("aria-hidden", "true");
        face.add(corner(card, "top-corner"), center(card), corner(card, "bottom-corner"));
        add(face);
    }

    private PlayingCard(boolean placeholder) {
        addClassNames("playing-card", placeholder ? "card-placeholder" : "card-back");
        getElement().setAttribute("role", "img");
        getElement().setAttribute("aria-label", placeholder ? "Empty card space" : "Face-down card");
        var mark = new Span("♠");
        mark.getElement().setAttribute("aria-hidden", "true");
        add(mark);
    }

    static PlayingCard faceDown() { return new PlayingCard(false); }
    static PlayingCard placeholder() { return new PlayingCard(true); }

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
