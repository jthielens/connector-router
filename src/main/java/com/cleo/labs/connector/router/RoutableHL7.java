package com.cleo.labs.connector.router;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.cleo.labs.connector.router.Routables.Routable;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;

public class RoutableHL7 implements Routables.Routable {

    public static int PREVIEW_SIZE = 2 * 1024;

    public static class MSH {
        private static final String MINIMAL = "MSH|^~\\&||||||||||2.";
        private static final int ITEMS = 12; // how many items to isolate for analysis

        String fieldSeparator;
        String componentSeparator;
        String repetitionSeparator;
        String escapeCharacter;
        String subcomponentSeparator;
        int length;
        byte[] buf;
        int[] offsets;
        int[] lengths;
        boolean valid;

        private boolean inspect() {
            int length = Bytes.indexOf(buf, (byte)0x0d);
            if (length < MINIMAL.length()) {
                return false; // not nearly long enough
            }
            // MSH|
            if (buf[0]!='M' || buf[1]!='S' || buf[2]!='H') {
                return false; // does not start with MSH
            }
            byte fieldSeparatorByte = buf[3];
            offsets[1] = 3;
            lengths[1] = 1;
            // ^~\&
            byte componentSeparatorByte = buf[4];
            byte repetitionSeparatorByte = buf[5];
            byte escapeCharacterByte = buf[6];
            byte subcomponentSeparatorByte = buf[7];
            if (Sets.newHashSet(fieldSeparatorByte,
                    componentSeparatorByte,
                    repetitionSeparatorByte,
                    escapeCharacterByte,
                    subcomponentSeparatorByte).size() != 5) {
                return false; // separators must be unique
            }
            if (buf[8] != fieldSeparatorByte) {
                return false; // MSH-02 Encoding characters must end with fieldSeparator
            }
            offsets[2] = 4;
            lengths[2] = 4;
            // now isolate up to ITEMS
            int item = 3; // we are now at item MSH-03
            int i = 9; // at buffer index 9
            while (i < length && item <= ITEMS) {
                if (buf[i]==fieldSeparatorByte) {
                    offsets[item] = offsets[item-1]+lengths[item-1]+1;
                    lengths[item] = i-offsets[item];
                    item++;
                }
                i++;
            }
            if (item < ITEMS) {
                return false; // did not find at least ITEMS items before CR
            }
            fieldSeparator = new String(new byte[]{fieldSeparatorByte}, Charsets.UTF_8);
            componentSeparator = new String(new byte[]{componentSeparatorByte}, Charsets.UTF_8);
            repetitionSeparator = new String(new byte[]{repetitionSeparatorByte}, Charsets.UTF_8);
            escapeCharacter = new String(new byte[]{escapeCharacterByte}, Charsets.UTF_8);
            subcomponentSeparator = new String(new byte[]{subcomponentSeparatorByte}, Charsets.UTF_8);
            return true; // best guess
            
        }

        public boolean valid() {
            return valid;
        }

        public String unescape(String input) {
            Pattern escapes = Pattern.compile(Pattern.quote(escapeCharacter)+"([TSFRE]|X(?:[0-9a-fA-F]{2})+)"+Pattern.quote(escapeCharacter));
            Matcher m = escapes.matcher(input);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String escape = m.group(1);
                String replacement;
                if (escape.startsWith("X")) {
                    replacement = new String(BaseEncoding.base16().decode(escape.substring(1).toUpperCase()), Charsets.UTF_8);
                } else {
                    switch (escape) {
                    case "T":
                        replacement = subcomponentSeparator;
                        break;
                    case "S":
                        replacement = componentSeparator;
                        break;
                    case "F":
                        replacement = fieldSeparator;
                        break;
                    case "R":
                        replacement = repetitionSeparator;
                        break;
                    case "E":
                        replacement = escapeCharacter;
                        break;
                    default:
                        replacement = "";
                    }
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            return sb.toString();
 
        }

        private String rawItem(int i) {
            if (valid && i>0 && i<=ITEMS) {
                return new String(buf, offsets[i], lengths[i], Charsets.UTF_8);
            }
            return "";
        }
        public String item(int i) {
            return unescape(rawItem(i));
        }

        public String[] subitems(int i) {
            return Stream.of(rawItem(i).split(Pattern.quote(componentSeparator)))
                    .map((s) -> unescape(s))
                    .toArray(String[]::new);
        }

        public MSH(byte[] buf) {
            this.buf = buf;
            this.offsets = new int[ITEMS+1]; // we won't use [0]
            this.lengths = new int[ITEMS+1]; // we won't use [0]
            valid = inspect();
        }
    }

    /**
     * Analyzes the preview buffer of a {@link PreviewInputStream} to see
     * if it "looks like" HL7 version 2.  To reliably detect HL7, the preview
     * buffer should be at least {@link #PREVIEW_SIZE} long.
     * @param preview the {@link PreviewInputStream} to inspect
     * @return {@code true} if it "looks like" HL7 version 2
     */
    public static boolean canRoute(PreviewInputStream preview) {
        MSH msh = new MSH(preview.preview());
        return msh.item(12).startsWith("2.");
    }

    public static Iterator<Routable> getIterator(PreviewInputStream preview) {
        return Collections.singleton((Routable)new RoutableHL7(preview)).iterator();
    }

    private PreviewInputStream in;
    private RoutableHL7.HL7Metadata metadata;

    public RoutableHL7(PreviewInputStream preview) {
        this.in = preview;
        this.metadata = new HL7Metadata(new MSH(preview.preview()));
    }

    public Metadata metadata() {
        return metadata;
    }

    public boolean matches(Route route) {
        return metadata.matches(route);
    }

    public InputStream inputStream() {
        return in;
    }

    public static class HL7Metadata extends Metadata {
        /**
         * Parses an HL7 Hierarchic Designator into an EDIID.
         * @param id the id to set
         * @param subitems the parsed subitems
         */
        private void setHD(EDIID id, String[] subitems) {
            if (subitems.length > 2) {
                id.id(subitems[1]);
                id.qualifier(subitems[2]);
            } else if (subitems.length == 2) {
                id.id(subitems[0]);
                id.qualifier(subitems[1]);
            } else {
                id.id(subitems[0]);
            }
        }
        /**
         * Constructs a new {@code EDIMetadata} object based on
         * items extracted from the HL7 MSH header.
         * @param msh the parsed HL7 MSH header
         */
        public HL7Metadata(MSH msh) {
            super();
            setHD(sender, msh.subitems(4));
            setHD(receiver, msh.subitems(6));
            setHD(groupSender, msh.subitems(3));
            setHD(groupReceiver, msh.subitems(5));
            String[] msh9 = msh.subitems(9);
            type = msh9[0];
            function = msh9.length > 1 ? msh9[1] : "";
            icn = msh.item(10);
        }
    
       /**
        * HL7 Metadata matches only when the {@code Content} is empty.
        */
       @Override
       public boolean matches(Route route) {
           return Strings.isNullOrEmpty(route.content()) && super.matches(route);
       }
    }
}