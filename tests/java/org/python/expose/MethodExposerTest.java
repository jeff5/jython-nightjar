package org.python.expose;

import junit.framework.TestCase;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;

public class MethodExposerTest extends TestCase {

    public void setUp() {
        System.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize();
    }

    public PyBuiltinFunction createBound(MethodExposer me) throws Exception {
        Class descriptor = me.load(new BytecodeLoader.Loader());
        return instantiate(descriptor, me.getNames()[0]).bind(new SimpleExposed());
    }

    public PyBuiltinFunction instantiate(Class descriptor, String name) throws Exception {
        return (PyBuiltinFunction)descriptor.getConstructor(String.class).newInstance(name);
    }

    public void testSimpleMethod() throws Exception {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simple_method"));
        assertEquals("simple_method", mp.getNames()[0]);
        assertEquals(SimpleExposed.class, mp.getMethodClass());
        assertEquals("org/python/expose/SimpleExposed$exposed_simple_method", mp.getInternalName());
        assertEquals("org.python.expose.SimpleExposed$exposed_simple_method", mp.getClassName());
        Class descriptor = mp.load(new BytecodeLoader.Loader());
        PyBuiltinFunction instance = instantiate(descriptor, "simple_method");
        assertSame("simple_method", instance.__getattr__("__name__").toString());
        SimpleExposed simpleExposed = new SimpleExposed();
        PyBuiltinFunction bound = instance.bind(simpleExposed);
        assertEquals(instance.getClass(), bound.getClass());
        assertEquals(Py.None, bound.__call__());
        assertEquals(1, simpleExposed.timesCalled);
    }

    public void testPrefixing() throws Exception {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simpleexposed_prefixed"),
                                             "simpleexposed");
        assertEquals("prefixed", mp.getNames()[0]);
    }

    public void testStringReturn() throws Exception {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("toString"));
        assertEquals(Py.newString(SimpleExposed.TO_STRING_RETURN), createBound(me).__call__());
    }

    public void testBooleanReturn() throws Exception {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("__nonzero__"));
        assertEquals(Py.False, createBound(me).__call__());
    }

    public void testArgumentPassing() throws Exception {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("takesArgument",
                                                                           PyObject.class));
        PyBuiltinFunction bound = createBound(me);
        bound.__call__(Py.None);
        try {
            bound.__call__();
            fail("Need to pass an argument to takesArgument");
        } catch(Exception e) {}
    }

    public void testBinary() throws Exception {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("__add__",
                                                                           PyObject.class));
        PyBuiltinFunction bound = createBound(me);
        assertEquals(Py.NotImplemented, bound.__call__(Py.None));
        assertEquals(Py.One, bound.__call__(Py.False));
    }

    public void testCmp() throws Exception {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("__cmp__",
                                                                           PyObject.class),
                                                                           "simpleexpose");
        PyBuiltinFunction bound = createBound(me);
        try {
            bound.__call__(Py.None);
            fail("Returning -2 from __cmp__ should yield a type error");
        } catch(PyException e) {
            if(!Py.matchException(e, Py.TypeError)) {
                fail("Returning -2 from __cmp__ should yield a type error");
            }
        }
        assertEquals(Py.One, bound.__call__(Py.False));
    }
}
