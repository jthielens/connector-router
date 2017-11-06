package com.cleo.labs.connector.router;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cleo.labs.connector.router.Routables.Routable;
import com.google.common.base.Strings;

public class RoutableContent implements Routables.Routable {

    public static boolean canRoute(PreviewInputStream preview) {
        return true; // for now
    }

    public static Iterator<Routable> getIterator(PreviewInputStream preview) {
        return Collections.singleton((Routable)new RoutableContent(preview)).iterator();
    }

    private PreviewInputStream in;
    private RoutableContent.ContentMetadata metadata;
    private Map<String,RoutableContent.ContentMetadata> namedMetadata;

    public RoutableContent(PreviewInputStream preview) {
        this.in = preview;
        this.namedMetadata = new HashMap<>();
    }

    public Metadata metadata() {
        return metadata;
    }

    private static final Pattern NAMED = Pattern.compile("^(?<name>[a-zA-Z][a-zA-Z0-9]+)(?<names>(?:\\s*,\\s*[a-zA-Z][a-zA-Z0-9]+)*)\\s*:(?<pattern>.*)$");

    public boolean matches(Route route) {
        // false unless there is Content
        if (Strings.isNullOrEmpty(route.content())) {
            return false;
        }
        // could be name:, name:pattern, or just pattern
        Matcher named = NAMED.matcher(route.content());
        if (named.matches()) {
            String name = named.group("name");
            String names = named.group("names");
            String pattern = named.group("pattern");
            if (!Strings.isNullOrEmpty(pattern)) {
                // name:pattern -- match and set/replace metadata for name
                Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher m = p.matcher(new String(in.preview()));
                if (m.matches()) {
                    namedMetadata.put(name, new RoutableContent.ContentMetadata(m));
                } else {
                    namedMetadata.put(name, null);
                }
            }
            // name: -- lookup metadata associated with name (null means no match)
            metadata = namedMetadata.get(name);
            // name,names...: -- merge in other named lookups
            if (!Strings.isNullOrEmpty(names)) {
                if (metadata != null) {
                    metadata = new ContentMetadata(metadata); // make a copy to merge into
                }
                for (String from : names.split("\\s*,\\s*")) {
                    if (!Strings.isNullOrEmpty(from)) {
                        ContentMetadata fromMeta = namedMetadata.get(from);
                        if (fromMeta != null) {
                            if (metadata != null) {
                                metadata.merge(fromMeta);
                            } else {
                                metadata = new ContentMetadata(fromMeta);
                            }
                        }
                    }
                }
            }
            // return result
            if (metadata != null) {
                return metadata.matches(route);
            } else {
                return false;
            }
        }
        // ok, no name, so do a straight match against pattern
        Pattern p = Pattern.compile(route.content(), Pattern.DOTALL);
        Matcher m = p.matcher(new String(in.preview()));
        if (m.matches()) {
            metadata = new RoutableContent.ContentMetadata(m);
            return metadata.matches(route);
        } else {
            return false;
        }
    }

    public InputStream inputStream() {
        return in;
    }

    public static class ContentMetadata extends Metadata {
        /**
         * Constructs a new {@code EDIMetadata} object based on
         * a non-EDI parse represented in a matched {@link Matcher}.
         * The following named capture groups are supported (note
         * that the names are case sensitive):
         * <ul><li>sender</li>
         *     <li>receiver</li>
         *     <li>groupSender</li>
         *     <li>groupReceiver</li>
         *     <li>function</li>
         *     <li>type</li>
         *     <li>icn</li></ul>
         * @param m the matched {@link Matcher}.
         */
        public ContentMetadata(Matcher m) {
            sender.id(group(m, "sender"));
            receiver.id(group(m, "receiver"));
            groupSender.id(group(m, "groupSender"));
            groupReceiver.id(group(m, "groupReceiver"));
            function = group(m, "function");
            type = group(m, "type");
            icn = group(m, "icn");
        }
    
        /**
         * Copy constructor.
         * @param copy the ContentMetadata to copy
         */
        public ContentMetadata(ContentMetadata copy) {
            sender = new EDIID(copy.sender);
            receiver = new EDIID(copy.receiver);
            groupSender = new EDIID(copy.groupSender);
            groupReceiver = new EDIID(copy.groupReceiver);
            function = copy.function;
            type = copy.type;
            icn = copy.icn;
        }

        /**
         * Merges a ContentMetadata object over this object, giving
         * precedence to non-null/empty values in the {@code from} object.
         * @param from the source to merge from
         * @return {@code this}
         */
        public ContentMetadata merge(ContentMetadata from) {
            if (!Strings.isNullOrEmpty(from.sender.id())) sender.id(from.sender.id());
            if (!Strings.isNullOrEmpty(from.sender.qualifier())) sender.qualifier(from.sender.qualifier());
            if (!Strings.isNullOrEmpty(from.receiver.id())) receiver.id(from.receiver.id());
            if (!Strings.isNullOrEmpty(from.receiver.qualifier())) receiver.qualifier(from.receiver.qualifier());
            if (!Strings.isNullOrEmpty(from.groupSender.id())) groupSender.id(from.groupSender.id());
            if (!Strings.isNullOrEmpty(from.groupSender.qualifier())) groupSender.qualifier(from.groupSender.qualifier());
            if (!Strings.isNullOrEmpty(from.groupReceiver.id())) groupReceiver.id(from.groupReceiver.id());
            if (!Strings.isNullOrEmpty(from.groupReceiver.qualifier())) groupReceiver.qualifier(from.groupReceiver.qualifier());
            if (!Strings.isNullOrEmpty(from.function())) function = from.function();
            if (!Strings.isNullOrEmpty(from.type())) type = from.type();
            if (!Strings.isNullOrEmpty(from.icn())) icn = from.icn();
            return this;
        }

       /**
        * Attempts to extract the value of named capture group
        * {@code name} from a {@link Matcher}, returning {@code null}
        * instead of throwing an exception in case the named group
        * is not defined in the underlying {@link Pattern}.
        * @param m the matched {@link Matcher}
        * @param name the capture group name
        * @return the captured text, or {@code null} if the group was not defined
        */
       private static String group(Matcher m, String name) {
           try {
               return m.group(name);
           } catch (Exception e) {
               return null;
           }
       }
       /**
        * Content Metadata matches only when the {@code Content} is not empty.
        */
       @Override
       public boolean matches(Route route) {
           return !Strings.isNullOrEmpty(route.content()) && super.matches(route);
       }
    }
}