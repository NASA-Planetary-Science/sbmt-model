package edu.jhuapl.sbmt.model.dem;

import java.nio.file.Path;

public class DEMFactory
{
    public static DEM fromObj(Path objFile)
    {
        return new DEM(new DEMKey(objFile.toString(), objFile.toString()));
    }
}
