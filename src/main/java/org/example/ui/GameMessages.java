package org.example.ui;

import org.example.game.Card;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/** Locale-backed UI copy kept separate from game rules and view composition. */
final class GameMessages {
    private final Locale locale;
    private final ResourceBundle bundle;

    GameMessages(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    String text(String key, Object... arguments) {
        return new MessageFormat(bundle.getString(key), locale).format(arguments);
    }

    String cardLabel(Card card) {
        return text("card.label",
                text("rank." + card.rank().name().toLowerCase(Locale.ROOT)),
                text("suit." + card.suit().name().toLowerCase(Locale.ROOT)));
    }
}
