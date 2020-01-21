package edu.jhuapl.sbmt.model.image;

import java.util.function.Function;

/**
 * Represents a rotation of an image about its center by the provided angle,
 * including algebra for combining multiple rotations. This abstraction is as
 * null/fault-tolerant as possible for convenience, but note that this comes at
 * the expense of error checking.
 *
 * @author peachjm1
 *
 */
public final class ImageRotation implements Function<ImageRotation, ImageRotation>
{
    /**
     * Singleton rotation of 0 degrees (no rotation).
     */
    public static final ImageRotation Zero = ImageRotation.of(0.);

    /**
     * Return a rotation object representing the supplied rotation. This method
     * always creates a new object.
     *
     * @param rotation in degrees
     * @return
     */
    public static ImageRotation of(double rotation)
    {
        return new ImageRotation(rotation);
    }

    private final double rotation;

    private ImageRotation(double rotation)
    {
        this.rotation = rotation;
    }

    public double rotation()
    {
        return rotation;
    }

    /**
     * Return a single rotation object that represents the result of applying all
     * the supplied rotations.
     *
     * @param rotations
     * @return
     */
    public static ImageRotation apply(ImageRotation... rotations)
    {
        ImageRotation result = Zero;

        if (rotations != null)
        {
            double doubleResult = 0.;
            for (ImageRotation rotation : rotations)
            {
                doubleResult += nonNull(rotation).rotation;
            }
            result = new ImageRotation(doubleResult);
        }

        return result;
    }

    /**
     * Return a single rotation object that represents the result of applying the supplied rotation to this rotation.
     */
    @Override
    public ImageRotation apply(ImageRotation rotation)
    {
        return new ImageRotation(this.rotation + nonNull(rotation).rotation);
    }

    private static ImageRotation nonNull(ImageRotation rotation)
    {
        return rotation != null ? rotation : Zero;
    }

    public String toString()
    {
        return Double.toString(rotation);
    }

}
