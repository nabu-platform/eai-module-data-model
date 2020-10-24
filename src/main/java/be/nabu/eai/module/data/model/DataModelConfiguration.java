package be.nabu.eai.module.data.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "dataModel")
public class DataModelConfiguration {
	private List<DataModelEntry> entries;

	public List<DataModelEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<DataModelEntry> entries) {
		this.entries = entries;
	}
}
