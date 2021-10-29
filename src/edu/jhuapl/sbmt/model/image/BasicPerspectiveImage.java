package edu.jhuapl.sbmt.model.image;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.saavtk.util.file.DataFileReader.FileFormatException;
import edu.jhuapl.saavtk.util.file.FitsFileReader;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.perspectiveImage.PerspectiveImage;

import nom.tam.fits.FitsException;

public class BasicPerspectiveImage extends PerspectiveImage
{

    private static final Map<String, ImmutableMap<String, String>> SUM_FILE_MAP = new HashMap<>();

    public BasicPerspectiveImage(ImageKeyInterface key, List<SmallBodyModel> smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
    }

    protected String getImageBaseName(ImageKeyInterface key)
    {
        // By default, assume the base name is the key name field
        // with any extension stripped off.
        return key.getName().replaceFirst("\\.[^\\.]*$", "");
    }

    protected String getImageFileName(ImageKeyInterface key)
    {
        return getImageFileName(getKey().getImageFilename());
    }

    protected String getImageFileName(String imageName)
    {
        // If the proposed name does not include the extension, add .fits.
        if (!imageName.matches("^.*\\.[^\\\\.]*$"))
        {
            imageName += ".fits";
        }

        return imageName;
    }

    @Override
    public int[] getMaskSizes()
    {
        return new int[] { 0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath() throws IOException
    {
        File file = FileCache.getFileFromServer(getImageFileName(getKey()));
        try
        {
            FitsFileReader.of().checkFormat(file);
        }
        catch (FileFormatException e)
        {
            throw new IOException(e);
        }
        return file.getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKeyInterface key = getKey();
        String result = null;

        ImageSource source = key.getSource();
        if (source == ImageSource.SPICE || source == ImageSource.CORRECTED_SPICE)
        {
            String infoFilesDirName = source == ImageSource.SPICE ? "infofiles" : "infofiles-corrected";

            File keyFile = new File(key.getName());
            String imagerPath = getImagerPath(keyFile);
            String pointingFileName = keyFile.getName() + ".INFO";
            String pointingFilePath = SafeURLPaths.instance().getString(imagerPath, infoFilesDirName, pointingFileName);
            result = FileCache.getFileFromServer(pointingFilePath).getAbsolutePath();
        }

        return result;
    }

    @Override
    protected String initializeSumfileFullPath() throws IOException
    {
        ImageKeyInterface key = getKey();
        String result = null;

        if (key.getSource() == ImageSource.GASKELL)
        {
            File keyFile = new File(getImageFileName(key));
            String imagerDirectory = getImagerPath(keyFile);
            try
            {
                String pointingFileName = getSumFileName(imagerDirectory, key);
                String pointingFilePath = SafeURLPaths.instance().getString(imagerDirectory, "sumfiles", pointingFileName);
                result = FileCache.getFileFromServer(pointingFilePath).getAbsolutePath();
            }
            catch (ParseException e)
            {
                throw new IOException(e);
            }
        }

        return result;
    }

    protected String getImagerPath(File imageFile)
    {
        return getKey().getInstrument().getSearchQuery().getRootPath();
    }

    protected String getSumFileName(String imagerDirectory, ImageKeyInterface key) throws IOException, ParseException
    {
        if (!SUM_FILE_MAP.containsKey(imagerDirectory))
        {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            File mapFile = FileCache.getFileFromServer(SafeURLPaths.instance().getString(imagerDirectory, "make_sumfiles.in"));
            try (BufferedReader reader = new BufferedReader(new FileReader(mapFile)))
            {
                while (reader.ready())
                {
                    String wholeLine = reader.readLine();
                    String[] line = wholeLine.split("\\s*,\\s*");
                    if (line[0].equals(wholeLine))
                    {
                        line = wholeLine.split("\\s\\s*");
                    }
                    if (line.length < 2) throw new ParseException("Cannot parse line " + String.join(" ", line) + " to get sum file/image file names", line.length > 0 ? line[0].length() : 0);
                    String sumFile = line[0] + ".SUM";
                    String imageFile = getImageFileName(line[line.length - 1]);

                    builder.put(imageFile, sumFile);
                }
            }
            SUM_FILE_MAP.put(imagerDirectory, builder.build());
        }

        File imageFile = new File(getImageFileName(key));
        ImmutableMap<String, String> imagerSumFileMap = SUM_FILE_MAP.get(imagerDirectory);
        if (imagerSumFileMap.containsKey(imageFile.getName()))
        {
            return SUM_FILE_MAP.get(imagerDirectory).get(imageFile.getName());
        }
        return null;
    }

}
