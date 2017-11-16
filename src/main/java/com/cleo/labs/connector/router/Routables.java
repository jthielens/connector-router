package com.cleo.labs.connector.router;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * The Routables class analyzes an {@link InputStream} and produces an
 * Iterator over one or more {@link Routable} instances that might be
 * split out of it.  Only EDI streams will in fact be split: other content
 * types will produce a {@code Routable} singleton iterator representing
 * the entire InputStream.
 */
public class Routables implements Iterable<Routables.Routable> {

    /**
     * Wrapper around an {@link InputStream} including routing
     * metadata and a matching operator.
     */
    public interface Routable {
        /**
         * Implementations should return {@code true} if the metadata
         * parsed from the {@code InputStream} matches the {@link Route}.
         * @param route the {@link Route} to match
         * @return {@code true} if the route matches
         */
        public boolean matches(Route route);
        /**
         * An implementation should return the metadata parsed from
         * previewing the {@code InputStream}.
         * @return some {@link Metadata}
         */
        public Metadata metadata();
        /**
         * An implementation should return the underlying routable
         * {@code InputStream}, including the parts that may have been
         * previewed to extract metadata (using {@link PreviewInputStream}.
         * @return the routable {@link InputStream}
         */
        public InputStream inputStream();
    }

    private Iterator<Routable> iterator = null;

    /**
     * Previews an {@link InputStream} to determine its routability and
     * creates an appropriate iterator over the possibly split portions
     * of the file.  This iterator can be retrieved once by the {@link #iterator()}
     * method.
     * @param in the {@link InputStream} to analyze
     * @param previewSize the number of bytes to preview
     * @throws IOException
     */
    public Routables(InputStream in, int previewSize) throws IOException {
        this.iterator = Collections.emptyIterator(); // in case we fall through on error
        PreviewInputStream preview = new PreviewInputStream(in,
                IntStream.of(previewSize, RoutableEDI.PREVIEW_SIZE, RoutableHL7.PREVIEW_SIZE).max().getAsInt());
        if (RoutableEDI.canRoute(preview)) {
            this.iterator = RoutableEDI.getIterator(preview);
        } else if (RoutableHL7.canRoute(preview)) {
            this.iterator = RoutableHL7.getIterator(preview);
        } else if (RoutableContent.canRoute(preview)) {
            this.iterator = RoutableContent.getIterator(preview);
        } else {
            // fall through (although this can't currently happen since RoutableContent.canRoute() is always true
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