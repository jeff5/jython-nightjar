/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

// we probably should use StringBuffer instead of StringBuilder, since StringIO
// most likely means not thread-confined, so we want those locking semantics.

package org.python.modules;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * This module implements a file-like class, StringIO, that reads and
 * writes a string buffer (also known as memory files).
 * See the description on file objects for operations.
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version cStringIO.java,v 1.10 1999/05/20 18:03:20 fb Exp
 */
public class cStringIO {
    /**
     * Create an empty StringIO object
     * @return          a new StringIO object.
     */
    
    // would be nicer if we directly imported from os, but crazy to do so
    // since in python code itself
    private class os {
        public static final int SEEK_SET = 0;
        public static final int SEEK_CUR = 1;
        public static final int SEEK_END = 2;
    }
    
    public static StringIO StringIO() {
        return new StringIO();
    }

    /**
     * Create a StringIO object, initialized by the value.
     * @param buf       The initial value.
     * @return          a new StringIO object.
     */
    public static StringIO StringIO(String buffer) {
        return new StringIO(buffer);
    }


    /**
     * The StringIO object
     * @see cStringIO#StringIO()
     * @see cStringIO#StringIO(String)
     */
    public static class StringIO extends PyIterator {
        public boolean softspace = false;
        public boolean closed = false;
        public int pos = 0;

        private final StringBuffer buf;

        StringIO() {
            buf = new StringBuffer();
        }


        StringIO(String buffer) {
            buf = new StringBuffer(buffer);
        }

        private void _complain_ifclosed() {
            if (closed)
                throw Py.ValueError("I/O operation on closed file");
        }

        public void __setattr__(String name, PyObject value) {
            if (name == "softspace") {
                softspace = value.__nonzero__();
                return;
            }
            super.__setattr__(name, value);
        }

        public PyObject __iternext__() {
            _complain_ifclosed();
            String r = readline();
            if(r.equals(""))
                return null;
            return new PyString(r);
        }

        /**
         * Free the memory buffer.
         */
        public void close() {
            closed = true;
        }


        /**
         * Return false.
         * @return      false.
         */
        public boolean isatty() {
            _complain_ifclosed();
            return false;
        }


        /**
         * Position the file pointer to the absolute position.
         * @param       pos the position in the file.
         */
        public void seek(int pos) {
            seek(pos, os.SEEK_SET);
        }


        /**
         * Position the file pointer to the position in the .
         * @param       pos the position in the file.
         * @param       mode; 0=from the start, 1=relative, 2=from the end.
         */
        public void seek(int pos, int mode) {
            _complain_ifclosed();
            switch (mode) {
                case os.SEEK_CUR:
                    this.pos += pos;
                    break;
                case os.SEEK_END:
                    this.pos = pos + buf.length();
                    break;
                case os.SEEK_SET:
                default:
                    this.pos = pos;
                    break;
            }
        }

        /**
         * Reset the file position to the beginning of the file.
         */
        public void reset() {
            pos = 0;
        }

        /**
         * Return the file position.
         * @returns     the position in the file.
         */
        public long tell() {
            _complain_ifclosed();
            return pos;
        }



        /**
         * Read all data until EOF is reached.
         * An empty string is returned when EOF is encountered immediately.
         * @returns     A string containing the data.
         */
        public String read() {
            return read(-1);
        }


        /**
         * Read at most size bytes from the file (less if the read hits EOF).
         * If the size argument is negative, read all data until EOF is
         * reached. An empty string is returned when EOF is encountered
         * immediately.
         * @param size  the number of characters to read.
         * @returns     A string containing the data read.
         */
        public String read(int size) {
            _complain_ifclosed();
            int len = buf.length();
            String substr;
            if (size < 0) {
                substr = pos >= len ? "" : buf.substring(pos);
                pos = len;
            } else {
                int newpos = Math.min(pos + size, len);
                substr = buf.substring(pos, newpos);
                pos = newpos;
            }
            return substr;
        }


        /**
         * Read one entire line from the file. A trailing newline character
         * is kept in the string (but may be absent when a file ends with
         * an incomplete line).
         * An empty string is returned when EOF is hit immediately.
         * @returns data from the file up to and including the newline.
         */
        public String readline() {
            return readline(-1);
        }


        /**
         * Read one entire line from the file. A trailing newline character
         * is kept in the string (but may be absent when a file ends with an
         * incomplete line).
         * If the size argument is non-negative, it is a maximum byte count
         * (including the trailing newline) and an incomplete line may be
         * returned.
         * @returns data from the file up to and including the newline.
         */
        public String readline(int size) {
            _complain_ifclosed();
            int len = buf.length();
            if (pos == len) {
                return "";
            }
            int i = buf.indexOf("\n", pos);
            int newpos = (i < 0) ? len : i + 1;
            if (size >= 0) {
                newpos = Math.min(newpos - pos, size) + pos;
            }
            String r = buf.substring(pos, newpos);
            pos = newpos;
            return (i < 0 && size <= 0) ? r + "\n" : r;
        }


        /**
         * Read and return a line without the trailing newline.
         * Usind by cPickle as an optimization.
         */
        public String readlineNoNl() {
            _complain_ifclosed();
            int len = buf.length();
            int i = buf.indexOf("\n", pos);
            int newpos = (i < 0) ? len : i;
            String r = buf.substring(pos, newpos);
            pos = newpos;
            if (pos  < len) // Skip the newline
                pos++;
            return r;
        }



        /**
         * Read until EOF using readline() and return a list containing
         * the lines thus read.
         * @return      a list of the lines.
         */
        public PyObject readlines() {
            return readlines(0);
        }


        /**
         * Read until EOF using readline() and return a list containing
         * the lines thus read.
         * @return      a list of the lines.
         */
        public PyObject readlines(int sizehint) {
            _complain_ifclosed();
            int total = 0;
            PyList lines = new PyList();
            String line = readline();
            while (line.length() > 0) {
                lines.append(new PyString(line));
                total += line.length();
                if (0 < sizehint  && sizehint <= total)
                    break;
                line = readline();
            }
            return lines;
        }

        /**
         * truncate the file at the current position.
         */
        public void truncate() {
            truncate(-1);
        }

        /**
         * truncate the file at the position pos.
         */
        public void truncate(int pos) {
            if (pos < 0)
                pos = this.pos;
            buf.setLength(pos);
            this.pos = pos;
        }


        /**
         * Write a string to the file.
         * @param s     The data to write.
         */
        public void write(PyObject obj) {
            write(obj.toString());
        }

        public void write(String s) {
            _complain_ifclosed();
            buf.setLength(pos);
            int newpos = pos + s.length();
            buf.replace(pos, newpos, s);
            pos = newpos;
        }

        /**
         * Write a char to the file. Used by cPickle as an optimization.
         * @param ch    The data to write.
         */
        public void writeChar(char ch) {
            int len = buf.length();
            if (len <= pos)
                buf.setLength(pos + 1);
            buf.setCharAt(pos++, ch);
        }


        /**
         * Write a list of strings to the file.
         */
        public void writelines(PyObject lines) {
            for (PyObject line : lines.asIterable()) {
                write(line);
            }
        }


        /**
         * Flush the internal buffer. Does nothing.
         */
        public void flush() {
            _complain_ifclosed();
        }


        /**
         * Retrieve the entire contents of the ``file'' at any time
         * before the StringIO object's close() method is called.
         * @return      the contents of the StringIO.
         */
        public String getvalue() {
            return buf.toString();
        }

    }

}
