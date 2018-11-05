package edu.jhuapl.sbmt.model.pointing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class BaseLabelFileIO implements LabelFileIO
{
    private PerspectiveImage image;
    private static final Vector3D i = new Vector3D(1.0, 0.0, 0.0);
    private static final Vector3D j = new Vector3D(0.0, 1.0, 0.0);
    private static final Vector3D k = new Vector3D(0.0, 0.0, 1.0);

    public BaseLabelFileIO()
    {
        // TODO Auto-generated constructor stub
    }

    public void loadLabelFile() throws NumberFormatException, IOException
    {
        System.out.println("Loading label (.lbl) file...");
        String[] start = new String[1];
        String[] stop = new String[1];

        loadLabelFile(
                image.getLabelFileFullPath(),
                start,
                stop,
                image.getSpacecraftPositionOriginal(),
                image.getSunPositionOriginal(),
                image.getFrustum1Original(),
                image.getFrustum2Original(),
                image.getFrustum3Original(),
                image.getFrustum4Original(),
                image.getBoresightDirectionOriginal(),
                image.getUpVectorOriginal());

        image.setStartTime(start[0]);
        image.setStopTime(stop[0]);

    }

    //
    // Label (.lbl) file parsing methods
    //

    protected void loadLabelFile(
            String labelFileName,
            String[] startTime,
            String[] stopTime,
            double[][] spacecraftPosition,
            double[][] sunVector,
            double[][] frustum1,
            double[][] frustum2,
            double[][] frustum3,
            double[][] frustum4,
            double[][] boresightDirection,
            double[][] upVector) throws IOException
    {
        System.out.println(labelFileName);

        // for multispectral images, the image slice being currently parsed
        int slice = 0;

        // open a file input stream
        FileInputStream fs = new FileInputStream(labelFileName);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        //
        // Parse each line of the stream and process each key-value pair,
        // merging multiline numeric ("vector") values into a single-line
        // string. Multi-line quoted strings are ignored.
        //
        boolean inStringLiteral = false;
        boolean inVector = false;
        List<String> vector = new ArrayList<String>();
        String key = null;
        String value = null;
        String line = null;
        while ((line = in.readLine()) != null)
        {
            if (line.length() == 0)
                continue;

            // for now, multi-line quoted strings are ignored (i.e. treated as comments)
            if (line.trim().equals("\""))
            {
                inStringLiteral = false;
                continue;
            }

            if (inStringLiteral)
                continue;

            // terminate a multi-line numeric value (a "vector")
            if (line.trim().equals(")"))
            {
                inVector = false;
                value = "";
                for (String element : vector)
                    value = value + element;

                parseLabelKeyValuePair(
                        key,
                        value,
                        startTime,
                        stopTime,
                        spacecraftPosition[slice],
                        sunVector[slice],
                        frustum1[slice],
                        frustum2[slice],
                        frustum3[slice],
                        frustum4[slice],
                        boresightDirection[slice],
                        upVector[slice]);

                vector.clear();
                continue;
            }

            // add a line to the current vector
            if (inVector)
            {
                vector.add(line.trim());
                continue;
            }

            // extract key value pair
            String tokens[] = line.split("=");
            if (tokens.length < 2)
                continue;

            key = tokens[0].trim();
            value = tokens[1].trim();

            // detect and ignore comments
            if (value.equals("\""))
            {
                inStringLiteral = true;
                continue;
            }

            // start to accumulate numeric vector values
            if (value.equals("("))
            {
                inVector = true;
                continue;
            }

            if (value.startsWith("("))
                value = stripBraces(value);
            else
                value = stripQuotes(value);

            parseLabelKeyValuePair(
                    key,
                    value,
                    startTime,
                    stopTime,
                    spacecraftPosition[slice],
                    sunVector[slice],
                    frustum1[slice],
                    frustum2[slice],
                    frustum3[slice],
                    frustum4[slice],
                    boresightDirection[slice],
                    upVector[slice]);

        }

        in.close();

        //
        // calculate image projection from the parsed parameters
        //
        image.setFocalLengthMillimeters(image.getFocalLength());
        image.setNpx(image.getNumberOfPixels());
        image.setNln(image.getNumberOfLines());
        image.setKmatrix00(1.0 / image.getPixelWidth());
        image.setKmatrix11(1.0 / image.getPixelHeight());

        Vector3D boresightVector3D = image.getScOrientation().applyTo(i);
        boresightDirection[slice][0] = image.getCz()[0] = boresightVector3D.getX();
        boresightDirection[slice][1] = image.getCz()[1] = boresightVector3D.getY();
        boresightDirection[slice][2] = image.getCz()[2] = boresightVector3D.getZ();

        Vector3D upVector3D = image.getScOrientation().applyTo(j);
        upVector[slice][0] = image.getCy()[0] = upVector3D.getX();
        upVector[slice][1] = image.getCy()[1] = upVector3D.getY();
        upVector[slice][2] = image.getCy()[2] = upVector3D.getZ();

        Vector3D leftVector3D = image.getScOrientation().applyTo(k);
        image.getCx()[0] = -leftVector3D.getX();
        image.getCx()[1] = -leftVector3D.getY();
        image.getCx()[2] = -leftVector3D.getZ();

        //      double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
        //      double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));

        // Here we calculate the image width and height using the K-matrix values.
        // This is used only when the constructor of this function was called with
        // loadPointingOnly set to true. When set to false, the image width and
        // and height is set in the loadImage function (after this function is called
        // and will overwrite these values here--though they should not be different).
        // But when in pointing-only mode, the loadImage function is not called so
        // we therefore set the image width and height here since some functions need it.
        image.setImageWidth((int)image.getNpx());
        image.setImageHeight((int)image.getNln());
        //      if (kmatrix00 > kmatrix11)
        //          imageHeight = (int)Math.round(nln * (kmatrix00 / kmatrix11));
        //      else if (kmatrix11 > kmatrix00)
        //          imageWidth = (int)Math.round(npx * (kmatrix11 / kmatrix00));

        double[] cornerVector = new double[3];
        double fov1 = Math.atan(image.getNpx()/(2.0*image.getFocalLengthMillimeters()*image.getKmatrix00()));
        double fov2 = Math.atan(image.getNln()/(2.0*image.getFocalLengthMillimeters()*image.getKmatrix11()));
        cornerVector[0] = -Math.tan(fov1);
        cornerVector[1] = -Math.tan(fov2);
        cornerVector[2] = 1.0;

        double fx = cornerVector[0];
        double fy = cornerVector[1];
        double fz = cornerVector[2];
        frustum3[slice][0] = fx*image.getCx()[0] + fy*image.getCy()[0] + fz*image.getCz()[0];
        frustum3[slice][1] = fx*image.getCx()[1] + fy*image.getCy()[1] + fz*image.getCz()[1];
        frustum3[slice][2] = fx*image.getCx()[2] + fy*image.getCy()[2] + fz*image.getCz()[2];

        fx = -cornerVector[0];
        fy = cornerVector[1];
        fz = cornerVector[2];
        frustum4[slice][0] = fx*image.getCx()[0] + fy*image.getCy()[0] + fz*image.getCz()[0];
        frustum4[slice][1] = fx*image.getCx()[1] + fy*image.getCy()[1] + fz*image.getCz()[1];
        frustum4[slice][2] = fx*image.getCx()[2] + fy*image.getCy()[2] + fz*image.getCz()[2];

        fx = cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum1[slice][0] = fx*image.getCx()[0] + fy*image.getCy()[0] + fz*image.getCz()[0];
        frustum1[slice][1] = fx*image.getCx()[1] + fy*image.getCy()[1] + fz*image.getCz()[1];
        frustum1[slice][2] = fx*image.getCx()[2] + fy*image.getCy()[2] + fz*image.getCz()[2];

        fx = -cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum2[slice][0] = fx*image.getCx()[0] + fy*image.getCy()[0] + fz*image.getCz()[0];
        frustum2[slice][1] = fx*image.getCx()[1] + fy*image.getCy()[1] + fz*image.getCz()[1];
        frustum2[slice][2] = fx*image.getCx()[2] + fy*image.getCy()[2] + fz*image.getCz()[2];

        MathUtil.vhat(frustum1[slice], frustum1[slice]);
        MathUtil.vhat(frustum2[slice], frustum2[slice]);
        MathUtil.vhat(frustum3[slice], frustum3[slice]);
        MathUtil.vhat(frustum4[slice], frustum4[slice]);


    }

  //*********
    // Labels
    //*********
    private void parseLabelKeyValuePair(
            String key,
            String value,
            String[] startTime,
            String[] stopTime,
            double[] spacecraftPosition,
            double[] sunVector,
            double[] frustum1,
            double[] frustum2,
            double[] frustum3,
            double[] frustum4,
            double[] boresightDirection,
            double[] upVector) throws IOException
    {


        System.out.println("Label file key: " + key + " = " + value);

        if (key.equals("TARGET_NAME"))
            image.setTargetName(value);
        else if (key.equals("INSTRUMENT_ID"))
            image.setInstrumentId(value);
        else if (key.equals("FILTER_NAME"))
            image.setFilterName(value);
        else if (key.equals("OBJECT"))
            image.setObjectName(value);
        else if (key.equals("LINE_SAMPLES"))
        {
            if (image.getObjectName().equals("EXTENSION_CALGEOM_IMAGE"))
                image.setNumberOfPixels(Double.parseDouble(value));
        }
        else if (key.equals("LINES"))
        {
            if (image.getObjectName().equals("EXTENSION_CALGEOM_IMAGE"))
                image.setNumberOfLines(Double.parseDouble(value));
        }
        else if (key.equals("START_TIME"))
        {
            image.setStartTimeString(value);
            startTime[0] = image.getStartTimeString();
        }
        else if (key.equals("STOP_TIME"))
        {
            image.setStopTimeString(value);
            stopTime[0] = image.getStopTimeString();
        }
        else if (key.equals("SC_TARGET_POSITION_VECTOR"))
        {
            image.setScTargetPositionString(value);
            String p[] = image.getScTargetPositionString().split(",");
            spacecraftPosition[0] = Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
            spacecraftPosition[1] = Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
            spacecraftPosition[2] = Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
        }
        else if (key.equals("TARGET_SUN_POSITION_VECTOR"))
        {
            image.setTargetSunPositionString(value);
            String p[] = image.getTargetSunPositionString().split(",");
            sunVector[0] = -Double.parseDouble(p[0].trim().split("\\s+")[0].trim());
            sunVector[1] = -Double.parseDouble(p[1].trim().split("\\s+")[0].trim());
            sunVector[2] = -Double.parseDouble(p[2].trim().split("\\s+")[0].trim());
        }
        else if (key.equals("QUATERNION"))
        {
            image.setScOrientationString(value);
            String qstr[] = image.getScOrientationString().split(",");
            image.getQ()[0] = Double.parseDouble(qstr[0].trim().split("\\s+")[0].trim());
            image.getQ()[1] = Double.parseDouble(qstr[1].trim().split("\\s+")[0].trim());
            image.getQ()[2] = Double.parseDouble(qstr[2].trim().split("\\s+")[0].trim());
            image.getQ()[3] = Double.parseDouble(qstr[3].trim().split("\\s+")[0].trim());
            image.setScOrientation(new Rotation(image.getQ()[0], image.getQ()[1], image.getQ()[2], image.getQ()[3], false));
        }

    }

    private static String stripQuotes(String input)
    {
        String result = input;
        if (input.startsWith("\""))
            result = result.substring(1);
        if (input.endsWith("\""))
            result = result.substring(0, input.length()-2);
        return result;
    }

    private static String stripBraces(String input)
    {
        String result = input;
        if (input.startsWith("("))
            result = result.substring(1);
        if (input.endsWith(")"))
            result = result.substring(0, input.length()-2);
        return result;
    }


    public PerspectiveImage getImage()
    {
        return image;
    }

    public void setImage(PerspectiveImage image)
    {
        this.image = image;
    }

    @Override
    public String initLocalLabelFileFullPath()
    {
        return "";
    }


}
