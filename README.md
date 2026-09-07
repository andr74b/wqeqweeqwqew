# Blackjack · The Little Card Club

A responsive, single-player Blackjack game built with **Java 25**, **Vaadin Flow
25.2.6**, and **Spring Boot 4.1.1**. The UI uses Java Vaadin components and ordinary
CSS playing cards. All rules and hidden cards stay on the server. Build a
1,000-chip virtual bankroll while playing through a persistent 52-card deck.
English is served at `/`, Russian at `/ru`, and Spanish at `/es`; the in-game
language selector changes language without resetting the current game.

## Desktop application for Windows and macOS

The same Java and Vaadin application can be distributed as a normal desktop app.
The installer contains its own Java 25 runtime and opens the game in a dedicated
Electron window: users do not see a localhost address, do not need an external
browser and do not install Java separately. The window starts and stops the
embedded Spring Boot server automatically.

After changing any application code, build a fresh installer with:

```sh
cd desktop
npm ci
npm run dist
```

The command tests and packages the backend before producing the current
platform's installer under `desktop/dist`. Builds must run on their target
platform because the bundled JVM is native. Windows produces `.exe` and `.msi`;
macOS produces `.app` inside a `.dmg` plus a `.zip`. See
[desktop/README.md](desktop/README.md) for prerequisites, per-platform commands,
CI builds, release versioning, code signing, notarization and troubleshooting.

## Run from the terminal (macOS)

1. Open Terminal and enter the project directory:

   ```sh
   cd /Users/user1/Desktop/AI/wqeqweeqwqew
   ```

2. Select Java 25 and confirm it is available:

   ```sh
   export JAVA_HOME=$(/usr/libexec/java_home -v 25)
   export PATH="$JAVA_HOME/bin:$PATH"
   java -version
   ```

   This laptop already has Microsoft OpenJDK 25 installed. The output should
   start with `openjdk version "25..."`. On other systems, install a JDK 25 first.
   No preview features are needed.

3. Build, run all tests, and start the production application:

   ```sh
   ./run-production.sh
   ```

   Wait for `Started Main` in the terminal. The script stops if the build or any
   test fails; it cannot silently start an old JAR. The included Maven wrapper
   installs Maven automatically, and Vaadin manages its supported Node version
   for the frontend build. Initial downloads need internet access. No separate
   Maven, npm, or Node installation is required.

4. Open **http://localhost:8080** in a browser. Click **Deal** to play.
   Russian and Spanish are available at **http://localhost:8080/ru** and
   **http://localhost:8080/es**, or from the language selector.

5. To stop the server, press **Ctrl+C** in the terminal that is running it.

## After changing Java, CSS, or configuration

1. Save your changes.
2. Stop the running server with **Ctrl+C**.
3. In the project directory, run the launcher again:

   ```sh
   ./run-production.sh
   ```

4. Refresh the browser after the server starts. If an old style remains cached,
   use **Cmd+Shift+R**. Rebuilding does not update a JAR that is already running;
   stopping and restarting is required.

The equivalent manual build and run commands are:

```sh
./mvnw clean verify
java -jar target/blackjack.jar
```

Only run the second command if the build reports `BUILD SUCCESS`. To restart
the already-built game without rebuilding, run just the `java -jar` command.
On Windows, use `mvnw.cmd clean verify` and the same `java -jar` command.

If port 8080 is already in use, stop the other server or use a different port:

```sh
PORT=8081 ./run-production.sh
# Or, with an existing build:
java -jar target/blackjack.jar --server.port=8081
```

Then open **http://localhost:8081**. The script accepts additional Spring Boot
arguments, for example `./run-production.sh --server.address=127.0.0.1`.

## Run with Docker

Build the production image and run it locally:

```sh
docker build -t blackjack .
docker run --rm -p 8080:10000 blackjack
```

Then open **http://localhost:8080**. The image uses a Java 25 build stage and a
smaller Java 25 runtime stage. It listens on the `PORT` environment variable,
which makes it directly compatible with a Render Docker web service.

## Deploy on Render

1. Push this repository to GitHub.
2. In Render, create a **Web Service** from the GitHub repository.
3. Select **Docker** as the language/runtime. The root-level `Dockerfile` is
   detected automatically; no build or start command is required.
4. Use `/` as the health check path and deploy.

Render supplies `PORT` automatically. Game state is held in memory, so a deploy,
restart, or instance replacement resets active games and bankrolls.

## Rules and controls

- One **game** starts with 1,000 chips and contains many **hands**. The heading
  shows `New game` before the first deal and `Hand 01`, `Hand 02`, and so on
  afterward. **Deal again** keeps the bankroll, deck, and results. After a
  completed hand, **New game** explicitly resets all of them.
- Choose your stake using the **5 / 25 / 100 chip buttons** (each adds to the
  selected bet) or the **Your bet** field. **Clear** removes the selection.
  Bets start at 5 chips, use increments of 5, and can be as high as the current
  bankroll rounded down to that step. An all-in bet is allowed; there is no
  arbitrary fixed maximum.
- **Deal / Deal again** commits the selected stake and deals alternately to the player
  and dealer. The stake is deducted before dealing, and cannot change during a
  hand. The dealer's second card stays face down during the player's turn.
- The **same 52-card deck persists across hands**. Dealt cards are not returned
  between hands. The table shows the deck number and cards remaining, including
  the fact that the hole card has been drawn, without revealing its identity.
  At fewer than **20 cards remaining**, a fresh shuffled deck is prepared before
  the next deal. A cut notice and shuffle notice explain the transition. Exactly
  20 cards allows another hand. There is never a mid-hand shuffle; the reserve
  is sufficient for both hands in this single-player ruleset.
- Number cards have their printed value; J, Q, and K are 10. An ace is 11 when
  possible, otherwise 1. Multiple aces are adjusted automatically. A **Soft**
  score means one ace still counts as 11.
- A two-card ace + ten-value card is a **natural blackjack**. Naturals settle
  immediately: player natural wins, dealer natural loses, and two naturals push.
  A natural outranks a 21 made with more cards.
- **Hit** draws one card. Going over 21 loses immediately, without the dealer
  drawing. Reaching 21 automatically stands and resolves the dealer's turn.
- **Stand** reveals the dealer's hand. The dealer hits below 17 and stands on
  **all 17s, including soft 17**. Dealer busts lose; otherwise the higher total
  wins. Equal totals are a **push**, meaning neither side wins.
- The deal button is available before the first hand and after a result. It is
  hidden during a hand. Hit and Stand appear only during the player's turn.
  The rules engine also rejects out-of-turn commands and repeated result actions.
- Start with **1,000 virtual chips**. A normal win pays **1:1** profit; blackjack
  pays **3:2**; a push returns the stake; a loss forfeits it. A 25-chip win returns
  50 chips including the original stake. A 5-chip blackjack returns 12.5 chips,
  for 7.5 profit. Accounting uses integer half-chip units, so no payout is rounded.
- Results show the hand's net profit/loss. The amount returned, including the
  stake, is available in the payout tooltip and screen-reader announcement. The bankroll displays spendable chips; the gold table chip shows the
  wager. Game profit includes the stake still in play until a hand settles.
- A bankroll below the 5-chip table minimum ends the game. **New game** then
  resets the bankroll, deck and counters. The same reset is also available after
  any completed hand; it is never allowed while a wager is still in play.
- Wins, losses, and pushes accumulate in the current game. These chips have no
  monetary value. Splitting, doubling, surrender, and insurance are not included.
  There is no five-card automatic-win rule.
- The interface focuses on the cards, balance and current action. The **Rules**
  button opens a short guide in each supported language. Betting controls appear between
  hands; Hit and Stand appear while playing. Result messages stay concise.
- Phones stack the hands vertically. Larger screens place them side by side;
  hands with seven or more cards stack at widths up to 900px so card ranks remain
  readable. Standard phone and desktop screens fit the game comfortably. Short
  screens, longer messages and enlarged text may scroll rather than clip content.
- Buttons support keyboard Tab navigation and Enter/Space activation. Cards
  have readable suit/rank labels, results use a polite live region, and focus
  moves to the next usable control without jumping the page.

## Tests

Run the fast rules and Vaadin component tests:

```sh
./mvnw test
```

Run the full production build and integration suite:

```sh
./mvnw clean verify
```

The suite contains **80 unit/component cases and 2 integration tests**:

- `HandTest`: ace revaluation, multiple aces, soft/hard totals, natural vs.
  multi-card 21, busts, immutable hands, and invalid card data.
- `DeckTest`: 52 unique cards, ordered dealing, seeded shuffling, defensive
  copying, and explicit exhaustion without recycling cards.
- `BlackjackGameTest`: dealing, concealed dealer state, naturals, hit/stand,
  automatic stand on 21, dealer soft/hard rules, every result, invalid actions,
  new rounds, statistics, independent games, and Java session serialization.
- `BlackjackBettingTest`: exact stake deductions and every payout, 3:2 half-chip
  precision, invalid and unaffordable bets, repeated settlement protection,
  bankruptcy, fractional remainders, dynamic all-in limits, and explicit game reset.
- `BlackjackShoeTest`: cards persist across rounds, all draws reduce the deck,
  the exact cut boundary, between-round-only shuffles, and failed replacements
  leaving bankroll and previous results unchanged.
- `BlackjackViewTest`: actual Vaadin buttons and listeners connected to fixed
  decks; checks card concealment, control states, score/results, reset behavior,
  accessible localized card labels, per-tab state across language routes, chip
  controls, stake validation, bankroll displays, and game resets. No paid testing license needed.
- `SpanishBlackjackViewTest`: Spanish copy, route behavior, localized cards and
  the English/Russian/Spanish language selector.
- `BlackjackGameIT`: **2,000 reproducible rounds** across 100 independent tables,
  checked against an independent scoring and bankroll calculation. Checks card
  uniqueness across whole decks, including multiple rounds and reshuffles.
- `ProductionPackageIT`: inspects the built JAR, starts it with the desktop
  launcher handshake on a temporary port, verifies production mode, requests all
  three language routes, and validates the responsive stylesheet. It shuts down
  its own process afterwards.

Reports are written to `target/surefire-reports` and `target/failsafe-reports`.
The real production UI was also checked in the browser; see [TESTING.md](TESTING.md).

## Structure and state ownership

```text
src/main/java/org/example/
  Main.java                    Spring Boot entry point and stylesheet registration
  game/
    Card.java, Rank.java, Suit.java   Immutable card data
    Hand.java                  Immutable hand and ace-aware scoring
    Chips.java                 Exact immutable half-chip value
    Deck.java                  Finite shuffled deck; ordered decks for testing
    BlackjackGame.java         Rules, bankroll, payouts, deck lifecycle, and statistics
  ui/
    BlackjackView.java          Shared Vaadin composition, rendering, and controls
    RussianBlackjackView.java   Russian `/ru` route using the shared game UI
    SpanishBlackjackView.java   Spanish `/es` route using the shared game UI
    GameMessages.java           Resource-bundle localization boundary
    PlayingCard.java            Reusable accessible HTML card component
src/main/resources/i18n/       English, Russian, and Spanish UI copy
src/main/resources/META-INF/resources/
  styles/blackjack.css          Scoped responsive styling
  favicon.svg                  Local spade icon
src/test/java/                 Rules, component, and integration tests
```

Each Vaadin `UI`—normally one browser tab—owns one `BlackjackGame`. The language buttons navigate within Vaadin and explicitly carry the current
game into the destination route, including after a refresh. Changing language
preserves the hand and bankroll. There is **no application-wide mutable singleton or static game
state**. Different users and different tabs have independent hands, decks, and
results, even when tabs share an HTTP session. Vaadin's session lock serializes
that session's requests. The core game is plain
Java, with immutable records for cards, hands, statistics, and visible snapshots.
The hole card is excluded from snapshots until the round is over; it is never
sent as hidden HTML or a hidden numeric total.

State is held in memory for the view's lifetime. Vaadin's `@PreserveOnRefresh`
keeps the same hand, bankroll, deck and bet across a reload of the same tab.
Opening a separate tab starts an independent game. Closing the tab, HTTP
session expiry, or a server restart can end that game; no database or
long-term bankroll storage is configured. Request handling uses Spring Boot's virtual-thread support; game actions
are short and synchronous, so they need neither background executors nor push.

## Production and optional development mode

Production mode is the default. The deployable JAR contains its frontend assets
and excludes Vaadin development tools, Copilot, DevTools, and test libraries.
After building, only Java 25 is needed to run it.

For an explicit local development session:

```sh
./mvnw -Pdevelopment spring-boot:run
```

This profile enables Vaadin development tooling only for its run. For a reliable
edit/rebuild cycle, stop it with Ctrl+C and rerun after changes; Java hot reload
is not configured. Use `./run-production.sh` for normal runs.

Framework references: [Vaadin stylesheet API](https://vaadin.com/docs/latest/styling/stylesheets),
[UI/session scopes](https://vaadin.com/docs/latest/flow/integrations/spring/scopes),
and [production builds](https://vaadin.com/docs/latest/flow/production/production-build).
Payout reference: [Bicycle's Blackjack rules](https://bicyclecards.com/how-to-play/blackjack/).
