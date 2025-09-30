AI Workshop — Vibe Coding Chess (Java 21)

Overview
This repository contains a small chess playground built in plain Java 21 with a minimal rules engine, a CLI, and a lightweight Swing GUI. It is intended for experimentation and workshop exercises rather than full FIDE‑legal chess. The code is intentionally compact and readable, with a touch of functional style where it adds clarity (e.g., stream-based material evaluation).

What we built and why
- Core board model (ChessBoard):
  - 8×8 board, initial starting position, getters/setters.
  - Minimal SAN application for a subset of moves usable for both sides as the game progresses.
  - Simple move generator for pawns and knights only (captures included for knights and diagonal pawn captures; no en passant).
  - Functional-style evaluateMaterial using Java streams.
- Rendering (ChessBoardRenderer):
  - ANSI/Unicode textual board with blue/orange squares and optional from/to highlights.
- GUI (ChessBoardGUI):
  - Small Swing board mirror that updates after each legal move.
- CLI and gameplay loop (Main):
  - Interactive prompt reading SAN moves.
  - Optional Monte Carlo (very lightweight MCTS) move selection for Black on each Black turn.
- Tooling and quality:
  - Gradle project targeting Java 21 (toolchain + --release 21).
  - JUnit 5 tests for the core rules and renderer.
  - Spotless (Google Java Format) and Checkstyle for consistency.
  - Auto-commit after successful builds/tests (opt-out available via Gradle properties).

Simplified rules: what is legal vs. illegal
This engine intentionally supports a strict subset of chess to keep the code focused and approachable. All rules below are implemented by ChessBoard.tryApplySanSimple(String san, boolean whiteToMove).

Supported (legal) moves
- Pawn pushes (both colors):
  - One step forward if the destination square is empty.
  - Two steps forward from the starting rank if both intermediate and destination squares are empty.
  - Examples: e3, e4, d5 (for Black after it’s Black’s turn), a4.
- Pawn captures (both colors):
  - Diagonal one-square capture like exd5 (file letter of the capturing pawn, an "x", then destination square).
  - No en passant, no promotions.
  - Examples: exd5, cxd4, bxa6.
- Knight moves (both colors):
  - Non‑capturing: Nf3, Nc6, etc., destination square must be empty.
  - Capturing: Nxg5, Nxe4, etc., destination square must contain an opponent piece.
  - No disambiguation: moves like Nbd2 are not supported; there must be exactly one knight that can reach the destination.

Unsupported (illegal) in this engine
- Any move notation other than the supported subset above.
- Castling (O-O / O-O-O).
- Bishop, rook, queen, and king moves are not parsed through SAN in this simplified engine.
- En passant, promotions, checks (+), and checkmates (#) have no effect beyond being optionally stripped; they don’t legalize otherwise illegal moves.
- SAN disambiguation (e.g., Nbd2, R1e2), piece suffixes, or annotations.
- Any capture when the target square is empty or any quiet move when the target square is occupied.

Important notes
- The simplified SAN parser is used for ongoing play for both sides (not only from the initial position).
- A second helper, tryApplySanFromInitialWhite, exists for white’s very first move and supports only white pawn pushes and white knight moves; the main loop uses tryApplySanSimple instead.
- Legal move generation (generateSimpleMoves) is limited to pawns and knights and does not check checks/checkmates or other termination conditions.

How to build and run
Prerequisites
- Java 21 available on your machine (Gradle will try to use a Java 21 toolchain).
- On macOS/Linux/WSL, a GUI environment if you want to see the Swing board.

Build
- Run tests and build all artifacts:
  - ./gradlew build
- Run only tests:
  - ./gradlew test

Run (CLI + Swing GUI)
The app has a main class org.example.Main. When you run it, a small Swing window opens to mirror the board. You can drive the game from the terminal using SAN moves from the simplified rule set.

Option A — via Java directly
- After building: ./gradlew build
- Then run:
  - java -cp build/classes/java/main org.example.Main
- To start with exactly one move from the initial position (and exit afterward):
  - java -cp build/classes/java/main org.example.Main e4

Option B — from your IDE
- Import the Gradle project, ensure Java 21 is selected, and run org.example.Main.

Playing the game
- Interactive mode
  - Start without arguments: java -cp build/classes/java/main org.example.Main
  - You’ll get a prompt:
    - White >
  - Enter one SAN move at a time, for example:
    - e4 (white pawn two steps)
    - Nf6 (black knight to f6)
    - exd5 (white pawn from e4 captures a piece on d5)
  - After a legal move:
    - The text board prints in the console with highlighted from/to squares.
    - The Swing window updates to the same position.
    - Turns alternate automatically.
  - By default, when it becomes Black’s turn, a simple Monte Carlo player may pick a move under the same simplified rules. You’ll see a line like:
    - Black (MCTS) plays: e7-e5
  - You can continue entering White’s next move.

- Single-move mode
  - Provide one argument to describe a single move and exit after applying it (only if legal):
    - java -cp build/classes/java/main org.example.Main e4

- What to try
  - Legal examples (from the initial position): e4, d4, a3, h4, Nf3, Nc3; for Black: e5, d5, Nf6, Nc6.
  - Captures once contact is made: exd5, cxd4, Nxe5, Nxd4.
  - Illegal examples (engine will reject): O-O, Bb5, Qh5, Ke2, e5 when e5 is occupied, exd5 when d5 is empty, Nbd2, a8=Q, exd6 e.p.

Renderer and output
- The console board uses Unicode chess glyphs (♖♘♗♕♔♙ / ♜♞♝♛♚♟).
- Background colors: blue for dark squares, orange for light squares. Some terminals without 256‑color support may render orange as a basic color.
- Legend: ( ) marks the origin square, [ ] marks the destination square of the last move.

Developer notes
- Packages: org.example.*
- Key classes:
  - ChessBoard — state, rules, generators, evaluation.
  - ChessBoardRenderer — console rendering.
  - ChessBoardGUI — Swing rendering.
  - MonteCarloPlayer — very small MCTS for choosing a legal simplified move.
  - Main — CLI entry point.
- Tests live in src/test/java and can be run with ./gradlew test.
- Formatting & style: Spotless (Google Java Format) + Checkstyle. Run ./gradlew spotlessApply to apply formatting.
- Auto-commit behavior: after successful build/test, the Gradle task can auto-commit (and optionally push) changes with a descriptive message. You can opt out with -PskipAutoCommit=true and/or -PskipAutoPush=true.

License
- Workshop/educational use. Add an explicit license here if you plan to distribute broadly.
