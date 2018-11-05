package edu.jhuapl.sbmt.model.pointing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.jhuapl.saavtk.util.DateTimeUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;

public class BaseSumFileIO implements SumFileIO
{
    PerspectiveImage image;

    public BaseSumFileIO(PerspectiveImage image)
    {
        this.image = image;
    }



    /**
     * Sometimes Bob Gaskell sumfiles contain numbers of the form
     * .1192696009D+03 rather than .1192696009E+03 (i.e. a D instead
     * of an E). This function replaces D's with E's.
     * @param s
     * @return
     */
    private void replaceDwithE(String[] s)
    {
        for (int i=0; i<s.length; ++i)
            s[i] = s[i].replace('D', 'E');
    }

    private void loadSumfile(
            String sumfilename,
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
        if (sumfilename == null)
            throw new FileNotFoundException();

        FileInputStream fs = new FileInputStream(sumfilename);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader in = new BufferedReader(isr);

        // for multispectral images, the image slice being currently parsed
        int slice = 0;

        in.readLine();

        String datetime = in.readLine().trim();
        datetime = DateTimeUtil.convertDateTimeFormat(datetime);
        startTime[0] = datetime;
        stopTime[0] = datetime;

        String[] tmp = in.readLine().trim().split("\\s+");
        double npx = Integer.parseInt(tmp[0]);
        double nln = Integer.parseInt(tmp[1]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        double focalLengthMillimeters = Double.parseDouble(tmp[0]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        spacecraftPosition[slice][0] = -Double.parseDouble(tmp[0]);
        spacecraftPosition[slice][1] = -Double.parseDouble(tmp[1]);
        spacecraftPosition[slice][2] = -Double.parseDouble(tmp[2]);
        System.out.println("BaseSumFileIO: loadSumfile: set to " + spacecraftPosition[slice][0] );

        double[] cx = new double[3];
        double[] cy = new double[3];
        double[] cz = new double[3];
        double[] sz = new double[3];

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cx[0] = Double.parseDouble(tmp[0]);
        cx[1] = Double.parseDouble(tmp[1]);
        cx[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cy[0] = Double.parseDouble(tmp[0]);
        cy[1] = Double.parseDouble(tmp[1]);
        cy[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        cz[0] = Double.parseDouble(tmp[0]);
        cz[1] = Double.parseDouble(tmp[1]);
        cz[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        sz[0] = Double.parseDouble(tmp[0]);
        sz[1] = Double.parseDouble(tmp[1]);
        sz[2] = Double.parseDouble(tmp[2]);

        tmp = in.readLine().trim().split("\\s+");
        replaceDwithE(tmp);
        double kmatrix00 = Math.abs(Double.parseDouble(tmp[0]));
        double kmatrix11 = Math.abs(Double.parseDouble(tmp[4]));

        // Here we calculate the image width and height using the K-matrix values.
        // This is used only when the constructor of this function was called with
        // loadPointingOnly set to true. When set to false, the image width and
        // and height is set in the loadImage function (after this function is called
        // and will overwrite these values here--though they should not be different).
        // But when in pointing-only mode, the loadImage function is not called so
        // we therefore set the image width and height here since some functions need it.
        image.setImageWidth((int)npx);
        image.setImageHeight((int)nln);
        if (kmatrix00 > kmatrix11)
            image.setImageHeight((int)Math.round(nln * (kmatrix00 / kmatrix11)));
        else if (kmatrix11 > kmatrix00)
            image.setImageWidth((int)Math.round(npx * (kmatrix11 / kmatrix00)));

        double[] cornerVector = new double[3];
        double fov1 = Math.atan(npx/(2.0*focalLengthMillimeters*kmatrix00));
        double fov2 = Math.atan(nln/(2.0*focalLengthMillimeters*kmatrix11));
        cornerVector[0] = -Math.tan(fov1);
        cornerVector[1] = -Math.tan(fov2);
        cornerVector[2] = 1.0;

        double fx = cornerVector[0];
        double fy = cornerVector[1];
        double fz = cornerVector[2];
        frustum3[slice][0] = fx*cx[0] + fy*cy[0] + fz*cz[0];
        frustum3[slice][1] = fx*cx[1] + fy*cy[1] + fz*cz[1];
        frustum3[slice][2] = fx*cx[2] + fy*cy[2] + fz*cz[2];

        fx = -cornerVector[0];
        fy = cornerVector[1];
        fz = cornerVector[2];
        frustum4[slice][0] = fx*cx[0] + fy*cy[0] + fz*cz[0];
        frustum4[slice][1] = fx*cx[1] + fy*cy[1] + fz*cz[1];
        frustum4[slice][2] = fx*cx[2] + fy*cy[2] + fz*cz[2];

        fx = cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum1[slice][0] = fx*cx[0] + fy*cy[0] + fz*cz[0];
        frustum1[slice][1] = fx*cx[1] + fy*cy[1] + fz*cz[1];
        frustum1[slice][2] = fx*cx[2] + fy*cy[2] + fz*cz[2];

        fx = -cornerVector[0];
        fy = -cornerVector[1];
        fz = cornerVector[2];
        frustum2[slice][0] = fx*cx[0] + fy*cy[0] + fz*cz[0];
        frustum2[slice][1] = fx*cx[1] + fy*cy[1] + fz*cz[1];
        frustum2[slice][2] = fx*cx[2] + fy*cy[2] + fz*cz[2];

        MathUtil.vhat(frustum1[slice], frustum1[slice]);
        MathUtil.vhat(frustum2[slice], frustum2[slice]);
        MathUtil.vhat(frustum3[slice], frustum3[slice]);
        MathUtil.vhat(frustum4[slice], frustum4[slice]);

        MathUtil.vhat(cz, boresightDirection[slice]);
        MathUtil.vhat(cx, upVector[slice]);
        MathUtil.vhat(sz, sunVector[slice]);

        in.close();
    }

    public void loadSumfile() throws NumberFormatException, IOException
    {
        System.out.println("BaseSumFileIO: loadSumfile: loading sum file");
        String[] start = new String[1];
        String[] stop = new String[1];

        loadSumfile(
                image.getSumfileFullPath(),
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

//        System.out.println("BaseSumFileIO: loadSumfile: image sc pos " + image.getSpacecraftPositionOriginal()[0][0]);
        //        printpt(frustum1, "gas frustum1 ");
        //        printpt(frustum2, "gas frustum2 ");
        //        printpt(frustum3, "gas frustum3 ");
        //        printpt(frustum4, "gas frustum4 ");
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
    public String initLocalSumfileFullPath()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
