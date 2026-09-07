package org.example.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.example.TestDecks.game;
import static org.example.game.Rank.*;
import static org.junit.jupiter.api.Assertions.*;

class SpanishBlackjackViewTest {
    private SpanishBlackjackView view;
    private UI ui;

    @BeforeEach
    void createUi() {
        ui = new UI();
        UI.setCurrent(ui);
        view = new SpanishBlackjackView(game(TEN, SIX, SEVEN, TEN, KING));
        UI.getCurrent().add(view);
    }

    @AfterEach
    void clearUi() {
        UI.setCurrent(null);
        ui = null;
    }

    @Test
    void spanishRouteLocalizesTheWholeGameAndLanguageSwitcher() {
        assertEquals("Haz tu apuesta", component("status-title", H2.class).getText());
        assertEquals("Repartir", button("new-round").getText());
        assertEquals("true", button("language-es").getElement().getAttribute("aria-pressed"));
        assertEquals("false", button("language-en").getElement().getAttribute("aria-pressed"));
        assertEquals("false", button("language-ru").getElement().getAttribute("aria-pressed"));

        button("new-round").click();

        assertEquals("Mano 01", component("round-number", Span.class).getText());
        assertEquals("6 + ?", component("dealer-score", Span.class).getText());
        assertTrue(component("dealer-cards", Div.class).getElement().getOuterHTML().contains("Seis de tréboles"));
    }

    @Test
    void spanishBundleCannotDriftFromTheEnglishMessageContract() {
        var english = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        var spanish = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag("es"));
        assertEquals(english.keySet(), spanish.keySet());
    }

    private Button button(String id) {
        return component(id, Button.class);
    }

    private <T extends Component> T component(String id, Class<T> type) {
        return descendants(view)
                .filter(component -> component.getId().filter(id::equals).isPresent())
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    private Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::descendants));
    }
}
