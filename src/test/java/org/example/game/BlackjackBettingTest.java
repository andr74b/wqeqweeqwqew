package org.example.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.example.TestDecks.game;
import static org.example.game.BlackjackGame.BetValidation.*;
import static org.example.game.Rank.*;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackBettingTest {
    @Test
    void stakeIsDeductedAtDealAndAWinnerGetsStakePlusEqualProfit() {
        var game = game(TEN, TEN, NINE, SEVEN);
        assertTrue(game.newRound(25));
        assertEquals(Chips.whole(975), game.snapshot().bankroll());
        assertEquals(25, game.snapshot().wager());
        assertEquals(Chips.ZERO, game.snapshot().payout());
        assertEquals(0, game.snapshot().sessionProfitHalfChips());
        game.stand();
        assertEquals(Chips.whole(1_025), game.snapshot().bankroll());
        assertEquals(Chips.whole(50), game.snapshot().payout());
        assertEquals(50, game.snapshot().netHalfChips());
    }

    @Test
    void blackjackPaysThreeToTwoExactlyForAnOddBet() {
        var game = game(ACE, TEN, KING, SEVEN);
        game.newRound(5);
        assertEquals(new Chips(2015), game.snapshot().bankroll()); // 1007.5
        assertEquals(new Chips(25), game.snapshot().payout()); // 12.5 including 5 stake
        assertEquals(15, game.snapshot().netHalfChips()); // 7.5 profit
    }

    @Test
    void dealerBlackjackLosesStakeAndTwoNaturalsReturnIt() {
        var loss = game(TEN, ACE, SEVEN, KING);
        loss.newRound(25);
        assertEquals(Chips.whole(975), loss.snapshot().bankroll());
        assertEquals(Chips.ZERO, loss.snapshot().payout());
        var push = game(ACE, ACE, KING, QUEEN);
        push.newRound(25);
        assertEquals(Chips.whole(1_000), push.snapshot().bankroll());
        assertEquals(Chips.whole(25), push.snapshot().payout());
        assertEquals(0, push.snapshot().netHalfChips());
    }

    @Test
    void ordinaryPushReturnsStakeWithoutProfit() {
        var game = game(TEN, TEN, EIGHT, EIGHT);
        game.newRound(100);
        game.stand();
        assertEquals(Chips.whole(1_000), game.snapshot().bankroll());
        assertEquals(Chips.whole(100), game.snapshot().payout());
    }

    @Test
    void playerBustLosesStakeAndDealerBustPaysEvenMoney() {
        var loss = game(TEN, TEN, SIX, SEVEN, KING);
        loss.newRound(100);
        loss.hit();
        assertEquals(Chips.whole(900), loss.snapshot().bankroll());
        assertEquals(-200, loss.snapshot().netHalfChips());
        var win = game(TEN, TEN, SIX, SIX, KING);
        win.newRound(100);
        win.stand();
        assertEquals(Chips.whole(1_100), win.snapshot().bankroll());
        assertEquals(Chips.whole(200), win.snapshot().payout());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -5, 0, 1, 6, 499, 501, Integer.MAX_VALUE})
    void invalidBetsCannotConsumeCardsOrChips(int bet) {
        var game = new BlackjackGame();
        var before = game.snapshot();
        assertEquals(INVALID_AMOUNT, game.validateBet(bet));
        assertFalse(game.newRound(bet));
        assertEquals(before, game.snapshot());
    }

    @Test
    void betAndPayoutCannotBeAppliedTwiceAndStakeCannotChangeMidHand() {
        var game = game(TEN, TEN, NINE, SEVEN);
        game.newRound(25);
        var playing = game.snapshot();
        assertEquals(ROUND_IN_PROGRESS, game.validateBet(100));
        assertFalse(game.newRound(100));
        assertFalse(game.restartGame());
        assertEquals(playing, game.snapshot());
        game.stand();
        var finished = game.snapshot();
        game.stand();
        game.hit();
        assertEquals(finished, game.snapshot());
    }

    @Test
    void maximumBetTracksTheBankrollAndGameCanResetBetweenHands() {
        var game = game(TEN, TEN, SIX, EIGHT, TEN, TEN, SIX, EIGHT);
        assertEquals(1_000, game.snapshot().maximumBet());
        assertFalse(game.restartGame());
        game.newRound(500);
        game.stand();
        game.newRound(250);
        game.stand();
        assertEquals(Chips.whole(250), game.snapshot().bankroll());
        assertEquals(250, game.snapshot().maximumBet());
        assertEquals(INSUFFICIENT_CHIPS, game.validateBet(255));
        assertFalse(game.newRound(255));
        assertTrue(game.restartGame());
        assertEquals(Chips.whole(1_000), game.snapshot().bankroll());
        assertEquals(1_000, game.snapshot().maximumBet());
        assertEquals(0, game.snapshot().round());
    }

    @Test
    void losingBankrollEndsSessionUntilExplicitRestart() {
        var game = game(TEN, TEN, SIX, EIGHT, TEN, TEN, SIX, EIGHT);
        game.newRound(500);
        game.stand();
        game.newRound(500);
        assertFalse(game.snapshot().outOfChips(), "An all-in hand is still allowed to finish");
        assertFalse(game.restartGame());
        game.stand();
        assertTrue(game.snapshot().outOfChips());
        assertEquals(Chips.ZERO, game.snapshot().bankroll());
        assertFalse(game.newRound(5));
        assertTrue(game.restartGame());
        assertEquals(Chips.whole(1_000), game.snapshot().bankroll());
        assertEquals(0, game.snapshot().round());
        assertEquals(0, game.snapshot().statistics().roundsPlayed());
        assertEquals(52, game.snapshot().cardsRemaining());
        assertEquals(0, game.snapshot().wager());
    }

    @Test
    void halfChipRemainderBelowMinimumAlsoEndsSession() {
        var game = game(ACE, TWO, KING, THREE, TEN, TEN, SIX, EIGHT,
                TEN, TEN, SIX, EIGHT, QUEEN, QUEEN, SIX, EIGHT);
        game.newRound(5);
        game.newRound(500);
        game.stand();
        game.newRound(500);
        game.stand();
        assertEquals(new Chips(15), game.snapshot().bankroll()); // 7.5
        assertEquals(5, game.snapshot().maximumBet());
        game.newRound(5);
        game.stand();
        assertEquals(new Chips(5), game.snapshot().bankroll()); // 2.5
        assertTrue(game.snapshot().outOfChips());
        assertTrue(game.restartGame());
    }

    @Test
    void chipValuesRejectNegativeBalancesAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new Chips(-1));
        assertThrows(IllegalArgumentException.class, () -> Chips.whole(5).minus(Chips.whole(10)));
        assertThrows(ArithmeticException.class, () -> Chips.whole(Long.MAX_VALUE));
    }
}
