/**
 * 
 */
package org.cellprofiler.imagej;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;

import org.cellprofiler.knimebridge.IKnimeBridge;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.scijava.command.Command;
import org.scijava.menu.MenuConstants;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Menu;
import org.scijava.ui.UIService;

/**
 * @author Lee Kamentsky
 *
 * Run a pipeline by reading it from disk
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT),
	@Menu(label = "CellProfiler"), @Menu(label = "Run Pipeline...") })
public class RunPipeline implements Command {
	@Parameter
	CellProfilerService service;
	@Parameter
	UIService uiService;
	@Parameter
	ModuleService moduleService;
	@Parameter(label="CellProfiler pipeline" )
	File pipeline;
	@Parameter(label="CellProfiler URL", 
			   description=
			   "On Windows and Linux, you should start a CellProfiler worker like this:\n"+
				"    analysis_worker --knime-bridge-address=\"tcp://127.0.0.1:XXXX\"\n\n" +
				"where XXXX is a port number like \"8086\". You should enter the same\n"+
				"URL here, for instance, \"tcp://127.0.0.1:8086\".\n"+
				"On the Mac, you should open a terminal and execute the command,\n"+
				"    /Applications/CellProfiler.app/Contents/MacOS/CellProfiler --knime-bridge-address=...")
	String url;
	@Override
	public void run() {
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			uiService.showDialog(String.format("Your URL, \"%s\" is not in the correct format", url));
			return;
		}
		IKnimeBridge bridge;
		try {
			bridge = service.connectToBridge(uri);
		} catch (ProtocolException e) {
			uiService.showDialog("Failed to connect to CellProfiler. See log for details.");
			return;
		}
		Module pm;
		try {
			pm = service.createPipelineModule(bridge, pipeline);
		} catch (IOException e) {
			uiService.showDialog(e.getMessage(), "Failed to open pipeline");
			return;
		} catch (PipelineException e) {
			uiService.showDialog(e.getMessage(), "Pipeline error");
			return;
		} catch (ProtocolException e) {
			uiService.showDialog(e.getMessage(), "Communication error");
			return;
		}
		moduleService.run(pm, true, new Hashtable<String, Object>());
	}

}
