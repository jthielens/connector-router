package com.cleo.labs.connector.router;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;

import com.google.gwt.thirdparty.guava.common.base.Strings;

public class MacroEngine {
    private static final ScriptEngineManager engine_factory = new ScriptEngineManager();

    private ScriptEngine engine;

    public MacroEngine(EDIMetadata metadata, String filename) {
        engine  = engine_factory.getEngineByName("JavaScript");
        try {
            engine.eval("load('nashorn:mozilla_compat.js');");
            metadata(metadata);
            filename(filename);
        } catch (ScriptException e) {
            // gulp
        }
    }

    public MacroEngine filename(String filename) {
        String base = FilenameUtils.getBaseName(filename);
        String ext = FilenameUtils.getExtension(filename).replaceFirst("^(?=[^\\.])", "."); // include leading . unless empty
        engine.put("file", filename);
        engine.put("base", base);
        engine.put("ext", ext);
        return this;
    }

    public MacroEngine metadata(EDIMetadata metadata) {
        engine.put("sender", Strings.nullToEmpty(metadata.sender().id()));
        engine.put("receiver", Strings.nullToEmpty(metadata.receiver().id()));
        engine.put("groupSender", Strings.nullToEmpty(metadata.groupSender().id()));
        engine.put("groupReceiver", Strings.nullToEmpty(metadata.groupReceiver().id()));
        engine.put("type", Strings.nullToEmpty(metadata.type()));
        engine.put("icn", Strings.nullToEmpty(metadata.icn()));
        return this;
    }

    private static final Pattern SQUIGGLE = Pattern.compile("\\$\\{(?<expr>[^\\}]*)\\}");

    public String expand(String input) {
        Matcher m = SQUIGGLE.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, expr(m.group("expr")));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String expr(String macro) {
        try {
            Object result = engine.eval(macro);
            return result.toString();
        } catch (ScriptException e) {
            return "";
        }
    }
}
