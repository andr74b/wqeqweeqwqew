package org.example.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.FocusOption;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PreserveOnRefresh;
import org.example.game.BlackjackGame;
import org.example.game.BlackjackGame.Phase;
import org.example.game.Hand;

import java.util.Objects;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Route("")
@PageTitle("Blackjack · The Little Card Club")
@PreserveOnRefresh
public final class BlackjackView extends Div {
    // A route instance owns its game. Never make this a static field or singleton bean.
    private final BlackjackGame game;
    private final Div dealerCards = box("cards");
    private final Div playerCards = box("cards");
    private final Span dealerScore = text("hand-score", "—");
    private final Span playerScore = text("hand-score", "—");
    private final Span roundNumber = text("round-number", "THE TABLE IS YOURS");
    private final Span turn = text("turn-label", "READY TO PLAY");
    private final Span wins = new Span("0");
    private final Span losses = new Span("0");
    private final Span pushes = new Span("0");
    private final Span bankroll = text("bankroll-amount", "1,000");
    private final Span sessionProfit = text("session-profit", "Even this session");
    private final Span shoeStatus = text("shoe-status", "DECK 1 · 52 / 52 LEFT");
    private final Span shoeNote = text("shoe-note", "Cards carry over between rounds.");
    private final Span wagerChips = text("wager-chip", "25");
    private final Span wagerLabel = text("wager-label", "SELECTED BET");
    private final Span payoutText = text("payout-text", "");
    private final IntegerField betAmount = new IntegerField("Your bet");
    private final Button chip5 = chipButton(5);
    private final Button chip25 = chipButton(25);
    private final Button chip100 = chipButton(100);
    private final Button clearBet = new Button("Clear", event -> betAmount.clear());
    private final Button restart = new Button("New Session");
    private final H2 statusTitle = new H2();
    private final Paragraph statusDetail = new Paragraph();
    private final Div status = box("round-status", statusTitle, statusDetail);
    private final Button hit = new Button("Hit");
    private final Button stand = new Button("Stand");
    private final Button newRound = new Button("New Round");

    public BlackjackView() {
        this(new BlackjackGame());
    }

    // Package-private injection keeps component tests independent of randomness.
    BlackjackView(BlackjackGame game) {
        this.game = Objects.requireNonNull(game, "game");
        hit.addClickListener(event -> act(game::hit));
        stand.addClickListener(event -> act(game::stand));
        newRound.addClickListener(event -> act(() -> game.newRound(selectedBet())));
        restart.addClickListener(event -> {
            if (game.restartSession()) {
                betAmount.setValue(25);
                render();
                newRound.focus(FocusOption.PreventScroll.ENABLED);
            }
        });
        betAmount.setValue(25);
        addClassName("blackjack-app");
        var main = new Main(introduction(), table(), rules());
        main.addClassName("content");
        add(header(), main, footer());
        render();
    }

    private Header header() {
        var mark = text("brand-mark", "♠");
        mark.getElement().setAttribute("aria-hidden", "true");
        var header = new Header(box("brand", mark, new Span("the little card club")),
                text("header-note", "A GOOD HAND. A LITTLE LUCK."));
        header.addClassName("site-header");
        return header;
    }

    private Section introduction() {
        var title = new H1("Blackjack");
        title.setId("game-title");
        var copy = box("intro-copy", text("eyebrow", "TAKE A SEAT. PLAY A HAND."), title,
                new Paragraph("Get closer to 21 than the dealer. Know when to stay."));
        var stats = box("session-stats", statistic("Wins", wins, "wins"),
                statistic("Losses", losses, "losses"), statistic("Pushes", pushes, "pushes"));
        stats.getElement().setAttribute("role", "group");
        stats.getElement().setAttribute("aria-label", "Results in this tab");
        bankroll.setId("bankroll");
        sessionProfit.setId("session-profit");
        var bank = box("bankroll-line", text("bank-chip", "♠"), bankroll, text("bankroll-unit", "chips"));
        var intro = new Section(copy, box("scoreboard", text("eyebrow", "YOUR BANKROLL"), bank, sessionProfit, stats));
        intro.addClassName("introduction");
        intro.getElement().setAttribute("aria-labelledby", "game-title");
        return intro;
    }

    private Div statistic(String label, Span value, String id) {
        value.setId(id);
        return box("statistic", value, text("statistic-label", label));
    }

    private Section table() {
        dealerCards.setId("dealer-cards");
        playerCards.setId("player-cards");
        dealerScore.setId("dealer-score");
        playerScore.setId("player-score");
        roundNumber.setId("round-number");
        turn.setId("turn-label");
        shoeStatus.setId("shoe-status");
        shoeNote.setId("shoe-note");
        wagerChips.setId("wager-chips");
        payoutText.setId("payout-text");

        var dealer = handArea("Dealer", "One card stays hidden until your turn ends.", dealerScore, dealerCards);
        var player = handArea("Your hand", "Aces count as 1 or 11.", playerScore, playerCards);
        var divider = box("table-divider", text("table-inscription", "♣  ♦  ♠  ♥"),
                text("table-rule", "BLACKJACK PAYS 3:2 · DEALER STANDS ON ALL 17s"));
        divider.getElement().setAttribute("aria-hidden", "true");
        var felt = box("felt", box("table-topline", roundNumber, shoeStatus, turn), dealer, divider, player,
                box("wager-spot", wagerChips, wagerLabel), shoeNote);

        configureButton(hit, "hit", "hit-button", "Take one more card");
        configureButton(stand, "stand", "stand-button", "Keep your hand and let the dealer play");
        configureButton(newRound, "new-round", "new-round-button", "Deal a fresh round");
        configureButton(restart, "new-session", "new-round-button", "Start over with 1,000 virtual chips");
        status.setId("round-status");
        status.getElement().setAttribute("role", "status");
        status.getElement().setAttribute("aria-live", "polite");
        status.getElement().setAttribute("aria-atomic", "true");
        statusTitle.setId("status-title");
        statusDetail.setId("status-detail");

        status.add(payoutText);
        var controls = box("controls", status, box("actions", hit, stand, newRound, restart));
        var table = new Section(felt, bettingBar(), controls);
        table.addClassName("game-table");
        table.getElement().setAttribute("aria-label", "Blackjack table");
        return table;
    }

    private Div bettingBar() {
        betAmount.setId("bet-amount");
        betAmount.setMin(BlackjackGame.MIN_BET);
        betAmount.setMax(BlackjackGame.MAX_BET);
        betAmount.setStep(BlackjackGame.BET_STEP);
        betAmount.setStepButtonsVisible(true);
        betAmount.setValueChangeMode(ValueChangeMode.EAGER);
        betAmount.setManualValidation(true);
        betAmount.setHelperText("5–500 chips · increments of 5");
        betAmount.addValueChangeListener(event -> updateBetting(game.snapshot()));
        clearBet.setId("clear-bet");
        clearBet.addClassName("clear-bet");
        var rack = box("chip-rack", chip5, chip25, chip100, clearBet);
        rack.getElement().setAttribute("role", "group");
        rack.getElement().setAttribute("aria-label", "Add chips to your bet");
        return box("betting-bar", betAmount, box("chip-selection", text("betting-label", "CLICK A CHIP TO ADD TO YOUR BET"), rack));
    }

    private Button chipButton(int amount) {
        var button = new Button(Integer.toString(amount), event -> {
            long next = Math.max(0, selectedBet()) + (long) amount;
            if (!game.snapshot().canPlay() && next <= game.snapshot().maximumBet()) {
                betAmount.setValue((int) next);
            }
        });
        button.setId("chip-" + amount);
        button.addClassNames("bet-chip", "chip-" + amount);
        button.getElement().setAttribute("aria-label", "Add " + amount + " chips");
        return button;
    }

    private int selectedBet() {
        return betAmount.getValue() == null ? 0 : betAmount.getValue();
    }

    private void updateBetting(BlackjackGame.Snapshot state) {
        boolean editable = !state.canPlay() && !state.outOfChips();
        betAmount.setEnabled(editable);
        betAmount.setMax(Math.max(BlackjackGame.MIN_BET, state.maximumBet()));
        var validation = game.validateBet(selectedBet());
        betAmount.setInvalid(editable && validation != BlackjackGame.BetValidation.VALID);
        betAmount.setErrorMessage(validation == BlackjackGame.BetValidation.INSUFFICIENT_CHIPS
                ? "Your bet exceeds your bankroll."
                : "Enter 5–500 chips in increments of 5.");
        long base = Math.max(0, selectedBet());
        chip5.setEnabled(editable && base + 5 <= state.maximumBet());
        chip25.setEnabled(editable && base + 25 <= state.maximumBet());
        chip100.setEnabled(editable && base + 100 <= state.maximumBet());
        clearBet.setEnabled(editable && selectedBet() != 0);
        newRound.setEnabled(validation == BlackjackGame.BetValidation.VALID);
        restart.setVisible(state.outOfChips());
        newRound.setVisible(!state.outOfChips());
        wagerChips.setText(formatChips((state.phase() == Phase.READY ? Math.max(0, selectedBet()) : state.wager()) * 2L));
        wagerLabel.setText(state.phase() == Phase.READY ? "SELECTED BET" : state.canPlay() ? "ON THE TABLE" : "LAST BET");
        wagerChips.getElement().setAttribute("aria-label", (state.phase() == Phase.READY ? "Selected bet: " : "Round bet: ")
                + wagerChips.getText() + " chips");
    }

    private Section handArea(String title, String description, Span score, Div cards) {
        var name = new H2(title);
        cards.getElement().setAttribute("role", "group");
        cards.getElement().setAttribute("aria-label", title + " cards");
        var area = new Section(box("hand-heading", box("hand-name", name, score)), cards,
                text("hand-note", description));
        area.addClassName("hand-area");
        area.getElement().setAttribute("aria-label", title);
        return area;
    }

    private void configureButton(Button button, String id, String className, String tooltip) {
        button.setId(id);
        button.addClassName(className);
        button.setTooltipText(tooltip);
        button.getElement().setAttribute("aria-describedby", "status-detail");
    }

    private Section rules() {
        var section = new Section(rule("01", "Aim for 21", "Number cards keep their value. J, Q and K are 10. Aces are 1 or 11."),
                rule("02", "Make your move", "Hit for another card, or stand to hold. Go over 21 and you bust."),
                rule("03", "Make it count", "Wins pay 1:1. Blackjack pays 3:2. A push returns your bet. Protect your 1,000-chip bankroll."));
        section.addClassName("rules");
        section.getElement().setAttribute("aria-label", "How to play");
        return section;
    }

    private Div rule(String number, String title, String description) {
        return box("rule", text("rule-number", number), box("rule-copy", new H2(title), new Paragraph(description)));
    }

    private Footer footer() {
        var footer = new Footer(new Span("52 cards. Reshuffle below 20, between rounds only."),
                new Span("Virtual chips only · No splits, doubling or insurance."));
        footer.addClassName("site-footer");
        return footer;
    }

    private void act(Runnable command) {
        command.run();
        var state = game.snapshot();
        if (!state.canPlay() && !state.outOfChips() && selectedBet() > state.maximumBet()) {
            betAmount.setValue(state.maximumBet());
        }
        render();
        if (state.canPlay()) {
            hit.focus(FocusOption.PreventScroll.ENABLED);
        } else if (state.outOfChips()) {
            restart.focus(FocusOption.PreventScroll.ENABLED);
        } else if (!newRound.isEnabled()) {
            betAmount.focus(FocusOption.PreventScroll.ENABLED);
        } else {
            newRound.focus(FocusOption.PreventScroll.ENABLED);
        }
    }

    private void render() {
        var state = game.snapshot();
        renderHand(playerCards, state.player(), false);
        renderHand(dealerCards, state.dealer(), state.dealerCardHidden());
        playerScore.setText(score(state.player()));
        dealerScore.setText(state.dealerCardHidden() ? state.dealer().total() + " showing" : score(state.dealer()));
        playerScore.getElement().setAttribute("aria-label", "Your score: " + playerScore.getText());
        dealerScore.getElement().setAttribute("aria-label", "Dealer score: " + dealerScore.getText());
        roundNumber.setText(state.round() == 0 ? "THE TABLE IS YOURS" : "ROUND %02d".formatted(state.round()));
        turn.setText(switch (state.phase()) {
            case READY -> "READY TO PLAY";
            case PLAYER_TURN -> "YOUR MOVE";
            case ROUND_OVER -> "ROUND COMPLETE";
        });
        wins.setText(Integer.toString(state.statistics().wins()));
        losses.setText(Integer.toString(state.statistics().losses()));
        pushes.setText(Integer.toString(state.statistics().pushes()));
        bankroll.setText(formatChips(state.bankroll().halfUnits()));
        sessionProfit.setText(signedChips(state.sessionProfitHalfChips()) + " this session");
        sessionProfit.getElement().setAttribute("data-positive", Boolean.toString(state.sessionProfitHalfChips() >= 0));
        shoeStatus.setText("DECK %d · %d / 52 LEFT".formatted(state.shoeNumber(), state.cardsRemaining()));
        shoeNote.setText(state.shuffleBeforeNextRound() ? "Cut reached · Reshuffle before the next deal."
                : state.shuffledThisRound() ? "Fresh shuffle for this round · Discards returned to the deck."
                : "Cards carry over between rounds · " + (52 - state.cardsRemaining()) + " dealt from this deck.");
        hit.setEnabled(state.canPlay());
        stand.setEnabled(state.canPlay());
        updateBetting(state);

        var message = message(state);
        statusTitle.setText(state.outOfChips() ? "Your bankroll is below the minimum." : message.title());
        statusDetail.setText(state.outOfChips()
                ? "Session over after " + state.round() + " rounds. Select New Session to start with 1,000 chips."
                : message.detail());
        payoutText.setText(state.phase() == Phase.ROUND_OVER
                ? signedChips(state.netHalfChips()) + " chips · " + formatChips(state.payout().halfUnits()) + " returned (includes stake)"
                : state.canPlay() ? state.wager() + " chips committed to this hand." : "Choose your stake, then deal. Virtual chips only.");
        status.getElement().setAttribute("data-outcome", state.outcome().name().toLowerCase(java.util.Locale.ROOT));
        // A single, descriptive live update includes the scores after every action.
        status.getElement().setAttribute("aria-label", statusTitle.getText() + " " + statusDetail.getText() + " " + payoutText.getText()
                + (state.phase() == Phase.READY ? "" : " Your score: " + playerScore.getText()
                + ". Dealer: " + dealerScore.getText() + "."));
    }

    private static String formatChips(long halfUnits) {
        var format = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(java.math.BigDecimal.valueOf(halfUnits).divide(java.math.BigDecimal.valueOf(2)));
    }

    private static String signedChips(long halfUnits) {
        return (halfUnits > 0 ? "+" : "") + formatChips(halfUnits);
    }

    private void renderHand(Div container, Hand hand, boolean hidden) {
        container.removeAll();
        if (hand.cards().isEmpty()) {
            container.add(PlayingCard.placeholder(), PlayingCard.placeholder());
        } else {
            hand.cards().forEach(card -> container.add(new PlayingCard(card)));
            if (hidden) {
                container.add(PlayingCard.faceDown());
            }
        }
    }

    private static String score(Hand hand) {
        if (hand.cards().isEmpty()) { return "—"; }
        if (hand.isBlackjack()) { return "21 · Blackjack"; }
        if (hand.isBust()) { return hand.total() + " · Bust"; }
        return hand.total() + (hand.value().soft() ? " · Soft" : "");
    }

    private static Message message(BlackjackGame.Snapshot state) {
        return switch (state.outcome()) {
            case NONE -> state.phase() == Phase.READY
                    ? new Message("Your seat is ready.", "Set your bet and select New Round to deal.")
                    : new Message("A little nerve. A little luck.", "Hit to take a card, or stand to hold your " + state.player().total() + ".");
            case PLAYER_BLACKJACK -> new Message("Blackjack. Beautifully played.", "An ace and a ten-value card. You win this round.");
            case DEALER_BLACKJACK -> new Message("Dealer has blackjack.", "A natural 21 takes the round. Try a fresh hand.");
            case PLAYER_BUST -> new Message("Busted. The next hand awaits.", "Your " + state.player().total() + " is over 21. Dealer wins.");
            case DEALER_BUST -> new Message("Dealer busts. You win!", "Dealer went over with " + state.dealer().total() + ". Your hand holds.");
            case PLAYER_WIN -> new Message("This one's yours.", "Your " + state.player().total() + " beats the dealer's " + state.dealer().total() + ". You win.");
            case DEALER_WIN -> new Message("Dealer takes this one.", "Dealer's " + state.dealer().total() + " beats your " + state.player().total() + ". Another round?");
            case PUSH -> new Message("A push. Honours even.", state.player().isBlackjack()
                    ? "Two blackjacks. A remarkable draw."
                    : "Both hands total " + state.player().total() + ". Neither side wins.");
        };
    }

    private record Message(String title, String detail) { }

    private static Div box(String className, Component... children) {
        var div = new Div(children);
        div.addClassName(className);
        return div;
    }

    private static Span text(String className, String value) {
        var span = new Span(value);
        span.addClassName(className);
        return span;
    }
}
