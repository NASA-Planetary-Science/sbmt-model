package edu.jhuapl.sbmt.model.phobos;

public class MEGANESpectraFactory
{

//	public static void initializeModels(ISmallBodyModel smallBodyModel)
//	{
//		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> meganeSpectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
//		{
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument) throws IOException
//			{
//				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return spectrum;
//			}
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument, String timeString) throws IOException
//			{
//				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return spectrum;
//			}
//
//			@Override
//			public IBasicSpectrumRenderer buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel, boolean headless) throws IOException
//			{
//				return new BasicSpectrumRenderer(spectrum, smallBodyModel, headless);
//			}
//
//			@Override
//			public IBasicSpectrumRenderer buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument, boolean headless) throws IOException
//			{
//				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
//				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, headless);
//			}
//		};
//		SbmtSpectrumModelFactory.registerModel("MEGANE", meganeSpectra, smallBodyModel);
//		SpectraTypeFactory.registerSpectraType("MEGANE", MEGANEQuery.getInstance(), MEGANESpectrumMath.getInstance(), "cm^-1", new MEGANE().getBandCenters());
//	}
}
