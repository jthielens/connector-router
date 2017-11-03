package com.cleo.labs.connector.router;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;

import com.google.common.io.ByteStreams;

public class PreviewInputStream extends FilterInputStream {

    private byte[] buf;

    protected PreviewInputStream(InputStream in, int size) throws IOException {
        super(null);
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

}