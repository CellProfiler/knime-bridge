/**
 * 
 */
package org.cellprofiler.knimebridge.message;

import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Socket;

/**
 * @author Lee Kamentsky
 *
 */
public class CleanPipelineReq extends ZMsg{
	private static final String msgName = "clean-pipeline-request-1";
	
	/**
	 * Create a CleanPipelineReq wrapping the given pipeline text
	 * and the names of modules to remove.
	 * 
	 * @param sessionID ID of session being used for communication
	 * @param pipeline pipeline to be cleaned
	 * @param moduleNames names of modules to be removed
	 */
	protected CleanPipelineReq(String sessionID, String pipeline, Collection<String> moduleNames) {
		add(msgName);
		add(pipeline);
		final JsonArrayBuilder jsonModuleNames = Json.createArrayBuilder();
		for (String moduleName:moduleNames) jsonModuleNames.add(moduleName);
		add(jsonModuleNames.build().toString());
		wrap(new ZFrame(sessionID));
	}
	/**
	 * @param socket communicate with the server over this socket
	 * @param sessionID use this session for communication
	 * @param pipeline the pipeline text to be modified
	 * @param moduleNames the names of the modules to be removed
	 * @return the modified pipeline
	 * @throws PipelineException if the pipeline's format was wrong
	 * @throws ProtocolException if the server did not support the protocol of the request
	 */
	public static CleanPipelineReply send(
			Socket socket, String sessionID, String pipeline, Collection<String> moduleNames)
					throws PipelineException, ProtocolException {
		new CleanPipelineReq(sessionID, pipeline, moduleNames).send(socket);
		return CleanPipelineReply.recvReply(socket);
		
	}

}
