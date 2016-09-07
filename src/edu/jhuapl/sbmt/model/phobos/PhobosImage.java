package edu.jhuapl.sbmt.model.phobos;

import java.io.File;
import java.io.IOException;

import nom.tam.fits.FitsException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.ImageSource;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class PhobosImage extends PerspectiveImage
{
    public PhobosImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);

        // Label files are still used for determining Viking image
        // camera and filter names to display in image properties window
        setLabelFileFullPath(initializeLabelFileFullPath());
    }

    /**
     * Return whether or not this is a HiRISE image
     * @param key
     * @return
     */
    private boolean isHiRISE(String filename)
    {
        return filename.startsWith("ESP") || filename.startsWith("PSP");
    }

    /**
     * Return whether or not this is an HRSC image
     * @param key
     * @return
     */
    private boolean isHrsc(String filename)
    {
        return filename.startsWith("h");
    }

    /**
     * Return whether or not this is a Viking image
     * @param key
     * @return
     */
    private boolean isViking(String filename)
    {
        return filename.startsWith("V") || filename.startsWith("f");
    }

    /**
     * Is this a Phobos2 image taken with filter 2
     * @param key
     * @return
     */
    private boolean isPhobos2Filter2(String filename)
    {
        return filename.startsWith("P") && filename.endsWith("2") && !filename.startsWith("PSP");
    }

    /**
     * Is this a Phobos2 image taken with filters 1 or 3
     * @param key
     * @return
     */
    private boolean isPhobos2Filter1Or3(String filename)
    {
        return filename.startsWith("P") && !filename.endsWith("2") && !filename.startsWith("PSP");
    }

    /**
     * Return whether or not this is a MOC image
     * @param key
     * @return
     */
    private boolean isMoc(String filename)
    {
        return filename.startsWith("sp2");
    }

    @Override
    protected int[] getMaskSizes()
    {
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKey key = getKey();
        return FileCache.getFileFromServer(key.name + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String filename = keyFile.getName();

        // Note Viking images begin either with an upper case V or a lower case f.

        if (!isViking(filename))
            return null;

        String labelFilename = null;
        if (keyFile.getName().startsWith("V"))
            labelFilename = keyFile.getParent() + "/f" + keyFile.getName().substring(2, 8).toLowerCase() + ".lbl";
        else //if (keyFile.getName().startsWith("f"))
            labelFilename = key.name + ".lbl";

        return FileCache.getFileFromServer(labelFilename).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();

        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/infofiles/"
        + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        String sumfilesdir = "sumfiles";
        if (key.source == ImageSource.CORRECTED)
            sumfilesdir += "-corrected";
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent() + "/" + sumfilesdir + "/"
        + keyFile.getName() + ".SUM";

        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        // For Phobos 2 image, return 1, 2, or 3 which we can get by looking at the last number in the filename.
        // For Viking images, we need to parse the label file to get the filter.
        // for MEX, HiRISE, or MOC images, return -1
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String filename = keyFile.getName();
        if (isHrsc(filename) || isHiRISE(filename) || isMoc(filename))
        {
            return -1;
        }
        else if (isPhobos2Filter2(filename) || isPhobos2Filter1Or3(filename))
        {
            return Integer.parseInt(keyFile.getName().substring(7, 8));
        }
        else // is Viking
        {
            String labelFileFullPath = getLabelFileFullPath();
            if(labelFileFullPath != null)
            {
                try
                {
                    String filterLine = FileUtil.getFirstLineStartingWith(getLabelFileFullPath(), "FILTER_NAME");
                    String[] words = filterLine.trim().split("\\s+");
                    return getVikingFilterNumberFromName(words[2]);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return -1;
        }
    }

    private int getVikingFilterNumberFromName(String name)
    {
        int num = -1;
        if (name.equals("BLUE"))
            num = 4;
        if (name.equals("MINUS_BLUE"))
            num = 5;
        else if (name.equals("VIOLET"))
            num = 6;
        else if (name.equals("CLEAR"))
            num = 7;
        else if (name.equals("GREEN"))
            num = 8;
        else if (name.equals("RED"))
            num = 9;

        return num;
    }

    private String getFilterNameFromNumber(int num)
    {
        String name = null;
        if (num == 1)
            name = "Channel 1";
        else if (num == 2)
            name = "Channel 2";
        else if (num == 3)
            name = "Channel 3";
        else if (num == 4)
            name = "BLUE";
        else if (num == 5)
            name = "MINUS_BLUE";
        else if (num == 6)
            name = "VIOLET";
        else if (num == 7)
            name = "CLEAR";
        else if (num == 8)
            name = "GREEN";
        else if (num == 9)
            name = "RED";

        return name;
    }

    private String getCameraNameFromNumber(int num)
    {
        String name = null;
        if (num == 1)
            name = "Phobos 2, VSK";
        else if (num == 2)
            name = "Viking Orbiter 1, Camera A";
        else if (num == 3)
            name = "Viking Orbiter 1, Camera B";
        else if (num == 4)
            name = "Viking Orbiter 2, Camera A";
        else if (num == 5)
            name = "Viking Orbiter 2, Camera B";
        else if (num == 6)
            name = "Mars Express, HRSC";
        else if (num == 7)
            name = "Mars Reconnaissance Orbiter, HiRISE";
        else if (num == 8)
            name = "Mars Global Surveyor, MOC";

        return name;
    }

    @Override
    public String getFilterName()
    {
        return getFilterNameFromNumber(getFilter());
    }

    @Override
    public String getCameraName()
    {
        return getCameraNameFromNumber(getCamera());
    }

    @Override
    public int getCamera()
    {
        // Return the following:
        // 1 for phobos 2 images
        // 2 for viking orbiter 1 images camera A
        // 3 for viking orbiter 1 images camera B
        // 4 for viking orbiter 2 images camera A
        // 5 for viking orbiter 2 images camera B
        // 6 for MEX HRSC camera
        // 7 for HiRISE
        // 8 for MOC
        // We need to parse the label file to get which viking spacecraft

        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String filename = keyFile.getName();
        if (isHrsc(filename))
        {
            return 6;
        }
        else if (isHiRISE(filename))
        {
            return 7;
        }
        else if (isMoc(filename))
        {
            return 8;
        }
        else if (isPhobos2Filter2(filename) || isPhobos2Filter1Or3(filename))
        {
            return 1;
        }
        else // is Viking
        {
            String labelFileFullPath = getLabelFileFullPath();
            if(labelFileFullPath != null)
            {
                try
                {
                    String filterLine = FileUtil.getFirstLineStartingWith(labelFileFullPath, "SPACECRAFT_NAME");
                    String[] words = filterLine.trim().split("\\s+");
                    if (words[2].equals("VIKING_ORBITER_1"))
                    {
                        if (filename.toLowerCase().contains("a"))
                            return 2;
                        else
                            return 3;
                    }
                    else
                    {
                        if (filename.toLowerCase().contains("a"))
                            return 4;
                        else
                            return 5;
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return -1;
        }
    }
}
