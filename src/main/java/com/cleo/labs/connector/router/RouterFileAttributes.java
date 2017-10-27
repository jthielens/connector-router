package com.cleo.labs.connector.router;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Router file attribute views
 */
public class RouterFileAttributes implements DosFileAttributes, DosFileAttributeView {
    FileTime now = FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS);

    public RouterFileAttributes() {
    }

    @Override
    public FileTime lastModifiedTime() {
        return now;
    }

    @Override
    public FileTime lastAccessTime() {
        return now;
    }

    @Override
    public FileTime creationTime() {
        return now;
    }

    @Override
    public boolean isRegularFile() {
        return false; // pretend the .zip file is a directory containing the ZipEntrys
    }

    @Override
    public boolean isDirectory() {
        return true; // pretend the .zip file is a directory containing the ZipEntrys
    }

    @Override
    public boolean isSymbolicLink() {
        return false; // pretend the .zip file is a directory containing the ZipEntrys
    }

    @Override
    public boolean isOther() {
        return false; // pretend the .zip file is a directory containing the ZipEntrys
    }

    @Override
    public long size() {
        return 0L;
    }

    @Override
    public Object fileKey() {
        return null;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        if (lastModifiedTime != null || lastAccessTime != null || createTime != null) {
            throw new UnsupportedOperationException("setTimes() not supported for Router");
        }
    }

    @Override
    public String name() {
        return "zipfile";
    }

    @Override
    public DosFileAttributes readAttributes() throws IOException {
        return this;
    }

    @Override
    public void setReadOnly(boolean value) throws IOException {
        throw new UnsupportedOperationException("setHidden() not supported for Router");
    }

    @Override
    public void setHidden(boolean value) throws IOException {
        throw new UnsupportedOperationException("setHidden() not supported for Router");
    }

    @Override
    public void setSystem(boolean value) throws IOException {
        throw new UnsupportedOperationException("setSystem() not supported for Router");
    }

    @Override
    public void setArchive(boolean value) throws IOException {
        throw new UnsupportedOperationException("setArchive() not supported for Router");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isArchive() {
        return false;
    }

    @Override
    public boolean isSystem() {
        return false;
    }

}
