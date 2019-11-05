package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

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
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io.OVIRSSpectrumReader;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io.OVIRSSpectrumWriter;
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


public class OVIRSSpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
//    double boresightLatDeg, boresightLonDeg;
//    double[] calibratedRadianceUncertainty;
//    Vector3D boresightIntercept;
    private SpectrumInstrumentMetadataIO specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    double boundingBoxDiagonalLength;
    boolean headless;


    //Metadata Information
    private static final Key<OVIRSSpectrum> OVIRSSPECTRUM_KEY = Key.of("OVIRSSpectrum");
	private static final Key<String> FILENAME_KEY = Key.of("fileName");
	private static final Key<SpectrumInstrumentMetadataIO> SPECIO_KEY = Key.of("spectrumIO");
	private static final Key<Double> BOUNDINGBOX_KEY = Key.of("boundingBoxDiagonalLength");
	private static final Key<BasicSpectrumInstrument> SPECTRALINSTRUMENT_KEY = Key.of("spectralInstrument");
	private static final Key<Boolean> HEADLESS_KEY = Key.of("headless");
	private static final Key<Boolean> ISCUSTOM_KEY = Key.of("isCustom");


    public static void initializeSerializationProxy()
	{
    	InstanceGetter.defaultInstanceGetter().register(OVIRSSPECTRUM_KEY, (source) -> {

    		String filename = source.get(FILENAME_KEY);
    		ProvidesGenericObjectFromMetadata<SpectrumInstrumentMetadataIO> specIOMetadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(SPECIO_KEY);
    		SpectrumInstrumentMetadataIO specIO = specIOMetadata.provide(source);
    		double boundingBoxDiagonalLength = source.get(BOUNDINGBOX_KEY);
    		Boolean headless = source.get(HEADLESS_KEY);
    		Boolean isCustom = source.get(ISCUSTOM_KEY);

    		OVIRSSpectrum spec = null;
			try
			{
				spec = new OVIRSSpectrum(filename, specIO, boundingBoxDiagonalLength, new OVIRS(), headless, isCustom);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return spec;

    	}, OVIRSSpectrum.class, spec -> {

    		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
    		result.put(FILENAME_KEY, spec.serverpath);
    		result.put(SPECIO_KEY, spec.specIO);
    		result.put(BOUNDINGBOX_KEY, spec.boundingBoxDiagonalLength);
    		result.put(HEADLESS_KEY, spec.headless);
    		result.put(ISCUSTOM_KEY, spec.isCustomSpectra);
    		return result;
    	});

	}

    /**
     * @param filename 						Filename of the spectrum on the server
     * @param specIO						The spectrum metadata object
     * @param boundingBoxDiagonalLength		The diagonal length of the bounding box
     * @param instrument					The spectral instrument
     * @throws IOException
     */
    public OVIRSSpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument) throws IOException
    {
    	this(filename, specIO, boundingBoxDiagonalLength, instrument, false, false);
    }

    /**
     * @param filename						Filename of the spectrum on the server
     * @param specIO						The spectrum metadata object
     * @param boundingBoxDiagonalLength		The diagonal length of the bounding box
     * @param instrument					The spectral instrument
     * @param headless						Boolean describing whether this is loaded on the server or not
     * @param isCustom						Boolean descibing whether this is a custom spectra
     * @throws IOException
     */
    public OVIRSSpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = specIO;
        instrumentMetadata = specIO.getInstrumentMetadata("OVIRS");
        if (serverpath.contains("l3esci_reff"))
    		spec = instrumentMetadata.getSpecs().get(0);
    	else if (serverpath.contains("l3csci"))
    		spec = instrumentMetadata.getSpecs().get(1);
    	else if (serverpath.contains("l3esci_radf"))
    		spec = instrumentMetadata.getSpecs().get(2);
    	double dx = MathUtil.vnorm(spacecraftPosition) + boundingBoxDiagonalLength;
        toSunVectorLength=dx;
    }

    /**
     * Saves the spectrum and associated pointing information to 2 distinct files
     */
    @Override
    public void saveSpectrum(File file) throws IOException
    {
        new OVIRSSpectrumWriter(file.getAbsolutePath(), this).write();
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
    	if (serverpath.contains("l3esci_reff"))
    		spec = instrumentMetadata.getSpecs().get(0);
    	else if (serverpath.contains("l3csci"))
    		spec = instrumentMetadata.getSpecs().get(1);
    	else
    		spec = instrumentMetadata.getSpecs().get(2);

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
        //
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

//        this.dateTime=new DateTime(reader.getStartTime());
    }

    public void readSpectrumFromFile()
    {
    	super.readSpectrumFromFile();
        OVIRSSpectrumReader reader = null;
        if (!isCustomSpectra)
        {
            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
            reader=new OVIRSSpectrumReader(spectrumFile.getAbsolutePath());
        }
        else
        {
            reader=new OVIRSSpectrumReader(getLocalSpectrumFilePathOnServer());
        }
        reader.read();

        xData = reader.getXAxis();
        spectrum=reader.getData();
        time = reader.getSclk();
//        boresightIntercept = reader.getBoresightIntercept();
    }

    @Override
    public int getNumberOfBands()
    {
        return OVIRS.bandCentersLength;
    }

    public double getTime()
    {
        return time;
    }

//    public Vector3D getBoresightIntercept()
//    {
//        return boresightIntercept;
//    }

}
