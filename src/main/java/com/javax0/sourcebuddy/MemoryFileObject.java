package com.javax0.sourcebuddy;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Date;

public class MemoryFileObject implements JavaFileObject {

    private final String name;
    private final ByteArrayOutputStream fileBytesContent = new ByteArrayOutputStream();

    public MemoryFileObject(final String name) {
        this.name = name;
    }

    @Override
    public URI toUri() {
        return URI.create("string:///" + name.replace('.', '/')
                + Kind.SOURCE.extension);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(fileBytesContent.toByteArray());
    }

    public byte[] getByteArray() {
        return fileBytesContent.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() {
        return fileBytesContent;
    }

    @Override
    public Reader openReader(final boolean ignoreEncodingErrors) {
        return null;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
        return null;
    }

    @Override
    public Writer openWriter() {
        return null;
    }

    @Override
    public long getLastModified() {
        return new Date().getTime();
    }

    @Override
    public boolean delete() {
        return true;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public boolean isNameCompatible(final String simpleName, final Kind kind) {
        return true;
    }

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return Modifier.PUBLIC;
    }


}
