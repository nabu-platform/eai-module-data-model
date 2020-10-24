package be.nabu.eai.module.data.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.managers.util.RootElementWithPush;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.structure.Structure;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Transform;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

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
		ScrollPane scroll = new ScrollPane();
		AnchorPane.setBottomAnchor(scroll, 0d);
		AnchorPane.setLeftAnchor(scroll, 0d);
		AnchorPane.setRightAnchor(scroll, 0d);
		AnchorPane.setTopAnchor(scroll, 0d);
		pane.getChildren().add(scroll);
		scroll.setFitToWidth(true);
		
		BooleanProperty locked = controller.hasLock(model.getId());
		VBox total = new VBox();
		total.prefHeightProperty().bind(scroll.heightProperty().subtract(25));
		
		AnchorPane canvas = new AnchorPane();
		VBox.setVgrow(canvas, Priority.ALWAYS);
		
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
		
		total.getChildren().addAll(buttons, canvas);
		
		scroll.setContent(total);
		List<DataModelEntry> entries = model.getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				if (!(entry.getType() instanceof ComplexType)) {
					continue;
				}
				try {
					AnchorPane child = new AnchorPane();
					StructureGUIManager structureGUIManager = new StructureGUIManager();
					structureGUIManager.setActualId(model.getId());
					// not doing inline editing atm as the menus are too big
					// entry.getType() instanceof Structure
					Tree<Element<?>> display = structureGUIManager.display(controller, child, new RootElementWithPush((ComplexType) entry.getType(), true), entry.getType() instanceof Structure, false);
					display.getTreeCell(display.rootProperty().get()).expandAll(1);
					MovablePane makeMovable = MovablePane.makeMovable(child, locked);
					makeMovable.xProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
							entry.setX(newValue.intValue());
						}
					});
					makeMovable.yProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
							entry.setY(newValue.intValue());
						}
					});
					canvas.getChildren().add(child);
					canvas.layout();
					
					new Timeline(
						new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								Bounds boundsInLocal = display.getBoundsInLocal();
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
								child.resize(boundsInLocal.getWidth(), boundsInLocal.getHeight());
								child.setManaged(false);
								child.relocate(entry.getX(), entry.getY());
							}
						})
					).play();
				} 
				catch (Exception e) {
					controller.notify(e);
				}
			}
		}
	}

}
