package be.nabu.eai.module.data.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class DataModelManager extends JAXBArtifactManager<DataModelConfiguration, DataModelArtifact> {

	public DataModelManager() {
		super(DataModelArtifact.class);
	}

	@Override
	protected DataModelArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new DataModelArtifact(id, container, repository);
	}

	@Override
	public List<String> getReferences(DataModelArtifact artifact) throws IOException {
		List<String> references = new ArrayList<String>();
		List<String> parentReferences = super.getReferences(artifact);
		if (parentReferences != null) {
			references.addAll(parentReferences);
		}
		List<DataModelEntry> entries = artifact.getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				if (entry.getType() != null) {
					String id = entry.getType().getId();
					if (id != null && !references.contains(id)) {
						references.add(id);
					}
				}
			}
		}
		return references;
	}

	
}
