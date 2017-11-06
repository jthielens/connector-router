package com.cleo.labs.connector.router;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.cleo.connector.api.helper.Logger;

/**
 * Router file attribute views
 */
public class RouterFileAttributes implements DosFileAttributes, DosFileAttributeView {
    private Logger logger;
    private FileTime now = FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS);

    public RouterFileAttributes(Logger logger) {
        this.logger = logger;
    }

    @Override
    public FileTime lastModifiedTime() {
        logger.debug(String.format("lastModifiderTime()=%s", now.toString()));
        return now;
    }

    @Override
    public FileTime lastAccessTime() {
        logger.debug(String.format("lastAccessTime()=%s", now.toString()));
        return now;
    }

    @Override
    public FileTime creationTime() {
        logger.debug(String.format("creationTime()=%s", now.toString()));
        return now;
    }

    @Override
    public boolean isRegularFile() {
        logger.debug("isRegularFile()=true");
        return true; // pretend the router file is a plain file
    }

    @Override
    public boolean isDirectory() {
        logger.debug("isDirectory()=false");
        return false; // pretend the router file is a plain file
    }

    @Override
    public boolean isSymbolicLink() {
        logger.debug("isSymbolicLink()=false");
        return false; // pretend the router file is a plain file
    }

    @Override
    public boolean isOther() {
        logger.debug("isOther()=false");
        return false; // pretend the router file is a plain file
    }

    @Override
    public long size() {
        logger.debug("size()=0L");
        return 0L;
    }

    @Override
    public Object fileKey() {
        logger.debug("fileKey()=null");
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
        return "routerfile";
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
        logger.debug("isReadOnly()=false");
        return false;
    }

    @Override
    public boolean isHidden() {
        logger.debug("isHidden()=false");
        return false;
    }

    @Override
    public boolean isArchive() {
        logger.debug("isArchive()=false");
        return false;
    }

    @Override
    public boolean isSystem() {
        logger.debug("isSystem()=false");
        return false;
    }

}
