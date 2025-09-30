package org.example;

import java.util.Scanner;

/**
 * Simple CLI tool that accepts a chess move in standard algebraic notation
 * and prints a human-readable description, and (when legal from the initial
 * position) renders a board highlighting origin and destination squares.
 */
public class Main {
    public static void main(String[] args) {
        // If moves are provided as args, treat them as a sequence starting from the initial position
        if (args != null && args.length > 0) {
            ChessBoard board = ChessBoard.initial();
            boolean whiteToMove = true;
            for (String move : args) {
                String trimmed = move.trim();
                if (trimmed.isEmpty()) continue;
                System.out.println(ChessMoveDescriber.describe(trimmed));
                ChessBoard.MoveResult res = board.tryApplySanSimple(trimmed, whiteToMove);
                if (res.legal) {
                    board = res.after;
                    System.out.println(ChessBoardRenderer.render(board, res.fromFile, res.fromRank, res.toFile, res.toRank));
                    whiteToMove = !whiteToMove;
                } else {
                    System.out.println("(No move made: unsupported or illegal under simplified rules. It is still " + (whiteToMove?"White":"Black") + " to move.)");
                }
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
            } else {
                System.out.println("(No move made: unsupported or illegal under simplified rules. Try a pawn push like e4/e5 or a knight move like Nf3/Nf6.)");
            }
        }
        System.out.println("Goodbye.");
    }
}
