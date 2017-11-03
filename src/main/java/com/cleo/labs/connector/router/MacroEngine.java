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

/**
 * Provides a framework for expanding strings with embedded expressions of the
 * form {@code ${expression}} in the context of the {@link EDIMetadata} and
 * filename involved in processing a {@link Route}.
 */
import com.google.common.base.Strings;

public class MacroEngine {
    private static final ScriptEngineManager engine_factory = new ScriptEngineManager();

    private ScriptEngine engine;
    private Date now;
    private String filename;
    private Metadata metadata;

    /**
     * Enumerates the macro tokens with functions defining how the
     * values are derived from the {@code MacroEngine} metadata and
     * filename properties.
     */
    private enum Token {
        file(Function.identity(), null),
        base(FilenameUtils::getBaseName, null),
        ext((fn) -> FilenameUtils.getExtension(fn).replaceFirst("^(?=[^\\.])","."), null), // prefix with "." unless empty or already "."
        sender(null, (metadata) -> metadata.sender().id()),
        receiver(null, (metadata) -> metadata.receiver().id()),
        groupSender(null, (metadata) -> metadata.groupSender().id()),
        groupReceiver(null, (metadata) -> metadata.groupReceiver().id()),
        senderQualifier(null, (metadata) -> metadata.sender().qualifier()),
        receiverQualifier(null, (metadata) -> metadata.receiver().qualifier()),
        groupSenderQualifier(null, (metadata) -> metadata.groupSender().qualifier()),
        groupReceiverQualifier(null, (metadata) -> metadata.groupReceiver().qualifier()),
        function(null, Metadata::function),
        type(null, Metadata::type),
        icn(null, Metadata::icn);

        private final Function<String, String> filenameFunction;
        private final Function<Metadata, String> metadataFunction;

        private Token(Function<String,String> filenameFunction,
                Function<Metadata,String> metadataFunction) {
            this.filenameFunction = filenameFunction;
            this.metadataFunction = metadataFunction;
        }

    };

    /**
     * Starts up the {@link ScriptEngine}, if one has not
     * yet been started.
     */
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

    /**
     * Creates a new {@code MacroEngine} without starting the
     * real JavaScript {@link ScriptEngine}.
     */
    public MacroEngine() {
        now = new Date();
    }

    /**
     * Creates a new {@code MacroEngine} without starting the
     * real JavaScript {@link ScriptEngine}, but sets the initial
     * metadata and filename for convenience.
     * @param metadata the metadata to set
     * @param filename the filename to set
     */
    public MacroEngine(Metadata metadata, String filename) {
        this();
        metadata(metadata);
        filename(filename);
    }

    /**
     * Sets the filename for the engine.  If the {@link ScriptEngine} is started,
     * the values are updated in its environment as well.
     * @param filename the filename to set for the engine
     * @return {@code this} to allow for fluent-style setting
     */
    public MacroEngine filename(String filename) {
        this.filename = filename;
        if (engine != null) {
            Stream.of(Token.values())
                    .filter((t) -> t.filenameFunction != null)
                    .forEach((t) -> engine.put(t.name(), filename==null ? "" : t.filenameFunction.apply(filename)));
        }
        return this;
    }

    /**
     * Sets the metadata for the engine.  If the {@link ScriptEngine} is started,
     * the values are updated in its environment as well.
     * @param metadata the new metadata to set for the engine
     * @return {@code this} to allow for fluent-style setting
     */
    public MacroEngine metadata(Metadata metadata) {
        this.metadata = metadata;
        if (engine != null && metadata != null) {
            Stream.of(Token.values())
                    .filter((t) -> t.metadataFunction != null)
                    .forEach((t) -> engine.put(t.name(), metadata==null ? "" : t.metadataFunction.apply(metadata)));
        }
        return this;
    }

    /**
     * {@link Pattern} matching {@code ${expression}} intended for use in
     * a {@link Matcher#find()} loop.
     */
    private static final Pattern SQUIGGLE = Pattern.compile("\\$\\{(?<expr>[^\\}]*)\\}");

    /**
     * Returns the {@code input} with any embedded {@code ${expression}} replaced
     * with the results of evaluating them with {@link #expr(String)}.
     * @param input the string to process
     * @return the result after processing
     */
    public String expand(String input) {
        Matcher m = SQUIGGLE.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, expr(m.group("expr")));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * {@link Pattern} matching {@code date('...')} with {@code ...} as {@code group(1)}
     * or {@code date("...")} with {@code ...} as {@code group(2)}.
     */
    public static final Pattern DATEFUNCTION = Pattern.compile("date\\((?:'([^']*)'|\"([^\"]*)\")\\)");

    /**
     * Attempts a simple variable lookup for {@code name}, or tries simple
     * evaluation of {@code date} expressions, returning the result, or
     * {@code null} if the expression is not a simple lookup or date expression.
     * @param name the variable name or date expression
     * @return the result, or {@code null} if the input was not a simple variable name or date expression
     */
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
                    return filename == null ? "" : Strings.nullToEmpty(token.filenameFunction.apply(filename));
                } else if (token.metadataFunction != null) {
                    return metadata == null ? "" : Strings.nullToEmpty(token.metadataFunction.apply(metadata));
                }
            } catch (IllegalArgumentException e) {
                // fall through
            }
        }
        return null; // no such token
    }

    /**
     * Returns the results of evaluating the input {@code macro} as
     * a JavaScript expression.  First, tries to do a simple {@link #lookup(String)}
     * to avoid the JavaScript engine overhead.
     * @param macro the input expression to evaluate
     * @return the result after lookup or evaluation
     */
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
