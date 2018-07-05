package edu.jhuapl.sbmt.model.ryugu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.SafePaths;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

import nom.tam.fits.FitsException;

public class ONCImage extends PerspectiveImage
{

    public ONCImage(ImageKey key, SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initialize() throws FitsException, IOException
    {
        // Note this is a really bad idea in general (overriding something that is called
        // from the base class constructor), but this kind of thing is done
        // a lot in SBMT, so for now, just conforming to the pattern.
        fitFileImageExtension = 1;
        super.initialize();
    }

    @Override
    protected int[] getMaskSizes()
    {
    	// Don't mask anything.
        return new int[]{0, 0, 0, 0};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        return FileCache.getFileFromServer(getImageFileName()).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent()
        + "/infofiles/" + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImmutableMap<String, String> sumfileMap = getSumfileMap();
        String imageKey = getImageFileName().replaceFirst(".*/", "");
        String sumfileName = sumfileMap.get(imageKey);
        if (sumfileName == null) {
            throw new NullPointerException("Cannot determine correct sumfile for image " + imageKey);
        }
        String sumfileDir = new File(getImageFileName()).getParentFile().getParent();
        return FileCache.getFileFromServer(SafePaths.getString(sumfileDir, "sumfiles", sumfileName)).getAbsolutePath();
    }

    private String getImageFileName()
    {
        return key.name + ".fit";
    }

    private ImmutableMap<String, String> getSumfileMap()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        File keyFile = new File(getImageFileName());
        File mapFile = FileCache.getFileFromServer(SafePaths.getString(keyFile.getParentFile().getParent(), "make_sumfiles.in"));
        try (BufferedReader br = new BufferedReader(new FileReader(mapFile)))
        {
            while (br.ready())
            {
                String[] line = br.readLine().split("\\s\\s*");
                if (line.length < 2) throw new ParseException("Cannot parse line " + String.join(" ", line) + " to get sum file/image file names", line.length > 0 ? line[0].length() : 0);
                String sumFile = line[0] + ".SUM";
                String imageFile = line[line.length - 1].replace("xx", "");
                builder.put(imageFile, sumFile);
            }
        }
        catch (IOException | ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return builder.build();
    }
}
