package com.cleo.labs.connector.router;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestMacroEngine {

    @Test
    public void test() {
        Pattern p = Pattern.compile("(?<sender>\\S+)\\s+(?<receiver>\\S+)\\s+(?<type>\\S+)\\s+(?<icn>\\S+)");
        Matcher m = p.matcher("FROM TO 214 123456");
        m.matches();
        EDIMetadata metadata = new EDIMetadata(m);
        MacroEngine engine = new MacroEngine(metadata, "filename.ext");
        assertEquals("4", engine.expr("2+2"));
        assertEquals("FROM", engine.expr("sender"));
        assertEquals("final=FROM.ext.", engine.expand("final=${sender}${ext}."));
    }

}
