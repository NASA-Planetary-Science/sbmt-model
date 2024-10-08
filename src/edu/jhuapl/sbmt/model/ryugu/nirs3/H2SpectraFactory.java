package edu.jhuapl.sbmt.model.ryugu.nirs3;

/**
 * Registers spectrum builder with the core system, as well as spectrum type
 * @author steelrj1
 *
 */
public class H2SpectraFactory
{

//	public static void initializeModels(ISmallBodyModel smallBodyModel)
//	{
//		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> nirs3Spectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
//		{
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument) throws IOException
//			{
//				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return spectrum;
//			}
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument, String timeString) throws IOException
//			{
//				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return spectrum;
//			}
//
//			@Override
//			public IBasicSpectrumRenderer<NIRS3Spectrum> buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel, boolean headless) throws IOException
//			{
//				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
//			}
//
//			@Override
//			public IBasicSpectrumRenderer<NIRS3Spectrum> buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument, boolean headless) throws IOException
//			{
//				NIRS3Spectrum spectrum = new NIRS3Spectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
//			}
//		};
//		SbmtSpectrumModelFactory.registerModel("NIRS3", nirs3Spectra, smallBodyModel);
//
//		SpectraTypeFactory.registerSpectraType("NIRS3", NIRS3Query.getInstance(), NIRS3SpectrumMath.getInstance(), "cm^-1", new NIRS3().getBandCenters());
//	}

}
