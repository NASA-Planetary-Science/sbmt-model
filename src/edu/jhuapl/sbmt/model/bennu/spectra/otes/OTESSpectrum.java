package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.joda.time.DateTime;

import edu.jhuapl.saavtk.colormap.Colormap;
import edu.jhuapl.saavtk.colormap.Colormaps;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Frustum;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.SafeURLPaths;
import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.core.InstrumentMetadata;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumReader;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.io.OTESSpectrumWriter;
import edu.jhuapl.sbmt.model.image.InfoFileReader;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchSpec;
import edu.jhuapl.sbmt.spectrum.model.rendering.AdvancedSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.model.rendering.AdvancedSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.ISpectralInstrument;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.SpectrumColoringStyle;
import edu.jhuapl.sbmt.spectrum.model.statistics.SpectrumStatistics;
import edu.jhuapl.sbmt.spectrum.model.statistics.SpectrumStatistics.Sample;


public class OTESSpectrum extends BasicSpectrum
{
    File infoFile, spectrumFile;
    double time;
    String extension = "";
    private SpectraHierarchicalSearchSpecification<SpectrumSearchSpec> specIO;
    private InstrumentMetadata<SpectrumSearchSpec> instrumentMetadata;
    ISmallBodyModel smallBodyModel;

    public OTESSpectrum(String filename, ISmallBodyModel smallBodyModel,
            ISpectralInstrument instrument) throws IOException
    {
        this(filename, smallBodyModel, instrument, false, false);
    }

    public OTESSpectrum(String filename, ISmallBodyModel smallBodyModel,
            ISpectralInstrument instrument, boolean headless, boolean isCustom) throws IOException
    {
        super(filename, instrument, isCustom);
        extension = FilenameUtils.getExtension(serverpath.toString());
        this.specIO = smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification();
        this.smallBodyModel = smallBodyModel;
        instrumentMetadata = specIO.getInstrumentMetadata("OTES");
        double dx = MathUtil.vnorm(spacecraftPosition) + smallBodyModel.getBoundingBoxDiagonalLength();
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

    @Override
    public double[] getChannelColor()
    {
        if (coloringStyle == SpectrumColoringStyle.EMISSION_ANGLE)
        {
            //This calculation is using the average emission angle over the spectrum, which doesn't exacty match the emission angle of the
            //boresight - no good way to calculate this data at the moment.  Olivier said this is fine.  Need to present a way to either have this option or the old one via RGB for coloring
        	AdvancedSpectrumRenderer renderer = new AdvancedSpectrumRenderer(this, smallBodyModel, false);
            List<Sample> sampleEmergenceAngle = SpectrumStatistics.sampleEmergenceAngle(renderer, new Vector3D(spacecraftPosition));
            Colormap colormap = Colormaps.getNewInstanceOfBuiltInColormap("OREX Scalar Ramp");
            colormap.setRangeMin(0.0);  //was 5.4
            colormap.setRangeMax(90.00); //was 81.7

            Color color2 = colormap.getColor(SpectrumStatistics.getWeightedMean(sampleEmergenceAngle));
                    double[] color = new double[3];
            color[0] = color2.getRed()/255.0;
            color[1] = color2.getGreen()/255.0;
            color[2] = color2.getBlue()/255.0;
            return color;
        }
        else
        {
            //TODO: What do we do for L3 data here?  It has less XAxis points than the L2 data, so is the coloring scheme different?
            double[] color = new double[3];
            for (int i=0; i<3; ++i)
            {
                double val = 0.0;
                if (channelsToColorBy[i] < instrument.getBandCenters().length)
                {
                    val = spectrum[channelsToColorBy[i]];
                }
                else if (channelsToColorBy[i] < instrument.getBandCenters().length + instrument.getSpectrumMath().getDerivedParameters().length)
                    val = evaluateDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length);
                else
                    val = instrument.getSpectrumMath().evaluateUserDefinedDerivedParameters(channelsToColorBy[i]-instrument.getBandCenters().length-instrument.getSpectrumMath().getDerivedParameters().length, spectrum);

                if (val < 0.0)
                    val = 0.0;
                else if (val > 1.0)
                    val = 1.0;

                double slope = 1.0 / (channelsColoringMaxValue[i] - channelsColoringMinValue[i]);
                color[i] = slope * (val - channelsColoringMinValue[i]);
            }
            return color;
        }
    }

    public double getTime()
    {
        return time;
    }
}
