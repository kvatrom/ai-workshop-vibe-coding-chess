package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ChessBoardTest {

    @Test
    void initialPositionHasCorrectPieces() {
        ChessBoard b = ChessBoard.initial();
        // White pieces
        assertEquals('R', b.getAt(0,0));
        assertEquals('N', b.getAt(1,0));
        assertEquals('B', b.getAt(2,0));
        assertEquals('Q', b.getAt(3,0));
        assertEquals('K', b.getAt(4,0));
        assertEquals('P', b.getAt(0,1));
        // Black pieces
        assertEquals('p', b.getAt(0,6));
        assertEquals('r', b.getAt(0,7));
    }

    @Test
    void getSetRoundTrip() {
        ChessBoard b = ChessBoard.initial();
        b.setAt(3,3, 'N');
        assertEquals('N', b.getAt(3,3));
        b.setAt(3,3, ' ');
        assertEquals(' ', b.getAt(3,3));
    }

    @Test
    void tryApplySanFromInitialWhite_allowsPawnPushAndKnight() {
        ChessBoard b = ChessBoard.initial();
        // Pawn e4
        ChessBoard.MoveResult r1 = b.tryApplySanFromInitialWhite("e4");
        assertTrue(r1.legal());
        assertEquals(4, r1.fromFile());
        assertEquals(1, r1.fromRank());
        assertEquals(4, r1.toFile());
        assertEquals(3, r1.toRank());
        // Knight Nf3
        ChessBoard b2 = ChessBoard.initial();
        ChessBoard.MoveResult r2 = b2.tryApplySanFromInitialWhite("Nf3");
        assertTrue(r2.legal());
        assertEquals(5, r2.toFile());
        assertEquals(2, r2.toRank());
    }

    @Test
    void tryApplySanSimple_supportsPawnsKnightsAndCaptures() {
        ChessBoard b = ChessBoard.initial();
        // White: e4
        ChessBoard.MoveResult r1 = b.tryApplySanSimple("e4", true);
        assertTrue(r1.legal());
        b = r1.after();
        // Black: Nf6
        ChessBoard.MoveResult r2 = b.tryApplySanSimple("Nf6", false);
        assertTrue(r2.legal());
        b = r2.after();
        // Make a simple capture scenario: White plays exd5 after d-pawn moved by black previously.
        // Start a fresh board for a deterministic capture: 1. e4 d5 2. exd5
        ChessBoard g = ChessBoard.initial();
        ChessBoard.MoveResult w1 = g.tryApplySanSimple("e4", true);
        assertTrue(w1.legal());
        g = w1.after();
        ChessBoard.MoveResult b1 = g.tryApplySanSimple("d5", false);
        assertTrue(b1.legal());
        g = b1.after();
        ChessBoard.MoveResult w2 = g.tryApplySanSimple("exd5", true);
        assertTrue(w2.legal());
    }

    @Test
    void colorHelpers() {
        ChessBoard b = ChessBoard.initial();
        assertTrue(b.isWhite('Q'));
        assertTrue(b.isBlack('q'));
        assertFalse(b.isWhite('q'));
        assertFalse(b.isBlack('Q'));
    }

    @Test
    void generateSimpleMoves_initialHas20ForWhite() {
        ChessBoard b = ChessBoard.initial();
        List<ChessBoard.SimpleMove> moves = b.generateSimpleMoves(true);
        assertEquals(20, moves.size());
    }

    @Test
    void applyMovesAndEvaluate() {
        ChessBoard b = ChessBoard.initial();
        int eval0 = b.evaluateMaterial();
        assertEquals(0, eval0);
        // Apply a simple move and ensure board changes
        List<ChessBoard.SimpleMove> moves = b.generateSimpleMoves(true);
        assertFalse(moves.isEmpty());
        ChessBoard.SimpleMove mv = moves.get(0);
        ChessBoard after = b.apply(mv);
        assertNotEquals(b.getAt(mv.fromFile(), mv.fromRank()), after.getAt(mv.fromFile(), mv.fromRank()));
    }
}
