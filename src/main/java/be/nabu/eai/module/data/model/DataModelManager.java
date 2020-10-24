package be.nabu.eai.module.data.model;

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

}
