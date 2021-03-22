package foundation.e.blisslauncher.features.test;

import foundation.e.blisslauncher.core.database.model.LauncherItem;

/**
 * Utility object to manage the occupancy in a grid.
 */
public class GridOccupancy {

    private final int mCountX;
    private final int mCountY;

    // We use 1-d array for cells because we use index of the cell than x and y.
    public final boolean[] cells;

    public GridOccupancy(int countX, int countY) {
        mCountX = countX;
        mCountY = countY;
        cells = new boolean[countX * countY];
    }

    public void markCells(int index, boolean value) {
        if (index < 0 || index >= mCountX * mCountY) return;
        cells[index] = value;
    }

    public void markCells(CellAndSpan cell, boolean value) {
        markCells(cell.cellY * mCountX + cell.cellX, value);
    }

    public void markCells(LauncherItem item, boolean value) {
        markCells(item.cell, value);
    }

    public void clear() {
        for(int i = 0; i<mCountX*mCountY; i++) {
            markCells(i, false);
        }
    }
}
