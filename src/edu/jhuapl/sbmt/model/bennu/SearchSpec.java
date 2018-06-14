package edu.jhuapl.sbmt.model.bennu;

import edu.jhuapl.sbmt.model.image.ImageSource;


public interface SearchSpec
{

    String getDataName();

    String getDataRootLocation();

    String getDataPath();

    String getDataListFilename();

    ImageSource getSource();

    String getxAxisUnits();

    String getyAxisUnits();

    String getDataDescription();

}
