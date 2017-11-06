package com.cleo.labs.connector.router;

import java.io.IOException;

import com.cleo.connector.api.ConnectorConfig;
import com.cleo.connector.api.annotations.Client;
import com.cleo.connector.api.annotations.Connector;
import com.cleo.connector.api.annotations.Info;
import com.cleo.connector.api.annotations.Property;
import com.cleo.connector.api.interfaces.IConnectorProperty;
import com.cleo.connector.api.property.CommonProperties;
import com.cleo.connector.api.property.CommonProperty;
import com.cleo.connector.api.property.PropertyBuilder;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

@Connector(scheme = "router", description = "File Router")
@Client(RouterConnectorClient.class)
public class RouterConnectorSchema extends ConnectorConfig {
    @Property
    final public IConnectorProperty<String> routingTable = new PropertyBuilder<>("RoutingTable", "")
            .setRequired(true)
            .setAllowedInSetCommand(false)
            .setDescription("The list of routing rules.")
            .setExtendedClass(RoutingTableProperty.class)
            .build();

    @Property
    final IConnectorProperty<String> previewSize = new PropertyBuilder<>("PreviewSize", "8k")
            .setDescription("The number bytes to read ahead for content pattern matching.")
            .addPossibleRegexes("\\d+(?i:[kmg]b?)?")
            .setRequired(false)
            .build();

    @Property
    final IConnectorProperty<String> errorDestination = new PropertyBuilder<>("ErrorDestination", "")
            .setDescription("An optional destination expression for files that do not match any routing rules.")
            .setRequired(false)
            .build();

    @Property
    final IConnectorProperty<Boolean> forceUnique = new PropertyBuilder<>("ForceUnique", false)
            .setDescription("Always create unique filenames as if PUT -UNI were used")
            .build();

    @Property
    final IConnectorProperty<Boolean> enableDebug = CommonProperties.of(CommonProperty.EnableDebug);

    @Info
    protected static String info() throws IOException {
        return Resources.toString(RouterConnectorSchema.class.getResource("info.txt"), Charsets.UTF_8);
    }
}