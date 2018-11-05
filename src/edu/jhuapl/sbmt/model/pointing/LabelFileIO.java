package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

public interface LabelFileIO
{
    public void loadLabelFile() throws NumberFormatException, IOException;

    public String initLocalLabelFileFullPath();
}
