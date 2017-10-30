package com.cleo.labs.connector.router;

import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.labs.connector.router.Route;
import com.google.common.base.Strings;

public class RouterConnectorConfig {
    private RouterConnectorClient client;
    private RouterConnectorSchema schema;

    public RouterConnectorConfig(RouterConnectorClient client, RouterConnectorSchema schema) {
        this.client = client;
        this.schema = schema;
    }
 
    public int getPreviewSize () throws ConnectorPropertyException {
        return parseLength(schema.previewSize.getValue(client));
    }

    public String getErrorDestination() throws ConnectorPropertyException {
        return schema.errorDestination.getValue(client);
    }

    public Route[] getRoutes() throws ConnectorPropertyException {
        String value = schema.routingTable.getValue(client);
        return RoutingTableProperty.toRoutes(value);
    }

    /* Parses an optionally suffixed length:
     * <ul>
     * <li><b>nnnK</b> nnn KB (technically "kibibytes", * 1024)</li>
     * <li><b>nnnM</b> nnn MB ("mebibytes", * 1024^2)</li>
     * <li><b>nnnG</b> nnn GB ("gibibytes", * 1024^3)</li>
     * </ul>
     * Note that suffixes may be upper or lower case.  A trailing "b"
     * (e.g. kb, mb, ...) is tolerated but not required.
     * @param length the string to parse
     * @return the parsed int
     * @throws {@link NumberFormatException}
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
