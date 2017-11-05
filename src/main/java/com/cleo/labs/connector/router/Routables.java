package com.cleo.labs.connector.router;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.IntStream;

public class Routables implements Iterable<Routables.Routable> {

    public interface Routable {
        public boolean matches(Route route);
        public Metadata metadata();
        public InputStream inputStream();
    }

    private Iterator<Routable> iterator = null;

    public Routables(InputStream in, int previewSize) {
        this.iterator = Collections.emptyIterator(); // in case we fall through on error
        try {
            PreviewInputStream preview = new PreviewInputStream(in,
                    IntStream.of(previewSize, RoutableEDI.PREVIEW_SIZE, RoutableHL7.PREVIEW_SIZE).max().getAsInt());
            if (RoutableEDI.canRoute(preview)) {
                this.iterator = RoutableEDI.getIterator(preview);
            } else if (RoutableHL7.canRoute(preview)) {
                this.iterator = RoutableHL7.getIterator(preview);
            } else if (RoutableContent.canRoute(preview)) {
                this.iterator = RoutableContent.getIterator(preview);
            } else {
                // fall through
            }
        } catch (IOException e) {
            // fall through
        }
    }


    /**
     * On the first invocation, returns the appropriate Iterator over
     * the InputStream.  On subsequent invocations, returns an
     * EmptyIterator.
     */
    @Override
    public Iterator<Routable> iterator() {
        Iterator<Routable> result = iterator;
        iterator = Collections.emptyIterator();
        return result;
    }
}