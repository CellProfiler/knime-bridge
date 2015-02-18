/**
 * 
 */
package org.cellprofiler.knimebridge.message;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Socket;

/**
 * @author Lee Kamentsky
 *
 */
public class CleanPipelineReply extends AbstractReply {
	private static final String msgName = "clean-pipeline-reply-1";
	private String pipeline;
	
	protected CleanPipelineReply() {
	}

	/**
	 * Receive a CleanPipelineReply via the given socket
	 * 
	 * @param socket a socket to the CP server which has
	 *        previously been sent a CleanPipelineReq
	 * @return a reply with the pipeline wrapped by the message
	 * @throws ProtocolException if the clean-pipeline-request was not supported by the server
	 * @throws PipelineException if the pipeline was in the wrong format.
	 */
	public static CleanPipelineReply recvReply(Socket socket) 
			throws ProtocolException, PipelineException {
		CleanPipelineReply reply = new CleanPipelineReply();
		reply.recvNoCPException(socket);
		return reply;
	}
	
	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.message.AbstractReply#getMsgName()
	 */
	@Override
	protected String getMsgName() {
		return msgName;
	}

	/* (non-Javadoc)
	 * @see org.cellprofiler.knimebridge.message.AbstractReply#parse(org.zeromq.ZMsg)
	 */
	@Override
	protected void parse(ZMsg msg) throws ProtocolException, PipelineException,
			CellProfilerException {
		pipeline = msg.popString();
	}
	
	/**
	 * @return the pipeline sent in the body of the reply.
	 */
	public String getPipeline() {
		return pipeline;
	}

}
