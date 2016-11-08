package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

import vtk.vtkTexture;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.Properties;


public abstract class Image extends AbstractModel implements PropertyChangeListener
{
    public static final String IMAGE_NAMES = "ImageNames"; // What name to give this image for display
    public static final String IMAGE_FILENAMES = "ImageFilenames"; // Filename of image on disk
    public static final String IMAGE_MAP_PATHS = "ImageMapPaths"; // For backwards compatibility, still read this in
    public static final String PROJECTION_TYPES = "ProjectionTypes";
    public static final String IMAGE_TYPES = "ImageTypes";
    public static final String IMAGE_ROTATIONS = "ImageRotations";
    public static final String IMAGE_FLIPS = "ImageFlips";




//    public static class ImagingInstrument
//    {
//        public SpectralMode spectralMode;
//        public QueryBase searchQuery;
//        public ImageSource[] searchImageSources;
//        public ImageType type;
//        public Instrument instrumentName;
//        public double rotation;
//        public String flip;
//
//        public ImagingInstrument()
//        {
//            this(SpectralMode.MONO, null, null, null, null, 0.0, "None");
//        }
//        public ImagingInstrument(double rotation, String flip)
//        {
//            this(SpectralMode.MONO, null, ImageType.GENERIC_IMAGE, null, null, rotation, flip);
//        }
//
////        public ImagingInstrument(ImageType type, Instrument instrumentName)
////        {
////            this(SpectralMode.MONO, null, type, null, instrumentName, 0.0, "None");
////        }
//
////        public ImagingInstrument(SpectralMode spectralMode)
////        {
////            this(spectralMode, null, null, null, null, 0.0, "None");
////        }
//
//        public ImagingInstrument(SpectralMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName)
//        {
//            this(spectralMode, searchQuery, type, searchImageSources, instrumentName, 0.0, "None");
//        }
//
//        public ImagingInstrument(SpectralMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrumentName, double rotation, String flip)
//        {
//            this.spectralMode = spectralMode;
//            this.searchQuery = searchQuery;
//            this.type = type;
//            this.searchImageSources = searchImageSources;
//            this.instrumentName = instrumentName;
//            this.rotation = rotation;
//            this.flip = flip;
//        }
//
//        public ImagingInstrument clone()
//        {
//            return new ImagingInstrument(spectralMode, searchQuery.clone(), type, searchImageSources.clone(), instrumentName, rotation, flip);
//        }
//    }
//

    /**
     * An ImageKey should be used to uniquely distinguish one image from another.
     * It also contains metadata about the image that may be necessary to know
     * before the image is loaded, such as the image projection information and
     * type of instrument used to generate the image.
     *
     * No two images will have the same values for the fields of this class.
     */
    public static class ImageKey
    {
        // The path of the image as passed into the constructor. This is not the
        // same as fullpath but instead corresponds to the name needed to download
        // the file from the server (excluding the hostname and extension).
        public String name;

        public ImageSource source;

        public FileType fileType;

        public ImagingInstrument instrument;

        public ImageType imageType;

        public String band;

        public int slice;


        public ImageKey(String name, ImageSource source)
        {
            this(name, source, null, null, null, null, 0);
        }

        public ImageKey(String name, ImageSource source, ImagingInstrument instrument)
        {
            this(name, source, null, null, instrument, null, 0);
        }

        public ImageKey(String name, ImageSource source, FileType fileType, ImageType imageType, ImagingInstrument instrument, String band, int slice)
        {
            this.name = name;
            this.source = source;
            this.fileType = fileType;
            this.imageType = imageType;
            this.instrument = instrument;
            this.band = band;
            this.slice = slice;
            if (instrument != null)
                this.imageType = instrument.type;
        }

        @Override
        public boolean equals(Object obj)
        {
            return name.equals(((ImageKey)obj).name)
                    && source.equals(((ImageKey)obj).source)
//                    && fileType.equals(((ImageKey)obj).fileType)
                    ;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }

    protected final ImageKey key;

    public Image(ImageKey key)
    {
        this.key = key;
    }

    public ImageKey getKey()
    {
        return key;
    }

    public String getImageName()
    {
        return new File(key.name).getName();
    }

    public void imageAboutToBeRemoved()
    {
        // By default do nothing. Let subclasses handle this.
    }

    abstract public vtkTexture getTexture();
    abstract public LinkedHashMap<String, String> getProperties() throws IOException;
    abstract public void setDisplayedImageRange(IntensityRange range);


    public void setInterpolate(boolean enable)
    {
        vtkTexture texture = getTexture();
        if (texture != null)
        {
            texture.SetInterpolate(enable ? 1 : 0);
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public boolean getInterpolate()
    {
        vtkTexture texture = getTexture();
        if (texture != null)
            return texture.GetInterpolate() == 0 ? false : true;
        else
            return true;
    }

    public String getPickStatusMessage(double p0, double p1)
    {
        // Number format
        DecimalFormat df = new DecimalFormat("#.0");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // Construct status message
        String status = "Pixel Coordinate = (";
        status += df.format(p1);
        status += ", ";
        status += df.format(p0);
        status += ")";
        return status;
    }

    abstract public int getNumberOfComponentsOfOriginalImage();

    public int[] getCurrentMask()
    {
        return new int[] {0, 0, 0, 0};
    }

    public void setCurrentMask(int[] masking)
    {

    }

    public void outputToOBJ(String filename)
    {
    }

}
