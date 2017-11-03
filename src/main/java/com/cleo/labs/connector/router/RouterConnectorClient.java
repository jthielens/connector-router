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
        if (unique) {
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

    @Command(name = PUT, options = { Unique, Delete })
    public ConnectorCommandResult put(PutCommand put) throws ConnectorException, IOException {
        String destination = put.getDestination().getPath();
        IConnectorOutgoing source = put.getSource();

        logger.debug(String.format("PUT local '%s' to remote '%s'", source.getPath(), destination));

        boolean unique = ConnectorCommandUtil.isOptionOn(put.getOptions(), Unique);

        Route[] routes = Stream.of(config.getRoutes())
                .filter((r) -> Strings.isNullOrEmpty(r.filename()) || source.getPath().matches(r.filename()))
                .toArray(Route[]::new);

        boolean nomatch = false; // this will be set true if any stream is not routable
        MacroEngine engine = new MacroEngine().filename(source.getPath());

        for (Routable routable : new Routables(source.getStream(), config.getPreviewSize())) {
            List<String> destinations = new ArrayList<>();
            for (Route route : routes) {
                logger.debug(String.format("matching %s for route %s", source.getPath(), route.toString()));
                if (routable.matches(route)) {
                    engine.metadata(routable.metadata()); // metadata not necessarily available until matches()
                    logger.debug(String.format("matched metadata: %s", routable.metadata().toString()));
                    destinations.add(uniquely(engine, route.destination(), unique));
                }
            }
            OutputStream[] outputs = destinations
                    .stream()
                    .map(LexFile::new)
                    .map((f) -> {
                        try {
                            return new LexFileOutputStream(f);
                        } catch (Exception e) {
                            return null;
                        } })
                    .filter(Objects::nonNull)
                    .toArray(OutputStream[]::new);
            // TODO: unique
            if (outputs.length == 0)  {
                String errorDestination = config.getErrorDestination();
                if (!Strings.isNullOrEmpty(errorDestination)) {
                    try {
                        outputs = new OutputStream[] {
                                new LexFileOutputStream(new LexFile(engine.expand(errorDestination)))
                        };
                    } catch (Exception e) {
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
                    String.format("No matching routes found for '%s'.", source.getPath()));
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
        return new RouterFileAttributes();
    }

}
