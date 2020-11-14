package be.nabu.eai.module.data.model;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.types.api.DefinedType;

public class DataModelEntry {
	private DefinedType type;
	private int x, y;
	private boolean synchronize = true;
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedType getType() {
		return type;
	}
	public void setType(DefinedType type) {
		this.type = type;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public boolean isSynchronize() {
		return synchronize;
	}
	public void setSynchronize(boolean synchronize) {
		this.synchronize = synchronize;
	}
}
