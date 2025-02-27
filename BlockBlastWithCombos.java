import javax.swing.*;
import java.awt.*;
import java.awt.event.*;  // for MouseAdapter, MouseEvent
import java.util.*;
import java.util.List;

public class BlockBlastWithCombos extends JFrame {

    private static final int BOARD_ROWS = 8;
    private static final int BOARD_COLS = 8;
    private static final int CELL_SIZE = 20;

    // The board: each cell is a small panel that can be occupied or empty
    private Cell[][] cells = new Cell[BOARD_ROWS][BOARD_COLS];

    // The full “bank” of available pieces (shown on the right side)
    private List<Piece> availablePieces;

    // The 3 pieces the user has chosen for this round
    private List<Piece> selectedPieces = new ArrayList<>();

    // Map each Piece to its JButton so we can re-enable it if unselected
    private Map<Piece, JButton> pieceButtonMap = new HashMap<>();

    // Panel holding all piece buttons
    private JPanel piecePanel;

    // Button to confirm once exactly 3 pieces are selected
    private JButton confirmButton;

    // ----------------------------
    // COMBO SYSTEM
    // ----------------------------
    private int comboCount = 0;
    private int piecesSinceLastClear = 0;

    // For bracket-style printing of the board
    private int frameCounter = 1;

    // A static boolean to track if we're in Edit Mode
    public static boolean inEditMode = false;

    public BlockBlastWithCombos() {
        super("Block Blast Solver (Combos + Strategy)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1) Create the main board (8×8)
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_ROWS, BOARD_COLS));
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                cells[r][c] = new Cell();
                boardPanel.add(cells[r][c]);
            }
        }

        // 2) Right side: piece panel + confirm/unselect/edit mode
        JPanel rightPanel = new JPanel(new BorderLayout());
        piecePanel = new JPanel();
        piecePanel.setLayout(new BoxLayout(piecePanel, BoxLayout.Y_AXIS));
        piecePanel.setBackground(Color.LIGHT_GRAY);

        availablePieces = getAllPieces();
        refreshPiecePanel();

        confirmButton = new JButton("Confirm Selection (Pick 3)");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> {
            if (selectedPieces.size() == 3) {
                System.out.println("Frame " + frameCounter + " printed in console:");
                printBoardFrame(); // standard bracket grid
                frameCounter++;

                System.out.println("\"3 pieces selected\"");
                System.out.println("Printed in console pieces selected:");
                printSelectedPieces(selectedPieces);
                System.out.println("Print selected pieces names:");
                printSelectedPieceNames(selectedPieces);

                // Attempt to place these 3 pieces
                boolean placed = solveRound(selectedPieces);
                if (!placed) {
                    JOptionPane.showMessageDialog(this,
                        "No valid arrangement found. Game Over!");
                    System.exit(0);
                } else {
                    // Re-enable all buttons, let the user pick again
                    selectedPieces.clear();
                    confirmButton.setEnabled(false);
                    refreshPiecePanel();
                }
            }
        });

        JButton unselectButton = new JButton("Unselect Last Piece");
        unselectButton.addActionListener(e -> {
            if (!selectedPieces.isEmpty()) {
                Piece last = selectedPieces.remove(selectedPieces.size() - 1);
                JButton lastButton = pieceButtonMap.get(last);
                if (lastButton != null) {
                    lastButton.setEnabled(true);
                }
                if (selectedPieces.size() < 3) {
                    confirmButton.setEnabled(false);
                }
            }
        });

        JButton editModeButton = new JButton("Edit Mode");
        editModeButton.addActionListener(e -> {
            inEditMode = !inEditMode;
            System.out.println("Edit Mode is now: " + inEditMode);
        });

        // A text field + "Search" button to filter piece names
        JTextField searchField = new JTextField(10);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            String query = searchField.getText().toLowerCase().trim();
            if (query.isEmpty()) {
                refreshPiecePanel();
            } else {
                List<Piece> filtered = new ArrayList<>();
                for (Piece piece : availablePieces) {
                    if (piece.getName().toLowerCase().contains(query)) {
                        filtered.add(piece);
                    }
                }
                refreshPiecePanel(filtered);
            }
        });

        JPanel topPanel = new JPanel();
        topPanel.add(unselectButton);
        topPanel.add(editModeButton);
        topPanel.add(searchField);
        topPanel.add(searchButton);

        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(piecePanel), BorderLayout.CENTER);
        rightPanel.add(confirmButton, BorderLayout.SOUTH);

        add(boardPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Overloaded method to show either all pieces or a filtered list
    private void refreshPiecePanel() {
        refreshPiecePanel(availablePieces);
    }

    private void refreshPiecePanel(List<Piece> listToShow) {
        piecePanel.removeAll();

        for (Piece p : listToShow) {
            JButton pieceButton = new JButton(p.getName());
            pieceButton.setIcon(new PieceIcon(p, 15));
            pieceButton.setHorizontalTextPosition(SwingConstants.CENTER);
            pieceButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            pieceButton.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            pieceButtonMap.put(p, pieceButton);

            pieceButton.addActionListener(e -> {
                if (selectedPieces.size() < 3) {
                    selectedPieces.add(p);
                    pieceButton.setEnabled(false);
                    if (selectedPieces.size() == 3) {
                        confirmButton.setEnabled(true);
                    }
                }
            });

            piecePanel.add(pieceButton);
        }

        piecePanel.revalidate();
        piecePanel.repaint();
    }

    // -------------
    // NEW METHOD: Print an 8×8 bracket grid, highlighting the newly placed piece in 'R'
    // -------------
    private void printBoardFrameWithHighlightedPiece(boolean[][] board, Piece piece, int startRow, int startCol) {
        // Build a set of coordinates for the newly placed piece
        Set<Point> pieceCoords = new HashSet<>();
        for (int[] cellOffset : piece.getCells()) {
            int r = startRow + cellOffset[1];
            int c = startCol + cellOffset[0];
            pieceCoords.add(new Point(r, c));
        }

        for (int r = 0; r < BOARD_ROWS; r++) {
            System.out.print("[ ");
            for (int c = 0; c < BOARD_COLS; c++) {
                if (pieceCoords.contains(new Point(r, c))) {
                    System.out.print("R "); // new piece squares in red
                } else if (board[r][c]) {
                    System.out.print("x ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println("]");
        }
    }

    // We override the existing placeArrangement to print the “mini screens”
    private void placeArrangement(Arrangement arr) {
        // FIRST piece
        System.out.println("\n\"First piece placement and printed in console:\"");
        System.out.println("Frame " + frameCounter + " printed in console:");

        // 1) Place piece on the real board
        placePieceOnRealBoard(arr.p1, arr.r1, arr.c1);
        // 2) Copy occupancy to highlight the newly placed squares
        boolean[][] afterPlacement1 = copyBoardOccupancy();
        printBoardFrameWithHighlightedPiece(afterPlacement1, arr.p1, arr.r1, arr.c1);
        frameCounter++;

        System.out.println("Print placement of name and location: (e.g. Placing "
            + arr.p1.getName() + " at (" + arr.r1 + "," + arr.c1 + "))");

        int lines1 = checkAndClearLinesOnRealBoard();
        updateGlobalCombo(lines1);

        // SECOND piece
        System.out.println("\n\"Second piece placement and printed in console:\"");
        System.out.println("Frame " + frameCounter + " printed in console:");

        placePieceOnRealBoard(arr.p2, arr.r2, arr.c2);
        boolean[][] afterPlacement2 = copyBoardOccupancy();
        printBoardFrameWithHighlightedPiece(afterPlacement2, arr.p2, arr.r2, arr.c2);
        frameCounter++;

        System.out.println("Print placement of name and location: (e.g. Placing "
            + arr.p2.getName() + " at (" + arr.r2 + "," + arr.c2 + "))");

        int lines2 = checkAndClearLinesOnRealBoard();
        updateGlobalCombo(lines2);

        // THIRD piece
        System.out.println("\n\"Third piece placement and printed in console:\"");
        System.out.println("Frame " + frameCounter + " printed in console:");

        placePieceOnRealBoard(arr.p3, arr.r3, arr.c3);
        boolean[][] afterPlacement3 = copyBoardOccupancy();
        printBoardFrameWithHighlightedPiece(afterPlacement3, arr.p3, arr.r3, arr.c3);
        frameCounter++;

        System.out.println("Print placement of name and location: (Placing "
            + arr.p3.getName() + " at (" + arr.r3 + "," + arr.c3 + "))");

        int lines3 = checkAndClearLinesOnRealBoard();
        updateGlobalCombo(lines3);
    }

    // Existing code (solver logic, combos, etc.) remain unchanged below

    private boolean solveRound(List<Piece> threePieces) {
        boolean[][] originalBoard = copyBoardOccupancy();

        double bestScore = Double.NEGATIVE_INFINITY;
        Arrangement bestArrangement = null;

        List<List<Piece>> permutations = generatePermutations(threePieces);

        for (List<Piece> perm : permutations) {
            Piece p1 = perm.get(0);
            Piece p2 = perm.get(1);
            Piece p3 = perm.get(2);

            for (int row1 = 0; row1 < BOARD_ROWS; row1++) {
                for (int col1 = 0; col1 < BOARD_COLS; col1++) {
                    if (canPlace(p1, row1, col1, originalBoard)) {
                        boolean[][] afterP1 = copyArray(originalBoard);
                        placePiece(p1, row1, col1, afterP1);
                        int lines1 = clearLinesInSimulation(afterP1);

                        int localCombo1 = computeNewCombo(lines1);
                        boolean[][] afterP1Final = updateComboBoard(afterP1, lines1);

                        for (int row2 = 0; row2 < BOARD_ROWS; row2++) {
                            for (int col2 = 0; col2 < BOARD_COLS; col2++) {
                                if (canPlace(p2, row2, col2, afterP1Final)) {
                                    boolean[][] afterP2 = copyArray(afterP1Final);
                                    placePiece(p2, row2, col2, afterP2);
                                    int lines2 = clearLinesInSimulation(afterP2);

                                    int localCombo2 = computeNextCombo(localCombo1, lines2);
                                    boolean[][] afterP2Final = updateComboBoard(afterP2, lines2);

                                    for (int row3 = 0; row3 < BOARD_ROWS; row3++) {
                                        for (int col3 = 0; col3 < BOARD_COLS; col3++) {
                                            if (canPlace(p3, row3, col3, afterP2Final)) {
                                                boolean[][] afterP3 = copyArray(afterP2Final);
                                                placePiece(p3, row3, col3, afterP3);
                                                int lines3 = clearLinesInSimulation(afterP3);

                                                int localCombo3 = computeNextCombo(localCombo2, lines3);
                                                boolean[][] finalBoard = updateComboBoard(afterP3, lines3);

                                                double arrangementScore = evaluateArrangement(
                                                    finalBoard, lines1+lines2+lines3,
                                                    localCombo3
                                                );

                                                if (arrangementScore > bestScore) {
                                                    bestScore = arrangementScore;
                                                    bestArrangement = new Arrangement(
                                                        p1, row1, col1,
                                                        p2, row2, col2,
                                                        p3, row3, col3,
                                                        lines1+lines2+lines3,
                                                        localCombo3
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestArrangement == null) {
            return false;
        }

        // Place the arrangement on the REAL board, printing mini-screens
        placeArrangement(bestArrangement);
        return true;
    }

    private double evaluateArrangement(boolean[][] board, int totalLinesCleared, int localCombo) {
        double score = totalLinesCleared * 50;
        score += localCombo * 100;

        int occupiedCount = countOccupiedCells(board);
        int totalCells = BOARD_ROWS * BOARD_COLS;
        double fillRatio = (double)occupiedCount / totalCells;
        if (fillRatio > 0.75) {
            score -= (fillRatio - 0.75) * 200;
        }

        if (has3x3Space(board)) {
            score += 20;
        }
        if (has1x5Space(board)) {
            score += 20;
        }

        return score;
    }

    private int countOccupiedCells(boolean[][] board) {
        int count = 0;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                if (board[r][c]) count++;
            }
        }
        return count;
    }

    private boolean has3x3Space(boolean[][] board) {
        for (int r = 0; r <= BOARD_ROWS - 3; r++) {
            for (int c = 0; c <= BOARD_COLS - 3; c++) {
                boolean allEmpty = true;
                for (int rr = 0; rr < 3; rr++) {
                    for (int cc = 0; cc < 3; cc++) {
                        if (board[r+rr][c+cc]) {
                            allEmpty = false;
                            break;
                        }
                    }
                    if (!allEmpty) break;
                }
                if (allEmpty) return true;
            }
        }
        return false;
    }

    private boolean has1x5Space(boolean[][] board) {
        // check rows
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int startCol = 0; startCol <= BOARD_COLS - 5; startCol++) {
                boolean allEmpty = true;
                for (int c = 0; c < 5; c++) {
                    if (board[r][startCol + c]) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) return true;
            }
        }
        // check columns
        for (int c = 0; c < BOARD_COLS; c++) {
            for (int startRow = 0; startRow <= BOARD_ROWS - 5; startRow++) {
                boolean allEmpty = true;
                for (int r = 0; r < 5; r++) {
                    if (board[startRow + r][c]) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) return true;
            }
        }
        return false;
    }

    private int computeNewCombo(int linesCleared) {
        int tempCombo = comboCount;
        int tempPieces = piecesSinceLastClear;

        if (linesCleared > 0) {
            if (tempPieces <= 3) {
                tempCombo = (tempCombo == 0) ? 1 : tempCombo + 1;
            } else {
                tempCombo = 1;
            }
            tempPieces = 0;
        } else {
            tempPieces++;
            if (tempPieces > 3) {
                tempCombo = 0;
            }
        }
        return tempCombo;
    }

    private int computeNextCombo(int currentCombo, int linesCleared) {
        if (linesCleared > 0) {
            return (currentCombo == 0) ? 1 : currentCombo + 1;
        } else {
            return currentCombo;
        }
    }

    private List<List<Piece>> generatePermutations(List<Piece> input) {
        List<List<Piece>> results = new ArrayList<>();
        permuteHelper(input, 0, results);
        return results;
    }

    private void permuteHelper(List<Piece> arr, int start, List<List<Piece>> results) {
        if (start == arr.size() - 1) {
            results.add(new ArrayList<>(arr));
        } else {
            for (int i = start; i < arr.size(); i++) {
                Collections.swap(arr, i, start);
                permuteHelper(arr, start + 1, results);
                Collections.swap(arr, i, start);
            }
        }
    }


    private void printBoardFrame() {
        for (int r = 0; r < BOARD_ROWS; r++) {
            System.out.print("[ ");
            for (int c = 0; c < BOARD_COLS; c++) {
                if (cells[r][c].isOccupied()) {
                    System.out.print("x ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println("]");
        }
    }

        private void printSelectedPieces(List<Piece> pieces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            int cellCount = pieces.get(i).getCells().length;
            sb.append("[");
            for (int j = 0; j < cellCount; j++) {
                sb.append("x");
                if (j < cellCount - 1) sb.append(",");
            }
            sb.append("]");
            if (i < pieces.size() - 1) {
                sb.append(" and ");
            }
        }
        System.out.println(sb.toString());
    }

    private void printSelectedPieceNames(List<Piece> pieces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            sb.append(pieces.get(i).getName());
            if (i < pieces.size() - 1) {
                sb.append(", ");
            }
        }
        System.out.println(sb.toString());
    }

    // ---------- Inner Classes ----------

    private static class Cell extends JPanel {
        private boolean occupied;
        private String pieceName;
        private Color pieceColor;

        public Cell() {
            setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
            setBackground(Color.white);
            setBorder(BorderFactory.createLineBorder(Color.BLACK));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (BlockBlastWithCombos.inEditMode) {
                        // Toggle occupancy
                        if (!occupied) {
                            setOccupied(true, "Manual", Color.YELLOW);
                        } else {
                            setOccupied(false, "", Color.WHITE);
                        }
                    }
                }
            });
        }

        public boolean isOccupied() {
            return occupied;
        }

        public void setOccupied(boolean occupied, String pieceName, Color color) {
            this.occupied = occupied;
            this.pieceName = pieceName;
            this.pieceColor = color;
            setBackground(occupied ? color.brighter() : Color.WHITE);
            repaint();
        }
    }

    private void updateGlobalCombo(int linesCleared) {
        if (linesCleared > 0) {
            if (piecesSinceLastClear <= 3) {
                if (comboCount == 0) {
                    comboCount = 1;
                } else {
                    comboCount++;
                }
            } else {
                comboCount = 1;
            }
            piecesSinceLastClear = 0;
            System.out.println("Lines cleared! Current combo = " + comboCount);
        } else {
            piecesSinceLastClear++;
            if (piecesSinceLastClear > 3) {
                comboCount = 0;
                System.out.println("Combo lost (no line clear within 3 pieces).");
            }
        }
    }

    private void placePieceOnRealBoard(Piece piece, int startRow, int startCol) {
        for (int[] cellOffset : piece.getCells()) {
            int r = startRow + cellOffset[1];
            int c = startCol + cellOffset[0];
            cells[r][c].setOccupied(true, piece.getName(), piece.getColor());
        }
    }

    private int checkAndClearLinesOnRealBoard() {
        // Collect all full rows
        List<Integer> fullRows = new ArrayList<>();
        for (int r = 0; r < BOARD_ROWS; r++) {
            if (isRowFull(r)) {
                fullRows.add(r);
            }
        }
    
        // Collect all full columns
        List<Integer> fullCols = new ArrayList<>();
        for (int c = 0; c < BOARD_COLS; c++) {
            if (isColumnFull(c)) {
                fullCols.add(c);
            }
        }
    
        // The total lines cleared is the sum of all full rows + all full cols
        int linesCleared = fullRows.size() + fullCols.size();
    
        // Now clear them all in one pass
        for (int r : fullRows) {
            clearRow(r);
        }
        for (int c : fullCols) {
            clearColumn(c);
        }
    
        return linesCleared;
    }
    
    private boolean isRowFull(int row) {
        for (int c = 0; c < BOARD_COLS; c++) {
            if (!cells[row][c].isOccupied()) return false;
        }
        return true;
    }

    private boolean isColumnFull(int col) {
        for (int r = 0; r < BOARD_ROWS; r++) {
            if (!cells[r][col].isOccupied()) return false;
        }
        return true;
    }

    private void clearRow(int row) {
        for (int c = 0; c < BOARD_COLS; c++) {
            cells[row][c].setOccupied(false, "", Color.WHITE);
        }
        System.out.println("Cleared row " + row);
    }

    private void clearColumn(int col) {
        for (int r = 0; r < BOARD_ROWS; r++) {
            cells[r][col].setOccupied(false, "", Color.WHITE);
        }
        System.out.println("Cleared column " + col);
    }

    private boolean canPlace(Piece piece, int startRow, int startCol, boolean[][] occupied) {
        for (int[] cellOffset : piece.getCells()) {
            int r = startRow + cellOffset[1];
            int c = startCol + cellOffset[0];
            if (r < 0 || r >= BOARD_ROWS || c < 0 || c >= BOARD_COLS) {
                return false;
            }
            if (occupied[r][c]) {
                return false;
            }
        }
        return true;
    }

    private void placePiece(Piece piece, int startRow, int startCol, boolean[][] occupied) {
        for (int[] cellOffset : piece.getCells()) {
            int r = startRow + cellOffset[1];
            int c = startCol + cellOffset[0];
            occupied[r][c] = true;
        }
    }

    private int clearLinesInSimulation(boolean[][] occupied) {
        // Collect all full rows
        List<Integer> fullRows = new ArrayList<>();
        for (int r = 0; r < BOARD_ROWS; r++) {
            boolean rowFull = true;
            for (int c = 0; c < BOARD_COLS; c++) {
                if (!occupied[r][c]) {
                    rowFull = false;
                    break;
                }
            }
            if (rowFull) {
                fullRows.add(r);
            }
        }
    
        // Collect all full columns
        List<Integer> fullCols = new ArrayList<>();
        for (int c = 0; c < BOARD_COLS; c++) {
            boolean colFull = true;
            for (int r = 0; r < BOARD_ROWS; r++) {
                if (!occupied[r][c]) {
                    colFull = false;
                    break;
                }
            }
            if (colFull) {
                fullCols.add(c);
            }
        }
    
        // The total lines cleared is the sum of all full rows + all full cols
        int linesCleared = fullRows.size() + fullCols.size();
    
        // Now clear them all in one pass
        for (int r : fullRows) {
            for (int c = 0; c < BOARD_COLS; c++) {
                occupied[r][c] = false;
            }
        }
        for (int c : fullCols) {
            for (int r = 0; r < BOARD_ROWS; r++) {
                occupied[r][c] = false;
            }
        }
    
        return linesCleared;
    }
    
    private boolean[][] updateComboBoard(boolean[][] board, int linesCleared) {
        // We already updated 'board' in clearLinesInSimulation, so just return it.
        return board;
    }

    private boolean[][] copyBoardOccupancy() {
        boolean[][] result = new boolean[BOARD_ROWS][BOARD_COLS];
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                result[r][c] = cells[r][c].isOccupied();
            }
        }
        return result;
    }

    private boolean[][] copyArray(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][source[0].length];
        for (int r = 0; r < source.length; r++) {
            System.arraycopy(source[r], 0, copy[r], 0, source[r].length);
        }
        return copy;
    }


    static class Piece {
        private int[][] cells;  // (x,y) offsets
        private Color color;
        private String name;

        public Piece(int[][] cells, Color color, String name) {
            this.cells = new int[cells.length][2];
            for (int i = 0; i < cells.length; i++) {
                this.cells[i][0] = cells[i][0];
                this.cells[i][1] = cells[i][1];
            }
            this.color = color;
            this.name = name;
        }

        public int[][] getCells() { return cells; }
        public Color getColor() { return color; }
        public String getName() { return name; }
    }

    static class PieceIcon implements Icon {
        private Piece piece;
        private int cellSize;
        private int iconWidth;
        private int iconHeight;
        private int offsetX;
        private int offsetY;

        public PieceIcon(Piece piece, int cellSize) {
            this.piece = piece;
            this.cellSize = cellSize;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (int[] cell : piece.getCells()) {
                minX = Math.min(minX, cell[0]);
                maxX = Math.max(maxX, cell[0]);
                minY = Math.min(minY, cell[1]);
                maxY = Math.max(maxY, cell[1]);
            }
            offsetX = minX;
            offsetY = minY;
            iconWidth = (maxX - minX + 1) * cellSize + 4;
            iconHeight = (maxY - minY + 1) * cellSize + 4;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(piece.getColor());
            for (int[] cell : piece.getCells()) {
                int drawX = (cell[0] - offsetX) * cellSize + x + 2;
                int drawY = (cell[1] - offsetY) * cellSize + y + 2;
                g.fillRect(drawX, drawY, cellSize - 1, cellSize - 1);
            }
        }

        @Override
        public int getIconWidth() {
            return iconWidth;
        }

        @Override
        public int getIconHeight() {
            return iconHeight;
        }
    }


    static class Arrangement {
        Piece p1; int r1; int c1;
        Piece p2; int r2; int c2;
        Piece p3; int r3; int c3;
        int totalLines;
        int finalCombo;

        public Arrangement(Piece p1, int r1, int c1,
                           Piece p2, int r2, int c2,
                           Piece p3, int r3, int c3,
                           int totalLines, int finalCombo) {
            this.p1 = p1; this.r1 = r1; this.c1 = c1;
            this.p2 = p2; this.r2 = r2; this.c2 = c2;
            this.p3 = p3; this.r3 = r3; this.c3 = c3;
            this.totalLines = totalLines;
            this.finalCombo = finalCombo;
        }
    }

    // Your existing shapes remain below
    private static List<Piece> getAllPieces() {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,0} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {2,2} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {2,0} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {0,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {2,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0} },
            Color.GRAY,
            "Cube (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {2,1}, {2,0} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0}, {4,0} },
            Color.GRAY,
            "Horizontal Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0} },
            Color.GRAY,
            "Horizontal Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0} },
            Color.GRAY,
            "Horizontal Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0} },
            Color.GRAY,
            "Horizontal Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {0,3}, {0,4} },
            Color.GRAY,
            "Vertical Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} , {0,3}},
            Color.GRAY,
            "Vertical Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} },
            Color.GRAY,
            "Vertical Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1} },
            Color.GRAY,
            "Vertical Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2}, {2,0}, {2,1}, {2,2} },
            Color.GRAY,
            "Cube (9)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,0}, {2,1} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {2,0} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {0,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2}, {2,2} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {2,1}, {2,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {2,1}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0}, {1,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0} },
            Color.GRAY,
            "Dot (1)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,0} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {2,2} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {2,0} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {0,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {2,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0} },
            Color.GRAY,
            "Cube (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {2,1}, {2,0} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0}, {4,0} },
            Color.GRAY,
            "Horizontal Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0} },
            Color.GRAY,
            "Horizontal Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0} },
            Color.GRAY,
            "Horizontal Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0} },
            Color.GRAY,
            "Horizontal Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {0,3}, {0,4} },
            Color.GRAY,
            "Vertical Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} , {0,3}},
            Color.GRAY,
            "Vertical Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} },
            Color.GRAY,
            "Vertical Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1} },
            Color.GRAY,
            "Vertical Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2}, {2,0}, {2,1}, {2,2} },
            Color.GRAY,
            "Cube (9)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,0}, {2,1} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {2,0} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {0,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2}, {2,2} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {2,1}, {2,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {2,1}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0}, {1,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0} },
            Color.GRAY,
            "Dot (1)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,0} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,0}, {0,1}, {1,1} },
            Color.GRAY,
            "L (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {2,2} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {2,0} },
            Color.GRAY,
            "Diagonal (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0} },
            Color.GRAY,
            "Diagonal (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {0,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {2,0}, {0,1}, {1,1}, {2,1} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,2}, {1,0} },
            Color.GRAY,
            "L (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0} },
            Color.GRAY,
            "Cube (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {2,1}, {2,0} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2} },
            Color.GRAY,
            "Rectangle (6)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0}, {4,0} },
            Color.GRAY,
            "Horizontal Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {3,0} },
            Color.GRAY,
            "Horizontal Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0} },
            Color.GRAY,
            "Horizontal Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0} },
            Color.GRAY,
            "Horizontal Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {0,3}, {0,4} },
            Color.GRAY,
            "Vertical Line (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} , {0,3}},
            Color.GRAY,
            "Vertical Line (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2} },
            Color.GRAY,
            "Vertical Line (3)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1} },
            Color.GRAY,
            "Vertical Line (2)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,0}, {0,2}, {1,2}, {2,0}, {2,1}, {2,2} },
            Color.GRAY,
            "Cube (9)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {1,0}, {2,1} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {2,0} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,1}, {0,1}, {1,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {1,1}, {0,1}, {1,0}, {0,2} },
            Color.GRAY,
            "S (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,2}, {2,2} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {2,1}, {2,2}, {1,0}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {2,1}, {2,0} },
            Color.GRAY,
            "L (5)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {0,1}, {0,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,1}, {1,0}, {1,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,2}, {1,2}, {2,2}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0}, {1,0}, {2,0}, {1,1} },
            Color.GRAY,
            "T (4)"
        ));

        pieces.add(new Piece(
            new int[][] { {0,0} },
            Color.GRAY,
            "Dot (1)"
        ));

        return pieces;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BlockBlastWithCombos::new);
    }
}