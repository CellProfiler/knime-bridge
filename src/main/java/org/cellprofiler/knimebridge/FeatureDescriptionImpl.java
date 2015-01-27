/**
 * 
 */
package org.cellprofiler.knimebridge;

/**
 * @author Lee Kamentsky
 *
 */
public class FeatureDescriptionImpl implements IFeatureDescription {
	final String objectName;
	final String name;
	final Class<?> type;
	
	public FeatureDescriptionImpl(String objectName, String name, Class<?> type) {
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
	public Class<?> getType() {
		return type;
	}
}
