package be.nabu.eai.module.data.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.ElementMarshallable;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.ElementClipboardHandler;
import be.nabu.eai.developer.util.ElementSelectionListener;
import be.nabu.eai.developer.util.ElementTreeItem;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.RootElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
		
		AnchorPane canvas = new AnchorPane();
		scroll.setContent(canvas);
		
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
								// redraw to include new one...
								display(controller, pane, model);
							}
						}
					}
				}
			}
		});
		HBox buttons = new HBox();
		buttons.setPadding(new Insets(10));
		Button delete = new Button("Delete");
		buttons.getChildren().add(delete);
		
		total.getChildren().addAll(buttons, scroll);
		
		ObjectProperty<HBox> focused = new SimpleObjectProperty<HBox>();
		List<DataModelEntry> entries = model.getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				if (!(entry.getType() instanceof ComplexType)) {
					continue;
				}
				try {
					VBox child = new VBox();
					// save the entry for easy removal etc
					child.setUserData(entry);
					// borrowing from blox...
					child.getStyleClass().add("invokeWrapper");
					HBox name = new HBox();
					name.getStyleClass().add("invokeName");
					
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
							child.toFront();
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
					display.rootProperty().set(new ElementTreeItem(new RootElement((ComplexType) entry.getType()), null, false, false));
					display.getTreeCell(display.rootProperty().get()).expandedProperty().set(false);
					display.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
					
					child.getChildren().add(display);
					
//					Tree<Element<?>> display = structureGUIManager.display(controller, child, new RootElementWithPush((ComplexType) entry.getType(), true), entry.getType() instanceof Structure, false);
					display.setClipboardHandler(new ElementClipboardHandler(display, false));
					display.getRootCell().getNode().getStyleClass().add("invokeTree");
					
					ElementSelectionListener elementSelectionListener = new ElementSelectionListener(controller, false);
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
					
//					display.getTreeCell(display.rootProperty().get()).expandAll(1);
//					child.setManaged(false);
					canvas.getChildren().add(child);
//					child.relocate(entry.getX(), entry.getY());
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
				} 
				catch (Exception e) {
					controller.notify(e);
				}
			}
		}
	}

}
