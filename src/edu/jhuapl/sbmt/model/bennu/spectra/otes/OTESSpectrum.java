package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.FileUtil;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumReader;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumWriter;
import edu.jhuapl.sbmt.pointing.io.InfoFileReader;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumIOException;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.InstrumentMetadata;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.ProvidesGenericObjectFromMetadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;


/**
 * Represents the data that makes up an OTES Spectrum.  Includes ability to read spectrum and pointing information, as
 * well as saving the spectrum to disk.
 * @author steelrj1
 *
 */
public class OTESSpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectrumInstrumentMetadataIO specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    double boundingBoxDiagonalLength;
    boolean headless;

    //Metadata Information
    private static final Key<OTESSpectrum> OTESSPECTRUM_KEY = Key.of("OTESSpectrum");
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

    /**
     * @param filename 						Filename of the spectrum on the server
     * @param specIO						The spectrum metadata object
     * @param boundingBoxDiagonalLength		The diagonal length of the bounding box
     * @param instrument					The spectral instrument
     * @throws IOException
     */
    public OTESSpectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
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

    /**
     * Saves the spectrum and associated pointing information to 2 distinct files
     */
    @Override
    public void saveSpectrum(File file) throws IOException
    {
        new OTESSpectrumWriter(file.getAbsolutePath(), this).write();
        saveInfofile(file);
    }

    public void saveInfofile(File file) throws IOException
    {
    	try
    	{
	    	File infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
	        FileChannel src = new FileInputStream(infoFile).getChannel();
	        File infoFileDestination = new File(file.getParentFile() + File.separator + FilenameUtils.getBaseName(file.getName()) + ".INFO");
	        FileChannel dest = new FileOutputStream(infoFileDestination).getChannel();
	        dest.transferFrom(src, 0, src.size());
	        src.close();
	        dest.close();
    	}
    	catch (RuntimeException rte)
    	{
    		File cachedFile = new File(getFullPath());
    		File cachedInfoFile = new File(cachedFile.getParentFile(),
					FilenameUtils.getBaseName(cachedFile.getAbsolutePath()) + ".INFO");
			File toInfoFilename = new File(file.getParentFile(),
					FilenameUtils.getBaseName(file.getAbsolutePath()) + ".INFO");
			FileUtil.copyFile(cachedInfoFile, toInfoFilename);
    	}
    }

    private String getLocalInfoFilePathOnServer()
    {
    	String normalpath = SafeURLPaths.instance().getString(serverpath);
    	return FilenameUtils.removeExtension(normalpath) + ".INFO";
    }

    private String getLocalSpectrumFilePathOnServer()
    {
        return SafeURLPaths.instance().getString(serverpath);
    }

    private String getInfoFilePathOnServer()
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

    /**
     *	Returns the path of the spectrum
     */
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

    /**
     * Reads the pointing information for this spectra
     */
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

    /**
     * Reads the spectrum data from the file
     * @throws SpectrumIOException
     */
    @Override
    public void readSpectrumFromFile() throws SpectrumIOException
    {
    	super.readSpectrumFromFile();
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

    /**
     *	Returns the number of bands for this spectrum
     */
    @Override
    public int getNumberOfBands()
    {
        if (FilenameUtils.getExtension(serverpath.toString()).equals("spect"))
            return OTES.bandCentersLength;
        else
            return 208;
    }

    /**
     * Returns the doulbe
     * @return
     */
    public double getTime()
    {
        return time;
    }

    @Override
    public HashMap<String, String> getProperties() throws IOException
    {
        HashMap<String, String> properties = new LinkedHashMap<String, String>();

//        if (this.fullpath == null) getFullPath();
        String name = new File(serverpath).getName();
        properties.put("Name", name.substring(0, name.length()-4));

//        properties.put("Date", dateTime.toString());

//        properties.put("Day of Year", (new File(this.fullpath)).getParentFile().getName());

        //properties.put("Year", (new File(this.fullpath)).getParentFile().getParentFile().getName());

//        properties.put("MET", (new File(this.fullpath)).getName().substring(2,11));

//        properties.put("Duration", Double.toString(duration) + " seconds");

//        String polygonTypeStr = "Missing value";
//        switch(this.polygon_type_flag)
//        {
//        case 0:
//            polygonTypeStr = "Full (all vertices on shape)";
//            break;
//        case 1:
//            polygonTypeStr = "Partial (single contiguous set of vertices on shape)";
//            break;
//        case 2:
//            polygonTypeStr = "Degenerate (multiple contiguous sets of vertices on shape)";
//            break;
//        case 3:
//            polygonTypeStr = "Empty (no vertices on shape)";
//            break;
//        }
//        properties.put("Polygon Type", polygonTypeStr);

        // Note \u00B0 is the unicode degree symbol
        String deg = "\u00B0";
//        properties.put("Minimum Incidence", Double.toString(minIncidence)+deg);
//        properties.put("Maximum Incidence", Double.toString(maxIncidence)+deg);
//        properties.put("Minimum Emission", Double.toString(minEmission)+deg);
//        properties.put("Maximum Emission", Double.toString(maxIncidence)+deg);
//        properties.put("Minimum Phase", Double.toString(minPhase)+deg);
//        properties.put("Maximum Phase", Double.toString(maxPhase)+deg);
//
//        properties.put("Range", this.range + " km");
        properties.put("Spacecraft Position (km)",
                spacecraftPosition[0] + " " + spacecraftPosition[1] + " " + spacecraftPosition[2]);

        return properties;
    }
}
