package edu.jhuapl.sbmt.model.image.keys;


import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Preconditions;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.core.image.IImagingInstrument;
import edu.jhuapl.sbmt.core.image.ImageSource;
import edu.jhuapl.sbmt.core.image.ImageType;
import edu.jhuapl.sbmt.core.image.ImagingInstrument;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.StorableAsMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

/**
 * An ImageKey should be used to uniquely distinguish one image from another.
 * It also contains metadata about the image that may be necessary to know
 * before the image is loaded, such as the image projection information and
 * type of instrument used to generate the image.
 *
 * No two images will have the same values for the fields of this class.
 */
public class ImageKey implements ImageKeyInterface, StorableAsMetadata<ImageKey>
{
    // The path of the image as passed into the constructor. This is not the
    // same as fullpath but instead corresponds to the name needed to download
    // the file from the server (excluding the hostname and extension).
    public final String name;

    public final ImageSource source;

    public final FileType fileType;

    public final IImagingInstrument instrument;

    public final ImageType imageType;

    public String band;

    public int slice;

    public String pointingFile;

    public ImageKey(String name, ImageSource source)
    {
        this(name, source, null, null, null, null, 0, null);
    }

    public ImageKey(String name, ImageSource source, IImagingInstrument instrument)
    {
        this(name, source, null, null, instrument, null, 0, null);
    }

    public ImageKey(String name, ImageSource source, FileType fileType, ImageType imageType, IImagingInstrument instrument, String band, int slice, String pointingFile)
    {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(source);
        this.name = name;
        this.source = source;
        this.fileType = fileType;
        this.imageType = instrument != null ? instrument.getType(): imageType;
        this.instrument = instrument;
        this.band = band;
        this.slice = slice;
        this.pointingFile = pointingFile;
    }

    @Override
    public boolean equals(Object obj)
    {

//        String cleanedUpName2 = SafeURLPaths.instance().getString(name);

//        String cleanedUpOtherName2 = SafeURLPaths.instance().getString(((ImageKey)obj).name);
//        return cleanedUpName.equals(cleanedUpOtherName2) && source.equals(((ImageKey)obj).source);
    	if (((ImageKeyInterface)obj).getName().startsWith("C:") && (name.startsWith("C:")))
    		return name.equals(((ImageKeyInterface)obj).getName()) && source.equals(((ImageKeyInterface)obj).getSource());
    	else if (((ImageKeyInterface)obj).getName().startsWith("C:"))
    		return name.equals(SafeURLPaths.instance().getUrl(((ImageKeyInterface)obj).getName())) && source.equals(((ImageKeyInterface)obj).getSource());
    	else
    	{
    		String cleanedUpName = name.replace("file://", "");
    		String cleanedUpOtherName = ((ImageKeyInterface)obj).getName().replace("file://", "");
//    		System.out.println("Image.ImageKey: equals: cleaned up name " + cleanedUpName + " and source " + source);
//    		System.out.println("Image.ImageKey: equals: cleaned upname2 " + cleanedUpOtherName + " and source " + ((ImageKey)obj).source);
    		return FilenameUtils.getBaseName(cleanedUpName).equals(FilenameUtils.getBaseName(cleanedUpOtherName)) && source.equals(((ImageKeyInterface)obj).getSource());
    	}

//        return name.equals(((ImageKey)obj).name)
//                && source.equals(((ImageKey)obj).source)
////                && fileType.equals(((ImageKey)obj).fileType)
//                ;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    /* (non-Javadoc)
	 * @see edu.jhuapl.sbmt.model.image.ImageKeyInterface#toString()
	 */
    @Override
    public String toString()
    {
        return "ImageKey [name=" + name + ", source=" + source
                + ", fileType=" + fileType + ", instrument=" + instrument
                + ", imageType=" + imageType + ", band=" + band + ", slice="
                + slice + "]";
    }

    /* (non-Javadoc)
	 * @see edu.jhuapl.sbmt.model.image.ImageKeyInterface#getName()
	 */
    @Override
	public String getName()
    {
    	return name;
    }

    /* (non-Javadoc)
	 * @see edu.jhuapl.sbmt.model.image.ImageKeyInterface#getImageFilename()
	 */
    @Override
	public String getImageFilename()
    {
    	return name;
    }

    public ImageSource getSource()
	{
		return source;
	}

	public ImageType getImageType()
	{
		return imageType;
	}

	private static final Key<String> nameKey = Key.of("name");
    private static final Key<String> sourceKey = Key.of("source");
    private static final Key<String> fileTypeKey = Key.of("fileTypeKey");
    private static final Key<String> imageTypeKey = Key.of("imageType");
    private static final Key<Metadata> instrumentKey = Key.of("imagingInstrument");
    private static final Key<String> bandKey = Key.of("band");
    private static final Key<Integer> sliceKey = Key.of("slice");
    private static final Key<String> pointingFilenameKey = Key.of("pointingfilename");

    private static final Key<ImageKey> IMAGE_KEY = Key.of("image");

    @Override
    public Metadata store()
    {
        SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
        result.put(Key.of("customimagetype"), IMAGE_KEY.toString());
        result.put(nameKey, name);
        result.put(sourceKey, source.toString());
        result.put(fileTypeKey, fileType.toString());
        result.put(imageTypeKey, imageType.toString());
        result.put(instrumentKey, instrument.store());
        result.put(bandKey, band);
        result.put(sliceKey, slice);
        result.put(pointingFilenameKey, pointingFile);
        return result;
    }

	public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(IMAGE_KEY, (metadata) -> {

	        String name = metadata.get(nameKey);
	        ImageSource source = ImageSource.valueOf(metadata.get(sourceKey));
	        ImageType imageType = ImageType.valueOf(metadata.get(imageTypeKey));
	        ImagingInstrument instrument = new ImagingInstrument();
	        instrument.retrieve(metadata.get(instrumentKey));
	        int slice = metadata.get(sliceKey);
	        String band = metadata.get(bandKey);
	        FileType fileType = FileType.valueOf(metadata.get(fileTypeKey));
	        String pointingFilename = metadata.get(pointingFilenameKey);

	        ImageKey result = new ImageKey(name, source, fileType, imageType, instrument, band, slice, pointingFilename);

			return result;
		});
	}

	@Override
	public Key<ImageKey> getKey()
	{
		return IMAGE_KEY;
	}

	@Override
	public int getSlice()
	{
		return slice;
	}

	@Override
	public IImagingInstrument getInstrument()
	{
		return instrument;
	}

	@Override
	public FileType getFileType()
	{
		return fileType;
	}

	@Override
	public String getBand()
	{
		return band;
	}

	@Override
	public String getPointingFile()
	{
		return pointingFile;
	}

	@Override
	public String getOriginalName()
	{
		return name;
	}

	@Override
	public final String getFlip()
	{
		return getInstrument().getFlip();
	}

	@Override
	public final double getRotation()
	{
		return getInstrument().getRotation();
	}

}