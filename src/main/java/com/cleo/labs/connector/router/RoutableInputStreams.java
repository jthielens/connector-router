package com.cleo.labs.connector.router;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cleo.lexicom.edi.EDI;
import com.cleo.lexicom.edi.EDIFilterInputStream;
import com.cleo.lexicom.edi.EDISegment;

public class RoutableInputStreams implements Iterable<RoutableInputStreams.RoutableInputStream> {

    public interface Routable {
        public boolean matches(Route route);
        public EDIMetadata metadata();
    }

    public static abstract class RoutableInputStream extends FilterInputStream implements Routable {
        public RoutableInputStream() {
            super(null);
        }
    }

    private enum Mode {PREVIEW, EDI, NONE};

    private Mode mode;
    private RoutableInputStream in;
    private EDI edi;

    public RoutableInputStreams(InputStream in, int previewSize) {
        try {
            EDIFilterInputStream ediin = new EDIFilterInputStream(in);
            this.edi = ediin.getEDI();
            if (edi.getType() == EDI.UNKNOWN) {
                mode = Mode.PREVIEW;
                this.in = new PreviewInputStream(ediin, previewSize);
            } else {
                mode = Mode.EDI;
            }
        } catch (IOException e) {
            mode = Mode.NONE;
        }
    }

    public EDIFilter getEDIFilter() {
        try {
            return this.new EDIFilter();
        } catch (IOException e) {
            return null; // no more interchanges (usually)
        }
    }

    public class EDIFilter extends RoutableInputStream {
        private ByteBuffer buffer;
        private boolean lastSegment = false;
        private List<EDISegment> preview;
        private EDIMetadata metadata;

        public EDIFilter() throws IOException {
            super();
            metadata = new EDIMetadata(edi);
            preview = new ArrayList<>();
            while (!metadata.typed()) {
                EDISegment segment = edi.getNextSegment();
                if (segment == null) {
                    throw new EOFException("EDI syntax error: incomplete envelope");
                }
                metadata.process(segment);
                preview.add(segment);
            }
            load();
        }

        public EDIMetadata metadata() {
            return metadata;
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
    }

    public class InputStreamIterator implements Iterator<RoutableInputStream> {
        private EDIFilter edifilter = null;

        @Override
        public boolean hasNext() {
            if (mode == Mode.EDI) {
                // pre-load the next EDI stream if there is one
                if (edifilter == null) {
                    edifilter = getEDIFilter();
                    if (edifilter == null) {
                        mode = Mode.NONE;
                    }
                }
            }
            return mode != Mode.NONE;
        }

        @Override
        public RoutableInputStream next() {
            switch (mode) {
            case NONE:
                return null;
            case PREVIEW:
                mode = Mode.NONE;
                return in;
            case EDI:
                hasNext(); // force a load if hasNext() was not called
                EDIFilter result = edifilter;
                edifilter = null;
                return result;
            default:
            }
            return null;
        }
    }

    @Override
    public Iterator<RoutableInputStream> iterator() {
        return this.new InputStreamIterator();
    }
}
