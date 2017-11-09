package edu.jhuapl.sbmt.model.image;

public abstract class BasicFileReader implements FileReader
{
    protected final String filename;

    public BasicFileReader(String filename)
    {
        this.filename=filename;
    }

    @Override
    public String getFileName()
    {
        return filename;
    }
}
