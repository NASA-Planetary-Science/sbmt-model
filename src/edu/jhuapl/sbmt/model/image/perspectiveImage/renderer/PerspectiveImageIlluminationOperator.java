package edu.jhuapl.sbmt.model.image.perspectiveImage.renderer;

import java.beans.PropertyChangeEvent;

import vtk.vtkCell;
import vtk.vtkCellData;
import vtk.vtkDataArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;

import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.image.model.PerspectiveImage;

public class PerspectiveImageIlluminationOperator
{
	PerspectiveImage image;
    private boolean normalsGenerated = false;
    private vtkPolyDataNormals normalsFilter;
    private double minIncidence = Double.MAX_VALUE;
    private double maxIncidence = -Double.MAX_VALUE;
    private double minEmission = Double.MAX_VALUE;
    private double maxEmission = -Double.MAX_VALUE;
    private double minPhase = Double.MAX_VALUE;
    private double maxPhase = -Double.MAX_VALUE;

	public PerspectiveImageIlluminationOperator(PerspectiveImage image)
	{
		this.image = image;
		normalsFilter = new vtkPolyDataNormals();
	}

    public boolean isNormalsGenerated()
    {
        return normalsGenerated;
    }

    public void setNormalsGenerated(boolean normalsGenerated)
    {
        this.normalsGenerated = normalsGenerated;
    }

    void computeCellNormals()
    {
        if (normalsGenerated == false)
        {
        	vtkPolyData[] footprints = image.getRendererHelper().getFootprint();
        	for (int currentSlice = 0; currentSlice<footprints.length; currentSlice++)
        	{
	            normalsFilter.SetInputData(footprints[currentSlice]);
	            normalsFilter.SetComputeCellNormals(1);
	            normalsFilter.SetComputePointNormals(0);
	            normalsFilter.SplittingOff();
	            normalsFilter.Update();

	            if (footprints != null && footprints[currentSlice] != null)
	            {
	                vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
	                footprints[currentSlice].DeepCopy(normalsFilterOutput);
	            }
        	}
        	normalsGenerated = true;
        }
    }

    // Computes the incidence, emission, and phase at a point on the footprint with
    // a given normal.
    // (I.e. the normal of the plate which the point is lying on).
    // The output is a 3-vector with the first component equal to the incidence,
    // the second component equal to the emission and the third component equal to
    // the phase.
    public double[] computeIlluminationAnglesAtPoint(double[] pt, double[] normal)
    {
    	int currentSlice = image.getCurrentSlice();
    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
        double[] scvec = {
        		spacecraftPositionAdjusted[currentSlice][0] - pt[0],
        		spacecraftPositionAdjusted[currentSlice][1] - pt[1],
        		spacecraftPositionAdjusted[currentSlice][2] - pt[2] };

        double[] sunVectorAdjusted = image.getSunVector();
        double incidence = MathUtil.vsep(normal, sunVectorAdjusted) * 180.0 / Math.PI;
        double emission = MathUtil.vsep(normal, scvec) * 180.0 / Math.PI;
        double phase = MathUtil.vsep(sunVectorAdjusted, scvec) * 180.0 / Math.PI;

        double[] angles = { incidence, emission, phase };

        return angles;
    }

    public void computeIlluminationAngles()
    {
//    	int currentSlice = image.getCurrentSlice();
        this.minEmission = Double.MAX_VALUE;
        this.maxEmission = -Double.MAX_VALUE;
        this.minIncidence = Double.MAX_VALUE;
        this.maxIncidence = -Double.MAX_VALUE;
        this.minPhase = Double.MAX_VALUE;
        this.maxPhase = -Double.MAX_VALUE;
//        if (footprintGenerated[currentSlice] == false)
//            loadFootprint();

        computeCellNormals();

        vtkPolyData[] footprints = image.getRendererHelper().getFootprint();

    	for (int currentSlice = 0; currentSlice<footprints.length; currentSlice++)
    	{
	        int numberOfCells = footprints[currentSlice].GetNumberOfCells();

	        vtkPoints points = footprints[currentSlice].GetPoints();
	        vtkCellData footprintCellData = footprints[currentSlice].GetCellData();
	        vtkDataArray normals = footprintCellData.GetNormals();

	        for (int i = 0; i < numberOfCells; ++i)
	        {
	            vtkCell cell = footprints[currentSlice].GetCell(i);
	            double[] pt0 = points.GetPoint(cell.GetPointId(0));
	            double[] pt1 = points.GetPoint(cell.GetPointId(1));
	            double[] pt2 = points.GetPoint(cell.GetPointId(2));
	            double[] centroid = {
	                    (pt0[0] + pt1[0] + pt2[0]) / 3.0,
	                    (pt0[1] + pt1[1] + pt2[1]) / 3.0,
	                    (pt0[2] + pt1[2] + pt2[2]) / 3.0
	            };
	            double[] normal = normals.GetTuple3(i);

	            double[] angles = computeIlluminationAnglesAtPoint(centroid, normal);
	            double incidence = angles[0];
	            double emission = angles[1];
	            double phase = angles[2];

	            if (incidence < minIncidence)
	                minIncidence = incidence;
	            if (incidence > maxIncidence)
	                maxIncidence = incidence;
	            if (emission < minEmission)
	                minEmission = emission;
	            if (emission > maxEmission)
	                maxEmission = emission;
	            if (phase < minPhase)
	                minPhase = phase;
	            if (phase > maxPhase)
	                maxPhase = phase;
	            cell.Delete();
	        }

	        points.Delete();
	        footprintCellData.Delete();
	        if (normals != null)
	            normals.Delete();
    	}
    }

    public double getMinIncidence()
    {
        return minIncidence;
    }

    public double getMaxIncidence()
    {
        return maxIncidence;
    }

    public double getMinEmission()
    {
        return minEmission;
    }

    public double getMaxEmission()
    {
        return maxEmission;
    }

    public double getMinPhase()
    {
        return minPhase;
    }

    public double getMaxPhase()
    {
        return maxPhase;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            normalsGenerated = false;
            this.minEmission = Double.MAX_VALUE;
            this.maxEmission = -Double.MAX_VALUE;
            this.minIncidence = Double.MAX_VALUE;
            this.maxIncidence = -Double.MAX_VALUE;
            this.minPhase = Double.MAX_VALUE;
            this.maxPhase = -Double.MAX_VALUE;
        }
    }
}
