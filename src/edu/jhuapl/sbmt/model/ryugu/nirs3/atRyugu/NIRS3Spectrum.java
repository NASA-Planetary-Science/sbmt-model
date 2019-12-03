package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

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
import org.joda.time.DateTime;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumIOException;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.InstrumentMetadata;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.ProvidesGenericObjectFromMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

/**
 * Represents the data that makes up an NIRS3 Spectrum.  Includes ability to read spectrum and pointing information, as
 * well as saving the spectrum to disk.
 * @author steelrj1
 *
 */
public class NIRS3Spectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectrumInstrumentMetadataIO specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    private double utcStart;
	private double utcMid;
	private double utcEnd;
	private double[] geoFields;
    double boundingBoxDiagonalLength;
    boolean headless;

    //Metadata Information
    private static final Key<NIRS3Spectrum> NIRS3SPECTRUM_KEY = Key.of("NIRS3Spectrum");
	private static final Key<String> FILENAME_KEY = Key.of("fileName");
	private static final Key<SpectrumInstrumentMetadataIO> SPECIO_KEY = Key.of("spectrumIO");
	private static final Key<Double> BOUNDINGBOX_KEY = Key.of("boundingBoxDiagonalLength");
	private static final Key<BasicSpectrumInstrument> SPECTRALINSTRUMENT_KEY = Key.of("spectralInstrument");
	private static final Key<Boolean> HEADLESS_KEY = Key.of("headless");
	private static final Key<Boolean> ISCUSTOM_KEY = Key.of("isCustom");


    public static void initializeSerializationProxy()
	{
    	InstanceGetter.defaultInstanceGetter().register(NIRS3SPECTRUM_KEY, (source) -> {

    		String filename = source.get(FILENAME_KEY);
    		ProvidesGenericObjectFromMetadata<SpectrumInstrumentMetadataIO> specIOMetadata = InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(SPECIO_KEY);
    		SpectrumInstrumentMetadataIO specIO = specIOMetadata.provide(source);
    		double boundingBoxDiagonalLength = source.get(BOUNDINGBOX_KEY);
    		Boolean headless = source.get(HEADLESS_KEY);
    		Boolean isCustom = source.get(ISCUSTOM_KEY);

    		NIRS3Spectrum spec = null;
			try
			{
				spec = new NIRS3Spectrum(filename, specIO, boundingBoxDiagonalLength, new NIRS3(), headless, isCustom);
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return spec;

    	}, NIRS3Spectrum.class, spec -> {

    		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
    		result.put(FILENAME_KEY, spec.serverpath);
    		result.put(SPECIO_KEY, spec.specIO);
    		result.put(BOUNDINGBOX_KEY, spec.boundingBoxDiagonalLength);
    		result.put(HEADLESS_KEY, spec.headless);
    		result.put(ISCUSTOM_KEY, spec.isCustomSpectra);
    		return result;
    	});

	}

    public NIRS3Spectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument) throws IOException
    {
    	this(filename, specIO, boundingBoxDiagonalLength, instrument, false, false);
    }

    public NIRS3Spectrum(String filename, SpectrumInstrumentMetadataIO specIO, double boundingBoxDiagonalLength,
            BasicSpectrumInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = specIO;
        instrumentMetadata = specIO.getInstrumentMetadata("NIRS3");
        this.boundingBoxDiagonalLength = boundingBoxDiagonalLength;
        double dx = MathUtil.vnorm(spacecraftPosition) + boundingBoxDiagonalLength;
        toSunVectorLength=dx;
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
    	new NIRS3SpectrumWriter(file.getAbsolutePath(), this).write();

    }

    public void saveInfofile(File file) throws IOException
    {
    	File infoFile;
    	if (isCustomSpectra)
    		infoFile = new File(getInfoFilePathOnServer());
    	else
    		infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        FileChannel src = new FileInputStream(infoFile).getChannel();
        File infoFileDestination = new File(file.getParentFile() + File.separator + FilenameUtils.getBaseName(file.getName()) + ".INFO");
        FileChannel dest = new FileOutputStream(infoFileDestination).getChannel();
        dest.transferFrom(src, 0, src.size());
        src.close();
        dest.close();
    }

    protected String getLocalInfoFilePathOnServer()
    {
        return Paths.get(getLocalSpectrumFilePathOnServer()).getParent().resolve(FilenameUtils.getBaseName(getLocalSpectrumFilePathOnServer()) + ".INFO").toString();
    }

    protected String getLocalSpectrumFilePathOnServer()
    {
        return serverpath;
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
  		spec = instrumentMetadata.getSpecs().get(0);
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
    	if (!isCustomSpectra)
            infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        else
            infoFile = new File(getInfoFilePathOnServer());
        //
        InfoFileReader reader = new InfoFileReader(infoFile.getAbsolutePath());
        reader.read();
        //
        Vector3D origin = new Vector3D(reader.getSpacecraftPosition()); //.scalarMultiply(1e-3);
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
        //
        toSunUnitVector = new Vector3D(reader.getSunPosition()).normalize();
        Frustum frustum = new Frustum(origin.toArray(), lookTarget.toArray(),
                boresightUnit.orthogonal().toArray(), fovDeg, fovDeg);
        frustum1 = frustum.ul;
        frustum2 = frustum.ur;
        frustum3 = frustum.lr;
        frustum4 = frustum.ll;
        spacecraftPosition = frustum.origin;

        this.dateTime=new DateTime(reader.getStartTime());
    }

    public void readSpectrumFromFile() throws SpectrumIOException
    {
    	super.readSpectrumFromFile();
    	if (!isCustomSpectra)
            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
        else
            spectrumFile = new File(getSpectrumPathOnServer());
        //
        NIRS3SpectrumReader reader = new NIRS3SpectrumReader(spectrumFile.getAbsolutePath());
        reader.read();

        spectrum=reader.spectra.get(0).getSpectrum();
        xData = new NIRS3().getBandCenters();
        time = reader.spectra.get(0).getEt();
        geoFields = reader.spectra.get(0).geoFields;
        utcStart = reader.spectra.get(0).getUtcStart();
        utcMid = reader.spectra.get(0).getUtcMid();
        utcEnd = reader.spectra.get(0).getUtcEnd();
    }

    @Override
    public String getFullPath()
    {
    	File spectrumFile;
    	if (!isCustomSpectra)
            spectrumFile=FileCache.getFileFromServer(getSpectrumPathOnServer());
        else
            spectrumFile = new File(getSpectrumPathOnServer());
    	this.fullpath = spectrumFile.getAbsolutePath();
        return fullpath;
    }

    @Override
    public int getNumberOfBands()
    {
        return NIRS3.bandCentersLength;
    }

	public double getTime()
	{
		return time;
	}

	public double getUtcStart()
	{
		return utcStart;
	}

	public double getUtcMid()
	{
		return utcMid;
	}

	public double getUtcEnd()
	{
		return utcEnd;
	}

	public double[] getGeoFields()
	{
		return geoFields;
	}

	 @Override
	    public HashMap<String, String> getProperties() throws IOException
	    {
	        HashMap<String, String> properties = new LinkedHashMap<String, String>();

	        if (this.fullpath == null) getFullPath();
	        String name = new File(this.fullpath).getName();
	        properties.put("Name", name.substring(0, name.length()-4));

	        properties.put("Date", dateTime.toString());

	        // Note \u00B0 is the unicode degree symbol
	        String deg = "\u00B0";
//	        properties.put("Minimum Incidence", Double.toString(minIncidence)+deg);
//	        properties.put("Maximum Incidence", Double.toString(maxIncidence)+deg);
//	        properties.put("Minimum Emission", Double.toString(minEmission)+deg);
//	        properties.put("Maximum Emission", Double.toString(maxIncidence)+deg);
//	        properties.put("Minimum Phase", Double.toString(minPhase)+deg);
//	        properties.put("Maximum Phase", Double.toString(maxPhase)+deg);
	//
//	        properties.put("Range", this.range + " km");
	        properties.put("Spacecraft Position (km)",
	                spacecraftPosition[0] + " " + spacecraftPosition[1] + " " + spacecraftPosition[2]);

	        return properties;
	    }
}
