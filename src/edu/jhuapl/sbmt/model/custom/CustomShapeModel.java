package edu.jhuapl.sbmt.model.custom;

import java.io.File;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.gui.render.camera.CameraUtil;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.dem.DEM;

public class CustomShapeModel extends SmallBodyModel
{
    // Cache vars
    private Vector3D cAverageSurfaceNormal;

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

        cAverageSurfaceNormal = null;

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
    	SafeURLPaths safeUrlPaths = SafeURLPaths.instance();
        if (config.customTemporary)
        {
            return safeUrlPaths.getUrl(config.modelLabel);
        }
        else
        {
            return safeUrlPaths.getUrl(safeUrlPaths.getString(Configuration.getImportedShapeModelsDir(), config.modelLabel, "model.vtk"));
        }
    }

	@Override
	public Vector3D getAverageSurfaceNormal()
	{
		// Return the cached value
		if (cAverageSurfaceNormal != null)
			return cAverageSurfaceNormal;

		// Determine if the shape model is a polyhedron. If it is then
		// there is no average surface normal.
		boolean isPolyhedron = CameraUtil.isPolyhedron(this);

		cAverageSurfaceNormal = Vector3D.ZERO;
		if (isPolyhedron == false)
			cAverageSurfaceNormal = CameraUtil.calcSurfaceNormal(this);

		return cAverageSurfaceNormal;
	}

}
