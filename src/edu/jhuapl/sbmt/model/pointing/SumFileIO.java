package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

public interface SumFileIO
{
    public void loadAdjustedSumfile() throws NumberFormatException, IOException;

    public void loadSumfile() throws NumberFormatException, IOException;
}
