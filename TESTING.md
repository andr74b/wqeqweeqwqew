# Blackjack verification

Verified on **7 September 2026**, using Microsoft OpenJDK **25.0.4.1**,
Vaadin **25.2.6**, Spring Boot **4.1.1** and Maven wrapper **3.9.16**.

## Automated results

Command: `./mvnw --batch-mode --no-transfer-progress verify`

**BUILD SUCCESS — 82 tests, 0 failures, 0 errors, 0 skipped.**

| Suite | Cases | Result |
| --- | ---: | --- |
| HandTest | 17 | Pass |
| DeckTest | 3 | Pass |
| BlackjackGameTest | 20 | Pass |
| BlackjackViewTest | 16 | Pass |
| SpanishBlackjackViewTest | 2 | Pass |
| BlackjackBettingTest | 18 | Pass |
| BlackjackShoeTest | 4 | Pass |
| BlackjackGameIT | 1, exercising 2,000 rounds | Pass |
| ProductionPackageIT | 1 | Pass |

The production artifact is `target/blackjack.jar`. The packaging test starts
that JAR in an empty temporary directory, checks production configuration,
uses the desktop port-file handshake, requests all three language routes and
verifies that the card stylesheet is served.

Component expectations reflect the shorter English/Russian messages, Spanish
localization, and the three-language selector. The tests also cover
phase-specific actions. Exact half-chip profits remain visible, and the full
returned amount remains available in the accessible result announcement.
The test fixture retains a strong reference to its UI: Vaadin stores the current
UI weakly, so retaining it prevents intermittent garbage-collection failures.

## Browser checks

The actual production JAR was checked in the Codex in-app browser at
`http://127.0.0.1:8082`, in English and Russian. Spanish route, copy, card names,
and language-selector behavior are covered by component and integration tests.

- Initial state: 1,000 chips, a 25-chip bet, 52 cards, and one primary Deal action.
- Active hand: the stake is deducted, the dealer's second card stays hidden,
  betting controls disappear, and only Hit/Stand are shown.
- Completed hand: the cards remain clear of the betting controls, the result and
  signed profit are shown, and Deal again/New game become available.
- The language selector uses server-side navigation. A live hand with an
  18-point player total, 975 chips and 48 cards remaining retained the same cards,
  balance and deck count when switched from English to Russian.
- Rules open in a scrollable dialog. At 320px width the close button remains
  available while the longer guide scrolls within the dialog.
- Responsive measurements covered 320×568, 390×844, 640×596, 768×1024,
  1280×720 and 1440×900. No horizontal document overflow or intersections between
  the header, introduction, felt, betting controls, result area and footer were
  found. Card bounds stayed within their hand areas.
- At the shortest sizes, completed results can add vertical page scrolling;
  content is no longer forced into overlapping fixed-height rows.
- A temporary static stress fixture used the production stylesheet with eleven
  cards per hand. Card containment and exposed rank corners were checked at
  phone, tablet and desktop widths. At narrow tablet widths, long hands stack to
  keep the rank of every card readable. This was a layout fixture, not a dealt
  game or a new production route.

These are interactive browser checks, not a committed browser test suite.
Physical iOS/Android devices and separate Safari/Firefox engines were not tested.
Automated reports are under `target/surefire-reports` and
`target/failsafe-reports`. The local production preview remains on port 8082.

## Desktop packaging checks

`JAVA_HOME=... npm run dist:mac -- --arch arm64` completed successfully from
the `desktop` directory. It rebuilt and verified the backend, generated a Java
runtime, and produced the `.app`, `.dmg`, and `.zip` outputs.

- The packaged `.app` launched in its own Electron window and used the Java
  executable and production JAR inside its application resources.
- Spring Boot bound to a random `127.0.0.1` port. `/`, `/ru`, and `/es` all
  returned HTTP 200 from the packaged process; no external browser was opened.
- Terminating the application stopped the child Java process and removed the
  temporary port file.
- The runtime was also started independently from its staging directory and
  served all three routes successfully.
- Removing `jlink --bind-services` kept unnecessary JDK development tools out of
  the runtime, reducing it from about 93 MB to 68 MB before packaging.
- The final unsigned Apple Silicon app is about 343 MB unpacked; its DMG and ZIP
  are about 183 MB each. Public downloads still need Developer ID signing and
  Apple notarization, as described in `desktop/README.md`.

Windows installer creation is configured and schema-validated, and the native
Windows build runs in the GitHub Actions matrix. It was not executed on this Mac
because the embedded Java runtime must be generated on the target platform.
