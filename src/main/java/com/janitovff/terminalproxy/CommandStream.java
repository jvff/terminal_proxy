package com.janitovff.terminalproxy;

import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandStream {
    private Reader source;

    public CommandStream(InputStream stream) {
        source = new InputStreamReader(stream, UTF_8);
    }

    public char readChar() throws IOException {
        int data = source.read();

        if (data < 0)
            throw new EOFException();

        return (char)data;
    }

    public short readShort() throws IOException {
        return (short)readInt();
    }

    public int readInt() throws IOException {
        return Base64Reader.readIntFrom(source);
    }

    public String readString() throws IOException {
        int numberOfChars = readInt();
        char[] chars = readChars(numberOfChars);

        return new String(chars);
    }

    private char[] readChars(int numberOfChars) throws IOException {
        char[] chars = new char[numberOfChars];
        int charsRead = 0;

        while (charsRead < numberOfChars) {
            int offset = charsRead;
            int length = numberOfChars - charsRead;
            int charsReadInThisIteration = source.read(chars, offset, length);

            if (charsReadInThisIteration < 0)
                throw new EOFException("Failed to read a String");

            charsRead += charsReadInThisIteration;
        }

        return chars;
    }
}
