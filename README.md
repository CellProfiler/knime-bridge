# knime-bridge
ZMQ connection between CellProfiler and Knime

The Knime bridge makes a TCP connection to a CellProfiler worker
and communicates via a protocol that lets the client (either
Knime or an ImageJ plugin) run a CellProfiler pipeline.

## Starting the Knime-bridge server

The Knime-bridge server protocol is supported on Windows
and Linux builds dated 1/28/2015 and later. The protocol
will be ready on OS/X shortly.

On Windows and Linux, start the server by running the
analysis_worker executable. On Linux, this should be
on your path after installing CellProfiler, on Windows,
it is in the CellProfiler installation directory -
typically, "c:/Program Files/CellProfiler". To start
the Knime bridge on the worker, use the --knime-bridge-address
command-line switch:

    analysis_worker --knime-bridge-address=tcp://127.0.0.1:8086
    
for instance, to start the Knime bridge so that it is
only accessible from the local machine on port 8086.

You should use the same address for the Knime bridge client.

## The Knime-bridge client

You can access the Knime bridge through Java using the
class, `org.cellprofiler.knimebridge.KnimeBridgeFactory`.
`KnimeBridgeFactory().newKnimeBridge()` will give you an
unconnected instance of the bridge. You first connect to the bridge,
supplying the same address as you did to the worker server, then
you can call loadPipeline to load a CellProfiler pipeline into
the bridge. At this point, you can ask the bridge for the image
inputs and measurement outputs and then supply one imgPlus() image
for each of the channels. The bridge will communicate with the
server to run your pipeline on the supplied images, returning
the measurements.

There is an example that runs the Knime bridge in the src/test/java
directory: `org.cellprofiler.knimebridge.Main.java`

## The ImageJ client

If you start ImageJ 2.0 with the Knime bridge jars and their
dependencies on the class path, you should see "CellProfiler"
on the plugins menu and "Run pipeline" in the submenu.
If you run the "Run pipeline" plugin, it will ask you
for the URL of your server (tcp://127.0.0.1:8086 in the
example above) and your pipeline file (hit the Browse button
to find it on disk). Once you finish this dialog, the
bridge will contact the server to get the details of your
pipeline. You'll be asked to choose your images for your pipeline
and after running, you'll get tables for each of the
types of objects you have in the pipeline, plus an image
measurements table.

## Pipelines

At present, there are some limitations to the pipelines you
can run. Grouping is not yet supported. There is no
mechanism to set properties, so CellProfiler will use
the defaults for any that aren't entered via command-line
switches to analysis_worker. SaveImages can only output
to fixed file names and any part of the pipeline that
extracts metadata from file names will not work. In general,
you should not use modules that perform I/O such as SaveImages,
ExportToSpreadsheet and ExportToDatabase.

The input modules and the LoadImages module can be used -
the bridge finds the names of the channels from these modules.
It's possible for you to use LoadData, but only if you read
the image list file from a fixed location - it's best
to modify your pipeline to use some other image input method. 