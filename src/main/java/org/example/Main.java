package org.example;

import java.util.Scanner;

/**
 * Simple CLI tool that accepts a chess move in standard algebraic notation
 * and prints a human-readable description, and (when legal from the initial
 * position) renders a board highlighting origin and destination squares.
 */
public class Main {
    private static String coord(int file, int rank) {
        char f = (char)('a' + file);
        char r = (char)('1' + rank);
        return "" + f + r;
    }
    public static void main(String[] args) {
        // If a move is provided as an arg, accept exactly one move from the initial position
        if (args != null && args.length > 0) {
            if (args.length != 1) {
                System.out.println("Please provide exactly one move at a time (e.g., e4). Multiple moves are not allowed.");
                return;
            }
            String move = args[0].trim();
            if (move.isEmpty() || move.contains(" ")) {
                System.out.println("Please provide exactly one move (no spaces), e.g., e4 or Nf6.");
                return;
            }
            ChessBoard board = ChessBoard.initial();
            boolean whiteToMove = true;
            System.out.println(ChessMoveDescriber.describe(move));
            ChessBoard.MoveResult res = board.tryApplySanSimple(move, whiteToMove);
            if (res.legal) {
                board = res.after;
                System.out.println(ChessBoardRenderer.render(board, res.fromFile, res.fromRank, res.toFile, res.toRank));
            } else {
                System.out.println("(No move made: unsupported or illegal under simplified rules from the initial position.)");
            }
            return;
        }
        // Interactive mode with persistent board and alternating turns
        Scanner scanner = new Scanner(System.in);
        ChessBoard board = ChessBoard.initial();
        boolean whiteToMove = true;
        System.out.println("Enter SAN moves (e.g., d4, Nf6). After each legal move the board updates and turns alternate. Ctrl+D (Unix) or Ctrl+Z (Windows) to exit.");
        while (true) {
            System.out.print((whiteToMove?"White":"Black") + " > ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;
            System.out.println(ChessMoveDescriber.describe(line));
            ChessBoard.MoveResult res = board.tryApplySanSimple(line, whiteToMove);
            if (res.legal) {
                board = res.after;
                System.out.println(ChessBoardRenderer.render(board, res.fromFile, res.fromRank, res.toFile, res.toRank));
                whiteToMove = !whiteToMove;
                // If it's now Black to move, let Black play using MCTS under simplified rules
                if (!whiteToMove) {
                    java.util.Optional<MonteCarloPlayer.MoveChoice> choice = MonteCarloPlayer.chooseMove(board, false, 300);
                    if (choice.isPresent()) {
                        MonteCarloPlayer.MoveChoice mc = choice.get();
                        ChessBoard.SimpleMove mv = mc.move;
                        board = mc.resultingState;
                        System.out.println("Black (MCTS) plays: " + coord(mv.fromFile, mv.fromRank) + "-" + coord(mv.toFile, mv.toRank));
                        System.out.println(ChessBoardRenderer.render(board, mv.fromFile, mv.fromRank, mv.toFile, mv.toRank));
                    } else {
                        System.out.println("Black (MCTS) has no legal simplified moves. Waiting for White's next input.");
                    }
                    whiteToMove = true; // return turn to White
                }
            } else {
                System.out.println("(No move made: unsupported or illegal under simplified rules. Try a pawn push like e4/e5 or a knight move like Nf3/Nf6.)");
            }
        }
        System.out.println("Goodbye.");
    }
}
