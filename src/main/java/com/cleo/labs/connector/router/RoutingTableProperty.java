package com.cleo.labs.connector.router;

import com.cleo.connector.api.annotations.Array;
import com.cleo.connector.api.annotations.Display;
import com.cleo.connector.api.annotations.Property;
import com.cleo.connector.api.interfaces.IConnectorProperty;
import com.cleo.connector.api.property.PropertyBuilder;
import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * Routing table extended property - @Array of subproperties (identified by @Property)
 * 
 * Each route contains a set of {@link java.util.regex.Pattern Patterns}
 * that are matched against the metadata for a
 * {@link Routables.RoutableInputStream RoutableInputStream}, plus
 * an Enabled flag and a Destination expression.  Empty patterns are
 * considered "disabled" and match anything (including empty/{@code null}
 * values).<p/>
 * Input streams are either EDI or non-EDI, and the set of routes to be
 * considered for each depends on the {@code content} property.  EDI files
 * are parsed by the VersaLex EDI libraries, so any route with a defined
 * {@code content} matching property will be ignored.  Non-EDI files are
 * matched and parsed (using named capture groups) by the regular expression
 * in the {@code content} property, so only routes with {@code content}
 * properties will be considered.
 */
@Array
public class RoutingTableProperty {

    private static final Gson GSON = new Gson();

    /**
     * Display value for the Routing Table property
     * @param value the Routing Table property value (a JSON array)
     * @return "n Records" (or "1 Record")
     */
    @Display
    public String display(String value) {
        int size = toRoutes(value).length;
        return String.format("%d Record%s", size, size==1?"":"s");
    }
  
    @Property
    final IConnectorProperty<Boolean> enabled = new PropertyBuilder<>("Enabled", true)
        .setRequired(true)
        .build();

    @Property
    final public IConnectorProperty<String> filename = new PropertyBuilder<>("Filename", "")
        .setDescription("Regular expression to match filenames")
        .build();

    @Property
    final public IConnectorProperty<String> content = new PropertyBuilder<>("Content", "")
        .setDescription("Regular expression to match and parse file contents")
        .build();

    @Property
    final public IConnectorProperty<String> sender = new PropertyBuilder<>("Sender", "")
        .setDescription("Regular expression to match the file sender")
        .build();

    @Property
    final public IConnectorProperty<String> receiver = new PropertyBuilder<>("Receiver", "")
        .setDescription("Regular expression to match the file receiver")
        .build();

    @Property
    final public IConnectorProperty<String> groupSender = new PropertyBuilder<>("GroupSender", "")
        .setDescription("Regular expression to match the file group sender")
        .build();

    @Property
    final public IConnectorProperty<String> groupReceiver = new PropertyBuilder<>("GroupReceiver", "")
        .setDescription("Regular expression to match the file group receiver")
        .build();

    @Property
    final public IConnectorProperty<String> function = new PropertyBuilder<>("Function", "")
        .setDescription("Regular expression to match the functional group identifier")
        .build();

    @Property
    final public IConnectorProperty<String> type = new PropertyBuilder<>("Type", "")
        .setDescription("Regular expression to match the transaction type")
        .build();

    @Property
    final public IConnectorProperty<String> destination = new PropertyBuilder<>("Destination", "")
        .setDescription("Expression, using embedded ${token}s, to compute the destination"+
                        " for the file.  The destination can be a URI, e.g. scheme:path.")
        .build();

    /**
     * Deserialize the JSON array into a Java {@code Route[]}.
     * @param value the JSON array (may be {@code null})
     * @return a {@code Route[]}, may be {@code Route[0]}, but never {@code null}
     */
    public static Route[] toRoutes(String value) {
        return Strings.isNullOrEmpty(value) ? new Route[0] : GSON.fromJson(value, Route[].class);
    }
}