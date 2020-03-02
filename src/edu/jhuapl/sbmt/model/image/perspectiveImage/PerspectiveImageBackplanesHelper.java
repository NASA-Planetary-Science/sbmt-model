package edu.jhuapl.sbmt.model.image.perspectiveImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import vtk.vtkDataArray;
import vtk.vtkGenericCell;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.util.ImageDataUtil;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.util.BackPlanesXml;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.BPMetaBuilder;
import edu.jhuapl.sbmt.util.BackplaneInfo;

import nom.tam.fits.FitsException;

class PerspectiveImageBackplanesHelper
{
	PerspectiveImage image;
    public static final float PDS_NA = -ImageDataUtil.FILL_CUTOFF;

	public PerspectiveImageBackplanesHelper(PerspectiveImage image)
	{
		this.image = image;
	}

	/**
     * Generate PDS 3 format backplanes label file. This is the default
     * implementation for classes extending PerspectiveImage.
     *
     * @param imgName - pointer to the data File for which this label is being
     *            created
     * @param lblFileName - pointer to the output label file to be written, without
     *            file name extension. The extension is dependent on image type
     *            (e.g. MSI images are written as PDS 4 XML labels), and is assigned
     *            in the class implementing this function.
     * @throws IOException
     */
    void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
    {
        StringBuffer strbuf = new StringBuffer("");

        int numBands = 16;

        appendWithPadding(strbuf, "PDS_VERSION_ID               = PDS3");
        appendWithPadding(strbuf, "");

        appendWithPadding(strbuf, "PRODUCT_TYPE                 = DDR");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String dateStr = sdf.format(date).replace(' ', 'T');
        appendWithPadding(strbuf, "PRODUCT_CREATION_TIME        = " + dateStr);
        appendWithPadding(strbuf, "PRODUCER_INSTITUTION_NAME    = \"APPLIED PHYSICS LABORATORY\"");
        appendWithPadding(strbuf, "SOFTWARE_NAME                = \"Small Body Mapping Tool\"");
        appendWithPadding(strbuf, "SHAPE_MODEL                  = \"" + image.getModelName() + "\"");

        appendWithPadding(strbuf, "");
        appendWithPadding(strbuf, "/* This DDR label describes one data file:                               */");
        appendWithPadding(strbuf, "/* 1. A multiple-band backplane image file with wavelength-independent,  */");
        appendWithPadding(strbuf, "/* spatial pixel-dependent geometric and timing information.             */");
        appendWithPadding(strbuf, "");
        appendWithPadding(strbuf, "OBJECT                       = FILE");

        appendWithPadding(strbuf, "  ^IMAGE                     = \"" + imgName.getName() + "\"");

        appendWithPadding(strbuf, "  RECORD_TYPE                = FIXED_LENGTH");
        appendWithPadding(strbuf, "  RECORD_BYTES               = " + (image.imageHeight * 4));
        appendWithPadding(strbuf, "  FILE_RECORDS               = " + (image.imageWidth * numBands));
        appendWithPadding(strbuf, "");

        appendWithPadding(strbuf, "  OBJECT                     = IMAGE");
        appendWithPadding(strbuf, "    LINES                    = " + image.imageHeight);
        appendWithPadding(strbuf, "    LINE_SAMPLES             = " + image.imageWidth);
        appendWithPadding(strbuf, "    SAMPLE_TYPE              = IEEE_REAL");
        appendWithPadding(strbuf, "    SAMPLE_BITS              = 32");
        appendWithPadding(strbuf, "    CORE_NULL                = 16#F49DC5AE#"); // bit pattern of -1.0e32 in hex

        appendWithPadding(strbuf, "    BANDS                    = " + numBands);
        appendWithPadding(strbuf, "    BAND_STORAGE_TYPE        = BAND_SEQUENTIAL");
        appendWithPadding(strbuf, "    BAND_NAME                = (\"Pixel value\",");
        appendWithPadding(strbuf, "                                \"x coordinate of center of pixel, km\",");
        appendWithPadding(strbuf, "                                \"y coordinate of center of pixel, km\",");
        appendWithPadding(strbuf, "                                \"z coordinate of center of pixel, km\",");
        appendWithPadding(strbuf, "                                \"Latitude, deg\",");
        appendWithPadding(strbuf, "                                \"Longitude, deg\",");
        appendWithPadding(strbuf, "                                \"Distance from center of body, km\",");
        appendWithPadding(strbuf, "                                \"Incidence angle, deg\",");
        appendWithPadding(strbuf, "                                \"Emission angle, deg\",");
        appendWithPadding(strbuf, "                                \"Phase angle, deg\",");
        appendWithPadding(strbuf, "                                \"Horizontal pixel scale, km per pixel\",");
        appendWithPadding(strbuf, "                                \"Vertical pixel scale, km per pixel\",");
        appendWithPadding(strbuf, "                                \"Slope, deg\",");
        appendWithPadding(strbuf, "                                \"Elevation, m\",");
        appendWithPadding(strbuf, "                                \"Gravitational acceleration, m/s^2\",");
        appendWithPadding(strbuf, "                                \"Gravitational potential, J/kg\")");
        appendWithPadding(strbuf, "");
        appendWithPadding(strbuf, "  END_OBJECT                 = IMAGE");
        appendWithPadding(strbuf, "END_OBJECT                   = FILE");

        appendWithPadding(strbuf, "");
        appendWithPadding(strbuf, "END");

        // return strbuf.toString();
        byte[] bytes = strbuf.toString().getBytes();
        OutputStream out = new FileOutputStream(lblFileName.getAbsolutePath() + ".lbl");
        out.write(bytes, 0, bytes.length);
        out.close();
    }

    void appendWithPadding(StringBuffer strbuf, String str)
    {
        strbuf.append(str);

        int length = str.length();
        while (length < 78)
        {
            strbuf.append(' ');
            ++length;
        }

        strbuf.append("\r\n");
    }

    float[] generateBackplanes()
    {
        return generateBackplanes(false);
    }

    /**
     * If <code>returnNullIfContainsLimb</code> then return null if any ray in the
     * direction of a pixel in the image does not intersect the asteroid. By setting
     * this boolean to true, you can (usually) determine whether or not the image
     * contains a limb without having to compute the entire backplane. Note that
     * this is a bit of a hack and a better way is needed to quickly determine if
     * there is a limb.
     *
     * @param returnNullIfContainsLimb
     * @return
     */
    float[] generateBackplanes(boolean returnNullIfContainsLimb)
    {
    	double[][] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted();
    	double[][] frustum1Adjusted = image.getFrustum1Adjusted();
    	double[][] frustum2Adjusted = image.getFrustum2Adjusted();
    	double[][] frustum3Adjusted = image.getFrustum3Adjusted();
    	int currentSlice = image.currentSlice;
        // We need to use cell normals not point normals for the calculations
        vtkDataArray normals = null;
        if (!returnNullIfContainsLimb)
            normals = image.getSmallBodyModel().getCellNormals();

        float[] data = new float[image.numBackplanes * image.imageHeight * image.imageWidth];

        vtksbCellLocator cellLocator = image.getSmallBodyModel().getCellLocator();

        // vtkPoints intersectPoints = new vtkPoints();
        // vtkIdList intersectCells = new vtkIdList();
        vtkGenericCell cell = new vtkGenericCell();

        // For each pixel in the image we need to compute the vector
        // from the spacecraft pointing in the direction of that pixel.
        // To do this, for each row in the image compute the left and
        // right vectors of the entire row. Then for each pixel in
        // the row use the two vectors from either side to compute
        // the vector of that pixel.
        double[] corner1 = {
        		spacecraftPositionAdjusted[currentSlice][0] + frustum1Adjusted[currentSlice][0],
        		spacecraftPositionAdjusted[currentSlice][1] + frustum1Adjusted[currentSlice][1],
        		spacecraftPositionAdjusted[currentSlice][2] + frustum1Adjusted[currentSlice][2]
        };
        double[] corner2 = {
        		spacecraftPositionAdjusted[currentSlice][0] + frustum2Adjusted[currentSlice][0],
        		spacecraftPositionAdjusted[currentSlice][1] + frustum2Adjusted[currentSlice][1],
                spacecraftPositionAdjusted[currentSlice][2] + frustum2Adjusted[currentSlice][2]
        };
        double[] corner3 = {
        		spacecraftPositionAdjusted[currentSlice][0] + frustum3Adjusted[currentSlice][0],
        		spacecraftPositionAdjusted[currentSlice][1] + frustum3Adjusted[currentSlice][1],
        		spacecraftPositionAdjusted[currentSlice][2] + frustum3Adjusted[currentSlice][2]
        };
        double[] vec12 = {
                corner2[0] - corner1[0],
                corner2[1] - corner1[1],
                corner2[2] - corner1[2]
        };
        double[] vec13 = {
                corner3[0] - corner1[0],
                corner3[1] - corner1[1],
                corner3[2] - corner1[2]
        };

        double horizScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum3Adjusted[currentSlice]) / 2.0) / image.imageHeight;
        double vertScaleFactor = 2.0 * Math.tan(MathUtil.vsep(frustum1Adjusted[currentSlice], frustum2Adjusted[currentSlice]) / 2.0) / image.imageWidth;

        double scdist = MathUtil.vnorm(spacecraftPositionAdjusted[currentSlice]);

        for (int i = 0; i < image.imageHeight; ++i)
        {
            // Compute the vector on the left of the row.
            double fracHeight = ((double) i / (double) (image.imageHeight - 1));
            double[] left = {
                    corner1[0] + fracHeight * vec13[0],
                    corner1[1] + fracHeight * vec13[1],
                    corner1[2] + fracHeight * vec13[2]
            };

            for (int j = 0; j < image.imageWidth; ++j)
            {
                // If we're just trying to know if there is a limb, we
                // only need to do intersections around the boundary of
                // the backplane, not the interior pixels.
                if (returnNullIfContainsLimb)
                {
                    if (j == 1 && i > 0 && i < image.imageHeight - 1)
                    {
                        j = image.imageWidth - 2;
                        continue;
                    }
                }

                double fracWidth = ((double) j / (double) (image.imageWidth - 1));
                double[] vec = {
                        left[0] + fracWidth * vec12[0],
                        left[1] + fracWidth * vec12[1],
                        left[2] + fracWidth * vec12[2]
                };
                vec[0] -= spacecraftPositionAdjusted[currentSlice][0];
                vec[1] -= spacecraftPositionAdjusted[currentSlice][1];
                vec[2] -= spacecraftPositionAdjusted[currentSlice][2];
                MathUtil.unorm(vec, vec);

                double[] lookPt = {
                		spacecraftPositionAdjusted[currentSlice][0] + 2.0 * scdist * vec[0],
                		spacecraftPositionAdjusted[currentSlice][1] + 2.0 * scdist * vec[1],
                		spacecraftPositionAdjusted[currentSlice][2] + 2.0 * scdist * vec[2]
                };

                // cellLocator.IntersectWithLine(spacecraftPosition, lookPt, intersectPoints,
                // intersectCells);
                double tol = 1e-6;
                double[] t = new double[1];
                double[] x = new double[3];
                double[] pcoords = new double[3];
                int[] subId = new int[1];
                int[] cellId = new int[1];
                int result = cellLocator.IntersectWithLine(spacecraftPositionAdjusted[currentSlice], lookPt, tol, t, x, pcoords, subId, cellId, cell);

                // if (intersectPoints.GetNumberOfPoints() == 0)
                // System.out.println(i + " " + j + " " + intersectPoints.GetNumberOfPoints());

                // int numberOfPoints = intersectPoints.GetNumberOfPoints();

                if (result > 0)
                {
                    // If we're just trying to know if there is a limb, do not
                    // compute the values of the backplane (It will crash since
                    // we don't have normals of the asteroid itself)
                    if (returnNullIfContainsLimb)
                        continue;

                    // double[] closestPoint = intersectPoints.GetPoint(0);
                    // int closestCell = intersectCells.GetId(0);
                    double[] closestPoint = x;
                    int closestCell = cellId[0];
                    double closestDist = MathUtil.distanceBetween(closestPoint, spacecraftPositionAdjusted[currentSlice]);

                    /*
                     * // compute the closest point to the spacecraft of all the intersecting
                     * points. if (numberOfPoints > 1) { for (int k=1; k<numberOfPoints; ++k) {
                     * double[] pt = intersectPoints.GetPoint(k); double dist =
                     * GeometryUtil.distanceBetween(pt, spacecraftPosition); if (dist < closestDist)
                     * { closestDist = dist; closestCell = intersectCells.GetId(k); closestPoint =
                     * pt; } } }
                     */

                    LatLon llr = MathUtil.reclat(closestPoint);
                    double lat = llr.lat * 180.0 / Math.PI;
                    double lon = llr.lon * 180.0 / Math.PI;
                    if (lon < 0.0)
                        lon += 360.0;

                    double[] normal = normals.GetTuple3(closestCell);
                    double[] illumAngles = image.computeIlluminationAnglesAtPoint(closestPoint, normal);

                    double horizPixelScale = closestDist * horizScaleFactor;
                    double vertPixelScale = closestDist * vertScaleFactor;

                    double[] coloringValues;
                    try
                    {
                        coloringValues = image.getSmallBodyModel().getAllColoringValues(closestPoint);
                    }
                    catch (@SuppressWarnings("unused") IOException e)
                    {
                        coloringValues = new double[] {};
                    }
                    int colorValueSize = coloringValues.length;

                    data[index(j, i, BackplaneInfo.PIXEL.ordinal())] = (float) image.getRawImage().GetScalarComponentAsFloat(j, i, 0, 0);
                    data[index(j, i, BackplaneInfo.X.ordinal())] = (float) closestPoint[0];
                    data[index(j, i, BackplaneInfo.Y.ordinal())] = (float) closestPoint[1];
                    data[index(j, i, BackplaneInfo.Z.ordinal())] = (float) closestPoint[2];
                    data[index(j, i, BackplaneInfo.LAT.ordinal())] = (float) lat;
                    data[index(j, i, BackplaneInfo.LON.ordinal())] = (float) lon;
                    data[index(j, i, BackplaneInfo.DIST.ordinal())] = (float) llr.rad;
                    data[index(j, i, BackplaneInfo.INC.ordinal())] = (float) illumAngles[0];
                    data[index(j, i, BackplaneInfo.EMI.ordinal())] = (float) illumAngles[1];
                    data[index(j, i, BackplaneInfo.PHASE.ordinal())] = (float) illumAngles[2];
                    data[index(j, i, BackplaneInfo.HSCALE.ordinal())] = (float) horizPixelScale;
                    data[index(j, i, BackplaneInfo.VSCALE.ordinal())] = (float) vertPixelScale;
                    data[index(j, i, BackplaneInfo.SLOPE.ordinal())] = colorValueSize > 0 ? (float) coloringValues[0] : 0.0F; // slope
                    data[index(j, i, BackplaneInfo.EL.ordinal())] = colorValueSize > 1 ? (float) coloringValues[1] : 0.0F; // elevation
                    data[index(j, i, BackplaneInfo.GRAVACC.ordinal())] = colorValueSize > 2 ? (float) coloringValues[2] : 0.0F; // grav acc;
                    data[index(j, i, BackplaneInfo.GRAVPOT.ordinal())] = colorValueSize > 3 ? (float) coloringValues[3] : 0.0F; // grav pot
                }
                else
                {
                    if (returnNullIfContainsLimb)
                        return null;

                    data[index(j, i, 0)] = (float) image.getRawImage().GetScalarComponentAsFloat(j, i, 0, 0);
                    for (int k = 1; k < image.numBackplanes; ++k)
                        data[index(j, i, k)] = PDS_NA;
                }
            }
        }

        return data;
    }

    /**
     * Generate metadata to be used in PDS4 XML creation by parsing existing PDS3
     * label. By default creates a bare-bones metadata class that only contains the
     * output XML filename. Use this method to use an existing PDS3 label as the
     * source metadata on which to describe a new PDS4 product.
     */
    public BPMetaBuilder pds3ToXmlMeta(String pds3Fname, String outXmlFname)
    {
        BPMetaBuilder metaDataBuilder = new BackPlanesXmlMeta.BPMetaBuilder(outXmlFname);
        return metaDataBuilder;
    }

    /**
     * Generate metadata to be used in PDS4 XML creation by parsing existing PDS4
     * label. By default creates a bare-bones metdata class that only contains the
     * output XML filename. Use this method to use an existing PDS4 label as the
     * source metadata on which to describe a new PDS4 product.
     */
    public BPMetaBuilder pds4ToXmlMeta(String pds4Fname, String outXmlFname)
    {
        BPMetaBuilder metaDataBuilder = new BackPlanesXmlMeta.BPMetaBuilder(outXmlFname);
        return metaDataBuilder;
    }

    /**
     * Parse additional metadata from the fits file and add to the metaDataBuilder.
     *
     * @throws FitsException
     */
    public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder metaDataBuilder) throws FitsException
    {
        return metaDataBuilder;
    }

    /**
     * Generate XML document from XmlMetadata
     *
     * @param metaData - metadata to be used in populating XmlDoc
     * @param xmlTemplate - path to XML template file
     */
    public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate)
    {
        BackPlanesXml xmlLabel = new BackPlanesXml(metaData, xmlTemplate);
        return xmlLabel;
    }

    int index(int i, int j, int k)
    {
        return ((k * image.imageHeight + j) * image.imageWidth + i);
    }

}
