package org.example;

/**
 * Renders the chessboard with ANSI colors and highlights from/to squares. - Board squares use
 * colored backgrounds: blue for dark squares, orange for light squares. - Pieces are colored: black
 * for both sides (Unicode glyphs). - Origin square highlighted with parentheses, e.g., (P) or ( ).
 * - Destination square highlighted with brackets, e.g., [P] or [ ]. Cells are 3 characters wide.
 */
public class ChessBoardRenderer {

  // ANSI escape codes
  private static final String RESET = "\u001B[0m";
  private static final String FG_BLACK = "\u001B[30m";
  private static final String BG_BLUE = "\u001B[44m"; // dark squares
  // Use 256-color "orange" if supported; many terminals will render it. Fallback to yellow if not
  // supported.
  private static final String BG_ORANGE = "\u001B[48;5;208m"; // light squares (approx orange)
  private static final String BG_YELLOW = "\u001B[43m"; // fallback if 256 colors not supported

  // Map internal piece letters to Unicode chess glyphs
  private static char toUnicode(char piece) {
    if (piece == ' ') return ' ';
    boolean white = Character.isUpperCase(piece);
    switch (Character.toLowerCase(piece)) {
      case 'k':
        return white ? '♔' : '♚';
      case 'q':
        return white ? '♕' : '♛';
      case 'r':
        return white ? '♖' : '♜';
      case 'b':
        return white ? '♗' : '♝';
      case 'n':
        return white ? '♘' : '♞';
      case 'p':
        return white ? '♙' : '♟';
      default:
        return ' ';
    }
  }

  public static String render(
      ChessBoard board, int fromFile, int fromRank, int toFile, int toRank) {
    StringBuilder sb = new StringBuilder();
    // Header files
    sb.append("    a   b   c   d   e   f   g   h\n");
    sb.append("  +---+---+---+---+---+---+---+---+\n");
    for (int r = 7; r >= 0; r--) {
      sb.append(r + 1).append(' ').append('|');
      for (int f = 0; f < 8; f++) {
        char piece = board.getAt(f, r);
        boolean isFrom = (f == fromFile && r == fromRank);
        boolean isTo = (f == toFile && r == toRank);
        boolean darkSquare = ((f + r) % 2 == 0); // a1 (0,0) is dark

        String bg = darkSquare ? BG_BLUE : BG_ORANGE;
        // Some very limited terminals may not support 256-color orange. If it renders as plain,
        // user still sees blue/blank.
        // We could try to detect support, but keep it simple/minimal per requirements.

        String cellContent;
        if (isFrom) {
          cellContent = formatHighlight(piece, '(', ')');
        } else if (isTo) {
          cellContent = formatHighlight(piece, '[', ']');
        } else {
          char c = (piece == ' ') ? ' ' : toUnicode(piece);
          cellContent = new String(new char[] {' ', c, ' '});
        }

        // Color piece foreground
        String coloredContent = colorPiece(cellContent, piece);

        // Apply background color to the whole 3-char cell
        sb.append(bg).append(coloredContent).append(RESET).append('|');
      }
      sb.append(' ').append(r + 1).append('\n');
      sb.append("  +---+---+---+---+---+---+---+---+\n");
    }
    sb.append("    a   b   c   d   e   f   g   h\n");
    sb.append("Legend: ( ) origin, [ ] destination.\n");
    return sb.toString();
  }

  private static String colorPiece(String cell, char piece) {
    if (piece == ' ') {
      return cell; // leave empty cells uncolored in foreground
    }
    // All pieces rendered in black foreground, regardless of side
    return FG_BLACK + cell + RESET;
  }

  private static String formatHighlight(char piece, char open, char close) {
    char c = (piece == ' ') ? ' ' : toUnicode(piece);
    return new String(new char[] {open, c, close});
  }
}
