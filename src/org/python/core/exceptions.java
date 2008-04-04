// Copyright 2001 Finn Bock
package org.python.core;

import java.io.File;
import java.lang.reflect.Method;

import org.python.modules.zipimport.zipimport;

/**
 * The builtin exceptions module. The entire module should be imported from
 * python. None of the methods defined here should be called from java.
 */
public class exceptions implements ClassDictInit {

    public static String __doc__ =
            "Exceptions found here are defined both in the exceptions module and the "
            + "built-in namespace.  It is recommended that user-defined exceptions "
            + "inherit from Exception.  See the documentation for the exception "
            + "inheritance hierarchy.";

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) {
        dict.invoke("clear");
        dict.__setitem__("__name__", new PyString("exceptions"));
        dict.__setitem__("__doc__", new PyString(__doc__));

        ThreadState ts = Py.getThreadState();
        if (ts.systemState == null) {
            ts.systemState = Py.defaultSystemState;
        }
        // Push frame
        PyFrame frame = new PyFrame(null, new PyStringMap());
        frame.f_back = ts.frame;
        if (frame.f_builtins == null) {
            if (frame.f_back != null) {
                frame.f_builtins = frame.f_back.f_builtins;
            } else {
                frame.f_builtins = PySystemState.builtins;
            }
        }
        ts.frame = frame;

        dict.__setitem__("BaseException",  PyBaseException.TYPE);

        buildClass(dict, "KeyboardInterrupt", "BaseException", "empty__init__",
                "Program interrupted by user.");

        buildClass(dict, "SystemExit", "BaseException", "SystemExit",
                "Request to exit from the interpreter.");

        buildClass(dict, "Exception", "BaseException", "empty__init__",
                "Common base class for all non-exit exceptions.");

        buildClass(dict, "StandardError", "Exception", "empty__init__",
                "Base class for all standard Python exceptions.");

        buildClass(dict, "SyntaxError", "StandardError", "SyntaxError",
                "Invalid syntax");

        buildClass(dict, "IndentationError", "SyntaxError", "empty__init__",
                "Improper indentation");

        buildClass(dict, "TabError", "IndentationError", "empty__init__",
                "Improper mixture of spaces and tabs.");

        buildClass(dict, "EnvironmentError", "StandardError",
                "EnvironmentError", "Base class for I/O related errors.");

        buildClass(dict, "IOError", "EnvironmentError", "empty__init__",
                "I/O operation failed.");

        buildClass(dict, "OSError", "EnvironmentError", "empty__init__",
                "OS system call failed.");

        buildClass(dict, "RuntimeError", "StandardError", "empty__init__",
                "Unspecified run-time error.");

        buildClass(dict, "NotImplementedError", "RuntimeError",
                "empty__init__",
                "Method or function hasn't been implemented yet.");

        buildClass(dict, "SystemError", "StandardError", "empty__init__",
                "Internal error in the Python interpreter.\n\n"
                        + "Please report this to the Python maintainer, "
                        + "along with the traceback,\n"
                        + "the Python version, and the hardware/OS "
                        + "platform and version.");

        buildClass(dict, "ReferenceError", "StandardError", "empty__init__",
                "Weak ref proxy used after referent went away.");

        buildClass(dict, "EOFError", "StandardError", "empty__init__",
                "Read beyond end of file.");

        buildClass(dict, "ImportError", "StandardError", "empty__init__",
                "Import can't find module, or can't find name in module.");

        buildClass(dict, "TypeError", "StandardError", "empty__init__",
                "Inappropriate argument type.");

        buildClass(dict, "ValueError", "StandardError", "empty__init__",
                "Inappropriate argument value (of correct type).");

        buildClass(dict, "UnicodeError", "ValueError", "empty__init__",
                "Unicode related error.");

        buildClass(dict, "UnicodeEncodeError", "UnicodeError", "UnicodeEncodeError",
                "Unicode encoding error.");

        buildClass(dict, "UnicodeDecodeError", "UnicodeError", "UnicodeDecodeError",
                "Unicode decoding error.");

        buildClass(dict, "UnicodeTranslateError", "UnicodeError", "UnicodeTranslateError",
                "Unicode translation error.");

        buildClass(dict, "AssertionError", "StandardError", "empty__init__",
                "Assertion failed.");

        buildClass(dict, "ArithmeticError", "StandardError", "empty__init__",
                "Base class for arithmetic errors.");

        buildClass(dict, "OverflowError", "ArithmeticError", "empty__init__",
                "Result too large to be represented.");

        buildClass(dict, "FloatingPointError", "ArithmeticError",
                "empty__init__", "Floating point operation failed.");

        buildClass(dict, "ZeroDivisionError", "ArithmeticError",
                "empty__init__",
                "Second argument to a division or modulo operation "
                        + "was zero.");

        buildClass(dict, "LookupError", "StandardError", "empty__init__",
                "Base class for lookup errors.");

        buildClass(dict, "IndexError", "LookupError", "empty__init__",
                "Sequence index out of range.");

        buildClass(dict, "KeyError", "LookupError", "empty__init__",
                "Mapping key not found.");

        buildClass(dict, "AttributeError", "StandardError", "empty__init__",
                "Attribute not found.");

        buildClass(dict, "NameError", "StandardError", "empty__init__",
                "Name not found globally.");

        buildClass(dict, "UnboundLocalError", "NameError", "empty__init__",
                "Local name referenced but not bound to a value.");

        buildClass(dict, "MemoryError", "StandardError", "empty__init__",
                "Out of memory.");

        buildClass(dict, "StopIteration", "Exception", "empty__init__",
                "Signal the end from iterator.next().");
        
        buildClass(dict, "GeneratorExit", "Exception", "empty__init__",
                "Request that a generator exit.");

        buildClass(dict, "Warning", "Exception", "empty__init__",
                "Base class for warning categories.");

        buildClass(dict, "UserWarning", "Warning", "empty__init__",
                "Base class for warnings generated by user code.");

        buildClass(dict, "DeprecationWarning", "Warning", "empty__init__",
                "Base class for warnings about deprecated features.");
        
        buildClass(dict, "PendingDeprecationWarning", "Warning", "empty__init__",
                "Base class for warnings about features which will be deprecated in the future.");

        buildClass(dict, "SyntaxWarning", "Warning", "empty__init__",
                "Base class for warnings about dubious syntax.");

        buildClass(dict, "RuntimeWarning", "Warning", "empty__init__",
                "Base class for warnings about dubious runtime behavior.");

        buildClass(dict, "OverflowWarning", "Warning", "empty__init__",
                "Base class for warnings about numeric overflow.");
        
        buildClass(dict, "FutureWarning", "Warning", "empty__init__",
                "Base class for warnings about constructs that will change semantically in the future.");

        // Initialize ZipImportError here, where it's safe to; it's
        // needed immediately
        zipimport.initClassExceptions(dict);

        ts.frame = ts.frame.f_back;
    }

    // An empty __init__ method
    public static PyObject empty__init__(PyObject[] arg, String[] kws) {
        return new PyStringMap();
    }

    public static PyObject SyntaxError(PyObject[] arg, String[] kws) {
        PyObject __dict__ = new PyStringMap();
        defineSlots(__dict__, "msg", "filename", "lineno", "offset", "text",
                    "print_file_and_line");
        __dict__.__setitem__("__init__", bindStaticJavaMethod("__init__", "SyntaxError__init__"));
        __dict__.__setitem__("__str__", bindStaticJavaMethod("__str__", "SyntaxError__str__"));
        return __dict__;
    }

    public static void SyntaxError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);

        if (args.length >= 1) {
            self.__setattr__("msg", args[0]);
        }
        if (args.length == 2) {
            PyObject[] info = Py.make_array(args[1]);
            if (info.length != 4) {
                throw Py.IndexError("tuple index out of range");
            }

            self.__setattr__("filename", info[0]);
            self.__setattr__("lineno", info[1]);
            self.__setattr__("offset", info[2]);
            self.__setattr__("text", info[3]);
        }
    }

    public static PyString SyntaxError__str__(PyObject self, PyObject[] arg, String[] kws) {
        PyObject msg = self.__getattr__("msg");
        PyObject str = msg.__str__();
        if (!(msg instanceof PyString)) {
            return Py.newString(str.toString());
        }

        PyObject filename = self.__findattr__("filename");
        PyObject lineno = self.__findattr__("lineno");
        boolean haveFilename = filename instanceof PyString;
        boolean haveLieno = lineno instanceof PyInteger;
        if (!haveFilename && !haveLieno) {
            return (PyString)str;
        }

        String result;
        if (haveFilename && haveLieno) {
            result = String.format("%s (%s, line %d)", str, basename(filename.toString()),
                                   lineno.asInt());
        } else if (haveFilename) {
            result = String.format("%s (%s)", str, basename(filename.toString()));
        } else {
            result = String.format("%s (line %d)", str, lineno.asInt());
        }

        return Py.newString(result);
    }

    public static PyObject EnvironmentError(PyObject[] args, String[] kwargs) {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "errno", "strerror", "filename");
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__", "EnvironmentError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "EnvironmentError__str__"));
        return dict;
    }

    public static void EnvironmentError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);

        if (args.length <= 1 || args.length > 3) {
            return;
        }
        PyObject errno = args[0];
        PyObject strerror = args[1];
        self.__setattr__("errno", errno);
        self.__setattr__("strerror", strerror);
        if (args.length == 3) {
            self.__setattr__("filename", args[2]);
            self.__setattr__("args", new PyTuple(errno, strerror));
        }
    }

    public static PyObject EnvironmentError__str__(PyObject self, PyObject[] args,
                                                   String[] kwargs) {
        PyObject errno = self.__findattr__("errno");
        PyObject strerror = self.__findattr__("strerror");
        PyObject filename = self.__findattr__("filename");
        String result;
        if (filename.__nonzero__()) {
            result = String.format("[Errno %s] %s: %s", errno, strerror, filename.__repr__());
        } else if (errno.__nonzero__() && strerror.__nonzero__()) {
            result = String.format("[Errno %s] %s", errno, strerror);
        } else {
            return PyBaseException.TYPE.invoke("__str__", self, args, kwargs);
        }
        return Py.newString(result);
    }

    public static PyObject SystemExit(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "code");
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__", "SystemExit__init__"));
        return dict;
    }

    public static void SystemExit__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        initSlots(self);

        if (args.length == 1) {
            self.__setattr__("code", args[0]);
        } else if (args.length > 1) {
            self.__setattr__("code", new PyTuple(args));
        }
    }

    public static PyObject UnicodeError(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        defineSlots(dict, "encoding", "object", "start", "end", "reason");
        // NOTE: UnicodeError doesn't actually use its own constructor
        return dict;
    }
    
    public static void UnicodeError__init__(PyObject self, PyObject[] args, String[] kwargs,
                                            PyType objectType) {
        ArgParser ap = new ArgParser("__init__", args, kwargs,
                                     new String[] {"encoding", "object", "start", "end",
                                                   "reason" },
                                     5);
        self.__setattr__("encoding", ap.getPyObjectByType(0, PyString.TYPE));
        self.__setattr__("object", ap.getPyObjectByType(1, objectType));
        self.__setattr__("start", ap.getPyObjectByType(2, PyInteger.TYPE));
        self.__setattr__("end", ap.getPyObjectByType(3, PyInteger.TYPE));
        self.__setattr__("reason", ap.getPyObjectByType(4, PyString.TYPE));
    }

    public static PyObject UnicodeDecodeError(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeDecodeError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "UnicodeDecodeError__str__"));
        return dict;
    }
    
    public static void UnicodeDecodeError__init__(PyObject self, PyObject[] args,
                                                  String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        UnicodeError__init__(self, args, kwargs, PyString.TYPE);
    }

    public static PyString UnicodeDecodeError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if (end == (start + 1)) {
            PyInteger badByte = new PyInteger((self.__getattr__("object")
                    .toString().charAt(start)) & 0xff);
            return Py.newString("'%.400s' codec can't decode byte 0x%02x in position %d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                         badByte,
                                         self.__getattr__("start"),
                                         self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("'%.400s' codec can't decode bytes in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                         self.__getattr__("start"),
                                         new PyInteger(end - 1),
                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    public static PyObject UnicodeEncodeError(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeEncodeError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__", "UnicodeEncodeError__str__"));
        return dict;
    }
    
    public static void UnicodeEncodeError__init__(PyObject self, PyObject[] args, String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        UnicodeError__init__(self, args, kwargs, PyUnicode.TYPE);
    }

    public static PyString UnicodeEncodeError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if(end == (start + 1)) {
            int badchar = self.__getattr__("object").toString().charAt(start);
            String format;
            if(badchar <= 0xff)
                format = "'%.400s' codec can't encode character u'\\x%02x' in position %d: %.400s";
            else if(badchar <= 0xffff)
                format = "'%.400s' codec can't encode character u'\\u%04x' in position %d: %.400s";
            else
                format = "'%.400s' codec can't encode character u'\\U%08x' in position %d: %.400s";
            return Py.newString(format)
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                                         new PyInteger(badchar),
                                                         self.__getattr__("start"),
                                                         self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("'%.400s' codec can't encode characters in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                                         self.__getattr__("start"),
                                                         new PyInteger(end - 1),
                                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    public static PyObject UnicodeTranslateError(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__init__", bindStaticJavaMethod("__init__",
                                                          "UnicodeTranslateError__init__"));
        dict.__setitem__("__str__", bindStaticJavaMethod("__str__",
                                                         "UnicodeTranslateError__str__"));
        return dict;
    }

    public static void UnicodeTranslateError__init__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        PyBaseException.TYPE.invoke("__init__", self, args, kwargs);
        ArgParser ap = new ArgParser("__init__", args, kwargs,
                                     new String[] {"object", "start", "end", "reason"},
                                     4);
        self.__setattr__("object", ap.getPyObjectByType(0, PyUnicode.TYPE));
        self.__setattr__("start", ap.getPyObjectByType(1, PyInteger.TYPE));
        self.__setattr__("end", ap.getPyObjectByType(2, PyInteger.TYPE));
        self.__setattr__("reason", ap.getPyObjectByType(3, PyString.TYPE));
    }

    public static PyString UnicodeTranslateError__str__(PyObject self, PyObject[] args,
                                                        String[] kwargs) {
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if(end == (start + 1)) {
            int badchar = (self.__getattr__("object").toString().charAt(start));
            String format;
            if(badchar <= 0xff)
                format = "can't translate character u'\\x%02x' in position %d: %.400s";
            else if(badchar <= 0xffff)
                format = "can't translate character u'\\u%04x' in position %d: %.400s";
            else
                format = "can't translate character u'\\U%08x' in position %d: %.400s";
            return Py.newString(format)
                    .__mod__(new PyTuple(new PyInteger(badchar),
                                                            self.__getattr__("start"),
                                                            self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("can't translate characters in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("start"),
                                                         new PyInteger(end - 1),
                                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    /**
     * Return the basename of a path string.
     *
     * @param name a path String
     * @return the basename'd result String
     */
    private static String basename(String name) {
        int lastSep = name.lastIndexOf(File.separatorChar);
        if (lastSep > -1) {
            return name.substring(lastSep + 1, name.length());
        }
        return name;
    }

    /**
     * Define __slots__ in dict with the specified slot names
     *
     * @param dict a PyObject dict
     * @param slotNames slot String names
     */
    private static void defineSlots(PyObject dict, String... slotNames) {
        PyObject[] slots = new PyObject[slotNames.length];
        for (int i = 0; i < slotNames.length; i++) {
            slots[i] = Py.newString(slotNames[i]);
        }
        dict.__setitem__("__slots__", new PyTuple(slots));
    }

    /**
     * Initialize all __slots__ arguments in the specified dict to
     * None.
     *
     * @param self a PyObject dict
     */
    private static void initSlots(PyObject self) {
        for (PyObject name : self.__findattr__("__slots__").asIterable()) {
            if (!(name instanceof PyString)) {
                continue;
            }
            self.__setattr__((PyString)name, Py.None);
        }
    }

    private static PyObject buildClass(PyObject dict, String classname,
            String superclass, String classCodeName, String doc) {
        PyObject[] sclass = Py.EmptyObjects;
        if (superclass != null) {
            sclass = new PyObject[] { dict
                    .__getitem__(new PyString(superclass)) };
        }
        PyObject cls = Py.makeClass("exceptions." + classname, sclass,
                                    Py.newJavaCode(exceptions.class, classCodeName),
                                    new PyString(doc));
        ((PyType)cls).builtin = true;
        dict.__setitem__(classname, cls);
        return cls;
    }

    public static PyObject bindStaticJavaMethod(String name, String methodName) {
        return bindStaticJavaMethod(name, exceptions.class, methodName);
    }

    public static PyObject bindStaticJavaMethod(String name, Class cls, String methodName) {
        Method javaMethod;
        try {
            javaMethod = cls.getMethod(methodName, new Class[] {PyObject.class, PyObject[].class,
                                                                String[].class});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        return new BoundStaticJavaMethod(name, javaMethod);
    }

    static class BoundStaticJavaMethod extends PyBuiltinMethod {

        /** The Java Method to be bound. Its signature must be:
         * (PyObject, PyObject[], String[])PyObject. */
        private Method javaMethod;

        public BoundStaticJavaMethod(String name, Method javaMethod) {
            super(name);
            this.javaMethod = javaMethod;
        }

        protected BoundStaticJavaMethod(PyType type, PyObject self, Info info, Method javaMethod) {
            super(type, self, info);
            this.javaMethod = javaMethod;
        }

        public PyBuiltinFunction bind(PyObject self) {
            return new BoundStaticJavaMethod(getType(), self, info, javaMethod);
        }

        public PyObject __get__(PyObject obj, PyObject type) {
            if (obj != null) {
                return bind(obj);
            }
            return makeDescriptor((PyType)type);
        }

        public PyObject __call__(PyObject[] args, String kwargs[]) {
            try {
                return Py.java2py(javaMethod.invoke(null, self, args, kwargs));
            } catch (Throwable t) {
                throw Py.JavaError(t);
            }
        }
    }
}
