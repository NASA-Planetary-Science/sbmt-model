package edu.jhuapl.sbmt.model.image;

import java.io.IOException;

public interface FileReader<E extends IOException>
{
    public void read() throws E;
    public String getFileName();
}
