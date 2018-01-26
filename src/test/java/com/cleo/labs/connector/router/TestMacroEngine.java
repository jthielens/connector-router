package com.cleo.labs.connector.router;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMacroEngine {

    private static Metadata metadata() {
        Metadata mock = mock(Metadata.class);
        when(mock.sender()).thenReturn(new EDIID("FF","FROM"));
        when(mock.receiver()).thenReturn(new EDIID("RR","TO"));
        when(mock.groupSender()).thenReturn(new EDIID("FG","GFROM"));
        when(mock.groupReceiver()).thenReturn(new EDIID("RG","GTO"));
        when(mock.function()).thenReturn("FUNCTION");
        when(mock.type()).thenReturn("MOCK");
        when(mock.icn()).thenReturn("123456");
        when(mock.isEmpty()).thenReturn(false);
        return mock;
    }

    @Test
    public void testFilename() {
        MacroEngine engine = new MacroEngine(metadata(), "filename.ext");
        assertEquals("FROM", engine.expr("sender"));
        assertEquals("final=FROM-.ext.", engine.expand("final=${sender}-${ext}."));
        assertEquals("final=FROM-.ext.", engine.expand("final=${sender+'-'+ext}."));
        assertTrue(engine.started());
    }

    @Test
    public void testMetadata() {
        MacroEngine engine = new MacroEngine(metadata(), "filename.ext").counter("17").unique(".UNIQUE");
        assertEquals("FROM", engine.expand("${sender}"));
        assertEquals("TO", engine.expand("${receiver}"));
        assertEquals("GFROM", engine.expand("${groupSender}"));
        assertEquals("GTO", engine.expand("${groupReceiver}"));
        assertEquals("FF", engine.expand("${senderQualifier}"));
        assertEquals("RR", engine.expand("${receiverQualifier}"));
        assertEquals("FG", engine.expand("${groupSenderQualifier}"));
        assertEquals("RG", engine.expand("${groupReceiverQualifier}"));
        assertEquals("FUNCTION", engine.expand("${function}"));
        assertEquals("MOCK", engine.expand("${type}"));
        assertEquals("123456", engine.expand("${icn}"));
        assertEquals(".UNIQUE", engine.expand("${unique}"));
        assertEquals("17", engine.expand("${counter}"));
        assertFalse(engine.started());
    }

    @Test
    public void testNullMetadata() {
        MacroEngine engine = new MacroEngine();
        assertEquals("", engine.expand("${sender}"));
        assertEquals("", engine.expand("${receiver}"));
        assertEquals("", engine.expand("${groupSender}"));
        assertEquals("", engine.expand("${groupReceiver}"));
        assertEquals("", engine.expand("${senderQualifier}"));
        assertEquals("", engine.expand("${receiverQualifier}"));
        assertEquals("", engine.expand("${groupSenderQualifier}"));
        assertEquals("", engine.expand("${groupReceiverQualifier}"));
        assertEquals("", engine.expand("${function}"));
        assertEquals("", engine.expand("${type}"));
        assertEquals("", engine.expand("${icn}"));
        assertEquals("", engine.expand("${unique}"));
        assertEquals("", engine.expand("${counter}"));
        assertFalse(engine.started());
    }

    @Test
    public void testNullMetadataOverwrite() {
        MacroEngine engine = new MacroEngine(metadata(), "filename.ext");
        engine.filename(null).metadata(null);
        assertEquals("", engine.expand("${file}"));
        assertEquals("", engine.expand("${base}"));
        assertEquals("", engine.expand("${ext}"));
        assertEquals("", engine.expand("${sender}"));
        assertEquals("", engine.expand("${receiver}"));
        assertEquals("", engine.expand("${groupSender}"));
        assertEquals("", engine.expand("${groupReceiver}"));
        assertEquals("", engine.expand("${senderQualifier}"));
        assertEquals("", engine.expand("${receiverQualifier}"));
        assertEquals("", engine.expand("${groupSenderQualifier}"));
        assertEquals("", engine.expand("${groupReceiverQualifier}"));
        assertEquals("", engine.expand("${function}"));
        assertEquals("", engine.expand("${type}"));
        assertEquals("", engine.expand("${icn}"));
        assertEquals("", engine.expand("${unique}"));
        assertEquals("", engine.expand("${counter}"));
        assertFalse(engine.started());
    }

    @Test
    public void testCapture() {
        Pattern p = Pattern.compile("(?<sender>\\S+)\\s+(?<receiver>\\S+)\\s+(?<function>\\S+)\\s+(?<type>\\S+)\\s+(?<icn>\\S+)");
        Matcher m = p.matcher("FROM TO GG 214 123456");
        m.matches();
        Metadata metadata = new RoutableContent.ContentMetadata(m);
        MacroEngine engine = new MacroEngine(metadata, "filename.ext");
        assertEquals("FROM", engine.expand("${sender}"));
        assertEquals("TO", engine.expand("${receiver}"));
        assertEquals("GG", engine.expand("${function}"));
        assertEquals("214", engine.expand("${type}"));
        assertEquals("123456", engine.expand("${icn}"));
    }

    @Test
    public void testDateLookup() {
        MacroEngine engine = new MacroEngine(metadata(), "filename.ext");
        Date now = new Date();
        String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(now);
        assertEquals(yyyyMMdd, engine.expr("date('yyyyMMdd')"));
        assertEquals(yyyyMMdd, engine.lookup("date('yyyyMMdd')"));
        assertEquals(yyyyMMdd, engine.expr("date(\"yyyyMMdd\")"));
        assertEquals(yyyyMMdd, engine.lookup("date(\"yyyyMMdd\")"));
    }

}
