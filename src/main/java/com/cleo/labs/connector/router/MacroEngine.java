package com.cleo.labs.connector.router;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;

import com.google.gwt.thirdparty.guava.common.base.Strings;

public class MacroEngine {
    private static final ScriptEngineManager engine_factory = new ScriptEngineManager();

    private ScriptEngine engine;
    private Date now;
    private String filename;
    private EDIMetadata metadata;

    private enum Token {
        file(Function.identity(), null),
        base(FilenameUtils::getBaseName, null),
        ext((fn) -> FilenameUtils.getExtension(fn).replaceFirst("^(?=[^\\.])","."), null),
        sender(null, (metadata) -> metadata.sender().id()),
        receiver(null, (metadata) -> metadata.receiver().id()),
        groupSender(null, (metadata) -> metadata.groupSender().id()),
        groupReceiver(null, (metadata) -> metadata.groupReceiver().id()),
        senderQualifier(null, (metadata) -> metadata.sender().qualifier()),
        receiverQualifier(null, (metadata) -> metadata.receiver().qualifier()),
        groupSenderQualifier(null, (metadata) -> metadata.groupSender().qualifier()),
        groupReceiverQualifier(null, (metadata) -> metadata.groupReceiver().qualifier()),
        type(null, EDIMetadata::type),
        icn(null, EDIMetadata::icn);

        private final Function<String, String> filenameFunction;
        private final Function<EDIMetadata, String> metadataFunction;

        private Token(Function<String,String> filenameFunction,
                Function<EDIMetadata,String> metadataFunction) {
            this.filenameFunction = filenameFunction;
            this.metadataFunction = metadataFunction;
        }

    };

    private void startEngine() {
        if (engine == null) {
            engine  = engine_factory.getEngineByName("JavaScript");
            try {
                engine.eval("load('nashorn:mozilla_compat.js');"+
                    "function date(format) { return new java.text.SimpleDateFormat(format).format(now); }");
                engine.put("now", now);
                metadata(metadata);
                filename(filename);
            } catch (ScriptException e) {
                // gulp
            }
        }
    }

    public MacroEngine(EDIMetadata metadata, String filename) {
        now = new Date();
        metadata(metadata);
        filename(filename);
    }

    public MacroEngine filename(String filename) {
        this.filename = filename;
        if (engine != null) {
            Stream.of(Token.values())
                    .filter((t) -> t.filenameFunction != null)
                    .forEach((t) -> engine.put(t.name(), t.filenameFunction.apply(filename)));
        }
        return this;
    }

    public MacroEngine metadata(EDIMetadata metadata) {
        this.metadata = metadata;
        if (engine != null) {
            Stream.of(Token.values())
                    .filter((t) -> t.metadataFunction != null)
                    .forEach((t) -> engine.put(t.name(), t.metadataFunction.apply(metadata)));
        }
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

    public static final Pattern DATEFUNCTION = Pattern.compile("date\\((?:'([^']*)'|\"([^\"]*)\")\\)");

    public String lookup(String name) {
        Matcher date = DATEFUNCTION.matcher(name);
        if (date.matches()) {
            String format = Strings.nullToEmpty(date.group(1)) +
                    Strings.nullToEmpty(date.group(2));
            return new SimpleDateFormat(format).format(now);
        } else {
            try {
                Token token = Token.valueOf(name);
                if (token.filenameFunction != null) {
                    return Strings.nullToEmpty(token.filenameFunction.apply(filename));
                } else if (token.metadataFunction != null) {
                    return Strings.nullToEmpty(token.metadataFunction.apply(metadata));
                }
            } catch (IllegalArgumentException e) {
                // fall through
            }
        }
        return null; // no such token
    }

    public String expr(String macro) {
        // first try just looking up a simple value
        String lookup = lookup(macro);
        if (lookup != null) {
            return lookup;
        }
        // ok, now run the JS engine, starting it if needed
        startEngine();
        try {
            Object result = engine.eval(macro);
            return result.toString();
        } catch (ScriptException e) {
            return "";
        }
    }
}
