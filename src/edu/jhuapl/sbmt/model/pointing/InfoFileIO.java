package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

public interface InfoFileIO
{
    public void deleteAdjustedImageInfo();

    public void saveImageInfo();

    public void saveImageInfo(String filename);

    public void loadImageInfo() throws IOException;

    public void loadAdjustedSumfile() throws NumberFormatException, IOException;

    public String initLocalInfoFileFullPath();

}
