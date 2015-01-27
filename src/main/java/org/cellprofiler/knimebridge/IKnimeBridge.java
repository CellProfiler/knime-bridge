package org.cellprofiler.knimebridge;

import java.net.URI;
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
	 * @return the names of the image input channels
	 */
	public List<String> getInputChannels() throws ZMQException;
	/**
	 * @return the names of the segmentations
	 */
	public List<String> getObjectNames() throws ZMQException;
	
	/**
	 * Get the features that you can expect to be returned in the table
	 * 
	 * @param objectName the name of the segmentation or null for the image features
	 * @return one feature description per feature
	 */
	public List<IFeatureDescription> getFeatures(String objectName) throws ZMQException;
	
	/**
	 * Run one cycle of the pipeline
	 * 
	 * @param images the images that will be the inputs, in the same order as for getInputChannels
	 * @throws ProtocolException 
	 * @throws PipelineException 
	 */
	public void run(Map<String, ImgPlus<?>> images) throws ZMQException, CellProfilerException, PipelineException, ProtocolException;
	
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
	
	public float [] getFloatMeasurements(IFeatureDescription feature);
	
	String getStringMeasurement(IFeatureDescription feature);
}
