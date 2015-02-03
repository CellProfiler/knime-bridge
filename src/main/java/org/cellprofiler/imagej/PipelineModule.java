package org.cellprofiler.imagej;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.ResultsTable;

import org.cellprofiler.knimebridge.CellProfilerException;
import org.cellprofiler.knimebridge.IFeatureDescription;
import org.cellprofiler.knimebridge.IKnimeBridge;
import org.cellprofiler.knimebridge.KBConstants;
import org.cellprofiler.knimebridge.PipelineException;
import org.cellprofiler.knimebridge.ProtocolException;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.log.LogService;
import org.scijava.module.DefaultMutableModule;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.UIService;import org.zeromq.ZMQException;

/**
 * @author Lee Kamentsky
 *
 * A pipeline module. The module communicates with a running
 * CellProfiler instance via the Knime bridge. You can get
 * a pipeline module programatically via the CellProfilerService
 * or you can run a pipeline on one image set using the
 * RunPipeline plugin.
 * 
 * The input parameters to the pipeline are 2d or color images.
 * The output parameters are the pipeline that was used
 * and tables for image-wide measurements and per-object
 * measurements.
 */
@SuppressWarnings("deprecation")
public class PipelineModule extends DefaultMutableModule implements Contextual {
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
			"returns all of the measurements that the pipeline produces on output.";
	final private IKnimeBridge bridge;
	final private String pipeline;
	final private Map<String, ModuleItem<Dataset>> channelInputs = 
			new Hashtable<String, ModuleItem<Dataset>>();
	final private Map<String, ModuleItem<ResultsTable>> tables;
	final private TreeSet<FeatureStuffer> features;
	private Context context;
	private ModuleItem<Boolean> groupImages;
	
	/**
	 * Construct the pipeline from a connected Knime bridge
	 * (bridge.connect() must have been called previously)
	 * and the text of a pipeline.
	 * 
	 * PipelineModule.init() must be called to complete
	 * the initialization process and configure the inputs and outputs.
	 * 
	 * @param bridge Knime bridge connected to CellProfiler
	 * @param pipeline the contents of a pipeline file
	 */
	protected PipelineModule(IKnimeBridge bridge, String pipeline) {
		this.bridge = bridge;
		this.pipeline = pipeline;
		this.tables = new Hashtable<String, ModuleItem<ResultsTable>>();
		this.features = new TreeSet<FeatureStuffer>();
	}
	/**
	 * init uploads the pipeline for the first time to CellProfiler
	 * which parses it and reports back with the channels (= 2d images)
	 * that will be needed for input and the measurements which
	 * CellProfiler will output.
	 * 
	 * @throws ZMQException on low-level communications failure
	 * @throws PipelineException if the pipeline could not be parsed
	 * @throws ProtocolException if the client or server side
	 *             of the bridge could not parse the protocol,
	 *             possibly because of unsupported versions.
	 */
	protected void init() throws ZMQException, PipelineException, ProtocolException {
		bridge.loadPipeline(pipeline);
		/*
		 * Set up the top-level module information
		 */
		ModuleInfo info = getInfo();
		info.setLabel(READABLE_NAME);
		info.setDescription(DESCRIPTION);
		info.setName(READABLE_NAME);
		/*
		 * groupImages determines whether to run
		 * a single pipeline cycle on an image set
		 * or whether to run stacks as a group of
		 * pipeline cycles.
		 */
		groupImages = addInput("groupImages", Boolean.class);
		groupImages.setValue(this, false);
		groupImages.setLabel("Group images");
		groupImages.setDescription(
				"Check this box and supply CellProfiler with image stacks\n"+
				"if you have a pipeline that uses grouping (e.g. a pipeline\n"+
				"with a TrackObjects or MakeProjection module.");
		/*
		 * pipelineOutput lets the user see the pipeline
		 */
		ModuleItem<String> pipelineOutput = addOutput("pipeline", String.class);
		pipelineOutput.setValue(this, pipeline);
		for (String channel:bridge.getInputChannels()) {
			final ModuleItem<Dataset> input = addInput(
					channelNameToInputName(channel), Dataset.class);
			input.setLabel(String.format("%s channel", channel));
			input.setDescription(String.format("Input image for channel %s", channel));
			channelInputs.put(channel, input);
		}
		List<String> objectNames = new ArrayList<String>(bridge.getResultTableNames());
		objectNames.add(KBConstants.IMAGE);
		for (String objectName:objectNames) {
			for (IFeatureDescription feature:bridge.getFeatures(objectName)) {
				if (feature.getType().equals(String.class)) continue;
				if (!tables.containsKey(objectName)) {
					ModuleItem<ResultsTable> output = 
							addOutput(tableNameToInputName(objectName), ResultsTable.class);
					output.setLabel(objectName);
					if (objectName.equals(KBConstants.IMAGE))
						output.setDescription("This table contains all numeric image-wide measurements.");
					else
						output.setDescription(
								"Measurements calculated on the %s objects. See CellProfiler documentation for their descriptions");
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
			if (groupImages.getValue(this).booleanValue())
				bridge.runGroup(imageMap);
			else
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
	/**
	 * Make a unique name for the channel input parameter
	 * 
	 * @param channel
	 * @return
	 */
	private static String channelNameToInputName(String channel) {
		return channel + "_"+ CHANNEL_SUFFIX;
	}
	
	/**
	 * Make a unique name for the table output parameter
	 * 
	 * @param object_name
	 * @return
	 */
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
	static public PipelineModule newInstance(Context ctx, IKnimeBridge bridge, String pipeline) 
			throws PipelineException, ProtocolException {
		PipelineModule module = new PipelineModule(bridge, pipeline);
		module.setContext(ctx);
		module.init();
		return module;
	}
	/**
	 * Display and log an error
	 * 
	 * @param message
	 * @param e
	 */
	private void error(String message, Throwable e) {
		logService.error(message, e);
		if (uiService != null) {
			uiService.showDialog(message, "Failed to run CellProfiler pipeline", MessageType.ERROR_MESSAGE);
		}
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * The FeatureStuffer is a class that can
	 * stuff the measurements for a feature into
	 * a table. It implements the Comparable interface
	 * so that a collection of feature stuffers
	 * can be sorted into alphabetical order.
	 */
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
		 * Add a column for this feature to the appropriate table
		 * 
		 * @param length the # of measurements made on this object
		 *        during the run 
		 * @return the column that was created.
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
	/**
	 * @author Lee Kamentsky
	 *
	 * A feature stuffer for integer values
	 */
	protected class IntFeatureStuffer extends FeatureStuffer {
		IntFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		@Override
		void add() {
			int [] results = bridge.getIntMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			for (int i=0; (i<results.length) && (i<column.size()); i++)
				column.setValue(i, results[i]);
		}
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A feature stuffer for float values
	 */
	protected class FloatFeatureStuffer extends FeatureStuffer {
		FloatFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		@Override
		void add() {
			float [] results = bridge.getFloatMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			for (int i=0; i<results.length; i++)
				column.addValue(results[i]);
		}
	}
	/**
	 * @author Lee Kamentsky
	 *
	 * A feature stuffer for double values
	 */
	protected class DoubleFeatureStuffer extends FeatureStuffer {
		DoubleFeatureStuffer(IFeatureDescription feature) {
			super(feature);
		}
		@Override
		void add() {
			double [] results = bridge.getDoubleMeasurements(feature);
			DoubleColumn column = addColumn(results.length);
			column.setArray(results);
		}
	}
	@Override
	public Context context() {
		return context;
	}
	@Override
	public Context getContext() {
		return context();
	}
	@Override
	public void setContext(Context context) {
		this.context = context;
		context.inject(this);
	}
}
