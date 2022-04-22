package be.nabu.eai.module.data.model;

import java.util.List;
import java.util.Set;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeRegistry;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.SynchronizableTypeRegistry;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.types.properties.IdProperty;

public class DataModelArtifact extends JAXBArtifact<DataModelConfiguration> implements DefinedTypeRegistry, SynchronizableTypeRegistry {

	private TypeRegistryImpl registry;
	
	public DataModelArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "data-model.xml", DataModelConfiguration.class);
	}
	
	@Override
	public Type getTypeById(String id) {
		List<DataModelEntry> entries = getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				DefinedType type = entry.getType();
				if (type != null) {
					String value = ValueUtils.getValue(IdProperty.getInstance(), type.getProperties());
					if (value == null) {
						value = type.getId();
					}
					if (id.equals(value)) {
						return type;
					}
				}
			}
		}
		return DefinedTypeRegistry.super.getTypeById(id);
	}
	
	private TypeRegistry getRegistry() {
		if (registry == null) {
			synchronized(this) {
				if (registry == null) {
					TypeRegistryImpl registry = new TypeRegistryImpl();
					List<DataModelEntry> entries = getConfig().getEntries();
					if (entries != null) {
						for (DataModelEntry entry : entries) {
							DefinedType type = entry.getType();
							if (type != null) {
								if (type instanceof ComplexType) {
									registry.register((ComplexType) type);
								}
								else {
									registry.register((SimpleType<?>) type);
								}
							}
						}
					}
					this.registry = registry;
				}
			}
		}
		return registry;
	}

	@Override
	public SimpleType<?> getSimpleType(String namespace, String name) {
		return getRegistry().getSimpleType(namespace, name);
	}

	@Override
	public ComplexType getComplexType(String namespace, String name) {
		return getRegistry().getComplexType(namespace, name);
	}

	@Override
	public Element<?> getElement(String namespace, String name) {
		return getRegistry().getElement(namespace, name);
	}

	@Override
	public Set<String> getNamespaces() {
		return getRegistry().getNamespaces();
	}

	@Override
	public List<SimpleType<?>> getSimpleTypes(String namespace) {
		return getRegistry().getSimpleTypes(namespace);
	}

	@Override
	public List<ComplexType> getComplexTypes(String namespace) {
		return getRegistry().getComplexTypes(namespace);
	}

	@Override
	public List<Element<?>> getElements(String namespace) {
		return getRegistry().getElements(namespace);
	}

	@Override
	public void setConfig(DataModelConfiguration config) {
		super.setConfig(config);
	}

	@Override
	public boolean isSynchronizable(Type type) {
		List<DataModelEntry> entries = getConfig().getEntries();
		if (entries != null) {
			for (DataModelEntry entry : entries) {
				if (type.equals(entry.getType())) {
					return entry.isSynchronize();
				}
			}
		}
		return false;
	}

}
