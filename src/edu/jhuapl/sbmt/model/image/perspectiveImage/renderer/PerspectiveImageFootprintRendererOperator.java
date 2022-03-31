package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import java.util.List;

import vtk.vtkFloatArray;
import vtk.vtkPointData;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

public class PerspectiveImageFootprintRendererOperator
{
	private PerspectiveImage image;
	private vtkFloatArray textureCoords;
	public vtkPolyData[] footprint;
	public boolean[] footprintGenerated;
	private List<SmallBodyModel> smallBodies;
	private vtkPolyData[] shiftedFootprint;
	private PerspectiveImageFootprintCacheOperator footprintCacheOperator;

	public PerspectiveImageFootprintRendererOperator(PerspectiveImage image, List<SmallBodyModel> smallBodies, PerspectiveImageFootprintCacheOperator footprintCacheOperator)
	{
		this.image = image;
		this.smallBodies = smallBodies;
		this.footprintCacheOperator = footprintCacheOperator;
		footprint = new vtkPolyData[smallBodies.size()];
        shiftedFootprint = new vtkPolyData[smallBodies.size()];
        footprintGenerated = new boolean[smallBodies.size()];
        for (int i=0; i<smallBodies.size(); i++)
        {
        	footprint[i] = new vtkPolyData();
        	shiftedFootprint[i] = new vtkPolyData();
        	footprintGenerated[i] = false;
        }
	}

	// **********************
	// Footprint
	// **********************

	public void loadFootprint( )
	{
		// int currentSlice = image.getCurrentSlice();
		double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
		double[][] frustum1Adjusted = image.getFrustum1Adjusted();
		double[][] frustum2Adjusted = image.getFrustum2Adjusted();
		double[][] frustum3Adjusted = image.getFrustum3Adjusted();
		double[][] frustum4Adjusted = image.getFrustum4Adjusted();

		vtkPolyData tmp = null;

		for (int i = 0; i < smallBodies.size(); i++)
		{
			SmallBodyModel smallBodyModel = smallBodies.get(i);
			if (!footprintGenerated[i])
			{

				tmp = smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted[0], frustum1Adjusted[0],
						frustum3Adjusted[0], frustum4Adjusted[0], frustum2Adjusted[0]);
				if (tmp == null)
					return;

				// Need to clear out scalar data since if coloring data is being
				// shown,
				// then the color might mix-in with the image.
				tmp.GetCellData().SetScalars(null);
				tmp.GetPointData().SetScalars(null);

				footprint[i].DeepCopy(tmp);

				footprintGenerated[i] = true;
			}
			vtkPointData pointData = footprint[i].GetPointData();
			pointData.SetTCoords(textureCoords);
			PolyDataUtil.generateTextureCoordinates(getFrustum(), image.getImageWidth(), image.getImageHeight(),
					footprint[i]);
			pointData.Delete();

			shiftedFootprint[i].DeepCopy(footprint[i]);
			PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint[i], image.getOffset());

			if (image.getSmallBodyModel().getModelResolution() > 3)
			{
				String intersectionFileName = image.getPrerenderingFileNameBase() + "_frustumIntersection.vtk";
				footprintCacheOperator.saveToDisk(FileCache.instance().getFile(intersectionFileName).getPath(),
						footprint[0]);
			}
		}
		setFootprintGenerated(true);
	}

//	public vtkPolyData[] getFootprint(int defaultSlice)
//	{
//		if (footprint[0] != null && footprint[0].GetNumberOfPoints() > 0)
//			return footprint[0];
//		// first check the cache
//		vtkPolyData[] existingFootprint = footprintCacheOperator
//				.checkForExistingFootprint(image.getPrerenderingFileNameBase());
//		if (existingFootprint != null)
//		{
//			return existingFootprint;
//		}
//		else
//		{
//			vtkPolyData footprint = image.getSmallBodyModel().computeFrustumIntersection(
//					image.getSpacecraftPositionAdjusted()[defaultSlice], image.getFrustum1Adjusted()[defaultSlice],
//					image.getFrustum3Adjusted()[defaultSlice], image.getFrustum4Adjusted()[defaultSlice],
//					image.getFrustum2Adjusted()[defaultSlice]);
//			return footprint;
//		}
//	}

	public void setFootprintGenerated(boolean footprintGenerated)
	{
		for (int i=0; i<this.footprintGenerated.length; i++)
			this.footprintGenerated[i] = footprintGenerated;
	}

	public void setFootprintGenerated(boolean footprintGenerated, int slice)
	{
		this.footprintGenerated[slice] = footprintGenerated;
	}

	public Frustum getFrustum()
	{

		Frustum	frusta = new Frustum(image.getSpacecraftPositionAdjusted()[0],
					image.getFrustum1Adjusted()[0], image.getFrustum3Adjusted()[0],
					image.getFrustum4Adjusted()[0], image.getFrustum2Adjusted()[0]);
		return frusta;
	}

	/**
	 * @return the footprint
	 */
	public vtkPolyData getFootprint(int index)
	{
		return footprint[index];
	}

	/**
	 * @return the footprint
	 */
	public vtkPolyData[] getFootprint()
	{
		return footprint;
	}

	/**
	 * @return the shiftedFootprint
	 */
	public vtkPolyData[] getShiftedFootprint()
	{
		return shiftedFootprint;
	}
}
