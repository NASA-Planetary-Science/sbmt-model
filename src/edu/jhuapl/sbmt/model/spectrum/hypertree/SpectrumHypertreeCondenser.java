package edu.jhuapl.sbmt.model.spectrum.hypertree;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.jhuapl.sbmt.lidar.hyperoctree.FSHyperTreeCondenser;


public class SpectrumHypertreeCondenser extends FSHyperTreeCondenser
{

    Path rootNodePath;
    Path outFilePath;
    FileWriter writer;

    public SpectrumHypertreeCondenser(Path rootPath, Path outFilePath)
    {
        super(rootPath, outFilePath);
    }

    public static void main(String[] args)
    {
        // assumes this is being run from misc/scripts/generate_spectra_hypertree.sh
        // so the temp_hypertree/ directory has been created in working directory.
        Path rootPath=Paths.get("temp_hypertree");
        Path outFilePath=rootPath.resolve("dataSource.spectra");
        System.out.println("Root path = "+rootPath);
        System.out.println("Output path = "+outFilePath);
        SpectrumHypertreeCondenser condenser=new SpectrumHypertreeCondenser(rootPath,outFilePath);
        condenser.condense();
        System.out.println("Wrote tree structure to "+outFilePath);
    }
}


