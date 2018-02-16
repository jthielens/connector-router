package com.cleo.labs.connector.router;

import java.io.File;
import java.io.OutputStream;

public interface RouterFileFactory {
    public File getFile(String filename);
    public OutputStream getOutputStream(String filename) throws Exception;
}
