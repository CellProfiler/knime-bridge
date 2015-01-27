package org.cellprofiler.knimebridge.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ImgUtil;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * @author Lee Kamentsky
 *
 * A request to run a pipeline using an image set.
 * 
 * The format is:
 *    frame containing the pipeline
 *    frame containing a JsonArray where each slot of the array
 *          is a 2-tuple of channel name and a JsonArray of three-tuples
 *          of axis name, dimension, and stride 
 *    one frame per image containing the image data of each channel in the
 *          order they appear above. The data are doubles in little-endian
 *          format, organized by the strides.  
 */
public class RunReq extends ZMsg {
	private static final String msgName = "run-request-1";
	
	@SuppressWarnings("unchecked")
	protected RunReq(String sessionID, String pipeline, Map<String, ImgPlus<?>> imageMap) {
		add(msgName);
		add(pipeline);
		JsonArrayBuilder builder = Json.createArrayBuilder();
		ArrayList<double []> dataChunks = new ArrayList<double []>();
		for (Map.Entry<String, ImgPlus<?>> entry:imageMap.entrySet()) {
			ImgPlus<?> imgPlus = entry.getValue();
			assert imgPlus.firstElement() instanceof RealType;
			RealType<?> firstElement = (RealType)(imgPlus.firstElement());
			String key = entry.getKey();
			double [] chunk = serializeImgPlus(key, (ImgPlus)imgPlus, builder );
			double scaling = 1;
			if (firstElement instanceof IntegerType) {
				int validBits = imgPlus.getValidBits();
				if (validBits != 0) {
					scaling = Math.pow(2.0, -validBits);
				}
			}
			for (int i=0; i<chunk.length; i++) {
				chunk[i] *= scaling;
			}
			dataChunks.add(chunk);
		}
		StringWriter sw = new StringWriter();
		Json.createWriter(sw).writeArray(builder.build());
		add(sw.toString());
		for (double [] chunk:dataChunks) {
			 byte[] v = new byte[chunk.length * Double.SIZE / Byte.SIZE];
			 for (int i=0; i<chunk.length; i++) {
				 long bits = Double.doubleToLongBits(chunk[i]);
				 v[i*8] = (byte)(bits);
				 v[i*8+1] = (byte)(bits >> 8);
				 v[i*8+2] = (byte)(bits >> 16);
				 v[i*8+3] = (byte)(bits >> 24);
				 v[i*8+4] = (byte)(bits >> 32);
				 v[i*8+5] = (byte)(bits >> 40);
				 v[i*8+6] = (byte)(bits >> 48);
				 v[i*8+7] = (byte)(bits >> 56);
			 }
			 add(v);
		}
		wrap(new ZFrame(sessionID));
	}
	
	/**
	 * Serialize the imgPlus metadata to Json, returning the raw intensities
	 * @param imgPlus
	 * @return array of doubles representing the intensities
	 */
	static protected  <T extends RealType<T>> double [] serializeImgPlus(
			String channel, ImgPlus<T> imgPlus, JsonArrayBuilder builder) {
		final int numDimensions = imgPlus.numDimensions();
		final int[] dimensions = new int [numDimensions];
		for (int i=0; i<numDimensions; i++) {
			dimensions[i] = (int)imgPlus.dimension(i);
		}
		final int[] strides = new int[dimensions.length];
		strides[numDimensions-1] = 1;
		for (int i=1; i<numDimensions; i++) {
			strides[numDimensions-i-1] = strides[numDimensions-i] * dimensions[numDimensions-i]; 
		}
		JsonArrayBuilder aBuilder = Json.createArrayBuilder();
		for (int i=0; i<numDimensions; i++) {
			aBuilder.add(Json.createArrayBuilder()
				.add(imgPlus.axis(i).type().toString())
				.add(dimensions[i])
				.add(strides[i]));
		}
		double [] data = new double[(int)(strides[0] * dimensions[0])];
		builder.add(Json.createArrayBuilder().add(channel).add(aBuilder).build());
		ImgUtil.copy(imgPlus.getImg(), data, 0, strides);
		return data;
	}
	static public RunReply run(
			Socket socket, String sessionID, 
			String pipeline, Map<String, ImgPlus<?>> imageMap) throws CellProfilerException, PipelineException, ProtocolException {
		RunReq req = new RunReq(sessionID, pipeline, imageMap);
		req.send(socket);
		RunReply reply = new RunReply();
		reply.recv(socket);
		return reply;
	}

}
