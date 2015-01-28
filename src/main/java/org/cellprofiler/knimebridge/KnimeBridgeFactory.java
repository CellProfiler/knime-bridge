/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

/**
 * @author Lee Kamentsky
 *
 * Creates instances of the Knime bridge.
 */
public class KnimeBridgeFactory {
	/**
	 * @return a new Knime bridge.
	 */
	public IKnimeBridge newKnimeBridge() {
		return new KnimeBridgeImpl();
	}

}
