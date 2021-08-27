package edu.jhuapl.sbmt.model.bennu.spectra;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.client.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTES;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESQuery;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrum;
import edu.jhuapl.sbmt.model.bennu.spectra.otes.OTESSpectrumMath;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRS;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSQuery;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrum;
import edu.jhuapl.sbmt.model.bennu.spectra.ovirs.OVIRSSpectrumMath;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.IBasicSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.SpectrumBuilder;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.sbmt.spectrum.rendering.AdvancedSpectrumRenderer;

/**
 * Registers spectrum builders with the core system, as well as spectra types
 * @author steelrj1
 *
 */
public class OREXSpectraFactory
{

	public static void initializeModels(ISmallBodyModel smallBodyModel)
	{
		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> otesSpectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
		{

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument) throws IOException
			{
//				System.out.println(
//						"OREXSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrum: path is " + path);
				OTESSpectrum spectrum = new OTESSpectrum(!(path.startsWith("bennu/shared/otes") || path.startsWith("/bennu/shared/otes")) ? "bennu/shared/otes/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return spectrum;
			}

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument, String timeString) throws IOException
			{
//				System.out.println(
//						"OREXSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrum: path is " + path);
				OTESSpectrum spectrum = new OTESSpectrum(!(path.startsWith("bennu/shared/otes") || path.startsWith("/bennu/shared/otes")) ? "bennu/shared/otes/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				spectrum.setDateTime(new DateTime(Long.parseLong(timeString), DateTimeZone.UTC));
				return spectrum;
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel, boolean headless) throws IOException
			{
//				System.out.println(
//						"OREXSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrumRenderer: path is " + spectrum.getServerpath());
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument, boolean headless) throws IOException
			{
//				System.out.println(
//						"OREXSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrumRenderer: path is " + path);
				OTESSpectrum spectrum = new OTESSpectrum(!(path.startsWith("bennu/shared/otes") || path.startsWith("/bennu/shared/otes")) ? "bennu/shared/otes/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
			}
		};
		SbmtSpectrumModelFactory.registerModel("OTES", otesSpectra, smallBodyModel);

		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> ovirsSpectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
		{

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument) throws IOException
			{
				OVIRSSpectrum spectrum = new OVIRSSpectrum(!(path.startsWith("bennu/shared/ovirs") || path.startsWith("/bennu/shared/ovirs")) ? "bennu/shared/ovirs/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return spectrum;
			}

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument, String timeString) throws IOException
			{
				OVIRSSpectrum spectrum = new OVIRSSpectrum(!(path.startsWith("bennu/shared/ovirs") || path.startsWith("/bennu/shared/ovirs")) ? "bennu/shared/ovirs/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				spectrum.setDateTime(new DateTime(Long.parseLong(timeString)));
				return spectrum;
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel, boolean headless) throws IOException
			{
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument, boolean headless) throws IOException
			{
				OVIRSSpectrum spectrum = new OVIRSSpectrum(!(path.startsWith("bennu/shared/ovirs") || path.startsWith("/bennu/shared/ovirs")) ? "bennu/shared/ovirs/" + path : path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
			}
		};
		SbmtSpectrumModelFactory.registerModel("OVIRS", ovirsSpectra, smallBodyModel);

		SpectraTypeFactory.registerSpectraType("OTES", OTESQuery.getInstance(), OTESSpectrumMath.getInstance(), "cm^-1", new OTES().getBandCenters());
		SpectraTypeFactory.registerSpectraType("OVIRS", OVIRSQuery.getInstance(), OVIRSSpectrumMath.getInstance(), "um", new OVIRS().getBandCenters());
	}

}
