package edu.jhuapl.sbmt.model;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import vtk.vtkCamera;

import edu.jhuapl.saavtk.gui.View;
import edu.jhuapl.saavtk.gui.render.ConfigurableSceneNotifier;
import edu.jhuapl.saavtk.gui.render.RenderPanel;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.Graticule;
import edu.jhuapl.saavtk.model.IPositionOrientationManager;
import edu.jhuapl.saavtk.model.Model;
import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.ShapeModelBody;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.model.structure.AbstractEllipsePolygonModel.Mode;
import edu.jhuapl.saavtk.model.structure.CircleSelectionModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.model.structure.PolygonModel;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.popup.PopupMenu;
import edu.jhuapl.saavtk.status.StatusNotifier;
import edu.jhuapl.saavtk.structure.gui.StructureMainPanel;
import edu.jhuapl.saavtk.structure.io.StructureLegacyUtil;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.FileCache;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.config.BasicConfigInfo;
import edu.jhuapl.sbmt.config.SmallBodyViewConfig;
import edu.jhuapl.sbmt.core.body.BodyType;
import edu.jhuapl.sbmt.core.body.ShapeModelDataUsed;
import edu.jhuapl.sbmt.core.body.ShapeModelPopulation;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.config.IFeatureConfig;
import edu.jhuapl.sbmt.core.listeners.PositionOrientationManagerListener;
import edu.jhuapl.sbmt.core.util.TimeUtil;
import edu.jhuapl.sbmt.dem.gui.DemMainPanel;
import edu.jhuapl.sbmt.image.config.ImagingInstrumentConfig;
import edu.jhuapl.sbmt.image.controllers.ImageSearchController;
import edu.jhuapl.sbmt.image.model.BasemapImageCollection;
import edu.jhuapl.sbmt.image.model.ImagingInstrument;
import edu.jhuapl.sbmt.image.model.PerspectiveImageCollection;
import edu.jhuapl.sbmt.image.model.SbmtInfoWindowManager;
import edu.jhuapl.sbmt.image.model.SbmtSpectralImageWindowManager;
import edu.jhuapl.sbmt.image.ui.table.popup.ImageListPopupManager;
import edu.jhuapl.sbmt.lidar.config.LidarInstrumentConfig;
import edu.jhuapl.sbmt.lidar.gui.LidarPanel;
import edu.jhuapl.sbmt.model.custom.CustomGraticule;
import edu.jhuapl.sbmt.model.lineament.LineamentControlPanel;
import edu.jhuapl.sbmt.model.lineament.LineamentModel;
import edu.jhuapl.sbmt.model.lineament.LineamentPopupMenu;
import edu.jhuapl.sbmt.model.phobos.model.CumulativeMEGANECollection;
import edu.jhuapl.sbmt.model.phobos.model.MEGANECollection;
import edu.jhuapl.sbmt.pointing.PositionOrientationManager;
import edu.jhuapl.sbmt.pointing.spice.SpiceInfo;
import edu.jhuapl.sbmt.pointing.spice.ingestion.controller.KernelSelectionFrame;
import edu.jhuapl.sbmt.spectrum.SbmtSpectrumWindowManager;
import edu.jhuapl.sbmt.spectrum.config.SpectrumInstrumentConfig;
import edu.jhuapl.sbmt.spectrum.controllers.custom.CustomSpectraSearchController;
import edu.jhuapl.sbmt.spectrum.controllers.standard.SpectrumSearchController;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrum;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.statistics.SpectrumStatisticsCollection;
import edu.jhuapl.sbmt.spectrum.rendering.SpectraCollection;
import edu.jhuapl.sbmt.spectrum.rendering.SpectrumBoundaryCollection;
import edu.jhuapl.sbmt.spectrum.service.SBMTSpectraFactory;
import edu.jhuapl.sbmt.spectrum.ui.SpectrumPopupMenu;
import edu.jhuapl.sbmt.stateHistory.config.StateHistoryConfig;
import edu.jhuapl.sbmt.stateHistory.controllers.ObservationPlanningController;
import edu.jhuapl.sbmt.stateHistory.model.stateHistory.StateHistoryCollection;
import edu.jhuapl.sbmt.stateHistory.rendering.model.StateHistoryRendererManager;

import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.MetadataManager;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.EmptyMetadata;
import crucible.crust.metadata.impl.SettableMetadata;
import crucible.crust.metadata.impl.TrackedMetadataManager;
import crucible.crust.metadata.impl.Utilities;

public abstract class BaseView extends View implements PropertyChangeListener
{
	private static final long serialVersionUID = 1L;
	protected final TrackedMetadataManager stateManager;
	protected final Map<String, MetadataManager> metadataManagers;
	// protected Colorbar smallBodyColorbar;
	protected BasicConfigInfo configInfo;
	private List<SmallBodyModel> smallBodyModels;
	protected HashMap<ModelNames, List<Model>> allModels = new HashMap<>();
	private StateHistoryRendererManager rendererManager;
	private List<PositionOrientationManagerListener> pomListeners;

	// need to move to phobos/megane
	private MEGANECollection meganeCollection;
	private CumulativeMEGANECollection cumulativeMeganeCollection;

	public BaseView(StatusNotifier aStatusNotifier, BasicConfigInfo configInfo)
	{
		super(aStatusNotifier, null);
		this.configInfo = configInfo;
		uniqueName = configInfo.getUniqueName();
		shapeModelName = configInfo.getShapeModelName();
		this.stateManager = TrackedMetadataManager.of("View " + configInfo.getUniqueName());
		this.metadataManagers = new HashMap<>();
		this.configURL = configInfo.getConfigURL();
		this.pomListeners = Lists.newArrayList();
		initializeStateManager();
	}

	/**
	 * By default a view should be created empty. Only when the user requests to
	 * show a particular View, should the View's contents be created in order to
	 * reduce memory and startup time. Therefore, this function should be called
	 * prior to first time the View is shown in order to cause it
	 */
	public BaseView(StatusNotifier aStatusNotifier, SmallBodyViewConfig smallBodyConfig)
	{
		super(aStatusNotifier, smallBodyConfig);
		this.configInfo = new BasicConfigInfo(smallBodyConfig, false);
		uniqueName = configInfo.getUniqueName();
		shapeModelName = configInfo.getShapeModelName();
		this.stateManager = TrackedMetadataManager.of("View " + getUniqueName());
		this.metadataManagers = new HashMap<>();
		this.configURL = configInfo.getConfigURL();
		this.pomListeners = Lists.newArrayList();
		initializeStateManager();
	}

	@Override
	protected void initialize() throws InvocationTargetException, InterruptedException
	{
		if (configInfo != null && (getConfig() == null))
		{
			setConfig(SmallBodyViewConfig.getSmallBodyConfig(configInfo));
		}

		// TODO Auto-generated method stub
		super.initialize();
	}

	@Override
	public String getUniqueName()
	{
		if (uniqueName != null)
			return uniqueName;
		return super.getUniqueName();
	}

	@Override
	public boolean isAccessible()
	{
		if (configURL != null)
		{
			return FileCache.instance().isAccessible(configURL);
		}
		return super.isAccessible();
	}

	@Override
	public String getShapeModelName()
	{
		if (configURL != null)
		{
			String[] parts = uniqueName.split("/");
			return parts[1];
		}
		return super.getShapeModelName();
	}

	public SmallBodyViewConfig getPolyhedralModelConfig()
	{
		return (SmallBodyViewConfig) super.getConfig();
	}

	@Override
	public String getPathRepresentation()
	{
		ShapeModelType author;
		String modelLabel;
		BodyType type;
		ShapeModelPopulation population;
		ShapeModelDataUsed dataUsed;
		ShapeModelBody body;
		if (configInfo == null)
		{
			SmallBodyViewConfig config = getPolyhedralModelConfig();
			author = config.author;
			modelLabel = config.modelLabel;
			type = config.type;
			population = config.population;
			dataUsed = config.dataUsed;
			body = config.body;
		}
		else
		{
			author = configInfo.getAuthor();
			modelLabel = configInfo.getModelLabel();
			type = configInfo.getType();
			population = configInfo.getPopulation();
			dataUsed = configInfo.getDataUsed();
			body = configInfo.getBody();
		}
		if (ShapeModelType.CUSTOM == author)
		{
			return Configuration.getAppTitle() + " - " + ShapeModelType.CUSTOM + " > " + modelLabel;
		}
		else
		{
			String path = type.str;
			if (population != null && population != ShapeModelPopulation.NA)
				path += " > " + population;
			path += " > " + body;
			if (dataUsed != null && dataUsed != ShapeModelDataUsed.NA)
				path += " > " + dataUsed;
			path += " > " + getDisplayName();
			return Configuration.getAppTitle() + " - " + path;
		}
	}

	@Override
	public String getDisplayName()
	{
		String result = "";
		if (configInfo == null)
		{
			SmallBodyViewConfig config = getPolyhedralModelConfig();

			if (config.modelLabel != null)
				result = config.modelLabel;
			else if (config.author == null)
				result = config.body.toString();
			else
				result = config.author.toString();

			if (config.version != null)
				result = result + " (" + config.version + ")";

		}
		else
		{
			if (configInfo.getModelLabel() != null)
				result = configInfo.getModelLabel();
			else if (configInfo.getAuthor() == null)
				result = configInfo.getBody().toString();
			else
				result = configInfo.getAuthor().toString();

			if (configInfo.getVersion() != null)
				result = result + " (" + configInfo.getVersion() + ")";
		}

		return result;
	}

	@Override
	public String getModelDisplayName()
	{
		ShapeModelBody body = null;
		body = configInfo == null ? getPolyhedralModelConfig().body : configInfo.getBody();
		return body != null ? body + " / " + getDisplayName() : getDisplayName();
	}

	@Override
	protected void setupModelManager()
	{
		ConfigurableSceneNotifier tmpSceneChangeNotifier = new ConfigurableSceneNotifier();

		setupBodyModels();
		setupImagerModel();
		setupSpectraModels(tmpSceneChangeNotifier);
		setLineamentModel();
		setStateHistoryModels();
		StatusNotifier tmpStatusNotifier = getStatusNotifier();
		setupStructureModels(tmpSceneChangeNotifier, tmpStatusNotifier);
		setupDEMModels();

		setModelManager(new ModelManager(smallBodyModels.get(0), allModels));

		tmpSceneChangeNotifier.setTarget(getModelManager());

		getModelManager().addPropertyChangeListener(this);

//		SBMTInfoWindowManagerFactory.initializeModels(getModelManager(), getLegacyStatusHandler());

		tmpSceneChangeNotifier.setTarget(getModelManager());

	}

	protected void setupBodyModels()
	{
		smallBodyModels = SbmtModelFactory.createSmallBodyModels(getPolyhedralModelConfig());
		// allModels.put(ModelNames.SMALL_BODY, smallBodyModel);
		List<Model> allBodies = Lists.newArrayList();
		allBodies.addAll(smallBodyModels);
		allModels.put(ModelNames.SMALL_BODY, allBodies);
	}

	protected void setupStructureModels(ConfigurableSceneNotifier tmpSceneChangeNotifier,
			StatusNotifier tmpStatusNotifier)
	{
		SmallBodyModel smallBodyModel = smallBodyModels.get(0);
		// ConfigurableSceneNotifier tmpSceneChangeNotifier = new
		// ConfigurableSceneNotifier();
		// StatusNotifier tmpStatusNotifier = getStatusNotifier();
		allModels.put(ModelNames.LINE_STRUCTURES,
				List.of(new LineModel<>(tmpSceneChangeNotifier, tmpStatusNotifier, smallBodyModel)));
		allModels.put(ModelNames.POLYGON_STRUCTURES,
				List.of(new PolygonModel(tmpSceneChangeNotifier, tmpStatusNotifier, smallBodyModel)));
		allModels.put(ModelNames.CIRCLE_STRUCTURES, List.of(StructureLegacyUtil.createManager(tmpSceneChangeNotifier,
				tmpStatusNotifier, smallBodyModel, Mode.CIRCLE_MODE)));
		allModels.put(ModelNames.ELLIPSE_STRUCTURES, List.of(StructureLegacyUtil.createManager(tmpSceneChangeNotifier,
				tmpStatusNotifier, smallBodyModel, Mode.ELLIPSE_MODE)));
		allModels.put(ModelNames.POINT_STRUCTURES, List.of(StructureLegacyUtil.createManager(tmpSceneChangeNotifier,
				tmpStatusNotifier, smallBodyModel, Mode.POINT_MODE)));
		allModels.put(ModelNames.CIRCLE_SELECTION,
				List.of(new CircleSelectionModel(tmpSceneChangeNotifier, tmpStatusNotifier, smallBodyModel)));
		// tmpSceneChangeNotifier.setTarget(getModelManager());
	}

	protected void setupSpectraModels(ConfigurableSceneNotifier tmpSceneChangeNotifier)
	{
		SmallBodyModel smallBodyModel = smallBodyModels.get(0);
		HashMap<ModelNames, List<Model>> models = new HashMap<ModelNames, List<Model>>();

		ShapeModelBody body = ((SmallBodyViewConfig) smallBodyModel.getConfig()).body;
		ShapeModelType author = ((SmallBodyViewConfig) smallBodyModel.getConfig()).author;
		String version = ((SmallBodyViewConfig) smallBodyModel.getConfig()).version;

		// TODO FIX THIS
		// models.put(ModelNames.SPECTRA_HYPERTREE_SEARCH, new
		// SpectraSearchDataCollection(smallBodyModel));

		SpectraCollection collection = new SpectraCollection(tmpSceneChangeNotifier, smallBodyModel);

		models.put(ModelNames.SPECTRA, List.of(collection));

		allModels.putAll(models);
		allModels.put(ModelNames.SPECTRA_BOUNDARIES, List.of(new SpectrumBoundaryCollection(smallBodyModel,
				(SpectraCollection) allModels.get(ModelNames.SPECTRA).get(0))));
		// if (getPolyhedralModelConfig().body == ShapeModelBody.EROS)
		allModels.put(ModelNames.STATISTICS, List.of(new SpectrumStatisticsCollection()));

		SpectraCollection customCollection = new SpectraCollection(tmpSceneChangeNotifier, smallBodyModel);
		allModels.put(ModelNames.CUSTOM_SPECTRA, List.of(customCollection));
		allModels.put(ModelNames.CUSTOM_SPECTRA_BOUNDARIES, List.of(new SpectrumBoundaryCollection(smallBodyModel,
				(SpectraCollection) allModels.get(ModelNames.CUSTOM_SPECTRA).get(0))));

		// TODO add this to phobox/megane setup
		// if (!getPolyhedralModelConfig().spectralInstruments.stream().filter(inst ->
		// inst.getDisplayName().equals("MEGANE")).toList().isEmpty())
		// {
		// meganeCollection = new MEGANECollection(smallBodyModel);
		// allModels.put(ModelNames.GRNS_SPECTRA, List.of(meganeCollection));
		// cumulativeMeganeCollection = new CumulativeMEGANECollection(smallBodyModel);
		// allModels.put(ModelNames.GRNS_CUSTOM_SPECTRA,
		// List.of(cumulativeMeganeCollection));
		// }
	}

	protected void setupImagerModel()
	{
		allModels.put(ModelNames.IMAGES_V2, List.of(new PerspectiveImageCollection(smallBodyModels)));
		allModels.put(ModelNames.BASEMAPS, List.of(new BasemapImageCollection<>(smallBodyModels)));
	}

	protected void setupDEMModels()
	{
//		SmallBodyModel smallBodyModel = smallBodyModels.get(0);
//		DEMCollection demCollection = new DEMCollection(smallBodyModel, getModelManager());
//		allModels.put(ModelNames.DEM, List.of(demCollection));
//		DEMBoundaryCollection demBoundaryCollection = new DEMBoundaryCollection(smallBodyModel, getModelManager());
//		allModels.put(ModelNames.DEM_BOUNDARY, List.of(demBoundaryCollection));
//		demCollection.setModelManager(getModelManager());
//		demBoundaryCollection.setModelManager(getModelManager());
	}

	protected void setStateHistoryModels()
	{
		SmallBodyModel smallBodyModel = smallBodyModels.get(0);

		rendererManager = new StateHistoryRendererManager(smallBodyModel, new StateHistoryCollection(smallBodyModel),
				getRenderer());
		allModels.put(ModelNames.STATE_HISTORY_COLLECTION_ELEMENTS, List.of(rendererManager));
	}

	protected void setLineamentModel()
	{
		allModels.put(ModelNames.LINEAMENT, List.of(createLineament()));
	}

	static public LineamentModel createLineament()
	{
		return new LineamentModel();
	}

	@Override
	protected void setupPopupManager()
	{
		ModelManager modelManager = getModelManager();
		SbmtInfoWindowManager imageInfoWindowManager = (SbmtInfoWindowManager) getInfoPanelManager();
		SbmtSpectralImageWindowManager multiSpectralInfoWindowManager = (SbmtSpectralImageWindowManager) getSpectrumPanelManager();
		Renderer renderer = getRenderer();
		setPopupManager(new ImageListPopupManager(modelManager, imageInfoWindowManager, multiSpectralInfoWindowManager,
				renderer));

		List<IFeatureConfig> spectrumConfigs = getPolyhedralModelConfig()
				.getConfigsForClass(SpectrumInstrumentConfig.class);
		for (IFeatureConfig instrumentConfig : spectrumConfigs)
		{

			SpectrumInstrumentConfig config = (SpectrumInstrumentConfig) instrumentConfig;
//			BasicSpectrumInstrument instrument = config.spectralInstruments.get(0);
			if (config.hasSpectralData)
			{
				// This needs to be updated to handle the multiple Spectral Instruments that can
				// exist on screen at the same time....
				// for (SpectralInstrument instrument :
				// getPolyhedralModelConfig().spectralInstruments)
				{
					SpectraCollection spectrumCollection = (SpectraCollection) getModel(ModelNames.SPECTRA);
					SpectrumBoundaryCollection spectrumBoundaryCollection = (SpectrumBoundaryCollection) getModel(
							ModelNames.SPECTRA_BOUNDARIES);
					PopupMenu popupMenu = new SpectrumPopupMenu(spectrumCollection, spectrumBoundaryCollection,
							getModelManager(), (SbmtSpectrumWindowManager) getInfoPanelManager(), getRenderer());
					registerPopup(getModel(ModelNames.SPECTRA), popupMenu);
				}
			}

		}

		if (getPolyhedralModelConfig().hasLineamentData)
		{
			PopupMenu popupMenu = new LineamentPopupMenu(getModelManager());
			registerPopup(getModel(ModelNames.LINEAMENT), popupMenu);
		}

//		if (getPolyhedralModelConfig().hasMapmaker || getPolyhedralModelConfig().hasBigmap)
//		{
//			PopupMenu popupMenu = new MapletBoundaryPopupMenu(getModelManager(), getRenderer());
//			registerPopup(getModel(ModelNames.DEM_BOUNDARY), popupMenu);
//		}
	}

	protected void setupImagerPopupManager()
	{
		// ModelManager modelManager = getModelManager();
		// SbmtInfoWindowManager imageInfoWindowManager = (SbmtInfoWindowManager)
		// getInfoPanelManager();
		// SbmtMultiSpectralWindowManager multiSpectralInfoWindowManager =
		// (SbmtMultiSpectralWindowManager) getMultiSpectralPanelManager();
		// Renderer renderer = getRenderer();
		// if
		// (getPolyhedralModelConfig().getInstrumentConfigs().get(ImagingInstrumentConfig.class)
		// == null) return;
		// for (IInstrumentConfig instrument :
		// getPolyhedralModelConfig().getInstrumentConfigs().get(ImagingInstrumentConfig.class))
		// {
		//// PerspectiveImageBoundaryCollection colorImageBoundaries =
		// (PerspectiveImageBoundaryCollection)
		// getModelManager().getModel(ModelNames.PERSPECTIVE_COLOR_IMAGE_BOUNDARIES);
		//// PerspectiveImageBoundaryCollection imageCubeBoundaries =
		// (PerspectiveImageBoundaryCollection)
		// getModelManager().getModel(ModelNames.PERSPECTIVE_IMAGE_CUBE_BOUNDARIES);
		//
		// //regular perspective images
		// ImageCollection images = (ImageCollection)
		// getModelManager().getModel(ModelNames.IMAGES);
		//// PerspectiveImageBoundaryCollection boundaries =
		// (PerspectiveImageBoundaryCollection)
		// getModelManager().getModel(ModelNames.PERSPECTIVE_IMAGE_BOUNDARIES);
		//
		// PopupMenu popupMenu = new ImagePopupMenu<>(modelManager, images,
		// /*boundaries,*/ imageInfoWindowManager, multiSpectralInfoWindowManager,
		// renderer, renderer);
		// registerPopup(getModel(ModelNames.PERSPECTIVE_IMAGE_BOUNDARIES), popupMenu);
		//
		//// //color perspective images
		//// ColorImageCollection colorImages =
		// (ColorImageCollection)getModelManager().getModel(ModelNames.COLOR_IMAGES);
		//// popupMenu = new ColorImagePopupMenu(colorImages, /*colorImageBoundaries,*/
		// imageInfoWindowManager, modelManager, renderer, renderer);
		//// PopupMenu colorImagePopupMenu = new ImagePopupMenu(modelManager, images,
		// /*colorImageBoundaries,*/ imageInfoWindowManager,
		// multiSpectralInfoWindowManager, renderer, renderer);
		//// registerPopup(getModel(ModelNames.PERSPECTIVE_COLOR_IMAGE_BOUNDARIES),
		// colorImagePopupMenu);
		////
		//// //perspective image cubes
		//// ImageCubeCollection imageCubes =
		// (ImageCubeCollection)getModelManager().getModel(ModelNames.CUBE_IMAGES);
		//// popupMenu = new ImageCubePopupMenu(imageCubes, /*imageCubeBoundaries,*/
		// imageInfoWindowManager, multiSpectralInfoWindowManager, renderer, renderer);
		//// PopupMenu imageCubePopupMenu = new ImagePopupMenu(modelManager, images,
		// /*imageCubeBoundaries,*/ imageInfoWindowManager,
		// multiSpectralInfoWindowManager, renderer, renderer);
		//// registerPopup(getModel(ModelNames.PERSPECTIVE_IMAGE_CUBE_BOUNDARIES),
		// imageCubePopupMenu);
		//
		// }
	}

	// TODO check this because it may be body/inst specific
	@Override
	public void setRenderer(Renderer renderer)
	{
		this.renderer = renderer;
		if (rendererManager == null)
			return;
		rendererManager.setRenderer(renderer);
		if (meganeCollection == null)
			return;
		meganeCollection.setRenderer(renderer);
		cumulativeMeganeCollection.setRenderer(renderer);
	}

	@Override
	protected void setupTabs()
	{
		// TODO Auto-generated method stub

	}

	protected void setupModelTab()
	{
		addTab(getPolyhedralModelConfig().getShapeModelName(), new SmallBodyControlPanel(getRenderer(),
				getModelManager(), getPolyhedralModelConfig().getShapeModelName()));

	}

	protected void setupNormalImagingTabs()
	{
		PerspectiveImageCollection collection = (PerspectiveImageCollection) getModelManager()
				.getModel(ModelNames.IMAGES_V2);

		List<IFeatureConfig> imagingConfigs = getPolyhedralModelConfig()
				.getConfigsForClass(ImagingInstrumentConfig.class);
		if (imagingConfigs == null)
			return;

		for (IFeatureConfig instrumentConfig : imagingConfigs)
		{
			ImagingInstrumentConfig config = (ImagingInstrumentConfig) instrumentConfig;
			if (config.imagingInstruments.size() == 0)
			{
				ImageSearchController cont = cont = new ImageSearchController(config, collection,
						Optional.ofNullable(null), getModelManager(), getPopupManager(), getRenderer(),
						getPickManager(), (SbmtInfoWindowManager) getInfoPanelManager(),
						(SbmtSpectralImageWindowManager) getSpectrumPanelManager(), getLegacyStatusHandler());
				addTab("Images", cont.getView());

			}
			else
			{
				for (ImagingInstrument instrument : config.imagingInstruments)
				{
					ImageSearchController cont = new ImageSearchController(config, collection, Optional.of(instrument),
							getModelManager(), getPopupManager(), getRenderer(), getPickManager(),
							(SbmtInfoWindowManager) getInfoPanelManager(),
							(SbmtSpectralImageWindowManager) getSpectrumPanelManager(), getLegacyStatusHandler());
					addTab(instrument.instrumentName.toString(), cont.getView());

					pomListeners.add(new PositionOrientationManagerListener()
					{
						@Override
						public void managerUpdated(IPositionOrientationManager manager)
						{
							((ImageSearchController) cont).setPositionOrientationManager(manager);
						}
					});
				}
			}

		}
	}

	protected void setupLineamentTab()
	{
        if (getPolyhedralModelConfig().hasLineamentData)
        {
            JComponent component = new LineamentControlPanel(getModelManager());
            addTab("Lineament", component);
        }
	}

	protected void setupSpectrumTabs()
	{
		List<IFeatureConfig> spectrumConfigs = getPolyhedralModelConfig()
				.getConfigsForClass(SpectrumInstrumentConfig.class);

		ImagingInstrumentConfig imagingConfig = (ImagingInstrumentConfig)getPolyhedralModelConfig()
				.getConfigForClass(ImagingInstrumentConfig.class);

		if (spectrumConfigs == null) return;

		for (IFeatureConfig instrumentConfig : spectrumConfigs)
		{
			SpectrumInstrumentConfig config = (SpectrumInstrumentConfig) instrumentConfig;
			for (BasicSpectrumInstrument instrument : config.spectralInstruments)
			{
				BaseSpectrumSearchModel model = (BaseSpectrumSearchModel) SBMTSpectraFactory.getModelFor(
						instrument.getDisplayName(),
						getModelManager().getPolyhedralModel().getBoundingBoxDiagonalLength());
				JComponent component = null;
				if (config.spectrumSearchDefaultStartDate != null)
				{
					double maxDistance = config.spectrumSearchDefaultMaxSpacecraftDistance;
					if (maxDistance == 0) maxDistance = imagingConfig.imageSearchDefaultMaxSpacecraftDistance;
					component = new SpectrumSearchController<BasicSpectrum>(
							config.spectrumSearchDefaultStartDate, config.spectrumSearchDefaultEndDate,
							config.hasHierarchicalSpectraSearch, maxDistance,
							config.getHierarchicalSpectraSearchSpecification(), getModelManager(),
							(SbmtSpectrumWindowManager) getSpectrumPanelManager(), getPickManager(), renderer, instrument,
							model, config).getPanel();
				}
				else
				{
					double maxDistance = config.spectrumSearchDefaultMaxSpacecraftDistance;
					if (maxDistance == 0) maxDistance = imagingConfig.imageSearchDefaultMaxSpacecraftDistance;
					component = new SpectrumSearchController<BasicSpectrum>(
							imagingConfig.imageSearchDefaultStartDate, imagingConfig.imageSearchDefaultEndDate,
							config.hasHierarchicalSpectraSearch, maxDistance,
							config.getHierarchicalSpectraSearchSpecification(), getModelManager(),
							(SbmtSpectrumWindowManager) getSpectrumPanelManager(), getPickManager(), renderer, instrument,
							model, config).getPanel();
				}
				addTab(instrument.getDisplayName(), component);
			}
		}

	}

	protected void setupLidarTabs()
	{
		// // Lidar tab
		// SmallBodyViewConfig tmpSmallBodyConfig = getPolyhedralModelConfig();
		// String lidarInstrName = "Tracks";
		// if (tmpSmallBodyConfig.hasLidarData == true)
		// lidarInstrName = tmpSmallBodyConfig.lidarInstrumentName.toString();
		//
		// try
		// {
		// JComponent lidarPanel = new LidarPanel(getRenderer(), getStatusNotifier(),
		// getPickManager(), tmpSmallBodyConfig, getModelManager().getPolyhedralModel(),
		// getModelManager());
		// addTab(lidarInstrName, lidarPanel);
		// } catch (Exception e)
		// {
		// e.printStackTrace();
		// }
		LidarInstrumentConfig config = (LidarInstrumentConfig) getPolyhedralModelConfig()
				.getConfigForClass(LidarInstrumentConfig.class);
		if (config != null)
		{
			// for (IInstrumentConfig instrumentConfig :
			// getPolyhedralModelConfig().getInstrumentConfigs().get(LidarInstrumentConfig.class))
			// {
			// LidarInstrumentConfig config = (LidarInstrumentConfig)instrumentConfig;
			// JComponent component = new LidarPanel(config, getModelManager(),
			// getPickManager(), getRenderer());
			JComponent component = new LidarPanel(getRenderer(), getStatusNotifier(), getPickManager(), config,
					smallBodyModels.get(0), getModelManager());

			addTab(config.lidarInstrumentName.toString(), component);
			// }
		}
	}

	protected void setupStructuresTab()
	{
		addTab("Structures",
				new StructureMainPanel(getPickManager(), getRenderer(), getStatusNotifier(), getModelManager()));
	}

	protected void setupCustomDataTab()
	{
		JTabbedPane customDataPane = new JTabbedPane();
		customDataPane.setBorder(BorderFactory.createEmptyBorder());

		List<IFeatureConfig> spectrumConfigs = getPolyhedralModelConfig()
				.getConfigsForClass(SpectrumInstrumentConfig.class);

		if (spectrumConfigs != null)
		{
			for (IFeatureConfig instrumentConfig : spectrumConfigs)
			{
				SpectrumInstrumentConfig config = (SpectrumInstrumentConfig) instrumentConfig;
				for (BasicSpectrumInstrument i : config.spectralInstruments)
				{
					// if (i.getDisplayName().equals("NIS"))
					// continue; //we can't properly handle NIS custom data for now without info
					// files, which we don't have.
					customDataPane.addTab(i.getDisplayName() + " Spectra",
							new CustomSpectraSearchController(getModelManager(),
									(SbmtSpectrumWindowManager) getInfoPanelManager(), getPickManager(), getRenderer(),
									config.hierarchicalSpectraSearchSpecification, i, config).getPanel());
					//
					SpectraCollection spectrumCollection = (SpectraCollection) getModel(ModelNames.CUSTOM_SPECTRA);
					SpectrumBoundaryCollection boundaryCollection = (SpectrumBoundaryCollection) getModel(
							ModelNames.CUSTOM_SPECTRA_BOUNDARIES);
					PopupMenu popupMenu = new SpectrumPopupMenu(spectrumCollection, boundaryCollection,
							getModelManager(), (SbmtSpectrumWindowManager) getInfoPanelManager(), getRenderer());
					registerPopup(spectrumCollection, popupMenu);
				}
			}
		}

		if (customDataPane.getTabCount() != 0)
			addTab("Custom Data", customDataPane);
	}

	protected void setupDEMTab()
	{
		addTab("Regional DTMs", new DemMainPanel(getRenderer(), getModelManager().getPolyhedralModel(),
				getStatusNotifier(), getPickManager(), getPolyhedralModelConfig()));
	}

	protected void setupStateHistoryTab()
	{
		StateHistoryConfig stateHistoryConfig = (StateHistoryConfig) getPolyhedralModelConfig()
				.getConfigForClass(StateHistoryConfig.class);

		ObservationPlanningController planningController = new ObservationPlanningController(getModelManager(),
				smallBodyModels.get(0), rendererManager, stateHistoryConfig,
				smallBodyModels.get(0).getColoringDataManager(), getStatusNotifier());
		addTab("Observing Conditions", planningController.getView());
		planningController.setPositionOrientationManager(positionOrientationManager);
		pomListeners.add(new PositionOrientationManagerListener()
		{
			@Override
			public void managerUpdated(IPositionOrientationManager manager)
			{
				planningController.setPositionOrientationManager(manager);
			}
		});
	}

	@Override
	protected void setupInfoPanelManager()
	{
		// setInfoPanelManager(new SbmtInfoWindowManager(getModelManager(),
		// getStatusNotifier()));
	}

	@Override
	protected void setupSpectrumPanelManager()
	{
		// SpectraCollection spectrumCollection =
		// (SpectraCollection)getModel(ModelNames.SPECTRA);
		// SpectrumBoundaryCollection spectrumBoundaryCollection =
		// (SpectrumBoundaryCollection)getModel(ModelNames.SPECTRA_BOUNDARIES);
		//
		// PopupMenu spectralImagesPopupMenu =
		// new SpectrumPopupMenu(spectrumCollection, spectrumBoundaryCollection,
		// getModelManager(), null, null);
		// setSpectrumPanelManager(new SbmtSpectrumWindowManager(getModelManager(),
		// spectralImagesPopupMenu));
	}

	@Override
	protected void setupPickManager()
	{
		PickManager tmpPickManager = new PickManager(getRenderer(), getModelManager());
		setPickManager(tmpPickManager);

		// Manually register the Renderer with the DefaultPicker
		tmpPickManager.getDefaultPicker().addListener(getRenderer());

		// Manually register the PopupManager with the DefaultPicker
		tmpPickManager.getDefaultPicker().addListener(getPopupManager());

		// TODO: This should be moved out of here to a logical relevant location
		// tmpPickManager.getDefaultPicker().addListener(new
		// ImageDefaultPickHandler(getModelManager()));
	}

	private static final Version METADATA_VERSION = Version.of(1, 1); // Nested CURRENT_TAB stored as an array of
																		// strings.
	private static final Version METADATA_VERSION_1_0 = Version.of(1, 0); // Top level CURRENT_TAB only stored as a
																			// single string.
	private static final Key<Map<String, Metadata>> METADATA_MANAGERS_KEY = Key.of("metadataManagers");
	private static final Key<Metadata> MODEL_MANAGER_KEY = Key.of("modelState");
	private static final Key<Integer> RESOLUTION_LEVEL_KEY = Key.of("resolutionLevel");
	private static final Key<double[]> POSITION_KEY = Key.of("cameraPosition");
	private static final Key<double[]> UP_KEY = Key.of("cameraUp");
	private static final Key<List<String>> CURRENT_TAB_KEY = Key.of("currentTab");
	private static final Key<String> CURRENT_TAB_KEY_1_0 = Key.of("currentTab");

	@Override
	protected void initializeStateManager()
	{
		if (!stateManager.isRegistered())
		{
			stateManager.register(new MetadataManager()
			{

				@Override
				public Metadata store()
				{
					if (!isInitialized())
					{
						return EmptyMetadata.instance();
					}

					SettableMetadata result = SettableMetadata.of(METADATA_VERSION);

					result.put(RESOLUTION_LEVEL_KEY, getModelManager().getPolyhedralModel().getModelResolution());

					Renderer localRenderer = BaseView.this.getRenderer();
					if (localRenderer != null)
					{
						RenderPanel panel = (RenderPanel) localRenderer.getRenderWindowPanel();
						vtkCamera camera = panel.getActiveCamera();
						result.put(POSITION_KEY, camera.GetPosition());
						result.put(UP_KEY, camera.GetViewUp());
					}

					// Redmine #1320/1439: this is what used to be here to save the state of imaging
					// search panels.
					// if (!searchPanelMap.isEmpty())
					// {
					// ImmutableSortedMap.Builder<String, Metadata> builder =
					// ImmutableSortedMap.naturalOrder();
					// for (Entry<String, ImagingSearchPanel> entry : searchPanelMap.entrySet())
					// {
					// MetadataManager imagingStateManager = entry.getValue().getMetadataManager();
					// if (imagingStateManager != null)
					// {
					// builder.put(entry.getKey(), imagingStateManager.store());
					// }
					// }
					// result.put(imagingKey, builder.build());
					// }
					Map<String, Metadata> metadata = Utilities.bulkStore(metadataManagers);
					result.put(METADATA_MANAGERS_KEY, metadata);

					ModelManager modelManager = getModelManager();
					if (modelManager instanceof MetadataManager)
					{
						result.put(MODEL_MANAGER_KEY, ((MetadataManager) modelManager).store());
					}

					JTabbedPane controlPanel = getControlPanel();
					if (controlPanel != null)
					{
						List<String> currentTabs = new ArrayList<>();
						compileCurrentTabs(controlPanel, currentTabs);
						result.put(CURRENT_TAB_KEY, currentTabs);
					}
					return result;
				}

				@Override
				public void retrieve(Metadata state)
				{
					try
					{
						initialize();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return;
					}

					Version serializedVersion = state.getVersion();

					if (state.hasKey(RESOLUTION_LEVEL_KEY))
					{
						try
						{
							getModelManager().getPolyhedralModel().setModelResolution(state.get(RESOLUTION_LEVEL_KEY));
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					Renderer localRenderer = BaseView.this.getRenderer();
					if (localRenderer != null)
					{
						RenderPanel panel = (RenderPanel) localRenderer.getRenderWindowPanel();
						vtkCamera camera = panel.getActiveCamera();
						camera.SetPosition(state.get(POSITION_KEY));
						camera.SetViewUp(state.get(UP_KEY));
						panel.resetCameraClippingRange();
						panel.Render();
					}

					// Redmine #1320/1439: this is what used to be here to retrieve the state of
					// imaging search panels.
					// if (!searchPanelMap.isEmpty())
					// {
					// SortedMap<String, Metadata> metadataMap = state.get(imagingKey);
					// for (Entry<String, ImagingSearchPanel> entry : searchPanelMap.entrySet())
					// {
					// Metadata imagingMetadata = metadataMap.get(entry.getKey());
					// if (imagingMetadata != null)
					// {
					// MetadataManager imagingStateManager = entry.getValue().getMetadataManager();
					// imagingStateManager.retrieve(imagingMetadata);
					// }
					// }
					// }
					Map<String, Metadata> metadata = state.get(METADATA_MANAGERS_KEY);
					Utilities.bulkRetrieve(metadataManagers, metadata);

					if (state.hasKey(MODEL_MANAGER_KEY))
					{
						ModelManager modelManager = getModelManager();
						if (modelManager instanceof MetadataManager)
						{
							((MetadataManager) modelManager).retrieve(state.get(MODEL_MANAGER_KEY));
						}
					}

					List<String> currentTabs = ImmutableList.of();
					if (serializedVersion.compareTo(METADATA_VERSION_1_0) > 0)
					{
						currentTabs = state.get(CURRENT_TAB_KEY);
					}
					else if (state.hasKey(CURRENT_TAB_KEY_1_0))
					{
						currentTabs = ImmutableList.of(state.get(CURRENT_TAB_KEY_1_0));
					}

					restoreCurrentTabs(getControlPanel(), currentTabs);
				}

				private void compileCurrentTabs(JTabbedPane tabbedPane, List<String> tabs)
				{
					int selectedIndex = tabbedPane.getSelectedIndex();
					if (selectedIndex >= 0)
					{
						tabs.add(tabbedPane.getTitleAt(selectedIndex));
						Component component = tabbedPane.getSelectedComponent();
						if (component instanceof JTabbedPane)
						{
							compileCurrentTabs((JTabbedPane) component, tabs);
						}
					}
				}

				private void restoreCurrentTabs(JTabbedPane tabbedPane, List<String> tabTitles)
				{
					if (tabbedPane != null)
					{
						if (!tabTitles.isEmpty())
						{
							String title = tabTitles.get(0);
							for (int index = 0; index < tabbedPane.getTabCount(); ++index)
							{
								String tabTitle = tabbedPane.getTitleAt(index);
								if (title.equalsIgnoreCase(tabTitle))
								{
									tabbedPane.setSelectedIndex(index);
									Component component = tabbedPane.getSelectedComponent();
									if (component instanceof JTabbedPane)
									{
										restoreCurrentTabs((JTabbedPane) component,
												tabTitles.subList(1, tabTitles.size()));
									}
									break;
								}
							}
						}
					}
				}

			});
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e)
	{
		if (e.getPropertyName().equals(Properties.MODEL_CHANGED))
			renderer.notifySceneChange();
		else
			renderer.getRenderWindowPanel().Render();

		// if (e.getPropertyName().equals(Properties.MODEL_CHANGED))
		// {
		// renderer.setProps(getModelManager().getProps());
		//
		// if (smallBodyColorbar == null)
		// return;
		//
		// PolyhedralModel sbModel = (PolyhedralModel)
		// getModelManager().getModel(ModelNames.SMALL_BODY);
		// if (sbModel.isColoringDataAvailable() && sbModel.getColoringIndex() >= 0)
		// {
		// if (!smallBodyColorbar.isVisible())
		// smallBodyColorbar.setVisible(true);
		// smallBodyColorbar.setColormap(sbModel.getColormap());
		// int index = sbModel.getColoringIndex();
		// String title = sbModel.getColoringName(index).trim();
		// String units = sbModel.getColoringUnits(index).trim();
		// if (units != null && !units.isEmpty())
		// {
		// title += " (" + units + ")";
		// }
		// smallBodyColorbar.setTitle(title);
		// if
		// (renderer.getRenderWindowPanel().getRenderer().HasViewProp(smallBodyColorbar.getActor())
		// == 0)
		// renderer.getRenderWindowPanel().getRenderer().AddActor(smallBodyColorbar.getActor());
		// smallBodyColorbar.getActor().SetNumberOfLabels(sbModel.getColormap().getNumberOfLabels());
		// }
		// else
		// smallBodyColorbar.setVisible(false);
		//
		// }
		// else
		// {
		// renderer.getRenderWindowPanel().Render();
		// }
	}

	static public Graticule createGraticule(SmallBodyModel smallBodyModel)
	{
		SmallBodyViewConfig config = (SmallBodyViewConfig) smallBodyModel.getSmallBodyConfig();
		ShapeModelType author = config.author;

		if (ShapeModelType.GASKELL == author && smallBodyModel.getNumberResolutionLevels() == 4)
		{
			String[] graticulePaths = new String[]
			{ config.rootDirOnServer + "/coordinate_grid_res0.vtk.gz",
					config.rootDirOnServer + "/coordinate_grid_res1.vtk.gz",
					config.rootDirOnServer + "/coordinate_grid_res2.vtk.gz",
					config.rootDirOnServer + "/coordinate_grid_res3.vtk.gz" };

			return new Graticule(smallBodyModel, graticulePaths);
		}
		else if (ShapeModelType.CUSTOM == author && !config.customTemporary)
		{
			return new CustomGraticule(smallBodyModel);
		}

		return new Graticule(smallBodyModel);
	}

	public BasicConfigInfo getConfigInfo()
	{
		return configInfo;
	}

	@Override
	protected void setupPositionOrientationManager()
	{
		StateHistoryConfig stateHistoryConfig = (StateHistoryConfig) getPolyhedralModelConfig()
				.getConfigForClass(StateHistoryConfig.class);
		if (stateHistoryConfig == null)
			return;

		if (stateHistoryConfig.spiceInfo == null || (getModelManager().getModels(ModelNames.SMALL_BODY).size() == 1))
			return;

		KernelSelectionFrame kernelSelectionFrame = new KernelSelectionFrame(getModelManager(),
				new Function<String, Void>()
				{

					@Override
					public Void apply(String arg0)
					{
						SpiceInfo spiceInfo = stateHistoryConfig.spiceInfo;
						List<SmallBodyModel> bodies = getModelManager().getModels(ModelNames.SMALL_BODY).stream()
								.map(body ->
								{
									return (SmallBodyModel) body;
								}).toList();
						SpiceInfo firstSpiceInfo = spiceInfo;
						SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
						String dateTimeString = dateFormatter.format(stateHistoryConfig.stateHistoryStartDate);
						double time = TimeUtil.str2et(dateTimeString);
						positionOrientationManager = new PositionOrientationManager(bodies, arg0, firstSpiceInfo,
								firstSpiceInfo.getInstrumentNamesToBind()[0], spiceInfo.getBodyName(), time);
						HashMap<ModelNames, List<Model>> allModels = new HashMap(getModelManager().getAllModels());
						allModels.put(ModelNames.SMALL_BODY, positionOrientationManager.getUpdatedBodies());
						setModelManager(new ModelManager(bodies.get(0), allModels));
						pomListeners.forEach(listener -> listener.managerUpdated(positionOrientationManager));
						return null;
					}
				});
		kernelSelectionFrame.setLocationRelativeTo(this);

	}

}
