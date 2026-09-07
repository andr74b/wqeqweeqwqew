package org.example.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.FocusOption;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
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
public class BlackjackView extends Div {
    // A browser UI (tab) owns its game, allowing locale routes to share state safely.
    private final BlackjackGame game;
    private final Locale locale;
    private final GameMessages copy;
    private final Div dealerCards = box("cards");
    private final Div playerCards = box("cards");
    private final Span dealerScore = text("hand-score", "—");
    private final Span playerScore = text("hand-score", "—");
    private final Span roundNumber = text("round-number", "THE TABLE IS YOURS");
    private final Span wins = new Span("0");
    private final Span losses = new Span("0");
    private final Span pushes = new Span("0");
    private final Span bankroll = text("bankroll-amount", "1,000");
    private final Span sessionProfit = text("session-profit", "");
    private final Span shoeStatus = text("shoe-status", "");
    private final Span shoeNote = text("shoe-note", "");
    private final Span wagerChips = text("wager-chip", "25");
    private final Span wagerLabel = text("wager-label", "");
    private final Span payoutText = text("payout-text", "");
    private final IntegerField betAmount = new IntegerField();
    private final Button chip5 = chipButton(5);
    private final Button chip25 = chipButton(25);
    private final Button chip100 = chipButton(100);
    private final Button clearBet = new Button();
    private final Button restart = new Button();
    private final H2 statusTitle = new H2();
    private final Paragraph statusDetail = new Paragraph();
    private final Div status = box("round-status", statusTitle, statusDetail);
    private final Button hit = new Button();
    private final Button stand = new Button();
    private final Button newRound = new Button();
    private Div betting;

    public BlackjackView() {
        this(gameForCurrentUi(), Locale.ENGLISH);
    }

    // Package-private injection keeps component tests independent of randomness.
    BlackjackView(BlackjackGame game) {
        this(game, Locale.ENGLISH);
    }

    protected BlackjackView(BlackjackGame game, Locale locale) {
        this.game = Objects.requireNonNull(game, "game");
        this.locale = Objects.requireNonNull(locale, "locale");
        copy = new GameMessages(locale);
        var ui = UI.getCurrent();
        if (ui != null) {
            ui.setLocale(locale);
            if (ui.getSession() != null) {
                ui.getPage().executeJs("document.documentElement.lang = $0", locale.getLanguage());
            }
        }
        hit.addClickListener(event -> act(game::hit));
        stand.addClickListener(event -> act(game::stand));
        newRound.addClickListener(event -> act(() -> game.newRound(selectedBet())));
        clearBet.addClickListener(event -> betAmount.clear());
        restart.addClickListener(event -> {
            if (game.restartGame()) {
                betAmount.setValue(25);
                render();
                newRound.focus(FocusOption.PreventScroll.ENABLED);
            }
        });
        applyStaticCopy();
        betAmount.setValue(25);
        addClassName("blackjack-app");
        var main = new Main(introduction(), table());
        main.addClassName("content");
        add(header(), main, footer());
        render();
    }

    protected static BlackjackGame gameForCurrentUi() {
        var ui = UI.getCurrent();
        if (ui == null) {
            return new BlackjackGame();
        }
        var existing = ComponentUtil.getData(ui, BlackjackGame.class);
        if (existing != null) {
            return existing;
        }
        var created = new BlackjackGame();
        ComponentUtil.setData(ui, BlackjackGame.class, created);
        return created;
    }

    private void applyStaticCopy() {
        betAmount.setLabel(copy.text("bet.label"));
        clearBet.setText(copy.text("action.clear"));
        restart.setText(copy.text("action.newGame"));
        hit.setText(copy.text("action.hit"));
        stand.setText(copy.text("action.stand"));
        newRound.setText(copy.text("action.deal.first"));
        chip5.getElement().setAttribute("aria-label", copy.text("bet.add", 5));
        chip25.getElement().setAttribute("aria-label", copy.text("bet.add", 25));
        chip100.getElement().setAttribute("aria-label", copy.text("bet.add", 100));
    }

    private Header header() {
        var mark = text("brand-mark", "♠");
        mark.getElement().setAttribute("aria-hidden", "true");
        // Navigate within the Vaadin UI; plain anchors can create a new browser UI.
        var english = new Button("EN", event -> switchLanguage(BlackjackView.class));
        var russian = new Button("RU", event -> switchLanguage(RussianBlackjackView.class));
        var spanish = new Button("ES", event -> switchLanguage(SpanishBlackjackView.class));
        english.setId("language-en");
        russian.setId("language-ru");
        spanish.setId("language-es");
        english.setAriaLabel("English");
        russian.setAriaLabel("Русский");
        spanish.setAriaLabel("Español");
        var language = locale.getLanguage();
        english.getElement().setAttribute("aria-pressed", Boolean.toString(language.equals("en")));
        russian.getElement().setAttribute("aria-pressed", Boolean.toString(language.equals("ru")));
        spanish.getElement().setAttribute("aria-pressed", Boolean.toString(language.equals("es")));
        english.addClassName("language-option");
        russian.addClassName("language-option");
        spanish.addClassName("language-option");
        var activeLanguage = switch (language) {
            case "ru" -> russian;
            case "es" -> spanish;
            default -> english;
        };
        activeLanguage.addClassName("active");
        var languageSwitch = box("language-switch", english, russian, spanish);
        languageSwitch.getElement().setAttribute("role", "group");
        languageSwitch.getElement().setAttribute("aria-label", copy.text("language.label"));
        var header = new Header(box("brand", mark, new Span(copy.text("brand.name"))),
                box("header-tools", rulesButton(), languageSwitch));
        header.addClassName("site-header");
        return header;
    }

    private void switchLanguage(Class<? extends BlackjackView> target) {
        getUI().ifPresent(ui -> {
            // A preserved view may have been reattached to a new UI after a refresh.
            ComponentUtil.setData(ui, BlackjackGame.class, game);
            ui.navigate(target);
        });
    }

    private Section introduction() {
        var title = new H1(copy.text("game.title"));
        title.setId("game-title");
        var introCopy = box("intro-copy", title);
        bankroll.setId("bankroll");
        sessionProfit.setId("session-profit");
        var bank = box("bankroll-line", text("bank-chip", "♠"), bankroll,
                text("bankroll-unit", copy.text("bankroll.unit")));
        var intro = new Section(introCopy,
                box("scoreboard", text("eyebrow", copy.text("bankroll.heading")), bank, sessionProfit));
        intro.addClassName("introduction");
        intro.getElement().setAttribute("aria-labelledby", "game-title");
        return intro;
    }

    private Div statistic(String label, Span value, String id) {
        value.setId(id);
        return box("statistic", text("statistic-label", label), value);
    }

    private Section table() {
        dealerCards.setId("dealer-cards");
        playerCards.setId("player-cards");
        dealerScore.setId("dealer-score");
        playerScore.setId("player-score");
        roundNumber.setId("round-number");
        shoeStatus.setId("shoe-status");
        shoeNote.setId("shoe-note");
        wagerChips.setId("wager-chips");
        payoutText.setId("payout-text");

        var dealer = handArea(copy.text("hand.dealer"), copy.text("hand.dealer.cards"), dealerScore, dealerCards);
        dealer.addClassName("dealer-hand");
        var player = handArea(copy.text("hand.player"), copy.text("hand.player.cards"), playerScore, playerCards);
        player.addClassName("player-hand");
        var felt = box("felt", box("table-topline", roundNumber, shoeStatus), dealer, player,
                box("wager-spot", wagerChips, wagerLabel), shoeNote);

        configureButton(hit, "hit", "hit-button", copy.text("action.hit.tooltip"));
        configureButton(stand, "stand", "stand-button", copy.text("action.stand.tooltip"));
        configureButton(newRound, "new-round", "new-round-button", copy.text("action.deal.tooltip"));
        configureButton(restart, "new-game", "new-game-button", copy.text("action.newGame.tooltip"));
        status.setId("round-status");
        status.getElement().setAttribute("role", "status");
        status.getElement().setAttribute("aria-live", "polite");
        status.getElement().setAttribute("aria-atomic", "true");
        statusTitle.setId("status-title");
        statusDetail.setId("status-detail");

        status.add(payoutText);
        var controls = box("controls", status, box("actions", hit, stand, newRound, restart));
        betting = bettingBar();
        var table = new Section(felt, betting, controls);
        table.addClassName("game-table");
        table.getElement().setAttribute("aria-label", copy.text("table.label"));
        return table;
    }

    private Div bettingBar() {
        betAmount.setId("bet-amount");
        betAmount.setMin(BlackjackGame.MIN_BET);
        betAmount.setMax(game.snapshot().maximumBet());
        betAmount.setStep(BlackjackGame.BET_STEP);
        betAmount.setStepButtonsVisible(true);
        betAmount.setValueChangeMode(ValueChangeMode.EAGER);
        betAmount.setManualValidation(true);
        betAmount.addValueChangeListener(event -> updateBetting(game.snapshot()));
        clearBet.setId("clear-bet");
        clearBet.addClassName("clear-bet");
        var rack = box("chip-rack", chip5, chip25, chip100, clearBet);
        rack.getElement().setAttribute("role", "group");
        rack.getElement().setAttribute("aria-label", copy.text("bet.rack.label"));
        return box("betting-bar", betAmount, rack);
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
        betAmount.getElement().setAttribute("title", copy.text("bet.helper", formatChips(state.maximumBet() * 2L)));
        betting.setVisible(editable);
        betAmount.setErrorMessage(validation == BlackjackGame.BetValidation.INSUFFICIENT_CHIPS
                ? copy.text("bet.error.insufficient")
                : copy.text("bet.error.invalid"));
        long base = Math.max(0, selectedBet());
        chip5.setEnabled(editable && base + 5 <= state.maximumBet());
        chip25.setEnabled(editable && base + 25 <= state.maximumBet());
        chip100.setEnabled(editable && base + 100 <= state.maximumBet());
        clearBet.setEnabled(editable && selectedBet() != 0);
        newRound.setText(copy.text(state.round() == 0 ? "action.deal.first" : "action.deal.next"));
        newRound.setEnabled(validation == BlackjackGame.BetValidation.VALID);
        restart.setVisible(state.phase() == Phase.ROUND_OVER);
        restart.setEnabled(state.phase() == Phase.ROUND_OVER);
        newRound.setVisible(!state.canPlay() && !state.outOfChips());
        wagerChips.setText(formatChips((state.phase() == Phase.READY ? Math.max(0, selectedBet()) : state.wager()) * 2L));
        wagerLabel.setText(copy.text(state.phase() == Phase.READY
                ? "wager.selected" : state.canPlay() ? "wager.active" : "wager.previous"));
        wagerChips.getElement().setAttribute("aria-label", copy.text(
                state.phase() == Phase.READY ? "wager.selected.label" : "wager.hand.label", wagerChips.getText()));
    }

    private Section handArea(String title, String cardsLabel, Span score, Div cards) {
        var name = new H2(title);
        cards.getElement().setAttribute("role", "group");
        cards.getElement().setAttribute("aria-label", cardsLabel);
        var area = new Section(box("hand-heading", box("hand-name", name, score)), cards);
        area.addClassName("hand-area");
        area.getElement().setAttribute("aria-label", title);
        return area;
    }

    private void configureButton(Button button, String id, String className, String tooltip) {
        button.setId(id);
        button.addClassName(className);
        button.setTooltipText(tooltip);
        button.getElement().setAttribute("aria-describedby", "round-status");
    }

    private Button rulesButton() {
        var dialog = new Dialog();
        dialog.setId("rules-dialog");
        dialog.addClassName("blackjack-rules-dialog");
        dialog.setHeaderTitle(copy.text("rules.label"));
        dialog.setWidth("440px");
        dialog.setMaxWidth("calc(100vw - 32px)");
        var section = new Section(rule("01", copy.text("rule.1.title"), copy.text("rule.1.detail")),
                rule("02", copy.text("rule.2.title"), copy.text("rule.2.detail")),
                rule("03", copy.text("rule.3.title"), copy.text("rule.3.detail")),
                rule("04", copy.text("rule.4.title"), copy.text("rule.4.detail")),
                new Paragraph(copy.text("footer.limits")));
        section.addClassName("rules");
        dialog.add(section);
        var close = new Button(copy.text("action.close"), event -> dialog.close());
        close.setId("close-rules");
        dialog.getFooter().add(close);
        add(dialog);
        var button = new Button(copy.text("action.rules"), event -> dialog.open());
        button.setId("show-rules");
        button.addClassName("rules-button");
        button.getElement().setAttribute("aria-haspopup", "dialog");
        return button;
    }

    private Div rule(String number, String title, String description) {
        return box("rule", text("rule-number", number), box("rule-copy", new H2(title), new Paragraph(description)));
    }

    private Footer footer() {
        var stats = box("session-stats", statistic(copy.text("statistics.wins"), wins, "wins"),
                statistic(copy.text("statistics.losses"), losses, "losses"),
                statistic(copy.text("statistics.pushes"), pushes, "pushes"));
        stats.getElement().setAttribute("role", "group");
        stats.getElement().setAttribute("aria-label", copy.text("statistics.label"));
        var footer = new Footer(new Span(copy.text("footer.note")), stats);
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
        dealerScore.setText(state.dealerCardHidden()
                ? copy.text("score.showing", state.dealer().total()) : score(state.dealer()));
        playerScore.getElement().setAttribute("aria-label", copy.text("score.player", playerScore.getText()));
        dealerScore.getElement().setAttribute("aria-label", copy.text(state.dealerCardHidden() ? "score.dealer.hidden" : "score.dealer", dealerScore.getText()));
        roundNumber.setText(state.round() == 0 ? copy.text("game.new")
                : copy.text("hand.number", "%02d".formatted(state.round())));
        wins.setText(Integer.toString(state.statistics().wins()));
        losses.setText(Integer.toString(state.statistics().losses()));
        pushes.setText(Integer.toString(state.statistics().pushes()));
        bankroll.setText(formatChips(state.bankroll().halfUnits()));
        sessionProfit.setText(copy.text("profit.game", signedChips(state.sessionProfitHalfChips())));
        sessionProfit.getElement().setAttribute("data-positive", Boolean.toString(state.sessionProfitHalfChips() >= 0));
        shoeStatus.setText(copy.text("deck.status", state.shoeNumber(), state.cardsRemaining()));
        shoeNote.setText(copy.text("deck.cut"));
        shoeNote.setVisible(state.shuffleBeforeNextRound());
        hit.setVisible(state.canPlay());
        stand.setVisible(state.canPlay());
        hit.setEnabled(state.canPlay());
        stand.setEnabled(state.canPlay());
        updateBetting(state);

        var message = message(state);
        statusTitle.setText(state.outOfChips() ? copy.text("status.bankroll.title") : message.title());
        statusDetail.setText(state.outOfChips()
                ? copy.text("status.bankroll.detail", state.round())
                : message.detail());
        statusDetail.setVisible(state.phase() == Phase.ROUND_OVER);
        payoutText.setVisible(state.phase() == Phase.ROUND_OVER);
        payoutText.setText(state.phase() == Phase.ROUND_OVER
                ? copy.text("payout.complete", signedChips(state.netHalfChips()), formatChips(state.payout().halfUnits()))
                : "");
        payoutText.getElement().setAttribute("title", copy.text("payout.returned", formatChips(state.payout().halfUnits())));
        status.getElement().setAttribute("data-outcome", state.outcome().name().toLowerCase(java.util.Locale.ROOT));
        // A single, descriptive live update includes the scores after every action.
        status.getElement().setAttribute("aria-label", statusTitle.getText() + " " + statusDetail.getText() + " " + payoutText.getText()
                + (state.phase() == Phase.ROUND_OVER ? " " + copy.text("payout.returned", formatChips(state.payout().halfUnits())) : "")
                + (state.phase() == Phase.READY ? "" : " " + copy.text("score.player", playerScore.getText())
                + ". " + copy.text(state.dealerCardHidden() ? "score.dealer.hidden" : "score.dealer", dealerScore.getText()) + "."));
    }

    private String formatChips(long halfUnits) {
        var format = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(locale));
        return format.format(java.math.BigDecimal.valueOf(halfUnits).divide(java.math.BigDecimal.valueOf(2)));
    }

    private String signedChips(long halfUnits) {
        return (halfUnits > 0 ? "+" : "") + formatChips(halfUnits);
    }

    private void renderHand(Div container, Hand hand, boolean hidden) {
        container.removeAll();
        int count = hand.cards().isEmpty() ? 2 : hand.cards().size() + (hidden ? 1 : 0);
        container.getStyle().set("--card-count", Integer.toString(count));
        if (hand.cards().isEmpty()) {
            container.add(PlayingCard.placeholder(copy), PlayingCard.placeholder(copy));
        } else {
            hand.cards().forEach(card -> container.add(new PlayingCard(card, copy)));
            if (hidden) {
                container.add(PlayingCard.faceDown(copy));
            }
        }
    }

    private String score(Hand hand) {
        if (hand.cards().isEmpty()) { return "—"; }
        if (hand.isBlackjack()) { return copy.text("score.blackjack"); }
        if (hand.isBust()) { return copy.text("score.bust", hand.total()); }
        return hand.value().soft() ? copy.text("score.soft", hand.total()) : Integer.toString(hand.total());
    }

    private Message message(BlackjackGame.Snapshot state) {
        return switch (state.outcome()) {
            case NONE -> state.phase() == Phase.READY
                    ? message("status.ready.title", "status.ready.detail")
                    : new Message(copy.text("status.turn.title"), copy.text("status.turn.detail", state.player().total()));
            case PLAYER_BLACKJACK -> message("status.playerBlackjack.title", "status.playerBlackjack.detail");
            case DEALER_BLACKJACK -> message("status.dealerBlackjack.title", "status.dealerBlackjack.detail");
            case PLAYER_BUST -> new Message(copy.text("status.playerBust.title"),
                    copy.text("status.playerBust.detail", state.player().total()));
            case DEALER_BUST -> new Message(copy.text("status.dealerBust.title"),
                    copy.text("status.dealerBust.detail", state.dealer().total()));
            case PLAYER_WIN -> new Message(copy.text("status.playerWin.title"),
                    copy.text("status.playerWin.detail", state.player().total(), state.dealer().total()));
            case DEALER_WIN -> new Message(copy.text("status.dealerWin.title"),
                    copy.text("status.dealerWin.detail", state.dealer().total(), state.player().total()));
            case PUSH -> new Message(copy.text("status.push.title"), state.player().isBlackjack()
                    ? copy.text("status.push.blackjacks") : copy.text("status.push.equal", state.player().total()));
        };
    }

    private Message message(String titleKey, String detailKey) {
        return new Message(copy.text(titleKey), copy.text(detailKey));
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
