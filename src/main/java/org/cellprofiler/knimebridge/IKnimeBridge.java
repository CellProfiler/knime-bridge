/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.zeromq.ZMQException;

import net.imagej.ImgPlus;

/**
 * The KnimeBridge represents a persistent connection
 * to a CellProfiler worker to manage a pipeline.
 * 
 * @author Lee Kamentsky
 *
 */
@SuppressWarnings("deprecation")
public interface IKnimeBridge {
	/**
	 * Establish a CellProfiler/Knime bridge context
	 * 
	 * @param uri URI of the TCP address to connect to
	 * @throws ProtocolException 
	 */
	public void connect(URI uri) throws ZMQException, ProtocolException;
	/**
	 * 
	 */
	public void disconnect();
	/**
	 * Load a pipeline into this bridge context.
	 * 
	 * @param pipeline A CellProfiler pipeline
	 * @throws ProtocolException if the server couldn't communicate properly using the protocol
	 */
	public void loadPipeline(String pipeline) throws PipelineException, ZMQException, ProtocolException;
	
	/**
	 * Load a pipeline from a file
	 * 
	 * @param pipeline - file containing the pipeline
	 * 
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws IOException if the file could not be opened
	 * @throws ProtocolException on messing protocol error
	 */
	public void loadPipeline(File pipeline) throws PipelineException, IOException, ProtocolException;
	
	/**
	 * Clean the pipeline loaded by loadPipeline of all output modules.
	 * @see IKnimeBridge#cleanPipeline(String) for details.
	 * 
	 * @throws PipelineException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void cleanPipeline() throws PipelineException, IOException, ProtocolException;
	
	/**
	 * Clean selected modules from the pipeline loaded by loadPipeline.
	 * 
	 * @param flags @see IKnimeBridge#cleanPipeline(String, int) for details
	 * @throws PipelineException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void cleanPipeline(int flags) throws PipelineException, IOException, ProtocolException;
	
	/**
	 * Clean modules with given names from the pipeline loaded by loadPipeline.
	 * 
	 * @param moduleNames
	 * @throws PipelineException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	public void cleanPipeline(Collection<String> moduleNames) 
			throws PipelineException, IOException, ProtocolException;
	/**
	 * Remove modules by name from a pipeline
	 * 
	 * @param pipeline the text from a CellProfiler pipeline
	 * @param moduleNames the names of the modules to be removed as they appear
	 *                    in CellProfiler.
	 * @return the modified pipeline text
	 * @throws PipelineException if the input pipeline was badly formatted
	 * @throws IOException if there was a communication failure
	 * @throws ProtocolException if the server did not support the function
	 */
	public String cleanPipeline(String pipeline, Collection<String> moduleNames)
			throws PipelineException, IOException, ProtocolException;
	/**
	 * Remove selected categories of modules from the pipeline
	 * that are inappropriate for execution by the bridge.
	 * 
	 * There may be situations where users want to keep some
	 * categories of modules and discard others, for instance
	 * if their pipeline creates an illumination function via
	 * SaveImages, but they want the ExportToSpreadsheet module
	 * bypassed because the measurements will be collected by Knime.
	 * 
	 * @param pipeline the text of a CellProfiler pipeline file
	 * @param flags a combination of the flags, {@value KBConstants#REMOVE_EXPORT_TO_DATABASE},
	 *        {@value KBConstants#REMOVE_EXPORT_TO_SPREADSHEET} and / or
	 *        {@value KBConstants#REMOVE_SAVE_IMAGES}. Only the flagged classes of
	 *        modules will be removed.
	 *        
	 * @return a modified pipeline
	 * @throws PipelineException if the input pipeline was badly formatted
	 * @throws IOException if there was a communication failure
	 * @throws ProtocolException if the server did not support the function
	 */
	public String cleanPipeline(String pipeline, int flags) 
			throws PipelineException, IOException, ProtocolException;
	/**
	 * Remove all modules from the pipeline that are inappropriate
	 * for execution via the bridge because they output
	 * to disk. (ExportToSpreadsheet, ExportToDatabase, SaveImages)
	 * 
	 * @param pipeline the text of a CellProfiler pipeline
	 * @return a rewritten pipeline without the export/save modules
	 * @throws PipelineException if the input pipeline was badly formatted
	 * @throws IOException if there was a communication failure
	 * @throws ProtocolException if the server did not support the function
	 */
	public String cleanPipeline(String pipeline) throws PipelineException, IOException, ProtocolException;
	/**
	 * @return the names of the image input channels
	 */
	public List<String> getInputChannels() throws ZMQException;
	/**
	 * @return the names of the result tables
	 */
	public List<String> getResultTableNames() throws ZMQException;
	
	/**
	 * Get the features that you can expect to be returned in the table
	 * 
	 * @param resultTableName the name of the result table that has the features
	 * @return one feature description per column
	 */
	public List<IFeatureDescription> getFeatures(String resultTableName) throws ZMQException;
	
	/**
	 * Run one cycle of the pipeline
	 * 
	 * @param images the images that will be the inputs, in the same order as for getInputChannels
	 * @throws ProtocolException 
	 * @throws PipelineException 
	 */
	public void run(Map<String, ImgPlus<?>> images) throws ZMQException, CellProfilerException, PipelineException, ProtocolException;
	
	/**
	 * Run a group of images
	 * 
	 * Some CellProfiler pipelines have grouping, for instance
	 * illumination correction or tracking. You can run
	 * grouped pipelines by creating imgPlus images with
	 * multiple Z or T planes.
	 * 
	 * @param images a map of channel name to image. Each image should
	 *               have the same number of Z or T planes
	 * @throws ZMQException on network error
	 * @throws CellProfilerException if CellProfiler encountered an error
	 *                               while running the pipeline.
	 * @throws PipelineException if there was a configuration problem
	 *                           with the pipeline.
	 * @throws ProtocolException
	 */
	public void runGroup(Map<String, ImgPlus<?>> images) throws ZMQException, CellProfilerException, PipelineException, ProtocolException;
	
	/**
	 * Get the # of rows to expect for each feature for this result table
	 * 
	 * @param resultTableName
	 * @return
	 */
	public int getNumberOfRows(String resultTableName);
	/**
	 * If the feature description is of type Integer, return results as an array of ints
	 * 
	 * @param feature
	 * @return
	 */
	public int [] getIntMeasurements(IFeatureDescription feature);
	/**
	 * If the feature description is of type Double, return result as array of doubles
	 * @param feature
	 * @return
	 */
	public double [] getDoubleMeasurements(IFeatureDescription feature);
	
	/**
	 * If the feature description is of type Float, return result
	 * as an array of floats.
	 * 
	 * @param feature
	 * @return
	 */
	public float [] getFloatMeasurements(IFeatureDescription feature);
	
	/**
	 * If the feature description is of type String, there
	 * will be just a single string returned, for instance
	 * some image-wide metadata.
	 * 
	 * @param feature
	 * @return
	 */
	public String getStringMeasurement(IFeatureDescription feature);
}
