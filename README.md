Interactive Board and Pieces

A main 8×8 board (Cell objects) where you can place puzzle pieces.
A piece bank (right-side panel) containing many Tetris-like shapes based off of Block Blast pieces (Piece objects) in various forms.
Visual Feedback: When placing each piece, the console prints a “mini screen” with the newly placed piece highlighted, along with bracketed board output.

User Interaction

Edit Mode allows you to click cells on the board to toggle them occupied or free, simulating a mid-game state.
You can select exactly three pieces from the piece bank (the right panel) and then press Confirm to have the solver find an arrangement for those pieces on the board.

Puzzle-Solving / Search

The solver (Search class) performs a brute-force (permutation + placement) search to find a best arrangement of the three chosen pieces, factoring in line clears and combos.
It uses a simulation of the board to check how many rows/columns are cleared, calculates a score, and picks the arrangement that yields the highest score.

Line Clears and Combos

If any row or column is fully occupied, the game clears it and awards a “combo.” The system tracks how many combos happen consecutively.
