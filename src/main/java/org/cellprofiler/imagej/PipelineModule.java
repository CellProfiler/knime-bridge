package org.cellprofiler.imagej;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.table.Column;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.GenericTable;
import net.imagej.table.ResultsTable;
import net.imagej.table.Table;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.IFeatureDescription;
import org.cellprofiler.knimebridge.IKnimeBridge;
import org.cellprofiler.knimebridge.KBConstants;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.module.DefaultMutableModule;
import org.scijava.module.DefaultMutableModuleInfo;
import org.scijava.module.DefaultMutableModuleItem;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.UIService;
import org.zeromq.ZMQException;

/**
 * @author Lee Kamentsky
 *
 * A pipeline module.
 */
public class PipelineModule extends DefaultMutableModule {
	@Parameter
	private LogService logService;
	@Parameter
	private UIService uiService;
	
	final static private String READABLE_NAME = "CellProfiler pipeline";
	final static private String CHANNEL_SUFFIX = "Channel";
	final static private String TABLE_SUFFIX = "Table";
	final static private String DESCRIPTION = 
			"A CellProfiler pipeline plugin runs a single cycle of a CellProfiler pipeline. " +
			"The user supplies the images needed to run the pipeline and the pipeline " +
			"returns all of the measurements that the pipeline producles on output.";
	final private IKnimeBridge bridge;
	final private String pipeline;
	final private Map<String, ModuleItem<Dataset>> channelInputs = 
			new Hashtable<String, ModuleItem<Dataset>>();
	final private Map<String, ModuleItem<ResultsTable>> tables;
	final private TreeSet<FeatureStuffer> features;
	
	protected PipelineModule(IKnimeBridge bridge, String pipeline) {
		this.bridge = bridge;
		this.pipeline = pipeline;
		this.tables = new Hashtable<String, ModuleItem<ResultsTable>>();
		this.features = new TreeSet<FeatureStuffer>();
	}
	protected void init() throws ZMQException, PipelineException, ProtocolException {
		bridge.loadPipeline(pipeline);
		ModuleInfo info = getInfo();
		info.setLabel(READABLE_NAME);
		info.setDescription(DESCRIPTION);
		info.setName(READABLE_NAME);
		ModuleItem<String> pipelineOutput = addOutput("pipeline", String.class);
		pipelineOutput.setValue(this, pipeline);
		for (String channel:bridge.getInputChannels()) {
			final ModuleItem<Dataset> input = addInput(
					channelNameToInputName(channel), Dataset.class);
			input.setDescription(String.format("Input image for channel %s", channel));
			channelInputs.put(channel, input);
		}
		List<String> objectNames = new ArrayList<String>(bridge.getObjectNames());
		objectNames.add(KBConstants.IMAGE);
		for (String objectName:objectNames) {
			for (IFeatureDescription feature:bridge.getFeatures(objectName)) {
				String name = feature.getName();
				if (feature.getType().equals(String.class)) continue;
				if (!tables.containsKey(objectName)) {
					ModuleItem<ResultsTable> output = 
							addOutput(tableNameToInputName(objectName), ResultsTable.class);
					tables.put(objectName, output);
				}
				if (feature.getType().equals(Double.class)) {
					features.add(new DoubleFeatureStuffer(feature));
				} else if (feature.getType().equals(Float.class)) {
					features.add(new FloatFeatureStuffer(feature));
				}  else if (feature.getType().equals(Integer.class)) {
					features.add(new IntFeatureStuffer(feature));
				}
			}
		}
		
	}
	
	@Override
	public void run() {
		Map<String, ImgPlus<?>> imageMap = new Hashtable<String, ImgPlus<?>>();
		for (Entry<String, ModuleItem<Dataset>> entry:channelInputs.entrySet()) {
			Dataset dataset = entry.getValue().getValue(this);
			imageMap.put(entry.getKey(), dataset.getImgPlus());
		}
		try {
			bridge.run(imageMap);
		} catch (ZMQException e) {
			error("CellProfiler pipeline failed to run because of a communication error", e);
		} catch (CellProfilerException e) {
			error("CellProfiler encountered an internal error while trying to run the pipeline", e);
		} catch (PipelineException e) {
			error("CellProfiler could not run because of a problem in the pipeline or its configuration", e);
		} catch (ProtocolException e) {
			error("There is a mismatch between the communication format used by this plugin and that supported by your version of CellProfiler.", e);
		}
		for (FeatureStuffer stuffit:features) {
			stuffit.add();
		}
	}
	private static String channelNameToInputName(String channel) {
		return channel + "_"+ CHANNEL_SUFFIX;
	}
	
	private static String tableNameToInputName(String object_name) {
		return object_name + "_"+ TABLE_SUFFIX;
	}
	
	/**
	 * Create a new pipeline module, given a bridge and
	 * the text of a pipeline
	 * 
	 * @param bridge
	 * @param pipeline
	 * @return
	 * @throws ProtocolException 
	 * @throws PipelineException 
	 * @throws ZMQException 
	 */
	static public PipelineModule newInstance(IKnimeBridge bridge, String pipeline) 
			throws PipelineException, ProtocolException {
		PipelineModule module = new PipelineModule(bridge, pipeline);
		module.init();
		return module;
	}
	private void error(String message, Throwable e) {
		logService.error(message, e);
		if (uiService != null) {
			uiService.showDialog(message, "Failed to run CellProfiler pipeline", MessageType.ERROR_MESSAGE);
		}
	}
	abstract protected class FeatureStuffer implements Comparable<FeatureStuffer> {
		final protected IFeatureDescription feature;
		FeatureStuffer(IFeatureDescription feature) {
			this.feature = feature;
		}
		@Override
		public int compareTo(FeatureStuffer o) {
			int result = feature.getObjectName().compareTo(o.feature.getObjectName());
			if (result == 0) {
				String name = feature.getName();
				String otherName = o.feature.getName();
				if (name.equals(otherName)) return 0;
				if (name.equals("Number_Object_Number")) return -1;
				if (otherName.equals("Number_Object_Number")) return 1;
				result = feature.getName().compareTo(o.feature.getName());
			}
			return result;
		}
		/**
		 * @param length 
		 * @return
		 */
		protected DoubleColumn addColumn(int length) {
			final ModuleItem<ResultsTable> moduleItem = tables.get(feature.getObjectName());
			ResultsTable table = moduleItem.getValue(PipelineModule.this);
			if (table == null) {
				table = new DefaultResultsTable(0, Math.max(1, length));
				moduleItem.setValue(PipelineModule.this, table);
			}
			DoubleColumn column = table.appendColumn(feature.getName());
			column.setSize(table.getRowCount());
			return column;
		}
		abstract void add();
	}
	protected class IntFeatureStuffer extends FeatureStuffer {
		IntFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		void add() {
			int [] results = bridge.getIntMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			for (int i=0; (i<results.length) && (i<column.size()); i++)
				column.setValue(i, results[i]);
		}
	}
	protected class FloatFeatureStuffer extends FeatureStuffer {
		FloatFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		void add() {
			float [] results = bridge.getFloatMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			for (int i=0; i<results.length; i++)
				column.addValue(results[i]);
		}
	}
	protected class DoubleFeatureStuffer extends FeatureStuffer {
		DoubleFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		void add() {
			double [] results = bridge.getDoubleMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			column.setArray(results);
		}
	}
}
