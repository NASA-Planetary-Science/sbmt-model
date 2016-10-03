package edu.jhuapl.sbmt.model.europa;

import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;

import vtk.vtkProp;

public interface SurfacePatch extends ComboBoxModel
{
    /** name of patch */
    public String getName();

    /** ordinal identifier of patch */
    public int getId();

    /** total area of patch */
    public double getArea();

    /** current data type */
    public String getCurrentDataType();

    /** get scalar value at the specified prop */
    public Double getScalarValue(vtkProp prop, int cellId, double[] pickPosition);

    /** number of vertices in boundary */
    public int getBoundarySize();

    /** array of boundary vertices [boundarySize][3] */
    public double[][] getBoundaryVertex();

    /** number of vertices in patch */
    public int getPatchSize();

    /** array of patch vertices [patchSize][3] */
    public double[][] getPatchVertex();

    /** array of (i,j); indices into the patchVertex array [patchSize][2] */
    public int[][] getPatchIndex();

    /** Map of field names to lists of doubles containing scalar surface data */
    public Map<String, List<Double>> getSurfaceDataColumns();

    public double[] getNormalizedSurfaceData(String dataFieldName);

    public double[] getSurfaceData(String dataFieldName);

    public double[] getSurfaceDataRange(String dataFieldName);
}
