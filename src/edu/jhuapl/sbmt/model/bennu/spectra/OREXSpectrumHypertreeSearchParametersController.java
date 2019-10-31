package edu.jhuapl.sbmt.model.bennu.spectra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import vtk.vtkCubeSource;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.model.ModelManager;
import edu.jhuapl.saavtk.model.ModelNames;
import edu.jhuapl.saavtk.model.structure.AbstractEllipsePolygonModel;
import edu.jhuapl.saavtk.model.structure.EllipsePolygon;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickManager.PickMode;
import edu.jhuapl.saavtk.util.BoundingBox;
import edu.jhuapl.sbmt.client.SmallBodyModel;
import edu.jhuapl.sbmt.core.listeners.SearchProgressListener;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperBox;
import edu.jhuapl.sbmt.lidar.hyperoctree.HyperException.HyperDimensionMismatchException;
import edu.jhuapl.sbmt.model.boundedobject.hyperoctree.BoundedObjectHyperTreeSkeleton;
import edu.jhuapl.sbmt.model.boundedobject.hyperoctree.HyperBoundedObject;
import edu.jhuapl.sbmt.spectrum.model.core.search.BaseSpectrumSearchModel;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectrumSearchParametersModel;
import edu.jhuapl.sbmt.spectrum.model.hypertree.SpectraSearchDataCollection;
import edu.jhuapl.sbmt.spectrum.ui.search.SpectrumHypertreeSearchParametersPanel;

public class OREXSpectrumHypertreeSearchParametersController
{
    protected SpectrumHypertreeSearchParametersPanel panel;
    protected BaseSpectrumSearchModel model;
    private JPanel auxPanel;
    protected PickManager pickManager;
    protected SpectraHierarchicalSearchSpecification spectraSpec;
    private boolean hasHierarchicalSpectraSearch;
    private double imageSearchDefaultMaxSpacecraftDistance;
    private Date imageSearchDefaultStartDate;
    private Date imageSearchDefaultEndDate;
    private ModelManager modelManager;
    private SpectrumSearchParametersModel searchParameters;
    private String dataSourceName = "";
    private String dataSpecName = "";

    public OREXSpectrumHypertreeSearchParametersController(Date imageSearchDefaultStartDate, Date imageSearchDefaultEndDate,
    													   boolean hasHierarchicalSpectraSearch, double imageSearchDefaultMaxSpacecraftDistance,
    													   SpectraHierarchicalSearchSpecification spectraSpec,
    													   BaseSpectrumSearchModel model, PickManager pickManager, ModelManager modelManager)
    {
        this.model = model;
        searchParameters = new SpectrumSearchParametersModel();
        this.spectraSpec = spectraSpec;
        this.panel = new SpectrumHypertreeSearchParametersPanel(hasHierarchicalSpectraSearch);
        this.pickManager = pickManager;
        this.hasHierarchicalSpectraSearch = hasHierarchicalSpectraSearch;
        this.imageSearchDefaultMaxSpacecraftDistance = imageSearchDefaultMaxSpacecraftDistance;
        this.imageSearchDefaultEndDate = imageSearchDefaultEndDate;
        this.imageSearchDefaultStartDate = imageSearchDefaultStartDate;
    }

    public void setupSearchParametersPanel()
    {
        initHierarchicalImageSearch();

        //        if(model.getSmallBodyConfig().hasHierarchicalSpectraSearch)
        //        {
        //            model.getSmallBodyConfig().hierarchicalSpectraSearchSpecification.processTreeSelections(
        //                    panel.getCheckBoxTree().getCheckBoxTreeSelectionModel().getSelectionPaths());
        //        }
        //        else
        {
            if (model.getInstrument().getDisplayName().equals("OTES"))
            {
                panel.addRadioButtons("OTES TYPE", new String[]{"L2", "L3"});
            }
            else
            {
                panel.addRadioButtons("OVIRS TYPE", new String[]{"I/F", "REFF"});
            }


            panel.getClearRegionButton().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    clearRegionButtonActionPerformed(evt);
                }
            });

            JSpinner startSpinner = panel.getStartSpinner();
            startSpinner.setModel(new javax.swing.SpinnerDateModel(searchParameters.getStartDate(), null, null, java.util.Calendar.DAY_OF_MONTH));
            startSpinner.setEditor(new javax.swing.JSpinner.DateEditor(startSpinner, "yyyy-MMM-dd HH:mm:ss"));
            startSpinner.setMinimumSize(new java.awt.Dimension(36, 22));
            startSpinner.setPreferredSize(new java.awt.Dimension(180, 22));
            startSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    startSpinnerStateChanged(evt);
                }
            });


            panel.getEndDateLabel().setText("End Date:");
            JSpinner endSpinner = panel.getEndSpinner();
            endSpinner.setModel(new javax.swing.SpinnerDateModel(searchParameters.getEndDate(), null, null, java.util.Calendar.DAY_OF_MONTH));
            endSpinner.setEditor(new javax.swing.JSpinner.DateEditor(endSpinner, "yyyy-MMM-dd HH:mm:ss"));
            endSpinner.setMinimumSize(new java.awt.Dimension(36, 22));
            endSpinner.setPreferredSize(new java.awt.Dimension(180, 22));
            endSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    endSpinnerStateChanged(evt);
                }
            });

            panel.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentHidden(java.awt.event.ComponentEvent evt) {
                    formComponentHidden(evt);
                }
            });

            JFormattedTextField toPhaseTextField = panel.getToPhaseTextField();
            toPhaseTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            toPhaseTextField.setText("180");
            toPhaseTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField fromPhaseTextField = panel.getFromPhaseTextField();
            fromPhaseTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            fromPhaseTextField.setText("0");
            fromPhaseTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField toEmissionTextField = panel.getToEmissionTextField();
            toEmissionTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            toEmissionTextField.setText("180");
            toEmissionTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField fromEmissionTextField = panel.getFromEmissionTextField();
            fromEmissionTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            fromEmissionTextField.setText("0");
            fromEmissionTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField toIncidenceTextField = panel.getToIncidenceTextField();
            toIncidenceTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            toIncidenceTextField.setText("180");
            toIncidenceTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField fromIncidenceTextField = panel.getFromIncidenceTextField();
            fromIncidenceTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            fromIncidenceTextField.setText("0");
            fromIncidenceTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField toDistanceTextField = panel.getToDistanceTextField();
            toDistanceTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            toDistanceTextField.setText("5000000");
            toDistanceTextField.setPreferredSize(new java.awt.Dimension(0, 22));

            JFormattedTextField fromDistanceTextField = panel.getFromDistanceTextField();
            fromDistanceTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.###"))));
            fromDistanceTextField.setText("0");
            fromDistanceTextField.setPreferredSize(new java.awt.Dimension(0, 22));


            searchParameters.setStartDate(imageSearchDefaultStartDate);
            ((SpinnerDateModel)startSpinner.getModel()).setValue(searchParameters.getStartDate());
            searchParameters.setEndDate(imageSearchDefaultEndDate);
            ((SpinnerDateModel)endSpinner.getModel()).setValue(searchParameters.getEndDate());

//            panel.getFullCheckBox().addActionListener(new ActionListener()
//            {
//                @Override
//                public void actionPerformed(ActionEvent e)
//                {
//                    model.addToPolygonsSelected(0);
//                }
//            });
//
//            panel.getPartialCheckBox().addActionListener(new ActionListener()
//            {
//                @Override
//                public void actionPerformed(ActionEvent e)
//                {
//                    model.addToPolygonsSelected(0);
//                }
//            });
//
//            panel.getDegenerateCheckBox().addActionListener(new ActionListener()
//            {
//                @Override
//                public void actionPerformed(ActionEvent e)
//                {
//                    model.addToPolygonsSelected(0);
//                }
//            });

            panel.getFromDistanceTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getFromDistanceTextField().getText().equals(""))
                        searchParameters.setMinDistanceQuery(Integer.parseInt(panel.getFromDistanceTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getFromDistanceTextField().getText().equals(""))
                        searchParameters.setMinDistanceQuery(Integer.parseInt(panel.getFromDistanceTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getFromDistanceTextField().getText().equals(""))
                        searchParameters.setMinDistanceQuery(Integer.parseInt(panel.getFromDistanceTextField().getText()));

                }
            });

            panel.getToDistanceTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getToDistanceTextField().getText().equals(""))
                        searchParameters.setMaxDistanceQuery(Integer.parseInt(panel.getToDistanceTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getToDistanceTextField().getText().equals(""))
                        searchParameters.setMaxDistanceQuery(Integer.parseInt(panel.getToDistanceTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getToDistanceTextField().getText().equals(""))
                        searchParameters.setMaxDistanceQuery(Integer.parseInt(panel.getToDistanceTextField().getText()));
                }
            });

            panel.getFromIncidenceTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getFromIncidenceTextField().getText().equals(""))
                        searchParameters.setMinIncidenceQuery(Integer.parseInt(panel.getFromIncidenceTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getFromIncidenceTextField().getText().equals(""))
                        searchParameters.setMinIncidenceQuery(Integer.parseInt(panel.getFromIncidenceTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getFromIncidenceTextField().getText().equals(""))
                        searchParameters.setMinIncidenceQuery(Integer.parseInt(panel.getFromIncidenceTextField().getText()));

                }
            });

            panel.getToIncidenceTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getToIncidenceTextField().getText().equals(""))
                        searchParameters.setMaxIncidenceQuery(Integer.parseInt(panel.getToIncidenceTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getToIncidenceTextField().getText().equals(""))
                        searchParameters.setMaxIncidenceQuery(Integer.parseInt(panel.getToIncidenceTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getToIncidenceTextField().getText().equals(""))
                        searchParameters.setMaxIncidenceQuery(Integer.parseInt(panel.getToIncidenceTextField().getText()));

                }
            });

            panel.getFromEmissionTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getFromEmissionTextField().getText().equals(""))
                        searchParameters.setMinEmissionQuery(Integer.parseInt(panel.getFromEmissionTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getFromEmissionTextField().getText().equals(""))
                        searchParameters.setMinEmissionQuery(Integer.parseInt(panel.getFromEmissionTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getFromEmissionTextField().getText().equals(""))
                        searchParameters.setMinEmissionQuery(Integer.parseInt(panel.getFromEmissionTextField().getText()));

                }
            });

            panel.getToEmissionTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getToEmissionTextField().getText().equals(""))
                        searchParameters.setMaxEmissionQuery(Integer.parseInt(panel.getToEmissionTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getToEmissionTextField().getText().equals(""))
                        searchParameters.setMaxEmissionQuery(Integer.parseInt(panel.getToEmissionTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getToEmissionTextField().getText().equals(""))
                        searchParameters.setMaxEmissionQuery(Integer.parseInt(panel.getToEmissionTextField().getText()));
                }
            });

            panel.getFromPhaseTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getFromPhaseTextField().getText().equals(""))
                        searchParameters.setMinPhaseQuery(Integer.parseInt(panel.getFromPhaseTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getFromPhaseTextField().getText().equals(""))
                        searchParameters.setMinPhaseQuery(Integer.parseInt(panel.getFromPhaseTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getFromPhaseTextField().getText().equals(""))
                        searchParameters.setMinPhaseQuery(Integer.parseInt(panel.getFromPhaseTextField().getText()));
                }
            });

            panel.getToPhaseTextField().getDocument().addDocumentListener(new DocumentListener()
            {

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    if (!panel.getToPhaseTextField().getText().equals(""))
                        searchParameters.setMaxPhaseQuery(Integer.parseInt(panel.getToPhaseTextField().getText()));
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    if (!panel.getToPhaseTextField().getText().equals(""))
                        searchParameters.setMaxPhaseQuery(Integer.parseInt(panel.getToPhaseTextField().getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    if (!panel.getToPhaseTextField().getText().equals(""))
                        searchParameters.setMaxPhaseQuery(Integer.parseInt(panel.getToPhaseTextField().getText()));
                }
            });
            //            toDistanceTextField.setValue(smallBodyConfig.imageSearchDefaultMaxSpacecraftDistance);

            searchParameters.setStartDate((Date)panel.getStartSpinner().getValue());
            searchParameters.setEndDate((Date)panel.getEndSpinner().getValue());
            searchParameters.setMinDistanceQuery(Integer.parseInt(panel.getFromDistanceTextField().getText()));
            searchParameters.setMaxDistanceQuery(Integer.parseInt(panel.getToDistanceTextField().getText()));
            searchParameters.setMinIncidenceQuery(Integer.parseInt(panel.getFromIncidenceTextField().getText()));
            searchParameters.setMaxIncidenceQuery(Integer.parseInt(panel.getToIncidenceTextField().getText()));
            searchParameters.setMinEmissionQuery(Integer.parseInt(panel.getFromEmissionTextField().getText()));
            searchParameters.setMaxEmissionQuery(Integer.parseInt(panel.getToEmissionTextField().getText()));
            searchParameters.setMinPhaseQuery(Integer.parseInt(panel.getFromPhaseTextField().getText()));
            searchParameters.setMaxPhaseQuery(Integer.parseInt(panel.getToPhaseTextField().getText()));

        }

        panel.getClearRegionButton().setText("Clear Region");
        panel.getClearRegionButton().addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearRegionButtonActionPerformed(evt);
            }
        });


        panel.getSubmitButton().setText("Search");
        panel.getSubmitButton().addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                //                if (model.getSmallBodyConfig().hasHierarchicalSpectraSearch)
                //                    model.setSelectedPath(panel.getCheckBoxTree().getCheckBoxTreeSelectionModel().getSelectionPaths());
                panel.getSelectRegionButton().setSelected(false);
                model.clearSpectraFromDisplay();
                if (model.getInstrument().getDisplayName().equals("OTES"))
                {
                    String[] names = new String[] {"OTES_L2", "OTES_L3"};
                    String[] dataSpecNames = new String[] {"OTES L2", "OTES L3"};
                    int i=0;
                    for (Enumeration<AbstractButton> buttons = panel.getGroup().getElements(); buttons.hasMoreElements();) {
                        AbstractButton button = buttons.nextElement();
                        if (button.isSelected()) {
                            dataSourceName = names[i];
                            dataSpecName = dataSpecNames[i];
                        }
                        i++;
                    }

                    //                    if (panel.getGroup().getSelection())
                    //                    if (((SpectrumSearchView) view).getL2Button().isSelected())
                    //                        spectraDatasourceName = "OTES_L2";
                    //                    else
                    //                        spectraDatasourceName = "OTES_L3";
                }
                if (model.getInstrument().getDisplayName().equals("OVIRS"))
                { // only L3 for OVIRS currently
                    String[] names = new String[] {"OVIRS_IF", "OVIRS_REF"};
                    String[] dataSpecNames = new String[] {"OVIRS L3 I/F Spectra", "OVIRS L3 REFF"};
                    int i=0;
                    for (Enumeration<AbstractButton> buttons = panel.getGroup().getElements(); buttons.hasMoreElements();) {
                        AbstractButton button = buttons.nextElement();
                        if (button.isSelected()) {
                        	 dataSourceName = names[i];
                             dataSpecName = dataSpecNames[i];
                        }
                        i++;
                    }
                    //                    if (((SpectrumSearchView) view).getIFButton().isSelected())
                    //                        spectraDatasourceName = "OVIRS_IF";
                    //                    else
                    //                        spectraDatasourceName = "OVIRS_REF";
                }
                try
				{
					pickManager.setPickMode(PickMode.DEFAULT);
					String spectraDatasourceName = dataSourceName; //model.getSpectraHypertreeSourceName();

					SpectraSearchDataCollection spectraModel = (SpectraSearchDataCollection) modelManager
							.getModel(ModelNames.SPECTRA_HYPERTREE_SEARCH);
					String spectraDatasourcePath = spectraModel.getSpectraDataSourceMap().get(spectraDatasourceName);

					spectraModel.addDatasourceSkeleton(spectraDatasourceName, spectraDatasourcePath);
					spectraModel.setCurrentDatasourceSkeleton(spectraDatasourceName);
					spectraModel.readSkeleton();
					BoundedObjectHyperTreeSkeleton skeleton = (BoundedObjectHyperTreeSkeleton) spectraModel
							.getCurrentSkeleton();

					double[] selectionRegionCenter = null;
					double selectionRegionRadius = 0.0;

					AbstractEllipsePolygonModel selectionModel = (AbstractEllipsePolygonModel) modelManager
							.getModel(ModelNames.CIRCLE_SELECTION);
					SmallBodyModel smallBodyModel = (SmallBodyModel) modelManager.getModel(ModelNames.SMALL_BODY);
					EllipsePolygon region = null;
					vtkPolyData interiorPoly = new vtkPolyData();

					if (selectionModel.getAllItems().size() > 0)
					{
						region = (EllipsePolygon) selectionModel.getStructure(0);
						selectionRegionCenter = region.getCenter();
						selectionRegionRadius = region.getRadius();

						// Always use the lowest resolution model for getting
						// the
						// intersection cubes list.
						// Therefore, if the selection region was created using
						// a
						// higher resolution model,
						// we need to recompute the selection region using the
						// low
						// res model.
						if (smallBodyModel.getModelResolution() > 0)
							smallBodyModel.drawRegularPolygonLowRes(selectionRegionCenter, region.getRadius(),
									region.getNumberOfSides(), interiorPoly, null); // this
																				// sets
																				// interiorPoly
						else
							interiorPoly = region.getVtkInteriorPolyData();

					}
					else
					{
						vtkCubeSource box = new vtkCubeSource();
						double[] bboxBounds = smallBodyModel.getBoundingBox().getBounds();
						BoundingBox bbox = new BoundingBox(bboxBounds);
						bbox.increaseSize(0.01);
						box.SetBounds(bbox.getBounds());
						box.Update();
						interiorPoly.DeepCopy(box.GetOutput());
					}

					Set<String> files = new HashSet<String>();
					HashMap<String, HyperBoundedObject> fileSpecMap = new HashMap<String, HyperBoundedObject>();
					double[] times = new double[]
					{ searchParameters.getStartDate().getTime(), searchParameters.getEndDate().getTime() };
					double[] spectraLims = new double[]
					{ searchParameters.getMinEmissionQuery(), searchParameters.getMaxEmissionQuery(),
							searchParameters.getMinIncidenceQuery(), searchParameters.getMaxIncidenceQuery(),
							searchParameters.getMinPhaseQuery(), searchParameters.getMaxPhaseQuery(),
							searchParameters.getMinDistanceQuery(), searchParameters.getMaxDistanceQuery() };
					double[] bounds = interiorPoly.GetBounds();
					TreeSet<Integer> cubeList = ((SpectraSearchDataCollection) spectraModel)
							.getLeavesIntersectingBoundingBox(new BoundingBox(bounds), times, spectraLims);
					HyperBox hbb = new HyperBox(new double[]
					{ bounds[0], bounds[2], bounds[4], times[0], spectraLims[0], spectraLims[2], spectraLims[4],
							spectraLims[6] }, new double[]
					{ bounds[1], bounds[3], bounds[5], times[1], spectraLims[1], spectraLims[3], spectraLims[5],
							spectraLims[7] });
					model.performHypertreeSearch(searchParameters, cubeList, skeleton, hbb,
							dataSpecName, true,
							hasHierarchicalSpectraSearch, spectraSpec, new SearchProgressListener()
							{

								@Override
								public void searchStarted()
								{
									// TODO Auto-generated method stub

								}

								@Override
								public void searchProgressChanged(int percentComplete)
								{
									// TODO Auto-generated method stub

								}

								@Override
								public void searchEnded()
								{
									// TODO Auto-generated method stub

								}

								@Override
								public void searchIndeterminate() {}

								@Override
								public void searchNoteUpdated(String note)
								{
									// TODO Auto-generated method stub

								};
							});

				} catch (HyperDimensionMismatchException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });


        panel.getSelectRegionButton().setText("Select Region");
        panel.getSelectRegionButton().addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectRegionButtonActionPerformed(evt);
            }
        });




    }

    // Sets up everything related to hierarchical image searches
    protected void initHierarchicalImageSearch()
    {
        // Show/hide panels depending on whether this body has hierarchical image search capabilities
        if(hasHierarchicalSpectraSearch)
        {
            // Has hierarchical search capabilities, these replace the camera and filter checkboxes so hide them
            //            panel.getFilterCheckBoxPanel().setVisible(false);
            //            panel.getUserDefinedCheckBoxPanel().setVisible(false);
            //            panel.getAuxPanel().setVisible(false);

            // Create the tree
            spectraSpec.clearTreeLeaves();
            spectraSpec.readHierarchyForInstrument(model.getInstrument().getDisplayName());
            //            panel.setCheckBoxTree(new CheckBoxTree(model.getSmallBodyConfig().hierarchicalSpectraSearchSpecification.getTreeModel()));
            //
            //            // Place the tree in the panel
            //            if (panel.getHierarchicalSearchScrollPane() != null)
            //                panel.getHierarchicalSearchScrollPane().setViewportView(panel.getCheckBoxTree());
        }
        //        else
        //        {
        //            // No hierarchical search capabilities, hide the scroll pane
        //            panel.getHierarchicalSearchScrollPane().setVisible(false);
        //        }
    }

    public void formComponentHidden(java.awt.event.ComponentEvent evt)
    {
        panel.getSelectRegionButton().setSelected(false);
        pickManager.setPickMode(PickMode.DEFAULT);
    }

    public void startSpinnerStateChanged(javax.swing.event.ChangeEvent evt)
    {
        Date date =
                ((SpinnerDateModel)panel.getStartSpinner().getModel()).getDate();
        if (date != null)
            searchParameters.setStartDate(date);
    }

    public void endSpinnerStateChanged(javax.swing.event.ChangeEvent evt)
    {
        Date date =
                ((SpinnerDateModel)panel.getEndSpinner().getModel()).getDate();
        if (date != null)
            searchParameters.setEndDate(date);

    }

    public void selectRegionButtonActionPerformed(ActionEvent evt)
    {
        if (panel.getSelectRegionButton().isSelected())
            pickManager.setPickMode(PickMode.CIRCLE_SELECTION);
        else
            pickManager.setPickMode(PickMode.DEFAULT);
    }

    public void clearRegionButtonActionPerformed(ActionEvent evt)
    {
        AbstractEllipsePolygonModel selectionModel = (AbstractEllipsePolygonModel)modelManager.getModel(ModelNames.CIRCLE_SELECTION);
        selectionModel.removeAllStructures();
    }

    public SpectrumHypertreeSearchParametersPanel getPanel()
    {
        return panel;
    }

    public void setPanel(SpectrumHypertreeSearchParametersPanel panel)
    {
        this.panel = panel;
    }

    public JPanel getAuxPanel()
    {
        return auxPanel;
    }

    public void setAuxPanel(JPanel auxPanel)
    {
        this.auxPanel = auxPanel;
        panel.setAuxPanel(auxPanel);
    }
}
