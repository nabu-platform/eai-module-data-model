package be.nabu.eai.module.data.model;

public enum DataModelType {
	// show as it would be modelled in the database
	DATABASE,
	// show with all fields (inherited or not)
	EXTENDED,
	// show only with local fields
	LOCAL
}