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
	private Boolean cIsPolyhedron;
    private Vector3D cAverageSurfaceNormal;
    private Vector3D cGeometricCenterPoint;

    public CustomShapeModel(SmallBodyViewConfig config)
    {
        super(config,
                new String[] { config.modelLabel },
                null,
                null,
                null,
                null,
                null,
                ColoringValueType.CELLDATA,
                false);

        cIsPolyhedron = null;
        cAverageSurfaceNormal = null;
        cGeometricCenterPoint = null;

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

		// Calculate the average surface normal if this is a polygonal model
		// rather than a polyhedral model
		cAverageSurfaceNormal = Vector3D.ZERO;
		if (isPolyhedron() == false)
			cAverageSurfaceNormal = CameraUtil.calcSurfaceNormal(this);

		return cAverageSurfaceNormal;
	}

	@Override
	public Vector3D getGeometricCenterPoint()
	{
		// Return the cached value
		if (cGeometricCenterPoint != null)
			return cGeometricCenterPoint;

		// Calculate the geometric center point if this is a polygonal model
		// rather than a polyhedral model
		cGeometricCenterPoint = Vector3D.ZERO;
		if (isPolyhedron() == false)
			cGeometricCenterPoint = CameraUtil.calcCenterPoint(this);

		return cGeometricCenterPoint;
	}

	/**
	 * Helper method that return if this is a true polyhedron (rather than just
	 * a polygonal surface).
	 */
	public boolean isPolyhedron()
	{
		// Return the cached value
		if (cIsPolyhedron != null)
			return cIsPolyhedron;

		cIsPolyhedron = CameraUtil.isPolyhedron(this);
		return cIsPolyhedron;
	}

}
