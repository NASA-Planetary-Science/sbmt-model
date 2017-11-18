package edu.jhuapl.sbmt.model.ryugu.nirs3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import edu.jhuapl.sbmt.util.TimeUtil;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;


public class NIRS3Preprocessor
{
    public static void main(String[] args) throws FitsException, IOException
    {
        String basedir="/Users/zimmemi1/sbmt/hayabusa2/nirs3/l2c/earth/";

        List<String> fitFiles = Lists.newArrayList("hyb2_nirs3_20151015.fit",
                "hyb2_nirs3_20151110.fit", "hyb2_nirs3_20151112.fit",
                "hyb2_nirs3_20151126.fit", "hyb2_nirs3_20151204a.fit",
                "hyb2_nirs3_20151204b.fit", "hyb2_nirs3_20151206.fit",
                "hyb2_nirs3_20151207a.fit", "hyb2_nirs3_20151207b.fit",
                "hyb2_nirs3_20151207c.fit", "hyb2_nirs3_20151207d.fit",
                "hyb2_nirs3_20151207e.fit", "hyb2_nirs3_20151207f.fit",
                "hyb2_nirs3_20151207g.fit", "hyb2_nirs3_20151208a.fit",
                "hyb2_nirs3_20151208b.fit", "hyb2_nirs3_20151221.fit");

        for (int m=0; m<fitFiles.size(); m++)
        {
            Fits f = new Fits(basedir+"/fit/"+fitFiles.get(m));// FileCache.getFileFromServer(filename));
            BasicHDU hdu = f.read()[0];
            int[] axes = hdu.getAxes();
            float[][] data = (float[][]) hdu.getData().getData();

            String date=hdu.getHeader().getStringValue("DATE-OBS");

            String utStrCard=hdu.getHeader().findKey("UT-STR");
            String utEndCard=hdu.getHeader().findKey("UT-END");

            String[] utStrTokens=utStrCard.split("\\s+");
            String[] utEndTokens=utEndCard.split("\\s+");

            String utStr=utStrTokens[2];
            String utEnd=utEndTokens[2];

            // XXX: right now it is assumed that none of the time intervals span midnight!!!
            double etStart=TimeUtil.str2et(date+"T"+utStr);
            double etEnd=TimeUtil.str2et(date+"T"+utEnd);

            //String spectFileName=filename.replace(oldChar, newChar)
            for (int j=0; j<axes[0]; j++)
            {
                double etNow=(etEnd-etStart)*((double)j/((double)axes[1]-1))+etStart;   // there is no explicit time per spectrum, so we just assume linearly increasing time from the given start and end in the .fit file
                String filename=basedir+"/spect/"+String.valueOf((long)etNow)+"_NIRS3.spect";
                FileWriter writer=new FileWriter(new File(filename));
                writer.write(etNow+" ");
                for (int i=0; i<axes[1]; i++)
                    writer.write(data[j][i]+" ");
                writer.close();
            }
        }
    }
}
