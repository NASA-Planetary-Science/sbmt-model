package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.image.model.PerspectiveImage;

public class PerspectiveImageFrustumRendererOperator
{
	vtkPolyData frustumPolyData;
	private vtkActor frustumActor;
	PerspectiveImage image;
	public double[] maxFrustumDepth;
	public double[] minFrustumDepth;
	public Frustum[] frusta;

	public PerspectiveImageFrustumRendererOperator(PerspectiveImage image)
	{
		this.image = image;
		maxFrustumDepth = new double[image.getImageDepth()];
		minFrustumDepth = new double[image.getImageDepth()];

	}

	// **********************
	// frustum
	// **********************

	public void calculateFrustum()
	{
		frustumActor = new vtkActor();
		double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
		double[][] frustum1Adjusted = image.getFrustum1Adjusted();
		double[][] frustum2Adjusted = image.getFrustum2Adjusted();
		double[][] frustum3Adjusted = image.getFrustum3Adjusted();
		double[][] frustum4Adjusted = image.getFrustum4Adjusted();
		int currentSlice = image.getCurrentSlice();
		if (frustumActor == null)
			return;
		// System.out.println("recalculateFrustum()");
		frustumPolyData = new vtkPolyData();

		vtkPoints points = new vtkPoints();
		vtkCellArray lines = new vtkCellArray();

		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(2);

		double maxFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice])
				+ image.getSmallBodyModel().getBoundingBoxDiagonalLength();
		double[] origin = spacecraftPositionAdjusted[currentSlice];
		double[] UL =
		{ origin[0] + frustum1Adjusted[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frustum1Adjusted[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frustum1Adjusted[currentSlice][2] * maxFrustumRayLength };
		double[] UR =
		{ origin[0] + frustum2Adjusted[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frustum2Adjusted[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frustum2Adjusted[currentSlice][2] * maxFrustumRayLength };
		double[] LL =
		{ origin[0] + frustum3Adjusted[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frustum3Adjusted[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frustum3Adjusted[currentSlice][2] * maxFrustumRayLength };
		double[] LR =
		{ origin[0] + frustum4Adjusted[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frustum4Adjusted[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frustum4Adjusted[currentSlice][2] * maxFrustumRayLength };

		double minFrustumRayLength = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice])
				- image.getSmallBodyModel().getBoundingBoxDiagonalLength();
		maxFrustumDepth[currentSlice] = maxFrustumRayLength; // a reasonable
																// approximation
																// for a max
																// bound on the
																// frustum depth
		minFrustumDepth[currentSlice] = minFrustumRayLength; // a reasonable
																// approximation
																// for a min
																// bound on the
																// frustum depth

		points.InsertNextPoint(spacecraftPositionAdjusted[currentSlice]);
		points.InsertNextPoint(UL);
		points.InsertNextPoint(UR);
		points.InsertNextPoint(LL);
		points.InsertNextPoint(LR);

		idList.SetId(0, 0);
		idList.SetId(1, 1);
		lines.InsertNextCell(idList);
		idList.SetId(0, 0);
		idList.SetId(1, 2);
		lines.InsertNextCell(idList);
		idList.SetId(0, 0);
		idList.SetId(1, 3);
		lines.InsertNextCell(idList);
		idList.SetId(0, 0);
		idList.SetId(1, 4);
		lines.InsertNextCell(idList);

		frustumPolyData.SetPoints(points);
		frustumPolyData.SetLines(lines);

		vtkPolyDataMapper frusMapper = new vtkPolyDataMapper();
		frusMapper.SetInputData(frustumPolyData);

		frustumActor.SetMapper(frusMapper);
	}

	/**
	 * @return the frustumPolyData
	 */
	public vtkPolyData getFrustumPolyData()
	{
		return frustumPolyData;
	}

	/**
	 * @return the frustumActor
	 */
	public vtkActor getFrustumActor()
	{
		return frustumActor;
	}

	public double getMaxFrustumDepth(int slice)
	{
		return maxFrustumDepth[slice];
	}

	public void setMaxFrustumDepth(int slice, double value)
	{
		maxFrustumDepth[slice] = value;
	}

	public double getMinFrustumDepth(int slice)
	{
		return minFrustumDepth[slice];
	}

	public void setMinFrustumDepth(int slice, double value)
	{
		minFrustumDepth[slice] = value;
	}

	public Frustum getFrustum(int slice)
    {
    	int sliceToUse = slice;

        if (frusta[sliceToUse] == null)
            frusta[sliceToUse] = new Frustum(image.getSpacecraftPositionAdjusted()[sliceToUse],
            							image.getFrustum1Adjusted()[sliceToUse],
            							image.getFrustum3Adjusted()[sliceToUse],
            							image.getFrustum4Adjusted()[sliceToUse],
            							image.getFrustum2Adjusted()[sliceToUse]);
        return frusta[sliceToUse];
    }
}
