package com.cleo.labs.connector.router;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.cleo.labs.connector.router.RoutableHL7.MSH;
import com.cleo.labs.connector.router.Routables.Routable;
import com.google.gwt.thirdparty.guava.common.io.CharStreams;

public class TestRoutableHL7 {

    @Test
    public void testMinimalMSH() {
        assertTrue(new MSH("MSH|^~\\&||||||||||2.\r".getBytes()).valid());
    }
    @Test
    public void testMSHTooShort() {
        assertFalse(new MSH("MSH|^~\\&|||||||||2.\r".getBytes()).valid());
    }
    @Test
    public void testMSHTooFew() {
        assertFalse(new MSH("MSH|^~\\&|||||longenough||||2.\r".getBytes()).valid());
    }
    @Test
    public void testMSHDupSeparator() {
        assertFalse(new MSH("MSH|^~^&||||||||||2+\r".getBytes()).valid());
    }
    private static final String EPIC = "MSH|^~\\&|EPIC|EPICADT|SMS|SMSADT|199912271408|CHARRIS|ADT^A04|1817457|D|2.5|\rmore stuff";
    @Test
    public void testMSHParsing() {
        MSH msh = new MSH(EPIC.getBytes());
        assertEquals("EPIC", msh.item(3));
        assertArrayEquals(new String[]{"EPICADT"}, msh.subitems(4));
        assertEquals("ADT^A04", msh.item(9));
        assertArrayEquals(new String[]{"ADT","A04"}, msh.subitems(9));
        assertEquals("2.5", msh.item(12));
    }
    @Test
    public void testMSHEscapes() {
        MSH msh = new MSH("MSH|^~\\&|EPIC|\\T\\\\S\\\\F\\\\R\\\\E\\\\X614263\\|SMS|SMSADT|199912271408|CHARRIS|ADT^A04|1817457|D|2.5|\r".getBytes());
        assertEquals("EPIC", msh.item(3));
        assertEquals("SMS", msh.item(5));
        assertEquals("&^|~\\aBc", msh.item(4));
    }
    @Test
    public final void testHL7Stream() throws IOException {
        InputStream bis = new ByteArrayInputStream(EPIC.getBytes());
        int count = 0;
        Route r = new Route().sender("EPICADT").type("ADT").function("A04");
        Route no = new Route().sender("EPIC").type("A04");
        for (Routable routable : new Routables(bis, 2048)) {
            assertNotNull(routable.metadata());
            assertNotNull(routable.metadata().sender());
            assertEquals("EPICADT", routable.metadata().sender().id());
            assertNull(routable.metadata().sender().qualifier());
            assertEquals("SMSADT", routable.metadata().receiver().id());
            assertNull(routable.metadata().receiver().qualifier());
            assertEquals("EPIC", routable.metadata().groupSender().id());
            assertEquals("SMS", routable.metadata().groupReceiver().id());
            assertEquals("A04", routable.metadata().function());
            assertEquals("ADT", routable.metadata().type());
            assertEquals("1817457", routable.metadata().icn());
            assertEquals(EPIC, CharStreams.toString(new InputStreamReader(routable.inputStream())));
            assertTrue(routable.matches(r));
            assertFalse(routable.matches(no));
            count++;
            routable.inputStream().close();
        }
        assertEquals(1, count);
    }
 
}
