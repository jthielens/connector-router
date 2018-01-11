package com.cleo.labs.connector.router;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cleo.labs.connector.router.Routables.Routable;
import com.cleo.lexicom.edi.EDI;
import com.cleo.lexicom.edi.EDIElement;
import com.cleo.lexicom.edi.EDISegment;
import com.google.common.base.Strings;

public class RoutableEDI extends InputStream implements Routable {
    private EDI edi;
    private ByteBuffer buffer;
    private boolean lastSegment = false;
    private List<EDISegment> preview;
    private EDIMetadata metadata;

    public static int PREVIEW_SIZE = 4 * 1024;

    public static boolean canRoute(PreviewInputStream preview) {
        try {
            EDI tester = new EDI(new ByteArrayInputStream(preview.preview()));
            switch (tester.getType()) {
            case EDI.X12:
            case EDI.EDIFACT:
            case EDI.TRADACOMS:
                return true;
            default:
                // fall through
            }
        } catch (IOException e) {
            // guess it isn't EDI
        }
        return false;
    }

    public static Iterator<Routable> getIterator(PreviewInputStream preview) {
        return new RoutableEDIIterator(preview);
    }

    public static class RoutableEDIIterator implements Iterator<Routable> {
        private EDI edi;
        private RoutableEDI nextfilter;
        private boolean done;

        private void step() {
            try {
                this.nextfilter = new RoutableEDI(edi);
            } catch (EOFException e) {
                this.nextfilter = null;
            } catch (IOException e) {
                this.nextfilter = null;
                throw new IllegalArgumentException("EDI Syntax Error", e);
            }
            done = nextfilter == null;
        }

        public RoutableEDIIterator(PreviewInputStream preview) {
            try {
                this.nextfilter = null;
                this.done = false;
                this.edi = new EDI(preview);
            } catch (IOException e) {
                this.edi = null;
                this.nextfilter = null;
                this.done = true;
            }
        }

        @Override
        public boolean hasNext() {
            if (nextfilter == null && !done) {
                step();
            }
            return nextfilter != null;
        }

        @Override
        public Routable next() {
            hasNext(); // force step() in case hasNext() was not called
            RoutableEDI result = nextfilter;
            nextfilter = null;
            return result;
        }
    }

    public RoutableEDI(EDI edi) throws IOException {
        super();
        this.edi = edi;
        this.metadata = EDIMetadata.getEDIMetadata(edi);
        if (this.metadata == null) {
            throw new IOException("EDI syntax error: no envelope found");
        }
        this.preview = new ArrayList<>();
        while (!metadata.typed()) {
            EDISegment segment = edi.getNextSegment();
            if (segment == null) {
                if (preview.isEmpty()) {
                    throw new EOFException();
                } else {
                    throw new IOException("EDI syntax error: incomplete envelope");
                }
            }
            metadata.process(segment);
            preview.add(segment);
        }
        load();
    }

    public Metadata metadata() {
        return metadata;
    }

    @Override
    public InputStream inputStream() {
        return this;
    }

    @Override
    public boolean matches(Route route) {
        return metadata.matches(route);
    }

    private void load() throws IOException {
        if (lastSegment) {
            buffer = null;
        } else if (!preview.isEmpty()) {
            EDISegment segment = preview.remove(0);
            buffer = ByteBuffer.wrap(segment.getSegment());
        } else {
            EDISegment segment = edi.getNextSegment();
            if (segment == null) {
                buffer = null;
            } else {
                buffer = ByteBuffer.wrap(segment.getSegment());
                lastSegment = metadata.isLastSegment(segment);
            }
        }
    }

    @Override
    public int read() throws IOException {
        while (buffer != null && !buffer.hasRemaining()) {
            load();
        }
        if (buffer==null) {
            return -1;
        } else {
            int value = buffer.get();
            return value & 0xff;
        }
    }
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = 0;
        while (read < len && buffer != null) {
            while (buffer != null && !buffer.hasRemaining()) {
                load();
            }
            if (buffer != null) {
                int chunk = Math.min(len-read, buffer.remaining());
                buffer.get(b, off+read, chunk);
                read += chunk;
            }
        }
        return read == 0 ? -1 : read;
    }
    @Override
    public int available() throws IOException {
        return buffer == null ? 0 : buffer.remaining();
    }
    @Override
    public boolean markSupported() {
        return false;
    }
    @Override
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (skipped < n && buffer != null && !buffer.hasRemaining()) {
            load();
            int chunk = (int)Math.min(n-skipped, (long)buffer.remaining());
            buffer.position(buffer.position()+chunk);
            skipped += chunk;
        }
        return skipped;
    }
    @Override
    public void close() throws IOException {
        // keep the EDI stream open
    }

    public static abstract class EDIMetadata extends Metadata {
        /**
         * Constructs a new {@code EDIMetadata} object based on
         * an {@code EDI} parsing object.
         * @param edi the {@code EDI} object
         * @throws IOException
         */
        public static EDIMetadata getEDIMetadata(EDI edi) {
            EDIMetadata result;
            switch (edi.getType()) {
            case EDI.X12:
                result = new X12Metadata();
                break;
            case EDI.EDIFACT:
                result = new EdifactMetadata();
                break;
            case EDI.TRADACOMS:
                result = new TradacomsMetadata();
                break;
            default:
                return null;
            }
            result.reset();
            return result;
        }

        /**
         * Use {@link #getEDIMetadata(EDI)} instead of the constructor.
         */
        private EDIMetadata() {
        }

        protected boolean typed;
        /**
         * Gets the {@code typed} flag indicating completion
         * of the envelope parsing.
         * @return {@code true} if all headers have been parsed
         */
        public boolean typed() {
            return typed;
        }
        /**
         * Process an EDI segment, dispatching the parsing to the syntax-specific
         * parsing routines above.
         * @param edisegment the {@code EDISegment} to parse
         */
        public abstract void process(EDISegment segment);
        /**
         * Return {@code true} if an {@code EDISegment} is the closing
         * segment of an interchange, depending on syntax.
         * @param segment the {@code EDISegment} to parse
         * @return {@code true} if the segment is a closing segment
         */
        public abstract boolean isLastSegment(EDISegment segment);
        /**
         * EDI Metadata matches only when the {@code Content} is empty.
         */
        @Override
        public boolean matches(Route route) {
            return Strings.isNullOrEmpty(route.content()) && super.matches(route);
        }
        @Override
        public void reset() {
            this.typed = false;
            super.reset();
        }
        /**
         * Safely gets subelement 0 for element {@code index} from {@code segment},
         * returning {@code ""} if there are not that many elements.  If the
         * element has subelements, returns the first subelement.
         * avoiding an Exception.  Return values are {@link String#trim()}ed.
         * @param segment the {@code EDISegment} from which to extract the element
         * @param index which (0-relative) element to extract
         * @return the element, or {@code ""} if it does not exist
         */
        protected static String getSubelementOrNot(EDISegment segment, int index) {
            if (segment.getElementCount() > index) {
                EDIElement e = segment.getElement(index);
                if (e.getSubelementCount() > 0) {
                    return e.getSubelement(0).trim();
                } else {
                    return e.getElement().trim();
                }
            } else {
                return "";
            }
        }

        /**
         * Safely gets element {@code index} from {@code segment},
         * returning {@code ""} if there are not that many elements,
         * avoiding an Exception.  Return values are {@link String#trim()}ed.
         * @param segment the {@code EDISegment} from which to extract the element
         * @param index which (0-relative) element to extract
         * @return the element, or {@code ""} if it does not exist
         */
        protected static String getElement(EDISegment segment, int index) {
            try {
                return segment.getElement(index).getElement().trim();
            } catch (IndexOutOfBoundsException e) {
                return "";
            }
        }
    }
    private static class X12Metadata extends EDIMetadata {
        /**
         * Parses X12 {@code ISA}, {@code GS}, and {@code ST},
         * marking processing {@code typed} after the {@code ST}.
         */
        @Override
        public void process(EDISegment segment) {
            if (segment.getName().equals("ISA")) { // interchange header
                sender.id(getElement(segment, 5))
                        .qualifier(getElement(segment, 4));
                receiver.id(getElement(segment, 7))
                        .qualifier(getElement(segment, 6));
                icn = getElement(segment, 12);
            } else if (segment.getName().equals("GS")) { // functional group header
                function = getElement(segment, 0);
                groupSender.id(getElement(segment, 1));
                groupReceiver.id(getElement(segment, 2));
            } else if (segment.getName().equals("ST")) { // transaction set header
                type = getElement(segment, 0);
                typed = true;
            }
            
        }
        /**
         * {@code IEA} ends an X12 Interchange.
         */
        @Override
        public boolean isLastSegment(EDISegment segment) {
            return segment.getName().equals("IEA");
        }
    }
    private static class EdifactMetadata extends EDIMetadata {
        /**
         * Parses Edifact {@code UNB}, {@code UNG}, and {@code UNH},
         * marking processing {@code typed} after the {@code UNH}.
         */
        @Override
        public void process(EDISegment segment) {
            if (segment.getName().startsWith("UNA")) { // optional data delimiter header
            } else if (segment.getName().equals("UNB")) { // interchange header
                sender.fromEdifact(segment, 1);
                receiver.fromEdifact(segment, 2);
                icn = getSubelementOrNot(segment, 4);
            } else if (segment.getName().equals("UNG")) { // functional group header
                function = getSubelementOrNot(segment, 0);
                groupSender.fromEdifact(segment, 1);
                groupReceiver.fromEdifact(segment, 2);
            } else if (segment.getName().equals("UNH")) { // message header
                type = getSubelementOrNot(segment, 1);
                typed = true;
            }
        }
        /**
         * {@code UNZ} ends an Edifact Interchange.
         */
        @Override
        public boolean isLastSegment(EDISegment segment) {
            return segment.getName().equals("UNZ");
        }
    }
    private static class TradacomsMetadata extends EDIMetadata {
        /**
         * Parses Tradacoms {@code STX} and {@code MHD},
         * marking processing {@code typed} after the {@code MHD}.
         */
        @Override
        public void process(EDISegment segment) {
            if (segment.getName().equals("STX")) { // interchange header
                sender.id(getSubelementOrNot(segment, 1));
                receiver.id(getSubelementOrNot(segment, 2));
                icn = getSubelementOrNot(segment, 4);
            } else if (segment.getName().equals("BAT")) { // functional group header
            } else if (segment.getName().equals("MHD")) { // message header
                type = getSubelementOrNot(segment, 1);
                typed = true;
            }
        }
        /**
         * {@code END} ends a Tradacoms Interchange.
         */
        @Override
        public boolean isLastSegment(EDISegment segment) {
            return segment.getName().equals("END");
        }
    }
}