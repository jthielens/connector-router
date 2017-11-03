package com.cleo.labs.connector.router;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
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

    public RoutableContent(PreviewInputStream preview) {
        this.in = preview;
    }

    public Metadata metadata() {
        return metadata;
    }

    public boolean matches(Route route) {
        if (route.content() != null) {
            Pattern p = Pattern.compile(route.content(), Pattern.DOTALL);
            Matcher m = p.matcher(new String(in.preview()));
            if (m.matches()) {
                metadata = new RoutableContent.ContentMetadata(m);
                return metadata.matches(route);
            } else {
                return false;
            }
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