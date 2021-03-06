/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.imagej.ImgPlus;

import org.cellprofiler.knimebridge.message.CleanPipelineReq;
import org.cellprofiler.knimebridge.message.ConnectReq;
import org.cellprofiler.knimebridge.message.PipelineInfoReply;
import org.cellprofiler.knimebridge.message.PipelineInfoReq;
import org.cellprofiler.knimebridge.message.RunGroupReq;
import org.cellprofiler.knimebridge.message.RunReply;
import org.cellprofiler.knimebridge.message.RunReq;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

/**
 * @author Lee Kamentsky
 * 
 * The top-level of the ZMQ protocol-driven
 * Knime bridge.
 *
 */
@SuppressWarnings("deprecation")
class KnimeBridgeImpl implements IKnimeBridge {
	private final static Context context = ZMQ.context(1);
	
	private final Socket socket = context.socket(ZMQ.REQ);
	private String sessionID;
	private String pipeline;
	private PipelineInfoReply piReply;
	private RunReply runReply;
	
	/**
	 * @return the ZMQ context that should be used
	 *         throughout this process.
	 */
	public static Context theContext() { return context; }

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#connect(java.net.URI)
	 */
	@Override
	public void connect(URI uri) throws ZMQException, ProtocolException {
		socket.connect(uri.toString());
		sessionID = ConnectReq.connect(socket);
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#disconnect()
	 */
	@Override
	public void disconnect() {
		socket.close();

	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#loadPipeline(java.lang.String)
	 */
	@Override
	public void loadPipeline(String pipeline) throws PipelineException,
			ZMQException, ProtocolException {
		this.pipeline = pipeline;
		piReply = PipelineInfoReq.send(socket, sessionID, pipeline);
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#getInputChannels()
	 */
	@Override
	public List<String> getInputChannels() throws ZMQException {
		return piReply.getChannels();
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#getObjectNames()
	 */
	@Override
	public List<String> getResultTableNames() throws ZMQException {
		final List<String> names = new ArrayList<String>(piReply.getObjects());
		names.add(KBConstants.IMAGE);
		return names;
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#getFeatures(java.lang.String)
	 */
	@Override
	public List<IFeatureDescription> getFeatures(String objectName)
			throws ZMQException {
		if (objectName == null) return piReply.getImageFeatureDescriptions();
		return piReply.getFeatureDescriptions(objectName);
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#run(java.util.Map)
	 */
	@Override
	public void run(Map<String, ImgPlus<?>> images) throws ZMQException,
			CellProfilerException, PipelineException, ProtocolException {
		runReply = RunReq.run(socket, sessionID, pipeline, images);

	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.IKnimeBridge#getMeasurements(org.cellprofiler.knimebridge.IFeatureDescription)
	 */
	@Override
	public String getStringMeasurement(IFeatureDescription feature) {
		return runReply.getStringMeasurement(feature.getObjectName(), feature.getName());
	}

	@Override
	public int[] getIntMeasurements(IFeatureDescription feature) {
		return runReply.getIntMeasurements(feature.getObjectName(), feature.getName());
	}

	@Override
	public double[] getDoubleMeasurements(IFeatureDescription feature) {
		return runReply.getDoubleMeasurements(feature.getObjectName(), feature.getName());
	}

	@Override
	public float[] getFloatMeasurements(IFeatureDescription feature) {
		return runReply.getFloatMeasurements(feature.getObjectName(), feature.getName());
	}

	@Override
	public void loadPipeline(File pipeline) throws PipelineException,
			IOException, ProtocolException {
		final char [] buffer = new char[4096];
		StringBuffer sb = new StringBuffer();
		Reader rdr = new FileReader(pipeline);
		try {
			while (true) {
				final int nRead = rdr.read(buffer, 0, buffer.length);
				if (nRead < 0) break;
				sb.append(buffer);
			}
			loadPipeline(sb.toString());
		} finally {
			rdr.close();
		}
	}

	@Override
	public void runGroup(Map<String, ImgPlus<?>> images) throws ZMQException,
			CellProfilerException, PipelineException, ProtocolException {
		runReply = RunGroupReq.run(socket, sessionID, pipeline, images);
	}

	@Override
	public int getNumberOfRows(String resultTableName) {
		return runReply.getNumberOfObjects(resultTableName);
	}

	@Override
	public String cleanPipeline(String pipeline, int flags)
			throws PipelineException, IOException, ProtocolException {
		final List<String> moduleNames = new ArrayList<String>();
		if ((flags & KBConstants.REMOVE_EXPORT_TO_DATABASE) != 0)
			moduleNames.add(KBConstants.EXPORT_TO_DATABASE);
		if ((flags & KBConstants.REMOVE_EXPORT_TO_SPREADSHEET) != 0)
			moduleNames.add(KBConstants.EXPORT_TO_SPREADSHEET);
		if ((flags & KBConstants.REMOVE_SAVE_IMAGES) != 0)
			moduleNames.add(KBConstants.SAVE_IMAGES);
		return cleanPipeline(pipeline, moduleNames);
	}

	@Override
	public String cleanPipeline(String pipeline) throws PipelineException,
			IOException, ProtocolException {
		return cleanPipeline(pipeline, KBConstants.REMOVE_ALL);
	}

	@Override
	public String cleanPipeline(String pipeline, Collection<String> moduleNames)
			throws PipelineException, IOException, ProtocolException {
		return CleanPipelineReq.send(socket, sessionID, pipeline, moduleNames).getPipeline();
	}

	@Override
	public void cleanPipeline() throws PipelineException, IOException,
			ProtocolException {
		this.pipeline = cleanPipeline(this.pipeline);
		
	}

	@Override
	public void cleanPipeline(int flags) throws PipelineException, IOException,
			ProtocolException {
		this.pipeline = cleanPipeline(this.pipeline, flags);
	}

	@Override
	public void cleanPipeline(Collection<String> moduleNames)
			throws PipelineException, IOException, ProtocolException {
		this.pipeline = cleanPipeline(this.pipeline, moduleNames);
	}

}
