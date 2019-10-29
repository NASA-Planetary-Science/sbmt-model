package edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.model.ryugu.nirs3.NIRS3;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.InstrumentMetadata;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;


public class NIRS3Spectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    ISmallBodyModel smallBodyModel;
    private double utcStart;
	private double utcMid;
	private double utcEnd;
	private double[] geoFields;

    public NIRS3Spectrum(String filename, ISmallBodyModel smallBodyModel,
    		BasicSpectrumInstrument instrument) throws IOException
    {
    	this(filename, smallBodyModel, instrument, false, false);
    }

    public NIRS3Spectrum(String filename, ISmallBodyModel smallBodyModel,
            BasicSpectrumInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification();
        instrumentMetadata = specIO.getInstrumentMetadata("NIRS3");
        this.smallBodyModel = smallBodyModel;
    }

    @Override
    public void saveSpectrum(File file) throws IOException
    {
    	new NIRS3SpectrumWriter(file.getAbsolutePath(), this).write();
    	File infoFile;
    	if (isCustomSpectra)
    		infoFile = new File(getInfoFilePathOnServer());
    	else
    		infoFile = FileCache.getFileFromServer(getInfoFilePathOnServer());
        FileChannel src = new FileInputStream(infoFile).getChannel();
        File infoFileDestination = new File(file.getParentFile() + File.separator + file.getName() + ".INFO");
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

        // 1 double[] ul, // ordering is from
        // smallBodyModel.computeFrustumIntersection(spacecraftPosition,
        // frustum1, frustum2, frustum3, frustum4);
        // 2 double[] ur,
        // 3 double[] lr,
        // 4 double[] ll)

    }

    public void readSpectrumFromFile()
    {
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
    	System.out.println("NISSpectrum: getDataName: getting data name " + spec.getDataName());
    	return spec.getDataName();
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

//    @Override
//    public double[] getChannelColor()
//    {
//        if (coloringStyle == SpectrumColoringStyle.EMISSION_ANGLE)
//        {
//        	AdvancedSpectrumRenderer renderer = new AdvancedSpectrumRenderer(this, smallBodyModel, false);
//            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(renderer, new Vector3D(spacecraftPosition));
//            Colormap colormap = Colormaps.getNewInstanceOfBuiltInColormap("OREX Scalar Ramp");
//            colormap.setRangeMin(0.0);  //was 5.4
//            colormap.setRangeMax(90.00); //was 81.7
//
//            Color color2 = colormap.getColor(SpectrumStatistics.getWeightedMean(sampleEmergenceAngle));
//                    double[] color = new double[3];
//            color[0] = color2.getRed()/255.0;
//            color[1] = color2.getGreen()/255.0;
//            color[2] = color2.getBlue()/255.0;
//            return color;
//        }
//        else
//        {
//	        double[] color = new double[3];
//	        for (int i=0; i<3; ++i)
//	        {
//	            double val = 0.0;
//	            if (channelsToColorBy[i] < instrument.getBandCenters().length)
//	            {
//	                val = spectrum[channelsToColorBy[i]];
//	            }
//	            else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
//	            {
//	                val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
//	            }
//	            else
//	            {
//	                val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);
//	            }
//	            if (val < 0.0)
//	                val = 0.0;
//	            else if (val > 1.0)
//	                val = 1.0;
//
//	            double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
//	            color[i] = slope * (val - channelsColoringMinValue[i]);
//	        }
//	        return color;
//        }
//    }
}
