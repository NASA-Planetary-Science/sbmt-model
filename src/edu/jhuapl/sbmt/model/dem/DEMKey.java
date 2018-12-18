package edu.jhuapl.sbmt.model.dem;


/**
 * An DEMKey should be used to uniquely distinguish one DEM from another.
 * It also contains metadata about the DEM that may be necessary to know
 * before the DEM is loaded.
 *
 * No two DEMs will have the same values for the fields of this class.
 */
public class DEMKey
{
    // The path of the DEM as passed into the constructor. This is not the
    // same as fullpath but instead corresponds to the name needed to download
    // the file from the server (excluding the hostname and extension).
    public String fileName;
    public String displayName;

    public DEMKey(String fileName, String displayName)
    {
        this.fileName = fileName;
        this.displayName = displayName;
    }

    // Copy constructor
    public DEMKey(DEMKey copyKey)
    {
        fileName = copyKey.fileName;
        this.displayName = copyKey.displayName;
    }

    @Override
    public boolean equals(Object obj)
    {
        return fileName.equals(((DEMKey)obj).fileName);
    }

    @Override
    public int hashCode()
    {
        return fileName.hashCode();
    }
}
