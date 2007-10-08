package org.python.expose;

import junit.framework.TestCase;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PySystemState;

public class MethodExposerTest extends TestCase {

    public void testSimpleMethod() throws SecurityException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        System.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize();
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simple_method"));
        assertEquals("simple_method", mp.getName());
        assertEquals(SimpleExposed.class, mp.getMethodClass());
        assertEquals("org/python/expose/SimpleExposed$exposed_simple_method", mp.getInternalName());
        assertEquals("org.python.expose.SimpleExposed$exposed_simple_method", mp.getClassName());
        Class descriptor = mp.load(new BytecodeLoader.Loader());
        PyBuiltinMethod instance = (PyBuiltinMethod)descriptor.newInstance();
        assertSame("simple_method", instance.__getattr__("__name__").toString());
        SimpleExposed simpleExposed = new SimpleExposed();
        PyBuiltinFunction bound = instance.bind(simpleExposed);
        assertEquals(instance.getClass(), bound.getClass());
        assertEquals(Py.None, bound.__call__());
        assertEquals(1, simpleExposed.timesCalled);
    }
}
