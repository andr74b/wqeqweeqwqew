# Blackjack verification

Verified on **6 September 2026**, on this Mac with Microsoft OpenJDK **25.0.4.1**.
Vaadin **25.2.6**, Spring Boot **4.1.1**, Maven wrapper **3.9.16**.

## Automated results

Final command: `./mvnw --batch-mode --no-transfer-progress clean verify`

**BUILD SUCCESS — 76 tests, 0 failures, 0 errors, 0 skipped.**

| Suite | Cases | Result |
| --- | ---: | --- |
| HandTest | 17 | Pass |
| DeckTest | 3 | Pass |
| BlackjackGameTest | 20 | Pass |
| BlackjackViewTest | 12 | Pass |
| BlackjackBettingTest | 18 | Pass |
| BlackjackShoeTest | 4 | Pass |
| BlackjackGameIT | 1, exercising 2,000 rounds | Pass |
| ProductionPackageIT | 1 | Pass |

The generated production artifact is `target/blackjack.jar`. The packaging test
starts this actual JAR in an empty temporary directory, checks its production
configuration and bundled dependencies, and requests the page and card CSS.
The simulation uses 100 independent tables, independent scoring and bankroll
calculations, and card-uniqueness checks across complete decks and multiple rounds.
Detailed XML/text results are in `target/surefire-reports` and
`target/failsafe-reports` after building.

## Browser checks

The production JAR was started with `java -jar target/blackjack.jar
--server.port=8082 --server.address=127.0.0.1`, and its real Vaadin client was
exercised in the Codex in-app browser at localhost:8082. This avoided interrupting
the existing servers on ports 8080 and 8081.

- Initial state: 1,000 chips, 52 cards, empty card spaces, zero counters, New
  Round enabled for the initial 25-chip bet, Hit and Stand disabled.
- Chip rack: Clear disables dealing and displays validation. Adding the 5 and
  25 chips selects a 30-chip stake while leaving the bankroll at 1,000.
- Stake and settlement: a 30-chip deal reduced the bankroll to 970. An observed
  comparison win returned 60 chips, showing a 1,030 balance and +30 session profit.
- Dealing: two player cards, one visible dealer card and a patterned card back;
  the dealer score shows only the upcard. New Round becomes disabled.
- Hit: one card added; observed a player bust, immediate loss, revealed dealer
  cards, and correctly disabled move controls.
- Stand: observed comparison wins and dealer drawing to a bust; result text,
  dealer cards, and win counters updated together.
- Natural blackjack: observed automatic resolution on the deal, including the
  blackjack score label and a win with no extra dealer draw.
- Persistent deck: the next round dealt from the same deck, showing 44 / 52
  cards left. Later the counter reached 19 / 52 and displayed the cut notice.
  The next deal displayed Deck 2, 48 / 52, and an explicit fresh-shuffle notice.
- Refresh preservation: compared the hand markup, bankroll, deck counter and
  round before and after a page reload during play; all remained identical.
- Bankruptcy: two losing 500-chip hands brought a separate tab's bankroll to
  zero. New Round was replaced by New Session. Clicking it explicitly restored
  1,000 chips, Deck 1 with 52 cards, the initial 25-chip selection, and zero rounds.
- Isolation: the second tab's bets, losses, and bankruptcy did not change the
  first tab's bankroll, deck counter or round. New tabs begin independently.
- Keyboard: Enter on Stand activated the command; focus moved to the next
  enabled control. Accessible card names and status announcements are present.
- The view uses Vaadin's PreventScroll focus option to avoid programmatic jumps
  when moving focus to the next enabled action.
- Responsive widths checked: **320**, **390**, **768**, **1280**, and **1440**
  pixels. Document width matched the viewport; no horizontal overflow was found.
  Chip buttons measured 46 × 46 pixels on the phone layout. Action buttons use
  46-pixel height on phones and 44 on desktop/tablet.
  The page scrolls vertically on short screens.
- No browser warning/error console entries were recorded during the checks.

The UI checks above are an interactive smoke test, not a committed automated
browser suite. Rule edge cases, including both-natural pushes, dealer natural,
soft 17, multiple aces, ordinary pushes, and repeated commands, are covered by
the deterministic automated suites. Physical iOS/Android devices and separate
Safari/Firefox engines were not tested.

The temporary test server on port 8082 and temporary browser tabs were stopped
after verification. Existing servers were left running. Stop the older game in
its terminal with Ctrl+C, then run `./run-production.sh` to load the new build.
