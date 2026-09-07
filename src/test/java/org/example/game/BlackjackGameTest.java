package org.example.game;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.example.TestDecks.game;
import static org.example.game.BlackjackGame.Outcome.*;
import static org.example.game.BlackjackGame.Phase.*;
import static org.example.game.Rank.*;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackGameTest {
    @Test
    void startsReadyAndIgnoresHitAndStandBeforeTheDeal() {
        var game = new BlackjackGame();
        var initial = game.snapshot();
        game.hit();
        game.stand();
        assertEquals(initial, game.snapshot());
        assertEquals(READY, initial.phase());
        assertEquals(0, initial.round());
        assertTrue(initial.player().cards().isEmpty());
        assertTrue(initial.dealer().cards().isEmpty());
    }

    @Test
    void dealsAlternatelyAndDoesNotExposeTheHoleCardOrFullDealerScore() {
        var game = game(TEN, SIX, SEVEN, ACE);
        game.newRound(25);
        var state = game.snapshot();
        assertEquals(PLAYER_TURN, state.phase());
        assertEquals(17, state.player().total());
        assertEquals(2, state.player().cards().size());
        assertEquals(1, state.dealer().cards().size());
        assertEquals(6, state.dealer().total());
        assertTrue(state.dealerCardHidden());
        assertEquals(NONE, state.outcome());
        game.stand();
        assertEquals(17, game.snapshot().dealer().total());
        assertFalse(game.snapshot().dealerCardHidden());
    }

    @Test
    void playerNaturalWinsImmediatelyAndDealerDoesNotDraw() {
        var game = game(ACE, TWO, KING, THREE);
        game.newRound(25);
        assertFinished(game, PLAYER_BLACKJACK);
        assertEquals(2, game.snapshot().dealer().cards().size());
        assertEquals(1, game.snapshot().statistics().wins());
    }

    @Test
    void dealerNaturalWinsBeforePlayerCanHit() {
        var game = game(TEN, ACE, SIX, QUEEN, FIVE);
        game.newRound(25);
        assertFinished(game, DEALER_BLACKJACK);
        game.hit();
        assertEquals(16, game.snapshot().player().total());
        assertEquals(1, game.snapshot().statistics().losses());
    }

    @Test
    void twoNaturalsPush() {
        var game = game(ACE, ACE, JACK, KING);
        game.newRound(25);
        assertFinished(game, PUSH);
        assertEquals(1, game.snapshot().statistics().pushes());
    }

    @Test
    void hittingAddsExactlyOneCardAndKeepsDealerHidden() {
        var game = game(FIVE, SIX, SEVEN, TEN, TWO);
        game.newRound(25);
        game.hit();
        assertEquals(14, game.snapshot().player().total());
        assertEquals(3, game.snapshot().player().cards().size());
        assertTrue(game.snapshot().canPlay());
        assertTrue(game.snapshot().dealerCardHidden());
    }

    @Test
    void playerBustLosesImmediatelyEvenWhenDealerWouldHaveBusted() {
        var game = game(KING, SIX, SIX, TEN, QUEEN);
        game.newRound(25);
        game.hit();
        assertFinished(game, PLAYER_BUST);
        assertEquals(26, game.snapshot().player().total());
        assertEquals(2, game.snapshot().dealer().cards().size());
    }

    @Test
    void hittingTwentyOneAutomaticallyRunsDealerAndIsNotANatural() {
        var game = game(TEN, NINE, FIVE, SEVEN, SIX, FOUR);
        game.newRound(25);
        game.hit();
        assertFinished(game, PLAYER_WIN);
        assertEquals(21, game.snapshot().player().total());
        assertFalse(game.snapshot().player().isBlackjack());
        assertEquals(20, game.snapshot().dealer().total());
    }

    @Test
    void threeCardTwentyOnePushesDealerTwentyOne() {
        var game = game(TEN, SEVEN, FIVE, NINE, SIX, FIVE);
        game.newRound(25);
        game.hit();
        assertFinished(game, PUSH);
        assertEquals(21, game.snapshot().dealer().total());
    }

    @Test
    void dealerStandsOnSoftSeventeen() {
        var game = game(TEN, ACE, EIGHT, SIX, KING);
        game.newRound(25);
        game.stand();
        assertFinished(game, PLAYER_WIN);
        assertEquals(2, game.snapshot().dealer().cards().size());
        assertEquals(new Hand.Value(17, true), game.snapshot().dealer().value());
    }

    @Test
    void dealerStandsOnHardSeventeen() {
        var game = game(TEN, TEN, SIX, SEVEN, KING);
        game.newRound(25);
        game.stand();
        assertFinished(game, DEALER_WIN);
        assertEquals(2, game.snapshot().dealer().cards().size());
    }

    @Test
    void dealerHitsSoftSixteenAndRevaluesAcesUntilReachingSeventeen() {
        var game = game(TEN, ACE, NINE, FIVE, KING, ACE);
        game.newRound(25);
        game.stand();
        assertFinished(game, PLAYER_WIN);
        assertEquals(new Hand.Value(17, false), game.snapshot().dealer().value());
        assertEquals(4, game.snapshot().dealer().cards().size());
    }

    @Test
    void dealerBustWinsForPlayer() {
        var game = game(TEN, SIX, TWO, TEN, QUEEN);
        game.newRound(25);
        game.stand();
        assertFinished(game, DEALER_BUST);
        assertEquals(26, game.snapshot().dealer().total());
        assertEquals(1, game.snapshot().statistics().wins());
    }

    @Test
    void equalNonBlackjackTotalsPush() {
        var game = game(TEN, QUEEN, EIGHT, EIGHT);
        game.newRound(25);
        game.stand();
        assertFinished(game, PUSH);
    }

    @Test
    void playerAceCanBecomeOneWithoutBusting() {
        var game = game(ACE, TEN, FIVE, SEVEN, KING);
        game.newRound(25);
        game.hit();
        assertEquals(new Hand.Value(16, false), game.snapshot().player().value());
        assertTrue(game.snapshot().canPlay());
    }

    @Test
    void newRoundCannotAbandonAnActiveHandAndOldSnapshotsAreImmutable() {
        var game = game(TEN, TEN, FIVE, SEVEN, TWO);
        game.newRound(25);
        var initial = game.snapshot();
        game.newRound(25);
        assertEquals(initial, game.snapshot());
        game.hit();
        assertEquals(15, initial.player().total());
        assertEquals(17, game.snapshot().player().total());
        assertThrows(UnsupportedOperationException.class, () -> initial.player().cards().clear());
        assertThrows(UnsupportedOperationException.class, () -> initial.dealer().cards().clear());
    }

    @Test
    void finishedCommandsAreIdempotentAndNewRoundResetsCardsButPreservesStatistics() {
        var game = game(TEN, TEN, EIGHT, SEVEN);
        game.newRound(25);
        game.stand();
        var completed = game.snapshot();
        game.hit();
        game.stand();
        assertEquals(completed, game.snapshot());
        game.newRound(25);
        assertEquals(2, game.snapshot().round());
        assertEquals(NONE, game.snapshot().outcome());
        assertEquals(completed.statistics(), game.snapshot().statistics());
        assertTrue(game.snapshot().dealerCardHidden());
        game.stand();
        assertEquals(1, game.snapshot().statistics().wins());
        assertEquals(2, game.snapshot().statistics().roundsPlayed());
    }

    @Test
    void separateGamesDoNotShareCardsResultsOrStatistics() {
        var first = game(TEN, TEN, EIGHT, SEVEN);
        var second = game(FIVE, SEVEN, EIGHT, TEN);
        first.newRound(25);
        second.newRound(25);
        var otherState = second.snapshot();
        first.stand();
        first.newRound(25);
        assertEquals(otherState, second.snapshot());
    }

    @Test
    void incompleteDeckIsRejectedBeforeChangingState() {
        assertThrows(IllegalArgumentException.class,
                () -> new BlackjackGame(() -> org.example.TestDecks.ordered(ACE, KING, QUEEN)));
    }

    @Test
    void gameSurvivesSessionSerializationIncludingItsDeckFactory() throws Exception {
        var game = new BlackjackGame();
        game.newRound(25);
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(game);
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            var restored = (BlackjackGame) input.readObject();
            assertEquals(game.snapshot(), restored.snapshot());
            restored.stand();
            restored.newRound(25);
            assertEquals(2, restored.snapshot().round());
            assertEquals(1, game.snapshot().round());
        }
    }

    private void assertFinished(BlackjackGame game, BlackjackGame.Outcome outcome) {
        assertEquals(ROUND_OVER, game.snapshot().phase());
        assertEquals(outcome, game.snapshot().outcome());
        assertFalse(game.snapshot().dealerCardHidden());
        assertFalse(game.snapshot().canPlay());
        assertEquals(1, game.snapshot().statistics().roundsPlayed());
    }
}
