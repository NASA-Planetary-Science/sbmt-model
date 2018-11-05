package edu.jhuapl.sbmt.model.pointing;

import java.io.IOException;

import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class SumFileFromConfigIO implements SumFileIO
{
    PerspectiveImage image;

    public SumFileFromConfigIO(PerspectiveImage image)
    {
        this.image = image;
    }


    @Override
    public void loadSumfile() throws NumberFormatException, IOException
    {
        System.out.println("SumFileFromConfigIO: loadSumfile: loading from config");
        initLocalSumfileFullPath();
    }

    public String initLocalSumfileFullPath()
    {
//        // TODO this is bad in that we read from the config file 3 times in this class
//
//        // Look in the config file and figure out which index this image
//        // corresponds to. The config file is located in the same folder
//        // as the image file
//        String configFilename = new File(image.getKey().name).getParent() + File.separator + "config.txt";
//        MapUtil configMap = new MapUtil(configFilename);
//        String[] imageFilenames = configMap.getAsArray(Image.IMAGE_FILENAMES);
//        for (int i=0; i<imageFilenames.length; ++i)
//        {
//            String filename = new File(image.getKey().name).getName();
//            if (filename.equals(imageFilenames[i]))
//            {
//                return new File(image.getKey().name).getParent() + File.separator + configMap.getAsArray(PerspectiveImage.SUMFILENAMES)[i];
//            }
//        }

        return null;
    }

}
