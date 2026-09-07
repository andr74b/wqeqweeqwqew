# Blackjack · The Little Card Club

A responsive, single-player Blackjack game built with **Java 25**, **Vaadin Flow
25.2.6**, and **Spring Boot 4.1.1**. The UI uses Java Vaadin components and ordinary
CSS playing cards. All rules and hidden cards stay on the server. Build a
1,000-chip virtual bankroll while playing through a persistent 52-card deck.

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

4. Open **http://localhost:8080** in a browser. Click **New Round** to deal.

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

- Choose your stake using the **5 / 25 / 100 chip buttons** (each adds to the
  selected bet) or the **Your bet** field. **Clear** removes the selection.
  Bets must be 5–500 chips in increments of 5 and cannot exceed your bankroll.
- **New Round** commits the selected stake and deals alternately to the player
  and dealer. The stake is deducted before dealing, and cannot change during a
  hand. The dealer's second card stays face down during the player's turn.
- The **same 52-card deck persists across rounds**. Dealt cards are not returned
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
- **New Round** is available before the first deal and after a result. It is
  disabled during a hand. Hit and Stand are disabled outside the player's turn.
  The rules engine also rejects out-of-turn commands and repeated result actions.
- Start with **1,000 virtual chips**. A normal win pays **1:1** profit; blackjack
  pays **3:2**; a push returns the stake; a loss forfeits it. A 25-chip win returns
  50 chips including the original stake. A 5-chip blackjack returns 12.5 chips,
  for 7.5 profit. Accounting uses integer half-chip units, so no payout is rounded.
- Results show the hand's net profit/loss and the amount returned, including the
  stake. The bankroll displays spendable chips; the gold table chip shows the
  wager. Session profit includes the stake still in play until a hand settles.
- A bankroll below the 5-chip table minimum ends the session. **New Session**
  then explicitly resets the bankroll, deck and counters. There is no automatic
  refill, and a healthy session cannot be reset using that command.
- Wins, losses, and pushes accumulate in the current tab. These chips have no
  monetary value. Splitting, doubling, surrender, and insurance are not included.
  There is no five-card automatic-win rule.
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

The suite contains **74 unit/component cases and 2 integration tests**:

- `HandTest`: ace revaluation, multiple aces, soft/hard totals, natural vs.
  multi-card 21, busts, immutable hands, and invalid card data.
- `DeckTest`: 52 unique cards, ordered dealing, seeded shuffling, defensive
  copying, and explicit exhaustion without recycling cards.
- `BlackjackGameTest`: dealing, concealed dealer state, naturals, hit/stand,
  automatic stand on 21, dealer soft/hard rules, every result, invalid actions,
  new rounds, statistics, independent games, and Java session serialization.
- `BlackjackBettingTest`: exact stake deductions and every payout, 3:2 half-chip
  precision, invalid and unaffordable bets, repeated settlement protection,
  bankruptcy, fractional remainders, and explicit session restart.
- `BlackjackShoeTest`: cards persist across rounds, all draws reduce the deck,
  the exact cut boundary, between-round-only shuffles, and failed replacements
  leaving bankroll and previous results unchanged.
- `BlackjackViewTest`: actual Vaadin buttons and listeners connected to fixed
  decks; checks card concealment, control states, score/results, reset behavior,
  accessible card labels, independent views, chip controls, stake validation,
  bankroll displays, and the bankruptcy/restart flow. No paid testing license needed.
- `BlackjackGameIT`: **2,000 reproducible rounds** across 100 independent tables,
  checked against an independent scoring and bankroll calculation. Checks card
  uniqueness across whole decks, including multiple rounds and reshuffles.
- `ProductionPackageIT`: inspects the built JAR, starts it on a temporary port
  from an empty directory, verifies production mode, and requests the page and
  responsive card stylesheet. It shuts down its own process afterwards.

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
    BlackjackView.java          Vaadin route, rendering, controls, result copy
    PlayingCard.java            Reusable accessible HTML card component
src/main/resources/META-INF/resources/
  styles/blackjack.css          Scoped responsive styling
  favicon.svg                  Local spade icon
src/test/java/                 Rules, component, and integration tests
```

Each `BlackjackView` creates its own `BlackjackGame`. There is **no shared mutable
singleton or static game state**. Different users and different tabs have
independent hands, decks, and results, even when tabs share an HTTP session.
Vaadin's session lock serializes that session's requests. The core game is plain
Java, with immutable records for cards, hands, statistics, and visible snapshots.
The hole card is excluded from snapshots until the round is over; it is never
sent as hidden HTML or a hidden numeric total.

State is held in memory for the view's lifetime. Vaadin's `@PreserveOnRefresh`
keeps the same hand, bankroll, deck and bet across a reload of the same tab.
Opening a separate tab starts an independent session. Closing the tab, HTTP
session expiry, or a server restart can end that session; no database or
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
