package edu.jhuapl.sbmt.model.eros.nis;

import edu.jhuapl.sbmt.core.body.ISmallBodyModel;

/**
 * Registers spectrum builder for NIS with the core system, as well as spectra type
 * @author steelrj1
 *
 */
public class NEARSpectraFactory
{
	public static void initializeModels(ISmallBodyModel smallBodyModel)
	{
//		SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument> nisSpectra = new SpectrumBuilder<String, ISmallBodyModel, BasicSpectrumInstrument>()
//		{
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument) throws IOException
//			{
//				NISSpectrum spectrum = new NISSpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel, instrument);
////				String str = path;
////	            String strippedFileName = str.substring(str.lastIndexOf("/NIS/2000/") + 10);
////	            System.out.println(
////						"NEARSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrum: stripped file name " + strippedFileName);
////	            String detailedTime = NISSearchModel.nisFileToObservationTimeMap.get(strippedFileName);
////	            System.out.println(
////						"NEARSpectraFactory.initializeModels(...).new SpectrumBuilder() {...}: buildSpectrum: detailed time " + detailedTime);
////	            List<String> result = new ArrayList<String>();
////	            result.add(str);
////	            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
////	            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
////	            try
////				{
////					spectrum.setDateTime(new DateTime(sdf.parse(detailedTime).getTime()));
////				}
////	            catch (ParseException e)
////				{
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
//
//				return spectrum;
//			}
//
//			@Override
//			public BasicSpectrum buildSpectrum(String path, ISmallBodyModel smallBodyModel,
//					BasicSpectrumInstrument instrument, String timeString) throws IOException
//			{
//				return buildSpectrum(path, smallBodyModel, instrument);
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
//				NISSpectrum spectrum = new NISSpectrum(path, (SpectrumInstrumentMetadataIO)smallBodyModel.getSmallBodyConfig().getHierarchicalSpectraSearchSpecification(), smallBodyModel, instrument);
//				return new BasicSpectrumRenderer(spectrum, smallBodyModel, headless);
//			}
//
//
//		};
//		SbmtSpectrumModelFactory.registerModel("NIS", nisSpectra, smallBodyModel);
//
//		SpectraTypeFactory.registerSpectraType("NIS", NisQuery.getInstance(), NISSpectrumMath.getSpectrumMath(), "cm^-1", new NIS().getBandCenters());
	}
}
