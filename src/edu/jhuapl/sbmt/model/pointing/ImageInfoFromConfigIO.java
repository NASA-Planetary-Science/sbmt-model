package edu.jhuapl.sbmt.model.pointing;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.saavtk.util.MapUtil;
import edu.jhuapl.sbmt.model.image.Image;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class ImageInfoFromConfigIO implements InfoFileIO
{
    PerspectiveImage image;

    public ImageInfoFromConfigIO(PerspectiveImage image)
    {
        this.image = image;
    }

    @Override
    public void deleteAdjustedImageInfo()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveImageInfo()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadImageInfo() throws IOException
    {
        loadImageInfoFromConfigFile();
    }

    @Override
    public void saveImageInfo(String filename)
    {
        // TODO Auto-generated method stub

    }

    private void loadImageInfoFromConfigFile()
    {
        // Look in the config file and figure out which index this image
        // corresponds to. The config file is located in the same folder
        // as the image file
        String configFilename = new File(image.getKey().name).getParent() + File.separator + "config.txt";
        MapUtil configMap = new MapUtil(configFilename);
        String[] imageFilenames = configMap.getAsArray(Image.IMAGE_FILENAMES);
        for (int i=0; i<imageFilenames.length; ++i)
        {
            String filename = new File(image.getKey().name).getName();
            if (filename.equals(imageFilenames[i]))
            {
                image.setImageName(configMap.getAsArray(Image.IMAGE_NAMES)[i]);
                break;
            }
        }
    }

    protected String initLocalInfoFileFullPath()
    {
        String configFilename = new File(image.getKey().name).getParent() + File.separator + "config.txt";
        MapUtil configMap = new MapUtil(configFilename);
        String[] imageFilenames = configMap.getAsArray(Image.IMAGE_FILENAMES);
        for (int i=0; i<imageFilenames.length; ++i)
        {
            String filename = new File(image.getKey().name).getName();
            if (filename.equals(imageFilenames[i]))
            {
                return new File(image.getKey().name).getParent() + File.separator + configMap.getAsArray(PerspectiveImage.INFOFILENAMES)[i];
            }
        }

        return null;
    }

}
