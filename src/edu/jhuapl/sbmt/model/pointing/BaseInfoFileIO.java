package edu.jhuapl.sbmt.model.pointing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class BaseInfoFileIO implements InfoFileIO
{
    private PerspectiveImage image;
    public static final String FRUSTUM1 = "FRUSTUM1";
    public static final String FRUSTUM2 = "FRUSTUM2";
    public static final String FRUSTUM3 = "FRUSTUM3";
    public static final String FRUSTUM4 = "FRUSTUM4";
    public static final String BORESIGHT_DIRECTION = "BORESIGHT_DIRECTION";
    public static final String SPACECRAFT_POSITION = "SPACECRAFT_POSITION";
    public static final String SUN_POSITION_LT = "SUN_POSITION_LT";
    public static final String START_TIME = "START_TIME";
    public static final String STOP_TIME = "STOP_TIME";
    public static final String TARGET_PIXEL_COORD = "TARGET_PIXEL_COORD";
    public static final String TARGET_ROTATION = "TARGET_ROTATION";
    public static final String APPLY_ADJUSTMENTS = "APPLY_ADJUSTMENTS";
    public static final String TARGET_ZOOM_FACTOR = "TARGET_ZOOM_FACTOR";
    public static final String UP_DIRECTION = "UP_DIRECTION";

    public BaseInfoFileIO(PerspectiveImage image)
    {
        this.image = image;
    }

    public void saveImageInfo()
    {
        String[] infoFileNames = image.getInfoFilesFullPath();
        String sumFileName = image.getSumfileFullPath();

        //        int slice = getCurrentSlice();
        //        System.out.println("Saving current slice: " + slice);
        try
        {
            int nfiles = infoFileNames.length;
            for (int fileindex=0; fileindex<nfiles; fileindex++)
            {
                String filename = infoFileNames[fileindex];
                if (filename == null || filename.endsWith("/null"))
                    filename = sumFileName.substring(0, sumFileName.length()-3) + "INFO";

                int slice = image.getNumberBands() / 2;

                saveImageInfo(
                        filename,
                        slice,
                        image.getStartTime(),
                        image.getStopTime(),
                        image.getSpacecraftPositionOriginal(),
                        image.getSunPositionOriginal(),
                        image.getFrustum1Original(),
                        image.getFrustum2Original(),
                        image.getFrustum3Original(),
                        image.getFrustum4Original(),
                        image.getBoresightDirectionOriginal(),
                        image.getUpVectorOriginal(),
                        image.getTargetPixelCoordinates(),
                        image.getZoomFactor(),
                        image.getRotationOffset(),
                        image.getApplyFramedAdjustments(),
                        false);
            }
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Saves adjusted image info out to an INFO file, folding in the adjusted values, so no adjustment keywords appear.
     *
     * @param infoFileName
     */
    public void saveImageInfo(String infoFileName)
    {
        int slice = (image.getNumberBands() - 1) / 2;
        try
        {
            saveImageInfo(
                    infoFileName,
                    slice,
                    image.getStartTime(),
                    image.getStopTime(),
                    image.getSpacecraftPositionAdjusted(),
                    image.getSunPositionAdjusted(),
                    image.getFrustum1Adjusted(),
                    image.getFrustum2Adjusted(),
                    image.getFrustum3Adjusted(),
                    image.getFrustum4Adjusted(),
                    image.getBoresightDirectionAdjusted(),
                    image.getUpVectorAdjusted(),
                    image.getTargetPixelCoordinates(),
                    image.getZoomFactor(),
                    image.getRotationOffset(),
                    image.getApplyFramedAdjustments(),
                    true);
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void loadImageInfo() throws NumberFormatException, IOException
    {
        String[] infoFileNames = image.getInfoFilesFullPath();
        //        for (String name : infoFileNames) System.out.println("PerspectiveImage: loadImageInfo: name is " + name);
        if (infoFileNames == null)
            System.out.println("infoFileNames is null");

        int nfiles = infoFileNames.length;
        int nslices = image.getNumberBands();

        //        if (nslices > 1)
        //            initSpacecraftStateVariables();

        boolean pad = nfiles > 1;

        for (int k=0; k<nfiles; k++)
        {
            String[] start = new String[1];
            String[] stop = new String[1];
            boolean[] ato = new boolean[1];
            ato[0] = true;

            //            System.out.println("Loading image: " + infoFileNames[k]);

            loadImageInfo(
                    infoFileNames[k],
                    k,
                    pad,
                    start,
                    stop,
                    image.getSpacecraftPositionOriginal(),
                    image.getSunPositionOriginal(),
                    image.getFrustum1Original(),
                    image.getFrustum2Original(),
                    image.getFrustum3Original(),
                    image.getFrustum4Original(),
                    image.getBoresightDirectionOriginal(),
                    image.getUpVectorOriginal(),
                    image.getTargetPixelCoordinates(),
                    ato);

            // should startTime and stopTime be an array? -turnerj1
            image.setStartTime(start[0]);
            image.setStopTime(stop[0]);
            image.setApplyFrameAdjustments(ato[0]);

            //            updateFrustumOffset();

            //        printpt(frustum1, "pds frustum1 ");
            //        printpt(frustum2, "pds frustum2 ");
            //        printpt(frustum3, "pds frustum3 ");
            //        printpt(frustum4, "pds frustum4 ");
        }
    }

    public void deleteAdjustedImageInfo()
    {
        String[] infoFileNames = image.getInfoFilesFullPath();

        int nfiles = infoFileNames.length;

        boolean pad = nfiles > 1;

        for (int k=0; k<nfiles; k++)
        {
            String[] start = new String[1];
            String[] stop = new String[1];
            boolean[] ato = new boolean[1];
            ato[0] = true;

            deleteAdjustedImageInfo(infoFileNames[k]);
        }
    }

    protected void loadImageInfo(
            String infoFilename,
            int startSlice,        // for loading multiple info files, the starting array index to put the info into
            boolean pad,           // if true, will pad out the rest of the array with the same info
            String[] startTime,
            String[] stopTime,
            double[][] spacecraftPosition,
            double[][] sunPosition,
            double[][] frustum1,
            double[][] frustum2,
            double[][] frustum3,
            double[][] frustum4,
            double[][] boresightDirection,
            double[][] upVector,
            double[] targetPixelCoordinates,
            boolean[] applyFrameAdjustments) throws NumberFormatException, IOException, FileNotFoundException
    {
        if (infoFilename == null || infoFilename.endsWith("null"))
            throw new FileNotFoundException();

        boolean offset = true;

        FileInputStream fs = null;

        // look for an adjusted file first
        try {
            fs = new FileInputStream(infoFilename + ".adjusted");
        } catch (FileNotFoundException e) {
            fs = null;
        }

        // if no adjusted file exists, then load in the original unadjusted file
        if (fs == null)
        {
            //            try {
            fs = new FileInputStream(infoFilename);
            //            } catch (FileNotFoundException e) {
            //                e.printStackTrace();
            //            }
        }

        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        // for multispectral images, the image slice being currently parsed
        int slice = startSlice - 1;

        String str;
        while ((str = in.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(str);
            while (st.hasMoreTokens())
            {
                String token = st.nextToken();
                if (token == null)
                    continue;

                if (START_TIME.equals(token))
                {
                    st.nextToken();
                    startTime[0] = st.nextToken();
                }
                if (STOP_TIME.equals(token))
                {
                    st.nextToken();
                    stopTime[0] = st.nextToken();
                }
                // eventually, we should parse the number of exposures from the INFO file, for now it is hard-coded -turnerj1
                //                if (NUMBER_EXPOSURES.equals(token))
                //                {
                //                    numberExposures = Integer.parseInt(st.nextToken());
                //                    if (numberExposures > 1)
                //                    {
                //                        spacecraftPosition = new double[numberExposures][3];
                //                        frustum1 = new double[numberExposures][3];
                //                        frustum2 = new double[numberExposures][3];
                //                        frustum3 = new double[numberExposures][3];
                //                        frustum4 = new double[numberExposures][3];
                //                        sunVector = new double[numberExposures][3];
                //                        boresightDirection = new double[numberExposures][3];
                //                        upVector = new double[numberExposures][3];
                //                        frusta = new Frustum[numberExposures];
                //                        footprint = new vtkPolyData[numberExposures];
                //                        footprintCreated = new boolean[numberExposures];
                //                        shiftedFootprint = new vtkPolyData[numberExposures];
                //                    }
                //                }
                // For backwards compatibility with MSI images we use the endsWith function
                // rather than equals for FRUSTUM1, FRUSTUM2, FRUSTUM3, FRUSTUM4, BORESIGHT_DIRECTION
                // and UP_DIRECTION since these are all prefixed with MSI_ in the info file.
                if (token.equals(TARGET_PIXEL_COORD))
                {
                    st.nextToken();
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double y = Double.parseDouble(st.nextToken());
                    targetPixelCoordinates[0] = x;
                    targetPixelCoordinates[1] = y;
                }
                if (token.equals(TARGET_ROTATION))
                {
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    image.getRotationOffset()[0] = x;
                }
                if (token.equals(TARGET_ZOOM_FACTOR))
                {
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    image.getZoomFactor()[0] = x;
                }
                if (token.equals(APPLY_ADJUSTMENTS))
                {
                    st.nextToken();
                    offset = Boolean.parseBoolean(st.nextToken());
                    applyFrameAdjustments[0] = offset;
                }

                if (SPACECRAFT_POSITION.equals(token) ||
                        SUN_POSITION_LT.equals(token) ||
                        token.endsWith(FRUSTUM1) ||
                        token.endsWith(FRUSTUM2) ||
                        token.endsWith(FRUSTUM3) ||
                        token.endsWith(FRUSTUM4) ||
                        token.endsWith(BORESIGHT_DIRECTION) ||
                        token.endsWith(UP_DIRECTION))
                {
                    st.nextToken();
                    st.nextToken();
                    double x = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double y = Double.parseDouble(st.nextToken());
                    st.nextToken();
                    double z = Double.parseDouble(st.nextToken());
                    if (SPACECRAFT_POSITION.equals(token))
                    {
                        // SPACECRAFT_POSITION is assumed to be at the start of a frame, so increment slice count
                        slice++;
                        spacecraftPosition[slice][0] = x;
                        spacecraftPosition[slice][1] = y;
                        spacecraftPosition[slice][2] = z;
                    }
                    if (SUN_POSITION_LT.equals(token))
                    {
                        sunPosition[slice][0] = x;
                        sunPosition[slice][1] = y;
                        sunPosition[slice][2] = z;
                        //                        MathUtil.vhat(sunPosition[slice], sunPosition[slice]);
                    }
                    else if (token.endsWith(FRUSTUM1))
                    {
                        frustum1[slice][0] = x;
                        frustum1[slice][1] = y;
                        frustum1[slice][2] = z;
                        MathUtil.vhat(frustum1[slice], frustum1[slice]);
                    }
                    else if (token.endsWith(FRUSTUM2))
                    {
                        frustum2[slice][0] = x;
                        frustum2[slice][1] = y;
                        frustum2[slice][2] = z;
                        MathUtil.vhat(frustum2[slice], frustum2[slice]);
                    }
                    else if (token.endsWith(FRUSTUM3))
                    {
                        frustum3[slice][0] = x;
                        frustum3[slice][1] = y;
                        frustum3[slice][2] = z;
                        MathUtil.vhat(frustum3[slice], frustum3[slice]);
                    }
                    else if (token.endsWith(FRUSTUM4))
                    {
                        frustum4[slice][0] = x;
                        frustum4[slice][1] = y;
                        frustum4[slice][2] = z;
                        MathUtil.vhat(frustum4[slice], frustum4[slice]);
                    }
                    if (token.endsWith(BORESIGHT_DIRECTION))
                    {
                        boresightDirection[slice][0] = x;
                        boresightDirection[slice][1] = y;
                        boresightDirection[slice][2] = z;
                    }
                    if (token.endsWith(UP_DIRECTION))
                    {
                        upVector[slice][0] = x;
                        upVector[slice][1] = y;
                        upVector[slice][2] = z;
                    }
                }
            }
        }

        // once we've read in all the frames, pad out any additional missing frames
        if (pad)
        {
            int nslices = image.getNumberBands();
            for (int i=slice+1; i<nslices; i++)
            {
                System.out.println("PerspectiveImage: loadImageInfo: num slices " + nslices + " and slice is " + slice + " and i is " + i + " and spacecraft pos length" + spacecraftPosition.length);

                spacecraftPosition[i][0] = spacecraftPosition[slice][0];
                spacecraftPosition[i][1] = spacecraftPosition[slice][1];
                spacecraftPosition[i][2] = spacecraftPosition[slice][2];

                sunPosition[i][0] = sunPosition[slice][0];
                sunPosition[i][1] = sunPosition[slice][1];
                sunPosition[i][2] = sunPosition[slice][2];

                frustum1[i][0] = frustum1[slice][0];
                frustum1[i][1] = frustum1[slice][1];
                frustum1[i][2] = frustum1[slice][2];

                frustum2[i][0] = frustum2[slice][0];
                frustum2[i][1] = frustum2[slice][1];
                frustum2[i][2] = frustum2[slice][2];

                frustum3[i][0] = frustum3[slice][0];
                frustum3[i][1] = frustum3[slice][1];
                frustum3[i][2] = frustum3[slice][2];

                frustum4[i][0] = frustum4[slice][0];
                frustum4[i][1] = frustum4[slice][1];
                frustum4[i][2] = frustum4[slice][2];

                boresightDirection[i][0] = boresightDirection[slice][0];
                boresightDirection[i][1] = boresightDirection[slice][1];
                boresightDirection[i][2] = boresightDirection[slice][2];

                upVector[slice][0] = upVector[slice][0];
                upVector[slice][1] = upVector[slice][1];
                upVector[slice][2] = upVector[slice][2];
            }
        }

        in.close();
    }

//    public void saveImageInfo(
//            String infoFilename,
//            int slice,        // currently, we only support single-frame INFO files
//            String startTime,
//            String stopTime,
//            double[][] spacecraftPosition,
//            double[][] sunPosition,
//            double[][] frustum1,
//            double[][] frustum2,
//            double[][] frustum3,
//            double[][] frustum4,
//            double[][] boresightDirection,
//            double[][] upVector,
//            double[] targetPixelCoordinates,
//            double[] zoomFactor,
//            double[] rotationOffset,
//            boolean applyFrameAdjustments,
//            boolean flatten) throws NumberFormatException, IOException
//    {
//        PerspectiveImageIOHelpers.saveImageInfo(infoFilename, slice, startTime, stopTime, spacecraftPosition, sunPosition, frustum1, frustum2, frustum3, frustum4, boresightDirection, upVector, targetPixelCoordinates, zoomFactor, rotationOffset, applyFrameAdjustments, flatten);
//    }

//    protected void loadImageInfo(
//            String infoFilename,
//            int startSlice,        // for loading multiple info files, the starting array index to put the info into
//            boolean pad,           // if true, will pad out the rest of the array with the same info
//            String[] startTime,
//            String[] stopTime,
//            double[][] spacecraftPosition,
//            double[][] sunPosition,
//            double[][] frustum1,
//            double[][] frustum2,
//            double[][] frustum3,
//            double[][] frustum4,
//            double[][] boresightDirection,
//            double[][] upVector,
//            double[] targetPixelCoordinates,
//            boolean[] applyFrameAdjustments) throws NumberFormatException, IOException, FileNotFoundException
//    {
//        loadImageInfo(infoFilename, startSlice, pad, startTime, stopTime, spacecraftPosition, sunPosition, frustum1, frustum2, frustum3, frustum4, boresightDirection, upVector, targetPixelCoordinates, applyFrameAdjustments);
//    }

    private void deleteAdjustedImageInfo(String filePath)
    {
        // Deletes for either SUM or INFO files with the following priority scheme:
        // - if a SUM file is specified, look first for an adjusted INFO file, then look for the SUM file
        // - if an INFO file is specified, look first for an adjusted INFO file, the the INFO file



        if (filePath == null || filePath.endsWith("null"))
        {
            filePath = image.getSumfileFullPath();
            if (filePath != null && filePath.endsWith("SUM"))
                filePath = filePath.substring(0, filePath.length()-3) + "INFO";
            else
                filePath = "";
        }

        // look for an adjusted file first
        try {
            File f = new File(filePath + ".adjusted");
            if (f.exists())
                f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveImageInfo(
            String infoFilename,
            int slice,        // currently, we only support single-frame INFO files
            String startTime,
            String stopTime,
            double[][] spacecraftPosition,
            double[][] sunPosition,
            double[][] frustum1,
            double[][] frustum2,
            double[][] frustum3,
            double[][] frustum4,
            double[][] boresightDirection,
            double[][] upVector,
            double[] targetPixelCoordinates,
            double[] zoomFactor,
            double[] rotationOffset,
            boolean applyFrameAdjustments,
            boolean flatten) throws NumberFormatException, IOException
    {
        // for testing purposes only:
        //        infoFilename = infoFilename + ".txt";
        //        System.out.println("Saving infofile to: " + infoFilename + ".adjusted");

        FileOutputStream fs = null;

        // save out info file to cache with ".adjusted" appended to the name
        String suffix = flatten ? "" : ".adjusted";
        try {
            fs = new FileOutputStream(infoFilename + suffix);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        OutputStreamWriter osw = new OutputStreamWriter(fs);
        BufferedWriter out = new BufferedWriter(osw);

        out.write(String.format("%-20s= %s\n", START_TIME, startTime));
        out.write(String.format("%-20s= %s\n", STOP_TIME, stopTime));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", SPACECRAFT_POSITION, spacecraftPosition[slice][0], spacecraftPosition[slice][1], spacecraftPosition[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", BORESIGHT_DIRECTION, boresightDirection[slice][0], boresightDirection[slice][1], boresightDirection[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", UP_DIRECTION, upVector[slice][0], upVector[slice][1], upVector[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM1, frustum1[slice][0], frustum1[slice][1], frustum1[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM2, frustum2[slice][0], frustum2[slice][1], frustum2[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM3, frustum3[slice][0], frustum3[slice][1], frustum3[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM4, frustum4[slice][0], frustum4[slice][1], frustum4[slice][2]));
        out.write(String.format("%-20s= ( %1.16e , %1.16e , %1.16e )\n", SUN_POSITION_LT, sunPosition[slice][0], sunPosition[slice][1], sunPosition[slice][2]));

        boolean writeApplyAdustments = false;

        if (!flatten)
        {
            if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1] != Double.MAX_VALUE)
            {
                out.write(String.format("%-20s= ( %1.16e , %1.16e )\n", TARGET_PIXEL_COORD, targetPixelCoordinates[0], targetPixelCoordinates[1]));
                writeApplyAdustments = true;
            }

            if (zoomFactor[0] != 1.0)
            {
                out.write(String.format("%-20s= %1.16e\n", TARGET_ZOOM_FACTOR, zoomFactor[0]));
                writeApplyAdustments = true;
            }

            if (rotationOffset[0] != 0.0)
            {
                out.write(String.format("%-20s= %1.16e\n", TARGET_ROTATION, rotationOffset[0]));
                writeApplyAdustments = true;
            }

            // only write out user-modified offsets if the image info has been modified
            if (writeApplyAdustments)
                out.write(String.format("%-20s= %b\n", APPLY_ADJUSTMENTS, applyFrameAdjustments));
        }

        out.close();
    }


    public PerspectiveImage getImage()
    {
        return image;
    }

    public void setImage(PerspectiveImage image)
    {
        this.image = image;
    }

    public void loadAdjustedSumfile() throws NumberFormatException, IOException
    {
        // Looks for either SUM or INFO files with the following priority scheme:
        // - if a SUM file is specified, look first for an adjusted INFO file, then look for the SUM file
        // - if an INFO file is specified, look first for an adjusted INFO file, the the INFO file
        String filePath = image.getSumfileFullPath();
        if (filePath != null && filePath.endsWith("SUM"))
            filePath = filePath.substring(0, filePath.length()-3) + "INFO";
        else
            filePath = "";

        String[] start = new String[1];
        String[] stop = new String[1];
        boolean[] ato = new boolean[1];
        ato[0] = true;

        System.out.println("BaseSumFileIO: loadAdjustedSumfile: file path " + filePath);

        loadImageInfo(
                filePath,
                0,
                false,
                start,
                stop,
                image.getSpacecraftPositionOriginal(),
                image.getSunPositionOriginal(),
                image.getFrustum1Original(),
                image.getFrustum2Original(),
                image.getFrustum3Original(),
                image.getFrustum4Original(),
                image.getBoresightDirectionOriginal(),
                image.getUpVectorOriginal(),
                image.getTargetPixelCoordinates(),
                ato);

        // should startTime and stopTime be an array? -turnerj1
        image.setStartTime(start[0]);
        image.setStopTime(stop[0]);
        image.setApplyFrameAdjustments(ato[0]);
    }

    @Override
    public String initLocalInfoFileFullPath()
    {
        return "";
    }



}
