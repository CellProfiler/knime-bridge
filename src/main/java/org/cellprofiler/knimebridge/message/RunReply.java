/*
 * Copyright (c) 2015, Broad Institute
 * All rights reserved.
 *
 * Published under a BSD license, see LICENSE for details
 */
package org.cellprofiler.knimebridge.message;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.KBConstants;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.joni.exception.ValueException;
import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

/**
 * @author Lee Kamentsky
 *
 * The run-reply is sent following a successful run. The format is a block
 * of metadata of the form:
 *     array of double features composed of a two tuple of object name and...
 *          array of two tuple of feature name and count of instances
 *     array of float features composed of a two tuple of object name and...
 *          array of two tuple of feature name and count of instances
 *     array of integer features composed of a two tuple of object name and...
 *          array of two tuple of feature name and count of instances
 *     array of string features composed of a two tuple of object name and...
 *          array of two tuple of feature name and the length in bytes of each
 *          UTF-8 encoded string.
 *          
 * This is followed by a single frame containing all the data in lowendian form
 * in the same order as above.
 *          
*/
public class RunReply extends AbstractReply {
	private final static String msgName = "run-reply-1";
	private final Map<String, Map<String, double []>> doubleFeatures =
			new Hashtable<String, Map<String,double[]>>();
	private final Map<String, Map<String, float []>> floatFeatures =
			new Hashtable<String, Map<String, float[]>>();
	private final Map<String, Map<String, int[]>> intFeatures = 
			new Hashtable<String, Map<String,int[]>>();
	private final Map<String, Map<String, String>> stringFeatures =
			new Hashtable<String, Map<String,String>>();
	@Override
	protected String getMsgName() {
		return msgName;
	}
	@Override
	protected void parse(ZMsg msg) throws CellProfilerException, PipelineException, ProtocolException {
		String featureMetadata = msg.popString();
		JsonReader rdr = Json.createReader(new StringReader(featureMetadata));
		ZFrame frame = msg.pop();
		if (frame == null) throw new ProtocolException("Missing data value frame");
		byte [] data = frame.getData();
		int offset = 0;
		JsonArray wrapper = rdr.readArray();
		if (wrapper == null)
			throw new ProtocolException("Missing or invalid metadata wrapper");
		else if (wrapper.size() != 4) 
			throw new ProtocolException(String.format("Wrong # of metadata sections: expected 4, got %d", wrapper.size()));
		offset = parseFeatures(wrapper.getJsonArray(0), data, offset, new DoubleHacker(), 
				new IdentityAdapter<double []>(), doubleFeatures);
		offset = parseFeatures(wrapper.getJsonArray(1), data, offset, new FloatHacker(), 
				new IdentityAdapter<float []>(), floatFeatures);
		offset = parseFeatures(wrapper.getJsonArray(2), data, offset, new IntHacker(), 
				new IdentityAdapter<int []>(), intFeatures);
		offset = parseFeatures(wrapper.getJsonArray(3), data, offset, new ByteHacker(), new ByteToStringAdapter(), stringFeatures);
		
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * An array hacker converts byte data into an
	 * array of ints, floats or doubles.
	 * 
	 * @param <T> the type of the output array, e.g. double []
	 */
	private interface ArrayHacker<T> {
		void check(byte [] data, int offset, int length) throws ProtocolException;
		T allocate(int length);
		int hack(byte [] data, int offset, T container);
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * An adapter that converts an array into another type,
	 * for instance a String.
	 * 
	 * @param <T>
	 * @param <U>
	 */
	private interface ArrayAdapter<T, U> {
		/**
		 * Convert an array of type T to an object of type U
		 * @param array array to be converted
		 * @return an object, built out of the array data.
		 */
		U convert(T array);
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A pass-through adapter that does nothing.
	 * 
	 * @param <T> the input and output type
	 */
	private static class IdentityAdapter<T> implements ArrayAdapter<T, T> {
		public T convert(T array) { return array; }
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A hacker that converts bytes to doubles.
	 */
	private static class DoubleHacker implements ArrayHacker<double []> {

		@Override
		public int hack(byte[] data, int offset, double[] doubleData) {
			for (int i=0; i<doubleData.length; i++) {
				long bits = 0;
				for (long j = 0; j<Double.SIZE/Byte.SIZE; j++) {
					bits += (((long)(data[offset++])) & 0xFFL) << (j*8L);
				}
				doubleData[i] = Double.longBitsToDouble(bits);
			}
			return offset;
		}

		@Override
		public double[] allocate(int length) {
			return new double[length];
		}

		@Override
		public void check(byte[] data, int offset, int length)
				throws ProtocolException {
			if (offset + Double.SIZE / Byte.SIZE * length > data.length)
				throw new ProtocolException("Buffer overrun when deserializing doubles");
		}
		
	}
	
	/**
	 * @author Lee Kamentsky
	 *
	 * A hacker that converts bytes to floats
	 */
	private static class FloatHacker implements ArrayHacker<float []> {

		@Override
		public int hack(byte[] data, int offset, float[] doubleData) {
			for (int i=0; i<doubleData.length; i++) {
				int bits = 0;
				for (int j = 0; j<Float.SIZE/Byte.SIZE; j++) {
					bits += (((int)(data[offset++])) & 0xFF) << (j*8);
				}
				doubleData[i] = Float.intBitsToFloat(bits);
			}
			return offset;
		}

		@Override
		public float[] allocate(int length) {
			return new float[length];
		}

		@Override
		public void check(byte[] data, int offset, int length)
				throws ProtocolException {
			if (offset + Float.SIZE / Byte.SIZE * length > data.length)
				throw new ProtocolException("Buffer overrun when deserializing floats");
		}
		
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A hacker that converts lowendian bytes to integers
	 */
	private static class IntHacker implements ArrayHacker<int []> {
		@Override
		public int hack(byte[] data, int offset, int[] doubleData) {
			for (int i=0; i<doubleData.length; i++) {
				int bits = 0;
				for (int j = 0; j<Integer.SIZE / Byte.SIZE; j++) {
					bits += (((int)(data[offset++])) & 0xFF) << (j*8);
				}
				doubleData[i] = bits;
			}
			return offset;
		}

		@Override
		public int[] allocate(int length) {
			return new int[length];
		}

		@Override
		public void check(byte[] data, int offset, int length)
				throws ProtocolException {
			if (offset + Integer.SIZE / Byte.SIZE * length > data.length)
				throw new ProtocolException("Buffer overrun when deserializing integers");
		}
		
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A pass-through hacker that eats bytes.
	 */
	private static class ByteHacker implements ArrayHacker<byte []> {
		@Override
		public int hack(byte[] data, int offset, byte [] buf) {
			for (int i=0; i< buf.length; i++) {
				buf[i] = data[i+offset];
			}
			return offset+buf.length;
		}

		@Override
		public byte[] allocate(int length) {
			return new byte[length];
		}

		@Override
		public void check(byte[] data, int offset, int length)
				throws ProtocolException {
			if (offset + length > data.length)
				throw new ProtocolException("Buffer overrun when deserializing integers");
		}
		
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * An adapter that converts an array of bytes to a string
	 * using UTF-8 encoding.
	 */
	private static class ByteToStringAdapter implements ArrayAdapter<byte[], String> {
		static final Charset charset = Charset.forName("UTF-8");
		public String convert(byte[] buffer) {
			return new String(buffer, charset);
		}
	}
	/**
	 * Parse the features from Json describing their
	 * layout in the byte array
	 * 
	 * @param metadata a Json array of two tuples of object name
	 *                 and an array of two tuples of feature name
	 *                 and # of elements to eat from the data array
	 * @param data the data array containing the measurement data
	 * @param offset the offset to the first measurement to parse out of the data
	 * @param hacker a hacker that will produce an array of type T
	 * @param adapter an adapter that will produce an object of type U
	 *                given a data array of type T
	 * @param dest a map of object name to a map of feature name and
	 *        measurement data. This map will be populated with the
	 *        parsed measurement data.
	 *       
	 * @return the offset to the first byte after the parsed data
	 *         in the data array
	 *         
	 * @throws ProtocolException if the Json was not correctly parsed
	 *                           or if there was a buffer overrun.
	 */
	private <T, U> int parseFeatures(JsonArray metadata, byte[] data, int offset, ArrayHacker<T> hacker,
			ArrayAdapter<T, U> adapter,
			Map<String, Map<String, U>> dest)
			throws ProtocolException {
		for (JsonValue dme:metadata) {
			if (! ((dme instanceof JsonArray) && (((JsonArray)dme).size() == 2))) {
				throw new ProtocolException("Double metadata element was not an array of length 2");
			}
			JsonArray dmea = (JsonArray)dme;
			String key = dmea.getString(0);
			JsonArray features = dmea.getJsonArray(1);
			final Hashtable<String, U> mapFeatureToValues = new Hashtable<String, U>();
			dest.put(key, mapFeatureToValues);
			for (JsonValue feature:features) {
				if (! ((feature instanceof JsonArray) && (((JsonArray)feature).size() == 2))) {
					throw new ProtocolException("Double metadata feature was not an array of length 2");
				}
				JsonArray jfeature = (JsonArray)feature;
				String featureName = jfeature.getString(0);
				int length = jfeature.getInt(1);
				hacker.check(data, offset, length);
				final T arrayData = hacker.allocate(length);
				offset = hacker.hack(data, offset, arrayData);
				mapFeatureToValues.put(featureName, adapter.convert(arrayData ));
			}
			
		}
		return offset;
	}
	/**
	 * Get a string measurement from the parsed data
	 * 
	 * @param objectName the name of the segmentation
	 *                   or "Image" or null to get a
	 *                   image-wide string measurement
	 * @param name the name of the feature
	 * @return the string value of the measurement
	 */
	public String getStringMeasurement(String objectName, String name) {
		if (objectName == null) objectName = KBConstants.IMAGE;
		if (! stringFeatures.containsKey(objectName)) return null;
		return stringFeatures.get(objectName).get(name);
	}
	/**
	 * Get integer measurements from the parsed data
	 * 
	 * @param objectName the name of the segmentation (or "Image" or null)
	 * @param name the name of the feature
	 * @return an array of integer values for each segmented object
	 */
	public int[] getIntMeasurements(String objectName, String name) {
		if (objectName == null) objectName = KBConstants.IMAGE;
		if (! intFeatures.containsKey(objectName)) return null;
		return intFeatures.get(objectName).get(name);
	}
	/**
	 * Get float measurements from the parsed data
	 * 
	 * @param objectName the name of the segmentation (or "Image" or null)
	 * @param name the name of the feature
	 * @return an array of float values for each segmented object
	 */
	public float[] getFloatMeasurements(String objectName, String name) {
		if (objectName == null) objectName = KBConstants.IMAGE;
		if (! floatFeatures.containsKey(objectName)) return null;
		return floatFeatures.get(objectName).get(name);
	}
	/**
	 * Get double measurements from the parsed data
	 * 
	 * @param objectName the name of the segmentation (or "Image" or null)
	 * @param name the name of the feature
	 * @return an array of float values for each segmented object
	 */
	public double[] getDoubleMeasurements(String objectName, String name) {
		if (objectName == null) objectName = KBConstants.IMAGE;
		if (! floatFeatures.containsKey(objectName)) return null;
		return doubleFeatures.get(objectName).get(name);
	}
	/**
	 * Return the number of objects in an image data set
	 * 
	 * For groups, this is the total # in all image sets processed
	 * 
	 * @param resultTableName
	 * @return # of rows of measurement data to expect
	 */
	public int getNumberOfObjects(String resultTableName) {
		if (resultTableName.equals(KBConstants.IMAGE)) 
			return intFeatures.get(resultTableName).get(KBConstants.IMAGE_NUMBER).length;
		return intFeatures.get(resultTableName).get(KBConstants.OBJECT_NUMBER).length;
	}

}
