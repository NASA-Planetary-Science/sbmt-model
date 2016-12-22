package edu.jhuapl.sbmt.model.eros;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vtk.vtkImageData;
import vtk.vtkImageReslice;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.model.image.PerspectiveImage;
import edu.jhuapl.sbmt.util.BackPlanesXml;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.BPMetaBuilder;
import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.MetaField;
import edu.jhuapl.sbmt.util.BackplanesFileFormat;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

public class MSIImage extends PerspectiveImage
{
    // Size of image after resampling. Before resampling image is 537 by 244 pixels.
    // MSI pixels are resampled to make them square. According to SPICE kernel msi15.ti,
    // MSI pixel size in degrees is 2.2623/244 in Y; 2.9505/537 in X. To square the
    // pixels, resample X to 2.2623 * 537/2.9505 = ~412.
    public static final int RESAMPLED_IMAGE_WIDTH = 537;
    public static final int RESAMPLED_IMAGE_HEIGHT = 412;

    // Number of pixels on each side of the image that are
    // masked out (invalid) due to filtering.
    private static final int LEFT_MASK = 14;
    private static final int RIGHT_MASK = 14;
    private static final int TOP_MASK = 2;
    private static final int BOTTOM_MASK = 2;
    private static final String xmlTemplate = "edu/jhuapl/sbmt/model/eros/msiXmlTemplate.xml";

    public MSIImage(ImageKey key,
            SmallBodyModel smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {
        super(key, smallBodyModel, loadPointingOnly);

        //the parent class looks like it only wants to set the labelFileFullPath if the ImageSource = LABEL
        //but the initialization of pngFileFullPath sets it to the label file. Just copy that to the
        //labelFileFullPath.
        setLabelFileFullPath(getPngFileFullPath());
    }

    @Override
    protected void processRawImage(vtkImageData rawImage)
    {
        int[] dims = rawImage.GetDimensions();
        int originalHeight = dims[1];

        vtkImageReslice reslice = new vtkImageReslice();
        reslice.SetInputData(rawImage);
        reslice.SetInterpolationModeToLinear();
        reslice.SetOutputSpacing(1.0, (double)originalHeight/(double)RESAMPLED_IMAGE_HEIGHT, 1.0);
        reslice.SetOutputOrigin(0.0, 0.0, 0.0);
        reslice.SetOutputExtent(0, RESAMPLED_IMAGE_WIDTH-1, 0, RESAMPLED_IMAGE_HEIGHT-1, 0, 0);
        reslice.Update();

        vtkImageData resliceOutput = reslice.GetOutput();
        rawImage.DeepCopy(resliceOutput);
        rawImage.SetSpacing(1, 1, 1);

    }

    @Override
    protected int[] getMaskSizes()
    {
        return new int[]{TOP_MASK, RIGHT_MASK, BOTTOM_MASK, LEFT_MASK};
    }

    @Override
    protected String initializeFitFileFullPath()
    {
        ImageKey key = getKey();
        return FileCache.getFileFromServer(key.name + ".FIT").getAbsolutePath();
    }

    @Override
    protected String initializeLabelFileFullPath()
    {
        ImageKey key = getKey();
        String imgLblFilename = key.name + ".LBL";
        return FileCache.getFileFromServer(imgLblFilename).getAbsolutePath();
    }

    @Override
    protected String initializeInfoFileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String infoFilename = keyFile.getParentFile().getParent()
        + "/infofiles/" + keyFile.getName() + ".INFO";
        return FileCache.getFileFromServer(infoFilename).getAbsolutePath();
    }

    @Override
    protected String initializeSumfileFullPath()
    {
        ImageKey key = getKey();
        File keyFile = new File(key.name);
        String sumFilename = keyFile.getParentFile().getParent()
        + "/sumfiles/" + keyFile.getName().substring(0, 11) + ".SUM";

        //This is for the ~90K new sumfiles from Olivier for the MSI backplanes delivery
        if (true)
        {
            sumFilename = keyFile.getParentFile().getParent()
            + "/sumfiles_to_be_delivered/" + keyFile.getName().substring(0, 11) + ".SUM";
            System.err.println("SUMFILE: " + sumFilename);
        }

        return FileCache.getFileFromServer(sumFilename).getAbsolutePath();
    }

    @Override
    public int getFilter()
    {
        String fitName = new File(getFitFileFullPath()).getName();
        return Integer.parseInt(fitName.substring(12,13));
    }

    /**
     * Note although there is only 1 MSI camera, we are abusing the following function
     * to return 1 if image is IOF or 2 if image is CIF.
     */
    @Override
    public int getCamera()
    {
        String fitName = new File(getFitFileFullPath()).getName();
        if (fitName.toUpperCase().contains("_IOF_"))
            return 1;
        else // CIF
            return 2;
    }

    public String getCameraName()
    {
        return "MSI";
    }

    /**
     * Return File containing path to Xml template as resource
     * @return
     */
    public String getXmlTemplate() {
        return xmlTemplate;
    }

    @Override
    /**
     * MSI PDS4 label generation. Uses the PDS3 label file associated with the source image to populate
     * the XML tags in the output lblFilename. This implementation assumes the product file referenced
     * by imgName is a FITS file.
     * @param imgName - full name of image
     * @param lblFileName - base name of label file, no extension. The extension is dependent on image
     *                      type and is added here (e.g. PDS 3 extension is ".lbl", PDS 4 is ".xml").
     * @throws IOException
     */
    public void generateBackplanesLabel(File imgName, File lblFileName) throws IOException
    {
        if (FilenameUtils.getExtension(imgName.getAbsolutePath()).toUpperCase().compareTo(BackplanesFileFormat.IMG.getExtension().toUpperCase()) == 0)
        {
            System.err.println("PDS4 MSI backplanes label generator requires a FITS backplanes image. Input file " + imgName + " is IMG format.");
            System.err.println("Writing PDS3 label, not PDS4.");
            super.generateBackplanesLabel(imgName, lblFileName);
            return;
        }

        //Append the appropriate extension
        File labelFileName = new File(lblFileName.getAbsolutePath() + ".xml");

        //generate XML metadata from PDS3 label. NOTE: the PDS3 label is the label associated with the source image,
        //i.e. the image that was used to generate this class.
        BPMetaBuilder xmlMetaDataBuilder = pds3ToXmlMeta(this.getLabelFileFullPath(), labelFileName.getAbsolutePath());

        //gather additional metadata from image Fits file
        try
        {
            xmlMetaDataBuilder = fitsToXmlMeta(imgName, xmlMetaDataBuilder); //imgName must be a FITS file
        }
        catch (FitsException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            System.out.println("ERROR trying to parse additional metadata from file:" + imgName.getName());
            System.out.println("Stopping with error in MSIImage.generateBackplanesLabel()");
        }

        //add more metadata not parsed from fits or label file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String createDate = sdf.format(date).replace(' ', 'T') + "Z";
        xmlMetaDataBuilder.setMetaField(MetaField.CREATIONDATETIME, createDate);

        //generate Xml Document
        BackPlanesXmlMeta xmlMetaData = xmlMetaDataBuilder.build();
        BackPlanesXml xmlLabel = metaToXmlDoc(xmlMetaData, getXmlTemplate());

        //add source file as an external reference. Do this because the source is in PDS3 and has no PDS4 LID.
        //the following assumes there is only 1 <Reference_List>. This will have to be modified if multiple <Reference_List> tags exist.
        NodeList docNodeList = xmlLabel.xmlDoc.doc.getElementsByTagName("Reference_List");
        Node externalRefList = docNodeList.item(0);

        //text for <description> tag within <External_Reference>
        String datasetId = xmlMetaData.metaStrings.get(MetaField.DATASETID);
        String desc = xmlMetaData.metaStrings.get(MetaField.SRCFILENAME) + " from " + datasetId;

        //text for <reference_text> tag within <External_Reference>
        String refText = "Murchie S., Taylor H., NEAR MSI IMAGES FOR EROS/ORBIT, " + datasetId + " NASA Planetary Data System, 2001.";
        xmlLabel.addExternalRef(xmlLabel.xmlDoc, externalRefList, desc, refText);

        //create PDS4 XML label
        try
        {
            xmlLabel.writeXML(labelFileName.getAbsolutePath());
        }
        catch (XPathExpressionException | TransformerException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("ERROR! Could not write XML label:" + labelFileName);
        }

    }

    @Override
    //gather additional metadata from image Fits file
    public BPMetaBuilder fitsToXmlMeta(File fitsFile, BPMetaBuilder xmlMetaDataBuilder) throws FitsException {

        //MSI XML template expects that the fits files contains this many planes
        int numPlanes = 16;

        Fits thisFits = new Fits(fitsFile);
        try
        {

            //add metadata describing fits file.
            BasicHDU thisHDU = thisFits.getHDU(0);
            xmlMetaDataBuilder.hdrSize(thisHDU.getHeader().getSize());
            xmlMetaDataBuilder.setMetaField(MetaField.PRODUCTFILENAME, fitsFile.getName());

            //build logical ID
            xmlMetaDataBuilder.setMetaField(MetaField.LOGICALID, "urn:nasa:pds:nearmsi.shapebackplane:data:" + fitsFile.getName());

            /*
             * retrieve FITS axes information. Fits library returns the axes in order of
             * least varying to most varying:
             *
             * NAXIS3 = imgAxes[0]
             * NAXIS2 = imgAxes[1]
             * NAXIS1 = imgAxes[2]
             */
            int[] imgAxes = thisHDU.getAxes();
            int naxis3 = imgAxes[0];
            int naxis2 = imgAxes[1];
            int naxis1 = imgAxes[2];

            /*
             * set lines, samples.
             * Refer to http://sbndev.astro.umd.edu/wiki/Notes_for_Labelling_FITS_files
             * for details on axis ordering and values for PDS4 lines, samples.
             * NAXIS1 corresponds to PDS4 axis 2, which MUST have an axis name of "Sample"
             * NAXIS2 corresponds to PDS4 axis 1, which MUST have an axis name of "Line"
             */
            xmlMetaDataBuilder.setSamples(naxis1);
            xmlMetaDataBuilder.setLines(naxis2);

            if (numPlanes != naxis3) {
                System.out.println("ERROR! Expected number of planes:" + numPlanes);
                System.out.println("Fits file actually has " + naxis3 + " planes");
                System.out.println("Stopping program in MSIImage.fitsToXMlMeta()");
                System.exit(1);
            }

            //determine offset from start of fits file in bytes for each image plane.
            //first image plane is 'fits header bytes' away from beginning of file.
            int bytesPerPixel = (int) Math.abs(thisHDU.getBitPix()/ 8);
            int bytesPerImage = naxis1 * naxis2 * bytesPerPixel;
            int imgOffset = (int) thisHDU.getHeader().getSize();
            for (int ii = 0; ii < numPlanes; ii++) {
                xmlMetaDataBuilder.addOffset(imgOffset);
                imgOffset = imgOffset + bytesPerImage;
            }

        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return xmlMetaDataBuilder;
    }


    @Override
    /**
     * Read and extract information from PDS3 label and add it to output PDS4 label
     * @param pds3LblFname - PDS3 label to extract information from.
     * @param outXmlFname - PDS4 label to add information to.
     * @return
     */
    public BPMetaBuilder pds3ToXmlMeta(String pds3LblFname, String outXmlFname) {
        BPMetaBuilder metaDataBuilder = metaBfromPDS3(pds3LblFname, outXmlFname);
        return metaDataBuilder;
    }

    @Override
    /**
     * Use the information in metaData to fill out the XML document.
     * @param metaData - contains metadata information parsed from various sources.
     * @param xmlTemplate - pointer to XML template. Contains blank values for XML tags that
     *                are filled in from metadata.
     * @return
     */
    public BackPlanesXml metaToXmlDoc(BackPlanesXmlMeta metaData, String xmlTemplate) {
        BackPlanesXml xmlLabel = new BackPlanesXml(metaData, xmlTemplate);
        return xmlLabel;
    }


    /**
     * Load and parse PDS3 label for original PDS3 MSI image. Returns builder so that
     * a follow-on method can add to the builder if needed.
     */
    private BPMetaBuilder metaBfromPDS3(String pds3LblFname, String outXmlFname) {
        //load PDS3 label file into memory.
    	List<String> labelContents = new ArrayList<String>();
        try
        {
            labelContents = FileUtil.getFileLinesAsStringList(pds3LblFname);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Error! Could not parse label file:" + pds3LblFname);
            System.exit(1);
        }

        //initialize metadata builder
        BPMetaBuilder metaBuilder = new BPMetaBuilder(outXmlFname);

        //parse contents of PDS3 label. This could be streamlined and made more OO but for now I brute force.
        for (String line : labelContents) {
            if (line.startsWith("START_TIME")) {
                metaBuilder.setMetaField(MetaField.STARTDATETIME, BackPlanesXmlMeta.valFromKeyVal(line));
            } else if (line.startsWith("STOP_TIME")) {
                metaBuilder.setMetaField(MetaField.STOPDATETIME, BackPlanesXmlMeta.valFromKeyVal(line));
            } else if (line.startsWith("PRODUCT_ID")) {

                //this is the product ID of the original PDS3 label, which is referenced in the PDS4 label as the source filename.
                metaBuilder.setMetaField(MetaField.SRCFILENAME, BackPlanesXmlMeta.valFromKeyVal(line));
            } else if (line.startsWith("DATA_SET_ID")) {
                metaBuilder.setMetaField(MetaField.DATASETID, BackPlanesXmlMeta.valFromKeyVal(line));
            }
        }

        return metaBuilder;
    }



}
