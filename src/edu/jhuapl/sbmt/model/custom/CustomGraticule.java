package edu.jhuapl.sbmt.model.custom;

import java.io.File;

import edu.jhuapl.saavtk.model.Graticule;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileCache;

public class CustomGraticule extends Graticule
{
    public CustomGraticule(PolyhedralModel smallBodyModel)
    {
        super(smallBodyModel,
              new String[] { FileCache.FILE_PREFIX +
                Configuration.getImportedShapeModelsDir() +
                File.separator +
                smallBodyModel.getModelName() +
                File.separator +
                "grid.vtk" });
    }
}
