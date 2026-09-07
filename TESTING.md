# Blackjack verification

Verified on **7 September 2026**, on this Mac with Microsoft OpenJDK **25.0.4.1**.
Vaadin **25.2.6**, Spring Boot **4.1.1**, Maven wrapper **3.9.16**.

## Automated results

Final command: `./mvnw --batch-mode --no-transfer-progress clean verify`

**BUILD SUCCESS — 80 tests, 0 failures, 0 errors, 0 skipped.**

| Suite | Cases | Result |
| --- | ---: | --- |
| HandTest | 17 | Pass |
| DeckTest | 3 | Pass |
| BlackjackGameTest | 20 | Pass |
| BlackjackViewTest | 16 | Pass |
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
exercised in Google Chrome at localhost:8082.

- Initial state: `NEW GAME`, 1,000 chips, 52 cards, zero results, an initial
  25-chip selection, and **Deal First Hand** enabled. Hit and Stand are disabled.
- Betting: the field advertises `5–1,000 chips · all-in allowed`; its actual max
  is 1,000. After committing 25 chips, the max becomes the 975-chip spendable
  bankroll and the field remains locked until the hand ends.
- Game lifecycle: a deal changes the heading to `HAND 01`; the next deal keeps
  the bankroll, deck and results, while **New Game** resets them after a completed
  hand. Reset remains unavailable while a wager is active.
- English/Russian routing: a live English hand was switched from `/` to `/ru`.
  Chrome showed the Russian page title, `lang=ru`, `РУКА 01`, localized controls,
  status, card names and number formatting while preserving that hand and bankroll.
- Gameplay smoke test: dealing concealed the hole card and locked the wager;
  Stand revealed the dealer, settled the hand once, updated the counter and showed
  both **Ещё рука** and **Новая игра**.
- Responsive viewports checked: **320×568**, **390×844**, **768×1024**,
  **1280×720**, and **1440×757**. At every size the document dimensions exactly
  matched the viewport and measured bounds found no gameplay control outside it.
  The completed Russian state—with all four action buttons—also fit at 320×568.
- The 320×568 render was visually inspected: hands remain side by side, cards
  overlap safely, the wager field and chip rack remain operable, and the result
  plus all actions stay on screen without horizontal or vertical page scrolling.
- No Chrome warning, error, or uncaught-exception entries were recorded during
  the final route, interaction, and resize checks.

The UI checks above are an interactive smoke test, not a committed automated
browser suite. Rule edge cases, including both-natural pushes, dealer natural,
soft 17, multiple aces, ordinary pushes, and repeated commands, are covered by
the deterministic automated suites. Physical iOS/Android devices and separate
Safari/Firefox engines were not tested.

The temporary test server on port 8082 and temporary browser tabs were stopped
after verification. Existing servers were left running. Stop the older game in
its terminal with Ctrl+C, then run `./run-production.sh` to load the new build.
