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

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.annotations.Command;
import com.cleo.connector.api.command.ConnectorCommandResult;
import com.cleo.connector.api.command.PutCommand;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.cleo.labs.connector.router.RoutableInputStreams.RoutableInputStream;
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

    @Command(name = PUT, options = { Unique, Delete })
    public ConnectorCommandResult put(PutCommand put) throws ConnectorException, IOException {
        String destination = put.getDestination().getPath();
        IConnectorOutgoing source = put.getSource();

        logger.debug(String.format("PUT local '%s' to remote '%s'", source.getPath(), destination));

        //TODO: boolean unique = ConnectorCommandUtil.isOptionOn(put.getOptions(), Unique);

        Route[] routes = Stream.of(config.getRoutes())
                .filter((r) -> Strings.isNullOrEmpty(r.filename()) || source.getPath().matches(r.filename()))
                .toArray(Route[]::new);

        boolean nomatch = false; // this will be set true if any stream is not routable
        MacroEngine engine = new MacroEngine().filename(source.getPath());

        for (RoutableInputStream is : new RoutableInputStreams(source.getStream(), config.getPreviewSize())) {
            List<String> destinations = new ArrayList<>();
            for (Route route : routes) {
                logger.debug(String.format("matching %s for route %s", source.getPath(), route.toString()));
                if (is.matches(route)) {
                    engine.metadata(is.metadata());
                    logger.debug(String.format("matched metadata: %s", is.metadata().toString()));
                    String output = engine.expand(route.destination());
                    destinations.add(output);
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
                ByteStreams.copy(is, ByteStreams.nullOutputStream());
            } else {
                ParallelOutputStream out = new ParallelOutputStream(outputs);
                transfer(is, out, false);
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
