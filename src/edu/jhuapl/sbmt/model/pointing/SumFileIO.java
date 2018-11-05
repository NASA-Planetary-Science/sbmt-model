package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

public interface SumFileIO
{

    public void loadSumfile() throws NumberFormatException, IOException;

    public String initLocalSumfileFullPath();
}
