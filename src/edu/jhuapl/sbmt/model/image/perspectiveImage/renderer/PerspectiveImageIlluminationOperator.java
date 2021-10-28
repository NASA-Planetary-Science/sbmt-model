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
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

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
        	int currentSlice = image.getCurrentSlice();
            normalsFilter.SetInputData(footprint[currentSlice]);
            normalsFilter.SetComputeCellNormals(1);
            normalsFilter.SetComputePointNormals(0);
            // normalsFilter.AutoOrientNormalsOn();
            // normalsFilter.ConsistencyOn();
            normalsFilter.SplittingOff();
            normalsFilter.Update();

            if (footprint != null && footprint[currentSlice] != null)
            {
                vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
                footprint[currentSlice].DeepCopy(normalsFilterOutput);
                normalsGenerated = true;
            }
        }
    }

    // Computes the incidence, emission, and phase at a point on the footprint with
    // a given normal.
    // (I.e. the normal of the plate which the point is lying on).
    // The output is a 3-vector with the first component equal to the incidence,
    // the second component equal to the emission and the third component equal to
    // the phase.
    double[] computeIlluminationAnglesAtPoint(double[] pt, double[] normal)
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

    void computeIlluminationAngles()
    {
    	int currentSlice = image.getCurrentSlice();
        if (footprintGenerated[currentSlice] == false)
            loadFootprint();

        computeCellNormals();

        int numberOfCells = footprint[currentSlice].GetNumberOfCells();

        vtkPoints points = footprint[currentSlice].GetPoints();
        vtkCellData footprintCellData = footprint[currentSlice].GetCellData();
        vtkDataArray normals = footprintCellData.GetNormals();

        this.minEmission = Double.MAX_VALUE;
        this.maxEmission = -Double.MAX_VALUE;
        this.minIncidence = Double.MAX_VALUE;
        this.maxIncidence = -Double.MAX_VALUE;
        this.minPhase = Double.MAX_VALUE;
        this.maxPhase = -Double.MAX_VALUE;

        for (int i = 0; i < numberOfCells; ++i)
        {
            vtkCell cell = footprint[currentSlice].GetCell(i);
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

    double getMinIncidence()
    {
        return minIncidence;
    }

    double getMaxIncidence()
    {
        return maxIncidence;
    }

    double getMinEmission()
    {
        return minEmission;
    }

    double getMaxEmission()
    {
        return maxEmission;
    }

    double getMinPhase()
    {
        return minPhase;
    }

    double getMaxPhase()
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
