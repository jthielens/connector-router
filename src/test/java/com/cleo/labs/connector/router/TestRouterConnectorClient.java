package com.cleo.labs.connector.router;

import static com.cleo.connector.api.command.ConnectorCommandName.PUT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.command.PutCommand;
import com.cleo.connector.api.directory.Directory.Type;
import com.cleo.connector.api.helper.NetworkConnection;
import com.cleo.connector.api.directory.Entry;
import com.cleo.connector.api.interfaces.IConnectorConfig;
import com.cleo.connector.api.interfaces.IConnectorFile;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.connector.shell.interfaces.IConnector;
import com.cleo.connector.shell.interfaces.IConnectorAction;
import com.cleo.connector.shell.interfaces.IConnectorConnection;
import com.cleo.connector.shell.interfaces.IConnectorHost;
import com.google.common.base.Joiner;

public class TestRouterConnectorClient {

    static private final String ryder = Joiner.on("").join(new String[] {
            "ISA*00*          *00*          *02*SCAC           *01*006922827HUH1  *080903*1132*U*00401*000010067*0*P*>~",
            "GS*QM*SCAC*006922827HUH1*20080903*1132*9951*X*004010~",
            "ST*214*099510001~",
            "B10*4735103*5365205*SCAC~",
            "L11*5365205*LO~",
            "L11*01*QN~",
            "L11*392651*PO~",
            "L11*392651*PO~",
            "N1*SH*HUHTAMAKI FSBU~",
            "N3*5566 NEW VIENNA ROAD~",
            "N4*NEW VIENNA*OH*45159*US~",
            "N1*CN*HUHTAMAKI~",
            "N3*100 HIGGENSON AVE~",
            "N4*LINCOLN*RI*02865*US~",
            "LX*1~",
            "AT7***AA*NA*20080903*16000000*ET~",
            "MS1*NEW VIENNA*OH*US~",
            "AT8*G*L*6240*402~",
            "SE*17*099510001~"});
    static private final String xmlsample = Joiner.on("").join(new String[] {
            "<File>",
            "</File>"});

    private static class StringSource implements IConnectorOutgoing {
        private String path;
        private String content;
        public StringSource(String path, String content) {
            this.path = path;
            this.content = content;
        }
        @Override
        public String getDefaultName() { return null; }
        @Override
        public IConnectorFile getFile() { return null; }
        @Override
        public Long getLength() { return null; }
        @Override
        public Map<String, String> getMetadata() { return null; }
        @Override
        public String getName() { return path.replaceFirst(".*/", ""); }
        @Override
        public String getPath() { return path; }
        @Override
        public IConnectorFile getSentboxCopy() { return null; }
        @Override
        public InputStream getStream() { return new ByteArrayInputStream(content.getBytes()); }
        @Override
        public String getTransferId() { return "transfer-id"; }
        @Override
        public boolean isFile() { return false; }
        @Override
        public boolean isForward() { return false; }
        @Override
        public boolean isStream() { return true; }
        @Override
        public void setForward(boolean arg0) { }
        @Override
        public void setMetadata(Map<String, String> arg0) { }
        @Override
        public IConnectorOutgoing setStream(InputStream arg0) { return null; }
        @Override
        public void setTransferId(String arg0) { }
    }

    private static class OutputCollector implements RouterFileFactory {
        private static class Output {
            public String name;
            public ByteArrayOutputStream output;
            public Output name(String name) {
                this.name = name;
                return this;
            }
            public Output output(ByteArrayOutputStream output) {
                this.output = output;
                return this;
            }
        }
        private List<Output> output = new ArrayList<>();

        public int size() {
            return output.size();
        }
        public String name(int i) {
            return output.get(i).name;
        }
        public String output(int i) {
            return new String(bytes(i));
        }
        public byte[] bytes(int i) {
            return output.get(i).output.toByteArray();
        }

        @SuppressWarnings("serial")
        @Override
        public File getFile(String filename) {
            return new File(filename) {
                @Override
                public boolean exists() {
                    return false;
                }
            };
        }
        @Override
        public OutputStream getOutputStream(String filename) throws Exception {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            output.add(new Output().name(filename).output(os));
            return os;
        }
    }

    public static class TestConfig extends RouterConnectorConfig {
        // config data
        private int previewSize = 4096;
        private String errorDestination = null;
        private boolean forceUnique = false;
        private Route[] routes = null;
        private boolean routeToFirstMatchingRouteOnly = false;
        // fluent setters
        public TestConfig previewSize(int previewSize) {
            this.previewSize = previewSize;
            return this;
        }
        public TestConfig errorDestination(String errorDestination) {
            this.errorDestination = errorDestination;
            return this;
        }
        public TestConfig forceUnique(boolean forceUnique) {
            this.forceUnique = forceUnique;
            return this;
        }
        public TestConfig routes(Route[] routes) {
            this.routes = routes;
            return this;
        }
        public TestConfig routes(String routes) {
            return routes(RoutingTableProperty.toRoutes(routes));
        }
        public TestConfig routeToFirstMatchingRouteOnly(boolean routeToFirstMatchingRouteOnly) {
            this.routeToFirstMatchingRouteOnly = routeToFirstMatchingRouteOnly;
            return this;
        }
        // override classic getters
        @Override
        public String getErrorDestination() throws ConnectorPropertyException {
            return errorDestination;
        }
        @Override
        public boolean getForceUnique() throws ConnectorPropertyException {
            return forceUnique;
        }
        @Override
        public int getPreviewSize() throws ConnectorPropertyException {
            return previewSize;
        }
        @Override
        public Route[] getRoutes() throws ConnectorPropertyException {
            return routes;
        }
        @Override
        public boolean getRouteToFirstMatchingRouteOnly() throws ConnectorPropertyException {
            return routeToFirstMatchingRouteOnly;
        }
        // default constructor
        public TestConfig() {
            super(null, null);
        }
    }

    private static RouterConnectorClient setupClient(RouterConnectorClient client) {
        IConnectorConnection cc = new IConnectorConnection() {
            @Override
            public OutputStream getConnectionOutputStream(OutputStream os) throws IOException { return os; }
            @Override
            public InputStream getConnectionInputStream(InputStream is) throws IOException { return is; }
            @Override
            public void connect(NetworkConnection arg0) throws IOException { }
        };
        IConnectorAction ca = mock(IConnectorAction.class);
        when(ca.isInterrupted()).thenReturn(false);
        IConnector connector = mock(IConnector.class);
        when(connector.getConnectorConnection()).thenReturn(cc);
        when(connector.getConnectorAction()).thenReturn(ca);
        IConnectorConfig connectorConfig = mock(IConnectorConfig.class);
        IConnectorHost connectorHost = mock(IConnectorHost.class);
        client.setup(connector, connectorConfig, connectorHost);

        return client;
    }

    @Test
    public void testPutA214() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes("[{'enabled':'true','type':'214','destination':'local/output/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("local/output/214-test.000010067.edi.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

    @Test
    public void testPutA214Twice() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes("[{'enabled':'true','type':'214','destination':'output/one/214-${base}.${icn}${ext}.${counter}'},"+
                        " {'enabled':'true','type':'214','destination':'output/two/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(2, collector.size());
        assertEquals("output/one/214-test.000010067.edi.1.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
        assertEquals("output/two/214-test.000010067.edi.1.2", collector.name(1));
        assertEquals(ryder, collector.output(1));
    }

    @Test
    public void testPutA214TwiceButOnlyFirst() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routeToFirstMatchingRouteOnly(true)
                .routes("[{'enabled':'true','type':'214','destination':'output/one/214-${base}.${icn}${ext}.${counter}'},"+
                        " {'enabled':'true','type':'214','destination':'output/two/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("output/one/214-test.000010067.edi.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

    @Test
    public void testPutSubcounter() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes("[{'enabled':'true','type':'214','destination':'${ext}'},"+ // this evaluates to empty, so destination ignored
                        " {'enabled':'true','type':'214','destination':'output/two/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("output/two/214-test.000010067.1.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

    @Test
    public void testPutDisabledSoError() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes("[{'enabled':'false','type':'214','destination':'local/output/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("error-test.edi", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

    @Test
    public void testPut() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes("[{'enabled':'true','type':'214','destination':'local/output/214-${base}.${icn}${ext}.${counter}'},"+
                        " {'enabled':'true','type':'990','destination':'local/output/990-${base}.${icn}${ext}.${counter}'},"+
                        " {'enabled':'true','type':'210','destination':'local/output/210-${base}.${icn}${ext}.${counter}'},"+
                        " {'enabled':'true','type':'210','destination':'local/output/210-${base}.${icn}${ext}.${counter}-dup'},"+
                        " {'enabled':'true','content':'.*?<API>(?<type>[^<]*)</API>.*','type':'SingleCarrierHub','destination':'local/output/xml-${file}.${type}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("local/output/214-test.000010067.edi.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

    @Test
    public void testPutFilenameXML() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.xml", xmlsample);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes(new Route[] {
                        new Route().enabled(true).filename("test.xml").destination("success-${file}")
                        });
        //"[{'enabled':'false','type':'214','destination':'local/output/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("success-test.xml", collector.name(0));
        assertEquals(xmlsample, collector.output(0));
    }

    @Test
    public void testPutFilenameEDI() throws ConnectorException, IOException {
        OutputCollector collector = new OutputCollector();
        IConnectorOutgoing source = new StringSource("test.edi", ryder);
        RouterConnectorConfig config = new TestConfig()
                .errorDestination("error-${file}")
                .routes(new Route[] {
                        new Route().enabled(true).filename("test.edi").destination("success-${type}-${base}.${icn}${ext}.${counter}")
                        });
        //"[{'enabled':'false','type':'214','destination':'local/output/214-${base}.${icn}${ext}.${counter}'}]");
        RouterConnectorClient client = setupClient(new RouterConnectorClient(config, collector));

        Entry destination = new Entry(Type.dir);
        PutCommand put = new PutCommand(PUT, Collections.emptySet(), new IConnectorOutgoing[] {source}, destination, Collections.emptyMap());
        client.put(put);

        assertEquals(1, collector.size());
        assertEquals("success-214-test.000010067.edi.1", collector.name(0));
        assertEquals(ryder, collector.output(0));
    }

}
