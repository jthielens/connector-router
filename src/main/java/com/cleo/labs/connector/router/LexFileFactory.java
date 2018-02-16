package com.cleo.labs.connector.router;

import java.io.File;
import java.io.OutputStream;

import com.cleo.lexicom.beans.LexFile;
import com.cleo.lexicom.streams.LexFileOutputStream;

public class LexFileFactory implements RouterFileFactory {

    @Override
    public File getFile(String filename) {
        return new LexFile(filename);
    }

    @Override
    public OutputStream getOutputStream(String filename) throws Exception {
        return new LexFileOutputStream(new LexFile(filename));
    }

}
