package edu.jhuapl.sbmt.model.eros;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import edu.jhuapl.sbmt.util.BackplaneInfo;

import altwg.Fits.HeaderTag;
import altwg.Fits.HeaderTags;
import altwg.util.FitsHeader;
import altwg.util.FitsHeader.FitsHeaderBuilder;
import altwg.util.FitsUtil;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;


/**
 * Contains static methods for interacting with fits files
 *
 * @author espirrc1
 *
 */
public class MSIFits {

  /**
   * Return a list of HeaderCards that are common for all near.msi image products. HeaderCards are populated by the
   * information contained in the FitsHeader object. Fixed value headers are populated by information contained in the
   * Enumeration tags, ex. NAXIS (always assumed to be 3). Taken from the OSIRIS-REx fits generation code.
   * Several O-REX specific fits headers have been commented out.
   *
   * nguyel1:
   * This was modified by espirrc1 from OSIRIS-REx ALTWG's Fits.java for MSI backplanes PDS delivery.
   *
   * @param fitsHdr
   * @return
   * @throws HeaderCardException
   */
  private static List<HeaderCard> getCommonHeaderCards(FitsHeader fitsHdr) throws HeaderCardException {
      List<HeaderCard> headers = new ArrayList<HeaderCard>();

      // TODO need to reference productType in fits Header to determine which
      // keyword to display

      headers.add(fitsHdr.getHeaderCard(HeaderTag.HDRVERS));

      // mission information
      headers.add(new HeaderCard("COMMENT", "Mission Information", false));
      headers.add(new HeaderCard(HeaderTag.MISSION.toString(), "NEAR EARTH ASTEROID RENDEZVOUS",
              HeaderTag.MISSION.comment()));
      headers.add(new HeaderCard(HeaderTag.HOSTNAME.toString(), "NEAR",
              HeaderTag.HOSTNAME.comment()));

//      headers.add(fitsHdr.getHeaderCard(HeaderTags.TARGET));
      headers.add(new HeaderCard(HeaderTag.TARGET.toString(), "EROS",  HeaderTag.HOSTNAME.comment()));
//      headers.add(fitsHdr.getHeaderCard(HeaderTags.ORIGIN));

      // observation information
      headers.add(new HeaderCard("COMMENT", "Observation Information", false));
//      headers.add(fitsHdr.getHeaderCard(HeaderTags.MPHASE));

      // data source
      headers.add(new HeaderCard("COMMENT", "Data Source", false));
 //     headers.add(fitsHdr.getHeaderCard(HeaderTags.DATASRC));
      headers.add(fitsHdr.getHeaderCard(HeaderTag.DATASRCF));
//      headers.add(fitsHdr.getHeaderCard(HeaderTags.DATASRCV));

      // timing information
//      headers.add(new HeaderCard("COMMENT", "Timing Information", false));
//      headers.add(fitsHdr.getHeaderCard(HeaderTags.DATASRCD));

      // DateFormat dateFormat = new
      // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssssZ");
      // date-time terminates in 'Z'
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      Date date = new Date();

      headers.add(new HeaderCard(HeaderTag.DATEPRD.toString(), dateFormat.format(date), null));

      // processing information
      headers.add(new HeaderCard("COMMENT", "Processing Information", false));
      headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODNAME));
      headers.add(fitsHdr.getHeaderCard(HeaderTag.PRODVERS));
//      headers.add(fitsHdr.getHeaderCard(HeaderTags.CREATOR));

      return headers;
  }

  public static void saveFitsCube(double[][][] data, String outfile,
         Map<String, HeaderCard> prevValues) throws FitsException, IOException {

    //grab number of planes in data cube and check to see if number of planes in the cube
    //matches the number of planes we expect.
    int numPlanes = data.length;
    if (numPlanes != BackplaneInfo.values().length) {
      System.out.println("ERROR! The data cube has " + numPlanes + " planes while we were expecting "
        + BackplaneInfo.values().length + " planes! Something is wrong! Stopping with error!");
      System.exit(1);
    }
    // create the list of planes in the exact order they are written to the fits file
    List<BackplaneInfo> planeList = new ArrayList<BackplaneInfo>();
    for (BackplaneInfo thisPlane : BackplaneInfo.values()) {
      planeList.add(thisPlane);
    }

    savePlanes2Fits(data, planeList, outfile, prevValues);

  }


  /**
   * Save data to fits file. Allows user to specify the data planes in the exact order that they appear in the data.
   *
   */
  public static void savePlanes2Fits(double[][][] data, List<BackplaneInfo> planeList, String outfile,
           Map<String, HeaderCard> prevValues)
                  throws FitsException, IOException {

      List<HeaderCard> headers = new ArrayList<HeaderCard>();

      FitsHeaderBuilder fitsB = new FitsHeader.FitsHeaderBuilder();

      if (prevValues != null) {
          // try to preserve other keywords from source fits header or from config file.
          for (String key : prevValues.keySet()) {
              fitsB.setbyHeaderCard(prevValues.get(key));
          }
      }

      // either the common Fits header has filled values or it will show default values.
      FitsHeader fitsHeader = fitsB.build();
      headers.addAll(getCommonHeaderCards(fitsHeader));

      int c = 1;
      for (BackplaneInfo thisPlane : planeList) {

          headers.add(new HeaderCard("PLANE" + c, thisPlane.value(), thisPlane.comment()));
          c++;
      }

      if (planeList.size() != data.length) {
          System.out.println(
                  "Error: cube should contain " + planeList.size() + " planes but has " + data.length + " planes");
          new Exception().printStackTrace();
          System.exit(1);
      }

      //The backplanes are flipped bottom-to-top, correct them.
      data = FitsUtil.flipVertical(data);

      String localFitsFile = outfile;
//      System.out.println("saving to fits file:" + localFitsFile);
      FitsUtil.saveFits(data, localFitsFile, headers);

  }
}
