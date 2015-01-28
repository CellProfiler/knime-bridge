/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.imagej;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.cellprofiler.knimebridge.IKnimeBridge;
import org.cellprofiler.knimebridge.ProtocolException;
import org.cellprofiler.knimebridge.PipelineException;
import org.scijava.module.Module;
import org.scijava.plugin.RichPlugin;
import org.scijava.service.Service;

/**
 * @author Lee Kamentsky
 *
 * A service that connects to CellProfiler across the Knime bridge
 */
public interface CellProfilerService extends Service, RichPlugin {
	/**
	 * Connect to a bridge that is already running.
	 * 
	 * @param uri
	 * @return the Knime bridge interface
	 */
	public IKnimeBridge connectToBridge(URI uri) throws ProtocolException;
	/**
	 * Create an ImageJ module that's configured to run
	 * a CellProfiler pipeline. The input parameters of the
	 * module will be the images needed to run one cycle of
	 * the pipeline and the output parameters will be the
	 * tables that are the result of running the pipeline.
	 * 
	 * @param pipeline the file containing the pipeline
	 * @return a module that, when configured, can run the pipeline.
	 * @throws IOException if file can't be opened or read
	 * @throws ProtocolException if there was a communication protocol
	 *         mismatch between CellProfiler and the bridge client
	 * @throws PipelineException if the pipeline was not valid.
	 */
	public Module createPipelineModule(IKnimeBridge bridge, File pipeline) throws 
		IOException, PipelineException, ProtocolException;
	
	/**
	 * Create an ImageJ module that's configured to run
	 * a CellProfiler pipeline. The input parameters of the
	 * module will be the images needed to run one cycle of
	 * the pipeline and the output parameters will be the
	 * tables that are the result of running the pipeline.
	 * 
	 * @param pipeline the text of a pipeline file
	 * @return a module that, when configured, can run the pipeline.
	 * @throws ProtocolException if there was a communication protocol
	 *         mismatch between CellProfiler and the bridge client
	 * @throws PipelineException if the pipeline was not valid.
	 */
	public Module createPipelineModule(IKnimeBridge bridge, String pipeline) throws
		PipelineException, ProtocolException;
}
