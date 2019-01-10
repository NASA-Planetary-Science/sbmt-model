package edu.jhuapl.sbmt.model.bennu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import edu.jhuapl.saavtk.metadata.Key;
import edu.jhuapl.saavtk.metadata.Metadata;
import edu.jhuapl.saavtk.metadata.MetadataManager;
import edu.jhuapl.saavtk.metadata.SettableMetadata;
import edu.jhuapl.saavtk.metadata.Version;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.sbmt.model.bennu.otes.SpectraHierarchicalSearchSpecification;

public class OREXSpectrumInstrumentMetadataIO extends SpectraHierarchicalSearchSpecification<OREXSearchSpec>
{
    private static Gson gson = null;
    List<OREXSpectrumInstrumentMetadata<OREXSearchSpec>> info = null;
    private File path;
    private String pathString;

    public OREXSpectrumInstrumentMetadataIO(String instrumentName) throws FileNotFoundException
    {
        super(instrumentName);
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();

    }

    @Override
    public SpectraHierarchicalSearchSpecification<OREXSearchSpec> clone()
    {
    	// TODO Auto-generated method stub
    	try
		{
    		OREXSpectrumInstrumentMetadataIO specIO = new OREXSpectrumInstrumentMetadataIO(rootName);
    		specIO.setPathString(pathString);
    		return specIO;
		}
    	catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }

    @Override
    public void loadMetadata() throws FileNotFoundException
    {
        this.path = FileCache.getFileFromServer(pathString);
        readMetadata(path);
    }

    public void setPathString(String path)
    {
        this.pathString = path;
    }

    public void writeJSON(File file, String json) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(json);
        writer.close();
    }

    public void readMetadata(File file) throws FileNotFoundException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        Type collectionType = new TypeToken<List<OREXSpectrumInstrumentMetadata<OREXSearchSpec>>>(){}.getType();
        info = gson.fromJson(bufferedReader, collectionType);
    }

    public InstrumentMetadata<OREXSearchSpec> readMetadataFromFileForInstrument(File file, String instrumentName) throws FileNotFoundException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        Type collectionType = new TypeToken<List<OREXSpectrumInstrumentMetadata<OREXSearchSpec>>>(){}.getType();
        info = gson.fromJson(bufferedReader, collectionType);
        return getInstrumentMetadata(instrumentName);
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadataIO#getInstrumentMetadata(java.lang.String)
     */
    @Override
    public InstrumentMetadata<OREXSearchSpec> getInstrumentMetadata(String instrumentName)
    {
        for (OREXSpectrumInstrumentMetadata<OREXSearchSpec> instInfo : info)
        {
            if (instInfo.getInstrumentName().equals(instrumentName))
            {
                return instInfo;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.jhuapl.sbmt.model.bennu.InstrumentMetadataIO#readHierarchyForInstrument(java.lang.String)
     */
    @Override
    public void readHierarchyForInstrument(String instrumentName)
    {
        InstrumentMetadata<OREXSearchSpec> instrumentMetadata = getInstrumentMetadata(instrumentName);
        for (OREXSearchSpec spec : instrumentMetadata.getSpecs())
        {
            addHierarchicalSearchPath(new String[] {spec.getDataName()}, instrumentMetadata.getSpecs().indexOf(spec),-1);
        }
    }

    private <T> void writeMetadataArray(Key<Metadata[]> key, MetadataManager[] values, SettableMetadata configMetadata)
    {
        if (values != null)
        {
            Metadata[] data = new Metadata[values.length];
            int i=0;
            for (MetadataManager val : values) data[i++] = val.store();
            configMetadata.put(key, data);
        }
    }

    private Metadata[] readMetadataArray(Key<Metadata[]> key, Metadata configMetadata)
    {
        Metadata[] values = configMetadata.get(key);
        if (values != null)
        {
            return values;
        }
        return null;
    }



    Key<Metadata[]> infoKey = Key.of("spectraInfo");

    @Override
    public MetadataManager getMetadataManager()
    {
        return new MetadataManager() {

            @Override
            public Metadata store()
            {
                SettableMetadata result = SettableMetadata.of(Version.of(1, 0));

                if (info != null)
                {
	                MetadataManager[] specs = new MetadataManager[info.size()];
	                for (int i=0; i<info.size(); i++)
	                {

	                    specs[i] = info.get(i);
	                }
	                writeMetadataArray(infoKey, specs, result);
                }

                return result;
            }

            @Override
            public void retrieve(Metadata source)
            {
                Metadata[] specs = readMetadataArray(infoKey, source);
                info = new ArrayList<OREXSpectrumInstrumentMetadata<OREXSearchSpec>>();
                for (Metadata meta : specs)
                {
                    OREXSpectrumInstrumentMetadata<OREXSearchSpec> inf = new OREXSpectrumInstrumentMetadata<OREXSearchSpec>();
                    inf.retrieve(meta);
                    info.add(inf);
                }

            }
        };
    }

    public static void main(String[] args) throws FileNotFoundException
    {
//        OREXSpectrumInstrumentMetadataIO test2 = new OREXSpectrumInstrumentMetadataIO("OTES");
//        List<OREXSpectrumInstrumentMetadata> metadata = test2.readMetadata(new File("/Users/steelrj1/Desktop/metadata.json"));
//        for (OREXSpectrumInstrumentMetadata data : metadata)
//        {
//            System.out.println("MetadataTest2: test: data is " + data);
//            List<OREXSearchSpec> searchSpecs = data.getSpecs();
//            for (OREXSearchSpec spec : searchSpecs)
//            {
//                System.out.println("MetadataTest2: test: " + spec);
//            }
//        }

//        ArrayList<OREXSearchSpec> specs = new ArrayList<OREXSearchSpec>();
//        ArrayList<OREXSpectrumInstrumentMetadata<OREXSearchSpec>> infos = new ArrayList<OREXSpectrumInstrumentMetadata<OREXSearchSpec>>();
//        OREXSpectrumInstrumentMetadata<OREXSearchSpec> otesInfo = new OREXSpectrumInstrumentMetadata<OREXSearchSpec>("OTES");
//        otesInfo.setQueryType("file");
//        specs.add(new OREXSearchSpec("OTES L2 Calibrated Radiance", "/earth/osirisrex/otes/spectra/l2", "spectra", "spectrumlist.txt", ImageSource.CORRECTED_SPICE,
//                                  "Wavenumber (1/cm)", "Radiance", "OTES L2 Calibrated Radiance"));
//        specs.add(new OREXSearchSpec("OTES L3 Spot Emissivity", "/earth/osirisrex/otes/spectra/l3", "spectra", "spectrumlist.txt", ImageSource.CORRECTED_SPICE,
//              "Wavenumber (1/cm)", "Emissivity", "OTES L3 Spot Emissivity"));
//        otesInfo.addSearchSpecs(specs);
//        infos.add(otesInfo);
//
//
//        OREXSpectrumInstrumentMetadata<OREXSearchSpec> ovirsInfo = new OREXSpectrumInstrumentMetadata<OREXSearchSpec>("OVIRS");
//        ovirsInfo.setQueryType("file");
//        specs = new ArrayList<OREXSearchSpec>();
//        specs.add(new OREXSearchSpec("OVIRS L3 I/F Spectra", "/earth/osirisrex/ovirs/spectra/l3/if", "spectra", "spectrumlist.txt", ImageSource.CORRECTED_SPICE,
//              "Wavenumber (1/cm)", "I/F", "OVIRS L3 I/F Spectra"));
//        specs.add(new OREXSearchSpec("OVIRS L3 REFF", "/earth/osirisrex/ovirs/spectra/l3/reff", "spectra", "spectrumlist.txt", ImageSource.CORRECTED_SPICE,
//                "Wavenumber (1/cm)", "REFF", "OVIRS L3 REFF"));
//        ovirsInfo.addSearchSpecs(specs);
//        infos.add(ovirsInfo);
//
//        String je = gson.toJson(infos);
//        System.out.println("MetadataTest2: test: je " + je);
//        Type collectionType = new TypeToken<List<OREXSpectrumInstrumentMetadata<OREXSearchSpec>>>(){}.getType();
//        List<OREXSpectrumInstrumentMetadata<OREXSearchSpec>> info2 = gson.fromJson(je, collectionType);
//        for (InstrumentMetadata<OREXSearchSpec> data : info2)
//        {
//            System.out.println("MetadataTest2: test: data is " + data);
//            List<OREXSearchSpec> searchSpecs = data.getSpecs();
//            for (SearchSpec spec : searchSpecs)
//            {
//                System.out.println("MetadataTest2: test: " + spec);
//            }
//        }
//        try
//        {
//            test2.writeJSON(new File("/Users/steelrj1/Desktop/metadata.json"), je);
//        }
//        catch (IOException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

}






//
//
//    private class InfoTranslator implements JsonDeserializer<OREXSpectrumInstrumentMetadata>, JsonSerializer<OREXSpectrumInstrumentMetadata> {
//        public OREXSpectrumInstrumentMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//            JsonObject jobj = json.getAsJsonObject();
//            OREXSpectrumInstrumentMetadata info = new OREXSpectrumInstrumentMetadata();
////            info.setInstrumentName(jobj.get("instrumentName").getAsString());
////            info = gson.fromJson(json, OREXSpectrumInstrumentMetadata.class);
////            System.out.println(
////                    "MetadataTest2.InfoTranslator: deserialize: specs " + jobj.get("specs").getAsJsonArray());
////            info.setSpecs(jobj.get("specs").getAsJsonArray());
////            System.out.println(
////                    "MetadataTest2.InfoTranslator: deserialize: " + gson.fromJson(jobj.get("specs"), OREXSearchSpec.class));
////            System.out.println(
////                    "MetadataTest2.InfoTranslator: deserialize: " + jobj.get("specs").getAsJso);
//
//            return info;
//        }
//
//       public JsonElement serialize(OREXSpectrumInstrumentMetadata src, Type typeOfSrc, JsonSerializationContext context) {
//            JsonObject jobj = new JsonObject();
//            jobj.addProperty("instrumentName", src.getInstrumentName());
////            jobj.addProperty("specs", gson.toJson(src.getSpecs()));
//            jobj.add("specs", gson.toJsonTree(src.getSpecs()));
////            jobj.addProperty("price", src.getPrice());
////            jobj.add("writer", gson.toJson(src.getAuthor()));
//            return jobj;
//       }
//    }
//
//    private class SpecTranslator implements JsonDeserializer<OREXSearchSpec>, JsonSerializer<OREXSearchSpec> {
//        public OREXSearchSpec deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//            JsonArray jobj = json.getAsJsonArray();
//            System.out.println(
//                    "MetadataTest2.SpecTranslator: deserialize: jobj " + jobj);
//
//            OREXSearchSpec spec = new OREXSearchSpec();
////            spec.dataName = json.getAsJsonArray().get(0).getAsJsonArray().get(0);
////            spec.dataName = jobj.get("dataName").getAsString();
////            spec.dataRootLocation = jobj.get("dataRootLocation").getAsString();
////            spec.dataPath = jobj.get("dataPath").getAsString();
////            spec.dataListFilename = jobj.get("dataListFileName").getAsString();
////            spec.source = jobj.get("source").getAsString();
////            spec.xAxisUnits = jobj.get("xAxisUnits").getAsString();
////            spec.yAxisUnits = jobj.get("yAxisUnits").getAsString();
////            spec.dataDescription = jobj.get("dataDescription").getAsString();
//            return spec;
//        }
//
//       public JsonElement serialize(OREXSearchSpec src, Type typeOfSrc, JsonSerializationContext context) {
//            JsonObject jobj = new JsonObject();
//            jobj.addProperty("dataName", src.getDataName());
//            jobj.addProperty("dataRootLocation", src.getDataRootLocation());
//            jobj.addProperty("dataPath", src.getDataPath());
//            jobj.addProperty("dataListFilename", src.getDataListFilename());
//            jobj.addProperty("source", src.getSource().toString());
//            jobj.addProperty("xAxisUnits", src.getxAxisUnits());
//            jobj.addProperty("yAxisUnits", src.getyAxisUnits());
//            jobj.addProperty("dataDescription", src.getDataDescription());
//            return jobj;
//       }
//    }

