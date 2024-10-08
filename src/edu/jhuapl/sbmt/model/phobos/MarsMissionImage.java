package edu.jhuapl.sbmt.model.phobos;

import java.io.File;
import java.io.IOException;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.pointing.PointingSource;
import edu.jhuapl.sbmt.image.interfaces.ImageKeyInterface;
import edu.jhuapl.sbmt.image.model.PerspectiveImage;
import edu.jhuapl.sbmt.image.old.BasicPerspectiveImage;
import nom.tam.fits.FitsException;

public class MarsMissionImage extends BasicPerspectiveImage
{
    public static PerspectiveImage of(ImageKeyInterface key, SmallBodyModel smallBodyModel, boolean loadPointingOnly) throws FitsException, IOException
    {
        String filename = new File(key.getOriginalName()).getName();

        PerspectiveImage result;
        if (isPhobos2(filename))
        {
            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex == -1)
            {
                dotIndex = filename.length();
            }

            int filterFromFileName;
            try
            {
                filterFromFileName = Integer.parseInt(filename.substring(dotIndex - 1, dotIndex));
            }
            catch (Exception e)
            {
                System.err.println("Unable to determine Phobos2 filter from original file name (last character is not an integer). Setting filter to 1");
                filterFromFileName = 1;
            }

            int filter = filterFromFileName;
            String filterName = "Channel " + filterFromFileName;

            result = new MarsMissionImage(key, smallBodyModel, loadPointingOnly) {

                @Override
                public int getFilter()
                {
                    return filter;
                }

                @Override
                public String getFilterName()
                {
                    return filterName;
                }

                @Override
                public int getCamera()
                {
                    return 1;
                }

                @Override
                public String getCameraName()
                {
                    return "Phobos 2, VSK";
                }
            };
        }
        else if (isViking(filename))
        {
            String labelFilename = SafeURLPaths.instance().getString(new File(key.getName()).getParent(), filename.replaceFirst("^V", "f").replaceFirst("\\.[^\\.]*$", "") + ".lbl");

            result = new MarsMissionImage(key, smallBodyModel, loadPointingOnly) {
                String filterName = null;
                Integer filter = null;
                Integer camera = null;

                @Override
                public int getFilter()
                {
                    if (filter == null)
                    {
                        try
                        {
                            String filterLine = FileUtil.getFirstLineStartingWith(getLabelFileFullPath(), "FILTER_NAME");
                            String[] words = filterLine.trim().split("\\s+");
                            filterName = words[2];
                            filter = getVikingFilterNumberFromName(filterName);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            filterName = null;
                            filter = -1;
                        }
                    }

                    return filter;
                }

                @Override
                public String getFilterName()
                {
                    // Must call getFilter to ensure filterName field has been initialized.
                    getFilter();

                    return filterName;
                }

                @Override
                public int getCamera()
                {
                    if (camera == null)
                    {
                        String labelFileFullPath = getLabelFileFullPath();
                        try
                        {
                            String filterLine = FileUtil.getFirstLineStartingWith(labelFileFullPath, "SPACECRAFT_NAME");
                            String[] words = filterLine.trim().split("\\s+");
                            if (words[2].equals("VIKING_ORBITER_1"))
                            {
                                if (filename.toLowerCase().contains("a"))
                                    camera = 2;
                                else
                                    camera = 3;
                            }
                            else
                            {
                                if (filename.toLowerCase().contains("a"))
                                    camera = 4;
                                else
                                    camera = 5;
                            }
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            camera = -1;
                        }
                    }

                    return camera;
                }

                @Override
                public String getCameraName()
                {
                    int num = getCamera();

                    String name = null;
                    if (num == 2)
                    {
                        name = "Viking Orbiter 1, Camera A";
                    }
                    else if (num == 3)
                    {
                        name = "Viking Orbiter 1, Camera B";
                    }
                    else if (num == 4)
                    {
                        name = "Viking Orbiter 2, Camera A";
                    }
                    else if (num == 5)
                    {
                        name = "Viking Orbiter 2, Camera B";
                    }

                    return name;
                }

                @Override
                protected String initializeLabelFileFullPath()
                {
                    String labelFile;
                    try
                    {
                        labelFile = FileCache.getFileFromServer(labelFilename).getAbsolutePath();
                    }
                    catch (Exception e)
                    {
                        // Ignore -- label files are optional even for Viking.
                        labelFile = null;
                    }

                    return labelFile;
                }

            };
        }
        else if (isMOC(filename))
        {
            result = of(key, smallBodyModel, loadPointingOnly, 8, "Mars Global Surveyor, MOC");
        }
        else if (isHiRISE(filename))
        {
            result = of(key, smallBodyModel, loadPointingOnly, 7, "Mars Reconnaissance Orbiter, HiRISE");
        }
        else if (isHRSC(filename))
        {
            result = of(key, smallBodyModel, loadPointingOnly, 6, "Mars Express, HRSC");
        }
        else
        {
            throw new IllegalArgumentException("Image for key " + key.getName() + " cannot be identified as a Mars system image");
        }

        return result;
    }

    protected static MarsMissionImage of(ImageKeyInterface key, SmallBodyModel smallBodyModel, boolean loadPointingOnly, int camera, String cameraName) throws FitsException, IOException
    {
        return new MarsMissionImage(key, smallBodyModel, loadPointingOnly) {
            @Override
            public int getCamera()
            {
                return camera;
            }

            @Override
            public String getCameraName()
            {
                return cameraName;
            }
        };
    }

    protected MarsMissionImage(ImageKeyInterface key, SmallBodyModel smallBodyModel, boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    /**
     * Return whether or not this is a Phobos2 image
     *
     * @param key
     * @return
     */
    private static boolean isPhobos2(String filename)
    {
        return filename.startsWith("P") && !filename.startsWith("PSP");
    }

    /**
     * Return whether or not this is a Viking image
     *
     * @param key
     * @return
     */
    private static boolean isViking(String filename)
    {
        return filename.startsWith("V") || filename.startsWith("f");
    }

    /**
     * Return whether or not this is a MOC image
     *
     * @param key
     * @return
     */
    private static boolean isMOC(String filename)
    {
        return filename.startsWith("sp2");
    }

    /**
     * Return whether or not this is a HiRISE image
     *
     * @param key
     * @return
     */
    private static boolean isHiRISE(String filename)
    {
        return filename.startsWith("ESP") || filename.startsWith("PSP");
    }

    /**
     * Return whether or not this is an HRSC image
     *
     * @param key
     * @return
     */
    private static boolean isHRSC(String filename)
    {
        return filename.startsWith("h");
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKeyInterface key = getKey();
        return FileCache.getFileFromServer(key.getName() + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();

        File keyFile = new File(key.getName());
        String fileName = keyFile.getParentFile().getParent() + "/infofiles/"
                + keyFile.getName() + ".INFO";

        return FileCache.getFileFromServer(fileName).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKeyInterface key = getKey();
        String sumfilesdir = "sumfiles";
        if (key.getSource() == PointingSource.CORRECTED)
            sumfilesdir += "-corrected";
        File keyFile = new File(key.getName());
        String fileName = keyFile.getParentFile().getParent() + "/" + sumfilesdir + "/"
                + keyFile.getName() + ".SUM";

        return FileCache.getFileFromServer(fileName).getAbsolutePath();
    }

    private static int getVikingFilterNumberFromName(String name)
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

    @Override
    public String toString()
    {
        ImageKeyInterface key = getKey();

        return key.getImageType() + " image " + key.getName();
    }
}
