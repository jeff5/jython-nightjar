package org.python.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PyBytecode extends PyBaseCode {

    // for debugging
    static int count = 0; // total number of opcodes run
    final static int maxCount = 900; // less than my buffer on iterm
    static PyObject opname;

    static synchronized PyObject getOpname() {
        if (opname == null) {
            opname = __builtin__.__import__("dis").__getattr__("opname");
        }
        return opname;
    }
    // end debugging
    public final static int CO_MAXBLOCKS = 20; // same as in CPython
    public final char[] co_code; // to avoid sign issues
    public final PyObject[] co_consts;
    public final String[] co_names;
    public final int co_stacksize; // ignored, probably shouldn't be
    public final PyObject[] co_lnotab; // ignored

    // follows new.code's interface
    public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
            String codestring, PyObject[] constants, String[] names, String varnames[],
            String filename, String name, int firstlineno, PyObject[] lnotab) {
        this(argcount, nlocals, stacksize, flags, codestring,
                constants, names, varnames, filename, name, firstlineno, lnotab,
                null, null);
    }

    public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
            String codestring, PyObject[] constants, String[] names, String varnames[],
            String filename, String name, int firstlineno, PyObject[] lnotab,
            String[] cellvars, String[] freevars) {
        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_nlocals = nlocals; // maybe assert = varnames.length;
        co_filename = filename;
        co_firstlineno = firstlineno;
        co_cellvars = cellvars;
        co_freevars = freevars;
        co_name = name;

// need to look at how this is used, since it's not part of the standard new.code interface
//        this.varargs = varargs;
//        if (varargs) {
//            co_argcount -= 1;
//            co_flags |= CO_VARARGS;
//        }
//        this.varkwargs = varkwargs;
//        if (varkwargs) {
//            co_argcount -= 1;
//            co_flags |= CO_VARKEYWORDS;
//        }
        co_flags |= flags;

        co_stacksize = stacksize;
        co_consts = constants;
        co_names = names;
        co_code = codestring.toCharArray();
        co_lnotab = lnotab;
    }
    private static final String[] __members__ = {
        "co_name", "co_argcount",
        "co_varnames", "co_filename", "co_firstlineno",
        "co_flags", "co_cellvars", "co_freevars", "co_nlocals",
        "co_code", "co_consts", "co_names", "co_lnotab", "co_stacksize"
    };

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++) {
            members[i] = new PyString(__members__[i]);
        }
        return new PyList(members);
    }


    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++) {
            if (__members__[i] == name) {
                throw Py.TypeError("readonly attribute");
            }
        }
        throw Py.AttributeError(name);
    }

    public void __setattr__(String name, PyObject value) {
        // no writable attributes
        throwReadonly(name);
    }

    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) {
            return Py.EmptyTuple;
        }
        int sz = ar.length;
        PyString[] pystr = new PyString[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyString(ar[i]);
        }
        return new PyTuple(pystr);
    }

    public PyObject __findattr_ex__(String name) {
        // have to craft co_varnames specially
        if (name == "co_varnames") {
            return toPyStringTuple(co_varnames);
        }
        if (name == "co_cellvars") {
            return toPyStringTuple(co_cellvars);
        }
        if (name == "co_freevars") {
            return toPyStringTuple(co_freevars);
        }
        if (name == "co_filename") {
            return new PyString(co_filename);
        }
        if (name == "co_name") {
            return new PyString(co_name);
        }
        return super.__findattr_ex__(name);
    }

    enum Why {

        NOT, /* No error */
        EXCEPTION, /* Exception occurred */
        RERAISE, /* Exception re-raised by 'finally' */
        RETURN, /* 'return' statement */
        BREAK, /* 'break' statement */
        CONTINUE, /* 'continue' statement */
        YIELD	/* 'yield' operator */

    };

    private Why do_raise() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Why do_raise(PyObject type) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Why do_raise(PyObject type, PyObject value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Why do_raise(PyObject type, PyObject value, PyObject traceback) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static String stringify_blocks(PyFrame f) {
        if (f.f_exits == null) return "[]";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < f.f_blockstate[0]; i++) {
            buf.append(f.f_exits[i].toString());
        }
        return buf.toString();
    }

    private static void print_debug(int next_instr, int opcode, int oparg, PyStack stack, PyFrame f) {
        System.err.println(count + "," + f.f_lasti + "> opcode: " +
                getOpname().__getitem__(Py.newInteger(opcode)) +
                (opcode > Opcode.HAVE_ARGUMENT ? ", oparg: " + oparg : "") +
                ", stack: " + stack.toString() +
                           ", blocks: " + stringify_blocks(f));
    }

    @Override
    protected PyObject interpret(PyFrame f) {
        final PyStack stack = new PyStack();
        int next_instr = -1;
        int opcode;	/* Current opcode */
        int oparg = 0; /* Current opcode argument, if any */
        Why why = Why.NOT;
        PyObject retval = null;

        while (count < maxCount) { // XXX - replace with while(true)

            next_instr += 1;
            f.f_lasti = next_instr; // should have no worries about needing co_lnotab, just keep this current
            opcode = co_code[next_instr];
            if (opcode > Opcode.HAVE_ARGUMENT) {
                next_instr += 2;
                oparg = (co_code[next_instr] << 8) + co_code[next_instr - 1];
            }
            print_debug(next_instr, opcode, oparg, stack, f);

            count += 1;

            try {
                switch (opcode) {
                    case Opcode.NOP:
                        break;

                    case Opcode.LOAD_FAST:
                        stack.push(f.getlocal(oparg));
                        break;

                    case Opcode.LOAD_CONST:
                        stack.push(co_consts[oparg]);
                        break;

                    case Opcode.STORE_FAST:
                        f.setlocal(oparg, stack.pop());
                        break;

                    case Opcode.POP_TOP:
                        stack.pop();
                        break;

                    case Opcode.ROT_TWO:
                        stack.rotN(2);
                        break;

                    case Opcode.ROT_THREE:
                        stack.rotN(3);
                        break;

                    case Opcode.ROT_FOUR:
                        stack.rotN(4);
                        break;

                    case Opcode.DUP_TOP:
                        stack.dup();
                        break;

                    case Opcode.DUP_TOPX:
                        if (oparg == 2 || oparg == 3) {
                            stack.dupN(oparg);
                        } else {
                            throw Py.RuntimeError("invalid argument to DUP_TOPX" +
                                    " (bytecode corruption?)");
                        }
                        break;

                    case Opcode.UNARY_POSITIVE:
                        stack.push(stack.pop().__pos__());
                        break;

                    case Opcode.UNARY_NEGATIVE:
                        stack.push(stack.pop().__neg__());
                        break;

                    case Opcode.UNARY_NOT:
                        stack.push(stack.pop().__not__());
                        break;

                    case Opcode.UNARY_CONVERT:
                        stack.push(stack.pop().__repr__());
                        break;

                    case Opcode.UNARY_INVERT:
                        stack.push(stack.pop().__invert__());
                        break;

                    case Opcode.BINARY_POWER: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._pow(b));
                        break;
                    }

                    case Opcode.BINARY_MULTIPLY: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._mul(b));
                        break;
                    }

                    case Opcode.BINARY_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();

                        if ((co_flags & CO_FUTUREDIVISION) == 0) {
                            stack.push(a._div(b));
                        } else {
                            stack.push(a._truediv(b));
                        }
                        break;
                    }

                    case Opcode.BINARY_TRUE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._truediv(b));
                        break;
                    }

                    case Opcode.BINARY_FLOOR_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._floordiv(b));
                        break;
                    }

                    case Opcode.BINARY_MODULO: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._mod(b));
                        break;
                    }

                    case Opcode.BINARY_ADD: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._add(b));
                        break;
                    }

                    case Opcode.BINARY_SUBTRACT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._sub(b));
                        break;
                    }

                    case Opcode.BINARY_SUBSCR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a.__getitem__(b));
                        break;
                    }

                    case Opcode.BINARY_LSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._lshift(b));
                        break;
                    }

                    case Opcode.BINARY_RSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._rshift(b));
                        break;
                    }

                    case Opcode.BINARY_AND: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._and(b));
                        break;
                    }


                    case Opcode.BINARY_XOR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._xor(b));
                        break;
                    }

                    case Opcode.BINARY_OR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._or(b));
                        break;
                    }

                    case Opcode.LIST_APPEND: {
                        PyObject b = stack.pop();
                        PyList a = (PyList) (stack.pop());
                        a.append(b);
                        break;
                    }

                    case Opcode.INPLACE_POWER: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a.__ipow__(b));
                        break;
                    }

                    case Opcode.INPLACE_MULTIPLY: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._imul(b));
                        break;
                    }

                    case Opcode.INPLACE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        if ((co_flags & CO_FUTUREDIVISION) == 0) {
                            stack.push(a._idiv(b));
                        } else {
                            stack.push(a._itruediv(b));
                        }
                        break;
                    }

                    case Opcode.INPLACE_TRUE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._itruediv(b));
                        break;
                    }

                    case Opcode.INPLACE_FLOOR_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ifloordiv(b));
                        break;
                    }

                    case Opcode.INPLACE_MODULO: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._imod(b));
                        break;
                    }

                    case Opcode.INPLACE_ADD: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._iadd(b));
                        break;
                    }

                    case Opcode.INPLACE_SUBTRACT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._isub(b));
                        break;
                    }

                    case Opcode.INPLACE_LSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ilshift(b));
                        break;
                    }

                    case Opcode.INPLACE_RSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._irshift(b));
                        break;
                    }

                    case Opcode.INPLACE_AND: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._iand(b));
                        break;
                    }

                    case Opcode.INPLACE_XOR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ixor(b));
                        break;
                    }

                    case Opcode.INPLACE_OR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ior(b));
                        break;
                    }

//
//		case SLICE+0:
//		case SLICE+1:
//		case SLICE+2:
//		case SLICE+3:
//			if ((opcode-SLICE) & 2)
//				w = POP();
//			else
//				w = NULL;
//			if ((opcode-SLICE) & 1)
//				v = POP();
//			else
//				v = NULL;
//			u = TOP();
//			x = apply_slice(u, v, w);
//			Py_DECREF(u);
//			Py_XDECREF(v);
//			Py_XDECREF(w);
//			SET_TOP(x);
//			if (x != NULL) continue;
//			break;
//
//		case Opcode.STORE_SLICE+0:
//		case Opcode.STORE_SLICE+1:
//		case Opcode.STORE_SLICE+2:
//		case Opcode.STORE_SLICE+3:
//			if (((opcode - Opcode.STORE_SLICE) & 2) !=0) {
//
//            }
//				w = POP();
//			else
//				w = NULL;
//			if ((opcode-STORE_SLICE) & 1)
//				v = POP();
//			else
//				v = NULL;
//			u = POP();
//			t = POP();
//			err = assign_slice(u, v, w, t); /* u[v:w] = t */
//			Py_DECREF(t);
//			Py_DECREF(u);
//			Py_XDECREF(v);
//			Py_XDECREF(w);
//			if (err == 0) continue;
//			break;
//
//		case DELETE_SLICE+0:
//		case DELETE_SLICE+1:
//		case DELETE_SLICE+2:
//		case DELETE_SLICE+3:
//			if ((opcode-DELETE_SLICE) & 2)
//				w = POP();
//			else
//				w = NULL;
//			if ((opcode-DELETE_SLICE) & 1)
//				v = POP();
//			else
//				v = NULL;
//			u = POP();
//			err = assign_slice(u, v, w, (PyObject *)NULL);
//							/* del u[v:w] */
//			Py_DECREF(u);
//			Py_XDECREF(v);
//			Py_XDECREF(w);
//			if (err == 0) continue;
//			break;
//
                    case Opcode.STORE_SUBSCR: {
                        PyObject key = stack.pop();
                        PyObject obj = stack.pop();
                        PyObject value = stack.pop();
                        obj.__setitem__(key, value);
                        break;
                    }

                    case Opcode.DELETE_SUBSCR: {
                        PyObject key = stack.pop();
                        PyObject obj = stack.pop();
                        obj.__delitem__(key);
                        break;
                    }

                    case Opcode.PRINT_EXPR:
                        PySystemState.displayhook(stack.pop());
                        break;

                    case Opcode.PRINT_ITEM_TO:
                        Py.printComma(stack.pop(), stack.pop());
                        break;

                    case Opcode.PRINT_ITEM:
                        Py.printComma(stack.pop());
                        break;

                    case Opcode.PRINT_NEWLINE_TO:
                        Py.printlnv(stack.pop());
                        break;

                    case Opcode.PRINT_NEWLINE:
                        Py.println();
                        break;

                    case Opcode.RAISE_VARARGS:

                        switch (oparg) {
                            case 3: {
                                PyObject tb = stack.pop();
                                PyObject value = stack.pop();
                                PyObject type = stack.pop();
                                why = do_raise(type, value, tb);
                                break;
                            }
                            case 2: {
                                PyObject value = stack.pop();
                                PyObject type = stack.pop();
                                why = do_raise(type, value);
                                break;
                            }
                            case 1: {
                                PyObject type = stack.pop();
                                why = do_raise(type);
                                break;
                            }
                            case 0:
                                why = do_raise();
                                break;
                            default:
                                throw Py.SystemError("bad RAISE_VARARGS oparg");
                        }
                        break;

                    case Opcode.LOAD_LOCALS:
                        stack.push(f.f_locals);
                        break;

                    case Opcode.RETURN_VALUE:
                        retval = stack.pop();
                        why = Why.RETURN;
                        break;

                    case Opcode.YIELD_VALUE:
                        retval = stack.pop();
                        // need to implement something like this when we reenter after a yield
//                        code.invokevirtual("org/python/core/PyFrame", "getGeneratorInput", "()" + $obj);
//                        code.dup();
//                        code.instanceof_("org/python/core/PyException");
//                        Label done2 = new Label();
//                        code.ifeq(done2);
//                        code.checkcast("java/lang/Throwable");
//                        code.athrow();
//                        code.label(done2);
//                        code.checkcast("org/python/core/PyObject");
                        why = Why.YIELD;
                        break;

                    case Opcode.EXEC_STMT: {
                        PyObject locals = stack.pop();
                        PyObject globals = stack.pop();
                        PyObject code = stack.pop();

                        if ((locals == null || locals == Py.None) &&
                                (globals == null || globals == Py.None)) {
                            throw Py.SystemError("globals and locals cannot be NULL");
                        }
                        Py.exec(code, globals, locals);

                        if (!(globals.__finditem__("__builtins__").__nonzero__())) {
                            globals.__setitem__("__builtins__", f.f_builtins);
                        }
                        break;
                    }

                    case Opcode.POP_BLOCK:
                         {
                            PyTryBlock b = (PyTryBlock) (f.f_exits[--f.f_blockstate[0]]);
                            while (stack.size() > b.b_level) {
                                stack.pop();
                            }
                        }
                        break;
//
//		case END_FINALLY:
//			v = POP();
//			if (PyInt_Check(v)) {
//				why = (enum why_code) PyInt_AS_LONG(v);
//				assert(why != WHY_YIELD);
//				if (why == WHY_RETURN ||
//				    why == WHY_CONTINUE)
//					retval = POP();
//			}
//			else if (PyExceptionClass_Check(v) || PyString_Check(v)) {
//				w = POP();
//				u = POP();
//				PyErr_Restore(v, w, u);
//				why = WHY_RERAISE;
//				break;
//			}
//			else if (v != Py_None) {
//				PyErr_SetString(PyExc_SystemError,
//					"'finally' pops bad exception");
//				why = WHY_EXCEPTION;
//			}
//			Py_DECREF(v);
//			break;
//
                    case Opcode.BUILD_CLASS: {
                        PyObject methods = stack.pop();
                        PyObject bases[] = (new PyTuple(stack.pop())).getArray();
                        String name = stack.pop().toString();
                        stack.push(Py.makeClass(name, bases, methods));
                        break;
                    }

                    case Opcode.STORE_NAME:
                        f.setlocal(oparg, stack.pop());
                        break;

                    case Opcode.DELETE_NAME:
                        f.dellocal(oparg);
                        break;

                    case Opcode.UNPACK_SEQUENCE: {
                        PyObject v = stack.pop();
                        int i = 0;
                        PyObject items[] = new PyObject[oparg];
                        for (PyObject item : v.asIterable()) {
                            items[i++] = item;
                            if (i > oparg) {
                                break;
                            }
                        }
                        for (i = i - 1; i >= 0; i--) {
                            stack.push(items[i]);
                        }
                        break;
                    }

                    case Opcode.STORE_ATTR: {
                        PyObject obj = stack.pop();
                        PyObject v = stack.pop();
                        obj.__setattr__(co_names[oparg], v);
                        break;
                    }

                    case Opcode.DELETE_ATTR:
                        stack.pop().__delattr__(co_names[oparg]);
                        break;

                    case Opcode.STORE_GLOBAL:
                        f.setglobal(co_names[oparg], stack.pop());
                        break;

                    case Opcode.DELETE_GLOBAL:
                        f.delglobal(co_names[oparg]);
                        break;

                    case Opcode.LOAD_NAME:
                        stack.push(f.getname(co_names[oparg]));
                        break;

                    case Opcode.LOAD_GLOBAL:
                        stack.push(f.getglobal(co_names[oparg]));
                        break;

                    case Opcode.DELETE_FAST:
                        f.dellocal(oparg);
                        break;

                    case Opcode.LOAD_CLOSURE:
                        stack.push(f.getclosure(oparg));
                        break;

                    case Opcode.LOAD_DEREF:
                        stack.push(f.getderef(oparg));
                        break;

                    case Opcode.STORE_DEREF:
                        f.setderef(oparg, stack.pop());
                        break;

                    case Opcode.BUILD_TUPLE:
                        stack.push(new PyTuple(stack.popN(oparg)));
                        break;

                    case Opcode.BUILD_LIST:
                        stack.push(new PyList(stack.popN(oparg)));
                        break;

                    case Opcode.BUILD_MAP:
                        stack.push(new PyDictionary());

                    case Opcode.LOAD_ATTR:
                        stack.push(stack.pop().__getattr__(co_names[oparg]));
                        break;

                    case Opcode.COMPARE_OP: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();

                        switch (oparg) {

                            case Opcode.PyCmp_LT:
                                stack.push(a._lt(b));
                                break;
                            case Opcode.PyCmp_LE:
                                stack.push(a._le(b));
                                break;
                            case Opcode.PyCmp_EQ:
                                stack.push(a._eq(b));
                                break;
                            case Opcode.PyCmp_NE:
                                stack.push(a._ne(b));
                                break;
                            case Opcode.PyCmp_GT:
                                stack.push(a._gt(b));
                                break;
                            case Opcode.PyCmp_GE:
                                stack.push(a._ge(b));
                                break;
                            case Opcode.PyCmp_IN:
                                stack.push(a._in(b));
                                break;
                            case Opcode.PyCmp_NOT_IN:
                                stack.push(a._notin(b));
                                break;
                            case Opcode.PyCmp_IS:
                                stack.push(a._is(b));
                                break;
                            case Opcode.PyCmp_IS_NOT:
                                stack.push(a._isnot(b));
                                break;
                            case Opcode.PyCmp_EXC_MATCH:
                                stack.push(Py.newBoolean(Py.matchException(new PyException(a), b)));
                                break;

                        }
                        break;
                    }

                    case Opcode.IMPORT_NAME: {
                        PyObject __import__ = f.f_builtins.__finditem__("__import__");
                        if (__import__ == null) {
                            throw Py.ImportError("__import__ not found");
                        }
                        PyString name = Py.newString(co_names[oparg]);
                        PyObject fromlist = stack.pop();
                        PyObject level = stack.pop();

                        if (level.asInt() != -1) {
                            stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist, level}));
                        } else {
                            stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist}));
                        }
                        break;
                    }

                    case Opcode.IMPORT_STAR: {
                        String module = stack.pop().toString();
                        imp.importAll(module, f);
                        break;
                    }

                    case Opcode.IMPORT_FROM:
                        String name = co_names[oparg];
                        try {
                            stack.push(stack.pop().__getattr__(name));

                        } catch (PyException pye) {
                            if (Py.matchException(pye, Py.AttributeError)) {
                                throw Py.ImportError(String.format("cannot import name %.230s", name));
                            } else {
                                throw pye;
                            }
                        }
                        break;

                    case Opcode.JUMP_FORWARD:
                        next_instr += oparg;
                        break;

                    case Opcode.JUMP_IF_FALSE:
                        if (!stack.top().__nonzero__()) {
                            next_instr += oparg;
                        }
                        break;

                    case Opcode.JUMP_IF_TRUE:
                        if (stack.top().__nonzero__()) {
                            next_instr += oparg;
                        }
                        break;

                    case Opcode.JUMP_ABSOLUTE:
                        next_instr = oparg - 1; // XXX - continue to a label is probably much better
                        break;

                    case Opcode.GET_ITER: {
                        PyObject it = stack.top().__iter__();
                        if (it != null) {
                            stack.set_top(it);
                        }
                        break;
                    }

                    case Opcode.FOR_ITER: {
                        PyObject it = stack.pop();
                        try {
                            PyObject x = it.__iternext__();
                            if (x != null) {
                                stack.push(it);
                                stack.push(x);
                                break;
                            }
                        } catch (PyException pye) {
                            if (!Py.matchException(pye, Py.StopIteration)) {
                                throw pye;
                            }
                        }
                        next_instr += oparg;
                        break;
                    }

                    case Opcode.BREAK_LOOP:
                        why = Why.BREAK;
                        break;

                    case Opcode.CONTINUE_LOOP:
                        retval = Py.newInteger(oparg);
                        if (retval.__nonzero__()) {
                            why = Why.CONTINUE;
                        }
                        break;

                    case Opcode.SETUP_LOOP:
                    case Opcode.SETUP_EXCEPT:
                    case Opcode.SETUP_FINALLY: {
                        if (f.f_exits == null) { // allocate in the frame where they can fit! consider supporting directly in the frame
                            f.f_exits = new PyObject[CO_MAXBLOCKS]; // f_blockstack in CPython - a simple ArrayList might be best
                            f.f_blockstate = new int[]{0};        // f_iblock in CPython
                        }
                        f.f_exits[f.f_blockstate[0]++] = new PyTryBlock(opcode, next_instr + oparg, stack.size());
                        break;
                    }
//
//		case WITH_CLEANUP:
//		{
//			/* TOP is the context.__exit__ bound method.
//			   Below that are 1-3 values indicating how/why
//			   we entered the finally clause:
//			   - SECOND = None
//			   - (SECOND, THIRD) = (WHY_{RETURN,CONTINUE}), retval
//			   - SECOND = WHY_*; no retval below it
//			   - (SECOND, THIRD, FOURTH) = exc_info()
//			   In the last case, we must call
//			     TOP(SECOND, THIRD, FOURTH)
//			   otherwise we must call
//			     TOP(None, None, None)
//
//			   In addition, if the stack represents an exception,
//			   *and* the function call returns a 'true' value, we
//			   "zap" this information, to prevent END_FINALLY from
//			   re-raising the exception.  (But non-local gotos
//			   should still be resumed.)
//			*/
//
//			x = TOP();
//			u = SECOND();
//			if (PyInt_Check(u) || u == Py_None) {
//				u = v = w = Py_None;
//			}
//			else {
//				v = THIRD();
//				w = FOURTH();
//			}
//			/* XXX Not the fastest way to call it... */
//			x = PyObject_CallFunctionObjArgs(x, u, v, w, NULL);
//			if (x == NULL)
//				break; /* Go to error exit */
//			if (u != Py_None && PyObject_IsTrue(x)) {
//				/* There was an exception and a true return */
//				Py_DECREF(x);
//				x = TOP(); /* Again */
//				STACKADJ(-3);
//				Py_INCREF(Py_None);
//				SET_TOP(Py_None);
//				Py_DECREF(x);
//				Py_DECREF(u);
//				Py_DECREF(v);
//				Py_DECREF(w);
//			} else {
//				/* Let END_FINALLY do its thing */
//				Py_DECREF(x);
//				x = POP();
//				Py_DECREF(x);
//			}
//			break;
//		}
//
                    case Opcode.CALL_FUNCTION: {
                        int na = oparg & 0xff;
                        int nk = (oparg >> 8) & 0xff;
                        int n = na + 2 * nk;

                        PyObject args[] = stack.popN(na);
                        PyObject callable = stack.pop();
                        System.err.println("__call__:" + callable + "," + Arrays.toString(args));
                        stack.push(callable.__call__(args));
                        break;
                    }

//
//		case CALL_FUNCTION_VAR:
//		case CALL_FUNCTION_KW:
//		case CALL_FUNCTION_VAR_KW:
//		{
//		    int na = oparg & 0xff;
//		    int nk = (oparg>>8) & 0xff;
//		    int flags = (opcode - CALL_FUNCTION) & 3;
//		    int n = na + 2 * nk;
//		    PyObject **pfunc, *func, **sp;
//		    PCALL(PCALL_ALL);
//		    if (flags & CALL_FLAG_VAR)
//			    n++;
//		    if (flags & CALL_FLAG_KW)
//			    n++;
//		    pfunc = stack_pointer - n - 1;
//		    func = *pfunc;
//
//		    if (PyMethod_Check(func)
//			&& PyMethod_GET_SELF(func) != NULL) {
//			    PyObject *self = PyMethod_GET_SELF(func);
//			    Py_INCREF(self);
//			    func = PyMethod_GET_FUNCTION(func);
//			    Py_INCREF(func);
//			    Py_DECREF(*pfunc);
//			    *pfunc = self;
//			    na++;
//			    n++;
//		    } else
//			    Py_INCREF(func);
//		    sp = stack_pointer;
//		    READ_TIMESTAMP(intr0);
//		    x = ext_do_call(func, &sp, flags, na, nk);
//		    READ_TIMESTAMP(intr1);
//		    stack_pointer = sp;
//		    Py_DECREF(func);
//
//		    while (stack_pointer > pfunc) {
//			    w = POP();
//			    Py_DECREF(w);
//		    }
//		    PUSH(x);
//		    if (x != NULL)
//			    continue;
//		    break;
//		}
//
                    case Opcode.MAKE_FUNCTION: {
                        PyCode code = (PyCode) stack.pop();
                        PyObject[] defaults = stack.popN(oparg);
                        PyFunction func = new PyFunction(f.f_globals, defaults, code);
                        stack.push(func);
                        break;
                    }

                    case Opcode.MAKE_CLOSURE: {
                        PyCode code = (PyCode) stack.pop();
                        PyObject[] closure_cells = new PyTuple(stack.pop()).getArray();
                        PyObject[] defaults = stack.popN(oparg);
                        PyFunction func = new PyFunction(f.f_globals, defaults, code, closure_cells);
                        stack.push(func);
                        break;
                    }

//		case BUILD_SLICE:
//			if (oparg == 3)
//				w = POP();
//			else
//				w = NULL;
//			v = POP();
//			u = TOP();
//			x = PySlice_New(u, v, w);
//			Py_DECREF(u);
//			Py_DECREF(v);
//			Py_XDECREF(w);
//			SET_TOP(x);
//			if (x != NULL) continue;
//			break;
//
                    case Opcode.EXTENDED_ARG:
                        opcode = co_code[next_instr++];
                        next_instr += 2;
                        oparg = oparg << 16 | ((co_code[next_instr - 1] << 8) + (co_code[next_instr - 2]));
                        break;

                    default:
                        Py.print(Py.getSystemState().stderr,
                                Py.newString(
                                String.format("XXX lineno: %d, opcode: %d\n",
                                f.f_lasti, opcode)));
                        throw Py.SystemError("unknown opcode");


                } // end switch
            } // end try
            catch (Throwable t) {
                // wrap so we can consume with our try block handling
                PyException pye = Py.JavaError(t);
                // XXX - but since that hasn't been written, for now just rethrow in wrapped form
                throw pye;
            }


            // process why, blocks
            switch (why) {
                case RETURN:
                    return retval;
                default:
                    break;
            }
        } // end while
        return Py.None; // should never get here, just for debugging
    }

    // XXX - perhaps add support for max stack size (presumably from co_stacksize)
    // and capacity hints
    private class PyStack {

        final List<PyObject> stack;

        PyStack() {
            stack = new ArrayList<PyObject>();
        }

        PyObject top() {
            return stack.get(stack.size() - 1);
        }

        PyObject pop() {
            return stack.remove(stack.size() - 1);
        }

        void push(PyObject v) {
            stack.add(v);
        }

        void set_top(PyObject v) {
            stack.set(stack.size() - 1, v);
        }

        void dup() {
            stack.add(top());
        }

        void dupN(int n) {
            PyObject v = top();
            for (int i = 0; i < n; i++) {
                stack.add(v);
            }
        }

        PyObject[] popN(int n) {
            int end = stack.size(); // exclusive
            PyObject ret[] = new PyObject[n];
            List<PyObject> lastN = stack.subList(end - n, end);
            lastN.toArray(ret);
            lastN.clear();
            return ret;
        }

        void rotN(int n) {
            int end = stack.size();
            List<PyObject> lastN = stack.subList(end - n, end);
            Collections.rotate(lastN, n);
        }

        int size() {
            return stack.size();
        }

        @Override
        public String toString() {
            return stack.toString();
        }
    }

    private class PyTryBlock extends PyObject { // purely to sit on top of the existing PyFrame in f_exits!!!

        int b_type;			/* what kind of block this is */

        int b_handler;		/* where to jump to find handler */

        int b_level;		/* value stack level to pop to */


        PyTryBlock(int type, int handler, int level) {
            b_type = type;
            b_handler = handler;
            b_level = level;
        }

        @Override
        public String toString() {
            return "[" + getOpname().__getitem__(Py.newInteger(b_type)) + "," +
                    b_handler + "," + b_level + "]";
        }
    }
}


