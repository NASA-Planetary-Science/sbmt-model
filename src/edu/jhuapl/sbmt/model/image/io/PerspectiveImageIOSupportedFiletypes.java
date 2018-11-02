package edu.jhuapl.sbmt.model.image.io;

public enum PerspectiveImageIOSupportedFiletypes
{
    PNG(new String[] {"png"}),
    ENVI(new String[] {"envi"}),
    FITS(new String[] {"fit", "fits"});

    private String[] extensions;

    private PerspectiveImageIOSupportedFiletypes(String[] extensions)
    {
        this.extensions = extensions;
    }

    public static PerspectiveImageIOSupportedFiletypes getTypeForExtension(String extension)
    {
        for (PerspectiveImageIOSupportedFiletypes type : values())
        {
            String[] extensions = type.extensions;
            for (String ext : extensions)
            {
                if (ext.equalsIgnoreCase(extension)) return type;
            }
        }
        return null;
    }
}
