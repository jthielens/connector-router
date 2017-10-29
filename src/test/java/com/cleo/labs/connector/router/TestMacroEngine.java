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

    private static EDIMetadata metadata() {
        EDIMetadata mock = mock(EDIMetadata.class);
        when(mock.sender()).thenReturn(new EDIID("FF","FROM"));
        when(mock.receiver()).thenReturn(new EDIID("RR","TO"));
        when(mock.groupSender()).thenReturn(new EDIID("FG","GFROM"));
        when(mock.groupReceiver()).thenReturn(new EDIID("RG","GTO"));
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
    }

    @Test
    public void testMetadata() {
        MacroEngine engine = new MacroEngine(metadata(), "filename.ext");
        assertEquals("FROM", engine.expand("${sender}"));
        assertEquals("TO", engine.expand("${receiver}"));
        assertEquals("GFROM", engine.expand("${groupSender}"));
        assertEquals("GTO", engine.expand("${groupReceiver}"));
        assertEquals("FF", engine.expand("${senderQualifier}"));
        assertEquals("RR", engine.expand("${receiverQualifier}"));
        assertEquals("FG", engine.expand("${groupSenderQualifier}"));
        assertEquals("RG", engine.expand("${groupReceiverQualifier}"));
        assertEquals("MOCK", engine.expand("${type}"));
        assertEquals("123456", engine.expand("${icn}"));
    }

    @Test
    public void testCapture() {
        Pattern p = Pattern.compile("(?<sender>\\S+)\\s+(?<receiver>\\S+)\\s+(?<type>\\S+)\\s+(?<icn>\\S+)");
        Matcher m = p.matcher("FROM TO 214 123456");
        m.matches();
        EDIMetadata metadata = new EDIMetadata(m);
        MacroEngine engine = new MacroEngine(metadata, "filename.ext");
        assertEquals("FROM", engine.expand("${sender}"));
        assertEquals("TO", engine.expand("${receiver}"));
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
