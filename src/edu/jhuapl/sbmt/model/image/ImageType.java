package edu.jhuapl.sbmt.model.image;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.google.common.base.Preconditions;

/**
 * An enumeration-like class for managing a singleton collection of defined
 * image types. Key differences from a standard enum are:
 * <p>
 * 1. Rather than throwing an exception like an enum would, the
 * {@link #valueOf(String)} method creates a new {@link ImageType} instance if
 * the supplied string does not correspond to any existing {@link ImageType}
 * instance.
 * <p>
 * 2. Similarly, the {@link #values()} method does not return a fixed array,
 * rather arrays that may grow over time as new {@link ImageType} instances are
 * created.
 * <p>
 * 3. This class does not implement Serializable. This could be added if
 * necessary, but for now one can serialize just the String returned by the
 * {@link #name()} method and deserialize using {@link #valueOf(String)}.
 * <p>
 * Except as noted above, all methods behave the same as their counterparts in
 * the {@link Enum} class, including {@link #hashCode()} and
 * {@link ImageType#equals(Object)}, i.e., equality implies identical instances.
 * <p>
 * This class used to be an actual enum.
 *
 * @author peachjm1
 *
 */
public final class ImageType implements Comparable<ImageType>
{
    private static final LinkedHashMap<String, ImageType> imageTypes = new LinkedHashMap<>();
    private static volatile ImageType[] values = new ImageType[0];

    /**
     * These constants are provided for backward compatibility. Do NOT add new
     * constants for new image types; instead use the result returned by the
     * {@link #valueOf(String)} method. These constants may be retired as calling
     * code is modified to replace all references to them with calls to valueOf.
     */
    public static final ImageType MSI_IMAGE = ImageType.valueOf("MSI_IMAGE");
    public static final ImageType AMICA_IMAGE = ImageType.valueOf("AMICA_IMAGE");
    public static final ImageType FC_IMAGE = ImageType.valueOf("FC_IMAGE");
    public static final ImageType FCCERES_IMAGE = ImageType.valueOf("FCCERES_IMAGE");
    public static final ImageType MARS_MOON_IMAGE = ImageType.valueOf("MARS_MOON_IMAGE");
    public static final ImageType OSIRIS_IMAGE = ImageType.valueOf("OSIRIS_IMAGE");
    public static final ImageType SATURN_MOON_IMAGE = ImageType.valueOf("SATURN_MOON_IMAGE");
    public static final ImageType SSI_GASPRA_IMAGE = ImageType.valueOf("SSI_GASPRA_IMAGE");
    public static final ImageType SSI_IDA_IMAGE = ImageType.valueOf("SSI_IDA_IMAGE");
    public static final ImageType MSI_MATHILDE_IMAGE = ImageType.valueOf("MSI_MATHILDE_IMAGE");
    public static final ImageType MVIC_JUPITER_IMAGE = ImageType.valueOf("MVIC_JUPITER_IMAGE");
    public static final ImageType LEISA_JUPITER_IMAGE = ImageType.valueOf("LEISA_JUPITER_IMAGE");
    public static final ImageType LORRI_IMAGE = ImageType.valueOf("LORRI_IMAGE");
    public static final ImageType POLYCAM_V3_IMAGE = ImageType.valueOf("POLYCAM_V3_IMAGE");
    public static final ImageType MAPCAM_V3_IMAGE = ImageType.valueOf("MAPCAM_V3_IMAGE");
    public static final ImageType POLYCAM_V4_IMAGE = ImageType.valueOf("POLYCAM_V4_IMAGE");
    public static final ImageType MAPCAM_V4_IMAGE = ImageType.valueOf("MAPCAM_V4_IMAGE");
    public static final ImageType MAPCAM_EARTH_IMAGE = ImageType.valueOf("MAPCAM_EARTH_IMAGE");
    public static final ImageType POLYCAM_EARTH_IMAGE = ImageType.valueOf("POLYCAM_EARTH_IMAGE");
    public static final ImageType SAMCAM_EARTH_IMAGE = ImageType.valueOf("SAMCAM_EARTH_IMAGE");
    public static final ImageType MAPCAM_FLIGHT_IMAGE = ImageType.valueOf("MAPCAM_FLIGHT_IMAGE");
    public static final ImageType POLYCAM_FLIGHT_IMAGE = ImageType.valueOf("POLYCAM_FLIGHT_IMAGE");
    public static final ImageType NAVCAM_FLIGHT_IMAGE = ImageType.valueOf("NAVCAM_FLIGHT_IMAGE");
    public static final ImageType SAMCAM_FLIGHT_IMAGE = ImageType.valueOf("SAMCAM_FLIGHT_IMAGE");
    public static final ImageType GENERIC_IMAGE = ImageType.valueOf("GENERIC_IMAGE");
    public static final ImageType ONC_IMAGE = ImageType.valueOf("ONC_IMAGE");
    public static final ImageType ONC_TRUTH_IMAGE = ImageType.valueOf("ONC_TRUTH_IMAGE");
    public static final ImageType TIR_IMAGE = ImageType.valueOf("TIR_IMAGE");

    private final String name;
    /**
     * If/when adding serialization, this has to be removed or made transient
     * because the order in which instances are created is not guaranteed. Also need
     * to be careful to ensure the states/invariants of the static fields
     * {@link #imageTypes} and {@link #values} are correctly maintained when
     * deserializing.
     */
    private final int ordinal;

    private ImageType(String name)
    {
        this.name = name;
        synchronized (ImageType.imageTypes)
        {
            this.ordinal = imageTypes.size();
        }
    }

    /**
     * Return an {@link ImageType} instance with the provided name. If an instance
     * with that name already exists, that instance is returned. Otherwise a new
     * instance is created, i.e., the first time this method is called with a given
     * name string.
     *
     * @param name the name of the image type
     * @return the {@link ImageType} instance
     * @throws NullPointerException if the supplied name string is null
     */
    public static ImageType valueOf(String name)
    {
        Preconditions.checkNotNull(name);

        synchronized (ImageType.imageTypes)
        {
            ImageType result = imageTypes.get(name);
            if (result == null)
            {
                result = new ImageType(name);
                ImageType.imageTypes.put(name, result);

                LinkedHashSet<ImageType> values = new LinkedHashSet<>(imageTypes.values());
                // For redmine-2018: this is a hack to prevent these image types from
                // appearing in lists of all image types while still allowing the types
                // to work if they are encountered explicitly.
                if (name.equals("PHOBOS_IMAGE") || name.equals("DEIMOS_IMAGE"))
                {
                    values.remove(result);
                }

                ImageType.values = values.toArray(ImageType.values);
            }
            return result;
        }
    }

    /**
     * Return an array holding all current {@link ImageType} instances in the order
     * in which they were created. Note this method is not guaranteed to return the
     * same array, or even an array of the same size if it is called again.
     * <p>
     * Note that PHOBOS_IMAGE and DEIMOS_IMAGE still function as valid image types
     * but they will not appear in the array returned by this method.
     *
     * @return array of all current {@link ImageType} instances
     */
    public static ImageType[] values()
    {
        synchronized (ImageType.imageTypes)
        {
            return values;
        }
    }

    /**
     * Return the name of this {@link ImageType} instance.
     *
     * @return the name
     */
    public String name()
    {
        return name;
    }

    /**
     * Return the ordinal, which is the order in which instances were created. For
     * the "built-in" public static constants in this class, this is the order in
     * which they are declared. For any constants added later using the
     * {@link #valueOf(String)} method, the order may change between runs of the
     * code. Within a code run, this method always returns the same value.
     * <p>
     * This is provided for completeness and is probably not needed/useful.
     *
     * @return the ordinal of this constant.d
     */
    public int ordinal()
    {
        return ordinal;
    }

    /**
     * Compares this constant with another in the usual {@link Comparable} way.
     * Within a run of the program, this method will always return the same result,
     * but there is no guarantee the result will be the same between runs.
     */
    @Override
    public int compareTo(ImageType other)
    {
        return ordinal < other.ordinal ? -1 : ordinal > other.ordinal ? 1 : 0;
    }

    /**
     * This is included for completeness for simulating a "real" enum. It just
     * returns the same thing as {@link #getClass()}.
     *
     * @return the class of {@link ImageType}
     */
    public Class<ImageType> getDeclaringClass()
    {
        @SuppressWarnings("unchecked")
        Class<ImageType> result = (Class<ImageType>) getClass();

        return result;
    }

    /**
     * For backward compatibility to when this was a conventional enum with no
     * {@link #toString()} override, this just returns the name of the image type
     * instance.
     */
    @Override
    public String toString()
    {
        return name;
    }

}