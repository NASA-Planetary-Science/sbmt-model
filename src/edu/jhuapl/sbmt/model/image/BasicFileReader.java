package edu.jhuapl.sbmt.model.image;

public abstract class BasicFileReader implements FileReader
{
    String filename=null;

    @Override
    public String getFileName()
    {
        return filename;
    }

    @Override
    public void setFileName(String filename)
    {
        this.filename = filename;
    }

}
