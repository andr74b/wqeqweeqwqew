package org.example.ui;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import org.example.game.BlackjackGame;

import java.util.Locale;

@Route("ru")
@PageTitle("Блэкджек · Маленький карточный клуб")
@PreserveOnRefresh
public final class RussianBlackjackView extends BlackjackView {
    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");

    public RussianBlackjackView() {
        super(gameForCurrentUi(), RUSSIAN);
    }

    RussianBlackjackView(BlackjackGame game) {
        super(game, RUSSIAN);
    }
}
