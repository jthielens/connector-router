package com.cleo.labs.connector.router;

import static com.cleo.connector.api.command.ConnectorCommandName.ATTR;
import static com.cleo.connector.api.command.ConnectorCommandName.PUT;
import static com.cleo.connector.api.command.ConnectorCommandOption.Delete;
import static com.cleo.connector.api.command.ConnectorCommandOption.Unique;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.annotations.Command;
import com.cleo.connector.api.command.ConnectorCommandResult;
import com.cleo.connector.api.command.ConnectorCommandUtil;
import com.cleo.connector.api.command.PutCommand;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.cleo.labs.connector.router.Routables.Routable;
import com.cleo.lexicom.beans.LexFile;
import com.cleo.lexicom.streams.LexFileOutputStream;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

public class RouterConnectorClient extends ConnectorClient {
    private RouterConnectorConfig config;

    /**
     * Constructs a new {@code RouterConnectorClient} for the schema
     * @param schema the {@code RouterConnectorSchema}
     */
    public RouterConnectorClient(RouterConnectorSchema schema) {
        this.config = new RouterConnectorConfig(this, schema);
    }

    /**
     * Expands {@code destination} using {@code engine}.  If {@code unique} is set,
     * the expanded destination will be tested for existence, with a uniqueness
     * counter (1, 2, ...) inserted until a non-existing filename is achieved.  The
     * location of the uniqueness token is determined either by the {@code ${unique}}
     * token being expanded in the destination expression, or for expressions that
     * do not appear to incorporate {@code ${unique}}, the filename have the counter
     * inserted before the filename extension.
     * @param engine the {@link MacroEngine} engine used for expansion
     * @param destination the destination filename expression
     * @param unique the {@code -UNI} flag to {@code PUT}
     * @return an expanded destination, uniquely if so requested
     */
    private String uniquely (MacroEngine engine, String destination, boolean unique) {
        String output = engine.expand(destination);
        if (unique && !Strings.isNullOrEmpty(output)) {
            LexFile file = new LexFile(output);
            int counter = 0;
            String candidate = output;
            boolean justSliceIt = false; // when true, forget the engine and use string slicing
            String base = ""; // when justSliceIt, the base of the filename
            String ext = ""; // when justSliceIt, the extension of the filename
            while (file.exists()) {
                counter++;
                if (!justSliceIt) {
                    engine.unique("."+counter);
                    candidate = engine.expand(destination);
                    if (candidate.equals(output)) {
                        justSliceIt = true;
                        // ${unique} not in pattern -- use traditional .extension
                        ext = FilenameUtils.getExtension(output).replaceFirst("^(?=[^\\.])","."); // prefix with "." unless empty or already "."
                        base = output.substring(0, output.length()-ext.length());
                    }
                }
                if (justSliceIt) {
                    candidate = base+"."+counter+ext;
                }
                file = new LexFile(candidate);
            }
            output = candidate;
            engine.unique(null); // reset it for the next file
        }
        return output;
    }

    /**
     * Figures out the best intent of the user for the destination filename to use:
     * <ul><li>if a destination path is provided, use it (e.g. PUT source destination or
     *         through a URI, LCOPY source router:host/destination).</li>
     *     <li>if the destination path matches the host alias (e.g. LCOPY source router:host),
     *         prefer the source filename</li>
     *     <li>if the destination is not useful and the source is not empty, use it</li>
     * @param put the {@link PutCommand}
     * @return a String to use as the filename
     */
    private String bestFilename(PutCommand put) {
        String destination = put.getDestination().getPath();
        if (Strings.isNullOrEmpty(destination) || destination.equals(getHost().getAlias())) {
            String source = put.getSource().getPath();
            if (!Strings.isNullOrEmpty(source)) {
                destination = source;
            }
        }
        return destination;
    }

    @Command(name = PUT, options = { Unique, Delete })
    public ConnectorCommandResult put(PutCommand put) throws ConnectorException, IOException {
        String destination = put.getDestination().getPath();
        IConnectorOutgoing source = put.getSource();
        String filename = bestFilename(put);

        logger.debug(String.format("PUT local '%s' to remote '%s' (matching filename '%s')",
                source.getPath(), destination, filename));

        boolean unique = config.getForceUnique() ||
                ConnectorCommandUtil.isOptionOn(put.getOptions(), Unique);

        Route[] routes = Stream.of(config.getRoutes())
                .filter((r) -> Strings.isNullOrEmpty(r.filename()) || filename.matches(r.filename()))
                .toArray(Route[]::new);

        boolean nomatch = false; // this will be set true if any stream is not routable
        MacroEngine engine = new MacroEngine().filename(filename);
        int counter = 0;

        for (Routable routable : new Routables(source.getStream(), config.getPreviewSize())) {
            if (routable.metadata() != null) {
                logger.debug(String.format("new routable metadata: %s", routable.metadata().toString()));
            }
            List<String> destinations = new ArrayList<>();
            // first collect unevaluated destinations
            for (Route route : routes) {
                logger.debug(String.format("matching %s for route %s", filename, route.toString()));
                if (route.enabled() && routable.matches(route)) {
                    engine.metadata(routable.metadata()); // metadata not necessarily available until matches()
                    logger.debug(String.format("matched metadata: %s", routable.metadata().toString()));
                    if (!Strings.isNullOrEmpty(route.destination())) {
                        destinations.add(route.destination());
                    }
                }
            }
            // now evaluate them, inserting the counters
            for (int d = 0; d < destinations.size(); d++) {
                if (destinations.size() == 1 || config.getRouteToFirstMatchingRouteOnly()) {
                    engine.counter(String.valueOf(counter+1));
                } else {
                    engine.counter(String.valueOf(counter+1)+"."+String.valueOf(d+1));
                }
                String output = Strings.emptyToNull(uniquely(engine, destinations.get(d), unique));
                destinations.set(d, output);
                if (output != null) {
                    logger.debug(String.format("routing file to: %s", output));
                    if (config.getRouteToFirstMatchingRouteOnly()) {
                        // after first match null out the rest of them, if any
                        for (int extra = d+1; extra < destinations.size(); extra++) {
                            destinations.set(extra, null);
                        }
                        break;
                    }
                }
            }
            counter++;
            // now convert to OutputStreams
            OutputStream[] outputs = destinations
                    .stream()
                    .filter(Objects::nonNull)
                    .map(LexFile::new)
                    .map((f) -> {
                        try {
                            return new LexFileOutputStream(f);
                        } catch (Exception e) {
                            logger.logWarning(String.format("Destination '%s' skipped due to error: %s", f, e.getMessage()));
                            return null;
                        } })
                    .filter(Objects::nonNull)
                    .toArray(OutputStream[]::new);
            if (outputs.length == 0)  {
                String errorDestination = config.getErrorDestination();
                if (!Strings.isNullOrEmpty(errorDestination)) {
                    String output = null;
                    try {
                        output = uniquely(engine, errorDestination, unique);
                        logger.debug(String.format("routing file to error destination: %s", output));
                        outputs = new OutputStream[] {
                                new LexFileOutputStream(new LexFile(output))
                        };
                    } catch (Exception e) {
                        logger.logWarning(String.format("Error Destination '%s' ignored due to error: %s", output, e.getMessage()));
                        // well, we tried
                    }
                }
            }
            if (outputs.length == 0) {
                nomatch = true;
                ByteStreams.copy(routable.inputStream(), ByteStreams.nullOutputStream());
            } else {
                ParallelOutputStream out = new ParallelOutputStream(outputs);
                transfer(routable.inputStream(), out, false);
            }
        }

        if (nomatch) {
            return new ConnectorCommandResult(ConnectorCommandResult.Status.Error,
                    String.format("No matching routes found for '%s'.", filename));
        } else {
            return new ConnectorCommandResult(ConnectorCommandResult.Status.Success);
        }
    }

    /**
     * Get the file attribute view associated with a file path
     * 
     * @param path the file path
     * @return the file attributes
     * @throws com.cleo.connector.api.ConnectorException
     * @throws java.io.IOException
     */
    @Command(name = ATTR)
    public BasicFileAttributeView getAttributes(String path) throws ConnectorException, IOException {
        logger.debug(String.format("ATTR '%s'", path));
        throw new ConnectorException(String.format("'%s' does not exist or is not accessible", path),
                ConnectorException.Category.fileNonExistentOrNoAccess);
        //return new RouterFileAttributes(logger);
    }

}
