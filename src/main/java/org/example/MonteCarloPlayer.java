package org.example;

import java.util.*;

/**
 * Very small Monte Carlo Tree Search player using simplified move rules from ChessBoard.
 * - No draw or check/checkmate detection.
 * - Uses only simple moves (pawns: pushes + diagonal captures, knights: all incl. captures).
 * - Rollouts are random with fixed depth. Evaluation is material balance.
 * - Designed to pick a move for the side to move; used for Black in this project.
 */
public class MonteCarloPlayer {

    private static final double EXPLORATION = Math.sqrt(2.0);
    private static final int ROLLOUT_PLIES = 24; // 12 moves per side max in a rollout

    private static class Node {
        final ChessBoard state;
        final boolean whiteToMove;
        final ChessBoard.SimpleMove moveFromParent; // null for root
        final Node parent;
        List<ChessBoard.SimpleMove> untriedMoves;
        final List<Node> children = new ArrayList<>();
        int visits = 0;
        double valueSum = 0.0; // from perspective of root player (the side to move at root)

        Node(ChessBoard state, boolean whiteToMove, ChessBoard.SimpleMove moveFromParent, Node parent) {
            this.state = state;
            this.whiteToMove = whiteToMove;
            this.moveFromParent = moveFromParent;
            this.parent = parent;
            this.untriedMoves = new ArrayList<>(state.generateSimpleMoves(whiteToMove));
        }

        boolean isTerminal() {
            return untriedMoves.isEmpty() && children.isEmpty();
        }

        boolean isFullyExpanded() {
            return untriedMoves.isEmpty();
        }

        Node expand(Random rng) {
            if (untriedMoves.isEmpty()) return this;
            int idx = rng.nextInt(untriedMoves.size());
            ChessBoard.SimpleMove mv = untriedMoves.remove(idx);
            ChessBoard next = state.apply(mv);
            Node child = new Node(next, !whiteToMove, mv, this);
            children.add(child);
            return child;
        }

        Node bestUCT(double exploration, boolean rootWhite) {
            Node best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (Node c : children) {
                double mean = (c.visits == 0) ? 0.0 : (c.valueSum / c.visits);
                double uct = mean + exploration * Math.sqrt(Math.log(Math.max(1, this.visits)) / Math.max(1, c.visits));
                if (uct > bestScore) { bestScore = uct; best = c; }
            }
            return best;
        }

        void backpropagate(double result) {
            Node n = this;
            while (n != null) {
                n.visits++;
                n.valueSum += result;
                n = n.parent;
                // Flip perspective for the opponent at each level
                result = -result;
            }
        }
    }

    public static Optional<MoveChoice> chooseMove(ChessBoard board, boolean whiteToMove, long timeMs) {
        long end = System.currentTimeMillis() + Math.max(10, timeMs);
        Random rng = new Random();
        Node root = new Node(board, whiteToMove, null, null);
        boolean rootWhite = whiteToMove;

        if (root.untriedMoves.isEmpty()) return Optional.empty();

        while (System.currentTimeMillis() < end) {
            // Selection
            Node node = root;
            while (node.isFullyExpanded() && !node.children.isEmpty()) {
                node = node.bestUCT(EXPLORATION, rootWhite);
            }
            // Expansion
            if (!node.isFullyExpanded()) {
                node = node.expand(rng);
            }
            // Simulation
            double result = rollout(node.state, node.whiteToMove, rng, rootWhite);
            // Backpropagation
            node.backpropagate(result);
        }

        // Choose the child with highest visit count (robust child)
        Node best = null;
        int bestVisits = -1;
        for (Node c : root.children) {
            if (c.visits > bestVisits) { bestVisits = c.visits; best = c; }
        }
        if (best == null) return Optional.empty();
        return Optional.of(new MoveChoice(best.moveFromParent, best.state));
    }

    private static double rollout(ChessBoard state, boolean whiteToMove, Random rng, boolean rootWhite) {
        ChessBoard cur = state;
        boolean side = whiteToMove;
        for (int ply = 0; ply < ROLLOUT_PLIES; ply++) {
            List<ChessBoard.SimpleMove> moves = cur.generateSimpleMoves(side);
            if (moves.isEmpty()) break; // No moves: just evaluate current position
            ChessBoard.SimpleMove mv = moves.get(rng.nextInt(moves.size()));
            cur = cur.apply(mv);
            side = !side;
        }
        int eval = cur.evaluateMaterial(); // positive = White ahead
        // Convert to root perspective
        return rootWhite ? eval : -eval;
    }

    public static class MoveChoice {
        public final ChessBoard.SimpleMove move;
        public final ChessBoard resultingState;
        public MoveChoice(ChessBoard.SimpleMove move, ChessBoard resultingState) {
            this.move = move;
            this.resultingState = resultingState;
        }
    }
}
