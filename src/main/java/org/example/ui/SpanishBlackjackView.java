package org.example.ui;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import org.example.game.BlackjackGame;

import java.util.Locale;

@Route("es")
@PageTitle("Blackjack · El pequeño club de cartas")
@PreserveOnRefresh
public final class SpanishBlackjackView extends BlackjackView {
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    public SpanishBlackjackView() {
        super(gameForCurrentUi(), SPANISH);
    }

    SpanishBlackjackView(BlackjackGame game) {
        super(game, SPANISH);
    }
}
