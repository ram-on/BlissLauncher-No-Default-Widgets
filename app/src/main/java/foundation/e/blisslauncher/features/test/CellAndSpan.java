package foundation.e.blisslauncher.features.test;

/**
 * Base class which represents an area on the grid.
 */
public class CellAndSpan {

    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    public CellAndSpan() {
    }

    public void copyFrom(CellAndSpan copy) {
        cellX = copy.cellX;
        cellY = copy.cellY;
    }

    public CellAndSpan(int cellX, int cellY) {
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public String toString() {
        return "(" + cellX + ", " + cellY + ")";
    }
}
