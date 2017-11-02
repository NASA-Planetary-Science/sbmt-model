package edu.jhuapl.sbmt.model.spectrum;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;

import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.AbstractModel;

public abstract class Spectrum extends AbstractModel implements PropertyChangeListener
{
    public abstract DateTime getDateTime();
    public abstract SpectralInstrument getInstrument();
    public abstract double[] getBandCenters();
    public abstract double[] getSpectrum();
    public abstract String getFullPath();
    public abstract String getServerPath();

    public abstract void addPropertyChangeListener(PropertyChangeListener l);
    public abstract void removePropertyChangeListener(PropertyChangeListener l);

    public abstract void shiftFootprintToHeight(double d);
    public abstract vtkPolyData getUnshiftedFootprint();
    public abstract vtkPolyData getShiftedFootprint();
    public abstract vtkPolyData getSelectionPolyData();


    public abstract void setShowFrustum(boolean show);
    public abstract void setShowOutline(boolean show);
    public abstract void setShowToSunVector(boolean show);

    public abstract boolean isFrustumShowing();
    public abstract boolean isOutlineShowing();
    public abstract boolean isToSunVectorShowing();

    public abstract double[] getSpacecraftPosition();
    public abstract double[] getFrustumCenter();
    public abstract double[] getFrustumCorner(int i);
    public abstract double[] getFrustumOrigin();

    public abstract void setChannelColoring(int[] channels, double[] mins, double[] maxs);
    public abstract void updateChannelColoring();
    public abstract double evaluateDerivedParameters(int channel);
    public abstract double[] getChannelColor();

    public abstract void setSelected();
    public abstract void setUnselected();
    public abstract boolean isSelected();

    public abstract void saveSpectrum(File file) throws IOException;

    public static final String faceAreaFractionArrayName="faceAreaFraction";
}
