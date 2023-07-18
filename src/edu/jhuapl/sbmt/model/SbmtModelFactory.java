package edu.jhuapl.sbmt.model;

import java.util.List;

import com.beust.jcommander.internal.Lists;

import edu.jhuapl.saavtk.config.IBodyViewConfig;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.sbmt.config.SmallBodyViewConfig;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.model.bennu.shapeModel.Bennu;
import edu.jhuapl.sbmt.model.bennu.shapeModel.BennuV4;
import edu.jhuapl.sbmt.model.custom.CustomShapeModel;
import edu.jhuapl.sbmt.model.eros.Eros;
import edu.jhuapl.sbmt.model.eros.ErosThomas;
import edu.jhuapl.sbmt.model.itokawa.Itokawa;
import edu.jhuapl.sbmt.model.lineament.LineamentModel;
import edu.jhuapl.sbmt.model.rosetta.CG;
import edu.jhuapl.sbmt.model.rosetta.Lutetia;
import edu.jhuapl.sbmt.model.simple.Sbmt2SimpleSmallBody;
import edu.jhuapl.sbmt.model.simple.SimpleSmallBody;
import edu.jhuapl.sbmt.model.vesta_old.VestaOld;

public class SbmtModelFactory
{
	static public SmallBodyModel createSmallBodyModel(IBodyViewConfig config)
	{
		return createSmallBodyModels(config).get(0);
	}

    static public List<SmallBodyModel> createSmallBodyModels(IBodyViewConfig config)
    {
        SmallBodyModel result = null;
        ShapeModelBody name = config.getBody();
        ShapeModelType author = config.getAuthor();

        if (ShapeModelType.GASKELL == author || ((ShapeModelType.EXPERIMENTAL == author || ShapeModelType.BLENDER == author) && ShapeModelBody.DEIMOS != name))
        {
            if (ShapeModelBody.EROS == name)
            {
                result = new Eros(config);
            }
            else if (ShapeModelBody.ITOKAWA == name)
            {
                result = new Itokawa(config);
            }
//            else if (ShapeModelBody.TEMPEL_1 == name)
//            {
//                String[] names = {
//                        name + " low"
//                };
//
//                result = new SimpleSmallBody(config, names);
//            }
            else if (ShapeModelBody.RQ36 == name)
            {
                if (config.getVersion().equals("V4"))
                {
                    result = new BennuV4(config);
                }
                else
                {
                    result = new Bennu(config);
                }
            }
            else
            {
                if (config.getRootDirOnServer().toLowerCase().equals(config.getRootDirOnServer()))
                {
                    result = new Sbmt2SimpleSmallBody(config);
                }
                else
                {
                    String[] names = {
                            name + " low",
                            name + " med",
                            name + " high",
                            name + " very high"
                    };
                    String[] paths = {
                    		config.getRootDirOnServer() + "/ver64q.vtk.gz",
                            config.getRootDirOnServer() + "/ver128q.vtk.gz",
                            config.getRootDirOnServer() + "/ver256q.vtk.gz",
                            config.getRootDirOnServer() + "/ver512q.vtk.gz"
                    };

                    result = new SimpleSmallBody(config, names);
                }
            }
        }
        else if (ShapeModelType.THOMAS == author)
        {
            if (ShapeModelBody.EROS == name)
                result = new ErosThomas(config);
            else if (ShapeModelBody.VESTA == name)
                result = new VestaOld(config);
        }
        else if (ShapeModelType.JORDA == author)
        {
            if (ShapeModelBody.LUTETIA == name)
                result = new Lutetia(config);
        }
        else if (ShapeModelType.DLR == author)
        {
            if (ShapeModelBody._67P == name)
                result = new CG(config);
        }
        else if (ShapeModelType.CUSTOM == author)
        {
            result = new CustomShapeModel(config);
        }

        if (result == null)
        {
        	if (config.getRootDirOnServer().toLowerCase().equals(config.getRootDirOnServer()))
            {
                result = new Sbmt2SimpleSmallBody(config);
            }
            else
            {
                result = new SimpleSmallBody(config);
            }
        }

        //check for other bodies in the sytem
        List<SmallBodyModel> allBodies = Lists.newArrayList();
        allBodies.add(result);
        if (config.hasSystemBodies())
        {
        	for (SmallBodyViewConfig extra : ((SmallBodyViewConfig)config).systemConfigs)
        	{
        		allBodies.addAll(createSmallBodyModels(extra));
        	}
        }

        return allBodies;
    }

    static public LineamentModel createLineament()
    {
        return new LineamentModel();
    }

//    static public HashMap<ModelNames, Model> createSpectralModels(SmallBodyModel smallBodyModel)
//    {
//        HashMap<ModelNames, Model> models = new HashMap<ModelNames, Model>();
//
//        ShapeModelBody body=((SmallBodyViewConfig)smallBodyModel.getConfig()).body;
//        ShapeModelType author=((SmallBodyViewConfig)smallBodyModel.getConfig()).author;
//        String version=((SmallBodyViewConfig)smallBodyModel.getConfig()).version;
//
//        models.put(ModelNames.SPECTRA_HYPERTREE_SEARCH, new SpectraSearchDataCollection(smallBodyModel));
//
//        models.put(ModelNames.SPECTRA, new SpectraCollection(smallBodyModel));
//        return models;
//    }

//    static public DEM createDEM(
//            DEMKey key,
//            SmallBodyModel smallBodyModel) //throws IOException, FitsException
//    {
//        return new DEM(key);
//    }

}
