package org.example.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import org.example.game.Card;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.example.TestDecks.game;
import static org.example.game.Rank.*;
import static org.example.game.Suit.HEARTS;
import static org.junit.jupiter.api.Assertions.*;

/** Real Vaadin components and listeners wired to a deterministic rules engine. */
class BlackjackViewTest {
    private BlackjackView view;

    @BeforeEach
    void createUi() {
        UI.setCurrent(new UI());
        view = new BlackjackView(game(TEN, SIX, SEVEN, TEN, KING));
        UI.getCurrent().add(view);
    }

    @AfterEach
    void clearUi() { UI.setCurrent(null); }

    @Test
    void initialControlsAndStatusInviteADeal() {
        assertFalse(button("hit").isEnabled());
        assertFalse(button("stand").isEnabled());
        assertTrue(button("new-round").isEnabled());
        assertEquals("Your seat is ready.", component("status-title", H2.class).getText());
        assertEquals("0", component("wins", Span.class).getText());
        assertEquals("polite", component("round-status", Div.class).getElement().getAttribute("aria-live"));
    }

    @Test
    void dealEnablesMovesAndRendersOnlyOneDealerFaceAndVisibleScore() {
        button("new-round").click();
        assertTrue(button("hit").isEnabled());
        assertTrue(button("stand").isEnabled());
        assertFalse(button("new-round").isEnabled());
        assertEquals("17", component("player-score", Span.class).getText());
        assertEquals("6 showing", component("dealer-score", Span.class).getText());
        var dealerCards = component("dealer-cards", Div.class);
        assertEquals(2, dealerCards.getChildren().count());
        assertEquals(1, dealerCards.getChildren().filter(card -> card.hasClassName("card-back")).count());
        assertTrue(dealerCards.getElement().getOuterHTML().contains("Six of clubs"));
        assertFalse(dealerCards.getElement().getOuterHTML().contains("Ten of diamonds"));
    }

    @Test
    void standRevealsDealerRunsRulesAndUpdatesStatisticsAndControls() {
        button("new-round").click();
        button("stand").click();
        assertEquals("Dealer busts. You win!", component("status-title", H2.class).getText());
        assertEquals("26 · Bust", component("dealer-score", Span.class).getText());
        assertEquals("1", component("wins", Span.class).getText());
        assertFalse(button("hit").isEnabled());
        assertFalse(button("stand").isEnabled());
        assertTrue(button("new-round").isEnabled());
        assertEquals(0, component("dealer-cards", Div.class).getChildren()
                .filter(card -> card.hasClassName("card-back")).count());
    }

    @Test
    void hitBustsAndNewRoundResetsCardsWhileKeepingResults() {
        button("new-round").click();
        button("hit").click();
        assertEquals("Busted. The next hand awaits.", component("status-title", H2.class).getText());
        assertEquals("27 · Bust", component("player-score", Span.class).getText());
        assertEquals("1", component("losses", Span.class).getText());
        button("new-round").click();
        assertEquals("ROUND 02", component("round-number", Span.class).getText());
        assertEquals("14 · Soft", component("player-score", Span.class).getText());
        assertEquals("1", component("losses", Span.class).getText());
        assertEquals(2, component("player-cards", Div.class).getChildren().count());
    }

    @Test
    void naturalBlackjackFinishesAtTheDealAndCannotBeHit() {
        view = new BlackjackView(game(ACE, SIX, KING, TEN));
        UI.getCurrent().add(view);
        button("new-round").click();
        assertEquals("21 · Blackjack", component("player-score", Span.class).getText());
        assertEquals("Blackjack. Beautifully played.", component("status-title", H2.class).getText());
        assertFalse(button("hit").isEnabled());
        assertTrue(button("new-round").isEnabled());
    }

    @Test
    void twoViewsEvenWithinOneUiHaveIndependentGames() {
        var otherView = new BlackjackView();
        UI.getCurrent().add(otherView);
        button("new-round").click();
        button("stand").click();
        assertEquals("1", component("wins", Span.class).getText());
        view = otherView;
        assertEquals("0", component("wins", Span.class).getText());
        assertEquals("Your seat is ready.", component("status-title", H2.class).getText());
    }

    @Test
    void cardsHaveAccessibleNamesAndRedSuits() {
        var card = new PlayingCard(new Card(QUEEN, HEARTS));
        assertEquals("Queen of hearts", card.getElement().getAttribute("aria-label"));
        assertEquals("img", card.getElement().getAttribute("role"));
        assertTrue(card.hasClassName("red-suit"));
        assertEquals("Face-down card", PlayingCard.faceDown().getElement().getAttribute("aria-label"));
    }

    @Test
    void chipControlsBuildAStakeWithoutSpendingUntilTheDeal() {
        assertEquals("1,000", component("bankroll", Span.class).getText());
        button("chip-5").click();
        button("chip-25").click();
        button("chip-100").click();
        assertEquals(155, component("bet-amount", IntegerField.class).getValue());
        assertEquals("1,000", component("bankroll", Span.class).getText());
        button("new-round").click();
        assertEquals("845", component("bankroll", Span.class).getText());
        assertEquals("155", component("wager-chips", Span.class).getText());
        assertFalse(component("bet-amount", IntegerField.class).isEnabled());
        assertFalse(button("chip-5").isEnabled());
        assertEquals("DECK 1 · 48 / 52 LEFT", component("shoe-status", Span.class).getText());
    }

    @Test
    void emptyOrInvalidBetDisablesDealAndShowsValidation() {
        button("clear-bet").click();
        assertFalse(button("new-round").isEnabled());
        assertTrue(component("bet-amount", IntegerField.class).isInvalid());
        component("bet-amount", IntegerField.class).setValue(6);
        assertFalse(button("new-round").isEnabled());
        component("bet-amount", IntegerField.class).setValue(505);
        assertFalse(button("new-round").isEnabled());
        component("bet-amount", IntegerField.class).setValue(500);
        assertTrue(button("new-round").isEnabled());
        assertFalse(button("chip-5").isEnabled());
        assertEquals("1,000", component("bankroll", Span.class).getText());
    }

    @Test
    void naturalPayoutDisplaysHalfChipsAndSessionProfit() {
        view = new BlackjackView(game(ACE, SIX, KING, TEN));
        UI.getCurrent().add(view);
        component("bet-amount", IntegerField.class).setValue(5);
        button("new-round").click();
        assertEquals("1,007.5", component("bankroll", Span.class).getText());
        assertEquals("+7.5 this session", component("session-profit", Span.class).getText());
        assertTrue(component("payout-text", Span.class).getText().contains("12.5 returned"));
    }

    @Test
    void nextRoundKeepsDepletingDeckAndWinningAddsChips() {
        button("new-round").click();
        button("stand").click();
        assertEquals("1,025", component("bankroll", Span.class).getText());
        assertEquals("DECK 1 · 47 / 52 LEFT", component("shoe-status", Span.class).getText());
        button("new-round").click();
        assertEquals("DECK 1 · 43 / 52 LEFT", component("shoe-status", Span.class).getText());
    }

    @Test
    void bankruptcyReplacesDealWithExplicitNewSession() {
        view = new BlackjackView(game(TEN, TEN, SIX, EIGHT, TEN, TEN, SIX, EIGHT));
        UI.getCurrent().add(view);
        component("bet-amount", IntegerField.class).setValue(500);
        button("new-round").click();
        button("stand").click();
        button("new-round").click();
        button("stand").click();
        assertEquals("0", component("bankroll", Span.class).getText());
        assertTrue(button("new-session").isVisible());
        assertFalse(button("new-round").isVisible());
        assertFalse(component("bet-amount", IntegerField.class).isEnabled());
        button("new-session").click();
        assertEquals("1,000", component("bankroll", Span.class).getText());
        assertEquals("DECK 1 · 52 / 52 LEFT", component("shoe-status", Span.class).getText());
        assertEquals(25, component("bet-amount", IntegerField.class).getValue());
        assertTrue(button("new-round").isEnabled());
        assertFalse(button("new-session").isVisible());
    }

    private Button button(String id) { return component(id, Button.class); }

    private <T extends Component> T component(String id, Class<T> type) {
        return descendants(view).filter(component -> component.getId().filter(id::equals).isPresent())
                .map(type::cast).findFirst().orElseThrow();
    }

    private Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::descendants));
    }
}
