package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.client.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.model.ryugu.nirs3.atRyugu.NIRS3Spectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.SpectrumBuilder;
import edu.jhuapl.sbmt.spectrum.rendering.AdvancedSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.rendering.IBasicSpectrumRenderer;

public class H2SpectraFactory
{

	public static void initializeModels(ISmallBodyModel smallBodyModel)
	{
		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> nirs3Spectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
		{

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument) throws IOException
			{
				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, smallBodyModel, instrument);
				return spectrum;
			}

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument, String timeString) throws IOException
			{
				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, smallBodyModel, instrument);
				return spectrum;
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel) throws IOException
			{
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, false);
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument) throws IOException
			{
				System.out.println(
						"H2SpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrumRenderer: building");
				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, smallBodyModel, instrument);
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, false);
			}
		};
		SbmtSpectrumModelFactory.registerModel("NIRS3", nirs3Spectra, smallBodyModel);

		SpectraTypeFactory.registerSpectraType("NIRS3", NIRS3Query.getInstance(), NIRS3SpectrumMath.getInstance(), "cm^-1", new NIRS3().getBandCenters());
	}

}
