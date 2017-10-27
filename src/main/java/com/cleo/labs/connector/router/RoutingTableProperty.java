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
 * Each route contains:
 * <ul><li>filename - a {@link java.util.regex.Pattern}.  If non-empty, this
 *           route will be activated only when the pattern matches.</li>
 *     <li>content - a {@link java.util.regex.Pattern}.  If non-empty, the file
 *           content will be read and compared to the pattern and the route
 *           will be activated in case of a match.  In addition, any named
 *           capture groups with the names {@code sender}, {@code receiver}, or
 *           {@code type} that match content will populate the corresponding
 *           metadata tokens.  Otherwise, if the content is EDI, these tokens
 *           will be parsed from the EDI according to the specification.</li>
 * </ul>
 */
@Array
public class RoutingTableProperty {

    private static final Gson GSON = new Gson();

    /**
     * Display value for the property
     * @param value the routing table property value (which is a json object array)
     * @return the number of records as the display value 
     */
    @Display
    public String display(String value) {
        int size = 0;
        if (!Strings.isNullOrEmpty(value)) {
            Route[] routes = GSON.fromJson(value, Route[].class);
            size = routes.length;
        }
        return String.format("%d Record%s", size, size==1?"":"(s)");
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
        .setDescription("Regular expression to match file contents")
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
    final public IConnectorProperty<String> type = new PropertyBuilder<>("Type", "")
        .setDescription("Regular expression to match the transaction type")
        .build();

    @Property
    final public IConnectorProperty<String> destination = new PropertyBuilder<>("Destination", "")
        .setDescription("Expression, using embedded ${token}s, to compute the destination"+
                        " for the file.  The destination can be a URI, e.g. scheme:path.")
        .build();

    /**
     * Convert the json array string to list of SRV record objects
     * @param value the json array string
     * @return the array of SRV record objects
     */
    public static Route[] toRoutes(String value) {
        return GSON.fromJson(value, Route[].class);
    }
}