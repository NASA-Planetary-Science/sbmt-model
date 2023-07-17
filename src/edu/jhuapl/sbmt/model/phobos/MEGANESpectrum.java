package edu.jhuapl.sbmt.model.phobos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.pointing.io.InfoFileReader;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.InstrumentMetadata;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.ProvidesGenericObjectFromMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;


public class MEGANESpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectrumInstrumentMetadataIO specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    double boundingBoxDiagonalLength;
    boolean headless;

    private static final Key<MEGANESpectrum> MEGANESPECTRUM_KEY = Key.of("MEGANESpectrum");
	private static final Key<String> FILENAME_KEY = Key.of("fileName");
	private static final Key<SpectrumInstrumentMetadataIO> SPECIO_KEY = Key.of("spectrumIO");
	private static final Key<Double> BOUNDINGBOX_KEY = Key.of("boundingBoxDiagonalLength");
	private static final Key<BasicSpectrumInstrument> SPECTRALINSTRUMENT_KEY = Key.of("spectralInstrument");
	private static final Key<Boolean> HEADLESS_KEY = Key.of("headless");
	private static final Key<Boolean> ISCUSTOM_KEY = Key.of("isCustom");


    public static void initializeSerializationProxy()
	{
    	InstanceGetter.defaultInstanceGetter().register(MEGANESPECTRUM_KEY, (source) -> {

    		String filename = source.get(FILENAME_KEY);
    		ProvidesGenericObjectFromMetadata<SpectrumInstrumentMetadataIO> specIOMetadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(SPECIO_KEY);
    		SpectrumInstrumentMetadataIO specIO = specIOMetadata.provide(source);
    		double boundingBoxDiagonalLength = source.get(BOUNDINGBOX_KEY);
    		Boolean headless = source.get(HEADLESS_KEY);
    		Boolean isCustom = source.get(ISCUSTOM_KEY);

    		MEGANESpectrum spec = null;
			try
			{
				spec = new MEGANESpectrum(filename, specIO, boundingBoxDiagonalLength, new MEGANE(), headless, isCustom);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return spec;

    	}, MEGANESpectrum.class, spec -> {

    		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
    		result.put(FILENAME_KEY, spec.serverpath);
    		result.put(SPECIO_KEY, spec.specIO);
    		result.put(BOUNDINGBOX_KEY, spec.boundingBoxDiagonalLength);
    		result.put(HEADLESS_KEY, spec.headless);
    		result.put(ISCUSTOM_KEY, spec.isCustomSpectra);
    		return result;
    	});

	}

    public MEGANESpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument) throws IOException
    {
        this(filename, specIO, boundingBoxDiagonalLength, instrument, false, false);
    }

    public MEGANESpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = specIO;
        instrumentMetadata = specIO.getInstrumentMetadata("NIRS3");
        this.boundingBoxDiagonalLength = boundingBoxDiagonalLength;
        double dx = MathUtil.vnorm(spacecraftPosition) + boundingBoxDiagonalLength; //smallBodyModel.getBoundingBoxDiagonalLength();
        toSunVectorLength=dx;
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
//        new MEGANESpectrumWriter(file.getAbsolutePath(), this).write();
//        File infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
//        FileChannel src = new FileInputStream(infoFile).getChannel();
//        File infoFileDestination = new File(file.getParentFile() + File.separator + file.getName() + ".INFO");
//        FileChannel dest = new FileOutputStream(infoFileDestination).getChannel();
//        dest.transferFrom(src, 0, src.size());
//        src.close();
//        dest.close();
    }

    public void saveInfofile(File file) throws IOException
    {

    }

    protected String getLocalInfoFilePathOnServer()
    {
    	String normalpath = SafeURLPaths.instance().getString(serverpath).substring(7);
    	return FilenameUtils.removeExtension(normalpath) + ".INFO";
    }

    protected String getLocalSpectrumFilePathOnServer()
    {
        return SafeURLPaths.instance().getString(serverpath).substring(7);
    }

    protected String getInfoFilePathOnServer()
    {
        if (isCustomSpectra)
        {
            return getLocalInfoFilePathOnServer();
        }
        else
        {
            String spectrumPath = getSpectrumPathOnServer().substring(0, getSpectrumPathOnServer().lastIndexOf("/"));
//            System.out.println("OTESSpectrum: getInfoFilePathOnServer: spectrum path " + Paths.get(getSpectrumPathOnServer()).getParent().resolveSibling("infofiles-corrected"));
            return Paths.get(spectrumPath).getParent()
                    .resolveSibling("infofiles-corrected")
                    .resolve(FilenameUtils.getBaseName(getSpectrumPathOnServer()) + ".INFO")
                    .toString();
        }
    }

    public String getSpectrumPathOnServer()
    {
    	if (serverpath.contains("ote_calrd"))
    		spec = instrumentMetadata.getSpecs().get(0);
    	else
    		spec = instrumentMetadata.getSpecs().get(1);

        if (isCustomSpectra)
        {
            return serverpath;
        }
        else
        {
            return Paths.get(serverpath).getParent()
                    .resolve(FilenameUtils.getBaseName(serverpath) + "." + extension)
                    .toString();
        }
    }

//    protected String getInfoFilePathOnServer()
//    {
//        String path = Paths.get(getSpectrumPathOnServer()).getParent()
//                .resolveSibling("infofiles-corrected")
//                .resolve(FilenameUtils.getBaseName(getSpectrumPathOnServer()) + ".INFO")
//                .toString();
//        String path2 = Paths.get(serverpath).getParent()
//                .resolve(FilenameUtils.getBaseName(serverpath) + ".INFO")
//                .toString();
//        if (FileCache.getFileInfoFromServer(path).isExistsOnServer() == YesOrNo.NO)
//        {
//            return  FilenameUtils.getBaseName(serverpath) + ".INFO";
//        }
//        else if (FileCache.isFileInCustomData(path2) == true)
//        {
//            return path2;
//        }
//        else
//        {
//            return path;
//        }
//    }
//
//    public String getSpectrumPathOnServer()
//    {
//        String path = Paths.get(serverpath).getParent()
//                .resolve(FilenameUtils.getBaseName(serverpath) + "." + extension)
//                .toString();
//        return path;
//    }

    public void readPointingFromInfoFile()
    {
        InfoFileReader reader = null;
        if (!isCustomSpectra)
        {
            infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
            reader = new InfoFileReader(infoFile.getAbsolutePath());
        }
        else
        {
            infoFile = new File(getInfoFilePathOnServer());
            reader = new InfoFileReader(infoFile.toString());
        }
//        String infoFilePath = getInfoFilePathOnServer();
//        if (FileCache.isFileInCustomData(infoFilePath) == false)
//            infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
//        else
//            infoFile = new File(infoFilePath);
        //
//        System.out.println("OTESSpectrum: readPointingFromInfoFile: info file path " + SafeURLPaths.instance().getUrl(infoFile.getAbsolutePath()));
//        InfoFileReader reader = new InfoFileReader(SafeURLPaths.instance().getString(infoFile.getAbsolutePath()));
        reader.read();
        //
        Vector3D origin = new Vector3D(reader.getSpacecraftPosition());
//                .scalarMultiply(1e-3);
        Vector3D fovUnit = new Vector3D(reader.getFrustum2()).normalize(); // for whatever
                                                               // reason,
                                                               // frustum2
                                                               // contains the
                                                               // vector along
                                                               // the field of
                                                               // view cone
        Vector3D boresightUnit = new Vector3D(reader.getBoresightDirection()).normalize();
        Vector3D lookTarget = origin
                .add(boresightUnit.scalarMultiply(origin.getNorm()));

        double[] intersectPoint = new double[3];
//        boresightInterceptFaceID = smallBodyModel.computeRayIntersection(origin.toArray(), boresightUnit.toArray(), intersectPoint);
//        double[] interceptNormal = smallBodyModel.getCellNormals().GetTuple3(boresightInterceptFaceID);
//        vtkCellCenters centers = new vtkCellCenters();
//        centers.SetInputData(smallBodyModel.getSmallBodyPolyData());
//        centers.VertexCellsOn();
//        centers.Update();
//        double[] center = centers.GetOutput().GetPoint(boresightInterceptFaceID);
//
//        double[] center = smallBodyModel.getSmallBodyPolyData().GetPoint(boresightInterceptFaceID);
//
//        System.out.println("OTESSpectrum: readPointingFromInfoFile: intercept normal " + interceptNormal[0] + " " + interceptNormal[1] + " " + interceptNormal[2]);
//        System.out.println("OTESSpectrum: readPointingFromInfoFile: center " + intersectPoint[0] + " " + intersectPoint[1] + " " + intersectPoint[2]);
//
//        Vector3D nmlVec=new Vector3D(interceptNormal).normalize();
//        Vector3D ctrVec=new Vector3D(intersectPoint).normalize();
//        Vector3D toScVec=new Vector3D(spacecraftPosition).subtract(ctrVec);
//        emissionAngle = Math.toDegrees(Math.acos(nmlVec.dotProduct(toScVec.normalize())));


        double fovDeg = Math
                .toDegrees(Vector3D.angle(fovUnit, boresightUnit) * 2.);
        toSunUnitVector = new Vector3D(reader.getSunPosition()).normalize();
        Frustum frustum = new Frustum(origin.toArray(), lookTarget.toArray(),
                boresightUnit.orthogonal().toArray(), fovDeg, fovDeg);
        frustum1 = frustum.ul;
        frustum2 = frustum.ur;
        frustum3 = frustum.lr;
        frustum4 = frustum.ll;
        spacecraftPosition = frustum.origin;

        this.dateTime=new DateTime(reader.getStartTime());

        // 1 double[] ul, // ordering is from
        // smallBodyModel.computeFrustumIntersection(spacecraftPosition,
        // frustum1, frustum2, frustum3, frustum4);
        // 2 double[] ur,
        // 3 double[] lr,
        // 4 double[] ll)

    }

    public void readSpectrumFromFile()
    {
//    	OTESSpectrumReader reader = null;
//        if (!isCustomSpectra)
//        {
//            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
//            reader=new OTESSpectrumReader(spectrumFile.getAbsolutePath(), getNumberOfBands());
//        }
//        else
//        {
//            reader=new OTESSpectrumReader(getLocalSpectrumFilePathOnServer(), getNumberOfBands());
//        }
//        reader.read();
//
//        spectrum=reader.getData();
//        xData = reader.getXAxis();
//        time = reader.getSclk();
    }

    @Override
    public int getNumberOfBands()
    {
//        if (FilenameUtils.getExtension(serverpath.toString()).equals("spect"))
//            return OTES.bandCentersLength;
//        else
            return 208;
    }

    @Override
    public String getxAxisUnits()
    {
        return spec.getxAxisUnits();
//        return "Wavenumber (1/cm)";
    }

    @Override
    public String getyAxisUnits()
    {
        return spec.getyAxisUnits();
//        if (FilenameUtils.getExtension(serverpath.toString()).equals("spect"))
//            return "Calibrated Radiance";
//        else
//            return "Emissivity Spectra";
    }

    @Override
    public String getDataName()
    {
    	return spec.getDataName();
//        if (spec != null)
//            return spec.getDataName();
//        else
//            return key.name;
////        if (FilenameUtils.getExtension(serverpath.toString()).equals("spect"))
////            return "OTES L2 Calibrated Radiance";
////        else
////            return "OTES L3 Spot Emissivity";
    }

//    @Override
//    public double[] getChannelColor()
//    {
//        if (coloringStyle == SpectrumColoringStyle.EMISSION_ANGLE)
//        {
//            //This calculation is using the average emission angle over the spectrum, which doesn't exacty match the emission angle of the
//            //boresight - no good way to calculate this data at the moment.  Olivier said this is fine.  Need to present a way to either have this option or the old one via RGB for coloring
////
////            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(this, new Vector3D(spacecraftPosition));
////            Colormap colormap = Colormaps.getNewInstanceOfBuiltInColormap("OREX Scalar Ramp");
////            colormap.setRangeMin(0.0);  //was 5.4
////            colormap.setRangeMax(90.00); //was 81.7
////
////            Color color2 = colormap.getColor(SpectrumStatistics.getWeightedMean(sampleEmergenceAngle));
//            double[] color = new double[3];
////            color[0] = color2.getRed()/255.0;
////            color[1] = color2.getGreen()/255.0;
////            color[2] = color2.getBlue()/255.0;
//            return color;
//        }
//        else
//        {
//            //TODO: What do we do for L3 data here?  It has less XAxis points than the L2 data, so is the coloring scheme different?
//            double[] color = new double[3];
//            for (int i=0; i<3; ++i)
//            {
//                double val = 0.0;
//                if (channelsToColorBy[i] < instrument.getBandCenters().length)
//                {
//                    val = spectrum[channelsToColorBy[i]];
//                }
//                else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
//                    val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
//                else
//                    val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);
//
//                if (val < 0.0)
//                    val = 0.0;
//                else if (val > 1.0)
//                    val = 1.0;
//
//                double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
//                color[i] = slope * (val - channelsColoringMinValue[i]);
//            }
//            return color;
//        }
//    }

    public double getTime()
    {
        return time;
    }




}
