package edu.jhuapl.sbmt.model.image;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.gui.image.model.custom.CustomCylindricalImageKey;
import edu.jhuapl.sbmt.model.bennu.imaging.MapCamEarthImage;
import edu.jhuapl.sbmt.model.bennu.imaging.MapCamImage;
import edu.jhuapl.sbmt.model.bennu.imaging.MapCamV4Image;
import edu.jhuapl.sbmt.model.bennu.imaging.OcamsFlightImage;
import edu.jhuapl.sbmt.model.bennu.imaging.PolyCamEarthImage;
import edu.jhuapl.sbmt.model.bennu.imaging.PolyCamImage;
import edu.jhuapl.sbmt.model.bennu.imaging.PolyCamV4Image;
import edu.jhuapl.sbmt.model.bennu.imaging.SamCamEarthImage;
import edu.jhuapl.sbmt.model.ceres.FcCeresImage;
import edu.jhuapl.sbmt.model.eros.MSIImage;
import edu.jhuapl.sbmt.model.gaspra.SSIGaspraImage;
import edu.jhuapl.sbmt.model.ida.SSIIdaImage;
import edu.jhuapl.sbmt.model.image.marsmissions.MarsMissionImage;
import edu.jhuapl.sbmt.model.itokawa.AmicaImage;
import edu.jhuapl.sbmt.model.leisa.LEISAJupiterImage;
import edu.jhuapl.sbmt.model.lorri.LorriImage;
import edu.jhuapl.sbmt.model.mathilde.MSIMathildeImage;
import edu.jhuapl.sbmt.model.mvic.MVICQuadJupiterImage;
import edu.jhuapl.sbmt.model.rosetta.OsirisImage;
import edu.jhuapl.sbmt.model.ryugu.onc.ONCImage;
import edu.jhuapl.sbmt.model.ryugu.onc.ONCTruthImage;
import edu.jhuapl.sbmt.model.ryugu.tir.TIRImage;
import edu.jhuapl.sbmt.model.saturnmoon.SaturnMoonImage;
import edu.jhuapl.sbmt.model.vesta.FcImage;

import nom.tam.fits.FitsException;

public class SbmtImageModelFactory
{

	static public Image createImage(
            ImageKeyInterface key,
            List<SmallBodyModel> smallBodyModel,
            boolean loadPointingOnly) throws FitsException, IOException
    {

//        SmallBodyViewConfig config = (SmallBodyViewConfig)smallBodyModel.getSmallBodyConfig();

        if (ImageSource.SPICE.equals(key.getSource()) ||
                ImageSource.GASKELL.equals(key.getSource()) ||
                ImageSource.GASKELL_UPDATED.equals(key.getSource()) ||
                ImageSource.LABEL.equals(key.getSource()) ||
                ImageSource.CORRECTED_SPICE.equals(key.getSource()) ||
                ImageSource.CORRECTED.equals(key.getSource()))
        {
            if (key.getInstrument() != null && key.getInstrument().getSpectralMode() == SpectralImageMode.MULTI)
            {
                if (key.getInstrument().getType() == ImageType.MVIC_JUPITER_IMAGE)
                    return new MVICQuadJupiterImage(key, smallBodyModel, loadPointingOnly);
                else
                    return null;
            }
            else if (key.getInstrument() != null && key.getInstrument().getSpectralMode() == SpectralImageMode.HYPER)
            {
                if (key.getInstrument().getType() == ImageType.LEISA_JUPITER_IMAGE)
                    return new LEISAJupiterImage(key, smallBodyModel, loadPointingOnly);
                else
                    return null;
            }
            else // SpectralMode.MONO
            {
                if (key.getInstrument().getType() == ImageType.MSI_IMAGE)
                    return new MSIImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.AMICA_IMAGE)
                    return new AmicaImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.FC_IMAGE)
                    return new FcImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.FCCERES_IMAGE)
                    return new FcCeresImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.valueOf("MARS_MOON_IMAGE"))
                    return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.valueOf("PHOBOS_IMAGE"))
                    return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.valueOf("DEIMOS_IMAGE"))
                    return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.OSIRIS_IMAGE)
                    return new OsirisImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.SATURN_MOON_IMAGE)
                    return new SaturnMoonImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.SSI_GASPRA_IMAGE)
                    return new SSIGaspraImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.SSI_IDA_IMAGE)
                    return new SSIIdaImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.MSI_MATHILDE_IMAGE)
                    return new MSIMathildeImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.LORRI_IMAGE)
                    return new LorriImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.POLYCAM_V3_IMAGE)
                    return new PolyCamImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.MAPCAM_V3_IMAGE)
                    return new MapCamImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.POLYCAM_V4_IMAGE)
                    return new PolyCamV4Image(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.MAPCAM_V4_IMAGE)
                    return new MapCamV4Image(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.POLYCAM_EARTH_IMAGE)
                    return new PolyCamEarthImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.SAMCAM_EARTH_IMAGE)
                    return new SamCamEarthImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.MAPCAM_EARTH_IMAGE)
                    return new MapCamEarthImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.POLYCAM_FLIGHT_IMAGE)
                    return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.MAPCAM_FLIGHT_IMAGE)
                    return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.SAMCAM_FLIGHT_IMAGE)
                    return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.NAVCAM_FLIGHT_IMAGE)
                    return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.ONC_TRUTH_IMAGE)
                    return new ONCTruthImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.ONC_IMAGE)
                    return new ONCImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.TIR_IMAGE)
                    return new TIRImage(key, smallBodyModel, loadPointingOnly);
                else if (key.getInstrument().getType() == ImageType.GENERIC_IMAGE)
                    return new CustomPerspectiveImage(key, smallBodyModel, loadPointingOnly);
            }
        }
        else if (ImageSource.LOCAL_PERSPECTIVE.equals(key.getSource()))
        {
            if (key.getImageType() == ImageType.MSI_IMAGE)
                return new MSIImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.AMICA_IMAGE)
                return new AmicaImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.FC_IMAGE)
                return new FcImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.FCCERES_IMAGE)
                return new FcCeresImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.valueOf("MARS_MOON_IMAGE"))
                return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.valueOf("PHOBOS_IMAGE"))
                return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.valueOf("DEIMOS_IMAGE"))
                return MarsMissionImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.OSIRIS_IMAGE)
                return new OsirisImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.SATURN_MOON_IMAGE)
                return new SaturnMoonImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.SSI_GASPRA_IMAGE)
                return new SSIGaspraImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.SSI_IDA_IMAGE)
                return new SSIIdaImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MSI_MATHILDE_IMAGE)
                return new MSIMathildeImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.LORRI_IMAGE)
                return new LorriImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.POLYCAM_V3_IMAGE)
                return new PolyCamImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MAPCAM_V3_IMAGE)
                return new MapCamImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.POLYCAM_V4_IMAGE)
                return new PolyCamV4Image(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MAPCAM_V4_IMAGE)
                return new MapCamV4Image(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.POLYCAM_EARTH_IMAGE)
                return new PolyCamEarthImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.SAMCAM_EARTH_IMAGE)
                return new SamCamEarthImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MAPCAM_EARTH_IMAGE)
                return new MapCamEarthImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.POLYCAM_FLIGHT_IMAGE)
                return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MAPCAM_FLIGHT_IMAGE)
                return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.SAMCAM_FLIGHT_IMAGE)
                return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.NAVCAM_FLIGHT_IMAGE)
                return OcamsFlightImage.of(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.GENERIC_IMAGE)
                return new CustomPerspectiveImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.MVIC_JUPITER_IMAGE)
              return new MVICQuadJupiterImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.LEISA_JUPITER_IMAGE)
                return new LEISAJupiterImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.ONC_IMAGE)
                return new ONCImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.ONC_TRUTH_IMAGE)
                return new ONCTruthImage(key, smallBodyModel, loadPointingOnly);
            else if (key.getImageType() == ImageType.TIR_IMAGE)
                return new TIRImage(key, smallBodyModel, loadPointingOnly);
        }
        else if (key instanceof CustomCylindricalImageKey)
        {
            return new CylindricalImage((CustomCylindricalImageKey) key, smallBodyModel);
        }
        System.out.println("SbmtImageModelFactory: createImage: ");
        return new BasicPerspectiveImage(key, smallBodyModel, loadPointingOnly);
    }
}
