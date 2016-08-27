/* Copyright (c) Jython Developers */
package org.python.core;

/**
 * Derived classes utility methods.
 */
public class Deriveds {

    /** object.__getattribute__ descriptor, cached for use by __findattr_ex__. */
    private static final PyObject objectGetattribute =
            PyObject.TYPE.__findattr__("__getattribute__");

    public static void dispatch__init__(PyObject self, PyObject[] args, String[] keywords) {
        PyType type = self.getType();
        PyObject init = type.lookup("__init__");
        if (init == null) {
            return;
        }
        PyObject result = init.__get__(self, type).__call__(args, keywords);
        if (result != Py.None) {
            throw Py.TypeError(String.format("__init__() should return None, not '%.200s'",
                                             result.getType().fastGetName()));
        }
        self.proxyInit();
    }

    /**
     * Deriveds' __findattr_ex__ implementation.
     *
     * This resides here (in org.python.core) because it manipulates PyType, and doesn't
     * call any of the Derived classes' superclass methods.
     */
    public static PyObject __findattr_ex__(PyObject self, String name) {
        PyType type = self.getType();
        PyException firstAttributeError = null;
        PyString pyName = null;

        try {
            if (type.getUsesObjectGetattribute()) {
                // Fast path: don't bother calling through the descriptor if using the
                // generic __getattribute__
                PyObject result = self.object___findattr__(name);
                if (result != null) {
                    return result;
                }
                // pass through to __getattr__
            } else {
                PyObject getattribute = type.lookup("__getattribute__");
                // This is a horrible hack for eventual consistency of the cache. We hope that the cached version
                // becomes available, but don't wait forever.
                for (int i = 0; i < 100000; i++) {
                    if (getattribute != null) {
                        break;
                    }
                    getattribute = type.lookup("__getattribute__");
                }
                if (getattribute == null) {
                    // This shouldn't happen
                    throw Py.SystemError(String.format(
                            "__getattribute__ not found on type %s. See http://bugs.jython.org/issue2487 for details.",
                            type.getName()));
                }
                if (getattribute == objectGetattribute) {
                    type.setUsesObjectGetattribute(true);
                }
                pyName = PyString.fromInterned(name);
                return getattribute.__get__(self, type).__call__(pyName);
            }
        } catch (PyException pye) {
            if (!pye.match(Py.AttributeError)) {
                throw pye;
            } else {
                // saved to avoid swallowing custom AttributeErrors, and pass through to
                // __getattr__
                firstAttributeError = pye;
            }
        }

        PyObject getattr = type.lookup("__getattr__");
        if (getattr != null) {
            return getattr.__get__(self, type).__call__(pyName != null
                                                        ? pyName : PyString.fromInterned(name));
        }
        if (firstAttributeError != null) {
            throw firstAttributeError;
        }
        return null;
    }
}
