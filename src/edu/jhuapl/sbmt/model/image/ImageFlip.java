package edu.jhuapl.sbmt.model.image;

import java.util.function.Function;

/**
 * Encapsulation of image flip conditions, including algebra for applying
 * multiple flips. This abstraction is as null/fault-tolerant as possible for
 * convenience, but note that this comes at the expense of error checking.
 *
 * @author peachjm1
 *
 */
public enum ImageFlip implements Function<ImageFlip, ImageFlip>
{
    /**
     * Do not flip the image.
     */
    NONE("None")
    {
        public ImageFlip apply(ImageFlip flip)
        {
            return nonNull(flip);
        }
    },
    /**
     * Flip the image about the X axis only.
     */
    X("X")
    {
        public ImageFlip apply(ImageFlip flip)
        {
            ImageFlip result;
            switch (nonNull(flip))
            {
            case NONE:
                result = this;
                break;
            case X:
                result = NONE;
                break;
            case Y:
                result = XY;
                break;
            case XY:
                result = Y;
                break;
            default:
                throw new AssertionError();
            }
            return result;
        }
    },
    /**
     * Flip the image about the Y axis only.
     */
    Y("Y")
    {
        public ImageFlip apply(ImageFlip flip)
        {
            ImageFlip result;
            switch (nonNull(flip))
            {
            case NONE:
                result = this;
                break;
            case X:
                result = XY;
                break;
            case Y:
                result = NONE;
                break;
            case XY:
                result = X;
                break;
            default:
                throw new AssertionError();
            }
            return result;
        }
    },
    /**
     * Flip the image about both X and Y axes.
     */
    XY("XY")
    {
        public ImageFlip apply(ImageFlip flip)
        {
            ImageFlip result;
            switch (nonNull(flip))
            {
            case NONE:
                result = this;
                break;
            case X:
                result = Y;
                break;
            case Y:
                result = X;
                break;
            case XY:
                result = NONE;
                break;
            default:
                throw new AssertionError();
            }
            return result;
        }
    },
    ;

    /**
     * Return a {@link ImageFlip} based on the supplied string as follows. If the
     * string contains both characters X and Y, {@link #XY} is returned. If the
     * string contains only X, {@link X} is returned. If the string contains only Y,
     * {@link #Y} is returned. Otherwise {@link #NONE} is returned.
     * <p>
     * All string operations are case-insensitive.
     * <p>
     * Prefer this method to the standard enum {@link #valueOf(String)} method.
     *
     * @param flipString the string describing the flip. May be null, which is
     *            interpreted as {@link #NONE}.
     * @return the flip object
     */
    public static ImageFlip of(String flipString)
    {
        ImageFlip result = NONE;

        if (flipString != null)
        {
            boolean xFlip = flipString.contains("x") || flipString.contains("X");
            boolean yFlip = flipString.contains("y") || flipString.contains("Y");
            result = xFlip && yFlip ? XY : xFlip ? X : yFlip ? Y : NONE;
        }

        return result;
    }

    private final String flipString;

    private ImageFlip(String flipString)
    {
        this.flipString = flipString;
    }

    public String flip()
    {
        return flipString;
    }

    /**
     * Return the flip resulting from applying all the supplied flips in sequence
     * using the flip instance {@link #apply(ImageFlip)} method in each case.
     * <p>
     * Any flips in the list/array of arguments that are null will be interpreted as
     * {@link #NONE}.
     *
     * @param flips the flips to apply. May be null, which is interpreted as
     *            {@link #NONE}
     * @return the resultant flip
     */
    public static ImageFlip apply(ImageFlip... flips)
    {
        ImageFlip result = NONE;

        if (flips != null)
        {
            for (ImageFlip flip : flips)
            {
                result = result.apply(nonNull(flip));
            }
        }

        return result;
    }

    /**
     * Return a flip which is the result of applying the supplied {@link ImageFlip}
     * to this flip according to the following standard rules. NONE does not change the
     * other flip, and two X flips or two Y flips cancel each other. X and Y flips
     * are performed independently, and all operations are commutative. For example:
     *
     * <pre>
     * X.apply(X) and Y.apply(Y) each return NONE
     * X.apply(Y) and Y.apply(X) each return XY
     * XY.apply(X) and X.apply(XY) each return Y
     * NONE.apply(imageFlip) and imageFlip.apply(NONE) each return imageFlip for any imageFlip
     * </pre>
     *
     * A null argument is interpreted as {@link #NONE}
     *
     * @return the resultant flip
     */
    @Override
    public abstract ImageFlip apply(ImageFlip flip);

    private static ImageFlip nonNull(ImageFlip flip)
    {
        return flip != null ? flip : NONE;
    }

    @Override
    public String toString()
    {
        return flipString;
    }

}