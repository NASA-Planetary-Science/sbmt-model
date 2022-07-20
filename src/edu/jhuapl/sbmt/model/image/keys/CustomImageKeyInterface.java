package edu.jhuapl.sbmt.model.image.keys;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import edu.jhuapl.saavtk.model.FileType;
import edu.jhuapl.sbmt.core.image.ImageSource;
import edu.jhuapl.sbmt.core.image.ImageType;
import edu.jhuapl.sbmt.gui.image.ui.custom.CustomImageImporterDialog.ProjectionType;
import edu.jhuapl.sbmt.model.image.ImageKeyInterface;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.ProvidesGenericObjectFromMetadata;
import crucible.crust.metadata.impl.InstanceGetter;

public interface CustomImageKeyInterface extends ImageKeyInterface
{
	public Date getDate();

	public ProjectionType getProjectionType();

	public void setImagefilename(String imagefilename);

	static CustomImageKeyInterface retrieveOldFormat(Metadata metadata)
	{
		 Key<ProjectionType> projectionTypeKey = Key.of("projectionType");
		 if (metadata.get(projectionTypeKey) == ProjectionType.PERSPECTIVE)
		 {
			 Key<String> nameKey = Key.of("name");
			 Key<String> imageFileNameKey = Key.of("imagefilename");
			 Key<String> imageTypeKey = Key.of("imageType");
			 Key<Double> rotationKey = Key.of("rotation");
			 Key<String> flipKey = Key.of("flip");
			 Key<String> infoFilenameKey = Key.of("infofilename");
			 Key<String> sumFilenameKey = Key.of("sumfilename");
			 String pointingFileName;
			 FileType fileType;
			 ImageSource imageSource;
			 if (!metadata.get(infoFilenameKey).isEmpty())
			 {
				 imageSource = ImageSource.SPICE;
				 fileType = FileType.INFO;
	        	 pointingFileName = metadata.get(infoFilenameKey);
			 }
			 else
			 {
				 imageSource = ImageSource.GASKELL_UPDATED;
				 fileType = FileType.SUM;
	        	 pointingFileName = metadata.get(sumFilenameKey);
			 }
			 return new CustomPerspectiveImageKey(metadata.get(nameKey), metadata.get(imageFileNameKey), imageSource, ImageType.valueOf(metadata.get(imageTypeKey)),
					 metadata.get(rotationKey), metadata.get(flipKey), fileType, pointingFileName, new Date(), metadata.get(nameKey));
		 }
		 else
		 {
			 Key<String> nameKey = Key.of("name");
			 Key<String> imageFileNameKey = Key.of("imagefilename");
			 Key<String> imageTypeKey = Key.of("imageType");
			 Key<Double> lllatKey = Key.of("lllat");
			 Key<Double> lllonKey = Key.of("lllon");
			 Key<Double> urlatKey = Key.of("urlat");
			 Key<Double> urlonKey = Key.of("urlon");
			 CustomCylindricalImageKey key = new CustomCylindricalImageKey(metadata.get(nameKey), metadata.get(imageFileNameKey), ImageType.valueOf(metadata.get(imageTypeKey)), ImageSource.LOCAL_PERSPECTIVE, new Date(), metadata.get(nameKey));
			 key.setLllat(metadata.get(lllatKey));
			 key.setLllon(metadata.get(lllonKey));
			 key.setUrlat(metadata.get(urlatKey));
			 key.setUrlon(metadata.get(urlonKey));
			 return key;
		 }
	}

	static CustomImageKeyInterface retrieve(Metadata objectMetadata)
	{
		final Key<String> key = Key.of("customimagetype");
//		Key<String> key = objectMetadata.get(Key.of("customimagetype"));
		if (key.toString().equals("CUSTOM_CYLINDRICAL_IMAGE_KEY"))
		{
			Key<CustomCylindricalImageKey> CUSTOM_CYLINDRICAL_IMAGE_KEY = Key.of("customCylindricalImage");
			ProvidesGenericObjectFromMetadata<CustomCylindricalImageKey> metadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(CUSTOM_CYLINDRICAL_IMAGE_KEY);
			return metadata.provide(objectMetadata);
		}
		else
		{
			Key<CustomPerspectiveImageKey> CUSTOM_PERSPECTIVE_IMAGE_KEY = Key.of("customPerspectiveImage");
			ProvidesGenericObjectFromMetadata<CustomPerspectiveImageKey> metadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(CUSTOM_PERSPECTIVE_IMAGE_KEY);
			return metadata.provide(objectMetadata);
		}
	}

    default List<String> toList()
    {
        List<String> string = new Vector<String>();
        string.add(getName());
        string.add(""+getDate().getTime());
        return string;

    }
}
