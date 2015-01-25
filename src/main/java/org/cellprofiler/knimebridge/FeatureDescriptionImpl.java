/**
 * 
 */
package org.cellprofiler.knimebridge;

/**
 * @author Lee Kamentsky
 *
 */
public class FeatureDescriptionImpl<T> implements IFeatureDescription<T> {
	final String objectName;
	final String name;
	final Class<T> type;
	
	public FeatureDescriptionImpl(String objectName, String name, Class<T> type) {
		this.objectName = objectName;
		this.name = name;
		this.type = type;
	}
	@Override
	public String getObjectName() {
		return objectName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<T> getType() {
		return type;
	}
}
