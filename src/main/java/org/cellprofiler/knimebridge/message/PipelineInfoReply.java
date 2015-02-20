/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.cellprofiler.knimebridge.FeatureDescriptionImpl;
import org.cellprofiler.knimebridge.IFeatureDescription;
import org.cellprofiler.knimebridge.KBConstants;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * A reply to a pipeline info request
 * 
 * @author Lee Kamentsky
 *
 * The PipelineInfoReply reports the required inputs,
 * and the output measurements for the pipeline passed
 * through PipelineInfoReq.
 * 
 * Format of a PipelineInfoReply:
 * 
 *     msgName
 *     body: Json string
 *        Array of channel names
 *        Array of type names
 *        Dictionary of segmentation name to array of 2-tuples
 *           2-tuple is feature name, index of type name
 * 
 */
public class PipelineInfoReply extends AbstractReply {
	private static final String msgName = "pipeline-info-reply-1";
	private List<String> channels;
	private List<String> objects;
	private Map<String, List<IFeatureDescription>> objectFeatures;
	
	protected PipelineInfoReply() {
		
	}
	/**
	 * Install the information retrieved from the server
	 * 
	 * @param channels the names of the image inputs to the pipeline
	 * @param objectFeatures a map of object name to feature produced
	 */
	protected void setInfo(List<String> channels, Map<String, List<IFeatureDescription>> objectFeatures) { 
		this.channels = Collections.unmodifiableList(channels);
		this.objectFeatures = Collections.unmodifiableMap(objectFeatures);
		final Set<String> objects = new HashSet<String>(objectFeatures.keySet());
		objects.remove(KBConstants.IMAGE);
		this.objects = Collections.unmodifiableList(new ArrayList<String>(objects));
	}
	
	/**
	 * @return the names of the image channels that need to be supplied to run()
	 */
	public List<String> getChannels() {
		return this.channels;
	}
	
	/**
	 * @return the names of the segmentations
	 */
	public List<String> getObjects() {
		return objects;
	}
	
	/**
	 * @param object the name of the segmentation
	 * @return the per-object features
	 */
	public List<IFeatureDescription> getFeatureDescriptions(String object) {
		if (! objectFeatures.containsKey(object))
			return Collections.emptyList();
		return Collections.unmodifiableList(this.objectFeatures.get(object));
	}
	
	/**
	 * @return the per-image features
	 */
	public List<IFeatureDescription> getImageFeatureDescriptions() {
		return getFeatureDescriptions(KBConstants.IMAGE);
	}
	
	/**
	 * Receive a reply to a PipelineInfoReq
	 * 
	 * @param socket read the reply message on this socket
	 * @return a PipelineInfoReply containing the inputs and outputs of the pipeline
	 * 
	 * @throws ProtocolException if the client could not understand the server
	 * @throws PipelineException if the pipeline could not be loaded
	 */
	public static PipelineInfoReply recvReply(Socket socket) throws ProtocolException, PipelineException {
		PipelineInfoReply reply = new PipelineInfoReply();
		reply.recvNoCPException(socket);
		return reply;
	}
	@Override
	protected void parse(ZMsg reply) throws ProtocolException {
		final String body = popString(reply);
		if (body == null) {
			throw new ProtocolException("Pipeline info reply is missing its body");
		}
		final JsonReader rdr = Json.createReader(new StringReader(body));
		final JsonArray wrapper = rdr.readArray();
		final JsonArray aChannels = wrapper.getJsonArray(0);
		if (aChannels == null) {
			throw new ProtocolException("Pipeline info is missing channel list");
		}
		final List<String> channels = new ArrayList<String>(aChannels.size());
		for (JsonValue v:aChannels) {
			if (!(v instanceof JsonString))
				throw new ProtocolException(String.format("Expected channel to be a String, was a %s", v.getValueType()));
			channels.add(((JsonString)v).getString());
		}
		final JsonArray aTypes = wrapper.getJsonArray(1);
		if (aTypes == null) {
			throw new ProtocolException("Pipeline info is missing list of types");
		}
		final List<Class<?>> types = new ArrayList<Class<?>>(aTypes.size());
		for (JsonValue v:aTypes) {
			if (!(v instanceof JsonString))
				throw new ProtocolException(String.format("Expected channel to be a String, was a %s", v.getValueType()));
			try {
				types.add(Class.forName(((JsonString)v).getString()));
			} catch (ClassNotFoundException e) {
				throw new ProtocolException(e.getMessage());
			}
		}
		Map<String, List<IFeatureDescription>> objectFeatures = 
				new Hashtable<String, List<IFeatureDescription>>();
		final JsonObject sObjectFeatures = wrapper.getJsonObject(2);
		if (sObjectFeatures == null) {
			throw new ProtocolException("Pipeline info is missing feature list");
		}
		for (String key:sObjectFeatures.keySet()) {
			final JsonArray ofTuples = sObjectFeatures.getJsonArray(key);
			if (ofTuples == null) {
				throw new ProtocolException(String.format("Segmentation %s is missing its features", key));
			}
			List<IFeatureDescription> features = new ArrayList<IFeatureDescription>(ofTuples.size());
			objectFeatures.put(key, features);
			for (JsonValue v:ofTuples) {
				if (! (v instanceof JsonArray)) {
					throw new ProtocolException(String.format("Expected Json array, got %s", v.getValueType().toString()));
				}
				final JsonArray ofTuple = (JsonArray)v;
				if (ofTuple.size() < 2) {
					throw new ProtocolException("Expected 2-tuple for feature description");
				}
				final String name = ofTuple.getString(0);
				final int typeIdx = ofTuple.getInt(1);
				if (typeIdx >= types.size()) {
					throw new ProtocolException(String.format("Got out of bounds type index: %d", typeIdx));
				}
				Class<?> type = types.get(typeIdx);
				addFeature(key, features, name, type);
			}
		}
		setInfo(channels, objectFeatures);
	}

	private static void addFeature(String key,
			List<IFeatureDescription> features, final String name,
			Class<?> type) {
		features.add(new FeatureDescriptionImpl(key, name, type));
	}

	@Override
	protected String getMsgName() {
		return msgName;
	}
}
