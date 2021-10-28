package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import java.io.File;

import vtk.vtkPolyData;
import vtk.vtkPolyDataReader;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SafeURLPaths;

public class PerspectiveImageFootprintCacheOperator
{

	public PerspectiveImageFootprintCacheOperator()
	{
		// TODO Auto-generated constructor stub
	}

    vtkPolyData checkForExistingFootprint(String preRenderingFilenameBase)
    {
//    	if (getFootprintGenerated()[image.getCurrentSlice()] == false) return null;
        String intersectionFileName = preRenderingFilenameBase + "_frustumIntersection.vtk.gz";
        File file = null;
        try
        {
        	file = FileCache.getFileFromServer(intersectionFileName);
        }
        catch (Exception e)
        {
        	file = new File(SafeURLPaths.instance().getString(intersectionFileName));
        	if (file.exists())
            {
            	vtkPolyDataReader reader = new vtkPolyDataReader();
                reader.SetFileName(file.getAbsolutePath());
                reader.Update();
                vtkPolyData footprint = reader.GetOutput();
                return footprint;
            }
        	else
        	{
        		return null;
        	}
        }
        if (file != null)
        {
        	vtkPolyDataReader reader = new vtkPolyDataReader();
            reader.SetFileName(file.getAbsolutePath());
            reader.Update();
            vtkPolyData footprint = reader.GetOutput();
            return footprint;
        }
        return null;
    }

}
