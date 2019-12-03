package edu.jhuapl.sbmt.model.image;

import edu.jhuapl.saavtk.config.TypedLookup;
import edu.jhuapl.sbmt.config.SessionConfiguration;
import edu.jhuapl.sbmt.imaging.instruments.ImagingInstrumentConfiguration;
import edu.jhuapl.sbmt.query.QueryBase;

public class BasicImagingInstrument extends ImagingInstrument
{
    public static ImagingInstrument of(TypedLookup bodyConfiguration) {
        ImagingInstrumentConfiguration configuration = bodyConfiguration.get(SessionConfiguration.IMAGING_INSTRUMENT_CONFIG);

        SpectralImageMode spectralMode = configuration.get(ImagingInstrumentConfiguration.SPECTRAL_MODE);
        QueryBase searchQuery = configuration.get(ImagingInstrumentConfiguration.QUERY_BASE);
        ImageType type = configuration.get(ImagingInstrumentConfiguration.IMAGE_TYPE);
        ImageSource[] searchImageSources = configuration.get(ImagingInstrumentConfiguration.IMAGE_SOURCE);
        Instrument instrument = configuration.get(ImagingInstrumentConfiguration.INSTRUMENT);

        return new ImagingInstrument(spectralMode, searchQuery, type, searchImageSources, instrument);
    }

//  protected BasicImagingInstrument(SpectralMode spectralMode, QueryBase searchQuery, ImageType type, ImageSource[] searchImageSources, Instrument instrument)
//  {
//      super(spectralMode, searchQuery, type, searchImageSources, instrument);
//  }

}
