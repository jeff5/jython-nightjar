package org.python.core;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Base class for Jython bytearray (and bytes in due course) that provides most of the Java API,
 * including Java List behaviour. Attempts to modify the contents through this API will throw a
 * TypeError if the actual type of the object is not mutable.
 * <p>
 * It is possible for a Java client to treat this class as a <tt>List&lt;PyInteger></tt>, obtaining
 * equivalent functionality to the Python interface in a Java paradigm. The reason {@link }
 * <p>
 * Subclasses must define (from {@link PySequence}):
 * <ul>
 * <li>{@link #getslice(int, int, int)}</li>
 * <li>{@link #repeat(int)}</li>
 * </ul>
 * each returning an appropriate concrete type. Mutable subclasses should override:
 * <ul>
 * <li>{@link #pyset(int, PyObject)}</li>
 * <li>{@link #setslice(int, int, int, PyObject)}</li>
 * <li>{@link #del(int)}</li>
 * <li>{@link #delRange(int, int)}</li>
 * </ul>
 * since the default implementations will otherwise throw an exception.
 */
public abstract class BaseBytes extends PySequence implements MemoryViewProtocol, List<PyInteger> {

    /**
     * Simple constructor of empty zero-length array of defined type.
     *
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type) {
        super(type);            // implicit setStorage( emptyStorage );
        setStorage(emptyStorage);
    }

    /**
     * Simple constructor of zero-filled array of defined size and type.
     *
     * @param size required
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type, int size) {
        super(type);
        newStorage(size);
    }

    /**
     * Construct byte array of defined type by copying values from int[].
     *
     * @param type explicit Jython type
     * @param value source of values (and size)
     */
    public BaseBytes(PyType type, int[] value) {
        super(type);
        int n = value.length;
        newStorage(n);
        for (int i = offset, j = 0; j < n; i++, j++) {
            storage[i] = byteCheck(value[j]);
        }
    }

    /**
     * Construct byte array of defined type by copying character values from a String. These values
     * have to be in the Python byte range 0 to 255.
     *
     * @param type explicit Jython type
     * @param value source of characters
     * @throws PyException if any value[i] > 255
     */
    protected BaseBytes(PyType type, String value) throws PyException {
        super(type);
        int n = value.length();
        newStorage(n);
        for (int i = offset, j = 0; j < n; j++) {
            storage[i++] = byteCheck(value.charAt(j));
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses. It
     * also permits shared storage between objects, which in general is unsafe if the storage is
     * subject to modification independent of the object now being created. Immutable types may
     * share storage (safely).
     *
     * @param storage byte array allocated by client
     * @param size number of bytes actually used
     * @param offset index of first byte used
     * @throws IllegalArgumentException if the range [offset:offset+size] is not within the array
     *             bounds of storage or size<0.
     */
    protected void setStorage(byte[] storage, int size, int offset) throws IllegalArgumentException {
        if (size < 0 || offset < 0 || offset + size > storage.length) {
            throw new IllegalArgumentException();
        } else {
            this.storage = storage;
            this.size = size;
            this.offset = offset;
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses in the
     * case where the storage should consist of the first part of the given array.
     *
     * @param storage byte array allocated by client
     * @param size number of bytes actually used
     * @throws IllegalArgumentException if the range [0:size] is not within the array bounds of
     *             storage.
     */
    protected void setStorage(byte[] storage, int size) throws IllegalArgumentException {
        if (size < 0 || size > storage.length) {
            throw new IllegalArgumentException();
        } else {
            this.storage = storage;
            this.size = size;
            this.offset = 0;
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses in the
     * case where the storage should consist of exactly the whole of the given array.
     *
     * @param storage byte array allocated by client
     */
    protected void setStorage(byte[] storage) {
        this.storage = storage;
        this.size = storage.length;
        this.offset = 0;
    }

    /*
     * ========================================================================================
     * Support for memoryview
     * ========================================================================================
     *
     * This is present in order to facilitate development of PyMemoryView which a full
     * implementation of bytearray would depend on, while at the same time a full implementation of
     * memoryview depends on bytearray.
     */
    /**
     * Get hold of a <code>memoryview</code> on the current byte array.
     *
     * @see MemoryViewProtocol#getMemoryView()
     */
    @Override
    public MemoryView getMemoryView() {
        if (mv == null) {
            mv = new MemoryViewImpl();
        }
        return mv;
    }

    private MemoryView mv;

    /**
     * All instances of BaseBytes have one dimension with stride one.
     */
    private static final PyTuple STRIDES = new PyTuple(Py.One);

    /**
     * Very simple MemoryView for one-dimensional byte array. This lacks any actual access to the
     * underlying storage as the interface is not presently defined.
     */
    private class MemoryViewImpl implements MemoryView {

        private final PyTuple SHAPE = new PyTuple(new PyInteger(storage.length));

        @Override
        public String get_format() {
            return "B";
        }

        @Override
        public int get_itemsize() {
            return 1;
        }

        @Override
        public PyTuple get_shape() {
            return SHAPE;
        }

        @Override
        public int get_ndim() {
            return 1;
        }

        @Override
        public PyTuple get_strides() {
            return STRIDES;
        }

        @Override
        public boolean get_readonly() {
            return true;
        }

    }

    /*
     * ========================================================================================
     * Support for construction and initialisation
     * ========================================================================================
     *
     * Methods here help subclasses set the initial state. They are designed with bytearray in mind,
     * but note that from Python 3, bytes() has the same set of calls and behaviours, although in
     * Peterson's "sort of backport" to Python 2.x, bytes is effectively an alias for str and it
     * shows.
     */

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * PyObject in subclasses.
     *
     * @see org.python.core.ByteArray#bytearray___init__(PyObject[], String[])
     * @see org.python.core.ByteArray#ByteArray(PyObject)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    protected void init(PyObject arg) {

        if (arg == null) {
            /*
             * bytearray() Construct a zero-length bytearray.
             */
            setStorage(emptyStorage);

        } else if (arg instanceof PyString) {
            /*
             * bytearray(string) Construct from a text string by default encoding and error policy.
             * Cases where encoding and error policy are specified explicitly are dealt with
             * elsewhere.
             */
            init((PyString)arg, (String)null, (String)null); // Casts select right init()

        } else if (arg.isIndex()) {
            /*
             * bytearray(int) Construct a zero-initialised bytearray of the given length.
             */
            init(arg.asIndex(Py.OverflowError)); // overflow if too big to be Java int

        } else if (arg instanceof BaseBytes) {
            /*
             * bytearray copy of bytearray (or bytes) -- do efficiently
             */
            init((BaseBytes)arg);

        } else if (arg instanceof MemoryViewProtocol) {
            /*
             * bytearray copy of object supporting Jython implementation of PEP 3118
             */
            init(((MemoryViewProtocol)arg).getMemoryView());

        } else {
            /*
             * The remaining alternative is an iterable returning (hopefully) right-sized ints. If
             * it isn't one, we get an exception about not being iterable, or about the values.
             */
            init(arg.asIterable());

        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from a
     * text string with the specified encoding in subclasses.
     *
     * @see #bytearray___init__(PyObject[], String[])
     * @see PyByteArray#PyByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    protected void init(PyString arg, PyObject encoding, PyObject errors) {
        String enc = encoding == null ? null : encoding.asString();
        String err = errors == null ? null : errors.asString();
        init(arg, enc, err);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from a
     * text string with the specified encoding in subclasses.
     *
     * @see #bytearray___init__(PyObject[], String[])
     * @see PyByteArray#PyByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding
     * @param errors name of optional errors policy
     */
    protected void init(PyString arg, String encoding, String errors) {
        // Jython encode emits a String (not byte[])
        String encoded = encode(arg, encoding, errors);
        newStorage(encoded.length());
        setBytes(0, encoded);
    }

    /**
     * Helper for {@linkplain #setslice(int, int, int, PyObject)}, for <code>__new__</code> and
     * <code>__init__</code> and the Java API constructor from a text string with the specified
     * encoding in subclasses. This method thinly wraps a call to the codecs module and deals with
     * checking for PyUnicode (where the encoding argument is mandatory).
     *
     * @see #ByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding
     * @param errors name of optional errors policy
     * @return encoded string
     * @throws PyException(TypeError) if the PyString is actually a PyUnicode and encoding is null
     */
    protected static String encode(PyString arg, String encoding, String errors) throws PyException {
        // Jython encode emits a String (not byte[])
        String encoded;

        if (arg instanceof PyUnicode) {
            if (encoding != null) {
                encoded = codecs.encode(arg, encoding, errors);
            } else {
                throw Py.TypeError("unicode argument without an encoding");
            }
        } else {
            if (encoding != null) {
                encoded = codecs.encode(arg, encoding, errors);
            } else {
                encoded = arg.getString();
            }
        }
        return encoded;
    }

    /**
     * Fill a defined section of a byte array by copying character values from a String. These
     * values have to be in the Python byte range 0 to 255.
     *
     * @param start index in this byte array at which the first character code lands
     * @param value source of characters
     * @throws PyException(ValueError) if any value[i] > 255
     */
    protected void setBytes(int start, String value) throws PyException {
        int n = value.length();
        int io = offset + start;
        for (int j = 0; j < n; j++) {
            storage[io++] = byteCheck(value.charAt(j));
        }
    }

    /**
     * Fill a strided slice of a byte array by copying character values from a String. These values
     * have to be in the Python byte range 0 to 255.
     *
     * @param start index in this byte array at which the first character code lands
     * @param value source of characters
     * @throws PyException(ValueError) if any value[i] > 255
     */
    protected void setBytes(int start, int step, String value) throws PyException {
        int n = value.length();
        int io = offset + start;
        for (int j = 0; j < n; j++) {
            storage[io] = byteCheck(value.charAt(j));
            io += step;
        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * int in subclasses. Construct zero-filled bytearray of specified size.
     *
     * @param n size of zero-filled array
     */
    protected void init(int n) {
        if (n < 0) {
            throw Py.ValueError("negative count");
        }
        newStorage(n);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * objects supporting the Jython implementation of PEP 3118 (memoryview) in subclasses.
     *
     * @param value a memoryview object consistent with the slice assignment
     * @throws PyException(NotImplementedError) until memoryview is properly supported
     * @throws PyException(TypeError) if the memoryview is not byte-oriented
     */
    protected void init(MemoryView value) throws PyException {
        // XXX Support memoryview once means of access to bytes is defined
        Py.NotImplementedError("memoryview not yet supported in bytearray");
        String format = value.get_format();
        boolean isBytes = format == null || "B".equals(format);
        if (value.get_ndim() != 1 || !isBytes) {
            Py.TypeError("memoryview value must be byte-oriented");
        } else {
            // Dimensions are given as a PyTuple (although only one)
            int len = value.get_shape().pyget(0).asInt();
            // XXX Access to memoryview bytes to go here
        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * bytearray or bytes in subclasses.
     *
     * @param source bytearray (or bytes) to copy
     */
    protected void init(BaseBytes source) {
        newStorage(source.size);
        System.arraycopy(source.storage, source.offset, storage, offset, size);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * an arbitrary iterable Python type in subclasses. This will include generators and lists.
     *
     * @param iter iterable source of values to enter in the array
     */
    protected void init(Iterable<? extends PyObject> iter) {
        /*
         * Different strategy is needed from that suited to "random append" operations. We shall
         * have a stream of append operations, and it might be long.
         */
        FragmentList fragList = new FragmentList();
        fragList.loadFrom(iter);

        // Now, aggregate all those fragments.
        //
        if (fragList.totalCount > 0) {

            if (fragList.size() == 1) {
                // Note that the first fragment is small: negligible waste if stolen directly.
                Fragment frag = fragList.getFirst();
                setStorage(frag.storage, frag.count);

            } else {
                // Stitch the fragments together in new storage of sufficient size
                newStorage(fragList.totalCount);
                fragList.emptyInto(storage, offset);
            }

        } else {
            // Nothing in the iterator
            setStorage(emptyStorage);
        }
    }

    /**
     * Intended as a fragment of temporary storage for use we do not know how many bytes of
     * allocate, and we are reading in some kind of iterable stream.
     */
    protected static class Fragment {

        static final int MINSIZE = 8;
        static final int MAXSIZE = 1024;

        byte[] storage;
        int count = 0;

        Fragment(int size) {
            storage = new byte[size];
        }

        // Convert to byte and add to buffer
        boolean isFilledBy(PyObject value) {
            storage[count++] = byteCheck(value);
            return count == storage.length;
        }
    }

    /**
     * A container of temporary storage when we do not know how many bytes to allocate, and we are
     * reading in some kind of iterable stream.
     */
    protected static class FragmentList extends LinkedList<Fragment> {

        /**
         * Total number of bytes being held.
         */
        int totalCount = 0;

        /**
         * Load bytes into the container from the given iterable
         *
         * @param iter iterable source of values to enter into the container
         * @throws PyException(TypeError) if any value not acceptable type
         * @throws PyException(ValueError) if any value<0 or value>255 or string length!=1
         */
        void loadFrom(Iterable<? extends PyObject> iter) throws PyException {

            int fragSize = Fragment.MINSIZE;
            Fragment curr = null;

            // Allocate series of fragments as needed, while the iterator runs to completion
            //
            for (PyObject value : iter) {
                if (curr == null) {
                    // Need a new Fragment
                    curr = new Fragment(fragSize);
                    add(curr);
                    if (fragSize < Fragment.MAXSIZE) {
                        fragSize <<= 1;
                    }
                }
                // Insert next item from iterator.
                if (curr.isFilledBy(value)) {
                    // Fragment is now full: signal a new one will be needed
                    totalCount += curr.count;
                    curr = null;
                }
            }

            // Don't forget the bytes in the final Fragment
            if (curr != null) {
                totalCount += curr.count;
            }
        }

        /**
         * Move the contents of this container to the given byte array at the specified index. This
         * method leaves this container empty.
         *
         * @param target destination array
         * @param p position to write first byte
         */
        void emptyInto(byte[] target, int p) {

            for (Fragment frag : this) {
                System.arraycopy(frag.storage, 0, target, p, frag.count);
                p += frag.count;
            }
            clear(); // Encourage recycling
            totalCount = 0;
        }

        /**
         * Move the contents of this container to a strided subset of the given byte array at the
         * specified index. Bytes are assigned at start, start+step, start+2*step, and so on until
         * we run out. (You must have checked beforehand that the destination is big enough.) This
         * method leaves this container empty. If the step size is one, it would be much quicker to
         * call {@link BaseBytes#emptyInto(byte[], int)}
         *
         * @param target destination array
         * @param start position to write first byte
         * @param step amount to advance index with each byte
         */
        void emptyInto(byte[] target, int start, int step) {
            int p = start;
            for (Fragment frag : this) {
                for (int i = 0; i < frag.count; i++) {
                    target[p] = frag.storage[i];
                    p += step;
                }
            }
            clear(); // Encourage recycling
            totalCount = 0;
        }

    }

    /*
     * ========================================================================================
     * Sharable storage
     * ========================================================================================
     *
     * The storage is provided by a byte array that may be somewhat larger than the number of bytes
     * actually stored, and these bytes may begin at an offset within the storage. Immutable
     * subclasses of BaseBytes may exploit this to share storage when constructed from a slice of
     * another immutable subclass. Mutable subclasses may exploit it to provide efficient insertions
     * near the start of the array.
     */

    /** Empty storage constant */
    protected static final byte[] emptyStorage = new byte[0];

    /** Storage array. */
    protected byte[] storage = emptyStorage;

    /** Number of bytes actually used in storage array. */
    protected int size = 0;

    /** Index of first byte used in storage array. */
    protected int offset = 0;

    /**
     * Check that an index is within the range of the array, that is <tt>0&lt;=index&lt;size</tt>.
     *
     * @param index to check
     * @throws PyException(IndexError) if the index is outside the array bounds
     */
    protected final void indexCheck(int index) throws PyException {
        if (index < 0 || index >= size) {
            throw Py.IndexError(getType().fastGetName() + " index out of range");
        }
    }

    /**
     * Allocate fresh, zero-filled storage for the requested number of bytes and make that the size.
     * If the size needed is zero, the "storage" allocated is the shared emptyStorage array. The
     * allocated size may be bigger than needed, and the method chooses a value for offset.
     *
     * @param needed becomes the new value of this.size
     */
    protected void newStorage(int needed) {
        // The implementation for immutable arrays allocates only as many bytes as needed.
        if (needed > 0) {
            setStorage(new byte[needed]); // guaranteed zero (by JLS 2ed para 4.5.5)
        } else {
            setStorage(emptyStorage);
        }
    }

    /**
     * Check that an integer is suitable for storage in a (Python) byte array, and convert it to the
     * Java byte value that can be stored there. (Java bytes run -128..127 whereas Python bytes run
     * 0..255.)
     *
     * @param value to convert.
     * @throws PyException(ValueError) if value<0 or value>255
     */
    protected static final byte byteCheck(int value) throws PyException {
        if (value < 0 || value > 255) {
            throw Py.ValueError("byte must be in range(0, 256)");
        }
        return (byte)value;
    }

    /**
     * Check that the value of an PyInteger is suitable for storage in a (Python) byte array, and
     * convert it to the Java byte value that can be stored there. (Java bytes run -128..127 whereas
     * Python bytes run 0..255.)
     *
     * @param value to convert.
     * @throws PyException(ValueError) if value<0 or value>255
     */
    protected static final byte byteCheck(PyInteger value) throws PyException {
        return byteCheck(value.asInt());
    }

    /**
     * Check that the type and value of a PyObject is suitable for storage in a (Python) byte array,
     * and convert it to the Java byte value that can be stored there. (Java bytes run -128..127
     * whereas Python bytes run 0..255.) Acceptable types are:
     * <ul>
     * <li>PyInteger in range 0 to 255 inclusive</li>
     * <li>PyLong in range 0 to 255 inclusive</li>
     * <li>Any type having an __index__() method, in range 0 to 255 inclusive</li>
     * <li>PyString of length 1</li>
     * </ul>
     *
     * @param value to convert.
     * @throws PyException(TypeError) if not acceptable type
     * @throws PyException(ValueError) if value<0 or value>255 or string length!=1
     */
    protected static final byte byteCheck(PyObject value) throws PyException {
        if (value.isIndex()) {
            // This will possibly produce Py.OverflowError("long int too large to convert")
            return byteCheck(value.asIndex());
        } else if (value.getType() == PyString.TYPE) {
            // Exactly PyString (not PyUnicode)
            String strValue = ((PyString)value).getString();
            if (strValue.length() != 1) {
                throw Py.ValueError("string must be of size 1");
            }
            return byteCheck(strValue.charAt(0));
        } else {
            throw Py.TypeError("an integer or string of size 1 is required");
        }
    }

    /*
     * ========================================================================================
     * Wrapper class to make other objects into byte arrays
     * ========================================================================================
     *
     * In much of the bytearray and bytes API, the "other sequence" argument will accept any type
     * that supports the buffer protocol, that is, the object can supply a memoryview through which
     * the value is treated as a byte array. We have not implemented memoryview objects yet, and it
     * is not clear what the Java API should be. As a temporary expedient, we define here a
     * byte-oriented view on the key built-in types.
     */

    interface View {

        /**
         * Return the indexed byte as a byte
         *
         * @param index
         * @return byte at index
         */
        public byte byteAt(int index);

        /**
         * Return the indexed byte as an unsigned integer
         *
         * @param index
         * @return value of the byte at index
         */
        public int intAt(int index);

        /**
         * Number of bytes in the view: valid indexes are from <code>0</code> to
         * <code>size()-1</code>.
         *
         * @return the size
         */
        public int size();

        /**
         * Return a new view that is a simple slice of this one defined by <code>[start:end]</code>.
         * <code>Py.None</code> or <code>null</code> are acceptable for start and end, and have
         * Python slice semantics. Negative values for start or end are treated as "from the end",
         * in the usual manner of Python slices.
         *
         * @param start first element to include
         * @param end first element after slice, not to include
         * @return byte-oriented view
         */
        public View slice(PyObject start, PyObject end);
    }

    /**
     * Some common apparatus for views including the implementation of slice semantics.
     */
    static abstract class ViewBase implements View {

        // Implement Python slice semantics
        public View slice(PyObject ostart, PyObject oend) {
            PySlice s = new PySlice(ostart, oend, null);
            int[] index = s.indicesEx(size());  // [ start, end, 1, end-start ]
            return sliceImpl(index[0], index[(index[3] > 0) ? 1 : 0]);
        }

        /**
         * Implementation-specific part of returning a slice of the current view. This is called by
         * the default implementations of {@link #slice(int, int)} and
         * {@link #slice(PyObject, PyObject)} once the <code>start</code> and <code>end</code>
         * arguments have been reduced to simple integer indexes. It is guaranteed that
         * <code>start>=0</code> and <code>size()>=end>=start</code> when the method is called.
         * Implementors are encouraged to do something more efficient than piling on anothe wrapper.
         *
         * @param start first element to include
         * @param end first element after slice, not to include
         * @return byte-oriented view
         */
        protected abstract View sliceImpl(int start, int end);

    }

    /**
     * Return a wrapper providing a byte-oriented view for whatever object is passed, or return
     * <code>null</code> if we don't know how.
     *
     * @param b object to wrap
     * @return byte-oriented view or null
     */
    protected static View getView(PyObject b) {
        if (b == null) {
            return null;
        } else if (b instanceof BaseBytes) {
            return new ViewOfBytes((BaseBytes)b);
        } else if (b.getType() == PyString.TYPE) {
            return new ViewOfString(b.asString());
        }
        return null;
    }

    /**
     * Return a wrapper providing a byte-oriented view for a slice of whatever object is passed, or
     * return <code>null</code> if we don't know how.
     *
     * @param b object to wrap
     * @param start index of first byte to include
     * @param end index of first byte after slice
     * @return byte-oriented view or null
     */
    protected static View getView(PyObject b, PyObject start, PyObject end) {
        View whole = getView(b);
        if (whole != null) {
            return whole.slice(start, end);
        } else {
            return null;
        }
    }

    /**
     * Return a wrapper providing a byte-oriented view of a slice of this object.
     *
     * @param ostart index of first byte to include
     * @param oend index of first byte after slice
     * @return byte-oriented view or null
     */
    protected ViewOfBytes slice(PyObject ostart, PyObject oend) {
        PySlice s = new PySlice(ostart, oend, null);
        int[] index = s.indicesEx(size); // [ start, end, 1, end-start ]
        return new ViewOfBytes(storage, offset + index[0], index[3]);
    }

    /**
     * Return a wrapper providing a byte-oriented view for whatever object is passed, or raise an
     * exception if we don't know how.
     *
     * @param b object to wrap
     * @return byte-oriented view
     */
    protected static View getViewOrError(PyObject b) {
        View res = getView(b);
        if (res == null) {
            // String fmt = "type %s doesn't support the buffer API"; // CPython
            String fmt = "cannot access type %s as bytes";
            throw Py.NotImplementedError(String.format(fmt, b.getType().fastGetName()));
        }
        return res;
    }

    /**
     * Return a wrapper providing a byte-oriented view for a slice of whatever object is passed, or
     * raise an exception if we don't know how.
     *
     * @param b object to wrap
     * @param start index of first byte to include
     * @param end index of first byte after slice
     * @return byte-oriented view or null
     */
    protected static View getViewOrError(PyObject b, PyObject start, PyObject end) {
        View whole = getViewOrError(b);
        return whole.slice(start, end);
    }

    /**
     * Wrapper providing a byte-oriented view for String (or PyString).
     */
    protected static class ViewOfString extends ViewBase {

        private String str;

        /**
         * Create a byte-oriented view of a String.
         *
         * @param str
         */
        public ViewOfString(String str) {
            this.str = str;
        }

        public byte byteAt(int index) {
            return byteCheck(str.charAt(index));
        }

        public int intAt(int index) {
            return str.charAt(index);
        }

        public int size() {
            return str.length();
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfString(str.substring(start, end));
        }

    }

    /**
     * Wrapper providing a byte-oriented view for byte arrays descended from BaseBytes. Not that
     * this view is not safe against concurrent modification by this or another thread: if the byte
     * array type is mutable, and the contents change, the contents of the view are likely to be
     * invalid.
     */
    protected static class ViewOfBytes extends ViewBase {

        private byte[] storage;
        private int offset;
        private int size;

        /**
         * Create a byte-oriented view of a byte array descended from BaseBytes.
         *
         * @param obj
         */
        public ViewOfBytes(BaseBytes obj) {
            this.storage = obj.storage;
            this.offset = obj.offset;
            this.size = obj.size;
        }

        /**
         * Create a byte-oriented view of a byte array explicitly. If the size<0, a zero-length
         * slice results.
         *
         * @param storage storage array
         * @param offset
         * @param size
         */
        ViewOfBytes(byte[] storage, int offset, int size) {
            if (size > 0) {
                this.storage = storage;
                this.offset = offset;
                this.size = size;
            } else {
                this.storage = emptyStorage;
                this.offset = 0;
                this.size = 0;
            }
        }

        public byte byteAt(int index) {
            return storage[offset + index];
        }

        public int intAt(int index) {
            return 0xff & storage[offset + index];
        }

        public int size() {
            return size;
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfBytes(storage, offset + start, end - start);
        }
    }

    /*
     * ======================================================================================== API
     * for org.python.core.PySequence
     * ========================================================================================
     */
    protected PyInteger pyget(int index) {
        return new PyInteger(intAt(index));
    }

    /*
     * We're not implementing these here, but we can give a stronger guarantee about the return type
     * and save some casting and type anxiety.
     */
    protected abstract BaseBytes getslice(int start, int stop, int step);

    protected abstract BaseBytes repeat(int count);

    /*
     * And this extension point should be overridden in mutable subclasses
     */

    /**
     * Insert the element (interpreted as a Python byte value) at the given index. The default
     * implementation produces a Python TypeError, for the benefit of immutable types. Mutable types
     * must override it.
     *
     * @param index to insert at
     * @param element to insert (by value)
     * @throws PyException(IndexError) if the index is outside the array bounds
     * @throws PyException(ValueError) if element<0 or element>255
     * @throws PyException(TypeError) if the subclass is immutable
     */
    public void pyinsert(int index, PyObject element) {
        // This won't succeed: it just produces the right error.
        // storageReplace(index, 0, 1);
        pyset(index, element);
    }

    /*
     * ========================================================================================
     * Support for Python API common to mutable and immutable subclasses
     * ========================================================================================
     */

    @Override
    public int __len__() {
        return size;
    }

    /**
     * Comparison function between two byte arrays returning 1, 0, or -1 as a>b, a==b, or a&lt;b
     * respectively. The comparison is by value, using Python unsigned byte conventions, and
     * left-to-right (low to high index). Zero bytes are significant, even at the end of the array:
     * <code>[1,2,3]&lt;[1,2,3,0]</code>, for example and <code>[]</code> is less than every other
     * value, even <code>[0]</code>.
     *
     * @param a left-hand array in the comparison
     * @param b right-hand array in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(BaseBytes a, BaseBytes b) {

        // Compare elements one by one in these ranges:
        int ap = a.offset;
        int aEnd = ap + a.size;
        int bp = b.offset;
        int bEnd = bp + b.size;

        while (ap < aEnd) {
            if (bp >= bEnd) {
                // a is longer than b
                return 1;
            } else {
                // Compare the corresponding bytes (as unsigned ints)
                int aVal = 0xff & a.storage[ap++];
                int bVal = 0xff & b.storage[bp++];
                int diff = aVal - bVal;
                if (diff != 0) {
                    return (diff < 0) ? -1 : 1;
                }
            }
        }

        // All the bytes matched and we reached the end of a
        if (bp < bEnd) {
            // But we didn't reach the end of b
            return -1;
        } else {
            // And the end of b at the same time, so they're equal
            return 0;
        }

    }

// /**
// * Comparison function between a byte array and a String returning 1, 0 or -1 as a>b, a==b, or
// * a&lt;b respectively. The comparison is by value, using Python unsigned byte conventions for
// * the byte array and character code for elements of the string, and left-to-right (low to high
// * index). Zero bytes are significant, even at the end of the array:
// * <code>[65,66,67]&lt;"ABC\u0000"</code>, for example and <code>[]</code> is less than every
// * non-empty String, while <code>[]==""</code>.
// *
// * @param a left-hand array in the comparison
// * @param b right-hand String in the comparison
// * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
// */
// private static int compare(BaseBytes a, String b) {
//
// // Compare elements one by one in these ranges:
// int ap = a.offset;
// int aEnd = ap + a.size;
// int bp = 0;
// int bEnd = b.length();
//
// while (ap < aEnd) {
// if (bp >= bEnd) {
// // a is longer than b
// return 1;
// } else {
// // Compare the corresponding bytes and character codes
// int aVal = 0xff & a.storage[ap++];
// int bVal = b.charAt(bp++);
// int diff = aVal - bVal;
// if (diff != 0) {
// return (diff < 0) ? -1 : 1;
// }
// }
// }
//
// // All the bytes matched and we reached the end of a
// if (bp < bEnd) {
// // But we didn't reach the end of b
// return -1;
// } else {
// // And the end of b at the same time, so they're equal
// return 0;
// }
//
// }

    /**
     * Comparison function between a byte array and a byte-oriented View of some other object, such
     * as a String, returning 1, 0 or -1 as a>b, a==b, or a&lt;b respectively. The comparison is by
     * value, using Python unsigned byte conventions, left-to-right (low to high index). Zero bytes
     * are significant, even at the end of the array: <code>[65,66,67]&lt;"ABC\u0000"</code>, for
     * example and <code>[]</code> is less than every non-empty b, while <code>[]==""</code>.
     *
     * @param a left-hand array in the comparison
     * @param b right-hand wrapped object in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(BaseBytes a, View b) {

        // Compare elements one by one in these ranges:
        int ap = a.offset;
        int aEnd = ap + a.size;
        int bp = 0;
        int bEnd = b.size();

        while (ap < aEnd) {
            if (bp >= bEnd) {
                // a is longer than b
                return 1;
            } else {
                // Compare the corresponding bytes
                int aVal = 0xff & a.storage[ap++];
                int bVal = b.intAt(bp++);
                int diff = aVal - bVal;
                if (diff != 0) {
                    return (diff < 0) ? -1 : 1;
                }
            }
        }

        // All the bytes matched and we reached the end of a
        if (bp < bEnd) {
            // But we didn't reach the end of b
            return -1;
        } else {
            // And the end of b at the same time, so they're equal
            return 0;
        }

    }

    /**
     * Comparison function between byte array types and any other object. The set of 6
     * "rich comparison" operators are based on this.
     *
     * @param b
     * @return 1, 0 or -1 as this>b, this==b, or this&lt;b respectively, or -2 if the comparison is
     *         not implemented
     */
    private synchronized int basebytes_cmp(PyObject b) {

        // This is not exposed as bytearray and bytes have no __cmp__.

        if (this == b) {
            // Same object: quick result
            return 0;

        } else {

            // Try to get a byte-oriented view
            View bv = getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject _cmp_unsafe() and related code.
                return -2;

            } else {
                // Object supported by our interim memory view
                return compare(this, bv);

            }
        }
    }

// /**
// * Comparison function between byte array types and any other object. The set of 6
// * "rich comparison" operators are based on this.
// *
// * @param b
// * @return 1, 0 or -1 as this>b, this==b, or this&lt;b respectively, or -2 if the comparison is
// * not implemented
// */
// private synchronized int basebytes_cmp(PyObject b) {
//
// // This is based on PyFloat.float___cmp__() as it seems to have the same need to support
// // multiple types for other, but it is not exposed as bytearray and bytes have no __cmp__.
//
// if (this == b) {
// // Same object: quick result
// return 0;
//
// } else if (b instanceof BaseBytes) {
// return compare(this, (BaseBytes)b);
//
// } else if (b.getType() == PyString.TYPE) {
// /*
// * Necessary to permit comparison of bytearray and bytes, which in in Python 2.7 is an
// * alias of str. Remove in 3.0
// */
// return compare(this, ((PyString)b).asString());
//
// } else if (b instanceof MemoryViewProtocol) {
// // XXX: Revisit when we have an implementation of memoryview
// // MemoryView mv = ((MemoryViewProtocol) other).getMemoryView();
// return -2;
//
// } else {
// // Signifies a type mis-match. See PyObject _cmp_unsafe() and related code.
// return -2;
// }
// }

    /**
     * Fail-fast comparison function between byte array types and any other object, for when the
     * test is only for equality. The "rich comparison" operators <code>__eq__</code> and
     * <code>__ne__</code> are based on this.
     *
     * @param b
     * @return 0 if this==b, or +1 or -1 if this!=b, or -2 if the comparison is not implemented
     */
    private synchronized int basebytes_cmpeq(PyObject b) {

        if (this == b) {
            // Same object: quick result
            return 0;

        } else {

            // Try to get a byte-oriented view
            View bv = getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject _cmp_unsafe() and related code.
                return -2;

            } else if (bv.size() != size) {
                // Different size: can't be equal, and we don't care which is bigger
                return 1;

            } else {
                // Object supported by our interim memory view
                return compare(this, bv);

            }
        }
    }

// /**
// * Fail-fast comparison function between byte array types and any other object, for when the
// * test is only for equality. The "rich comparison" operators <code>__eq__</code> and
// * <code>__ne__</code> are based on this.
// *
// * @param b
// * @return 0 if this==b, or +1 or -1 if this!=b, or -2 if the comparison is not implemented
// */
// private synchronized int basebytes_cmpeq(PyObject b) {
//
// if (b instanceof BaseBytes) {
// if (((BaseBytes)b).size != size) {
// // Different size: can't be equal, and we don't care which is bigger
// return 1;
// }
// } else if (b.getType() == PyString.TYPE) {
// /*
// * Necessary to permit comparison of bytearray and bytes, which in in Python 2.7 is an
// * alias of str. Remove in 3.0
// */
// if (((PyString)b).__len__() != size) {
// // Different size: can't be equal, and we don't care which is bigger
// return 1;
// }
// }
// return basebytes_cmp(b);
// }

    /**
     * Implementation of __eq__ (equality) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___eq__(PyObject other) {
        int cmp = basebytes_cmpeq(other);
        if (cmp == 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ne__ (not equals) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___ne__(PyObject other) {
        int cmp = basebytes_cmpeq(other);
        if (cmp == 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __lt__ (less than) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___lt__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp >= 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __le__ (less than or equal to) operator, capable of comparison with another
     * byte array or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___le__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp > 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ge__ (greater than or equal to) operator, capable of comparison with
     * another byte array or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___ge__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp >= 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __gt__ (greater than) operator, capable of comparison with another byte
     * array or bytes. Comparison with an invalid type returns null.
     *
     * @param other
     * @return
     */
    final PyObject basebytes___gt__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp > 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Equivalent of the 'string_escape' decode to a String that is all-printable, showing non
     * printable charater as lowercase hex escapes, except '\t', '\n', and '\r'. This supports
     * <code>__repr__()</code>.
     *
     * @return the byte array as a String, still encoded
     */
    protected synchronized String asEscapedString() {
        StringBuilder buf = new StringBuilder(size + (size >> 8) + 10);
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            int c = 0xff & storage[j];
            if (c >= 0x7f) {    // Unprintable high 128 and DEL
                appendHexEscape(buf, c);
            } else if (c >= ' ') { // Printable
                if (c == '\\') {    // Special case
                    buf.append("\\\\");
                } else {
                    buf.append((char)c);
                }
            } else if (c == '\t') { // Spacial cases in the low 32
                buf.append("\\t");
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else {
                appendHexEscape(buf, c);
            }
        }
        return buf.toString();
    }

    private static final void appendHexEscape(StringBuilder buf, int c) {
        buf.append("\\x")
                .append(Character.forDigit((c & 0xf0) >> 4, 16))
                .append(Character.forDigit(c & 0xf, 16));
    }

    /**
     * Search for the target in this byte array, returning true if found and false if not. The
     * target must be compatible with the Python byte range.
     *
     * @param target byte value to search for
     * @return true iff found
     */
    protected final synchronized boolean basebytes___contains__(PyObject target) {
        byte t = byteCheck(target);
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            if (storage[j] == t) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the bytes of a byte array to the characters of a String with no change in ordinal value.
     * This could also be described as 'latin-1' decoding of the byte array to a String.
     *
     * @return the byte array as a String, still encoded
     */
    private synchronized String asEncodedString() {
        StringBuilder buf = new StringBuilder(size);
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            buf.append((char)(0xff & storage[j]));
        }
        return buf.toString();
    }

    /**
     * Decode the byte array to a Unicode string according to the default encoding. The returned
     * PyObject should be a <code>PyUnicode</code>, since the default codec is well-behaved.
     *
     * @return object containing the decoded characters
     */
    public PyObject decode() {
        return decode(null, null);
    }

    /**
     * Decode the byte array to a Unicode string according to the specified encoding and default
     * error policy. The returned PyObject will usually be a <code>PyUnicode</code>, but in practice
     * it is whatever the <code>decode</code> method of the codec decides.
     *
     * @param encoding the name of the codec (uses default codec if null)
     * @return object containing the decoded characters
     */
    public PyObject decode(String encoding) {
        return decode(encoding, null);
    }

    /**
     * Decode the byte array to a Unicode string according to the specified encoding and error
     * policy. The returned PyObject will usually be a <code>PyUnicode</code>, but in practice it is
     * whatever the <code>decode</code> method of the codec decides.
     *
     * @param encoding the name of the codec (uses default codec if null)
     * @param errors the name of the error policy (uses 'strict' if null)
     * @return object containing the decoded characters
     */
    public PyObject decode(String encoding, String errors) {
        /*
         * Provide a Python <code>str</code> input to the decode method of a codec, which in v2.7
         * expects a PyString. (In Python 3k the codecs decode from the <code>bytes</code> type, so
         * we can pass this directly.)
         */
        PyString this_ = new PyString(this.asEncodedString());
        return codecs.decode(this_, encoding, errors);
    }

    /**
     * Ready-to-expose implementation of decode( [ encoding [, errors ]] )
     *
     * @param args Python argument list
     * @param keywords Assocaited keywords
     * @return
     */
    protected final PyObject basebytes_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return decode(encoding, errors);
    }

    /**
     * Ready-to-expose implementation of Python <code>find( sub [, start [, end ]] )</code>. Return
     * the lowest index in the byte array where byte sequence <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation. Return -1 if <code>sub</code> is
     * not found.
     *
     * @param sub bytes to find
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of ocurrence of sub within this byte array
     */
    final int basebytes_find(PyObject sub, PyObject start, PyObject end) {
        ViewOfBytes v = this.slice(start, end);
        byte[] buf = v.storage;
        View vsub = getViewOrError(sub);
        int n = vsub.size();
        // No point testing beyond this location, as insufficient space:
        int pmax = v.offset + v.size - n;
        // Use start positions from v.offset to pmax in the buffer buf
        for (int p = v.offset; p < pmax; p++) {
            int j;
            for (j = 0; j < n; j++) {
                if (buf[p + j] != vsub.byteAt(j)) {
                    break;
                }
            }
            // If we tested all n bytes, that's a match. Note that the returned index is
            // relative to the (operative) start of this, not relative to the start of the slice.
            if (j == n) {
                return p - this.offset;
            }
        }
        // We tested all the start positions without success
        return -1;
    }

    /**
     * Convenience method to create a <code>TypeError</code> PyException with the message
     * "can't concat {type} to {toType}"
     *
     * @param type
     * @param toType
     * @return PyException (TypeError) as detailed
     */
    public static PyException ConcatenationTypeError(PyType type, PyType toType) {
        String fmt = "can't concat %s to %s";
        return Py.TypeError(String.format(fmt, type.fastGetName(), toType.fastGetName()));
    }

    /**
     * Support for pickling byte arrays: reduce a byte array to the actual type, arguments for
     * (re-)construction of the object, and the dictionary of any user-defined sub-class.
     *
     * @return PyTuple that is first stage in pickling byte array
     */
    public PyObject __reduce__() {
        return basebytes___reduce__();
    }

    final PyTuple basebytes___reduce__() {
        PyUnicode encoded = new PyUnicode(this.asEncodedString());
        PyObject args = new PyTuple(encoded, getPickleEncoding());
        PyObject dict = __findattr__("__dict__");
        return new PyTuple(getType(), args, (dict != null) ? dict : Py.None);
    }

    private static PyString PICKLE_ENCODING;

    /**
     * Name the encoding effectively used in __reduce__() suport for pickling: this choice is
     * hard-coded in CPython as "latin-1".
     */
    private static final PyString getPickleEncoding() {
        if (PICKLE_ENCODING == null) {
            PICKLE_ENCODING = new PyString("latin-1");
        }
        return PICKLE_ENCODING;
    }

    /*
     * ======================================================================================== API
     * for Java access as byte[]
     * ========================================================================================
     *
     * Just the immutable case for now
     */

    /**
     * No range check access to byte[index].
     *
     * @param index
     * @return the byte at the given index
     */
    private final synchronized byte byteAt(int index) {
        return storage[index + offset];
    }

    /**
     * Return the Python byte (in range 0 to 255 inclusive) at the given index.
     *
     * @param index of value in byte array
     * @return the integer value at the index
     * @throws PyException(IndexError) if the index is outside the array bounds
     */
    public synchronized int intAt(int index) throws PyException {
        indexCheck(index);
        return 0xff & byteAt(index);
    }

    /**
     * Helper to implement {@link #repeat(int)}. Use something like:
     *
     * <pre>
     *
     *
     *
     * &#064;Override
     * protected PyByteArray repeat(int count) {
     *     PyByteArray ret = new PyByteArray();
     *     ret.setStorage(repeatImpl(count));
     *     return ret;
     * }
     * </pre>
     *
     * @param count the number of times to repeat this.
     * @return this byte array repeated count times.
     */
    protected synchronized byte[] repeatImpl(int count) {
        byte[] dst = new byte[count * size];
        for (int i = 0, p = 0; i < count; i++, p += size) {
            System.arraycopy(storage, offset, dst, p, size);
        }
        return dst;
    }

    /*
     * ======================================================================================== API
     * for java.util.List<PyInteger>
     * ========================================================================================
     */

    /**
     * Access to the bytearray (or bytes) as a {@link java.util.List}. The List interface supplied
     * by BaseBytes delegates to this object.
     */
    protected final List<PyInteger> listDelegate = new AbstractList<PyInteger>() {

        @Override
        public PyInteger get(int index) {
            // Not using __getitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            return pyget(index);
        }

        @Override
        public int size() {
            return size;
        }

        // For mutable subclass use

        /**
         * Replaces the element at the specified position in this list with the specified element.
         *
         * @see java.util.AbstractList#set(int, java.lang.Object)
         * @throws PyException(TypeError) if actual class is immutable
         * @throws PyException(IndexError) if the index is outside the array bounds
         * @throws PyException(ValueError) if element<0 or element>255
         */
        @Override
        public PyInteger set(int index, PyInteger element) throws PyException {
            // Not using __setitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            PyInteger result = pyget(index);
            pyset(index, element);      // TypeError if immutable
            return result;
        }

        /**
         * Inserts the specified element at the specified position in this list. Shifts the element
         * currently at that position and any subsequent elements to the right.
         *
         * @see java.util.AbstractList#add(int, java.lang.Object)
         * @throws PyException(IndexError) if the index is outside the array bounds
         * @throws PyException(ValueError) if element<0 or element>255
         * @throws PyException(TypeError) if the owning concrete subclass is immutable
         */
        @Override
        public void add(int index, PyInteger element) throws PyException {
            // Not using __setitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            pyinsert(index, element);          // TypeError if immutable
        }

        /**
         * Removes the element at the specified position in this list. Shifts any subsequent
         * elements to the left (subtracts one from their indices). Returns the element that was
         * removed from the list.
         *
         * @see java.util.AbstractList#remove(int)
         * @throws PyException(IndexError) if the index is outside the array bounds
         */
        @Override
        public PyInteger remove(int index) {
            // Not using __delitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            PyInteger result = pyget(index);
            del(index);      // TypeError if immutable
            return result;
        }
    };

    /**
     * Number of bytes in bytearray (or bytes) object.
     *
     * @see java.util.List#size()
     * @return Number of bytes in byte array.
     * */
    public int size() {
        return size;
    }

    /*
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns true if this list contains the specified value. More formally, returns true if and
     * only if this list contains at least one integer e such that o.equals(PyInteger(e)).
     */
    public boolean contains(Object o) {
        return listDelegate.contains(o);
    }

    /*
     * @return
     *
     * @see java.util.List#iterator()
     */
    public Iterator<PyInteger> iterator() {
        return listDelegate.iterator();
    }

    /*
     * @return
     *
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        return listDelegate.toArray();
    }

    /*
     * @param a
     *
     * @return
     *
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return listDelegate.toArray(a);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(PyInteger o) {
        return listDelegate.add(o);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return listDelegate.remove(o);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        return listDelegate.containsAll(c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends PyInteger> c) {
        return listDelegate.addAll(c);
    }

    /*
     * @param index
     *
     * @param c
     *
     * @return
     *
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends PyInteger> c) {
        return listDelegate.addAll(index, c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        return listDelegate.removeAll(c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        return listDelegate.retainAll(c);
    }

    /*
     *
     * @see java.util.List#clear()
     */
    public void clear() {
        listDelegate.clear();
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return listDelegate.equals(o);
    }

    /*
     * @return
     *
     * @see java.util.List#hashCode()
     */
    public int hashCode() {
        return listDelegate.hashCode();
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#get(int)
     */
    public PyInteger get(int index) {
        return listDelegate.get(index);
    }

    /*
     * @param index
     *
     * @param element
     *
     * @return
     *
     * @see java.util.List#set(int, java.lang.Object)
     */
    public PyInteger set(int index, PyInteger element) {
        return listDelegate.set(index, element);
    }

    /*
     * @param index
     *
     * @param element
     *
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, PyInteger element) {
        listDelegate.add(index, element);
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#remove(int)
     */
    public PyInteger remove(int index) {
        return listDelegate.remove(index);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        return listDelegate.indexOf(o);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        return listDelegate.lastIndexOf(o);
    }

    /*
     * @return
     *
     * @see java.util.List#listIterator()
     */
    public ListIterator<PyInteger> listIterator() {
        return listDelegate.listIterator();
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<PyInteger> listIterator(int index) {
        return listDelegate.listIterator(index);
    }

    /*
     * @param fromIndex
     *
     * @param toIndex
     *
     * @return
     *
     * @see java.util.List#subList(int, int)
     */
    public List<PyInteger> subList(int fromIndex, int toIndex) {
        return listDelegate.subList(fromIndex, toIndex);
    }

}
