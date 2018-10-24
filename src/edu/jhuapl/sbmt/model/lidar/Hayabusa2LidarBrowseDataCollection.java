package edu.jhuapl.sbmt.model.lidar;

import java.io.IOException;

import edu.jhuapl.sbmt.client.BodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyModel;

public class Hayabusa2LidarBrowseDataCollection extends LidarBrowseDataCollection
{

    public Hayabusa2LidarBrowseDataCollection(SmallBodyModel smallBodyModel)
    {
        super(smallBodyModel);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected LidarDataPerUnit createLidarDataPerUnitWhateverThatIs(String path,
            BodyViewConfig config) throws IOException
    {
        return new Hayabusa2LidarDataPerUnit(path, config);
    }

}
