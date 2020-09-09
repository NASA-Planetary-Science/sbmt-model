package edu.jhuapl.sbmt.model.image.perspectiveImage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;

import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;

public class PerspectiveImageFrustum
{
	vtkPolyData frustumPolyData;
	private vtkActor frustumActor;
	Frustum[] frusta = new Frustum[1];
	private boolean showFrustum = false;
	public double[] maxFrustumDepth;
	public double[] minFrustumDepth;
	int nslices;
	private List<vtkProp> frustumActors = new ArrayList<vtkProp>();
	int currentSlice;
	double diagonalLength;
	double[][] scPos, frus1, frus2, frus3, frus4;
	boolean useDefaultFootprint;
	int defaultSlice;
    private String instrumentName;
    private Color frustumColor;

	public PerspectiveImageFrustum(PerspectiveImage image)
	{
		this.currentSlice = image.getCurrentSlice();
		this.diagonalLength = image.getSmallBodyModel().getBoundingBoxDiagonalLength();
		this.scPos = image.getSpacecraftPositionAdjusted();
		this.frus1 = image.getFrustum1Adjusted();
		this.frus2 = image.getFrustum2Adjusted();
		this.frus3 = image.getFrustum3Adjusted();
		this.frus4 = image.getFrustum4Adjusted();
		this.useDefaultFootprint = true;
		this.defaultSlice = image.getDefaultSlice();

		nslices = image.getImageDepth();
		maxFrustumDepth = new double[image.getImageDepth()];
		minFrustumDepth = new double[image.getImageDepth()];
		frusta = new Frustum[nslices];
	}

	public PerspectiveImageFrustum(int numSlices, int currentSlice, int defaultSlice, boolean useDefaultFootprint, double diagonalLength)
	{
//		System.out.println("PerspectiveImageFrustum: PerspectiveImageFrustum: ***********************");

		this.nslices = numSlices;
		this.currentSlice = currentSlice;
		this.diagonalLength = diagonalLength;
		this.scPos = new double[1][];
		this.frus1 = new double[1][];
		this.frus2 = new double[1][];
		this.frus3 = new double[1][];
		this.frus4 = new double[1][];
		this.useDefaultFootprint = useDefaultFootprint;
		this.defaultSlice = defaultSlice;
		maxFrustumDepth = new double[1];
		minFrustumDepth = new double[1];
		frusta = new Frustum[1];
	}

	public PerspectiveImageFrustum(int numSlices, int currentSlice, int defaultSlice, boolean useDefaultFootprint,
			double diagonalLength, double[] scPos, double[] frus1, double[] frus2, double[] frus3, double[] frus4)
	{
		this.nslices = numSlices;
		this.currentSlice = currentSlice;
		this.diagonalLength = diagonalLength;
		this.scPos = new double[1][];
		this.frus1 = new double[1][];
		this.frus2 = new double[1][];
		this.frus3 = new double[1][];
		this.frus4 = new double[1][];
		this.scPos[0] = scPos;
		this.frus1[0] = frus1;
		this.frus2[0] = frus2;
		this.frus3[0] = frus3;
		this.frus4[0] = frus4;
		this.useDefaultFootprint = useDefaultFootprint;
		this.defaultSlice = defaultSlice;
		maxFrustumDepth = new double[1];
		minFrustumDepth = new double[1];
		frusta = new Frustum[1];
	}

	public void updatePointing(PerspectiveImage image)
	{
		this.scPos = image.getSpacecraftPositionAdjusted();
		this.frus1 = image.getFrustum1Adjusted();
		this.frus2 = image.getFrustum2Adjusted();
		this.frus3 = image.getFrustum3Adjusted();
		this.frus4 = image.getFrustum4Adjusted();
	}

	public void updatePointing(double[] scPos, double[] frus1, double[] frus2, double[] frus3, double[] frus4)
	{
		this.scPos[0] = scPos;
		this.frus1[0] = frus1;
		this.frus2[0] = frus2;
		this.frus3[0] = frus3;
		this.frus4[0] = frus4;
		calculateFrustum();
	}

	public void initialize()
	{
		maxFrustumDepth = new double[nslices];
		minFrustumDepth = new double[nslices];
	}

	public void initSpacecraftStateVariables()
	{
		frusta = new Frustum[nslices];
	}

	void calculateFrustum()
	{
		if (frustumActor == null || scPos[0] == null)
			return;
		frustumPolyData = new vtkPolyData();

		vtkPoints points = new vtkPoints();
		vtkCellArray lines = new vtkCellArray();

		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(2);

		double maxFrustumRayLength = MathUtil.vnorm(scPos[currentSlice])
				+ diagonalLength;
		double[] origin = scPos[currentSlice];
		double[] UL =
		{ origin[0] + frus1[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frus1[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frus1[currentSlice][2] * maxFrustumRayLength };
		double[] UR =
		{ origin[0] + frus2[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frus2[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frus2[currentSlice][2] * maxFrustumRayLength };
		double[] LL =
		{ origin[0] + frus3[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frus3[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frus3[currentSlice][2] * maxFrustumRayLength };
		double[] LR =
		{ origin[0] + frus4[currentSlice][0] * maxFrustumRayLength,
				origin[1] + frus4[currentSlice][1] * maxFrustumRayLength,
				origin[2] + frus4[currentSlice][2] * maxFrustumRayLength };


//		System.out.println("PerspectiveImageFrustum: calculateFrustum: sc origin " + new Vector3D(origin));
//		System.out.println("PerspectiveImageFrustum: calculateFrustum: upper left is " + new Vector3D(UL));

		double minFrustumRayLength = MathUtil.vnorm(scPos[currentSlice])
				- diagonalLength;
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

		points.InsertNextPoint(scPos[currentSlice]);
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

	void setMaxFrustumDepth(int slice, double value)
	{
		maxFrustumDepth[slice] = value;
	}

	double getMinFrustumDepth(int slice)
	{
		return minFrustumDepth[slice];
	}

	void setMinFrustumDepth(int slice, double value)
	{
		minFrustumDepth[slice] = value;
	}

	double getMaxFrustumDepth(int slice)
	{
		return maxFrustumDepth[slice];
	}

	boolean isFrustumShowing()
	{
		return showFrustum;
	}

	Frustum getFrustum()
	{
		return getFrustum(currentSlice);
	}

	Frustum getFrustum(int slice)
	{
		int sliceToUse = slice;
		if (useDefaultFootprint)
			sliceToUse = defaultSlice;
		if (frusta[sliceToUse] == null)
		{
			frusta[sliceToUse] = new Frustum(scPos[sliceToUse],
					frus1[sliceToUse], frus3[sliceToUse],
					frus4[sliceToUse], frus2[sliceToUse]);
		}
		return frusta[sliceToUse];
	}

	void setShowFrustum(boolean b)
	{
		showFrustum = b;

		if (showFrustum)
		{
			frustumActor.VisibilityOn();
		}
		else
		{
			frustumActor.VisibilityOff();
		}

//		image.firePropertyChange(Properties.MODEL_CHANGED, null, this);
	}

	List<vtkProp> getProps()
	{
		if (frustumActor == null)
		{
			frustumActor = new vtkActor();

			calculateFrustum();
			vtkProperty frustumProperty = frustumActor.GetProperty();
			frustumProperty.SetColor((double)frustumColor.getRed()/255.0, (double)frustumColor.getGreen()/255.0, (double)frustumColor.getBlue()/255.0);
			frustumProperty.SetLineWidth(2.0);
			frustumActor.VisibilityOff();

			frustumActors.add(frustumActor);
		}
		return frustumActors;

	}

	public void setColor(Color color)
	{
		this.frustumColor = color;
	}

	public vtkActor getFrustumActor()
	{
		if (frustumActor == null && scPos[0] != null)
		{
			getProps();
		}
		return frustumActor;
	}

	/**
	 * @return the instrumentName
	 */
	public String getInstrumentName()
	{
		return instrumentName;
	}

	/**
	 * @param instrumentName the instrumentName to set
	 */
	public void setInstrumentName(String instrumentName)
	{
		this.instrumentName = instrumentName;
	}

}