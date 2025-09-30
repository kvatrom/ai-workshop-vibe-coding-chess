package org.example;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to convert a move in Standard Algebraic Notation (SAN) to a human-readable description.
 * This does not validate legality or track board state; it only interprets notation.
 */
public class ChessMoveDescriber {

    private static final Pattern CASTLE_KINGSIDE = Pattern.compile("^O-O[+#]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASTLE_QUEENSIDE = Pattern.compile("^O-O-O[+#]?$", Pattern.CASE_INSENSITIVE);

    // Piece move, with optional disambiguation and capture
    // Examples: Nf3, Nbd2, R1e1, Qh5+, Bxe6, Raxb7#, Nb1xc3
    private static final Pattern PIECE_MOVE = Pattern.compile(
            "^([KQRBN])" +                 // 1: piece
            "([a-h1-8]?)" +               // 2: optional disambiguation (file or rank)
            "(x)?" +                      // 3: capture flag
            "([a-h][1-8])" +              // 4: destination square
            "(=([QRBN]))?" +              // 5: (unused full), 6: promotion piece (rare for piece moves; mostly for pawns)
            "([+#])?$",                   // 7: check/mate
            Pattern.CASE_INSENSITIVE);

    // Pawn move: e4, exd5, e8=Q, exd8=Q+
    private static final Pattern PAWN_MOVE = Pattern.compile(
            "^([a-h])" +                  // 1: pawn file
            "(x)?" +                      // 2: capture flag (if present, next is target file)
            "([a-h])?" +                  // 3: target file when capturing (optional)
            "([1-8])" +                   // 4: target rank
            "(=([QRBN]))?" +              // 5: (unused full), 6: promotion piece
            "([+#])?" +                   // 7: check/mate
            "(?:e\\.p\\.)?$",             // optional en passant marker
            Pattern.CASE_INSENSITIVE);

    public static String describe(String sanInput) {
        if (sanInput == null || sanInput.trim().isEmpty()) {
            return "Please provide a move in standard chess notation (e.g., d4, Nf3, O-O).";
        }
        String san = sanInput.trim();

        // Normalize common unicode/typo variants for castling (0-0 instead of O-O)
        san = san.replace('0', 'O');

        // Castling
        if (CASTLE_KINGSIDE.matcher(san).matches()) {
            boolean check = san.endsWith("+");
            boolean mate = san.endsWith("#");
            return "Castle kingside" + suffixCheckMate(check, mate) + ".";
        }
        if (CASTLE_QUEENSIDE.matcher(san).matches()) {
            boolean check = san.endsWith("+");
            boolean mate = san.endsWith("#");
            return "Castle queenside" + suffixCheckMate(check, mate) + ".";
        }

        // Piece moves
        Matcher pm = PIECE_MOVE.matcher(san);
        if (pm.matches()) {
            String piece = pieceName(pm.group(1));
            String disamb = pm.group(2);
            boolean capture = pm.group(3) != null;
            String dest = pm.group(4);
            String promo = pm.group(6); // extremely rare here
            String end = pm.group(7);

            StringBuilder sb = new StringBuilder();
            sb.append(piece).append(' ');
            if (disamb != null && !disamb.isEmpty()) {
                if (Character.isDigit(disamb.charAt(0))) {
                    sb.append("from rank ").append(disamb).append(' ');
                } else {
                    sb.append("from ").append(fileWord(disamb.charAt(0))).append("-file ");
                }
            }
            if (capture) {
                sb.append("captures on ");
            } else {
                sb.append("moves to ");
            }
            sb.append(squareWord(dest));

            if (promo != null) {
                sb.append(" and promotes to ").append(pieceName(promo));
            }
            if (end != null) {
                sb.append(suffixCheckMate(end.equals("+"), end.equals("#")));
            }
            sb.append('.');
            return sb.toString();
        }

        // Pawn moves
        Matcher paw = PAWN_MOVE.matcher(san);
        if (paw.matches()) {
            char pawnFile = paw.group(1).toLowerCase(Locale.ROOT).charAt(0);
            boolean capture = paw.group(2) != null;
            String targetFile = paw.group(3);
            String targetRank = paw.group(4);
            String promo = paw.group(6);
            String end = paw.group(7);

            StringBuilder sb = new StringBuilder();
            sb.append("Pawn ");
            if (capture) {
                // exd5 means pawn from e-file captures on d5
                sb.append("from ").append(fileWord(pawnFile)).append("-file captures on ");
                String dest = (targetFile != null ? targetFile : String.valueOf(pawnFile)) + targetRank;
                sb.append(squareWord(dest));
            } else {
                String dest = String.valueOf(pawnFile) + targetRank;
                sb.append("moves to ").append(squareWord(dest));
            }
            if (promo != null) {
                sb.append(" and promotes to ").append(pieceName(promo));
            }
            if (end != null) {
                sb.append(suffixCheckMate(end.equals("+"), end.equals("#")));
            }
            sb.append('.');
            return sb.toString();
        }

        // If nothing matched
        return "Unrecognized or unsupported move notation: '" + sanInput + "'. Supported examples: d4, Nf3, exd5, O-O, c8=Q#, Nbd2, R1e1.";
    }

    private static String suffixCheckMate(boolean check, boolean mate) {
        if (mate) return " with checkmate";
        if (check) return " with check";
        return "";
    }

    private static String pieceName(String letter) {
        if (letter == null || letter.isEmpty()) return "";
        switch (Character.toUpperCase(letter.charAt(0))) {
            case 'K': return "King";
            case 'Q': return "Queen";
            case 'R': return "Rook";
            case 'B': return "Bishop";
            case 'N': return "Knight";
            default: return "Piece";
        }
    }

    private static String fileWord(char file) {
        switch (Character.toLowerCase(file)) {
            case 'a': return "a";
            case 'b': return "b";
            case 'c': return "c";
            case 'd': return "d";
            case 'e': return "e";
            case 'f': return "f";
            case 'g': return "g";
            case 'h': return "h";
            default: return String.valueOf(file);
        }
    }

    private static String squareWord(String square) {
        if (square == null || square.length() != 2) return square;
        char file = square.toLowerCase(Locale.ROOT).charAt(0);
        char rank = square.charAt(1);
        return file + String.valueOf(rank);
    }
}
