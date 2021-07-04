package be.nabu.eai.module.data.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.TypeGenerator;
import be.nabu.eai.developer.api.TypeGeneratorTarget;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.impl.CustomTooltip;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.ElementLineConnectListener;
import be.nabu.eai.developer.managers.util.ElementMarshallable;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.managers.util.RootElementWithPush;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils.Endpoint;
import be.nabu.eai.developer.util.EAIDeveloperUtils.EndpointPicker;
import be.nabu.eai.developer.util.ElementClipboardHandler;
import be.nabu.eai.developer.util.ElementSelectionListener;
import be.nabu.eai.developer.util.ElementTreeItem;
import be.nabu.eai.developer.util.ElementTreeItem.ChildSelector;
import be.nabu.eai.developer.util.TypeGeneratorFactory;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.jfx.control.tree.drag.TreeDropListener;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.artifacts.api.DataSourceProviderArtifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.file.FileItem;
import be.nabu.libs.resources.memory.MemoryDirectory;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.services.jdbc.JDBCUtils;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeRegistry;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.RootElement;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.mask.MaskedContent;
import be.nabu.libs.types.properties.CollectionNameProperty;
import be.nabu.libs.types.properties.ForeignKeyProperty;
import be.nabu.libs.types.properties.LabelProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.PrimaryKeyProperty;
import be.nabu.libs.types.simple.Date;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.StructureInstance;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.mime.impl.FormatException;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

public class DataModelGUIManager extends BaseJAXBGUIManager<DataModelConfiguration, DataModelArtifact> implements TypeGeneratorTarget {

	private boolean editable = true;
	private boolean allowEditing = false;
	private boolean showAll = true;
	private AnchorPane pane;
	private DataModelArtifact model;
	private SplitPane split;
	
	// any pending changes because of update actions
	private List<DefinedType> pendingChanges = new ArrayList<DefinedType>();
	private boolean canBeSynchronized;
	private AnchorPane canvas;
	
	public DataModelGUIManager() {
		super("Data Model", DataModelArtifact.class, new DataModelManager(), DataModelConfiguration.class);
	}
	
	@Override
	public String getCategory() {
		return "Types";
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected DataModelArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new DataModelArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	public static void draw(DefinedTypeRegistry registry, AnchorPane pane) {
		DataModelGUIManager dataModelGUIManager = new DataModelGUIManager();
		dataModelGUIManager.setEditable(false);
		dataModelGUIManager.display(MainController.getInstance(), pane, toModel(registry));
	}
	
	// let's check that we have a saved version
	public static DataModelArtifact toModel(DefinedTypeRegistry registry) {
		DataModelArtifact model = new DataModelArtifact(registry.getId(), new MemoryDirectory(), EAIResourceRepository.getInstance());
		Entry entry = EAIResourceRepository.getInstance().getEntry(registry.getId());
		if (entry instanceof ResourceEntry) {
			Resource child = ((ResourceEntry) entry).getContainer().getChild("data-model.xml");
			if (child instanceof ReadableResource) {
				DataModelConfiguration config = null;
				try {
					JAXBContext context = JAXBContext.newInstance(DataModelConfiguration.class);
					Unmarshaller unmarshaller = context.createUnmarshaller();
					unmarshaller.setAdapter(new ArtifactXMLAdapter(EAIResourceRepository.getInstance()));
					ResourceReadableContainer container = new ResourceReadableContainer((ReadableResource) child);
					try {
						config = (DataModelConfiguration) unmarshaller.unmarshal(IOUtils.toInputStream(container));
						model.setConfig(config);
					}
					finally {
						container.close();
					}
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
		if (model.getConfig().getEntries() == null) {
			model.getConfig().setEntries(new ArrayList<DataModelEntry>());
		}
		Map<String, DataModelEntry> map = new HashMap<String, DataModelEntry>();
		Iterator<DataModelEntry> iterator = model.getConfig().getEntries().iterator();
		while (iterator.hasNext()) {
			DataModelEntry next = iterator.next();
			if (next == null || next.getType() == null) {
				iterator.remove();
			}
			else {
				map.put(next.getType().getId(), next);
			}
		}
		
		for (String namespace : registry.getNamespaces()) {
			for (ComplexType complexType : registry.getComplexTypes(namespace)) {
				if (complexType instanceof DefinedType) {
					String id = ((DefinedType) complexType).getId();
					// need to add it
					if (!map.containsKey(id)) {
						DataModelEntry single = new DataModelEntry();
						single.setType((DefinedType) complexType);
						model.getConfig().getEntries().add(single);
					}
					// remove it from the map so only the ones remain that are no longer in the registry
					else {
						map.remove(id);
					}
				}
			}
		}
		
		model.getConfig().getEntries().removeAll(map.values());
		
		return model;
	}
	
	@Override
	public void display(MainController controller, AnchorPane pane, DataModelArtifact model) {
		for (String dependency : model.getRepository().getDependencies(model.getId())) {
			Artifact resolve = model.getRepository().resolve(dependency);
			// we assume that a datasource provider that is also a service is basically a jdbcpoolartifact or something compatible...
			if (resolve instanceof DataSourceProviderArtifact && resolve instanceof DefinedService) {
				canBeSynchronized = true;
			}
		}
		// when you are defining data models that are to be used by other jdbc connections, you might not see it in dependencies
		// you still might need to tweak the behavior of what gets synced and what doesn't
		// so we set it to true always!
		canBeSynchronized = true;
		
		this.pane = pane;
		this.model = model;
		pane.getChildren().clear();
		
		split = new SplitPane();
		split.setOrientation(Orientation.HORIZONTAL);
		VBox total = new VBox();
//		AnchorPane.setBottomAnchor(total, 0d);
//		AnchorPane.setLeftAnchor(total, 0d);
//		AnchorPane.setRightAnchor(total, 0d);
//		AnchorPane.setTopAnchor(total, 0d);
		
//		total.prefWidthProperty().bind(pane.widthProperty());
//		total.prefHeightProperty().bind(pane.heightProperty());
//		pane.getChildren().add(total);
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		split.getItems().add(total);
		pane.getChildren().add(split);
		
		ScrollPane scroll = new ScrollPane();
		scroll.setFitToWidth(true);
		
		VBox.setVgrow(scroll, Priority.ALWAYS);
		
		BooleanProperty locked = controller.hasLock(model.getId());
		ObjectProperty<Pane> focused = new SimpleObjectProperty<Pane>();
		Map<String, VBox> drawn = new HashMap<String, VBox>();
		Map<String, List<Node>> shapes = new HashMap<String, List<Node>>();
		
		canvas = new AnchorPane();
		scroll.setContent(canvas);
		canvas.minHeightProperty().bind(scroll.heightProperty().subtract(25));
		
		if (editable) {
			canvas.addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
				@Override
				public void handle(DragEvent event) {
					if (locked.get()) {
						Dragboard dragboard = event.getDragboard();
						if (dragboard != null) {
							if (dragboard.hasFiles()) {
								event.acceptTransferModes(TransferMode.MOVE);
								event.consume();
							}
							else {
								Object content = dragboard.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedType.class)));
								// this will be the path in the tree
								if (content != null) {
									String artifactId = controller.getRepositoryBrowser().getControl().resolve((String) content).itemProperty().get().getId();
									if (artifactId != null) {
										Artifact resolve = model.getRepository().resolve(artifactId);
										if (resolve instanceof ComplexType) {
											event.acceptTransferModes(TransferMode.MOVE);
											event.consume();
										}
									}
								}
							}
						}
					}
				}
			});
			canvas.addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
				@SuppressWarnings("resource")
				@Override
				public void handle(DragEvent event) {
					if (locked.get()) {
						Dragboard dragboard = event.getDragboard();
						if (dragboard != null && !event.isDropCompleted() && !event.isConsumed()) {
							if (dragboard.hasFiles()) {
								List<File> files = dragboard.getFiles();
								fileLoop: for (File file : files) {
									FileItem fileItem = new FileItem(null, file, false);
									for (TypeGenerator generator : TypeGeneratorFactory.getInstance().getTypeGenerators()) {
										if (generator.processResource(fileItem, DataModelGUIManager.this)) {
											break fileLoop;
										}
									}
								}
							}
							else {
								Object content = dragboard.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedType.class)));
								// this will be the path in the tree
								if (content != null) {
									String artifactId = controller.getRepositoryBrowser().getControl().resolve((String) content).itemProperty().get().getId();
									if (artifactId != null) {
										Artifact resolve = model.getRepository().resolve(artifactId);
										if (resolve instanceof ComplexType) {
											DataModelEntry entry = new DataModelEntry();
											entry.setType((DefinedType) resolve);
											Point2D sceneToLocal = pane.sceneToLocal(event.getSceneX(), event.getSceneY());
											entry.setX((int) sceneToLocal.getX());
											entry.setY((int) sceneToLocal.getY());
											if (model.getConfig().getEntries() == null) {
												model.getConfig().setEntries(new ArrayList<DataModelEntry>());
											}
											model.getConfig().getEntries().add(entry);
											MainController.getInstance().setChanged();
											VBox draw = draw(model, entry, locked, focused, drawn, shapes);
											canvas.getChildren().add(draw);
											drawShapes(model, drawn, shapes, canvas, model.getConfig().getEntries());
											
											// bring new guy to the front
											toFront(entry.getType(), draw, shapes, drawn);
											
											// and expand it?
			//								Tree<?> tree = (Tree<?>) draw.lookup(".treeContainer");
			//								tree.getRootCell().expandAll(1);
										}
									}
								}
							}
						}
					}
				}
			});
		}
		HBox buttons = new HBox();
		buttons.setAlignment(Pos.CENTER_LEFT);
		buttons.setPadding(new Insets(10));
		
//		SplitMenuButton create = new SplitMenuButton();
//		create.getStyleClass().add("inline");
		Button create = new Button();
		create.setGraphic(MainController.loadFixedSizeGraphic("icons/add.png", 12));
		create.setText("Structure");
		create.getStyleClass().add("primary");
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				// TODO: request name, boolean for collectionable
				// if collectionable -> ask for collection name (placeholder is the default name)
				// make it generic so you can push a list of types prefilled with name etc -> can also use this from excel etc
				Structure structure = new Structure();
				structure.setName("unnamed");
				try {
					promptCreate(model, Arrays.asList(structure), false, null);
				}
				catch (Exception e) {
					MainController.getInstance().notify(e);
				}
			}
		});
		HBox.setMargin(create, new Insets(0, 2, 0, 0));
		buttons.getChildren().add(create);
		// TODO: get a list of other type providers that can suggest a number of types (give it a handler for suggestion, they can do popups etc as they see fit)
		
		Button delete = new Button("Delete Selected");
		Button export = new Button("Copy to clipboard (PNG)");
		Button synchronize = new Button("Synchronize To Database");
		synchronize.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				synchronize.setDisable(true);
				MainController.getInstance().submitTask("Synchronizing model", "Synchronizing data model: " + model.getId(), new Runnable() {
					@Override
					public void run() {
						synchronizeManagedTypes(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										synchronize.setDisable(false);
										MainController.getInstance().getNotificationHandler().notify("Synchronized the tables to the database(s)", 5000l, Severity.INFO);
									}
								});
							}
						});
					}
				});
			}
		});
		
		if (editable) {
			delete.disableProperty().bind(locked.not());
			create.disableProperty().bind(locked.not());
		}
		else {
			delete.setDisable(!editable);
			create.setDisable(!editable);
		}
		
		ComboBox<DataModelType> combo = new ComboBox<DataModelType>();
		combo.getItems().addAll(DataModelType.values());
		combo.getSelectionModel().select(model.getConfig().getType() == null ? DataModelType.DATABASE : model.getConfig().getType());
		
		combo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DataModelType>() {
			@Override
			public void changed(ObservableValue<? extends DataModelType> arg0, DataModelType arg1, DataModelType arg2) {
				if (locked.get()) {
					model.getConfig().setType(arg2);
					if (editable) {
						MainController.getInstance().setChanged();
					}
					else {
						persistLightly(model);
					}
				}
				for (Node node : drawn.values()) {
					Tree fromTree = (Tree<?>) node.lookup(".treeContainer");
					// refresh the tree!
					fromTree.refresh();
				}
			}
		});
		combo.disableProperty().bind(locked.not());
		
		Label viewLabel = new Label("View");
		viewLabel.setPadding(new Insets(10, 10, 10, 0));
		Separator separator = new Separator(Orientation.VERTICAL);
		HBox.setMargin(separator, new Insets(0, 10, 0, 10));
		buttons.getChildren().addAll(export, delete, synchronize, separator, viewLabel, combo);
		
		total.getChildren().addAll(buttons, scroll);
		
		if (editable) {
			delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					if (focused.get() != null) {
						delete(model, canvas, focused, drawn, shapes);
					}
				}
			});
		}
		
		// defocus
		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				if (!arg0.isConsumed()) {
					if (focused.get() != null) {
						focused.get().getStyleClass().remove("selectedInvoke");
						focused.set(null);
						toFront(null, null, shapes, drawn);
					}
				}
			}
		});
		
		export.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
				MainController.copy(snapshot);
			}
		});
		
		List<DataModelEntry> entries = model.getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				if (!(entry.getType() instanceof ComplexType)) {
					continue;
				}
				VBox draw = draw(model, entry, locked, focused, drawn, shapes);
				canvas.getChildren().add(draw);

				if (editable) {
					draw.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent arg0) {
							if (focused.get() != null && arg0.getCode() == KeyCode.DELETE) {
								DataModelEntry entryToDelete = (DataModelEntry) focused.get().getUserData();
								if (entryToDelete != null && entry.equals(entryToDelete)) {
									delete(model, canvas, focused, drawn, shapes);
								}
							}
						}
					});
				}
			}
			
			drawShapes(model, drawn, shapes, canvas, entries);
		}
		
		toFront(null, null, shapes, drawn);
	}
	
	private void synchronizeManagedTypes(EventHandler<ActionEvent> handler) {
		for (DefinedService service : getDependendPools(model)) {
			if (service != null) {
				try {
					synchronizeManagedTypes(service);
				}
				catch (Exception e) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							MainController.getInstance().notify(e);
						}
					});
				}
			}
		}
		handler.handle(null);
	}
	
	// shameless copy of jdbc pools, but don't want a dependency...
	private void synchronizeManagedTypes(Artifact artifact) throws InterruptedException, ExecutionException, ServiceException {
		Service service = (Service) EAIResourceRepository.getInstance().resolve("nabu.protocols.jdbc.pool.Services.synchronizeManagedTypes");
		if (service != null) {
			ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
			input.set("jdbcPoolId", artifact.getId());
			input.set("force", true);
			Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
			ServiceResult serviceResult = run.get();
			if (serviceResult.getException() != null) {
				throw serviceResult.getException();
			}
		}
	}

	private void drawShapes(DataModelArtifact model, Map<String, VBox> drawn, Map<String, List<Node>> shapes, AnchorPane canvas, List<DataModelEntry> entries) {
		// clear all shapes, we'll redraw them, too much work to delta this stuff
		for (List<Node> nodes : shapes.values()) {
			canvas.getChildren().removeAll(nodes);
		}
		shapes.clear();
		
		// after we draw them all, we start to draw the lines
		for (DataModelEntry entry : entries) {
			if (!(entry.getType() instanceof ComplexType)) {
				continue;
			}
			if (entry.getType().getSuperType() != null) {
				Type superType = entry.getType().getSuperType();
				// if we drew the supertype, add a line
				if (superType instanceof DefinedType && drawn.containsKey(((DefinedType) superType).getId())) {
					String superId = ((DefinedType) superType).getId();
					drawLine(drawn, shapes, canvas, entry.getType(), superId, null, null, "indexQueryLine");
				}
			}
			for (Element<?> element : getElements(model.getConfig().getType(), (ComplexType) entry.getType())) {
				Value<String> property = element.getProperty(ForeignKeyProperty.getInstance());
				if (property != null && property.getValue() != null) {
					String toId = property.getValue().split(":")[0];
					if (drawn.containsKey(toId)) {
						String fromPath = entry.getType().getName() + "/" + element.getName();
						String toPath = ((DefinedType) model.getRepository().resolve(toId)).getName() + "/" + property.getValue().split(":")[1];
						fromPath = element.getName();
						toPath = property.getValue().split(":")[1];
						drawLine(drawn, shapes, canvas, entry.getType(), toId, fromPath, toPath, "maskLine");
					}
				}
			}
		}
	}
	
	public List<Element<?>> getElements(DataModelType model, ComplexType type) {
		if (model == null || model == DataModelType.DATABASE) {
			return JDBCUtils.getFieldsInTable(type);
		}
		else if (model == DataModelType.LOCAL) {
			return TypeUtils.getLocalChildren(type);
		}
		else {
			return new ArrayList<Element<?>>(TypeUtils.getAllChildren(type));
		}
	}

	private void drawLine(Map<String, VBox> drawn, Map<String, List<Node>> shapes, AnchorPane canvas, DefinedType entry, String toId, String fromCellPath, String toCellPath, String style) {
		Tree fromTree = (Tree<?>) drawn.get(entry.getId()).lookup(".treeContainer");
		Tree toTree = (Tree<?>) drawn.get(toId).lookup(".treeContainer");
		
		TreeCell<?> fromCell = fromCellPath == null ? fromTree.getRootCell() : fromTree.getTreeCell(fromTree.resolve(fromCellPath));
		TreeCell<?> toCell = toCellPath == null ? toTree.getRootCell() : toTree.getTreeCell(toTree.resolve(toCellPath));
		
		Line line;
		if (showAll) {
			if (style.equals("maskLine")) {
				line = getLine(drawn, entry.getId(), toId, fromCell, toCell);	
			}
			else {
				// if we don't do the lookup, we get the whole box which is not bad but not awesome either
				// just the label at the top is ideal
				Pane fromPane = (Pane) drawn.get(entry.getId()).lookup(".invokeWrapper");
				Pane toPane = (Pane) drawn.get(toId).lookup(".invokeWrapper");
				line = getLine(drawn, entry.getId(), toId, fromPane, toPane);
			}
		}
		else {
			line = getLine(drawn, entry.getId(), toId, fromCell, toCell);
		}
		List<Shape> arrow1 = EAIDeveloperUtils.drawArrow(line, 0.5, 15d);
		
		// again borrow styling from blox
		line.getStyleClass().add(style);
		
		if (style.equals("maskLine")) {
			line.getStrokeDashArray().addAll(10d, 5d);
			line.setStrokeDashOffset(0);
		}
		
		for (Shape shape : arrow1) {
			shape.getStyleClass().add(style);	
		}
		
		if (!shapes.containsKey(entry.getId())) {
			shapes.put(entry.getId(), new ArrayList<Node>());
		}
		shapes.get(entry.getId()).add(line);
		shapes.get(entry.getId()).addAll(arrow1);
		
		// add them to both
		if (!shapes.containsKey(toId)) {
			shapes.put(toId, new ArrayList<Node>());
		}
		shapes.get(toId).add(line);
		shapes.get(toId).addAll(arrow1);
		
		canvas.getChildren().addAll(line);
		canvas.getChildren().addAll(arrow1);
		
		Circle fromCircle = new Circle();
		fromCircle.centerXProperty().bind(line.startXProperty());
		fromCircle.centerYProperty().bind(line.startYProperty());
		fromCircle.setRadius(2);
		fromCircle.getStyleClass().addAll("connectionCircle", style);
//		fromCircle.setManaged(false);
		
		Circle toCircle = new Circle();
		toCircle.centerXProperty().bind(line.endXProperty());
		toCircle.centerYProperty().bind(line.endYProperty());
		toCircle.setRadius(2);
		toCircle.getStyleClass().addAll("connectionCircle", style);
//		toCircle.setManaged(false);
		
		canvas.getChildren().addAll(fromCircle, toCircle);
		shapes.get(entry.getId()).add(fromCircle);
		shapes.get(entry.getId()).add(toCircle);
		shapes.get(toId).add(fromCircle);
		shapes.get(toId).add(toCircle);
	}

	private void toFront(DefinedType entry, VBox child, Map<String, List<Node>> shapes,  Map<String, VBox> drawn) {
		// shapes to the back
		for (List<Node> nodes : shapes.values()) {
			for (Node node : nodes) {
				node.toBack();
				// make partially translucent so as not to overcrowd
				node.setOpacity(0.3);
			}
		}

		// if we have a child, make sure it and any shapes related to it are front and center
		// we leave the drawn vboxes as is so you can play with the z-index yourself by clicking around
		
		if (child != null) {
			child.toFront();
			// any shapes related to this child should be front and center
			List<Node> list = shapes.get(entry.getId());
			if (list != null) {
				for (Node node : list) {
					node.toFront();
					node.setOpacity(1);
				}
			}
		}
	}
	
	private void persistLightly(DataModelArtifact model) {
		Entry entry = EAIResourceRepository.getInstance().getEntry(model.getId());
		if (entry instanceof ResourceEntry) {
			try {
				Resource child = ResourceUtils.touch(((ResourceEntry) entry).getContainer(), "data-model.xml");
				WritableContainer<ByteBuffer> writable = new ResourceWritableContainer((WritableResource) child);
				try {
					JAXBContext context = JAXBContext.newInstance(DataModelConfiguration.class);
					Marshaller marshaller = context.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
					marshaller.setAdapter(new ArtifactXMLAdapter(EAIResourceRepository.getInstance()));
					marshaller.marshal(model.getConfig(), IOUtils.toOutputStream(writable));
				}
				finally {
					writable.close();
				}
			}
			catch (Exception e) {
				MainController.getInstance().notify(e);
			}
		}
	}
	
	@Override
	protected BaseArtifactGUIInstance<DataModelArtifact> newGUIInstance(Entry entry) {
		BaseArtifactGUIInstance<DataModelArtifact> instance = new BaseArtifactGUIInstance<DataModelArtifact>(this, entry) {
			@Override
			public List<Validation<?>> save() throws IOException {
				List<Validation<?>> messages = super.save();
				// save all related nodes in same folder
				if (allowEditing) {
					List<DataModelEntry> entries = getArtifact().getConfig().getEntries();
					if (entries != null) {
						for (DataModelEntry entry : entries) {
							if (allowEditing(getArtifact(), entry)) {
								List<Validation<?>> save = new StructureManager().save(
									(ResourceEntry) getArtifact().getRepository().getEntry(((DefinedType) entry.getType()).getId()), 
									(DefinedStructure) entry.getType()
								);
								if (save != null) {
									messages.addAll(save);
								}
							}
						}
					}
				}
				// save any pending changes as well
				if (!pendingChanges.isEmpty()) {
					for (DefinedType defined : pendingChanges) {
						Entry entry = model.getRepository().getEntry(defined.getId());
						if (entry instanceof ResourceEntry) {
							try {
								new StructureManager().saveContent((ResourceEntry) entry, (ComplexType) defined);
								EAIDeveloperUtils.updated(entry.getId());
								display(MainController.getInstance(), pane, model);
							}
							catch (Exception e) {
								MainController.getInstance().notify(e);
							}
						}
					}
					pendingChanges.clear();
				}
				return messages;
			}
		};
		return instance;
	}
	
	private boolean allowEditing(DataModelArtifact model, DataModelEntry entry) {
		Entry repositoryEntry = entry.getType() instanceof DefinedType && entry.getType() instanceof Structure ? model.getRepository().getEntry(((DefinedType) entry.getType()).getId()) : null;
		// only allow in folder itself (recursively)
		if (repositoryEntry != null && repositoryEntry instanceof ResourceEntry && repositoryEntry.getId().startsWith(model.getId().replaceAll("[^.]+$", ""))) {
			return true;
		}
		return false;
	}

	private VBox draw(DataModelArtifact model, DataModelEntry entry, BooleanProperty locked, ObjectProperty<Pane> focused, Map<String, VBox> drawn, Map<String, List<Node>> shapes) {
		try {
			VBox child = new VBox();
			drawn.put(entry.getType().getId(), child);
			// save the entry for easy removal etc
			child.setUserData(entry);
			// borrowing from blox...
			child.getStyleClass().add("invokeWrapper");
			HBox name = new HBox();
			name.getStyleClass().add("invokeName");
			name.setUserData(entry);
			
			CheckBox synchronizable = new CheckBox();
			HBox.setMargin(synchronizable, new Insets(5));
			if (canBeSynchronized) {
				name.getChildren().add(synchronizable);
			}
			
			CustomTooltip tooltip = new CustomTooltip("");
			tooltip.install(synchronizable);
			setTooltip(synchronizable, entry, tooltip);
			
			VBox mainNameBox = new VBox();
			Label subscript = new Label(entry.getType().getId());

			// TODO: need pretty name here, either based on the entry (if its a node in the tree) or the type (label property etc)
			// also add an edit pencil icon? don't allow editing inline, just in the popup?
			// at least allow synchronization toggle?
			
			// the name is both for clarification and as a select/drag target!
			Label nameLabel = new Label(EAICollectionUtils.getPrettyName(entry.getType()));
			nameLabel.getStyleClass().add("invokeServiceName");
			nameLabel.setPadding(new Insets(5));
			
			mainNameBox.getChildren().add(nameLabel);
			subscript.getStyleClass().add("invokeSubscript");
			mainNameBox.getChildren().add(subscript);
			
			name.getChildren().add(mainNameBox);
			
			HBox spacer = new HBox();
			HBox.setHgrow(spacer, Priority.ALWAYS);
			name.getChildren().add(spacer);
			
			HBox buttons = new HBox();
			name.getChildren().add(buttons);
			HBox.setMargin(buttons, new Insets(0, 0, 0, 5));
			
			child.getStyleClass().add("service");
			EventHandler<MouseEvent> clickHandler = new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent arg0) {
					if (focused.get() != null) {
						focused.get().getStyleClass().remove("selectedInvoke");
					}
					toFront(entry.getType(), child, shapes, drawn);
					name.getStyleClass().add("selectedInvoke");
					focused.set(name);
					child.requestFocus();
					arg0.consume();
				}
			};
			child.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
			
			child.getChildren().add(name);
			
			child.getStyleClass().add("small");
			
			StructureGUIManager structureGUIManager = new StructureGUIManager();
			structureGUIManager.setAllowComplexChildren(false);
			structureGUIManager.setActualId(model.getId());
			
			// not doing inline editing atm as the menus are too big
			// entry.getType() instanceof Structure
			
			Tree<Element<?>> display;
			if (allowEditing) {
				AnchorPane anchorPane = new AnchorPane();
				
				display = structureGUIManager.display(MainController.getInstance(), anchorPane, new RootElementWithPush((ComplexType) entry.getType(), true), 
						entry.getType() instanceof Structure && allowEditing(model, entry), false);
				
				ElementTreeItem elementTreeItem = (ElementTreeItem) display.rootProperty().get();
				elementTreeItem.setChildSelector(new ChildSelector() {
					@Override
					public List<Element<?>> getChildren(ComplexType type) {
						return getElements(model.getConfig().getType(), type);
					}
				});
				// make sure we see the new selection of items, otherwise the child selector is not used
				display.refresh();
				
				// hide buttons and stuff
				Node lookup = anchorPane.lookup(".structure-all-buttons");
				if (lookup != null) {
//					lookup.setVisible(false);
//					lookup.setManaged(false);
					child.getChildren().add(lookup);
					((HBox) lookup).setPadding(new Insets(0));
				}
				lookup = anchorPane.lookup(".structure-move-buttons");
				if (lookup != null) {
//					lookup.setVisible(false);
//					lookup.setManaged(false);
				}
				mainNameBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent arg0) {
						ElementSelectionListener listener = structureGUIManager.getElementSelectionListener();
						if (listener != null) {
							listener.changed(null, null, display.getRootCell());
						}
					}
				});
			}
			else {
				display = new Tree<Element<?>>(new ElementMarshallable(), null, StructureGUIManager.newCellDescriptor());
				EAIDeveloperUtils.addElementExpansionHandler(display);
				display.setClipboardHandler(new ElementClipboardHandler(display, false));
				display.setReadOnly(true);
				ElementTreeItem elementTreeItem = new ElementTreeItem(new RootElement((ComplexType) entry.getType()), null, false, false);
				elementTreeItem.setChildSelector(new ChildSelector() {
					@Override
					public List<Element<?>> getChildren(ComplexType type) {
						return getElements(model.getConfig().getType(), type);
					}
				});
				display.rootProperty().set(elementTreeItem);
				display.setClipboardHandler(new ElementClipboardHandler(display, false));
				ElementSelectionListener elementSelectionListener = new ElementSelectionListener(MainController.getInstance(), false);
				elementSelectionListener.setActualId(entry.getType().getId());
				display.getSelectionModel().selectedItemProperty().addListener(elementSelectionListener);
				
				// if we can edit but are not using inline editing, allow opening in side panel!
				if (editable && entry.getType() instanceof Structure) {
					Entry repoEntry = model.getRepository().getEntry(entry.getType().getId());
					// only allow editing if you can save it (e.g. not from UMLs etc)
					if (repoEntry instanceof RepositoryEntry) {
						Label edit = new Label();
						edit.setGraphic(MainController.loadFixedSizeGraphic("icons/edit.png", 12));
						edit.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
							@Override
							public void handle(MouseEvent arg0) {
								try {
									promptCreate(model, Arrays.asList((Structure) entry.getType()), true, null);
								}
								catch (Exception e) {
									MainController.getInstance().notify(e);
								}
							}
						});
						HBox.setMargin(edit, new Insets(5));
						buttons.getChildren().add(edit);
					}
				}
			}
			
			if (entry.getType() instanceof DefinedType) {
				display.setId(((DefinedType) entry.getType()).getId());
			}
			
			TreeDragDrop.makeDraggable(display, new ElementLineConnectListener(canvas));
			TreeDragDrop.makeDroppable(display, new TreeDropListener<Element<?>>() {
				@Override
				public boolean canDrop(String dataType, TreeCell<Element<?>> target, TreeCell<?> dragged, TransferMode transferMode) {
					// can not drop on our own tree
					if (dragged.getTree().equals(display)) {
						return false;
					}
					// if the tree does not have an id, we can't drop a line to it (don't know which type)
					else if (target.getTree().getId() == null) {
						return false;
					}
					// currently we only allow dropping on primary keys (for foreign keys)
					Element<?> element = target.getItem().itemProperty().get();
					Value<Boolean> primaryKey = element.getProperty(PrimaryKeyProperty.getInstance());
					return primaryKey != null && primaryKey.getValue() != null && primaryKey.getValue();
				}
				@Override
				public void drop(String dataType, TreeCell<Element<?>> target, TreeCell<?> dragged, TransferMode transferMode) {
					Element<?> element = (Element<?>) dragged.getItem().itemProperty().get();
					element.setProperty(new ValueImpl<String>(ForeignKeyProperty.getInstance(), target.getTree().getId() + ":" + target.getItem().itemProperty().get().getName()));
					MainController.getInstance().setChanged();
					display(MainController.getInstance(), pane, model);
				}
			});
			
			List<DefinedService> dependendPools = getDependendPools(model);
			if (dependendPools.size() == 1) {
				Label browse = new Label();
				browse.setGraphic(MainController.loadFixedSizeGraphic("icons/search.png", 12));
				browse.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent arg0) {
						browse(entry.getType().getId(), dependendPools.get(0).getId());
					}
				});
				HBox.setMargin(browse, new Insets(5));
				buttons.getChildren().add(0, browse);
			}
			
			Label open = new Label();
			open.setGraphic(MainController.loadFixedSizeGraphic("right-chevron.png", 12));
			open.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent arg0) {
					MainController.getInstance().open(entry.getType().getId());
				}
			});
			HBox.setMargin(open, new Insets(5));
			buttons.getChildren().add(open);
			
			if (showAll) {
				display.getRootCell().expandedProperty().set(true);
				display.getRootCell().hideSelfProperty().set(true);
			}
			else {
				display.getTreeCell(display.rootProperty().get()).expandedProperty().set(false);
			}
			display.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
			
			child.getChildren().add(display);
			
			display.getRootCell().getNode().getStyleClass().add("invokeTree");
			
			// doesn't work in this context? the invoke wrapper isn't using it either
			display.prefWidthProperty().unbind();
			display.prefHeightProperty().unbind();
			
			display.resize();
			display.getStyleClass().add("treeContainer");
			display.getStyleClass().add("interface");
			display.getStyleClass().add("interfaceContainer");
			
			// initial resize won't work?
			display.setPrefWidth(100);
			
			display.minWidthProperty().unbind();
			display.minWidthProperty().bind(name.widthProperty());
			
			child.setLayoutX(entry.getX());
			child.setLayoutY(entry.getY());

			// always make it movable to make it more readable
			MovablePane makeMovable = MovablePane.makeMovable(child, new SimpleBooleanProperty(true));
			
			makeMovable.xProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					if (locked.get()) {
						entry.setX(newValue.intValue());
						if (editable) {
							MainController.getInstance().setChanged();
						}
					}
				}
			});
			makeMovable.yProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					if (locked.get()) {
						entry.setY(newValue.intValue());
						if (editable) {
							MainController.getInstance().setChanged();
						}
					}
				}
			});

			// if not editable, we don't want to save continuously, only when you stop dragging
			if (!editable) {
				makeMovable.setDragDoneHandler(new EventHandler<DragEvent>() {
					@Override
					public void handle(DragEvent arg0) {
						persistLightly(model);
					}
				});
			}
			
			return child;
		} 
		catch (Exception e) {
			MainController.getInstance().notify(e);
		}
		return null;
	}

	private void setTooltip(CheckBox box, DataModelEntry entry, CustomTooltip tooltip) {
		String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), entry.getType().getProperties());
		if (collectionName == null) {
			collectionName = entry.getType().getName();
		}
		if (entry.isSynchronize()) {
			tooltip.setText("This type is synchronized with the database table: " + NamingConvention.UNDERSCORE.apply(collectionName));
			box.setSelected(true);
		}
		else {
			tooltip.setText("This type is NOT synchronized. Enable to synchronize it with the database table: " + NamingConvention.UNDERSCORE.apply(collectionName));
			box.setSelected(false);
		}
		box.setDisable(!editable);
		box.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), entry.getType().getProperties());
				if (collectionName == null) {
					collectionName = entry.getType().getName();
					// if we are enabling it and we don't have a collection name, fill it in!
					if (arg2 != null && arg2) {
						((ModifiableType) entry.getType()).setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), collectionName));
						pendingChanges.add(entry.getType());
					}
				}
				if (arg2 != null && arg2) {
					tooltip.setText("This type is synchronized with the database table: " + NamingConvention.UNDERSCORE.apply(collectionName));
				}
				else {
					tooltip.setText("This type is NOT synchronized. Enable to synchronize it with the database table: " + NamingConvention.UNDERSCORE.apply(collectionName));
				}
				entry.setSynchronize(box.isSelected());
				MainController.getInstance().setChanged();
			}
		});
	}
	
	// very lame method because we want effectively final...
	private List<Node> ensure(List<Node> node) {
		return node == null ? new ArrayList<Node>() : node;
	}
	
	private void delete(DataModelArtifact model, AnchorPane canvas, ObjectProperty<Pane> focused, Map<String, VBox> drawn, Map<String, List<Node>> shapes) {
		DataModelEntry entry = (DataModelEntry) focused.get().getUserData();
		String id = entry.getType().getId();
		model.getConfig().getEntries().remove(entry);
		MainController.getInstance().setChanged();
		focused.set(null);
		
		List<Node> removed = ensure(shapes.remove(entry.getType().getId()));
		VBox remove = drawn.remove(entry.getType().getId());
		if (remove != null) {
			removed.add(0, remove);
		}

		Entry child = model.getRepository().getEntry(id);
		if (child != null && child.getParent() instanceof RepositoryEntry) {
			EventHandler<ActionEvent> cancelHandler = new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					canvas.getChildren().removeAll(removed);
				}
			};
			Confirm.confirm(ConfirmType.QUESTION, "Delete structure?", "Do you want to delete the underlying data type as well?", new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						EAIDeveloperUtils.delete(child.getId());
						// if we are synchronizing it to a data source provider, delete it
						if (entry.isSynchronize()) {
							List<DefinedService> services = getDependendPools(model);
							if (!services.isEmpty()) {
								Confirm.confirm(ConfirmType.QUESTION, "Delete table(s)?", "Do you want to delete the underlying table(s) as well?", new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent arg0) {
										String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), entry.getType().getProperties());
										if (collectionName == null) {
											collectionName = entry.getType().getName();
										}
										for (DefinedService service : services) {
											ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
											input.set("sql", "drop table " + NamingConvention.UNDERSCORE.apply(collectionName));
											Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
											try {
												ServiceResult serviceResult = run.get();
												if (serviceResult.getException() != null) {
													MainController.getInstance().notify(serviceResult.getException());
												}
											}
											catch (Exception e) {
												MainController.getInstance().notify(e);
											}
										}
										canvas.getChildren().removeAll(removed);
									}
								}, cancelHandler);
							}
							else {
								canvas.getChildren().removeAll(removed);
							}
						}
						else {
							canvas.getChildren().removeAll(removed);
						}
					}
					catch (Exception e) {
						MainController.getInstance().notify(e);
					}
				}

			}, cancelHandler);
		}
		else {
			canvas.getChildren().removeAll(removed);
		}
	}
	
	private List<DefinedService> getDependendPools(DataModelArtifact model) {
		List<String> dependencies = model.getRepository().getDependencies(model.getId());
		List<DefinedService> services = new ArrayList<DefinedService>();
		for (String dependency : dependencies) {
			Artifact resolve = model.getRepository().resolve(dependency);
			// we assume that a datsource provider that is also a service is basically a jdbcpoolartifact or something compatible...
			if (resolve instanceof DataSourceProviderArtifact && resolve instanceof DefinedService) {
				services.add((DefinedService) resolve);
			}
		}
		return services;
	}
	
	public static Line getLine(Map<String, VBox> drawn, String fromId, String toId, Pane from, Pane to) {
		Line line = new Line();
		
		// we use the names to do correct Y positioning on the thing
		Pane fromName = (Pane) from.lookup(".invokeName");
		Pane toName = (Pane) to.lookup(".invokeName");
		
		Endpoint fromOrigin = new Endpoint(drawn.get(fromId).layoutXProperty(), drawn.get(fromId).layoutYProperty());
		
		Endpoint fromLeft = new Endpoint(from.layoutXProperty(), from.layoutYProperty().add(fromName.heightProperty().divide(2)));
		Endpoint fromRight = new Endpoint(from.layoutXProperty().add(from.widthProperty()), from.layoutYProperty().add(fromName.heightProperty().divide(2)));
		
		Endpoint toOrigin = new Endpoint(drawn.get(toId).layoutXProperty(), drawn.get(toId).layoutYProperty());
		Endpoint toLeft = new Endpoint(to.layoutXProperty(), to.layoutYProperty().add(toName.heightProperty().divide(2)));
		Endpoint toRight = new Endpoint(to.layoutXProperty().add(to.widthProperty()), to.layoutYProperty().add(toName.heightProperty().divide(2)));
		
		EndpointPicker endpointPicker = new EndpointPicker(toOrigin, fromLeft, fromRight);
		line.startXProperty().bind(endpointPicker.xProperty());
		line.startYProperty().bind(endpointPicker.yProperty());
		
		endpointPicker = new EndpointPicker(fromOrigin, toLeft, toRight);
		line.endXProperty().bind(endpointPicker.xProperty());
		line.endYProperty().bind(endpointPicker.yProperty());
		
		return line;
	}
	
	public static Line getLine(Map<String, VBox> drawn, String fromId, String toId, TreeCell<?> from, TreeCell<?> to) {
		Line line = new Line();
		
		Endpoint fromOrigin = new Endpoint(drawn.get(fromId).layoutXProperty(), drawn.get(fromId).layoutYProperty());
		Endpoint fromLeft = new Endpoint(from.leftAnchorXProperty().add(drawn.get(fromId).layoutXProperty()), from.leftAnchorYProperty().add(drawn.get(fromId).layoutYProperty()));
		Endpoint fromRight = new Endpoint(from.rightAnchorXProperty().add(10).add(drawn.get(fromId).layoutXProperty()), from.rightAnchorYProperty().add(drawn.get(fromId).layoutYProperty()));
		
		Endpoint toOrigin = new Endpoint(drawn.get(toId).layoutXProperty(), drawn.get(toId).layoutYProperty());
		Endpoint toLeft = new Endpoint(to.leftAnchorXProperty().add(drawn.get(toId).layoutXProperty()), to.leftAnchorYProperty().add(drawn.get(toId).layoutYProperty()));
		Endpoint toRight = new Endpoint(to.rightAnchorXProperty().add(10).add(drawn.get(toId).layoutXProperty()), to.rightAnchorYProperty().add(drawn.get(toId).layoutYProperty()));
		
		EndpointPicker endpointPicker = new EndpointPicker(toOrigin, fromLeft, fromRight);
		line.startXProperty().bind(endpointPicker.xProperty());
		line.startYProperty().bind(endpointPicker.yProperty());
		
		endpointPicker = new EndpointPicker(fromOrigin, toLeft, toRight);
		line.endXProperty().bind(endpointPicker.xProperty());
		line.endYProperty().bind(endpointPicker.yProperty());
		
		return line;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	private void hide(Object popup) {
		if (popup instanceof Stage) {
			((Stage) popup).hide();
		}
		else if (popup instanceof Node) {
			split.getItems().remove((Node) popup);
		}
	}
	private VBox buildPopup(Pane root) {
		VBox popup = new VBox();
		popup.getStyleClass().addAll("sidebar");
		popup.getChildren().addAll(root);
		VBox.setVgrow(root, Priority.ALWAYS);
		// if we already opened some other screen, just close it!
		if (split.getItems().size() >= 2) {
			split.getItems().remove(1);
		}
		split.getItems().add(popup);
		return popup;
	}
	
	public void promptCreate(DataModelArtifact model, List<Structure> types, boolean update, ResultHandler resultHandler) throws IOException, ParseException {
		if (types != null && !types.isEmpty()) {
			VBox root = new VBox();
			root.getStyleClass().addAll("project", "popup-form");
			
			Label title = new Label((update ? "Update" : "Create") + " Type" + (types.size() == 1 ? "" : "s"));
			title.getStyleClass().add("h1");
			TilePane tiles = new TilePane();
			tiles.setTileAlignment(Pos.CENTER);
			tiles.setAlignment(Pos.CENTER);
			tiles.setVgap(2);
			tiles.setHgap(2);
			tiles.setPrefColumns(Math.min(types.size(), 3));
			
			root.getChildren().addAll(title);
			
//			Stage popup = EAIDeveloperUtils.buildPopup("Create Types", root, MainController.getInstance().getActiveStage(), StageStyle.DECORATED, false);
			Object popup = buildPopup(root);
			
			boolean hasMultiple = types.size() > 1;
			ScrollPane scroll = new ScrollPane();
			scroll.setPadding(new Insets(10));
			scroll.setContent(tiles);
			scroll.setFitToWidth(true);
			scroll.setFitToHeight(true);
			scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
			// we set 400 for the tree, add some form fields and buttons...
//			scroll.setMinHeight(hasMultiple ? 650 : 600);
			root.getChildren().add(scroll);
			VBox.setVgrow(scroll, Priority.ALWAYS);
			
			Map<Structure, BooleanProperty> addBooleans = new HashMap<Structure, BooleanProperty>();
			Map<Structure, BooleanProperty> insertBooleans = new HashMap<Structure, BooleanProperty>();
			for (Structure type : types) {
				SimpleBooleanProperty add = hasMultiple && !update ? new SimpleBooleanProperty(true) : null;
				addBooleans.put(type, add);
				
				SimpleBooleanProperty insert = resultHandler != null && resultHandler.hasData(type) ? new SimpleBooleanProperty(true) : null;
				tiles.getChildren().add(promptCreate(model, type, add, insert));
				insertBooleans.put(type, insert);
			}
			
			HBox buttons = new HBox();
			buttons.getStyleClass().add("buttons");
			Button create = new Button("Create");
			create.getStyleClass().add("primary");
			create.addEventHandler(ActionEvent.ANY, new javafx.event.EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Entry entry = model.getRepository().getEntry(model.getId());
					Entry typesEntry = getTypesEntry((RepositoryEntry) entry.getParent());
					if (typesEntry instanceof RepositoryEntry) {
						Map<Structure, Entry> result = new HashMap<Structure, Entry>();
						for (Structure structure : types) {
							try {
								if (addBooleans.get(structure) == null || addBooleans.get(structure).get()) {
									String typeName = structure.getName();
									int counter = 1;
									while (typesEntry.getChild(typeName) != null) {
										typeName = structure.getName() + counter++;
									}
									RepositoryEntry createNode = ((RepositoryEntry) typesEntry).createNode(typeName, new StructureManager(), true);
									new StructureManager().saveContent(createNode, structure);
									EAIDeveloperUtils.created(createNode.getId());
									
									boolean toInsert = false;
									if (insertBooleans.get(structure) != null && insertBooleans.get(structure).get()) {
										result.put(structure, createNode);
										toInsert = true;
									}
									
									// get the new structure so we can add it to our model!
									DefinedType artifact = (DefinedType) createNode.getNode().getArtifact();
									DataModelEntry dataModelEntry = new DataModelEntry();
									dataModelEntry.setType(artifact);
									// if we want to insert the data, we definitely want to sync it
									dataModelEntry.setSynchronize(toInsert || ValueUtils.getValue(CollectionNameProperty.getInstance(), structure.getProperties()) != null);
									if (model.getConfig().getEntries() == null) {
										model.getConfig().setEntries(new ArrayList<DataModelEntry>());
									}
									model.getConfig().getEntries().add(dataModelEntry);
									MainController.getInstance().setChanged();
									// redraw!
									display(MainController.getInstance(), pane, model);
								}
							}
							catch (Exception e) {
								MainController.getInstance().notify(e);
							}
						}
						if (resultHandler != null && !result.isEmpty()) {
							resultHandler.handle(result);
						}
					}
					hide(popup);
				}
			});
			Button updateButton = new Button("Update");
			updateButton.getStyleClass().add("primary");
			if (update) {
				for (Structure structure : types) {
					pendingChanges.add((DefinedType) structure);
				}
			}
			updateButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						MainController.getInstance().save(model.getId());
					}
					catch (IOException e) {
						MainController.getInstance().notify(e);
					}
					hide(popup);
				}
			});
			
			Button close = new Button("Cancel");
			buttons.getChildren().addAll(update ? updateButton : create, close);
			root.getChildren().add(buttons);
			close.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					hide(popup);
					// if we are updating, we should really reload our structures, you may have made changes that should be reverted
					if (update) {
						// remove from pending, we are resetting them
						for (Structure structure : types) {
							pendingChanges.remove((DefinedType) structure);
						}
						// reload them
						for (Structure structure : types) {
							MainController.getInstance().getRepository().reload(((DefinedType) structure).getId());
							// reload any data entries!
							for (DataModelEntry entry : model.getConfig().getEntries()) {
								if (entry.getType().getId().equals((((DefinedType) structure).getId()))) {
									entry.setType((DefinedType) MainController.getInstance().getRepository().resolve(entry.getType().getId()));
								}
							}
						}
						// redraw it all!
						display(MainController.getInstance(), pane, model);
					}
				}
			});
			// can't seem to properly resize it (once live), the root and scene etc don't change size when conditional content is added
//			stage.sizeToScene();
			if (popup instanceof Stage) {
				((Stage) popup).show();
			}
			// we add margin instead to the buttons...lame resize thing
			VBox.setMargin(buttons, new Insets(0, 0, 20, 0));
		}
	}
	
	private Entry getTypesEntry(RepositoryEntry parent) {
		return EAIDeveloperUtils.mkdir(parent, "types");
	}
	
	public Node promptCreate(DataModelArtifact model, Structure type, BooleanProperty add, BooleanProperty insert) throws IOException, ParseException {
		VBox root = new VBox();
		root.getStyleClass().addAll("section", "block");
		root.setPadding(new Insets(10));
		
		if (add != null) {
			CheckBox checkBox = new CheckBox();
			HBox checkBoxBox = EAIDeveloperUtils.newHBox("Create new type", checkBox);
			checkBoxBox.setPadding(new Insets(2));
			checkBox.setSelected(add.get());
			add.bind(checkBox.selectedProperty());
			root.getChildren().add(checkBoxBox);
		}
		
		SimpleBooleanProperty editable = new SimpleBooleanProperty(true);
		if (add != null) {
			editable.set(add.get());
			editable.bind(add);
		}
		
		TextField name = new TextField();
		name.disableProperty().bind(editable.not());
		TextField collectionName = new TextField();
		collectionName.disableProperty().bind(editable.not());
		String collectionNameValue = ValueUtils.getValue(CollectionNameProperty.getInstance(), type.getProperties());
		if (collectionNameValue != null) {
			collectionName.setText(NamingConvention.UNDERSCORE.apply(collectionNameValue));
		}
		CheckBox box = new CheckBox();
		// by default we assume you want to store it in the database, this also solves the nasty stage resize thing when the new field pops in...
		box.setSelected(true);
		boolean allowTogglingDatabase = false;
		name.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				if (arg2 == null || arg2.trim().isEmpty()) {
					arg2 = "unnamed";
				}
				String goodName = NamingConvention.LOWER_CAMEL_CASE.apply(NamingConvention.UNDERSCORE.apply(arg2));
				type.setName(goodName);
				// if we haven't filled anything in explicitly, we set the prompt when it is updated
				if (allowTogglingDatabase && box.isSelected() && (collectionName.getText() == null || collectionName.getText().trim().isEmpty())) {
					collectionName.setPromptText(goodName.equals("unnamed") ? goodName : goodName + "s");
					type.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), collectionName.getPromptText()));
				}
				type.setProperty(new ValueImpl<String>(LabelProperty.getInstance(), !arg2.equals(goodName) ? arg2 : null));
			}
		});
		if (type.getName() != null) {
			name.setText(type.getName());
		}
		else {
			name.setText("unnamed");
		}
		collectionName.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				String name = arg2 == null || arg2.trim().isEmpty() ? collectionName.getPromptText() : NamingConvention.LOWER_CAMEL_CASE.apply(NamingConvention.UNDERSCORE.apply(arg2));
				if (name != null && !name.trim().isEmpty()) {
					type.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), name));
				}
			}
		});
		HBox nameBox = EAIDeveloperUtils.newHBox("Type Name", name);
		nameBox.setPadding(new Insets(2));
		root.getChildren().add(nameBox);
		HBox collectionNameBox = EAIDeveloperUtils.newHBox("Table Name", collectionName);
		collectionNameBox.setPadding(new Insets(2));
		box.disableProperty().bind(editable.not());
		box.setPadding(new Insets(10));
		if (allowTogglingDatabase) {
			HBox checkBoxBox = EAIDeveloperUtils.newHBox("Link to dabase?", box);
			checkBoxBox.setPadding(new Insets(2));
			root.getChildren().addAll(checkBoxBox);
			collectionNameBox.visibleProperty().bind(box.selectedProperty());
			collectionNameBox.managedProperty().bind(box.selectedProperty());
		}
		root.getChildren().add(collectionNameBox);
		
		box.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				if (arg2 != null && arg2) {
					String name = collectionName.getText();
					if (name == null || name.trim().isEmpty()) {
						name = collectionName.getPromptText();
					}
					if (name != null && !name.trim().isEmpty()) {
						type.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), name));	
					}
				}
				else {
					type.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), null));
				}
			}
		});
		
		StructureGUIManager structureGUIManager = new StructureGUIManager();
		structureGUIManager.setActualId(model.getId());
		structureGUIManager.setAllowComplexChildren(false);
		VBox treeDisplay = new VBox();
		treeDisplay.disableProperty().bind(editable.not());
		treeDisplay.getStyleClass().add("small");
		treeDisplay.autosize();
		treeDisplay.setMaxHeight(400);
		treeDisplay.setPrefHeight(400);
		
		treeDisplay.getStyleClass().add("hardreset");
		
		Tree<Element<?>> display = structureGUIManager.display(MainController.getInstance(), treeDisplay, new RootElementWithPush(type, true), type instanceof Structure, false);
		root.getChildren().add(treeDisplay);
		String value = ValueUtils.getValue(CollectionNameProperty.getInstance(), type.getProperties());
		if (value == null && type.getName() != null) {
			value = NamingConvention.UNDERSCORE.apply(type.getName());
		}
		display.getRootCell().hideSelfProperty().set(true);
		display.getRootCell().expandedProperty().set(true);
		display.getSelectionModel().select(display.getRootCell());
		// if we "unselect" which is only possible by deleting the current one, we wanna fall back to the root
		display.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeCell<Element<?>>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<Element<?>>> arg0, TreeCell<Element<?>> arg1, TreeCell<Element<?>> arg2) {
				if (arg2 == null) {
					display.getSelectionModel().select(display.getRootCell());
				}
			}
		});
		if (insert != null) {
			CheckBox insertBox = new CheckBox();
			insertBox.setSelected(insert.get());
			HBox insertBoxBox = EAIDeveloperUtils.newHBox("Import data", insertBox);
			insertBoxBox.setPadding(new Insets(2));
			insertBox.setSelected(insert.get());
			insert.bind(insertBox.selectedProperty());
			root.getChildren().add(insertBoxBox);
		}
		return root;
	}
	
	public static interface ResultHandler {
		public void handle(Map<Structure, Entry> result);
		public boolean hasData(Structure structure);
	}

	@Override
	public void generate(Map<Structure, List<ComplexContent>> content) {
		try {
			this.promptCreate(model, new ArrayList<Structure>(content.keySet()), false, new ResultHandler() {
				@Override
				public void handle(Map<Structure, Entry> result) {
					List<DefinedService> dependendPools = getDependendPools(model);
					// if we have exactly one pool dependant on this, we know where to sync, otherwise (in the future) we can give an option to choose, for now we just skip it
					if (dependendPools.size() == 1) {
						for (Structure structure : content.keySet()) {
							Entry entry = result.get(structure);
							if (entry != null) {
								List<ComplexContent> list = content.get(structure);
								if (list != null && !list.isEmpty()) {
									// TODO
									// call the synchronize with force
									// once done, try to call the insert with the structures, not sure if this will work
									MainController.getInstance().submitTask("Import data", "Importing data for: " + EAICollectionUtils.getPrettyName(entry), new Runnable() {
										@Override
										public void run() {
											try {
												// synchronous reload of the structure entry so it is definitely known by the server at this point
												MainController.getInstance().getServer().getRemote().reload(entry.getId());
												DefinedService service = (DefinedService) MainController.getInstance().getRepository().resolve("nabu.protocols.jdbc.pool.Services.synchronize");
												if (service != null) {
													ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
													input.set("jdbcPoolId", dependendPools.get(0).getId());
													input.set("force", true);
													input.set("execute", true);
													input.set("typeIds", Arrays.asList(entry.getId()));
													
													boolean canProceed = true;
													Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
													ServiceResult serviceResult = run.get();
													if (serviceResult.getException() != null) {
														MainController.getInstance().notify(serviceResult.getException());
													}
													// ok, it was synchronized, let's move to insertion!
													// we first mask all the content as the new type (in case the ids don't match or whatever)
													else {
														DefinedStructure structure = (DefinedStructure) entry.getNode().getArtifact();
														List<ComplexContent> masked = new ArrayList<ComplexContent>();
														Date date = new Date();
														for (ComplexContent instance : list) {
															MaskedContent maskedInstance = new MaskedContent(instance, structure);
															for (Element<?> child : TypeUtils.getAllChildren(structure)) {
																Value<Integer> minOccurs = child.getProperty(MinOccursProperty.getInstance());
																// for mandatory values we do our best...
																if ((minOccurs == null || minOccurs.getValue() > 0) && maskedInstance.get(child.getName()) == null) {
																	Value<Boolean> property = child.getProperty(PrimaryKeyProperty.getInstance());
																	Class<?> clazz = ((SimpleType<?>) child.getType()).getInstanceClass();
																	if (property != null && property.getValue() != null && property.getValue()) {
																		if (UUID.class.isAssignableFrom(clazz)) {
																			maskedInstance.set(child.getName(), UUID.randomUUID());
																		}
																		else {
																			MainController.getInstance().logDeveloperText("Can not set primary key " + child.getName() + " because type is not supported");
																			canProceed = false;
																		}
																	}
																	else if (Date.class.isAssignableFrom(clazz)) {
																		maskedInstance.set(child.getName(), date);
																	}
																	else if (Boolean.class.isAssignableFrom(clazz)) {
																		maskedInstance.set(child.getName(), false);
																	}
																	else {
																		MainController.getInstance().logDeveloperText("Can not generate default value for mandatory " + child.getName() + " because type is not supported");
																		canProceed = false;
																	}
																}
															}
															masked.add(maskedInstance);
														}
														if (canProceed) {
															service = (DefinedService) MainController.getInstance().getRepository().resolve("nabu.services.jdbc.Services.insert");
															if (service != null) {
																input = service.getServiceInterface().getInputDefinition().newInstance();
																input.set("connection", dependendPools.get(0).getId());
																input.set("instances", masked);
																run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
																serviceResult = run.get();
																Platform.runLater(new Runnable() {
																	@Override
																	public void run() {
																		// too annoying
//																		Confirm.confirm(ConfirmType.INFORMATION, "Data imported", "The data has been successfully imported for: " + EAICollectionUtils.getPrettyName(entry), null);
																		MainController.getInstance().getNotificationHandler().notify("The data has been successfully imported for: " + EAICollectionUtils.getPrettyName(entry), 3000l, Severity.INFO);
																	}
																});
															}
														}
														else {
															Platform.runLater(new Runnable() {
																@Override
																public void run() {
																	Confirm.confirm(ConfirmType.WARNING, "Data not imported", "Can not insert partial data for: " + EAICollectionUtils.getPrettyName(entry), null);
																}
															});
														}
													}
												}
											}
											catch (Exception e) {
												MainController.getInstance().notify(e);
											}
										}
									});
								}
							}
						}
					}
				}
				@Override
				public boolean hasData(Structure structure) {
					return content.get(structure) != null && !content.get(structure).isEmpty();
				}
			});
		}
		catch (Exception e) {
			MainController.getInstance().notify(e);
		}
	}
	
	private void browse(String id, String poolId) {
		DefinedService service = (DefinedService) MainController.getInstance().getRepository().resolve("nabu.services.jdbc.Services.select");
		if (service != null) {
			Tab tab = MainController.getInstance().newTab("Browsing " + EAICollectionUtils.getPrettyName(id));
			HBox box = new HBox();
			box.setAlignment(Pos.CENTER);
			box.getChildren().add(new ProgressIndicator());
			tab.setContent(box);
			
			MainController.getInstance().submitTask("Retrieving data", "Retrieving data for: " + EAICollectionUtils.getPrettyName(id), new Runnable() {
				@Override
				public void run() {
					ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
					input.set("connection", poolId);
					input.set("typeId", id);
					input.set("limit", 1000);
					Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
					try {
						ServiceResult serviceResult = run.get();
						if (serviceResult.getException() != null) {
							MainController.getInstance().notify(serviceResult.getException());
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									box.getChildren().clear();
									Label failed = new Label("Could not retrieve data");
									failed.getStyleClass().add("p");
									box.getChildren().add(failed);
								}
							});
						}
						else {
							ComplexContent output = serviceResult.getOutput();
							if (output == null || output.get("select") == null || output.get("select/results") == null) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										box.getChildren().clear();
										Label failed = new Label("No data available");
										failed.getStyleClass().add("p");
										box.getChildren().add(failed);
									}
								});
							}
							else {
								Platform.runLater(new Runnable() {
									@SuppressWarnings("unchecked")
									@Override
									public void run() {
										Structure structure = new Structure();
										structure.setName("resultList");
										ComplexType targetType = (ComplexType) DefinedTypeResolverFactory.getInstance().getResolver().resolve(id);
										structure.add(new ComplexElementImpl("results", targetType, null, 
											new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
										
										StructureInstance newInstance = structure.newInstance();
										List list = (List) output.get("select/results");
										if (list != null) {
											for (int i = 0; i < list.size(); i++) {
												Object content = list.get(i);
												if (!(content instanceof ComplexContent)) {
													content = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(content);
												}
												newInstance.set("results[" + i + "]", new MaskedContent((ComplexContent) list.get(i), targetType));
											}
										}
										
										VBox target = new VBox();
										MainController.getInstance().showContent(target, newInstance, null);
										tab.setContent(target);
									}
								});
							}
						}
					}
					catch (Exception e) {
						MainController.getInstance().notify(e);
					}
				}
			});
			
		}
	}
}
