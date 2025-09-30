package org.example;

import java.util.Locale;

/**
 * Minimal chess board model starting from the standard initial position.
 * Supports applying a subset of SAN moves for White only (first move from the initial position):
 * - Pawn pushes: a3/a4 .. h3/h4 (no captures, no promotions)
 * - Knight moves: Na3, Nc3, Nd2, Ne2, Nf3, Nh3 (from b1 or g1)
 * If a move is legal under these constraints, returns a MoveResult containing origin/destination
 * and the board after the move. Otherwise returns illegal result.
 */
public class ChessBoard {
    // Board is [rank][file] with 0-based indexing: rank 0 = 1st rank, file 0 = 'a'.
    private final char[][] board = new char[8][8];

    // Simple move model for internal engines (not SAN)
    public static class SimpleMove {
        public final int fromFile, fromRank, toFile, toRank;
        public SimpleMove(int fromFile, int fromRank, int toFile, int toRank) {
            this.fromFile = fromFile; this.fromRank = fromRank; this.toFile = toFile; this.toRank = toRank;
        }
    }

    /**
     * Apply a very simple subset of SAN for either side on the current board state.
     * Supported:
     *  - Pawn pushes: one or two squares forward from starting rank, no captures, no promotions.
     *  - Knight non-capturing moves: N<dest>. Captures and disambiguation are not supported.
     * Ignores trailing + or #. Returns MoveResult with updated board when legal.
     */
    public MoveResult tryApplySanSimple(String sanInput, boolean whiteToMove) {
        if (sanInput == null) return MoveResult.illegal();
        String san = sanInput.trim();
        if (san.isEmpty()) return MoveResult.illegal();
        san = san.replace('0', 'O');

        // Strip trailing check/mate
        if (san.endsWith("+") || san.endsWith("#")) {
            san = san.substring(0, san.length()-1);
        }

        // Knights: N<square> or Nx<square> (captures supported, no disambiguation)
        if (Character.toUpperCase(san.charAt(0)) == 'N') {
            boolean wantsCapture = san.contains("x") || san.contains("X");
            String dest = san.substring(1).replace("x", "").replace("X", "");
            if (!isValidSquare(dest)) return MoveResult.illegal();
            int toFile = fileIndex(dest.charAt(0));
            int toRank = rankIndex(dest.charAt(1));
            char target = getAt(toFile, toRank);
            char knightChar = whiteToMove ? 'N' : 'n';
            // Validate occupancy according to capture flag
            if (wantsCapture) {
                if (target == ' ') return MoveResult.illegal();
                if (whiteToMove ? isWhite(target) : isBlack(target)) return MoveResult.illegal();
            } else {
                if (target != ' ') return MoveResult.illegal();
            }
            int fromFile = -1, fromRank = -1, matches = 0;
            for (int r = 0; r < 8; r++) {
                for (int f = 0; f < 8; f++) {
                    if (getAt(f, r) == knightChar && knightCanMove(f, r, toFile, toRank)) {
                        fromFile = f; fromRank = r; matches++;
                    }
                }
            }
            if (matches != 1) return MoveResult.illegal(); // ambiguous or none
            ChessBoard after = copy();
            after.setAt(fromFile, fromRank, ' ');
            after.setAt(toFile, toRank, knightChar);
            return MoveResult.legal(fromFile, fromRank, toFile, toRank, after);
        }

        // Pawn captures: exd5 style (no en-passant)
        if (san.length() == 4 && Character.isLetter(san.charAt(0)) && (san.charAt(1)=='x' || san.charAt(1)=='X') && Character.isLetter(san.charAt(2)) && Character.isDigit(san.charAt(3))) {
            int fromFile = fileIndex(san.charAt(0));
            int toFile = fileIndex(san.charAt(2));
            int toRank = rankIndex(san.charAt(3));
            int dir = whiteToMove ? 1 : -1;
            int fromRank = toRank - dir;
            if (fromFile < 0 || fromFile > 7 || toFile < 0 || toFile > 7 || toRank < 0 || toRank > 7 || fromRank < 0 || fromRank > 7) return MoveResult.illegal();
            if (Math.abs(toFile - fromFile) != 1) return MoveResult.illegal();
            char pawnChar = whiteToMove ? 'P' : 'p';
            if (getAt(fromFile, fromRank) != pawnChar) return MoveResult.illegal();
            char target = getAt(toFile, toRank);
            if (target == ' ') return MoveResult.illegal();
            if (whiteToMove ? isWhite(target) : isBlack(target)) return MoveResult.illegal();
            ChessBoard after = copy();
            after.setAt(fromFile, fromRank, ' ');
            after.setAt(toFile, toRank, pawnChar);
            return MoveResult.legal(fromFile, fromRank, toFile, toRank, after);
        }

        // Pawn pushes: <file><rank>, no captures
        if (san.length() == 2 && Character.isLetter(san.charAt(0)) && Character.isDigit(san.charAt(1))) {
            char fileChar = Character.toLowerCase(san.charAt(0));
            if (fileChar < 'a' || fileChar > 'h') return MoveResult.illegal();
            int toFile = fileIndex(fileChar);
            int toRank = rankIndex(san.charAt(1));
            int dir = whiteToMove ? 1 : -1;
            int startRank = whiteToMove ? 1 : 6; // indices: white pawns on rank 2 (1), black on rank 7 (6)
            char pawnChar = whiteToMove ? 'P' : 'p';

            // Destination must be empty for pawn push
            if (getAt(toFile, toRank) != ' ') return MoveResult.illegal();

            // One-step move
            int fromRankOne = toRank - dir;
            if (fromRankOne >= 0 && fromRankOne < 8 && getAt(toFile, fromRankOne) == pawnChar) {
                ChessBoard after = copy();
                after.setAt(toFile, fromRankOne, ' ');
                after.setAt(toFile, toRank, pawnChar);
                return MoveResult.legal(toFile, fromRankOne, toFile, toRank, after);
            }
            // Two-step move
            int fromRankTwo = toRank - 2*dir;
            int midRank = toRank - dir;
            if (fromRankTwo >= 0 && fromRankTwo < 8 && getAt(toFile, fromRankTwo) == pawnChar && fromRankTwo == startRank && getAt(toFile, midRank) == ' ') {
                ChessBoard after = copy();
                after.setAt(toFile, fromRankTwo, ' ');
                after.setAt(toFile, toRank, pawnChar);
                return MoveResult.legal(toFile, fromRankTwo, toFile, toRank, after);
            }
            return MoveResult.illegal();
        }

        // Not supported
        return MoveResult.illegal();
    }

    public static class MoveResult {
        public final boolean legal;
        public final int fromFile; // 0..7 (a..h)
        public final int fromRank; // 0..7 (1..8)
        public final int toFile;   // 0..7
        public final int toRank;   // 0..7
        public final ChessBoard after;

        private MoveResult(boolean legal, int fromFile, int fromRank, int toFile, int toRank, ChessBoard after) {
            this.legal = legal;
            this.fromFile = fromFile;
            this.fromRank = fromRank;
            this.toFile = toFile;
            this.toRank = toRank;
            this.after = after;
        }

        public static MoveResult illegal() {
            return new MoveResult(false, -1, -1, -1, -1, null);
        }

        public static MoveResult legal(int fromFile, int fromRank, int toFile, int toRank, ChessBoard after) {
            return new MoveResult(true, fromFile, fromRank, toFile, toRank, after);
        }
    }

    public static ChessBoard initial() {
        ChessBoard b = new ChessBoard();
        b.setupInitial();
        return b;
    }

    private ChessBoard copy() {
        ChessBoard c = new ChessBoard();
        for (int r = 0; r < 8; r++) {
            System.arraycopy(this.board[r], 0, c.board[r], 0, 8);
        }
        return c;
    }

    private void setupInitial() {
        // Clear
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                board[r][f] = ' ';
            }
        }
        // White pieces
        char[] back = new char[]{'R','N','B','Q','K','B','N','R'};
        for (int f = 0; f < 8; f++) {
            board[0][f] = back[f]; // rank 1
            board[1][f] = 'P';     // rank 2
        }
        // Black pieces (not really needed for first white move, but included for completeness)
        char[] backBlack = new char[]{'r','n','b','q','k','b','n','r'};
        for (int f = 0; f < 8; f++) {
            board[6][f] = 'p';     // rank 7
            board[7][f] = backBlack[f]; // rank 8
        }
    }

    public char getAt(int file, int rank) { // file 0..7, rank 0..7
        return board[rank][file];
    }

    public void setAt(int file, int rank, char piece) {
        board[rank][file] = piece;
    }

    /**
     * Try to apply a SAN move from the initial position for White's first move only.
     * Supported: pawn pushes (one/two squares, same file, no capture), knight moves (from b1/g1).
     */
    public MoveResult tryApplySanFromInitialWhite(String sanInput) {
        if (sanInput == null) return MoveResult.illegal();
        String san = sanInput.trim();
        if (san.isEmpty()) return MoveResult.illegal();
        san = san.replace('0', 'O'); // normalize zero

        // Reject castling and any capture, check, mate, promotion annotations
        if (san.equalsIgnoreCase("O-O") || san.equalsIgnoreCase("O-O-O")) return MoveResult.illegal();
        if (san.contains("x") || san.contains("= ") || san.contains("=") ) return MoveResult.illegal();

        // Knight move: N<dest>, optionally with check/mate suffix
        if (Character.toUpperCase(san.charAt(0)) == 'N') {
            String dest = stripSuffix(san.substring(1));
            if (!isValidSquare(dest)) return MoveResult.illegal();
            int toFile = fileIndex(dest.charAt(0));
            int toRank = rankIndex(dest.charAt(1));
            // Possible origins for white knights at start: b1 (1,0) and g1 (6,0)
            int[][] origins = new int[][]{{1,0},{6,0}};
            for (int[] o : origins) {
                if (knightCanMove(o[0], o[1], toFile, toRank) && getAt(o[0], o[1])=='N' && getAt(toFile,toRank)==' ') {
                    ChessBoard after = copy();
                    after.setAt(o[0], o[1], ' ');
                    after.setAt(toFile, toRank, 'N');
                    return MoveResult.legal(o[0], o[1], toFile, toRank, after);
                }
            }
            return MoveResult.illegal();
        }

        // Pawn push: <file><rank> possibly with + or # at end (ignored)
        if (san.length() >= 2 && san.length() <= 3 && Character.isLetter(san.charAt(0)) && Character.isDigit(san.charAt(1))) {
            char fileChar = Character.toLowerCase(san.charAt(0));
            if (fileChar < 'a' || fileChar > 'h') return MoveResult.illegal();
            int targetRankDigit = san.charAt(1) - '0';
            if (targetRankDigit < 1 || targetRankDigit > 8) return MoveResult.illegal();
            // ignore trailing + or #
            if (san.length()==3 && !(san.charAt(2)=='+' || san.charAt(2)=='#')) return MoveResult.illegal();

            int f = fileIndex(fileChar);
            int toRank = targetRankDigit - 1;
            int fromRank = 1; // white pawn starts at rank 2 -> index 1

            if (getAt(f, fromRank) != 'P') return MoveResult.illegal();

            // One-step to rank 3 (index 2)
            if (toRank == 2 && getAt(f,2)==' ') {
                ChessBoard after = copy();
                after.setAt(f, fromRank, ' ');
                after.setAt(f, toRank, 'P');
                return MoveResult.legal(f, fromRank, f, toRank, after);
            }
            // Two-step to rank 4 (index 3), must be clear at 2 and 3
            if (toRank == 3 && getAt(f,2)==' ' && getAt(f,3)==' ') {
                ChessBoard after = copy();
                after.setAt(f, fromRank, ' ');
                after.setAt(f, toRank, 'P');
                return MoveResult.legal(f, fromRank, f, toRank, after);
            }
            // One-step to rank 2 (index 1) or others are illegal at first move
            return MoveResult.illegal();
        }

        return MoveResult.illegal();
    }

    private static String stripSuffix(String s) {
        s = s.trim();
        if (s.endsWith("+") || s.endsWith("#")) {
            return s.substring(0, s.length()-1);
        }
        return s;
    }

    private static boolean isValidSquare(String sq) {
        if (sq == null || sq.length()!=2) return false;
        char f = Character.toLowerCase(sq.charAt(0));
        char r = sq.charAt(1);
        return f>='a' && f<='h' && r>='1' && r<='8';
    }

    private static int fileIndex(char file) {
        char f = Character.toLowerCase(file);
        return f - 'a';
    }

    private static int rankIndex(char rankChar) {
        return (rankChar - '1');
    }

    private static boolean knightCanMove(int fromFile, int fromRank, int toFile, int toRank) {
        int df = Math.abs(toFile - fromFile);
        int dr = Math.abs(toRank - fromRank);
        return (df==1 && dr==2) || (df==2 && dr==1);
    }

    public boolean isWhite(char p) { return p >= 'A' && p <= 'Z'; }
    public boolean isBlack(char p) { return p >= 'a' && p <= 'z'; }

    // Generate simplified pseudo-legal moves: pawns (pushes and diagonal captures, no en-passant), knights (including captures).
    // No check/termination rules considered.
    public java.util.List<SimpleMove> generateSimpleMoves(boolean whiteToMove) {
        java.util.ArrayList<SimpleMove> moves = new java.util.ArrayList<>();
        if (whiteToMove) {
            // White pawns
            for (int f = 0; f < 8; f++) {
                for (int r = 0; r < 8; r++) {
                    char p = getAt(f, r);
                    if (p == 'P') {
                        int toR = r + 1;
                        if (toR < 8 && getAt(f, toR) == ' ') {
                            moves.add(new SimpleMove(f, r, f, toR));
                            if (r == 1 && getAt(f, r + 2) == ' ') {
                                moves.add(new SimpleMove(f, r, f, r + 2));
                            }
                        }
                        // captures
                        if (toR < 8) {
                            if (f - 1 >= 0 && isBlack(getAt(f - 1, toR))) moves.add(new SimpleMove(f, r, f - 1, toR));
                            if (f + 1 < 8 && isBlack(getAt(f + 1, toR))) moves.add(new SimpleMove(f, r, f + 1, toR));
                        }
                    } else if (p == 'N') {
                        addKnightMoves(moves, f, r, true);
                    }
                }
            }
        } else {
            // Black pawns
            for (int f = 0; f < 8; f++) {
                for (int r = 0; r < 8; r++) {
                    char p = getAt(f, r);
                    if (p == 'p') {
                        int toR = r - 1;
                        if (toR >= 0 && getAt(f, toR) == ' ') {
                            moves.add(new SimpleMove(f, r, f, toR));
                            if (r == 6 && getAt(f, r - 2) == ' ') {
                                moves.add(new SimpleMove(f, r, f, r - 2));
                            }
                        }
                        // captures
                        if (toR >= 0) {
                            if (f - 1 >= 0 && isWhite(getAt(f - 1, toR))) moves.add(new SimpleMove(f, r, f - 1, toR));
                            if (f + 1 < 8 && isWhite(getAt(f + 1, toR))) moves.add(new SimpleMove(f, r, f + 1, toR));
                        }
                    } else if (p == 'n') {
                        addKnightMoves(moves, f, r, false);
                    }
                }
            }
        }
        return moves;
    }

    private void addKnightMoves(java.util.List<SimpleMove> moves, int f, int r, boolean white) {
        int[][] d = new int[][]{{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        for (int[] m: d) {
            int tf = f + m[0];
            int tr = r + m[1];
            if (tf < 0 || tf >= 8 || tr < 0 || tr >= 8) continue;
            char target = getAt(tf, tr);
            if (target == ' ' || (white ? isBlack(target) : isWhite(target))) {
                moves.add(new SimpleMove(f, r, tf, tr));
            }
        }
    }

    public ChessBoard apply(SimpleMove move) {
        ChessBoard after = copy();
        char piece = after.getAt(move.fromFile, move.fromRank);
        after.setAt(move.fromFile, move.fromRank, ' ');
        after.setAt(move.toFile, move.toRank, piece);
        return after;
    }

    public int evaluateMaterial() {
        int score = 0; // positive = White ahead
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                char p = getAt(f, r);
                int v = 0;
                switch (Character.toLowerCase(p)) {
                    case 'p': v = 100; break;
                    case 'n': v = 320; break;
                    case 'b': v = 330; break;
                    case 'r': v = 500; break;
                    case 'q': v = 900; break;
                    case 'k': v = 0; break;
                    default: v = 0;
                }
                if (isWhite(p)) score += v; else if (isBlack(p)) score -= v;
            }
        }
        return score;
    }
}
