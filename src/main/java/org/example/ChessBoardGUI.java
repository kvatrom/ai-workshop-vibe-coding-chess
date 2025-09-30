package org.example;

import javax.swing.*;
import java.awt.*;

/**
 * Simple Swing GUI for drawing the chessboard graphically.
 * - Board drawn with blue/orange squares.
 * - Pieces drawn as Unicode glyphs in black color.
 * - Origin square outlined in green, destination in red.
 * - The board panel is sized to roughly half the screen (square) and resizes nicely.
 */
public class ChessBoardGUI {
    private final JFrame frame;
    private final BoardPanel panel;

    public static ChessBoardGUI launch() {
        if (GraphicsEnvironment.isHeadless()) {
            return new ChessBoardGUI(true);
        }
        // Initialize on EDT
        final ChessBoardGUI[] holder = new ChessBoardGUI[1];
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> holder[0] = new ChessBoardGUI(false));
            } else {
                holder[0] = new ChessBoardGUI(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: create directly
            holder[0] = new ChessBoardGUI(false);
        }
        return holder[0];
    }

    private ChessBoardGUI(boolean headless) {
        if (headless) {
            frame = null;
            panel = null;
            return;
        }
        frame = new JFrame("Chess (GUI)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        panel = new BoardPanel();

        // Size to roughly half of the screen (square board area)
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int size = (int) (Math.min(screen.width, screen.height) * 0.5);
        panel.setPreferredSize(new Dimension(size, size));

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void update(ChessBoard board, int fromFile, int fromRank, int toFile, int toRank) {
        if (panel == null) return; // headless no-op
        SwingUtilities.invokeLater(() -> {
            panel.setState(board, fromFile, fromRank, toFile, toRank);
            panel.repaint();
        });
    }

    public void update(ChessBoard board) {
        if (panel == null) return; // headless no-op
        update(board, -1, -1, -1, -1);
    }

    // Inner panel class that does the drawing
    private static class BoardPanel extends JPanel {
        private ChessBoard board = ChessBoard.initial();
        private int fromFile = -1, fromRank = -1, toFile = -1, toRank = -1;

        // Colors similar to terminal renderer
        private static final Color DARK = new Color(0x1F, 0x55, 0x9B); // approx blue
        private static final Color LIGHT = new Color(0xFF, 0x8C, 0x00); // orange
        private static final Color PIECE_FG = Color.BLACK;
        private static final Color ORIGIN = new Color(0x2E, 0x7D, 0x32); // green
        private static final Color DEST = new Color(0xC6, 0x28, 0x28);   // red

        private static final BasicStroke THICK = new BasicStroke(4f);

        private void setState(ChessBoard b, int ff, int fr, int tf, int tr) {
            this.board = b;
            this.fromFile = ff; this.fromRank = fr; this.toFile = tf; this.toRank = tr;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int size = Math.min(w, h);
                int x0 = (w - size) / 2;
                int y0 = (h - size) / 2;
                int cell = size / 8;

                // Draw squares
                for (int r = 7; r >= 0; r--) {
                    for (int f = 0; f < 8; f++) {
                        boolean dark = ((f + r) % 2 == 0);
                        g2.setColor(dark ? DARK : LIGHT);
                        int sx = x0 + f * cell;
                        int sy = y0 + (7 - r) * cell; // y grows downwards
                        g2.fillRect(sx, sy, cell, cell);
                    }
                }

                // Draw highlights
                if (fromFile >= 0) {
                    g2.setColor(ORIGIN);
                    g2.setStroke(THICK);
                    int sx = x0 + fromFile * cell;
                    int sy = y0 + (7 - fromRank) * cell;
                    g2.drawRect(sx + 2, sy + 2, cell - 4, cell - 4);
                }
                if (toFile >= 0) {
                    g2.setColor(DEST);
                    g2.setStroke(THICK);
                    int sx = x0 + toFile * cell;
                    int sy = y0 + (7 - toRank) * cell;
                    g2.drawRect(sx + 2, sy + 2, cell - 4, cell - 4);
                }

                // Choose a font that likely supports chess unicode
                Font base = getChessFont(cell);
                g2.setFont(base);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(PIECE_FG);

                // Draw coordinates (files and ranks) inside the board corners
                float labelSize = Math.max(10f, cell * 0.18f);
                Font labelFont = g2.getFont().deriveFont(Font.PLAIN, labelSize);
                g2.setFont(labelFont);
                FontMetrics lfm = g2.getFontMetrics();
                // Files along bottom-left of each file on rank 1 squares
                for (int f = 0; f < 8; f++) {
                    int r = 0; // rank 1
                    boolean dark = ((f + r) % 2 == 0);
                    g2.setColor(dark ? Color.WHITE : Color.BLACK);
                    String s = String.valueOf((char)('a' + f));
                    int sx = x0 + f * cell + 4;
                    int sy = y0 + (7 - r) * cell + (cell - 4);
                    g2.drawString(s, sx, sy);
                }
                // Ranks along left side of each rank on file 'a' squares
                for (int r = 0; r < 8; r++) {
                    int f = 0; // file a
                    boolean dark = ((f + r) % 2 == 0);
                    g2.setColor(dark ? Color.WHITE : Color.BLACK);
                    String s = String.valueOf(r + 1);
                    int sx = x0 + 4;
                    int sy = y0 + (7 - r) * cell + lfm.getAscent();
                    g2.drawString(s, sx, sy);
                }

                // Draw pieces
                g2.setFont(base);
                fm = g2.getFontMetrics();
                for (int r = 0; r < 8; r++) {
                    for (int f = 0; f < 8; f++) {
                        char p = board.getAt(f, r);
                        if (p == ' ') continue;
                        char glyph = toUnicode(p);
                        String s = String.valueOf(glyph);
                        int sx = x0 + f * cell;
                        int sy = y0 + (7 - r) * cell;
                        int textW = fm.stringWidth(s);
                        int textH = fm.getAscent();
                        int tx = sx + (cell - textW) / 2;
                        int ty = sy + (cell + textH) / 2 - 2;
                        g2.drawString(s, tx, ty);
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        private static Font getChessFont(int cell) {
            int size = Math.max(12, (int) (cell * 0.75));
            // Try a few fonts known to include chess characters
            String[] candidates = new String[]{
                    "Segoe UI Symbol", "DejaVu Sans", "Arial Unicode MS", "Noto Sans Symbols2", "SansSerif"
            };
            for (String name : candidates) {
                Font f = new Font(name, Font.PLAIN, size);
                if (fontCanDisplayChess(f)) return f;
            }
            return new Font("SansSerif", Font.PLAIN, size);
        }

        private static boolean fontCanDisplayChess(Font f) {
            return f.canDisplay('♔') && f.canDisplay('♚') && f.canDisplay('♟');
        }

        private static char toUnicode(char piece) {
            if (piece == ' ') return ' ';
            boolean white = Character.isUpperCase(piece);
            switch (Character.toLowerCase(piece)) {
                case 'k': return white ? '♔' : '♚';
                case 'q': return white ? '♕' : '♛';
                case 'r': return white ? '♖' : '♜';
                case 'b': return white ? '♗' : '♝';
                case 'n': return white ? '♘' : '♞';
                case 'p': return white ? '♙' : '♟';
                default: return ' ';
            }
        }
    }
}
