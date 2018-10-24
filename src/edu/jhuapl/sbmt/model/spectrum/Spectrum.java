package edu.jhuapl.sbmt.model.spectrum;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;

import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.sbmt.model.bennu.SearchSpec;
import edu.jhuapl.sbmt.model.spectrum.coloring.SpectrumColoringStyle;
import edu.jhuapl.sbmt.model.spectrum.instruments.SpectralInstrument;

public abstract class Spectrum extends AbstractModel implements PropertyChangeListener
{
    public static final String SPECTRUM_NAMES = "SpectrumNames"; // What name to give this image for display
    public static final String SPECTRUM_FILENAMES = "SpectrumFilenames"; // Filename of image on disk
    public static final String SPECTRUM_MAP_PATHS = "SpectrumMapPaths"; // For backwards compatibility, still read this in
//    public static final String PROJECTION_TYPES = "ProjectionTypes";
    public static final String SPECTRUM_TYPES = "SpectrumTypes";
    public boolean isCustomSpectra = false;

    public abstract DateTime getDateTime();
    public abstract SpectralInstrument getInstrument();
    public abstract double[] getBandCenters();
    public abstract double[] getSpectrum();
    public abstract String getFullPath();
    public abstract String getSpectrumPathOnServer();

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
    public abstract double[] getToSunUnitVector();

    public abstract void setChannelColoring(int[] channels, double[] mins, double[] maxs);
    public abstract void updateChannelColoring();
    public abstract double evaluateDerivedParameters(int channel);
    public abstract double[] getChannelColor();

    public abstract void setSelected();
    public abstract void setUnselected();
    public abstract boolean isSelected();

    public abstract void saveSpectrum(File file) throws IOException;

    public static final String faceAreaFractionArrayName="faceAreaFraction";
    public abstract void setColoringStyle(SpectrumColoringStyle coloringStyle);
    public abstract void setMetadata(SearchSpec spec);
    public abstract SearchSpec getMetadata();

    protected SpectrumKey key;

    public Spectrum()
    {
        this.key = null;
    }

    public Spectrum(SpectrumKey key)
    {
        this.key = key;
    }

    public SpectrumKey getKey()
    {
        return key;
    }

    public void setKey(SpectrumKey key)
    {
        this.key = key;
    }

    public String getSpectrumName()
    {
        return new File(key.name).getName();
    }

    /**
     * An SpectrumKey should be used to uniquely distinguish one image from another.
     * It also contains metadata about the image that may be necessary to know
     * before the image is loaded, such as the image projection information and
     * type of instrument used to generate the image.
     *
     * No two images will have the same values for the fields of this class.
     */
    public static class SpectrumKey
    {
        // The path of the image as passed into the constructor. This is not the
        // same as fullpath but instead corresponds to the name needed to download
        // the file from the server (excluding the hostname and extension).
        public String name;

//        public ImageSource source;

        public FileType fileType;

        public SpectralInstrument instrument;

        public SpectraType spectrumType;

        public String band;

        public int slice;


        public SpectrumKey(String name)//, ImageSource source)
        {
            this(name, null, null, null);
        }

        public SpectrumKey(String name/*, ImageSource source*/, SpectralInstrument instrument)
        {
            this(name, null, null, instrument);
        }

        public SpectrumKey(String name, /*ImageSource source,*/ FileType fileType, SpectraType imageType, SpectralInstrument instrument)
        {
            this.name = name;
//            this.source = source;
            this.fileType = fileType;
            this.spectrumType = imageType;
            this.instrument = instrument;
        }

        @Override
        public boolean equals(Object obj)
        {
            return name.equals(((SpectrumKey)obj).name);
                   // && source.equals(((ImageKey)obj).source);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        @Override
        public String toString()
        {
            return "SpectrumKey [name=" + name
                    + ", fileType=" + fileType + ", instrument=" + instrument
                    + ", imageType=" + spectrumType + ", band=" + band + ", slice="
                    + slice + "]";
        }
    }
}
