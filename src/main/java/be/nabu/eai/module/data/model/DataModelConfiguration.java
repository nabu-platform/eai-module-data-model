package be.nabu.eai.module.data.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "dataModel")
public class DataModelConfiguration {
	private List<DataModelEntry> entries;
	private DataModelType type;

	public List<DataModelEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<DataModelEntry> entries) {
		this.entries = entries;
	}

	public DataModelType getType() {
		return type;
	}

	public void setType(DataModelType type) {
		this.type = type;
	}
}
