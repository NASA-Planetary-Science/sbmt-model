package edu.jhuapl.sbmt.model.image;

import java.io.File;
import java.util.List;

import com.google.common.base.Preconditions;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.gui.image.model.custom.CustomCylindricalImageKey;
import edu.jhuapl.sbmt.model.image.ColorImage.ColorImageKey;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

/**
 * Factory pattern class for creating {@link Image}s. After creating the
 * factory, optional properties may be set to customize the images this factory
 * creates.
 *
 * This class may be extended to add further customizations, but subclasses must
 * also provide custom serialization support to store the additional properties.
 *
 * @author peachjm1
 *
 */
public class ImageFactory
{
    public static ImageFactory of(String imagerLabel, String imageDirectoryName, ImageType imageType)
    {
        return new ImageFactory(imagerLabel, imageDirectoryName, imageType);
    }

    /**
     * General exception used to wrap any other exceptions encountered while trying
     * to create the image object
     *
     * @author peachjm1
     *
     */
    public static final class ImageCreationException extends Exception
    {
        public ImageCreationException(String description, Exception e)
        {
            super(description, e);
        }
    }

    private final String imagerLabel;
    private final String imageDirectoryName;
    private final ImageType imageType;
    private ImageFlip flip;
    private ImageRotation rotation;
    private Integer camera;
    protected String cameraName;
    protected Integer filter;
    protected String filterName;

    protected ImageFactory(String imagerLabel, String imageDirectoryName, ImageType imageType)
    {
        this.imagerLabel = Preconditions.checkNotNull(imagerLabel);
        this.imageDirectoryName = Preconditions.checkNotNull(imageDirectoryName);
        this.imageType = Preconditions.checkNotNull(imageType);
    }

    public String getImagerLabel()
    {
        return imagerLabel;
    }

    public String getImageDirectoryName()
    {
        return imageDirectoryName;
    }

    public ImageType getImageType()
    {
        return imageType;
    }

    /**
     * Return the flip if it is defined (non-null), or Flip.NONE if not.
     *
     * @return the flip object
     */
    public ImageFlip getFlip()
    {
        return flip != null ? flip : ImageFlip.NONE;
    }

    public ImageFactory setFlip(ImageFlip flip)
    {
        this.flip = flip;
        return this;
    }

    /**
     * Return the rotation if it is defined (non-null), or Rotation.Zero if not.
     *
     * @return the rotation object
     */
    public ImageRotation getRotation()
    {
        return rotation != null ? rotation : ImageRotation.Zero;
    }

    public ImageFactory setRotation(ImageRotation rotation)
    {
        this.rotation = rotation;
        return this;
    }

    /**
     * Return the camera identity as an int if it is defined (non-null), or -1 if
     * not.
     *
     * @return the camera
     */
    public int getCamera()
    {
        return camera != null ? camera : -1;
    }

    public ImageFactory setCamera(Integer camera)
    {
        this.camera = camera;
        return this;
    }

    /**
     * Return the camera name if it is defined (non-null), or "" if not.
     *
     * @return the camera name
     */
    public String getCameraName()
    {
        return cameraName != null ? cameraName : "";
    }

    public ImageFactory setCameraName(String cameraName)
    {
        this.cameraName = cameraName;
        return this;
    }

    /**
     * Return the filter identity as an int if it is defined (non-null), or -1 if
     * not.
     *
     * @return the filter
     */
    public int getFilter()
    {
        return filter != null ? filter : -1;
    }

    public ImageFactory setFilter(Integer filter)
    {
        this.filter = filter;
        return this;
    }

    /**
     * Return the filter name if it is defined (non-null), or "" if not.
     *
     * @return the filter name
     */
    public String getFilterName()
    {
        return filterName != null ? filterName : "";
    }

    public ImageFactory setFilterName(String filterName)
    {
        this.filterName = filterName;
        return this;
    }

    /**
     * Create image based on the arguments and all the properties that have been set
     * in this factory.
     *
     * @param key key identifying the image, its pointing and other properties
     * @param smallBodyModel the small body model on which the image will be
     *            projected
     * @param loadPointingOnly if true, the image is not loaded, only the pointing
     *            file
     * @return the created image
     * @throws ImageCreationException if any exception is thrown while attempting to
     *             create/load the image
     * @throws IllegalArgumentException if the key's {@link ImageType} does not
     *             match this factory's {@link ImageType}
     * @throws NullPointerException if any arguments are null
     */
    public Image createImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel, boolean loadPointingOnly) throws ImageCreationException
    {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(smallBodyModel);

        Preconditions.checkArgument(imageType == key.getImageType());

        try
        {
            Image result;
            switch (key.getSource())
            {
            case GASKELL:
            case GASKELL_UPDATED:
            case CORRECTED:
            case SPICE:
            case CORRECTED_SPICE:
            case LABEL:
            case LOCAL_PERSPECTIVE:
                result = new BasicPerspectiveImage(key, smallBodyModel, loadPointingOnly) {

                    @Override
                    public int getCamera()
                    {
                        return ImageFactory.this.getCamera();
                    }

                    @Override
                    public String getCameraName()
                    {
                        return ImageFactory.this.getCameraName();
                    }

                    @Override
                    public int getFilter()
                    {
                        return ImageFactory.this.getFilter();
                    }

                    @Override
                    public String getFilterName()
                    {
                        return ImageFactory.this.getFilterName();
                    }

                    @Override
                    public String toString()
                    {
                        return imageType.name() + " " + new File(key.getName()).getName();
                    }
                };
                break;
            case LOCAL_CYLINDRICAL:
            case IMAGE_MAP:
                Preconditions.checkArgument(key instanceof CustomCylindricalImageKey, "Cannot create a cylindrical image from a key of type " + key.getClass().getSimpleName());
                result = new CylindricalImage((CustomCylindricalImageKey) key, smallBodyModel);
                break;
            case FALSE_COLOR:
                Preconditions.checkArgument(key instanceof ColorImageKey, "Cannot create a false color image from a key of type " + key.getClass().getSimpleName());
                result = new ColorImage((ColorImageKey<?>) key, smallBodyModel, null);
                break;
            default:
                throw new AssertionError();
            }

            return result;
        }
        catch (Exception e)
        {
            throw new ImageCreationException("Unable to create image for key " + key, e);
        }

    }

    /**
     * Set up storage/retrieval of fields to/from metadata. This factory-pattern
     * class has fields which are set to null and effectively ignored unless the
     * caller sets them to a non-null value. For brevity in the metadata, these
     * "optional" fields are not written or read if they are null.
     */
    public static void initializeSerializationProxy()
    {
        Version version = Version.of(1, 0);

        Key<ImageFactory> proxyKey = Key.of("ImageFactory");

        // Required keys.
        Key<String> imagerLabelKey = Key.of("imagerLabel");
        Key<String> imageDirectoryNameKey = Key.of("imageDirectoryName");
        Key<String> imageTypeKey = Key.of("imageType");

        // Optional keys.
        Key<String> flipKey = Key.of("flip");
        Key<Double> rotationKey = Key.of("rotation");
        Key<Integer> cameraKey = Key.of("camera");
        Key<String> cameraNameKey = Key.of("cameraName");
        Key<Integer> filterKey = Key.of("filter");
        Key<String> filterNameKey = Key.of("filterName");

        InstanceGetter.defaultInstanceGetter().register(proxyKey, metadata -> {
            String imagerLabel = metadata.get(imagerLabelKey);
            String imageDirectoryName = metadata.get(imageDirectoryNameKey);
            ImageType imageType = ImageType.valueOf(metadata.get(imageTypeKey));

            ImageFactory factory = new ImageFactory(imagerLabel, imageDirectoryName, imageType);

            // Flip and Rotation must be created from saved primitive values.
            factory.setFlip(ImageFlip.of(getOptional(flipKey, metadata)));
            factory.setRotation(ImageRotation.of(getOptional(rotationKey, metadata)));

            factory.setCamera(getOptional(cameraKey, metadata));
            factory.setCameraName(getOptional(cameraNameKey, metadata));
            factory.setFilter(getOptional(filterKey, metadata));
            factory.setFilterName(getOptional(filterNameKey, metadata));

            return factory;
        }, ImageFactory.class, factory -> {
            SettableMetadata metadata = SettableMetadata.of(version);

            metadata.put(imagerLabelKey, factory.imagerLabel);
            metadata.put(imageDirectoryNameKey, factory.imageDirectoryName);
            metadata.put(imageTypeKey, factory.imageType.name());

            // Don't use getters for metadata; the getters don't return null if the field is
            // null.
            // Flip and Rotation are stored as their corresponding primitive values.
            putOptional(flipKey, metadata, factory.flip != null ? factory.flip.flip() : null);
            putOptional(rotationKey, metadata, factory.rotation != null ? factory.rotation.rotation() : null);

            putOptional(cameraKey, metadata, factory.camera);
            putOptional(cameraNameKey, metadata, factory.cameraName);
            putOptional(filterKey, metadata, factory.filter);
            putOptional(filterNameKey, metadata, factory.filterName);

            return metadata;
        });
    }

    /**
     * If a key is present in metadata, return the associated value, otherwise
     * return null. This is to read optional fields in metadata.
     *
     * @param key the key
     * @param metadata the metadata
     * @return the value, or null if missing
     */
    private static <T> T getOptional(Key<T> key, Metadata metadata)
    {
        return metadata.hasKey(key) ? metadata.get(key) : null;
    }

    /**
     * Write a key-value pair to metadata, but only if the value is non-null. This
     * makes it possible to have optional fields and to suppress the writing of null
     * values.
     *
     * @param key the key
     * @param metadata the metadata
     * @param item item to write, or null. If null, key-value pair is not written.
     */
    private static <T> void putOptional(Key<T> key, SettableMetadata metadata, T item)
    {
        if (item != null)
        {
            metadata.put(key, item);
        }
    }
}
