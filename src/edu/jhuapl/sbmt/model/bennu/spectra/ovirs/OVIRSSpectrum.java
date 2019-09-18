package edu.jhuapl.sbmt.model.bennu.spectra.ovirs;

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
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io.OVIRSSpectrumReader;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.io.OVIRSSpectrumWriter;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.InstrumentMetadata;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;


public class OVIRSSpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    String time;
    String extension = "";
    double boresightLatDeg, boresightLonDeg;
    double[] calibratedRadianceUncertainty;
    Vector3D boresightIntercept;
    private SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;

    public OVIRSSpectrum(String filename, SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO, double boundingBoxDiagonalLength,
    		BasicSpectrumInstrument instrument) throws IOException
    {
    	this(filename, specIO, boundingBoxDiagonalLength, instrument, false, false);
    }

    public OVIRSSpectrum(String filename, SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO, double boundingBoxDiagonalLength,
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
        boresightIntercept = reader.getBoresightIntercept();
    }

    @Override
    public int getNumberOfBands()
    {
        return OVIRS.bandCentersLength;
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
////        	AdvancedSpectrumRenderer renderer = new AdvancedSpectrumRenderer(this, smallBodyModel, false);
////            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(renderer, new Vector3D(spacecraftPosition));
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
//            double[] color = new double[3];
//            for (int i=0; i<3; ++i)
//            {
//                double val = 0.0;
//                if (channelsToColorBy[i] < instrument.getBandCenters().length)
//                    val = spectrum[channelsToColorBy[i]];
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

    public String getTime()
    {
        return time;
    }

    public Vector3D getBoresightIntercept()
    {
        return boresightIntercept;
    }

}
