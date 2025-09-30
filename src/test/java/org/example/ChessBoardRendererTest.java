package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChessBoardRendererTest {

    @Test
    void renderContainsHeadersAndUnicode() {
        ChessBoard b = ChessBoard.initial();
        String out = ChessBoardRenderer.render(b, -1, -1, -1, -1);
        assertTrue(out.contains("a   b   c   d   e   f   g   h"));
        // White rook at a1 should render as Unicode ♖ somewhere
        assertTrue(out.contains("♖"), "Expected white rook glyph in output");
        // Legend present
        assertTrue(out.contains("Legend: ( ) origin, [ ] destination."));
    }
}
