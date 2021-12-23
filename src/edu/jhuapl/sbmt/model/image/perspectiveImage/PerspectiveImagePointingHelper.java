//package edu.jhuapl.sbmt.model.image.perspectiveImage;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.StringTokenizer;
//
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
//import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
//
//import edu.jhuapl.saavtk.util.DateTimeUtil;
//import edu.jhuapl.saavtk.util.IntensityRange;
//import edu.jhuapl.saavtk.util.MathUtil;
//import edu.jhuapl.sbmt.model.image.ImageSource;
//
//import nom.tam.fits.FitsException;
//
//public class PerspectiveImagePointingHelper
//{
//	PerspectiveImage image;
//
//	public PerspectiveImagePointingHelper(PerspectiveImage image)
//	{
//		this.image = image;
//	}
//
//	   ///////////////////////////
//    // Pointing methods
//    ///////////////////////////
//    protected void loadImageInfo( //
//            String infoFilename, //
//            int startSlice, // for loading multiple info files, the starting array index to put the info
//                            // into
//            boolean pad, // if true, will pad out the rest of the array with the same info
//            String[] startTime, //
//            String[] stopTime, //
//            double[][] spacecraftPosition, //
//            double[][] sunPosition, //
//            double[][] frustum1, //
//            double[][] frustum2, //
//            double[][] frustum3, //
//            double[][] frustum4, //
//            double[][] boresightDirection, //
//            double[][] upVector, //
//            double[] targetPixelCoordinates, //
//            boolean[] applyFrameAdjustments, //
//            IntensityRange[] displayRange, //
//            IntensityRange[] offlimbDisplayRange) throws NumberFormatException, IOException, FileNotFoundException //
//    {
//        if (infoFilename == null || infoFilename.endsWith("null"))
//            throw new FileNotFoundException();
//
//        boolean offset = true;
//
//        FileInputStream fs = null;
//
//        // look for an adjusted file first
//        try
//        {
//            fs = new FileInputStream(infoFilename + ".adjusted");
//        }
//        catch (FileNotFoundException e)
//        {
//            fs = null;
//        }
//
//        // if no adjusted file exists, then load in the original unadjusted file
//        if (fs == null)
//        {
//            // try {
//            fs = new FileInputStream(infoFilename);
//            // } catch (FileNotFoundException e) {
//            // e.printStackTrace();
//            // }
//        }
//
//        InputStreamReader isr = new InputStreamReader(fs);
//        BufferedReader in = new BufferedReader(isr);
//
//        // for multispectral images, the image slice being currently parsed
//        int slice = startSlice - 1;
//
//        String str;
//        while ((str = in.readLine()) != null)
//        {
//            StringTokenizer st = new StringTokenizer(str);
//            while (st.hasMoreTokens())
//            {
//                String token = st.nextToken();
//                if (token == null)
//                    continue;
//
//                if (START_TIME.equals(token))
//                {
//                    st.nextToken();
//                    startTime[0] = st.nextToken();
//                }
//                if (STOP_TIME.equals(token))
//                {
//                    st.nextToken();
//                    stopTime[0] = st.nextToken();
//                }
//                // eventually, we should parse the number of exposures from the INFO file, for
//                // now it is hard-coded -turnerj1
//                // if (NUMBER_EXPOSURES.equals(token))
//                // {
//                // numberExposures = Integer.parseInt(st.nextToken());
//                // if (numberExposures > 1)
//                // {
//                // spacecraftPosition = new double[numberExposures][3];
//                // frustum1 = new double[numberExposures][3];
//                // frustum2 = new double[numberExposures][3];
//                // frustum3 = new double[numberExposures][3];
//                // frustum4 = new double[numberExposures][3];
//                // sunVector = new double[numberExposures][3];
//                // boresightDirection = new double[numberExposures][3];
//                // upVector = new double[numberExposures][3];
//                // frusta = new Frustum[numberExposures];
//                // footprint = new vtkPolyData[numberExposures];
//                // footprintCreated = new boolean[numberExposures];
//                // shiftedFootprint = new vtkPolyData[numberExposures];
//                // }
//                // }
//                // For backwards compatibility with MSI images we use the endsWith function
//                // rather than equals for FRUSTUM1, FRUSTUM2, FRUSTUM3, FRUSTUM4,
//                // BORESIGHT_DIRECTION
//                // and UP_DIRECTION since these are all prefixed with MSI_ in the info file.
//                if (token.equals(TARGET_PIXEL_COORD))
//                {
//                    st.nextToken();
//                    st.nextToken();
//                    double x = Double.parseDouble(st.nextToken());
//                    st.nextToken();
//                    double y = Double.parseDouble(st.nextToken());
//                    targetPixelCoordinates[0] = x;
//                    targetPixelCoordinates[1] = y;
//                }
//                if (token.equals(TARGET_ROTATION))
//                {
//                    st.nextToken();
//                    double x = Double.parseDouble(st.nextToken());
//                    getRotationOffset()[0] = x;
//                }
//                if (token.equals(TARGET_ZOOM_FACTOR))
//                {
//                    st.nextToken();
//                    double x = Double.parseDouble(st.nextToken());
//                    getZoomFactor()[0] = x;
//                }
//                if (token.equals(APPLY_ADJUSTMENTS))
//                {
//                    st.nextToken();
//                    offset = Boolean.parseBoolean(st.nextToken());
//                    applyFrameAdjustments[0] = offset;
//                }
//
//                if (SPACECRAFT_POSITION.equals(token) || //
//                        SUN_POSITION_LT.equals(token) || //
//                        token.endsWith(FRUSTUM1) || //
//                        token.endsWith(FRUSTUM2) || //
//                        token.endsWith(FRUSTUM3) || //
//                        token.endsWith(FRUSTUM4) || //
//                        token.endsWith(BORESIGHT_DIRECTION) || //
//                        token.endsWith(UP_DIRECTION)) //
//                {
//                    st.nextToken();
//                    st.nextToken();
//                    double x = Double.parseDouble(st.nextToken());
//                    st.nextToken();
//                    double y = Double.parseDouble(st.nextToken());
//                    st.nextToken();
//                    double z = Double.parseDouble(st.nextToken());
//                    if (SPACECRAFT_POSITION.equals(token))
//                    {
//                        // SPACECRAFT_POSITION is assumed to be at the start of a frame, so increment
//                        // slice count
//                        slice++;
//                        spacecraftPosition[slice][0] = x;
//                        spacecraftPosition[slice][1] = y;
//                        spacecraftPosition[slice][2] = z;
//                    }
//                    if (SUN_POSITION_LT.equals(token))
//                    {
//                        sunPosition[slice][0] = x;
//                        sunPosition[slice][1] = y;
//                        sunPosition[slice][2] = z;
//                        // MathUtil.vhat(sunPosition[slice], sunPosition[slice]);
//                    }
//                    else if (token.endsWith(FRUSTUM1))
//                    {
//                        frustum1[slice][0] = x;
//                        frustum1[slice][1] = y;
//                        frustum1[slice][2] = z;
//                        MathUtil.vhat(frustum1[slice], frustum1[slice]);
//                    }
//                    else if (token.endsWith(FRUSTUM2))
//                    {
//                        frustum2[slice][0] = x;
//                        frustum2[slice][1] = y;
//                        frustum2[slice][2] = z;
//                        MathUtil.vhat(frustum2[slice], frustum2[slice]);
//                    }
//                    else if (token.endsWith(FRUSTUM3))
//                    {
//                        frustum3[slice][0] = x;
//                        frustum3[slice][1] = y;
//                        frustum3[slice][2] = z;
//                        MathUtil.vhat(frustum3[slice], frustum3[slice]);
//                    }
//                    else if (token.endsWith(FRUSTUM4))
//                    {
//                        frustum4[slice][0] = x;
//                        frustum4[slice][1] = y;
//                        frustum4[slice][2] = z;
//                        MathUtil.vhat(frustum4[slice], frustum4[slice]);
//                    }
//                    if (token.endsWith(BORESIGHT_DIRECTION))
//                    {
//                        boresightDirection[slice][0] = x;
//                        boresightDirection[slice][1] = y;
//                        boresightDirection[slice][2] = z;
//                    }
//                    if (token.endsWith(UP_DIRECTION))
//                    {
//                        upVector[slice][0] = x;
//                        upVector[slice][1] = y;
//                        upVector[slice][2] = z;
//                    }
//                }
//                if (token.equals(DISPLAY_RANGE))
//                {
//                    st.nextToken();
//                    st.nextToken();
//                    int min = Integer.parseInt(st.nextToken());
//                    st.nextToken();
//                    int max = Integer.parseInt(st.nextToken());
//                    st.nextToken();
//                    displayRange[0] = new IntensityRange(min, max);
//                }
//                if (token.equals(OFFLIMB_DISPLAY_RANGE))
//                {
//                    st.nextToken();
//                    st.nextToken();
//                    int min = Integer.parseInt(st.nextToken());
//                    st.nextToken();
//                    int max = Integer.parseInt(st.nextToken());
//                    st.nextToken();
//                    offlimbDisplayRange[0] = new IntensityRange(min, max);
//                }
//            }
//        }
//
//        // once we've read in all the frames, pad out any additional missing frames
//        if (pad)
//        {
//            int nslices = getImageDepth();
//            for (int i = slice + 1; i < nslices; i++)
//            {
//                System.out.println("PerspectiveImage: loadImageInfo: num slices " + nslices + " and slice is " + slice + " and i is " + i + " and spacecraft pos length" + spacecraftPosition.length);
//
//                spacecraftPosition[i][0] = spacecraftPosition[slice][0];
//                spacecraftPosition[i][1] = spacecraftPosition[slice][1];
//                spacecraftPosition[i][2] = spacecraftPosition[slice][2];
//
//                sunPosition[i][0] = sunPosition[slice][0];
//                sunPosition[i][1] = sunPosition[slice][1];
//                sunPosition[i][2] = sunPosition[slice][2];
//
//                frustum1[i][0] = frustum1[slice][0];
//                frustum1[i][1] = frustum1[slice][1];
//                frustum1[i][2] = frustum1[slice][2];
//
//                frustum2[i][0] = frustum2[slice][0];
//                frustum2[i][1] = frustum2[slice][1];
//                frustum2[i][2] = frustum2[slice][2];
//
//                frustum3[i][0] = frustum3[slice][0];
//                frustum3[i][1] = frustum3[slice][1];
//                frustum3[i][2] = frustum3[slice][2];
//
//                frustum4[i][0] = frustum4[slice][0];
//                frustum4[i][1] = frustum4[slice][1];
//                frustum4[i][2] = frustum4[slice][2];
//
//                boresightDirection[i][0] = boresightDirection[slice][0];
//                boresightDirection[i][1] = boresightDirection[slice][1];
//                boresightDirection[i][2] = boresightDirection[slice][2];
//
//                upVector[slice][0] = upVector[slice][0];
//                upVector[slice][1] = upVector[slice][1];
//                upVector[slice][2] = upVector[slice][2];
//            }
//        }
//
//        in.close();
//    }
//
//    public void saveImageInfo( //
//            String infoFilename, //
//            int slice, // currently, we only support single-frame INFO files
//            String startTime, //
//            String stopTime, //
//            double[][] spacecraftPosition, //
//            double[][] sunPosition, //
//            double[][] frustum1, //
//            double[][] frustum2, //
//            double[][] frustum3, //
//            double[][] frustum4, //
//            double[][] boresightDirection, //
//            double[][] upVector, //
//            double[] targetPixelCoordinates, //
//            double[] zoomFactor, //
//            double[] rotationOffset, //
//            boolean applyFrameAdjustments, //
//            boolean flatten, //
//            IntensityRange displayRange, //
//            IntensityRange offLimbDisplayRange //
//    ) throws NumberFormatException, IOException //
//    {
//        // for testing purposes only:
//        // infoFilename = infoFilename + ".txt";
//        // System.out.println("Saving infofile to: " + infoFilename + ".adjusted");
//
//        FileOutputStream fs = null;
//
//        // save out info file to cache with ".adjusted" appended to the name
//        String suffix = flatten ? "" : ".adjusted";
//        try
//        {
//            fs = new FileOutputStream(infoFilename + suffix);
//        }
//        catch (FileNotFoundException e)
//        {
//            e.printStackTrace();
//            return;
//        }
//        OutputStreamWriter osw = new OutputStreamWriter(fs);
//        BufferedWriter out = new BufferedWriter(osw);
//
//        out.write(String.format("%-22s= %s\n", START_TIME, startTime));
//        out.write(String.format("%-22s= %s\n", STOP_TIME, stopTime));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SPACECRAFT_POSITION, spacecraftPosition[slice][0], spacecraftPosition[slice][1], spacecraftPosition[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", BORESIGHT_DIRECTION, boresightDirection[slice][0], boresightDirection[slice][1], boresightDirection[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", UP_DIRECTION, upVector[slice][0], upVector[slice][1], upVector[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM1, frustum1[slice][0], frustum1[slice][1], frustum1[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM2, frustum2[slice][0], frustum2[slice][1], frustum2[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM3, frustum3[slice][0], frustum3[slice][1], frustum3[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", FRUSTUM4, frustum4[slice][0], frustum4[slice][1], frustum4[slice][2]));
//        out.write(String.format("%-22s= ( %1.16e , %1.16e , %1.16e )\n", SUN_POSITION_LT, sunPosition[slice][0], sunPosition[slice][1], sunPosition[slice][2]));
//        out.write(String.format("%-22s= ( %16d , %16d )\n", DISPLAY_RANGE, displayRange.min, displayRange.max));
//        out.write(String.format("%-22s= ( %16d , %16d )\n", OFFLIMB_DISPLAY_RANGE, offLimbDisplayRange.min, offLimbDisplayRange.max));
//
//        boolean writeApplyAdustments = false;
//
//        if (!flatten)
//        {
//            if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1] != Double.MAX_VALUE)
//            {
//                out.write(String.format("%-22s= ( %1.16e , %1.16e )\n", TARGET_PIXEL_COORD, targetPixelCoordinates[0], targetPixelCoordinates[1]));
//                writeApplyAdustments = true;
//            }
//
//            if (zoomFactor[0] != 1.0)
//            {
//                out.write(String.format("%-22s= %1.16e\n", TARGET_ZOOM_FACTOR, zoomFactor[0]));
//                writeApplyAdustments = true;
//            }
//
//            if (rotationOffset[0] != 0.0)
//            {
//                out.write(String.format("%-22s= %1.16e\n", TARGET_ROTATION, rotationOffset[0]));
//                writeApplyAdustments = true;
//            }
//
//            // only write out user-modified offsets if the image info has been modified
//            if (writeApplyAdustments)
//                out.write(String.format("%-22s= %b\n", APPLY_ADJUSTMENTS, applyFrameAdjustments));
//        }
//
//        out.close();
//    }
//
//    public String getLabelFileFullPath()
//    {
//        return labelFileFullPath;
//    }
//
//    public String getInfoFileFullPath()
//    {
//        return infoFileFullPath;
//    }
//
//    public String[] getInfoFilesFullPath()
//    {
//        String[] result = { infoFileFullPath };
//        return result;
//    }
//
//    public String getSumfileFullPath()
//    {
//        return sumFileFullPath;
//    }
//
//    public String getLabelfileFullPath()
//    {
//        return getLabelFileFullPath();
//    }
//
//    protected void loadPointing() throws FitsException, IOException
//    {
//        if (key.getSource().equals(ImageSource.SPICE) || key.getSource().equals(ImageSource.CORRECTED_SPICE))
//        {
//            try
//            {
//                loadImageInfo();
//            }
//            catch (IOException ex)
//            {
//                System.out.println("INFO file not available");
//                ex.printStackTrace();
//            }
//        }
//        else if (key.getSource().equals(ImageSource.LABEL))
//        {
//            try
//            {
//                loadLabelFile();
//            }
//            catch (IOException ex)
//            {
//                System.out.println("LABEL file not available");
//            }
//        }
//        else if (key.getSource().equals(ImageSource.LOCAL_PERSPECTIVE))
//        {
//            boolean loaded = false;
//            try
//            {
//                loadAdjustedSumfile();
//                loaded = true;
//            }
//            catch (FileNotFoundException e)
//            {
//                loaded = false;
//            }
//            if (!loaded)
//            {
//                try
//                {
//                    loadSumfile();
//                    loaded = true;
//                }
//                catch (FileNotFoundException e)
//                {
//                    loaded = false;
//                }
//            }
//            if (!loaded)
//                this.loadImageInfo();
//        }
//        else
//        {
//            boolean loaded = false;
//            try
//            {
//                loadAdjustedSumfile();
//                loaded = true;
//            }
//            catch (FileNotFoundException e)
//            {
//                loaded = false;
//            }
//            if (!loaded)
//            {
//                try
//                {
//                    loadSumfile();
//                    loaded = true;
//                }
//                catch (FileNotFoundException e)
//                {
//                    System.out.println("SUM file not available");
//                    throw (e);
//                }
//            }
//        }
//
//        // copy loaded state values into the adjusted values
//        copySpacecraftState();
//    }
//
//    private void loadImageInfo() throws NumberFormatException, IOException
//    {
//        String[] infoFileNames = getInfoFilesFullPath();
//        // for (String name : infoFileNames) System.out.println("PerspectiveImage:
//        // loadImageInfo: name is " + name);
//        if (infoFileNames == null)
//            System.out.println("infoFileNames is null");
//
//        int nfiles = infoFileNames.length;
//
//        // if (nslices > 1)
//        // initSpacecraftStateVariables();
//
//        boolean pad = nfiles > 1;
//
//        for (int k = 0; k < nfiles; k++)
//        {
//            String[] start = new String[1];
//            String[] stop = new String[1];
//            boolean[] ato = new boolean[1];
//            ato[0] = true;
//
//            // System.out.println("Loading image: " + infoFileNames[k]);
//
//            IntensityRange[] displayRange = new IntensityRange[1];
//            IntensityRange[] offLimbDisplayRange = new IntensityRange[1];
//
//            loadImageInfo( //
//                    infoFileNames[k], //
//                    k, //
//                    pad, //
//                    start, //
//                    stop, //
//                    spacecraftPositionOriginal, //
//                    sunPositionOriginal, //
//                    frustum1Original, //
//                    frustum2Original, //
//                    frustum3Original, //
//                    frustum4Original, //
//                    boresightDirectionOriginal, //
//                    upVectorOriginal, //
//                    getTargetPixelCoordinates(), //
//                    ato, //
//                    displayRange, //
//                    offLimbDisplayRange);
//
//            // should startTime and stopTime be an array? -turnerj1
//            startTime = start[0];
//            stopTime = stop[0];
//            imageOffsetCalculator.applyFrameAdjustments[0] = ato[0];
//
//            if (displayRange[0] != null)
//            {
//                setDisplayedImageRange(displayRange[0]);
//            }
//            if (offLimbDisplayRange[0] != null)
//            {
//                setOfflimbImageRange(offLimbDisplayRange[0]);
//            }
//
//            // updateFrustumOffset();
//
//            // printpt(frustum1, "pds frustum1 ");
//            // printpt(frustum2, "pds frustum2 ");
//            // printpt(frustum3, "pds frustum3 ");
//            // printpt(frustum4, "pds frustum4 ");
//        }
//    }
//
//    private void deleteAdjustedImageInfo()
//    {
//        String[] infoFileNames = getInfoFilesFullPath();
//
//        int nfiles = infoFileNames.length;
//
//        for (int k = 0; k < nfiles; k++)
//        {
//            boolean[] ato = new boolean[1];
//            ato[0] = true;
//
//            deleteAdjustedImageInfo(infoFileNames[k]);
//        }
//    }
//
//    private void loadAdjustedSumfile() throws NumberFormatException, IOException
//    {
//        // Looks for either SUM or INFO files with the following priority scheme:
//        // - if a SUM file is specified, look first for an adjusted INFO file, then look
//        // for the SUM file
//        // - if an INFO file is specified, look first for an adjusted INFO file, the the
//        // INFO file
//        String filePath = getSumfileFullPath();
//        if (filePath != null && filePath.endsWith("SUM"))
//        	filePath = filePath.substring(0, filePath.length()-FilenameUtils.getExtension(filePath).length()) + "INFO";
//        else
//            filePath = "";
//
//        String[] start = new String[1];
//        String[] stop = new String[1];
//        boolean[] ato = new boolean[1];
//        ato[0] = true;
//
//        IntensityRange[] displayRange = new IntensityRange[1];
//        IntensityRange[] offLimbDisplayRange = new IntensityRange[1];
//
//        loadImageInfo( //
//                filePath, //
//                0, //
//                false, //
//                start, //
//                stop, //
//                spacecraftPositionOriginal, //
//                sunPositionOriginal, //
//                frustum1Original, //
//                frustum2Original, //
//                frustum3Original, //
//                frustum4Original, //
//                boresightDirectionOriginal, //
//                upVectorOriginal, //
//                getTargetPixelCoordinates(), //
//                ato, //
//                displayRange, //
//                offLimbDisplayRange);
//
//        // should startTime and stopTime be an array? -turnerj1
//        startTime = start[0];
//        stopTime = stop[0];
//        imageOffsetCalculator.applyFrameAdjustments[0] = ato[0];
//
//        if (displayRange[0] != null)
//        {
//            setDisplayedImageRange(displayRange[0]);
//        }
//        if (offLimbDisplayRange[0] != null)
//        {
//            setOfflimbImageRange(offLimbDisplayRange[0]);
//        }
//
//    }
//
//    void saveImageInfo()
//    {
//        String[] infoFileNames = getInfoFilesFullPath();
//        String sumFileName = this.getSumfileFullPath();
//
//        // int slice = getCurrentSlice();
//        // System.out.println("Saving current slice: " + slice);
//        try
//        {
//            int nfiles = infoFileNames.length;
//            for (int fileindex = 0; fileindex < nfiles; fileindex++)
//            {
//                String filename = infoFileNames[fileindex];
//                if (filename == null || filename.endsWith("/null"))
//                	filename = sumFileName.substring(0, sumFileName.length()-FilenameUtils.getExtension(sumFileName).length()) + "INFO";
//
//                int slice = this.getImageDepth() / 2;
//
//                saveImageInfo( //
//                        filename, //
//                        slice, //
//                        startTime, //
//                        stopTime, //
//                        spacecraftPositionOriginal, //
//                        sunPositionOriginal, //
//                        frustum1Original, //
//                        frustum2Original, //
//                        frustum3Original, //
//                        frustum4Original, //
//                        boresightDirectionOriginal, //
//                        upVectorOriginal, //
//                        getTargetPixelCoordinates(), //
//                        getZoomFactor(), //
//                        getRotationOffset(), //
//                        imageOffsetCalculator.applyFrameAdjustments[0], //
//                        false, //
//                        getDisplayedRange(), //
//                        getOffLimbDisplayedRange());
//            }
//        }
//        catch (NumberFormatException e)
//        {
//            e.printStackTrace();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Saves adjusted image info out to an INFO file, folding in the adjusted
//     * values, so no adjustment keywords appear.
//     *
//     * @param infoFileName
//     */
//    public void saveImageInfo(String infoFileName)
//    {
//        int slice = (getImageDepth() - 1) / 2;
//        try
//        {
//            saveImageInfo( //
//                    infoFileName, //
//                    slice, //
//                    startTime, //
//                    stopTime, //
//                    getSpacecraftPositionAdjusted(), //
//                    getSunPositionAdjusted(), //
//                    getFrustum1Adjusted(), //
//                    getFrustum2Adjusted(), //
//                    getFrustum3Adjusted(), //
//                    getFrustum4Adjusted(), //
//                    getBoresightDirectionAdjusted(), //
//                    getUpVectorAdjusted(), //
//                    getTargetPixelCoordinates(), //
//                    getZoomFactor(), //
//                    getRotationOffset(), //
//                    imageOffsetCalculator.applyFrameAdjustments[0], //
//                    true, //
//                    getDisplayedRange(), //
//                    getOffLimbDisplayedRange());
//        }
//        catch (NumberFormatException e)
//        {
//            e.printStackTrace();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Sometimes Bob Gaskell sumfiles contain numbers of the form .1192696009D+03
//     * rather than .1192696009E+03 (i.e. a D instead of an E). This function
//     * replaces D's with E's.
//     *
//     * @param s
//     * @return
//     */
//    private void replaceDwithE(String[] s)
//    {
//        for (int i = 0; i < s.length; ++i)
//            s[i] = s[i].replace('D', 'E');
//    }
//
//    protected void loadSumfile( //
//            String sumfilename, //
//            String[] startTime, //
//            String[] stopTime, //
//            double[][] spacecraftPosition, //
//            double[][] sunVector, //
//            double[][] frustum1, //
//            double[][] frustum2, //
//            double[][] frustum3, //
//            double[][] frustum4, //
//            double[][] boresightDirection, //
//            double[][] upVector) throws IOException //
//    {
//        if (sumfilename == null)
//            throw new FileNotFoundException();
//
//        FileInputStream fs = new FileInputStream(sumfilename);
//        InputStreamReader isr = new InputStreamReader(fs);
//        BufferedReader in = new BufferedReader(isr);
//
//        // for multispectral images, the image slice being currently parsed
//        int slice = 0;
//
//        in.readLine();
//
//        String datetime = in.readLine().trim();
//        datetime = DateTimeUtil.convertDateTimeFormat(datetime);
//        startTime[0] = datetime;
//        stopTime[0] = datetime;
//
//        String[] tmp = in.readLine().trim().split("\\s+");
//        double npx = Integer.parseInt(tmp[0]);
//        double nln = Integer.parseInt(tmp[1]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        double focalLengthMillimeters = Double.parseDouble(tmp[0]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        spacecraftPosition[slice][0] = -Double.parseDouble(tmp[0]);
//        spacecraftPosition[slice][1] = -Double.parseDouble(tmp[1]);
//        spacecraftPosition[slice][2] = -Double.parseDouble(tmp[2]);
//
//        double[] cx = new double[3];
//        double[] cy = new double[3];
//        double[] cz = new double[3];
//        double[] sz = new double[3];
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        cx[0] = Double.parseDouble(tmp[0]);
//        cx[1] = Double.parseDouble(tmp[1]);
//        cx[2] = Double.parseDouble(tmp[2]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        cy[0] = Double.parseDouble(tmp[0]);
//        cy[1] = Double.parseDouble(tmp[1]);
//        cy[2] = Double.parseDouble(tmp[2]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        cz[0] = Double.parseDouble(tmp[0]);
//        cz[1] = Double.parseDouble(tmp[1]);
//        cz[2] = Double.parseDouble(tmp[2]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        sz[0] = Double.parseDouble(tmp[0]);
//        sz[1] = Double.parseDouble(tmp[1]);
//        sz[2] = Double.parseDouble(tmp[2]);
//
//        tmp = in.readLine().trim().split("\\s+");
//        replaceDwithE(tmp);
//        double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
//        double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));
//
//        // Here we calculate the image width and height using the K-matrix values.
//        // This is used only when the constructor of this function was called with
//        // loadPointingOnly set to true. When set to false, the image width and
//        // and height is set in the loadImage function (after this function is called
//        // and will overwrite these values here--though they should not be different).
//        // But when in pointing-only mode, the loadImage function is not called so
//        // we therefore set the image width and height here since some functions need
//        // it.
//        imageWidth = (int) npx;
//        imageHeight = (int) nln;
//        if (kmatrix00 > kmatrix11)
//            imageHeight = (int) Math.round(nln * (kmatrix00 / kmatrix11));
//        else if (kmatrix11 > kmatrix00)
//            imageWidth = (int) Math.round(npx * (kmatrix11 / kmatrix00));
//
//        double[] cornerVector = new double[3];
//        double fov1 = Math.atan(npx / (2.0 * focalLengthMillimeters * kmatrix00));
//        double fov2 = Math.atan(nln / (2.0 * focalLengthMillimeters * kmatrix11));
//        cornerVector[0] = -Math.tan(fov1);
//        cornerVector[1] = -Math.tan(fov2);
//        cornerVector[2] = 1.0;
//
//        double fx = cornerVector[0];
//        double fy = cornerVector[1];
//        double fz = cornerVector[2];
//        frustum3[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum3[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum3[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = -cornerVector[0];
//        fy = cornerVector[1];
//        fz = cornerVector[2];
//        frustum4[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum4[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum4[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = cornerVector[0];
//        fy = -cornerVector[1];
//        fz = cornerVector[2];
//        frustum1[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum1[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum1[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = -cornerVector[0];
//        fy = -cornerVector[1];
//        fz = cornerVector[2];
//        frustum2[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum2[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum2[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        MathUtil.vhat(frustum1[slice], frustum1[slice]);
//        MathUtil.vhat(frustum2[slice], frustum2[slice]);
//        MathUtil.vhat(frustum3[slice], frustum3[slice]);
//        MathUtil.vhat(frustum4[slice], frustum4[slice]);
//
//        MathUtil.vhat(cz, boresightDirection[slice]);
//        MathUtil.vhat(cx, upVector[slice]);
//        MathUtil.vhat(sz, sunVector[slice]);
//
//        in.close();
//    }
//
//    private void deleteAdjustedImageInfo(String filePath)
//    {
//        // Deletes for either SUM or INFO files with the following priority scheme:
//        // - if a SUM file is specified, look first for an adjusted INFO file, then look
//        // for the SUM file
//        // - if an INFO file is specified, look first for an adjusted INFO file, the the
//        // INFO file
//
//        if (filePath == null || filePath.endsWith("null"))
//        {
//            filePath = getSumfileFullPath();
//            if (filePath != null && filePath.endsWith("SUM"))
//                filePath = filePath.substring(0, filePath.length()-FilenameUtils.getExtension(filePath).length()) + "INFO";
//            else
//                filePath = "";
//        }
//
//        // look for an adjusted file first
//        try
//        {
//            File f = new File(filePath + ".adjusted");
//            if (f.exists())
//                f.delete();
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    //
//    // Label (.lbl) file parsing methods
//    //
//
//    private static final Vector3D i = new Vector3D(1.0, 0.0, 0.0);
//    private static final Vector3D j = new Vector3D(0.0, 1.0, 0.0);
//    private static final Vector3D k = new Vector3D(0.0, 0.0, 1.0);
//
//    private String targetName = null;
//    private String instrumentId = null;
//    private String filterName = null;
//    private String objectName = null;
//
//    private String startTimeString = null;
//    private String stopTimeString = null;
//
//    private String scTargetPositionString = null;
//    private String targetSunPositionString = null;
//    private String scOrientationString = null;
//    private Rotation scOrientation = null;
//    private double[] q = new double[4];
//    private double[] cx = new double[3];
//    private double[] cy = new double[3];
//    private double[] cz = new double[3];
//
//    private double focalLengthMillimeters = 100.0;
//    private double npx = 4096.0;
//    private double nln = 32.0;
//    private double kmatrix00 = 1.0;
//    private double kmatrix11 = 1.0;
//
//    private void parseLabelKeyValuePair( //
//            String key, //
//            String value, //
//            String[] startTime, //
//            String[] stopTime, //
//            double[] spacecraftPosition, //
//            double[] sunVector, //
//            double[] frustum1, //
//            double[] frustum2, //
//            double[] frustum3, //
//            double[] frustum4, //
//            double[] boresightDirection, //
//            double[] upVector) throws IOException //
//    {
//        System.out.println("Label file key: " + key + " = " + value);
//
//        if (key.equals("TARGET_NAME"))
//            targetName = value;
//        else if (key.equals("INSTRUMENT_ID"))
//            instrumentId = value;
//        else if (key.equals("FILTER_NAME"))
//            filterName = value;
//        else if (key.equals("OBJECT"))
//            objectName = value;
//        else if (key.equals("LINE_SAMPLES"))
//        {
//            if (objectName.equals("EXTENSION_CALGEOM_IMAGE"))
//                numberOfPixels = Double.parseDouble(value);
//        }
//        else if (key.equals("LINES"))
//        {
//            if (objectName.equals("EXTENSION_CALGEOM_IMAGE"))
//                numberOfLines = Double.parseDouble(value);
//        }
//        else if (key.equals("START_TIME"))
//        {
//            startTimeString = value;
//            startTime[0] = startTimeString;
//        }
//        else if (key.equals("STOP_TIME"))
//        {
//            stopTimeString = value;
//            stopTime[0] = stopTimeString;
//        }
//        else if (key.equals("SC_TARGET_POSITION_VECTOR"))
//        {
//            scTargetPositionString = value;
//            String p[] = scTargetPositionString.split(",");
//            spacecraftPosition[0] = Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
//            spacecraftPosition[1] = Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
//            spacecraftPosition[2] = Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
//        }
//        else if (key.equals("TARGET_SUN_POSITION_VECTOR"))
//        {
//            targetSunPositionString = value;
//            String p[] = targetSunPositionString.split(",");
//            sunVector[0] = -Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
//            sunVector[1] = -Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
//            sunVector[2] = -Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
//        }
//        else if (key.equals("QUATERNION"))
//        {
//            scOrientationString = value;
//            String qstr[] = scOrientationString.split(",");
//            q[0] = Double.parseDouble(qstr[0].trim().split("\\s+")[0].trim());
//            q[1] = Double.parseDouble(qstr[1].trim().split("\\s+")[0].trim());
//            q[2] = Double.parseDouble(qstr[2].trim().split("\\s+")[0].trim());
//            q[3] = Double.parseDouble(qstr[3].trim().split("\\s+")[0].trim());
//            scOrientation = new Rotation(q[0], q[1], q[2], q[3], false);
//        }
//
//    }
//
//    protected void loadLabelFile( //
//            String labelFileName, //
//            String[] startTime, //
//            String[] stopTime, //
//            double[][] spacecraftPosition, //
//            double[][] sunVector, //
//            double[][] frustum1, //
//            double[][] frustum2, //
//            double[][] frustum3, //
//            double[][] frustum4, //
//            double[][] boresightDirection, //
//            double[][] upVector) throws IOException //
//    {
//        System.out.println(labelFileName);
//
//        // for multispectral images, the image slice being currently parsed
//        int slice = 0;
//
//        // open a file input stream
//        FileInputStream fs = new FileInputStream(labelFileName);
//        InputStreamReader isr = new InputStreamReader(fs);
//        BufferedReader in = new BufferedReader(isr);
//
//        //
//        // Parse each line of the stream and process each key-value pair,
//        // merging multiline numeric ("vector") values into a single-line
//        // string. Multi-line quoted strings are ignored.
//        //
//        boolean inStringLiteral = false;
//        boolean inVector = false;
//        List<String> vector = new ArrayList<String>();
//        String key = null;
//        String value = null;
//        String line = null;
//        while ((line = in.readLine()) != null)
//        {
//            if (line.length() == 0)
//                continue;
//
//            // for now, multi-line quoted strings are ignored (i.e. treated as comments)
//            if (line.trim().equals("\""))
//            {
//                inStringLiteral = false;
//                continue;
//            }
//
//            if (inStringLiteral)
//                continue;
//
//            // terminate a multi-line numeric value (a "vector")
//            if (line.trim().equals(")"))
//            {
//                inVector = false;
//                value = "";
//                for (String element : vector)
//                    value = value + element;
//
//                parseLabelKeyValuePair( //
//                        key, //
//                        value, //
//                        startTime, //
//                        stopTime, //
//                        spacecraftPosition[slice], //
//                        sunVector[slice], //
//                        frustum1[slice], //
//                        frustum2[slice], //
//                        frustum3[slice], //
//                        frustum4[slice], //
//                        boresightDirection[slice], //
//                        upVector[slice]);
//
//                vector.clear();
//                continue;
//            }
//
//            // add a line to the current vector
//            if (inVector)
//            {
//                vector.add(line.trim());
//                continue;
//            }
//
//            // extract key value pair
//            String tokens[] = line.split("=");
//            if (tokens.length < 2)
//                continue;
//
//            key = tokens[0].trim();
//            value = tokens[1].trim();
//
//            // detect and ignore comments
//            if (value.equals("\""))
//            {
//                inStringLiteral = true;
//                continue;
//            }
//
//            // start to accumulate numeric vector values
//            if (value.equals("("))
//            {
//                inVector = true;
//                continue;
//            }
//
//            if (value.startsWith("("))
//                value = stripBraces(value);
//            else
//                value = stripQuotes(value);
//
//            parseLabelKeyValuePair( //
//                    key, //
//                    value, //
//                    startTime, //
//                    stopTime, //
//                    spacecraftPosition[slice], //
//                    sunVector[slice], //
//                    frustum1[slice], //
//                    frustum2[slice], //
//                    frustum3[slice], //
//                    frustum4[slice], //
//                    boresightDirection[slice], //
//                    upVector[slice]);
//
//        }
//
//        in.close();
//
//        //
//        // calculate image projection from the parsed parameters
//        //
//        this.focalLengthMillimeters = getFocalLength();
//        this.npx = getNumberOfPixels();
//        this.nln = getNumberOfLines();
//        this.kmatrix00 = 1.0 / getPixelWidth();
//        this.kmatrix11 = 1.0 / getPixelHeight();
//
//        Vector3D boresightVector3D = scOrientation.applyTo(i);
//        boresightDirection[slice][0] = cz[0] = boresightVector3D.getX();
//        boresightDirection[slice][1] = cz[1] = boresightVector3D.getY();
//        boresightDirection[slice][2] = cz[2] = boresightVector3D.getZ();
//
//        Vector3D upVector3D = scOrientation.applyTo(j);
//        upVector[slice][0] = cy[0] = upVector3D.getX();
//        upVector[slice][1] = cy[1] = upVector3D.getY();
//        upVector[slice][2] = cy[2] = upVector3D.getZ();
//
//        Vector3D leftVector3D = scOrientation.applyTo(k);
//        cx[0] = -leftVector3D.getX();
//        cx[1] = -leftVector3D.getY();
//        cx[2] = -leftVector3D.getZ();
//
//        // double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
//        // double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));
//
//        // Here we calculate the image width and height using the K-matrix values.
//        // This is used only when the constructor of this function was called with
//        // loadPointingOnly set to true. When set to false, the image width and
//        // and height is set in the loadImage function (after this function is called
//        // and will overwrite these values here--though they should not be different).
//        // But when in pointing-only mode, the loadImage function is not called so
//        // we therefore set the image width and height here since some functions need
//        // it.
//        imageWidth = (int) npx;
//        imageHeight = (int) nln;
//        // if (kmatrix00 > kmatrix11)
//        // imageHeight = (int)Math.round(nln * (kmatrix00 / kmatrix11));
//        // else if (kmatrix11 > kmatrix00)
//        // imageWidth = (int)Math.round(npx * (kmatrix11 / kmatrix00));
//
//        double[] cornerVector = new double[3];
//        double fov1 = Math.atan(npx / (2.0 * focalLengthMillimeters * kmatrix00));
//        double fov2 = Math.atan(nln / (2.0 * focalLengthMillimeters * kmatrix11));
//        cornerVector[0] = -Math.tan(fov1);
//        cornerVector[1] = -Math.tan(fov2);
//        cornerVector[2] = 1.0;
//
//        double fx = cornerVector[0];
//        double fy = cornerVector[1];
//        double fz = cornerVector[2];
//        frustum3[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum3[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum3[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = -cornerVector[0];
//        fy = cornerVector[1];
//        fz = cornerVector[2];
//        frustum4[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum4[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum4[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = cornerVector[0];
//        fy = -cornerVector[1];
//        fz = cornerVector[2];
//        frustum1[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum1[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum1[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        fx = -cornerVector[0];
//        fy = -cornerVector[1];
//        fz = cornerVector[2];
//        frustum2[slice][0] = fx * cx[0] + fy * cy[0] + fz * cz[0];
//        frustum2[slice][1] = fx * cx[1] + fy * cy[1] + fz * cz[1];
//        frustum2[slice][2] = fx * cx[2] + fy * cy[2] + fz * cz[2];
//
//        MathUtil.vhat(frustum1[slice], frustum1[slice]);
//        MathUtil.vhat(frustum2[slice], frustum2[slice]);
//        MathUtil.vhat(frustum3[slice], frustum3[slice]);
//        MathUtil.vhat(frustum4[slice], frustum4[slice]);
//
//    }
//
//    private String stripQuotes(String input)
//    {
//        String result = input;
//        if (input.startsWith("\""))
//            result = result.substring(1);
//        if (input.endsWith("\""))
//            result = result.substring(0, input.length() - 2);
//        return result;
//    }
//
//    private String stripBraces(String input)
//    {
//        String result = input;
//        if (input.startsWith("("))
//            result = result.substring(1);
//        if (input.endsWith(")"))
//            result = result.substring(0, input.length() - 2);
//        return result;
//    }
//
//    private void loadSumfile() throws NumberFormatException, IOException
//    {
//        String[] start = new String[1];
//        String[] stop = new String[1];
//
//        loadSumfile( //
//                getSumfileFullPath(), //
//                start, //
//                stop, //
//                spacecraftPositionOriginal, //
//                sunPositionOriginal, //
//                frustum1Original, //
//                frustum2Original, //
//                frustum3Original, //
//                frustum4Original, //
//                boresightDirectionOriginal, //
//                upVectorOriginal);
//
//        startTime = start[0];
//        stopTime = stop[0];
//
//        // printpt(frustum1, "gas frustum1 ");
//        // printpt(frustum2, "gas frustum2 ");
//        // printpt(frustum3, "gas frustum3 ");
//        // printpt(frustum4, "gas frustum4 ");
//    }
//
//    private void loadLabelFile() throws NumberFormatException, IOException
//    {
//        System.out.println("Loading label (.lbl) file...");
//        String[] start = new String[1];
//        String[] stop = new String[1];
//
//        loadLabelFile( //
//                getLabelFileFullPath(), //
//                start, //
//                stop, //
//                spacecraftPositionOriginal, //
//                sunPositionOriginal, //
//                frustum1Original, //
//                frustum2Original, //
//                frustum3Original, //
//                frustum4Original, //
//                boresightDirectionOriginal, //
//                upVectorOriginal);
//
//        startTime = start[0];
//        stopTime = stop[0];
//
//    }
//
//    public void setLabelFileFullPath(String labelFileFullPath)
//    {
//        this.labelFileFullPath = labelFileFullPath;
//    }
//
//}
