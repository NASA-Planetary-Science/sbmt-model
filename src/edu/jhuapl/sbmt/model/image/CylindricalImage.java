package edu.jhuapl.sbmt.model.image;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import vtk.vtkActor;
import vtk.vtkAlgorithmOutput;
import vtk.vtkAppendPolyData;
import vtk.vtkClipPolyData;
import vtk.vtkCone;
import vtk.vtkFloatArray;
import vtk.vtkImageData;
import vtk.vtkImageMapToColors;
import vtk.vtkLookupTable;
import vtk.vtkPNGReader;
import vtk.vtkPlane;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkProperty;
import vtk.vtkTexture;
import vtk.vtkTransform;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.IntensityRange;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MapUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.util.VtkENVIReader;

public class CylindricalImage extends Image
{
    public static final String LOWER_LEFT_LATITUDES = "LLLat";
    public static final String LOWER_LEFT_LONGITUDES = "LLLon";
    public static final String UPPER_RIGHT_LATITUDES = "URLat";
    public static final String UPPER_RIGHT_LONGITUDES = "URLon";

    private vtkPolyData imagePolyData;
    private vtkPolyData shiftedImagePolyData;
    private vtkActor smallBodyActor;
    private vtkPolyDataMapper smallBodyMapper;
    private List<vtkProp> smallBodyActors = new ArrayList<vtkProp>();
    private double imageOpacity = 1.0;
    private SmallBodyModel smallBodyModel;
    private vtkTexture imageTexture;
    private boolean initialized = false;
    private double offset;
    private double lowerLeftLat = -90.0;
    private double lowerLeftLon = 0.0;
    private double upperRightLat = 90.0;
    private double upperRightLon = 360.0;
    private vtkImageData rawImage;
    private vtkImageData displayedImage;
    private float minValue;
    private float maxValue;
    private IntensityRange displayedRange = new IntensityRange(1,0);
    private String imageName = "";


    /**
     * For a cylindrical image, the name field of key must be as follows depending on key.source:
     * If key.source is IMAGE_MAP, then key.name is the path on the server to
     * the image.
     * If key.source is LOCAL_CYLINDRICAL, then key.name is the FULL PATH to the image file
     * on disk. For example:
     * /Users/joe/.neartool/custom-data-for-built-in-models/Gaskell/Eros/image-92197644-7bd9-4a4e-92c8-b7c193ed6906.png
     *
     * @param key
     * @param smallBodyModel
     */
    public CylindricalImage(
            ImageKey key,
            SmallBodyModel smallBodyModel)
    {
        super(key);
        this.smallBodyModel = smallBodyModel;

        imagePolyData = new vtkPolyData();
        shiftedImagePolyData = new vtkPolyData();

        this.offset = getDefaultOffset();

        loadImageInfoFromConfigFile();

        // If we're an IMAGE_MAP then the image name is the same as
        // as the key name.
        if (getKey().source.equals(ImageSource.IMAGE_MAP))
        {
            imageName = getKey().name;
        }
}

    private void loadImageInfoFromConfigFile()
    {
        if (getKey().source.equals(ImageSource.LOCAL_CYLINDRICAL))
        {
            // Look in the config file and figure out which index this image
            // corresponds to. The config file is located in the same folder
            // as the image file
            String configFilename = new File(getKey().name).getParent() + File.separator + "config.txt";
            MapUtil configMap = new MapUtil(configFilename);
            String[] imageFilenames = configMap.getAsArray(IMAGE_FILENAMES);
            for (int i=0; i<imageFilenames.length; ++i)
            {
                String filename = new File(getKey().name).getName();
                if (filename.equals(imageFilenames[i]))
                {
                    imageName = configMap.getAsArray(Image.IMAGE_NAMES)[i];
                    lowerLeftLat = configMap.getAsDoubleArray(LOWER_LEFT_LATITUDES)[i];
                    lowerLeftLon = configMap.getAsDoubleArray(LOWER_LEFT_LONGITUDES)[i];
                    upperRightLat = configMap.getAsDoubleArray(UPPER_RIGHT_LATITUDES)[i];
                    upperRightLon = configMap.getAsDoubleArray(UPPER_RIGHT_LONGITUDES)[i];
                    break;
                }
            }
        }
    }

    /**
     * Clip out the area for the texture using a series of clipping planes
     * for the longitudes and generate texture coordinates (This works for
     * both ellipsoidal and non-ellipsoidal bodies). Unfortunately, this
     * function is very complicated.
     * @param smallBodyPolyData
     * @return the clipped out polydata
     */
    private vtkPolyData clipOutTextureLongitudeAndGenerateTextureCoordinates(vtkPolyData smallBodyPolyData)
    {
        // Divide along the zero longitude line and compute
        // texture coordinates for each half separately, then combine the two.
        // This avoids having a "seam" at the zero longitude line.
        final double[] origin = {0.0, 0.0, 0.0};
        final double[] zaxis = {0.0, 0.0, 1.0};

        double lllon = lowerLeftLon;
        double urlon = upperRightLon;

        // Make sure longitudes are in the interval [0, 360) by converting to
        // rectangular and back to longitude (which puts longitude in interval
        // [-180, 180]) and then adding 360 if longitude is less than zero.
        lllon = MathUtil.reclat(MathUtil.latrec(new LatLon(0.0, lllon*Math.PI/180.0, 1.0))).lon*180.0/Math.PI;
        urlon = MathUtil.reclat(MathUtil.latrec(new LatLon(0.0, urlon*Math.PI/180.0, 1.0))).lon*180.0/Math.PI;
        if (lllon < 0.0)
            lllon += 360.0;
        if (urlon < 0.0)
            urlon += 360.0;
        if (lllon >= 360.0)
            lllon = 0.0;
        if (urlon >= 360.0)
            urlon = 0.0;

        vtkPlane planeZeroLon = new vtkPlane();
        {
            double[] vec = MathUtil.latrec(new LatLon(0.0, 0.0, 1.0));
            double[] normal = new double[3];
            MathUtil.vcrss(vec, zaxis, normal);
            planeZeroLon.SetOrigin(origin);
            planeZeroLon.SetNormal(normal);
        }

        // First do the hemisphere from longitude 0 to 180.
        boolean needToGenerateTextures0To180 = true;
        vtkClipPolyData clipPolyData1 = new vtkClipPolyData();
        clipPolyData1.SetClipFunction(planeZeroLon);
        clipPolyData1.SetInputData(smallBodyPolyData);
        clipPolyData1.SetInsideOut(1);
        clipPolyData1.Update();
        vtkPolyData clipPolyData1Output = clipPolyData1.GetOutput();
        vtkPolyData clipPolyData1Hemi = clipPolyData1Output;
        if (lllon > 0.0 && lllon < 180.0)
        {
            double[] vec = MathUtil.latrec(new LatLon(0.0, lllon*Math.PI/180.0, 1.0));
            double[] normal = new double[3];
            MathUtil.vcrss(vec, zaxis, normal);
            vtkPlane plane1 = new vtkPlane();
            plane1.SetOrigin(origin);
            plane1.SetNormal(normal);

            clipPolyData1 = new vtkClipPolyData();
            clipPolyData1.SetClipFunction(plane1);
            clipPolyData1.SetInputData(clipPolyData1Output);
            clipPolyData1.SetInsideOut(1);
            clipPolyData1.Update();
            clipPolyData1Output = clipPolyData1.GetOutput();
        }
        if (urlon > 0.0 && urlon < 180.0)
        {
            double[] vec = MathUtil.latrec(new LatLon(0.0, urlon*Math.PI/180.0, 1.0));
            double[] normal = new double[3];
            MathUtil.vcrss(vec, zaxis, normal);
            vtkPlane plane1 = new vtkPlane();
            plane1.SetOrigin(origin);
            plane1.SetNormal(normal);

            // If the following condition is true, that means there are 2 disjoint pieces in the
            // hemisphere, so we'll need to append the two together.
            if (lllon > 0.0 && lllon < 180.0 && urlon <= lllon)
            {
                clipPolyData1 = new vtkClipPolyData();
                clipPolyData1.SetClipFunction(plane1);
                clipPolyData1.SetInputData(clipPolyData1Hemi);
                clipPolyData1.Update();
                vtkPolyData clipOutput = clipPolyData1.GetOutput();

                generateTextureCoordinates(clipPolyData1Output, true, false);
                generateTextureCoordinates(clipOutput, false, true);
                needToGenerateTextures0To180 = false;

                vtkAppendPolyData appendFilter = new vtkAppendPolyData();
                appendFilter.UserManagedInputsOff();
                appendFilter.AddInputData(clipPolyData1Output);
                appendFilter.AddInputData(clipOutput);
                appendFilter.Update();
                clipPolyData1Output = appendFilter.GetOutput();
            }
            else
            {
                clipPolyData1 = new vtkClipPolyData();
                clipPolyData1.SetClipFunction(plane1);
                clipPolyData1.SetInputData(clipPolyData1Output);
                clipPolyData1.Update();
                clipPolyData1Output = clipPolyData1.GetOutput();
            }
        }


        // Next do the hemisphere from longitude 180 to 360.
        boolean needToGenerateTextures180To0 = true;
        vtkClipPolyData clipPolyData2 = new vtkClipPolyData();
        clipPolyData2.SetClipFunction(planeZeroLon);
        clipPolyData2.SetInputData(smallBodyPolyData);
        clipPolyData2.Update();
        vtkPolyData clipPolyData2Output = clipPolyData2.GetOutput();
        vtkPolyData clipPolyData2Hemi = clipPolyData2Output;
        if (lllon > 180.0 && lllon < 360.0)
        {
            double[] vec = MathUtil.latrec(new LatLon(0.0, lllon*Math.PI/180.0, 1.0));
            double[] normal = new double[3];
            MathUtil.vcrss(vec, zaxis, normal);
            vtkPlane plane2 = new vtkPlane();
            plane2.SetOrigin(origin);
            plane2.SetNormal(normal);

            clipPolyData2 = new vtkClipPolyData();
            clipPolyData2.SetClipFunction(plane2);
            clipPolyData2.SetInputData(clipPolyData2Output);
            clipPolyData2.SetInsideOut(1);
            clipPolyData2.Update();
            clipPolyData2Output = clipPolyData2.GetOutput();
        }
        if (urlon > 180.0 && urlon < 360.0)
        {
            double[] vec = MathUtil.latrec(new LatLon(0.0, urlon*Math.PI/180.0, 1.0));
            double[] normal = new double[3];
            MathUtil.vcrss(vec, zaxis, normal);
            vtkPlane plane2 = new vtkPlane();
            plane2.SetOrigin(origin);
            plane2.SetNormal(normal);

            // If the following condition is true, that means there are 2 disjoint pieces in the
            // hemisphere, so we'll need to append the two together.
            if (lllon > 180.0 && lllon < 360.0 && urlon <= lllon)
            {
                clipPolyData2 = new vtkClipPolyData();
                clipPolyData2.SetClipFunction(plane2);
                clipPolyData2.SetInputData(clipPolyData2Hemi);
                clipPolyData2.Update();
                vtkPolyData clipOutput = clipPolyData2.GetOutput();

                generateTextureCoordinates(clipPolyData2Output, true, false);
                generateTextureCoordinates(clipOutput, false, true);
                needToGenerateTextures180To0 = false;

                vtkAppendPolyData appendFilter = new vtkAppendPolyData();
                appendFilter.UserManagedInputsOff();
                appendFilter.AddInputData(clipPolyData2Output);
                appendFilter.AddInputData(clipOutput);
                appendFilter.Update();
                clipPolyData2Output = appendFilter.GetOutput();
            }
            else
            {
                clipPolyData2 = new vtkClipPolyData();
                clipPolyData2.SetClipFunction(plane2);
                clipPolyData2.SetInputData(clipPolyData2Output);
                clipPolyData2.Update();
                clipPolyData2Output = clipPolyData2.GetOutput();
            }
        }

        vtkAppendPolyData appendFilter = new vtkAppendPolyData();
        appendFilter.UserManagedInputsOff();
        // We may not need to include both hemispheres. Test to see
        // if the texture is contained in each hemisphere.
        if (doLongitudeIntervalsIntersect(0.0, 180.0, lllon, urlon))
        {
            if (needToGenerateTextures0To180)
            {
                boolean isOnLeftSide = false;
                boolean isOnRightSide = false;
                if (lllon >= 0.0 && lllon < 180.0)
                    isOnLeftSide = true;
                if (urlon > 0.0 && urlon <= 180.0)
                    isOnRightSide = true;
                generateTextureCoordinates(clipPolyData1Output, isOnLeftSide, isOnRightSide);
            }
            appendFilter.AddInputData(clipPolyData1Output);
        }
        if (doLongitudeIntervalsIntersect(180.0, 0.0, lllon, urlon))
        {
            if (needToGenerateTextures180To0)
            {
                boolean isOnLeftSide = false;
                boolean isOnRightSide = false;
                if (lllon >= 180.0)
                    isOnLeftSide = true;
                if (urlon > 180.0 || urlon == 0.0)
                    isOnRightSide = true;
                generateTextureCoordinates(clipPolyData2Output, isOnLeftSide, isOnRightSide);
            }
            appendFilter.AddInputData(clipPolyData2Output);
        }
        appendFilter.Update();
        smallBodyPolyData = appendFilter.GetOutput();

        return smallBodyPolyData;
    }

    /**
     * Clip out the texture area for latitudes. This is optimized for ellipsoids
     * and only uses planes (not cones).
     * @param smallBodyPolyData
     * @return
     */
    private vtkPolyData clipOutTextureLatitudeEllipsoid(vtkPolyData smallBodyPolyData)
    {
        double[] zaxis = {0.0, 0.0, 1.0};
        double lllat = lowerLeftLat * (Math.PI / 180.0);
        double urlat = upperRightLat * (Math.PI / 180.0);

        double[] intersectPoint = new double[3];
        smallBodyModel.getPointAndCellIdFromLatLon(lllat, 0.0, intersectPoint);
        double[] vec = new double[]{0.0, 0.0, intersectPoint[2]};


        vtkPlane plane3 = new vtkPlane();
        plane3.SetOrigin(vec);
        plane3.SetNormal(zaxis);

        vtkClipPolyData clipPolyData2 = new vtkClipPolyData();
        clipPolyData2.SetClipFunction(plane3);
        clipPolyData2.SetInputData(smallBodyPolyData);
        clipPolyData2.Update();
        vtkAlgorithmOutput clipPolyData2Output = clipPolyData2.GetOutputPort();


        smallBodyModel.getPointAndCellIdFromLatLon(urlat, 0.0, intersectPoint);
        vec = new double[]{0.0, 0.0, intersectPoint[2]};

        vtkPlane plane4 = new vtkPlane();
        plane4.SetOrigin(vec);
        plane4.SetNormal(zaxis);

        vtkClipPolyData clipPolyData3 = new vtkClipPolyData();
        clipPolyData3.SetClipFunction(plane4);
        clipPolyData3.SetInputConnection(clipPolyData2Output);
        clipPolyData3.SetInsideOut(1);
        clipPolyData3.Update();

        smallBodyPolyData = clipPolyData3.GetOutput();

        return smallBodyPolyData;
    }

    /**
     * Clip out texture area for latitudes. This works for any general shape model,
     * even non-ellipsoids, and uses cones to clip out lines of latitude.
     * @param smallBodyPolyData
     * @return
     */
    private vtkPolyData clipOutTextureLatitudeGeneral(vtkPolyData smallBodyPolyData)
    {
        double[] origin = {0.0, 0.0, 0.0};
        double[] zaxis = {0.0, 0.0, 1.0};
        double lllat = lowerLeftLat * (Math.PI / 180.0);
        double urlat = upperRightLat * (Math.PI / 180.0);

        // For clipping latitude, first split the shape model in half, do the clipping
        // on each half, and then combine the 2 halves.
        vtkPlane planeZeroLat = new vtkPlane();
        planeZeroLat.SetOrigin(origin);
        planeZeroLat.SetNormal(zaxis);

        double[] yaxis = {0.0, 1.0, 0.0};
        vtkTransform transform = new vtkTransform();
        transform.Identity();
        transform.RotateWXYZ(90.0, yaxis);

        // Do northern hemisphere first
        vtkClipPolyData clipPolyDataNorth = new vtkClipPolyData();
        clipPolyDataNorth.SetClipFunction(planeZeroLat);
        clipPolyDataNorth.SetInputData(smallBodyPolyData);
        clipPolyDataNorth.Update();
        vtkAlgorithmOutput clipNorthOutput = clipPolyDataNorth.GetOutputPort();
        if (lllat > 0.0)
        {
            vtkCone cone = new vtkCone();
            cone.SetTransform(transform);
            cone.SetAngle(90.0 - lowerLeftLat);

            clipPolyDataNorth = new vtkClipPolyData();
            clipPolyDataNorth.SetClipFunction(cone);
            clipPolyDataNorth.SetInputConnection(clipNorthOutput);
            clipPolyDataNorth.SetInsideOut(1);
            clipPolyDataNorth.Update();
            clipNorthOutput = clipPolyDataNorth.GetOutputPort();
        }
        if (urlat > 0.0)
        {
            vtkCone cone = new vtkCone();
            cone.SetTransform(transform);
            cone.SetAngle(90.0 - upperRightLat);

            clipPolyDataNorth = new vtkClipPolyData();
            clipPolyDataNorth.SetClipFunction(cone);
            clipPolyDataNorth.SetInputConnection(clipNorthOutput);
            clipPolyDataNorth.Update();
            clipNorthOutput = clipPolyDataNorth.GetOutputPort();
        }

        // Now do southern hemisphere
        vtkClipPolyData clipPolyDataSouth = new vtkClipPolyData();
        clipPolyDataSouth.SetClipFunction(planeZeroLat);
        clipPolyDataSouth.SetInputData(smallBodyPolyData);
        clipPolyDataSouth.SetInsideOut(1);
        clipPolyDataSouth.Update();
        vtkAlgorithmOutput clipSouthOutput = clipPolyDataSouth.GetOutputPort();
        if (lllat < 0.0)
        {
            vtkCone cone = new vtkCone();
            cone.SetTransform(transform);
            cone.SetAngle(90.0 + lowerLeftLat);

            clipPolyDataSouth = new vtkClipPolyData();
            clipPolyDataSouth.SetClipFunction(cone);
            clipPolyDataSouth.SetInputConnection(clipSouthOutput);
            clipPolyDataSouth.Update();
            clipSouthOutput = clipPolyDataSouth.GetOutputPort();
        }
        if (urlat < 0.0)
        {
            vtkCone cone = new vtkCone();
            cone.SetTransform(transform);
            cone.SetAngle(90.0 + upperRightLat);

            clipPolyDataSouth = new vtkClipPolyData();
            clipPolyDataSouth.SetClipFunction(cone);
            clipPolyDataSouth.SetInputConnection(clipSouthOutput);
            clipPolyDataSouth.SetInsideOut(1);
            clipPolyDataSouth.Update();
            clipSouthOutput = clipPolyDataSouth.GetOutputPort();
        }


        vtkAppendPolyData appendFilter = new vtkAppendPolyData();
        if (urlat > 0.0)
            appendFilter.AddInputConnection(clipNorthOutput);
        if (lllat < 0.0)
            appendFilter.AddInputConnection(clipSouthOutput);

        appendFilter.Update();
        smallBodyPolyData = appendFilter.GetOutput();

        return smallBodyPolyData;
    }

    private void initialize()
    {
        if (initialized)
            return;

        vtkPolyData smallBodyPolyData = smallBodyModel.getSmallBodyPolyData();

        double lllat = lowerLeftLat;
        double lllon = lowerLeftLon;
        double urlat = upperRightLat;
        double urlon = upperRightLon;

        // If the texture does not cover the entire model, then clip out the part
        // that it does cover.
        if (lllat != -90.0 || lllon != 0.0 || urlat != 90.0 || urlon != 360.0)
        {
            if (smallBodyModel.isEllipsoid())
                smallBodyPolyData = clipOutTextureLatitudeEllipsoid(smallBodyPolyData);
            else
                smallBodyPolyData = clipOutTextureLatitudeGeneral(smallBodyPolyData);
        }

        smallBodyPolyData = clipOutTextureLongitudeAndGenerateTextureCoordinates(smallBodyPolyData);

        // Need to clear out scalar data since if coloring data is being shown,
        // then the color might mix-in with the image.
        smallBodyPolyData.GetCellData().SetScalars(null);
        smallBodyPolyData.GetPointData().SetScalars(null);

        imagePolyData.DeepCopy(smallBodyPolyData);

        shiftedImagePolyData.DeepCopy(imagePolyData);
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedImagePolyData, offset);

        initialized = true;
    }

    /**
     * Generates the cylindrical projection texture coordinates for the polydata.
     * If isOnLeftSide is true, that means the polydata borders the left side (the side of lllon) of the image.
     * If isOnRightSide is true, that means the polydata borders the right side (the side of urlon) of the image.
     * @param polydata
     * @param isOnLeftSide
     * @param isOnRightSide
     */
    protected void generateTextureCoordinates(
            vtkPolyData polydata,
            boolean isOnLeftSide,
            boolean isOnRightSide)
    {
        double lllat = lowerLeftLat * (Math.PI / 180.0);
        double lllon = lowerLeftLon * (Math.PI / 180.0);
        double urlat = upperRightLat * (Math.PI / 180.0);
        double urlon = upperRightLon * (Math.PI / 180.0);

        // Make sure longitudes are in the interval [0, 2*PI) by converting to
        // rectangular and back to longitude (which puts longitude in interval
        // [-PI, PI]) and then adding 2*PI if longitude is less than zero.
        lllon = MathUtil.reclat(MathUtil.latrec(new LatLon(0.0, lllon, 1.0))).lon;
        urlon = MathUtil.reclat(MathUtil.latrec(new LatLon(0.0, urlon, 1.0))).lon;
        if (lllon < 0.0)
            lllon += 2.0*Math.PI;
        if (urlon < 0.0)
            urlon += 2.0*Math.PI;
        if (lllon >= 2.0*Math.PI)
            lllon = 0.0;
        if (urlon >= 2.0*Math.PI)
            urlon = 0.0;

        vtkFloatArray textureCoords = new vtkFloatArray();

        int numberOfPoints = polydata.GetNumberOfPoints();

        textureCoords.SetNumberOfComponents(2);
        textureCoords.SetNumberOfTuples(numberOfPoints);

        vtkPoints points = polydata.GetPoints();

        double xsize = getDistanceBetweenLongitudes(lllon, urlon);
        // If lower left and upper right longitudes are the same, that
        // means the image extends 360 degrees around the shape model.
        if (xsize == 0.0)
            xsize = 2.0*Math.PI;
        double ysize = urlat - lllat;

        for (int i=0; i<numberOfPoints; ++i)
        {
            double[] pt = points.GetPoint(i);

            LatLon ll = MathUtil.reclat(pt);

            if (ll.lon < 0.0)
               ll.lon += (2.0 * Math.PI);
            if (ll.lon >= 2.0 * Math.PI)
                ll.lon = 0.0;

            double dist = getDistanceBetweenLongitudes(lllon, ll.lon);
            if (isOnLeftSide)
            {
                if (Math.abs(2.0*Math.PI - dist) < 1.0e-2)
                    dist = 0.0;
            }
            else if (isOnRightSide)
            {
                if (Math.abs(dist) < 1.0e-2)
                    dist = xsize;
            }

            double u = dist / xsize;
            double v = (ll.lat - lllat) / ysize;

            if (u < 0.0) u = 0.0;
            else if (u > 1.0) u = 1.0;
            if (v < 0.0) v = 0.0;
            else if (v > 1.0) v = 1.0;

            textureCoords.SetTuple2(i, u, v);
        }

        polydata.GetPointData().SetTCoords(textureCoords);
    }

    /**
     * Assuming leftLon and rightLon are within interval [0, 2*PI], return
     * the distance between the two assuming leftLon is at a lower lon
     * than rightLon. Thus the returned result is always positive within
     * interval [0, 2*PI].
     * @param leftLon
     * @param rightLon
     * @return distance in radians
     */
    private double getDistanceBetweenLongitudes(double leftLon, double rightLon)
    {
        double dist = rightLon - leftLon;
        if (dist >= 0.0)
            return dist;
        else
            return dist + 2.0 * Math.PI;
    }

    /**
     * Same as previous but returns distance in degrees
     */
    private double getDistanceBetweenLongitudesDegrees(double leftLon, double rightLon)
    {
        double dist = rightLon - leftLon;
        if (dist >= 0.0)
            return dist;
        else
            return dist + 360.0;
    }

    /**
     * Returns if the two longitudinal intervals intersect at all. If they intersect at
     * a point, (e.g. one interval goes from 1 to 2 and the second goes from 2 to 3), false
     * is returned.
     * @param lower1
     * @param upper1
     * @param lower2
     * @param upper2
     * @return
     */
    private boolean doLongitudeIntervalsIntersect(double lower1, double upper1, double lower2, double upper2)
    {
        if (lower1 == lower2 || upper1 == upper2 || lower1 == upper1 || lower2 == upper2)
            return true;

        // First test if lower2 or upper2 is contained in the first interval
        double dist1 = getDistanceBetweenLongitudesDegrees(lower1, upper1);
        double d = getDistanceBetweenLongitudesDegrees(lower1, lower2);
        if (d > 0.0 && d < dist1)
            return true;
        d = getDistanceBetweenLongitudesDegrees(lower1, upper2);
        if (d > 0.0 && d < dist1)
            return true;

        // Then test if lower1 or upper1 is contained in the second interval
        double dist2 = getDistanceBetweenLongitudesDegrees(lower2, upper2);
        d = getDistanceBetweenLongitudesDegrees(lower2, lower1);
        if (d > 0.0 && d < dist2)
            return true;
        d = getDistanceBetweenLongitudesDegrees(lower2, upper1);
        if (d > 0.0 && d < dist2)
            return true;

        return false;
    }

    public List<vtkProp> getProps()
    {
        if (smallBodyActor == null)
        {
            initialize();
            loadImage(getKey().name);

            smallBodyMapper = new vtkPolyDataMapper();
            smallBodyMapper.ScalarVisibilityOff();
            smallBodyMapper.SetScalarModeToDefault();
            smallBodyMapper.SetInputData(shiftedImagePolyData);
            smallBodyMapper.Update();

            imageTexture = new vtkTexture();
            imageTexture.InterpolateOn();
            imageTexture.RepeatOff();
            imageTexture.EdgeClampOn();
            imageTexture.SetInputData(displayedImage);

            smallBodyActor = new vtkActor();
            smallBodyActor.SetMapper(smallBodyMapper);
            smallBodyActor.SetTexture(imageTexture);
            vtkProperty smallBodyProperty = smallBodyActor.GetProperty();
            smallBodyProperty.LightingOff();

            smallBodyActors.add(smallBodyActor);
        }

        return smallBodyActors;
    }

    public void setVisible(boolean b)
    {
        if (smallBodyActor != null)
            smallBodyActor.SetVisibility(b ? 1 : 0);

        super.setVisible(b);
    }

    public double getOpacity()
    {
        return imageOpacity;
    }

    public void setOpacity(double imageOpacity)
    {
        this.imageOpacity = imageOpacity;
        vtkProperty smallBodyProperty = smallBodyActor.GetProperty();
        smallBodyProperty.SetOpacity(imageOpacity);
        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    protected void loadImage(String name)
    {
        String imageFile = null;
        if (getKey().source == ImageSource.IMAGE_MAP)
            imageFile = FileCache.getFileFromServer(name).getAbsolutePath();
        else
            imageFile = getKey().name;

        if (rawImage == null)
            rawImage = new vtkImageData();

        // Check if image is a custom-supported one
        if(VtkENVIReader.isENVIFilename(imageFile))
        {
            // Customized support for ENVI binary files
            VtkENVIReader reader = new VtkENVIReader();
            reader.SetFileName(imageFile);
            reader.Update();
            rawImage.DeepCopy(reader.GetOutput());
        }
        else
        {
            // Otherwise, try vtk's built in reader
            vtkPNGReader reader = new vtkPNGReader();
            reader.SetFileName(imageFile);
            reader.Update();
            rawImage.DeepCopy(reader.GetOutput());
        }
        // TODO: add capability to read FITS images - this could be moved up into the Image class

        double[] scalarRange = rawImage.GetScalarRange();
        minValue = (float)scalarRange[0];
        maxValue = (float)scalarRange[1];
        setDisplayedImageRange(new IntensityRange(0, 255));
    }

    public double getDefaultOffset()
    {
        return 2.0*smallBodyModel.getMinShiftAmount();
    }

    public void setOffset(double offset)
    {
        this.offset = offset;

        shiftedImagePolyData.DeepCopy(imagePolyData);
        PolyDataUtil.shiftPolyDataInNormalDirection(shiftedImagePolyData, offset);

        this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
    }

    public double getOffset()
    {
        return offset;
    }

    public void delete()
    {
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_RESOLUTION_CHANGED.equals(evt.getPropertyName()))
        {
            initialized = false;
            initialize();
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    @Override
    public vtkTexture getTexture()
    {
        return imageTexture;
    }

    @Override
    public LinkedHashMap<String, String> getProperties() throws IOException
    {
        return new LinkedHashMap<String, String>();
    }

    public void setDisplayedImageRange(IntensityRange range)
    {
        if (rawImage.GetNumberOfScalarComponents() > 1)
        {
            displayedImage = rawImage;
            return;
        }

        if (displayedRange.min != range.min ||
                displayedRange.max != range.max)
        {
            displayedRange = range;

            float dx = (maxValue-minValue)/255.0f;
            float min = minValue + range.min*dx;
            float max = minValue + range.max*dx;

            // Update the displayed image
            vtkLookupTable lut = new vtkLookupTable();
            lut.SetTableRange(min, max);
            lut.SetValueRange(0.0, 1.0);
            lut.SetHueRange(0.0, 0.0);
            lut.SetSaturationRange(0.0, 0.0);
            //lut.SetNumberOfTableValues(402);
            lut.SetRampToLinear();
            lut.Build();

            /*
            // Change contrast of each channel separately and then combine the 3 channels into one image
            int numChannels = rawImage.GetNumberOfScalarComponents();
            vtkImageAppendComponents appendFilter = new vtkImageAppendComponents();
            for (int i=0; i<numChannels; ++i)
            {
                vtkImageMapToColors mapToColors = new vtkImageMapToColors();
                mapToColors.SetInput(rawImage);
                mapToColors.SetOutputFormatToRGB();
                mapToColors.SetLookupTable(lut);
                mapToColors.SetActiveComponent(i);
                mapToColors.Update();

                vtkAlgorithmOutput output = mapToColors.GetOutputPort();
                vtkImageMagnitude magnitudeFilter = new vtkImageMagnitude();
                magnitudeFilter.SetInputConnection(output);
                magnitudeFilter.Update();
                output = magnitudeFilter.GetOutputPort();

                if (i == 0)
                    appendFilter.SetInputConnection(0, output);
                else
                    appendFilter.AddInputConnection(0, output);
            }

            appendFilter.Update();
            vtkImageData appendFilterOutput = appendFilter.GetOutput();
             */

            vtkImageMapToColors mapToColors = new vtkImageMapToColors();
            mapToColors.SetInputData(rawImage);
            mapToColors.SetOutputFormatToRGBA();
            mapToColors.SetLookupTable(lut);
            mapToColors.Update();

            vtkImageData mapToColorsOutput = mapToColors.GetOutput();

            if (displayedImage == null)
                displayedImage = new vtkImageData();
            displayedImage.DeepCopy(mapToColorsOutput);

            mapToColors.Delete();
            lut.Delete();

            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
        }
    }

    public int getNumberOfComponentsOfOriginalImage()
    {
        return rawImage.GetNumberOfScalarComponents();
    }

    @Override
    public String getImageName()
    {
        return imageName;
    }
}
