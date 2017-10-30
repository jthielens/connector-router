package com.cleo.labs.connector.router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.ByteStreams;

public class PreviewInputStream extends RoutableInputStreams.RoutableInputStream {

    private byte[] buf;
    private EDIMetadata metadata;

    protected PreviewInputStream(InputStream in, int size) throws IOException {
        super();
        buf = new byte[size];
        int count = ByteStreams.read(in, buf, 0, size);
        if (count < size) {
            buf = Arrays.copyOf(buf, count);
        }
        InputStream bis = new ByteArrayInputStream(buf, 0, count);
        this.in = new SequenceInputStream(bis, in);
    }

    public byte[] preview() {
        return buf;
    }

    public EDIMetadata metadata() {
        return metadata;
    }

    public boolean matches(Route route) {
        if (route.content() != null) {
            Pattern p = Pattern.compile(route.content(), Pattern.DOTALL);
            Matcher m = p.matcher(new String(buf));
            if (m.matches()) {
                metadata = new EDIMetadata(m);
                return metadata.matches(route);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}