package be.nabu.eai.module.data.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.ElementMarshallable;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils.Endpoint;
import be.nabu.eai.developer.util.EAIDeveloperUtils.EndpointPicker;
import be.nabu.eai.developer.util.ElementClipboardHandler;
import be.nabu.eai.developer.util.ElementSelectionListener;
import be.nabu.eai.developer.util.ElementTreeItem;
import be.nabu.eai.developer.util.ElementTreeItem.ChildSelector;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.jdbc.JDBCUtils;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.RootElement;
import be.nabu.libs.types.properties.ForeignKeyProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class DataModelGUIManager extends BaseJAXBGUIManager<DataModelConfiguration, DataModelArtifact> {

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

	@Override
	public void display(MainController controller, AnchorPane pane, DataModelArtifact model) {
		pane.getChildren().clear();
		
		VBox total = new VBox();
		AnchorPane.setBottomAnchor(total, 0d);
		AnchorPane.setLeftAnchor(total, 0d);
		AnchorPane.setRightAnchor(total, 0d);
		AnchorPane.setTopAnchor(total, 0d);
		
		total.prefWidthProperty().bind(pane.widthProperty());
		total.prefHeightProperty().bind(pane.heightProperty());
		pane.getChildren().add(total);
		
		ScrollPane scroll = new ScrollPane();
		scroll.setFitToWidth(true);
		
		VBox.setVgrow(scroll, Priority.ALWAYS);
		
		BooleanProperty locked = controller.hasLock(model.getId());
		ObjectProperty<HBox> focused = new SimpleObjectProperty<HBox>();
		Map<String, VBox> drawn = new HashMap<String, VBox>();
		Map<String, List<Node>> shapes = new HashMap<String, List<Node>>();
		
		AnchorPane canvas = new AnchorPane();
		scroll.setContent(canvas);
		canvas.minHeightProperty().bind(scroll.heightProperty().subtract(25));
		
		canvas.addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard dragboard = event.getDragboard();
				if (dragboard != null) {
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
		});
		canvas.addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard dragboard = event.getDragboard();
				if (dragboard != null && !event.isDropCompleted() && !event.isConsumed()) {
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
								toFront(entry.getType(), draw, shapes);
								
								// and expand it?
//								Tree<?> tree = (Tree<?>) draw.lookup(".treeContainer");
//								tree.getRootCell().expandAll(1);
							}
						}
					}
				}
			}
		});
		HBox buttons = new HBox();
		buttons.setAlignment(Pos.CENTER_LEFT);
		buttons.setPadding(new Insets(10));
		Button delete = new Button("Delete Selected");
		Button export = new Button("Copy to clipboard (PNG)");
		
		ComboBox<DataModelType> combo = new ComboBox<DataModelType>();
		combo.getItems().addAll(DataModelType.values());
		combo.getSelectionModel().select(model.getConfig().getType());
		combo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DataModelType>() {
			@Override
			public void changed(ObservableValue<? extends DataModelType> arg0, DataModelType arg1, DataModelType arg2) {
				model.getConfig().setType(arg2);
				MainController.getInstance().setChanged();
				
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
		buttons.getChildren().addAll(export, delete, separator, viewLabel, combo);
		
		total.getChildren().addAll(buttons, scroll);
		
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (focused.get() != null) {
					delete(model, canvas, focused, drawn, shapes);
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
		canvas.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent arg0) {
				if (focused.get() != null && arg0.getCode() == KeyCode.DELETE) {
					delete(model, canvas, focused, drawn, shapes);
				}
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
			}
			
			drawShapes(model, drawn, shapes, canvas, entries);
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
		
		Line line = getLine(drawn, entry.getId(), toId, fromCell, toCell);
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

	private void toFront(DefinedType entry, VBox child, Map<String, List<Node>> shapes) {
		for (List<Node> nodes : shapes.values()) {
			for (Node node : nodes) {
				node.toFront();
			}
		}
		child.toFront();
		// any shapes related to this child should be front and center
		List<Node> list = shapes.get(entry.getId());
		if (list != null) {
			for (Node node : list) {
				node.toFront();
			}
		}
	}
	
	private VBox draw(DataModelArtifact model, DataModelEntry entry, BooleanProperty locked, ObjectProperty<HBox> focused, Map<String, VBox> drawn, Map<String, List<Node>> shapes) {
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
			
			// the name is both for clarification and as a select/drag target!
			Label nameLabel = new Label(entry.getType().getId());
			nameLabel.getStyleClass().add("invokeServiceName");
			nameLabel.setPadding(new Insets(5));
			name.getChildren().add(nameLabel);
			
			child.getStyleClass().add("service");
			EventHandler<MouseEvent> clickHandler = new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent arg0) {
					if (focused.get() != null) {
						focused.get().getStyleClass().remove("selectedInvoke");
					}
					toFront(entry.getType(), child, shapes);
					name.getStyleClass().add("selectedInvoke");
					focused.set(name);
				}
			};
			child.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
			
			child.getChildren().add(name);
			
			StructureGUIManager structureGUIManager = new StructureGUIManager();
			structureGUIManager.setActualId(model.getId());
			// not doing inline editing atm as the menus are too big
			// entry.getType() instanceof Structure
			
			Tree<Element<?>> display = new Tree<Element<?>>(new ElementMarshallable(), null, StructureGUIManager.newCellDescriptor());
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
			display.getTreeCell(display.rootProperty().get()).expandedProperty().set(false);
			display.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
			
			child.getChildren().add(display);
			
//					Tree<Element<?>> display = structureGUIManager.display(controller, child, new RootElementWithPush((ComplexType) entry.getType(), true), entry.getType() instanceof Structure, false);
			display.setClipboardHandler(new ElementClipboardHandler(display, false));
			display.getRootCell().getNode().getStyleClass().add("invokeTree");
			
			ElementSelectionListener elementSelectionListener = new ElementSelectionListener(MainController.getInstance(), false);
			elementSelectionListener.setActualId(entry.getType().getId());
			display.getSelectionModel().selectedItemProperty().addListener(elementSelectionListener);
			
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
			
			MovablePane makeMovable = MovablePane.makeMovable(child, locked);
			makeMovable.xProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					entry.setX(newValue.intValue());
					MainController.getInstance().setChanged();
				}
			});
			makeMovable.yProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					entry.setY(newValue.intValue());
					MainController.getInstance().setChanged();
				}
			});
			
			// hide buttons and stuff
			Node lookup = child.lookup(".structure-all-buttons");
			if (lookup != null) {
				lookup.setVisible(false);
				lookup.setManaged(false);
			}
			lookup = child.lookup(".structure-move-buttons");
			if (lookup != null) {
				lookup.setVisible(false);
				lookup.setManaged(false);
			}
			return child;
		} 
		catch (Exception e) {
			MainController.getInstance().notify(e);
		}
		return null;
	}
	
	private void delete(DataModelArtifact model, AnchorPane canvas, ObjectProperty<HBox> focused, Map<String, VBox> drawn, Map<String, List<Node>> shapes) {
		DataModelEntry entry = (DataModelEntry) focused.get().getUserData();
		model.getConfig().getEntries().remove(entry);
		MainController.getInstance().setChanged();
		focused.set(null);
		VBox remove = drawn.remove(entry.getType().getId());
		canvas.getChildren().remove(remove);
		
		List<Node> removed = shapes.remove(entry.getType().getId());
		if (removed != null) {
			canvas.getChildren().removeAll(removed);
		}
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

}
