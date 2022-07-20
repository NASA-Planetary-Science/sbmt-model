package edu.jhuapl.sbmt.model.image.keys;

import java.util.Date;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.sbmt.core.image.IImagingInstrument;
import edu.jhuapl.sbmt.core.image.ImageSource;
import edu.jhuapl.sbmt.core.image.ImageType;
import edu.jhuapl.sbmt.gui.image.ui.custom.CustomImageImporterDialog.ProjectionType;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.StorableAsMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

public class CustomPerspectiveImageKey implements StorableAsMetadata<CustomPerspectiveImageKey>, CustomImageKeyInterface
{
	public String name = ""; // name to call this image for display purposes
    public String imagefilename = ""; // filename of image on disk
    public ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    public double rotation = 0.0;
    public String flip = "None";
    public String pointingFilename = "null";
    public final ImageType imageType;
    public final FileType fileType;
    public final ImageSource source;
    private final Date date;
    private String originalName;

    private static final Key<String> nameKey = Key.of("name");
    private static final Key<String> imageFileNameKey = Key.of("imagefilename");
    private static final Key<String> sourceKey = Key.of("source");
    private static final Key<String> imageTypeKey = Key.of("imageType");
    private static final Key<Double> rotationKey = Key.of("rotation");
    private static final Key<String> flipKey = Key.of("flip");
    private static final Key<String> fileTypeKey = Key.of("fileTypeKey");
    private static final Key<String> pointingFilenameKey = Key.of("pointingfilename");
    private static final Key<Date> dateKey = Key.of("date");
    private static final Key<String> originalNameKey = Key.of("originalName");

    private static final Key<CustomPerspectiveImageKey> CUSTOM_PERSPECTIVE_IMAGE_KEY = Key.of("customPerspectiveImage");



    public CustomPerspectiveImageKey(String name, String imagefilename, ImageSource source, ImageType imageType,
    		double rotation, String flip, FileType fileType, String pointingFilename, Date date, String originalName)
    {
    	this.name = name;
    	this.imagefilename = imagefilename;
    	this.source = source;
    	this.imageType = imageType;
    	this.rotation = rotation;
    	this.flip = flip;
    	this.fileType = fileType;
    	this.pointingFilename = pointingFilename;
    	this.date = date;
    	this.originalName = originalName;

    }

    @Override
    public String toString()
    {
        if (imageType == ImageType.GENERIC_IMAGE)
            return name + ", Perspective" + ", " + imageType + ", Rotate " + rotation + ", Flip " + flip;
        else
            return name + "Image name: " + imagefilename +", Perspective" + ", " + imageType;
    }

    public String getName()
    {
    	return name;
    }

    public String getImageFilename()
    {
    	return imagefilename;
    }


	public void setImagefilename(String imagefilename)
	{
		this.imagefilename = imagefilename;
	}

	public ImageType getImageType()
	{
		return imageType;
	}

	public ProjectionType getProjectionType()
	{
		return projectionType;
	}

	public ImageSource getSource()
	{
		return source;
	}

	public double getRotation()
	{
		return rotation;
	}

	public String getFlip()
	{
		return flip;
	}

	@Override
    public Metadata store()
    {
        SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
        result.put(nameKey, name);
        result.put(imageFileNameKey, imagefilename);
        result.put(sourceKey, source.toString());
        result.put(imageTypeKey, imageType.toString());
        result.put(rotationKey, rotation);
        result.put(flipKey, flip);
        result.put(fileTypeKey, fileType.toString());
        result.put(pointingFilenameKey, pointingFilename);
        result.put(dateKey, date);
        result.put(originalNameKey, originalName);
        return result;
    }

	public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(CUSTOM_PERSPECTIVE_IMAGE_KEY, (metadata) -> {

	        String name = metadata.get(nameKey);
	        String imagefilename = metadata.get(imageFileNameKey);
	        ImageSource source = ImageSource.valueFor(metadata.get(sourceKey));
	        ImageType imageType = ImageType.valueOf(metadata.get(imageTypeKey));
	        double rotation = metadata.get(rotationKey);
	        String flip = metadata.get(flipKey);
	        FileType fileType = FileType.valueOf(metadata.get(fileTypeKey));
	        String pointingFilename = metadata.get(pointingFilenameKey);
	        Date date = metadata.get(dateKey);
	        String originalName = metadata.hasKey(originalNameKey) ? metadata.get(originalNameKey) : name;

	        return new CustomPerspectiveImageKey(name, imagefilename, source, imageType, rotation, flip, fileType, pointingFilename, date, originalName);
		}, CustomPerspectiveImageKey.class, key -> {
		    return key.store();
		});
	}

	@Override
	public Key<CustomPerspectiveImageKey> getKey()
	{
		return CUSTOM_PERSPECTIVE_IMAGE_KEY;
	}

	@Override
	public int getSlice()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IImagingInstrument getInstrument()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileType getFileType()
	{
		return fileType;
	}

	@Override
	public String getBand()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPointingFile()
	{
		return pointingFilename;
	}

	@Override
	public Date getDate()
	{
		return date;
	}

	public String getOriginalName()
	{
		return originalName;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileType == null) ? 0 : fileType.hashCode());
		result = prime * result + ((flip == null) ? 0 : flip.hashCode());
		result = prime * result + ((imageType == null) ? 0 : imageType.hashCode());
		result = prime * result + ((imagefilename == null) ? 0 : imagefilename.hashCode());
		result = prime * result + ((pointingFilename == null) ? 0 : pointingFilename.hashCode());
		result = prime * result + ((projectionType == null) ? 0 : projectionType.hashCode());
		long temp;
		temp = Double.doubleToLongBits(rotation);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		CustomPerspectiveImageKey other = (CustomPerspectiveImageKey) obj;
		if (fileType != other.fileType)
		{
			return false;
		}
		if (flip == null)
		{
			if (other.flip != null)
			{
				return false;
			}
		} else if (!flip.equals(other.flip))
		{
			return false;
		}
		if (imageType != other.imageType)
		{
			return false;
		}
		if (imagefilename == null)
		{
			if (other.imagefilename != null)
			{
				return false;
			}
		} else if (!imagefilename.equals(other.imagefilename))
		{
			return false;
		}
		if (pointingFilename == null)
		{
			if (other.pointingFilename != null)
			{
				return false;
			}
		} else if (!pointingFilename.equals(other.pointingFilename))
		{
			return false;
		}
		if (projectionType != other.projectionType)
		{
			return false;
		}
		if (Double.doubleToLongBits(rotation) != Double.doubleToLongBits(other.rotation))
		{
			return false;
		}
		if (source != other.source)
		{
			return false;
		}
		return true;
	}

//    @Override
//    public void retrieve(Metadata source)
//    {
//        name = source.get(nameKey);
//        imagefilename = source.get(imageFileNameKey);
//        projectionType = ProjectionType.valueOf(source.get(projectionKey));
//        imageType = ImageType.valueOf(source.get(imageTypeKey));
//        rotation = source.get(rotationKey);
//        flip = source.get(flipKey);
//        sumfilename = source.get(sumfilenameKey);
//        infofilename = source.get(infofileKey);
//    }

}
