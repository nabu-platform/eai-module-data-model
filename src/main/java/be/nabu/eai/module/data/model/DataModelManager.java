/*
* Copyright (C) 2020 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
