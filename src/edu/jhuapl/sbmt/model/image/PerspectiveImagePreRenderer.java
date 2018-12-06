package edu.jhuapl.sbmt.model.image;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.NativeLibraryLoader;
import edu.jhuapl.sbmt.client.SbmtModelFactory;
import edu.jhuapl.sbmt.client.SbmtMultiMissionTool;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.model.image.Image.ImageKey;
import edu.jhuapl.sbmt.tools.Authenticator;

import nom.tam.fits.FitsException;

public class PerspectiveImagePreRenderer
{
    private PerspectiveImage image;
    private int resolutionIndex;
    private String outputDir;

    public PerspectiveImagePreRenderer(PerspectiveImage image, String outputDir)
    {
        this.image = image;
        this.resolutionIndex = image.getSmallBodyModel().getModelResolution();
        this.outputDir = outputDir;
        calculateFootprint();
        calculateOffLimb();
    }

    private void calculateFootprint()
    {
        SmallBodyModel smallBodyModel = image.getSmallBodyModel();
        double[] frustum1Adjusted = image.getFrustum1Adjusted()[image.getDefaultSlice()];
        double[] frustum2Adjusted = image.getFrustum2Adjusted()[image.getDefaultSlice()];
        double[] frustum3Adjusted = image.getFrustum3Adjusted()[image.getDefaultSlice()];
        double[] frustum4Adjusted = image.getFrustum4Adjusted()[image.getDefaultSlice()];
        double[] spacecraftPositionAdjusted = image.getSpacecraftPositionAdjusted()[image.getDefaultSlice()];
        vtkPolyData footprint = smallBodyModel.computeFrustumIntersection(spacecraftPositionAdjusted,
                frustum1Adjusted, frustum3Adjusted, frustum4Adjusted, frustum2Adjusted);

        footprint.GetCellData().SetScalars(null);
        footprint.GetPointData().SetScalars(null);

        System.out.println("PerspectiveImagePreRenderer: calculateFootprint: footprint is " + footprint);

        vtkPolyDataWriter writer = new vtkPolyDataWriter();
        writer.SetInputData(footprint);
        System.out.println("PerspectiveImage: loadFootprint: fit file full path " + image.getFitFileFullPath());
//        String intersectionFileName = new File(image.getFitFileFullPath()).getParent() + File.separator  + FilenameUtils.getBaseName(image.getFitFileFullPath()) + "_" + resolutionIndex + "_frustumIntersection.vtk";
        String intersectionFileName = outputDir + File.separator  + FilenameUtils.getBaseName(image.getFitFileFullPath()) + "_" + resolutionIndex + "_frustumIntersection.vtk";

        System.out.println("PerspectiveImage: loadFootprint: saving footprint to " + intersectionFileName);

        writer.SetFileName(new File(intersectionFileName).toString());
        writer.SetFileTypeToBinary();
        writer.Write();
    }

    private void calculateOffLimb()
    {
        OffLimbPlaneCalculator calculator = new OffLimbPlaneCalculator(image);
        calculator.generateOffLimbPlane(image);
//        String filename = new File(image.getFitFileFullPath()).getParent() +  File.separator  + FilenameUtils.getBaseName(image.getFitFileFullPath()) + "_" + resolutionIndex + "_offLimbImageData.vtk";
        String filename = outputDir +  File.separator  + FilenameUtils.getBaseName(image.getFitFileFullPath()) + "_" + resolutionIndex + "_offLimbImageData.vtk";

        calculator.saveToDisk(filename);
    }

    public static void main(String[] args) throws FitsException, IOException
    {
        String inputDirectory = args[0];
        ImageSource source = ImageSource.valueOf(args[1]);
        ShapeModelBody body = ShapeModelBody.valueOf(args[2]);
        ShapeModelType type = ShapeModelType.valueOf(args[3]);
        int imagerIndex = Integer.parseInt(args[4]);
        String outputDirectory = args[5];

        boolean aplVersion = true;
//        String rootURL = FileCache.createFileURL("/disks/d0180/htdocs-sbmt/internal/sbmt").toString();
        String rootURL = "http://sbmt.jhuapl.edu/sbmt/prod/";
        Configuration.setAPLVersion(aplVersion);
        Configuration.setRootURL(rootURL);

        SbmtMultiMissionTool.configureMission();

        // authentication
        Authenticator.authenticate();

        // initialize view config
        SmallBodyViewConfig.initialize();

        // VTK
        System.setProperty("java.awt.headless", "true");
        NativeLibraryLoader.loadVtkLibrariesHeadless();

        SmallBodyViewConfig config = SmallBodyViewConfig.getSmallBodyConfig(body, type);
        ImagingInstrument instrument = config.imagingInstruments[imagerIndex];

        File[] fileList = new File(inputDirectory).listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return FilenameUtils.getExtension(name).contains("fit");
            }
        });
        for (File filename : fileList)
        {
            //may need to massage name here, need it to be /bennu/jfkfjksf, also need to strip .fits
            String basename = filename.getParent() + File.separator + FilenameUtils.getBaseName(filename.getAbsolutePath());
            basename = basename.substring(basename.indexOf("2") + 2);
            ImageKey key = new ImageKey(basename, source, instrument);
            System.out.println("PerspectiveImagePreRenderer: main: filename is " + basename);

            SmallBodyModel smallBodyModel = SbmtModelFactory.createSmallBodyModel(config);
            for (int i=0; i<smallBodyModel.getNumberResolutionLevels(); i++)
            {
                smallBodyModel.setModelResolution(i);
                PerspectiveImage image = (PerspectiveImage)SbmtModelFactory.createImage(key, smallBodyModel, false);
                PerspectiveImagePreRenderer preRenderer = new PerspectiveImagePreRenderer(image, outputDirectory);
            }
        }
    }
}
