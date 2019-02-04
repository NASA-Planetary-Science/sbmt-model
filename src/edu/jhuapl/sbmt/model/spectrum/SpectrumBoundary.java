package edu.jhuapl.sbmt.model.spectrum;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.spectrum.Spectrum.SpectrumKey;

public class SpectrumBoundary extends AbstractModel implements PropertyChangeListener
{
    private vtkActor actor;
    private vtkPolyData boundary;
    private vtkPolyDataMapper boundaryMapper;
    private double[] spacecraftPosition = new double[3];
    private double[] frustum1 = new double[3];
    private double[] frustum2 = new double[3];
    private double[] frustum3 = new double[3];
    private double[] boresightDirection = new double[3];
    private double[] upVector = new double[3];
    private Spectrum spectrum;
    private SmallBodyModel smallBodyModel;
    private static vtkPolyData emptyPolyData;
    private double offset =0.003;

    public SpectrumBoundary(Spectrum spectrum, SmallBodyModel smallBodyModel) throws IOException
    {
        this.spectrum = spectrum;
        this.smallBodyModel = smallBodyModel;
        this.actor = spectrum.outlineActor;
    }

    public void firePropertyChange()
    {
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
//      The following code seems broken and causes performance problems and issues with the colors
//      of the image list
//        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
//        {
////            System.out.println("Boundary MODEL_CHANGED event: " + evt.getSource().getClass().getSimpleName());
//            initialize();
//            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
//        }
    }

    @Override
    public List<vtkProp> getProps()
    {
        List<vtkProp> props = new ArrayList<vtkProp>();
        props.add(actor);
        return props;
    }

    public SpectrumKey getKey()
    {
        return spectrum.getKey();
    }

    public Spectrum getSpectrum()
    {
        return spectrum;
    }

    public void setVisibility(boolean visible)
    {
        if (visible == true)  { actor.VisibilityOn(); } else actor.VisibilityOff();
    }

    public void setBoundaryColor(Color color)
    {
        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();
        actor.VisibilityOn();
        actor.GetProperty().SetEdgeColor(r/255.0, g/255.0, b/255.0);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public int[] getBoundaryColor()
    {
        double[] c = new double[3];
        c = actor.GetProperty().GetEdgeColor();
        return new int[] {(int) (c[0]*255.0), (int) (c[1]*255.0), (int) (c[2]*255.0)};
    }

}
