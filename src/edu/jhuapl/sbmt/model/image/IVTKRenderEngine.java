package edu.jhuapl.sbmt.model.image;

import java.util.List;

import vtk.vtkActor;
import vtk.vtkFloatArray;
import vtk.vtkImageCanvasSource2D;
import vtk.vtkImageData;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.IntensityRange;


public interface IVTKRenderEngine
{

    List<vtkProp> getProps();

    void calculateFrustum();

    void setDisplayedImageRange(IntensityRange range);

    void loadFootprint();

    vtkPolyData generateBoundary();

    vtkImageData getRawImage();

    public void setRawImage(vtkImageData rawImage);

    Frustum getFrustum();

    Frustum getFrustum(int slice);

    public Frustum[] getFrusta();

    public vtkActor getFrustumActor();

    public vtkActor getFootprintActor();

    public vtkPolyData[] getFootprint();

    public void computeCellNormals();

    public vtkImageCanvasSource2D getMaskSource();

    public void reset();

    public void resetFrustaAndFootprints(int nslices);

    public vtkFloatArray getTextureCoords();

    public vtkPolyDataNormals getNormalsFilter();

    public vtkImageData getDisplayedImage();

    public vtkPolyData getShiftedFootprint();

    public vtkPolyData getShiftedFootprint(int index);

    public vtkPolyData getUnshiftedFootprint();

    public vtkTexture getTexture();

    public vtkPolyData getFootprint(int defaultSlice);

    public vtkPolyData getFrustumPolyData();

    public double getMaxFrustumDepth(int slice);

    public double getMinFrustumDepth(int slice);

    public void setMaxFrustumDepth(int slice, double value);

    public void setMinFrustumDepth(int slice, double value);

    public void setMaskSource(vtkImageCanvasSource2D maskSource);

    public void getPointingInformation();

}
