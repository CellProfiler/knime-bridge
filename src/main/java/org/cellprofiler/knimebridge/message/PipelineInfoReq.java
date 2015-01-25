/**
 * 
 */
package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 * @author Lee Kamentsky
 *
 */
public class PipelineInfoReq extends ZMsg {
	private static final String msgName = "pipeline-info-req-1";
	
	protected PipelineInfoReq(String sessionID, String pipeline) {
		add(msgName);
		add(pipeline);
		wrap(new ZFrame(sessionID));
	}
	
	public static PipelineInfoReply send(Socket socket, String sessionID, String pipeline) 
			throws PipelineException, ProtocolException {
		new PipelineInfoReq(sessionID, pipeline).send(socket);
		return PipelineInfoReply.recvReply(socket);
	}

}
