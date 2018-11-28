package edu.jhuapl.sbmt.model.custom;

import java.io.File;

import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.dem.DEM;

public class CustomShapeModel extends SmallBodyModel
{
    public CustomShapeModel(SmallBodyViewConfig config)
    {
        super(config,
                new String[] { config.modelLabel },
                new String[] { getModelFilename(config) },
                null,
                null,
                null,
                null,
                null,
                ColoringValueType.CELLDATA,
                false);

        // Check to see if this is an altwg FITs file, if so then extract the color and set it as well
        String fitsPath = Configuration.getImportedShapeModelsDir() +
                File.separator + config.modelLabel + File.separator + "model.fit";
        File fitsFile = new File(fitsPath);
        if(fitsFile.exists())
        {
            // Load in the file's plate colorings
            try
            {
                DEM.colorDEM(fitsPath, this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isBuiltIn()
    {
        return false;
    }

    public static String getModelFilename(SmallBodyViewConfig config)
    {
        if (config.customTemporary)
        {
            return FileCache.createFileURL(config.modelLabel).toString();
        }
        else
        {
            return FileCache.createFileURL(Configuration.getImportedShapeModelsDir(), config.modelLabel, "model.vtk").toString();
        }
    }
}
