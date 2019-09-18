package edu.jhuapl.sbmt.model.phobos;

import java.io.IOException;

import edu.jhuapl.sbmt.client.ISmallBodyModel;
import edu.jhuapl.sbmt.client.SbmtSpectrumModelFactory;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.interfaces.SpectrumBuilder;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.sbmt.spectrum.rendering.AdvancedSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.rendering.BasicSpectrumRenderer;
import edu.jhuapl.sbmt.spectrum.rendering.IBasicSpectrumRenderer;

public class MEGANESpectraFactory
{

	public static void initializeModels(ISmallBodyModel smallBodyModel)
	{
		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> meganeSpectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
		{

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument) throws IOException
			{
				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return spectrum;
			}

			@Override
			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
					BasicSpectrumInstrument instrument, String timeString) throws IOException
			{
				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return spectrum;
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(BasicSpectrum spectrum, ISmallBodyModel smallBodyModel) throws IOException
			{
				return new BasicSpectrumRenderer(spectrum, smallBodyModel, false);
			}

			@Override
			public IBasicSpectrumRenderer buildSpectrumRenderer(String path, ISmallBodyModel smallBodyModel, BasicSpectrumInstrument instrument) throws IOException
			{
				MEGANESpectrum spectrum = new MEGANESpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel.getBoundingBoxDiagonalLength(), instrument);
				return new AdvancedSpectrumRenderer(spectrum, smallBodyModel, false);
			}
		};
		SbmtSpectrumModelFactory.registerModel("MEGANE", meganeSpectra, smallBodyModel);
		SpectraTypeFactory.registerSpectraType("MEGANE", MEGANEQuery.getInstance(), MEGANESpectrumMath.getInstance(), "cm^-1", new MEGANE().getBandCenters());
	}

}
