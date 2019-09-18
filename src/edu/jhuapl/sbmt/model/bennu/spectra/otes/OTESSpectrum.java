package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumReader;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumWriter;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
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


public class OTESSpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectrumInstrumentMetadataIO specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    double boundingBoxDiagonalLength;
    boolean headless;

    private static final Key<OTESSpectrum> OTESSPECTRUM_KEY = Key.of("OREXSpectrum");
	private static final Key<String> FILENAME_KEY = Key.of("fileName");
	private static final Key<SpectrumInstrumentMetadataIO> SPECIO_KEY = Key.of("spectrumIO");
	private static final Key<Double> BOUNDINGBOX_KEY = Key.of("boundingBoxDiagonalLength");
	private static final Key<BasicSpectrumInstrument> SPECTRALINSTRUMENT_KEY = Key.of("spectralInstrument");
	private static final Key<Boolean> HEADLESS_KEY = Key.of("headless");
	private static final Key<Boolean> ISCUSTOM_KEY = Key.of("isCustom");


    public static void initializeSerializationProxy()
	{
    	InstanceGetter.defaultInstanceGetter().register(OTESSPECTRUM_KEY, (source) -> {

    		String filename = source.get(FILENAME_KEY);
    		ProvidesGenericObjectFromMetadata<SpectrumInstrumentMetadataIO> specIOMetadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(SPECIO_KEY);
    		SpectrumInstrumentMetadataIO specIO = specIOMetadata.provide(source);
    		double boundingBoxDiagonalLength = source.get(BOUNDINGBOX_KEY);
    		Boolean headless = source.get(HEADLESS_KEY);
    		Boolean isCustom = source.get(ISCUSTOM_KEY);

    		OTESSpectrum spec = null;
			try
			{
				spec = new OTESSpectrum(filename, specIO, boundingBoxDiagonalLength, new OTES(), headless, isCustom);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return spec;

    	}, OTESSpectrum.class, spec -> {

    		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
    		result.put(FILENAME_KEY, spec.serverpath);
    		result.put(SPECIO_KEY, spec.specIO);
    		result.put(BOUNDINGBOX_KEY, spec.boundingBoxDiagonalLength);
    		result.put(HEADLESS_KEY, spec.headless);
    		result.put(ISCUSTOM_KEY, spec.isCustomSpectra);
    		return result;
    	});

	}

    public OTESSpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument) throws IOException
    {
        this(filename, specIO, boundingBoxDiagonalLength, instrument, false, false);
    }

    public OTESSpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = specIO;
        instrumentMetadata = specIO.getInstrumentMetadata("OTES");
        this.boundingBoxDiagonalLength = boundingBoxDiagonalLength;
        double dx = MathUtil.vnorm(spacecraftPosition) + boundingBoxDiagonalLength; //smallBodyModel.getBoundingBoxDiagonalLength();
        toSunVectorLength=dx;
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
        new OTESSpectrumWriter(file.getAbsolutePath(), this).write();
        File infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        FileChannel src = new FileInputStream(infoFile).getChannel();
        File infoFileDestination = new File(file.getParentFile() + File.separator + file.getName() + ".INFO");
        FileChannel dest = new FileOutputStream(infoFileDestination).getChannel();
        dest.transferFrom(src, 0, src.size());
        src.close();
        dest.close();
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

    @Override
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
        reader.read();

        Vector3D origin = new Vector3D(reader.getSpacecraftPosition());
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
    }

    @Override
    public void readSpectrumFromFile()
    {
    	OTESSpectrumReader reader = null;
        if (!isCustomSpectra)
        {
            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
            reader=new OTESSpectrumReader(spectrumFile.getAbsolutePath(), getNumberOfBands());
        }
        else
        {
            reader=new OTESSpectrumReader(getLocalSpectrumFilePathOnServer(), getNumberOfBands());
        }
        reader.read();

        spectrum=reader.getData();
        xData = reader.getXAxis();
        time = reader.getSclk();
    }

    @Override
    public int getNumberOfBands()
    {
        if (FilenameUtils.getExtension(serverpath.toString()).equals("spect"))
            return OTES.bandCentersLength;
        else
            return 208;
    }

    @Override
    public String getxAxisUnits()
    {
        return spec.getxAxisUnits();
    }

    @Override
    public String getyAxisUnits()
    {
        return spec.getyAxisUnits();
    }

    @Override
    public String getDataName()
    {
    	return spec.getDataName();
    }

//    @Override
//    public double[] getChannelColor()
//    {
//        if (coloringStyle == SpectrumColoringStyle.EMISSION_ANGLE)
//        {
//            //This calculation is using the average emission angle over the spectrum, which doesn't exacty match the emission angle of the
//            //boresight - no good way to calculate this data at the moment.  Olivier said this is fine.  Need to present a way to either have this option or the old one via RGB for coloring
//        	AdvancedSpectrumRenderer renderer = new AdvancedSpectrumRenderer(this, smallBodyModel, false);
//            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(renderer, new Vector3D(spacecraftPosition));
//            Colormap colormap = Colormaps.getNewInstanceOfBuiltInColormap("OREX Scalar Ramp");
//            colormap.setRangeMin(0.0);  //was 5.4
//            colormap.setRangeMax(90.00); //was 81.7
//
//            Color color2 = colormap.getColor(SpectrumStatistics.getWeightedMean(sampleEmergenceAngle));
//            double[] color = new double[3];
//            color[0] = color2.getRed()/255.0;
//            color[1] = color2.getGreen()/255.0;
//            color[2] = color2.getBlue()/255.0;
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
