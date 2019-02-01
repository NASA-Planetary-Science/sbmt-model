package edu.jhuapl.sbmt.model.image;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.saavtk.util.file.DataFileReader.FileFormatException;
import edu.jhuapl.saavtk.util.file.FitsFileReader;
import edu.jhuapl.sbmt.client.SmallBodyModel;

import nom.tam.fits.FitsException;

public abstract class BasicPerspectiveImage extends PerspectiveImage
{

    private static final Map<String, ImmutableMap<String, String>> SUM_FILE_MAP = new HashMap<>();

    protected BasicPerspectiveImage(ImageKeyInterface key, SmallBodyModel smallBodyModel,
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
        // Image file name is based on the key name.
        String imageFileName = key.getName();

        // If the proposed name does not include the extension, add .fits.
        if (!key.getName().matches("^.*\\.[^\\\\.]*$"))
        {
            imageFileName += ".fits";
        }

        return imageFileName;
    }

    @Override
    protected int[] getMaskSizes()
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

        if (key.getSource() == ImageSource.SPICE)
        {
            File keyFile = new File(key.getName());
            File imagerDirectory = getImagerDirectory(keyFile);
            String pointingFileName = keyFile.getName() + ".INFO";
            String pointingFilePath = SafeURLPaths.instance().getString(imagerDirectory.getPath(), "infofiles", pointingFileName);
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
            String imagerDirectory = getImagerDirectory(keyFile).getPath();
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

    protected File getImagerDirectory(File imageFile)
    {
        File directory = imageFile.getParentFile();
        Preconditions.checkNotNull(directory);
        File imagerDirectory = directory.getParentFile();
        Preconditions.checkNotNull(imagerDirectory);
        return imagerDirectory;
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
                    String imageFile = line[line.length - 1];

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
