package edu.jhuapl.sbmt.model.bennu;

import java.io.BufferedWriter;
import java.io.IOException;

import edu.jhuapl.sbmt.model.image.ImageSource;

import crucible.crust.metadata.api.MetadataManager;


public interface SearchSpec extends MetadataManager
{

    String getDataName();

    String getDataRootLocation();

    String getDataPath();

    String getDataListFilename();

    ImageSource getSource();

    String getxAxisUnits();

    String getyAxisUnits();

    String getDataDescription();

    void toFile(BufferedWriter writer) throws IOException;

    public void fromFile(String csvLine);
}
