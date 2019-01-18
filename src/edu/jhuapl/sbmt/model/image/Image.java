package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

import com.google.common.base.Preconditions;

import vtk.vtkPolyData;
import vtk.vtkProp;
import vtk.vtkTexture;

import edu.jhuapl.saavtk.model.AbstractModel;
import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk.util.SafeURLPaths;


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
        public final String name;

        public final ImageSource source;

        public final FileType fileType;

        public final ImagingInstrument instrument;

        public final ImageType imageType;

        public String band;

        public int slice;

        public String pointingFile;

        public ImageKey(String name, ImageSource source)
        {
            this(name, source, null, null, null, null, 0, null);
        }

        public ImageKey(String name, ImageSource source, ImagingInstrument instrument)
        {
            this(name, source, null, null, instrument, null, 0, null);
        }

        public ImageKey(String name, ImageSource source, FileType fileType, ImageType imageType, ImagingInstrument instrument, String band, int slice, String pointingFile)
        {
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(source);
            this.name = name;
            this.source = source;
            this.fileType = fileType;
            this.imageType = instrument != null ? instrument.type : imageType;
            this.instrument = instrument;
            this.band = band;
            this.slice = slice;
            this.pointingFile = pointingFile;
        }

        @Override
        public boolean equals(Object obj)
        {

//            String cleanedUpName2 = SafeURLPaths.instance().getString(name);

//            String cleanedUpOtherName2 = SafeURLPaths.instance().getString(((ImageKey)obj).name);
//            return cleanedUpName.equals(cleanedUpOtherName2) && source.equals(((ImageKey)obj).source);
        	if (((ImageKey)obj).name.startsWith("C:") && (name.startsWith("C:")))
        		return name.equals(((ImageKey)obj).name) && source.equals(((ImageKey)obj).source);
        	else if (((ImageKey)obj).name.startsWith("C:"))
        		return name.equals(SafeURLPaths.instance().getUrl(((ImageKey)obj).name)) && source.equals(((ImageKey)obj).source);
        	else
        	{
        		String cleanedUpName = name.replace("file://", "");
        		String cleanedUpOtherName = ((ImageKey)obj).name.replace("file://", "");
//        		System.out.println("Image.ImageKey: equals: cleaned up name " + cleanedUpName + " and source " + source);
//        		System.out.println("Image.ImageKey: equals: cleaned upname2 " + cleanedUpOtherName + " and source " + ((ImageKey)obj).source);
        		return cleanedUpName.equals(cleanedUpOtherName) /*&& source.equals(((ImageKey)obj).source)*/;
        	}

//            return name.equals(((ImageKey)obj).name)
//                    && source.equals(((ImageKey)obj).source)
////                    && fileType.equals(((ImageKey)obj).fileType)
//                    ;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        @Override
        public String toString()
        {
            return "ImageKey [name=" + name + ", source=" + source
                    + ", fileType=" + fileType + ", instrument=" + instrument
                    + ", imageType=" + imageType + ", band=" + band + ", slice="
                    + slice + "]";
        }

        public String getName()
        {
        	return name;
        }

        public String getImageFilename()
        {
        	return name;
        }


    }


    public static void applyOffset(Image image, double offset)
    {
        vtkPolyData shiftedFootprint = image.getShiftedFootprint();
        shiftedFootprint.DeepCopy(image.getUnshiftedFootprint());
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedFootprint, offset);
    }

    protected final ImageKey key;
    // Use a lazily initialized Double to avoid calling
    // the non-final method getDefaultOffset from the  constructor.
    private Double offset;

    public Image(ImageKey key)
    {
        this.key = key;
        this.offset = null;
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

    /**
     * The shifted footprint is the original footprint shifted slightly in the
     * normal direction so that it will be rendered correctly and not obscured
     * by the asteroid.
     * @return
     */
    public abstract vtkPolyData getShiftedFootprint();

    /**
     * The original footprint whose cells exactly overlap the original asteroid.
     * If rendered as is, it would interfere with the asteroid.
     * @return
     */
    protected abstract vtkPolyData getUnshiftedFootprint();

    @Override
    public double getOffset() {
        if (offset == null) {
            offset = getDefaultOffset();
        }
        return offset;
    }

    @Override
    public void setOffset(double offset) {
        this.offset = offset;
        applyOffset(this, getOffset());
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

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

    @Override
    public String getClickStatusBarText(vtkProp prop, int cellId, double[] pickPosition)
    {
        // Number format
        DecimalFormat df = new DecimalFormat("#.0");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // Construct status message
        String status = "Pixel Coordinate = (";
        status += df.format(pickPosition[0]);
        status += ", ";
        status += df.format(pickPosition[1]);
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
