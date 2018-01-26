package com.cleo.labs.connector.router;

import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.labs.connector.router.Route;
import com.google.common.base.Strings;

/**
 * A configuration wrapper around a {@link RouterConnectorClient}
 * instance and its {@link RouterConnectorSchema}, exposing bean-like
 * getters for the schema properties converted to their usable forms:
 * <table border="1">
 *   <tr><th>Property</th><th>Stored As</th><th>Returned as</th></tr>
 *   <tr><td>Preview Size</td><td>String matching "\\d+[kmg[b]]"</td><td>int</td></tr>
 *   <tr><td>Error Destination</td><td>String</td><td>String</td></tr>
 *   <tr><td>Routes</td><td>JSON array</td><td>{@link Route Route[]}</td></tr>
 * </table>
 */
public class RouterConnectorConfig {
    private RouterConnectorClient client;
    private RouterConnectorSchema schema;

    /**
     * Constructs a configuration wrapper around a {@link RouterConnectorClient}
     * instance and its {@link RouterConnectorSchema}, exposing bean-like
     * getters for the schema properties converted to their usable forms.
     * @param client the RouterConnectorClient
     * @param schema its RouterConnectorSchema
     */
    public RouterConnectorConfig(RouterConnectorClient client, RouterConnectorSchema schema) {
        this.client = client;
        this.schema = schema;
    }
 
    /**
     * Gets the Preview Size property converted to an {@code int}.
     * @return the Preview Size
     * @throws ConnectorPropertyException
     */
    public int getPreviewSize () throws ConnectorPropertyException {
        return parseLength(schema.previewSize.getValue(client));
    }

    /**
     * Gets the Error Destination property.
     * @return the Error Destination
     * @throws ConnectorPropertyException
     */
    public String getErrorDestination() throws ConnectorPropertyException {
        return schema.errorDestination.getValue(client);
    }

    /**
     * Gets the Force Unique property.
     * @return the Force Unique property
     * @throws ConnectorPropertyException
     */
    public boolean getForceUnique() throws ConnectorPropertyException {
        return schema.forceUnique.getValue(client);
    }

    /**
     * Gets the Routes property, converted from its internal JSON
     * string representation to a {@link Route Route[]}.
     * @return an array of Routes, possibly empty, but never {@code null}
     * @throws ConnectorPropertyException
     */
    public Route[] getRoutes() throws ConnectorPropertyException {
        String value = schema.routingTable.getValue(client);
        return RoutingTableProperty.toRoutes(value);
    }

    /**
     * Gets the Route To First Matching Route Only property.
     * @return the Route To First Matching Route Only property
     * @throws ConnectorPropertyException
     */
    public boolean getRouteToFirstMatchingRouteOnly() throws ConnectorPropertyException {
        return schema.routeToFirstMatchingRouteOnly.getValue(client);
    }

    /**
     * Parses an optionally suffixed length:
     * <ul>
     * <li><b>nnnK</b> nnn KB (technically "kibibytes", * 1024)</li>
     * <li><b>nnnM</b> nnn MB ("mebibytes", * 1024^2)</li>
     * <li><b>nnnG</b> nnn GB ("gibibytes", * 1024^3)</li>
     * </ul>
     * Note that suffixes may be upper or lower case.  A trailing "b"
     * (e.g. kb, mb, ...) is tolerated but not required.
     * @param length the string to parse
     * @return the parsed int
     * @throws NumberFormatException
     * @see {@link Integer#parseInt(String)}
     */
    public static int parseLength(String length) {
        if (!Strings.isNullOrEmpty(length)) {
            int multiplier = 1;
            int  check = length.length()-1;
            if (check>=0) {
                char suffix = length.charAt(check);
                if ((suffix=='b' || suffix=='B') && check>0) {
                    check--;
                    suffix = length.charAt(check);
                }
                switch (suffix) {
                case 'k': case 'K': multiplier =           1024; break;
                case 'm': case 'M': multiplier =      1024*1024; break;
                case 'g': case 'G': multiplier = 1024*1024*1024; break;
                default:
                }
                if (multiplier != 1) {
                    length = length.substring(0, check);
                }
            }
            return Integer.parseInt(length)*multiplier;
        }
        return 0;
    }

}
