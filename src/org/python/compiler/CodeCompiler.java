// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.python.objectweb.asm.ClassWriter;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.core.CompilerFlags;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.parser.ParseException;
import org.python.parser.SimpleNode;
import org.python.parser.Visitor;
import org.python.parser.ast.Assert;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.AugAssign;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.BoolOp;
import org.python.parser.ast.Break;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.Continue;
import org.python.parser.ast.Delete;
import org.python.parser.ast.Dict;
import org.python.parser.ast.Ellipsis;
import org.python.parser.ast.Exec;
import org.python.parser.ast.Expr;
import org.python.parser.ast.Expression;
import org.python.parser.ast.ExtSlice;
import org.python.parser.ast.For;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Global;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Index;
import org.python.parser.ast.Interactive;
import org.python.parser.ast.Lambda;
import org.python.parser.ast.List;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Name;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Print;
import org.python.parser.ast.Raise;
import org.python.parser.ast.Repr;
import org.python.parser.ast.Return;
import org.python.parser.ast.Slice;
import org.python.parser.ast.Str;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.Suite;
import org.python.parser.ast.TryExcept;
import org.python.parser.ast.TryFinally;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.Unicode;
import org.python.parser.ast.While;
import org.python.parser.ast.Yield;
import org.python.parser.ast.excepthandlerType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.expr_contextType;
import org.python.parser.ast.keywordType;
import org.python.parser.ast.listcompType;
import org.python.parser.ast.modType;
import org.python.parser.ast.stmtType;

public class CodeCompiler extends Visitor implements Opcodes, ClassConstants //, PythonGrammarTreeConstants
{

    public static final Object Exit=new Integer(1);
    public static final Object NoExit=null;

    public static final int GET=0;
    public static final int SET=1;
    public static final int DEL=2;
    public static final int AUGGET=3;
    public static final int AUGSET=4;

    public Module module;
    public ClassWriter cw;
    public Code mv;
    public CodeCompiler mrefs;
    public CompilerFlags cflags;

    int temporary;
    int augmode;
    int augtmp1;
    int augtmp2;
    int augtmp3;
    int augtmp4;

    public boolean fast_locals, print_results;

    public Hashtable tbl;
    public ScopeInfo my_scope;

    boolean optimizeGlobals = true;
    public Vector names;
    public String className;

    public Stack continueLabels, breakLabels;
    public Stack exceptionHandlers;
    public Vector yields = new Vector();

    /* break/continue finally's level.
     * This is the lowest level in the exceptionHandlers which should
     * be executed at break or continue.
     * It is saved/updated/restored when compiling loops.
     * A similar level for returns is not needed because a new CodeCompiler
     * is used for each PyCode, ie. each 'function'.
     * When returning through finally's all the exceptionHandlers are executed.
     */
    public int bcfLevel = 0;

    public CodeCompiler(Module module, boolean print_results) {
        this.module = module;
        this.print_results = print_results;

        mrefs = this;
        cw = module.classfile.cw;

        continueLabels = new Stack();
        breakLabels = new Stack();
        exceptionHandlers = new Stack();
    }

    public int PyNone;
    public void getNone() throws IOException {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/python/core/Py", "None", $pyObj);
    }

    public void loadFrame() throws Exception {
        mv.visitVarInsn(ALOAD, 1);
    }

    public void setLastI(int idx) throws Exception {
        loadFrame();
        mv.iconst(idx);
        mv.visitFieldInsn(PUTFIELD, "org/python/core/PyFrame", "f_lasti", "I");
    }
    
    private void loadf_back() throws Exception {
        mv.visitFieldInsn(GETFIELD, "org/python/core/PyFrame", "f_back", $pyFrame);
    }

    public int storeTop() throws Exception {
        int tmp = mv.getLocal("org/python/core/PyObject");
        mv.visitVarInsn(ASTORE, tmp);
        return tmp;
    }

    public int setline;
    public void setline(int line) throws Exception {
        if (module.linenumbers) {
            //FJW mv.setline(line);
            loadFrame();
            mv.iconst(line);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setline", "(I)V");
        }
    }

    public void setline(SimpleNode node) throws Exception {
        setline(node.beginLine);
    }

    public void set(SimpleNode node) throws Exception {
        int tmp = storeTop();
        set(node, tmp);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, tmp);
        mv.freeLocal(tmp);
    }


    boolean inSet = false;
    public void set(SimpleNode node, int tmp) throws Exception {
        if (inSet) {
            System.err.println("recurse set: "+tmp+", "+temporary);
        }
        temporary = tmp;
        visit(node);
    }


    private void saveAugTmps(SimpleNode node, int count) throws Exception {
        if (count >= 4) {
            augtmp4 = mv.getLocal("org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, augtmp4);
        }
        if (count >= 3) {
            augtmp3 = mv.getLocal("org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, augtmp3);
        }
        if (count >= 2) {
            augtmp2 = mv.getLocal("org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, augtmp2);
        }
        augtmp1 = mv.getLocal("org/python/core/PyObject");
        mv.visitVarInsn(ASTORE, augtmp1);

        mv.visitVarInsn(ALOAD, augtmp1);
        if (count >= 2)
            mv.visitVarInsn(ALOAD, augtmp2);
        if (count >= 3)
            mv.visitVarInsn(ALOAD, augtmp3);
        if (count >= 4)
            mv.visitVarInsn(ALOAD, augtmp4);
    }


    private void restoreAugTmps(SimpleNode node, int count) throws Exception {
       mv.visitVarInsn(ALOAD, augtmp1);
       mv.freeLocal(augtmp1);
       if (count == 1)
           return;
       mv.visitVarInsn(ALOAD, augtmp2);
       mv.freeLocal(augtmp2);
       if (count == 2)
           return;
       mv.visitVarInsn(ALOAD, augtmp3);
       mv.freeLocal(augtmp3);
       if (count == 3)
           return;
       mv.visitVarInsn(ALOAD, augtmp4);
       mv.freeLocal(augtmp4);
   }


    public void parse(modType node, Code mv,
                      boolean fast_locals, String className,
                      boolean classBody, ScopeInfo scope, CompilerFlags cflags)
        throws Exception
    {
        this.fast_locals = fast_locals;
        this.className = className;
        this.mv = mv;
        this.cflags = cflags;

        my_scope = scope;
        names = scope.names;

        tbl = scope.tbl;
        optimizeGlobals = fast_locals&&!scope.exec&&!scope.from_import_star;

        Object exit = visit(node);

        if (classBody) {
            loadFrame();
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getf_locals", "()" + $pyObj);
            mv.visitInsn(ARETURN);
        } else {
            if (exit == null) {
                setLastI(-1);

                getNone();
                mv.visitInsn(ARETURN);
            }
        }
    }

    public Object visitInteractive(Interactive node) throws Exception {
        traverse(node);
        return null;
    }

    public Object visitModule(org.python.parser.ast.Module suite)
        throws Exception
    {
        if (suite.body.length > 0 &&
            suite.body[0] instanceof Expr &&
            ((Expr)suite.body[0]).value instanceof Str)
        {
            loadFrame();
            mv.visitLdcInsn("__doc__");
            visit(((Expr) suite.body[0]).value);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setglobal", "(" +$str + $pyObj + ")V");
        }
        if (module.setFile) {
            loadFrame();
            mv.visitLdcInsn("__file__");
            module.filename.get(mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setglobal", "(" +$str + $pyObj + ")V");
        }
        traverse(suite);
        return null;
    }

    public Object visitExpression(Expression node) throws Exception {
        if (my_scope.generator && node.body != null) {
            module.error("'return' with argument inside generator",
                         true, node);
        }
        return visitReturn(new Return(node.body, node), true);
    }

    public int EmptyObjects;
    public void makeArray(SimpleNode[] nodes) throws Exception {
        int n;

        if (nodes == null)
            n = 0;
        else
            n = nodes.length;

        if (n == 0) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "org/python/core/Py", "EmptyObjects", $pyObjArr);
        } else {
            int tmp = mv.getLocal("[org/python/core/PyObject");
            mv.iconst(n);
            mv.visitTypeInsn(ANEWARRAY, "org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, tmp);

            for(int i=0; i<n; i++) {
                mv.visitVarInsn(ALOAD, tmp);
                mv.iconst(i);
                visit(nodes[i]);
                mv.visitInsn(AASTORE);
            }
            mv.visitVarInsn(ALOAD, tmp);
            mv.freeLocal(tmp);
        }
    }

    public void getDocString(stmtType[] suite) throws Exception {
        if (suite.length > 0 && suite[0] instanceof Expr &&
            ((Expr) suite[0]).value instanceof Str)
        {
            visit(((Expr) suite[0]).value);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
    }

    int getclosure;

    public boolean makeClosure(ScopeInfo scope) throws Exception {
        if (scope == null || scope.freevars == null) return false;
        int n = scope.freevars.size();
        if (n == 0) return false;

        int tmp = mv.getLocal("[org/python/core/PyObject");
        mv.iconst(n);
        mv.visitTypeInsn(ANEWARRAY, "org/python/core/PyObject");
        mv.visitVarInsn(ASTORE, tmp);
        Hashtable upTbl = scope.up.tbl;
        for(int i=0; i<n; i++) {
            mv.visitVarInsn(ALOAD, tmp);
            mv.iconst(i);
            loadFrame();
            for(int j = 1; j < scope.distance; j++) {
                loadf_back();
            }
            SymInfo symInfo = (SymInfo)upTbl.get(scope.freevars.elementAt(i));
            mv.iconst(symInfo.env_index);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getclosure", "(I)" + $pyObj);
            mv.visitInsn(AASTORE);
        }

        mv.visitVarInsn(ALOAD, tmp);
        mv.freeLocal(tmp);

        return true;
    }



    int f_globals, PyFunction_init, PyFunction_closure_init;

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        String name = getName(node.name);

        setline(node);

        mv.visitTypeInsn(NEW, "org/python/core/PyFunction");
        mv.visitInsn(DUP);
        loadFrame();
        mv.visitFieldInsn(GETFIELD, "org/python/core/PyFrame", "f_globals", $pyObj);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        scope.setup_closure();
        scope.dump();
        module.PyCode(new Suite(node.body, node), name, true,
                      className, false, false,
                      node.beginLine, scope, cflags).get(mv);

        getDocString(node.body);

        if (!makeClosure(scope)) {
            mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + ")V");
        } else {
            mv.visitMethodInsn(INVOKESPECIAL,  "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")V");
        }

        set(new Name(node.name, Name.Store, node));
        return null;
    }

    public int printResult;

    public Object visitExpr(Expr node) throws Exception {
        setline(node);
        visit(node.value);

        if (print_results) {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "printResult", "(" + $pyObj + ")V");
        } else {
	        mv.visitInsn(POP);
        }
        return null;
    }

    public Object visitAssign(Assign node) throws Exception  {
        setline(node);
        visit(node.value);
        if (node.targets.length == 1) {
            set(node.targets[0]);
            return null;
        }
        int tmp = storeTop();
        for (int i=node.targets.length-1; i>=0; i--) {
            set(node.targets[i], tmp);
        }
        mv.freeLocal(tmp);
        return null;
    }

    public Object visitPrint(Print node) throws Exception {
        setline(node);
        int tmp = -1;

        if (node.dest != null) {
            visit(node.dest);
            tmp = storeTop();
        }
        if (node.values == null || node.values.length == 0) {
            if (node.dest != null) {
                mv.visitVarInsn(ALOAD, tmp);
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "printlnv", "(" + $pyObj + ")V");
            } else {
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "println", "()V");
            }
        } else {
            for (int i = 0; i < node.values.length; i++) {
                if (node.dest != null) {
                    mv.visitVarInsn(ALOAD, tmp);
                    visit(node.values[i]);
                    if (node.nl && i == node.values.length - 1) {
                        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "println", "(" + $pyObj + $pyObj + ")V");
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "printComma", "(" + $pyObj + $pyObj + ")V");
                    }
                } else {
                    visit(node.values[i]);
                    if (node.nl && i == node.values.length - 1) {
                        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "println", "(" + $pyObj + ")V");
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "printComma", "(" + $pyObj + ")V");
                    }

                }
            }
        }
        if (node.dest != null) {
            mv.freeLocal(tmp);
        }
        return null;
    }

    public Object visitDelete(Delete node) throws Exception {
        setline(node);
        traverse(node);
        return null;
    }

    public Object visitPass(Pass node) throws Exception {
        setline(node);
        return null;
    }

    public Object visitBreak(Break node) throws Exception {
        //setline(node); Not needed here...
        if (breakLabels.empty()) {
            throw new ParseException("'break' outside loop", node);
        }

        doFinallysDownTo(bcfLevel);

        mv.visitJumpInsn(GOTO, (Label)breakLabels.peek());
        return null;
    }

    public Object visitContinue(Continue node) throws Exception {
        //setline(node); Not needed here...
        if (continueLabels.empty()) {
            throw new ParseException("'continue' not properly in loop", node);
        }

        doFinallysDownTo(bcfLevel);

        mv.visitJumpInsn(GOTO, (Label)continueLabels.peek());
        return Exit;
    }

    int yield_count = 0;

    int f_savedlocals;

    public Object visitYield(Yield node) throws Exception {
        setline(node);
        if (!fast_locals) {
            throw new ParseException("'yield' outside function", node);
        }

        if (inFinallyBody()) {
            throw new ParseException("'yield' not allowed in a 'try' "+
                                     "block with a 'finally' clause", node);
        }

        saveLocals();
        visit(node.value);
        setLastI(++yield_count);
        mv.visitInsn(ARETURN);

        Label restart = new Label();
        yields.addElement(restart);
        mv.visitLabel(restart);
        restoreLocals();
        return null;
    }

    private boolean inFinallyBody() {
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            if (handler.isFinallyHandler()) {
                return true;
            }
        }
        return false;
    }
 
    private void restoreLocals() throws Exception {
        endExceptionHandlers();

        Vector v = mv.getActiveLocals();

        loadFrame();
        mv.visitFieldInsn(GETFIELD, "org/python/core/PyFrame", "f_savedlocals", "[Ljava/lang/Object;");

        int locals = mv.getLocal("[java/lang/Object");
        mv.visitVarInsn(ASTORE, locals);

        for (int i = 0; i < v.size(); i++) {
            String type = (String) v.elementAt(i);
            if (type == null)
                continue;
            mv.visitVarInsn(ALOAD, locals);
            mv.iconst(i);
            mv.visitInsn(AALOAD);
            mv.visitTypeInsn(CHECKCAST, type);
            mv.visitVarInsn(ASTORE, i);
        }
        mv.freeLocal(locals);

        restartExceptionHandlers();
    }

    /**
     *  Close all the open exception handler ranges.  This should be paired
     *  with restartExceptionHandlers to delimit internal code that
     *  shouldn't be handled by user handlers.  This allows us to set 
     *  variables without the verifier thinking we might jump out of our
     *  handling with an exception.
     */
    private void endExceptionHandlers()
    {
        Label end = new Label();
        mv.visitLabel(end);
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            handler.exceptionEnds.addElement(end);
        }
    }

    private void restartExceptionHandlers()
    {
        Label start = new Label();
        mv.visitLabel(start);
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            handler.exceptionStarts.addElement(start);
        }
    }

    private void saveLocals() throws Exception {
        Vector v = mv.getActiveLocals();
        mv.iconst(v.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int locals = mv.getLocal("[java/lang/Object");
        mv.visitVarInsn(ASTORE, locals);

        for (int i = 0; i < v.size(); i++) {
            String type = (String) v.elementAt(i);
            if (type == null)
                continue;
            mv.visitVarInsn(ALOAD, locals);
            mv.iconst(i);
            //code.checkcast(code.pool.Class("java/lang/Object"));
            if (i == 2222) {
                mv.visitInsn(ACONST_NULL);
            } else
                mv.visitVarInsn(ALOAD, i);
            mv.visitInsn(AASTORE);
        }

        loadFrame();
        mv.visitVarInsn(ALOAD, locals);
        mv.visitFieldInsn(PUTFIELD, "org/python/core/PyFrame", "f_savedlocals", "[Ljava/lang/Object;");
        mv.freeLocal(locals);
    }


    public Object visitReturn(Return node) throws Exception {
        return visitReturn(node, false);
    }

    public Object visitReturn(Return node, boolean inEval) throws Exception {
        setline(node);
        if (!inEval && !fast_locals) {
            throw new ParseException("'return' outside function", node);
        }
        int tmp = 0;
        if (node.value != null) {
            if (my_scope.generator)
                throw new ParseException("'return' with argument " +
                                         "inside generator", node);
            visit(node.value);
            tmp = mv.getReturnLocal();
            mv.visitVarInsn(ASTORE, tmp);
        }
        doFinallysDownTo(0);

        setLastI(-1);

        if (node.value != null) {
            mv.visitVarInsn(ALOAD, tmp);
        } else {
            getNone();
        }
        mv.visitInsn(ARETURN);
        return Exit;
    }

    public int makeException0, makeException1, makeException2, makeException3;

    public Object visitRaise(Raise node) throws Exception {
        setline(node);
        traverse(node);
        if (node.type == null) {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeException", "()" + $pyExc);
        } else if (node.inst == null) {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeException", "(" + $pyObj + ")" + $pyExc);
        } else if (node.tback == null) {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + ")" + $pyExc);
        } else {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyExc);
        }
        mv.visitInsn(ATHROW);
        return Exit;
    }

    public int importOne, importOneAs;

    public Object visitImport(Import node) throws Exception {
        setline(node);
        for (int i = 0; i < node.names.length; i++) {
            String asname = null;
            if (node.names[i].asname != null) {
                String name = node.names[i].name;
                asname = node.names[i].asname;
                mv.visitLdcInsn(name);
                loadFrame();
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/imp", "importOneAs", "(" + $str + $pyFrame + ")" + $pyObj);
            } else {
                String name = node.names[i].name;
                asname = name;
                if (asname.indexOf('.') > 0)
                    asname = asname.substring(0, asname.indexOf('.'));
                mv.visitLdcInsn(name);
                loadFrame();
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/imp", "importOne", "(" + $str + $pyFrame + ")" + $pyObj);
            }
            set(new Name(asname, Name.Store, node));
        }
        return null;
    }


    public int importAll, importFrom;

    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        setline(node);
        mv.visitLdcInsn(node.module);
        if (node.names.length > 0) {
            String[] names = new String[node.names.length];
            String[] asnames = new String[node.names.length];
            for (int i = 0; i < node.names.length; i++) {
                names[i] = node.names[i].name;
                asnames[i] = node.names[i].asname;
                if (asnames[i] == null)
                    asnames[i] = names[i];
            }
            makeStrings(mv, names, names.length);

            loadFrame();
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/imp", "importFrom", "(" + $str + $strArr + $pyFrame + ")" + $pyObjArr);
            int tmp = storeTop();
            for (int i = 0; i < node.names.length; i++) {
                mv.visitVarInsn(ALOAD, tmp);
                mv.iconst(i);
                mv.visitInsn(AALOAD);
                set(new Name(asnames[i], Name.Store, node));
            }
            mv.freeLocal(tmp);
        } else {
            loadFrame();
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/imp", "importAll", "(" + $str + $pyFrame + ")V");
        }
        return null;
    }

    public Object visitGlobal(Global node) throws Exception {
        return null;
    }

    public int exec;
    public Object visitExec(Exec node) throws Exception {
        setline(node);
        visit(node.body);

        if (node.globals != null) {
            visit(node.globals);
        } else {
            mv.visitInsn(ACONST_NULL);
        }

        if (node.locals != null) {
            visit(node.locals);
        } else {
            mv.visitInsn(ACONST_NULL);
        }

        //do the real work here
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "exec", "(" + $pyObj + $pyObj + $pyObj + ")V");
        return null;
    }

    public int asserttype;
    public Object visitAssert(Assert node) throws Exception {
        setline(node);
        Label end_of_assert = new Label();
        
        /* First do an if __debug__: */
        loadFrame();
        emitGetGlobal("__debug__");

        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");

        mv.visitJumpInsn(IFEQ, end_of_assert);

        /* Now do the body of the assert. If PyObject.__nonzero__ is true,
           then the assertion succeeded, the message portion should not be
           processed. Otherwise, the message will be processed. */
        visit(node.test);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");

        /* If evaluation is false, then branch to end of method */
        mv.visitJumpInsn(IFNE, end_of_assert);
        
        /* Push exception type onto stack(Py.AssertionError) */
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/python/core/Py", "AssertionError", "Lorg/python/core/PyObject;");

        /* Visit the message part of the assertion, or pass Py.None */
        if( node.msg != null ) {
             visit(node.msg);
        } else {
            getNone(); 
        }
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + ")" + $pyExc);
        
        /* Raise assertion error. Only executes this logic if assertion
           failed */
        mv.visitInsn(ATHROW);
 
        /* And finally set the label for the end of it all */
        mv.visitLabel(end_of_assert);

        return null;
    }

    public int nonzero;
    public Object doTest(Label end_of_if, If node, int index)
        throws Exception
    {
        Label end_of_suite = new Label();

        setline(node.test);
        visit(node.test);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");

        mv.visitJumpInsn(IFEQ, end_of_suite);

        Object exit = suite(node.body);

        if (end_of_if != null && exit == null)
            mv.visitJumpInsn(GOTO, end_of_if);

        mv.visitLabel(end_of_suite);

        if (node.orelse != null) {
            return suite(node.orelse) != null ? exit : null;
        } else {
            return null;
        }
    }

    public Object visitIf(If node) throws Exception {
        Label end_of_if = null;
        if (node.orelse != null)
            end_of_if = new Label();

        Object exit = doTest(end_of_if, node, 0);
        if (end_of_if != null)
            mv.visitLabel(end_of_if);
        return exit;
    }

    public int beginLoop() {
        continueLabels.push(new Label());
        breakLabels.push(new Label());
        int savebcf = bcfLevel;
        bcfLevel = exceptionHandlers.size();
        return savebcf;
    }

    public void finishLoop(int savebcf) {
        continueLabels.pop();
        breakLabels.pop();
        bcfLevel = savebcf;
    }


    public Object visitWhile(While node) throws Exception {
        int savebcf = beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();

        Label start_loop = new Label();

        mv.visitJumpInsn(GOTO, continue_loop);
        mv.visitLabel(start_loop);

        //Do suite
        suite(node.body);

        mv.visitLabel(continue_loop);
        setline(node);

        //Do test
        visit(node.test);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");
        mv.visitJumpInsn(IFNE, start_loop);

        finishLoop(savebcf);

        if (node.orelse != null) {
            //Do else
            suite(node.orelse);
        }
        mv.visitLabel(break_loop);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int iter=0;
    public int iternext=0;

    public Object visitFor(For node) throws Exception {
        int savebcf = beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();
        Label start_loop = new Label();
        Label next_loop = new Label();

        int iter_tmp = mv.getLocal("org/python/core/PyObject");
        int expr_tmp = mv.getLocal("org/python/core/PyObject");

        setline(node);

        //parse the list
        visit(node.iter);

        //set up the loop iterator
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__iter__", "()Lorg/python/core/PyObject;");
        mv.visitVarInsn(ASTORE, iter_tmp);

        //do check at end of loop.  Saves one opcode ;-)
        mv.visitJumpInsn(GOTO, next_loop);

        mv.visitLabel(start_loop);
        //set iter variable to current entry in list
        set(node.target, expr_tmp);

        //evaluate for body
        suite(node.body);

        mv.visitLabel(continue_loop);

        mv.visitLabel(next_loop);
        setline(node);
        //get the next element from the list
        mv.visitVarInsn(ALOAD, iter_tmp);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__iternext__", "()" + $pyObj);

        mv.visitVarInsn(ASTORE, expr_tmp);
        mv.visitVarInsn(ALOAD, expr_tmp);
        //if no more elements then fall through
        mv.visitJumpInsn(IFNONNULL, start_loop);

        finishLoop(savebcf);

        if (node.orelse != null) {
            //Do else clause if provided
            suite(node.orelse);
        }

        mv.visitLabel(break_loop);

        mv.freeLocal(iter_tmp);
        mv.freeLocal(expr_tmp);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int match_exception;

    public void exceptionTest(int exc, Label end_of_exceptions,
                              TryExcept node, int index)
        throws Exception
    {
        for (int i = 0; i < node.handlers.length; i++) {
            excepthandlerType handler = node.handlers[i];

            //setline(name);
            Label end_of_self = new Label();

            if (handler.type != null) {
                mv.visitVarInsn(ALOAD, exc);
                //get specific exception
                visit(handler.type);
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "matchException", "(" + $pyExc + $pyObj + ")Z");
                mv.visitJumpInsn(IFEQ, end_of_self);
            } else {
                if (i != node.handlers.length-1) {
                    throw new ParseException(
                        "bare except must be last except clause", handler.type);
                }
            }

            if (handler.name != null) {
                mv.visitVarInsn(ALOAD, exc);
                mv.visitFieldInsn(GETFIELD, "org/python/core/PyException", "value", "Lorg/python/core/PyObject;");
                set(handler.name);
            }

            //do exception body
            suite(handler.body);
            mv.visitJumpInsn(GOTO, end_of_exceptions);
            mv.visitLabel(end_of_self);
        }
        mv.visitVarInsn(ALOAD, exc);
        mv.visitInsn(ATHROW);
    }

    public int add_traceback;
    public Object visitTryFinally(TryFinally node) throws Exception
    {
        Label start = new Label();
        Label end = new Label();
        Label handlerStart = new Label();
        Label finallyEnd = new Label();

        Object ret;

        ExceptionHandler inFinally = new ExceptionHandler(node);

        // Do protected suite
        exceptionHandlers.push(inFinally);

        int excLocal = mv.getLocal("java/lang/Throwable");
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, excLocal);

        mv.visitLabel(start);
        inFinally.exceptionStarts.addElement(start);

        ret = suite(node.body);

        mv.visitLabel(end);
        inFinally.exceptionEnds.addElement(end);
        inFinally.bodyDone = true;

        exceptionHandlers.pop();

        if (ret == NoExit) {
            inlineFinally(inFinally);
            mv.visitJumpInsn(GOTO, finallyEnd);
        }

        // Handle any exceptions that get thrown in suite
        mv.visitLabel(handlerStart);
        mv.visitVarInsn(ASTORE, excLocal);

        mv.visitVarInsn(ALOAD, excLocal);
        loadFrame();

        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "addTraceback", "(" + $throwable + $pyFrame + ")V");

        inlineFinally(inFinally);
        mv.visitVarInsn(ALOAD, excLocal);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Throwable");
        mv.visitInsn(ATHROW);

        mv.visitLabel(finallyEnd);

        mv.freeLocal(excLocal);

        inFinally.addExceptionHandlers(handlerStart);
        // According to any JVM verifiers, this code block might not return
        return null;
    }

    private void inlineFinally(ExceptionHandler handler) throws Exception {
        if (!handler.bodyDone) {
            // end the previous exception block so inlined finally code doesn't
            // get covered by our exception handler.
            Label end = new Label();
            mv.visitLabel(end);
            handler.exceptionEnds.addElement(end);
            // also exiting the try: portion of this particular finally
         }
        if (handler.isFinallyHandler()) {
            suite(handler.node.finalbody);
        }
    }
    
    private void reenterProtectedBody(ExceptionHandler handler) throws Exception {
        // restart exception coverage
	Label restart = new Label();
	mv.visitLabel(restart);
        handler.exceptionStarts.addElement(restart);
    }
 
    /**
     *  Inline the finally handling code for levels down to the levelth parent
     *  (0 means all).  This takes care to avoid having more nested finallys
     *  catch exceptions throw by the parent finally code.  This also pops off
     *  all the handlers above level temporarily.
     */
    private void doFinallysDownTo(int level) throws Exception {
        Stack poppedHandlers = new Stack();
        while (exceptionHandlers.size() > level) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.pop();
            inlineFinally(handler);
            poppedHandlers.push(handler);
        }
        while (poppedHandlers.size() > 0) {
            ExceptionHandler handler = 
                (ExceptionHandler)poppedHandlers.pop();
            reenterProtectedBody(handler);
            exceptionHandlers.push(handler);
         }
     }
 
    public int set_exception;
    public Object visitTryExcept(TryExcept node) throws Exception {
        Label start = new Label();
        Label end = new Label();
        Label handler_start = new Label();
        Label handler_end = new Label();
        ExceptionHandler handler = new ExceptionHandler();

        mv.visitLabel(start);
        handler.exceptionStarts.addElement(start);
        exceptionHandlers.push(handler);
        //Do suite
        Object exit = suite(node.body);
        exceptionHandlers.pop();
        mv.visitLabel(end);
        handler.exceptionEnds.addElement(end);

        if (exit == null)
            mv.visitJumpInsn(GOTO, handler_end);

        mv.visitLabel(handler_start);

        loadFrame();

        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "setException", "(" + $throwable + $pyFrame + ")" + $pyExc);

        int exc = mv.getFinallyLocal("java/lang/Throwable");
        mv.visitVarInsn(ASTORE, exc);

        if (node.orelse == null) {
            //No else clause to worry about
            exceptionTest(exc, handler_end, node, 1);
            mv.visitLabel(handler_end);
        } else {
            //Have else clause
            Label else_end = new Label();
            exceptionTest(exc, else_end, node, 1);
            mv.visitLabel(handler_end);

            //do else clause
            suite(node.orelse);
            mv.visitLabel(else_end);
        }

        mv.freeFinallyLocal(exc);
        handler.addExceptionHandlers(handler_start);
        return null;
    }

    public Object visitSuite(Suite node) throws Exception {
        return suite(node.body);
    }

    public Object suite(stmtType[] stmts) throws Exception {
        int n = stmts.length;
        for(int i = 0; i < n; i++) {
            Object exit = visit(stmts[i]);
            if (exit != null)
                return Exit;
        }
        return null;
    }

    public Object visitBoolOp(BoolOp node) throws Exception {
        Label end = new Label();
        visit(node.values[0]);
        for (int i = 1; i < node.values.length; i++) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");
            switch (node.op) {
            case BoolOp.Or : 
                mv.visitJumpInsn(IFNE, end);
                break;
            case BoolOp.And : 
                mv.visitJumpInsn(IFEQ, end);
                break;
            }
	        mv.visitInsn(POP);
            visit(node.values[i]);
        }
        mv.visitLabel(end);
        return null;
    }


    public Object visitCompare(Compare node) throws Exception {
        int tmp1 = mv.getLocal("org/python/core/PyObject");
        int tmp2 = mv.getLocal("org/python/core/PyObject");
        Label end = new Label();

        visit(node.left);

        int n = node.ops.length;
        for(int i = 0; i < n - 1; i++) {
            visit(node.comparators[i]);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, tmp1);
            visitCmpop(node.ops[i]);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, tmp2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__nonzero__", "()Z");
            mv.visitJumpInsn(IFEQ, end);
            mv.visitVarInsn(ALOAD, tmp1);
        }

        visit(node.comparators[n-1]);
        visitCmpop(node.ops[n-1]);

        if (n > 1) {
            mv.visitVarInsn(ASTORE, tmp2);
            mv.visitLabel(end);
            mv.visitVarInsn(ALOAD, tmp2);
        }
        mv.freeLocal(tmp1);
        mv.freeLocal(tmp2);
        return null;
    }

    public void visitCmpop(int op) throws Exception {
        String name = null;
        switch (op) {
        case Compare.Eq:    name = "_eq"; break;
        case Compare.NotEq: name = "_ne"; break;
        case Compare.Lt:    name = "_lt"; break;
        case Compare.LtE:   name = "_le"; break;
        case Compare.Gt:    name = "_gt"; break;
        case Compare.GtE:   name = "_ge"; break;
        case Compare.Is:    name = "_is"; break;
        case Compare.IsNot: name = "_isnot"; break;
        case Compare.In:    name = "_in"; break;
        case Compare.NotIn: name = "_notin"; break;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
    }

    static String[] bin_methods = new String[] {
        null,
        "_add",
        "_sub",
        "_mul",
        "_div",
        "_mod",
        "_pow",
        "_lshift",
        "_rshift",
        "_or",
        "_xor",
        "_and",
        "_floordiv",
    };

    public Object visitBinOp(BinOp node) throws Exception {
        visit(node.left);
        visit(node.right);
        String name = bin_methods[node.op];
        if (node.op == BinOp.Div && module.getFutures().areDivisionOn()) {
            name = "_truediv";
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
        return null;
    }
    
    static String[] unary_methods = new String[] {
        null,
        "__invert__",
        "__not__",
        "__pos__",
        "__neg__",
    };

    public Object visitUnaryOp(UnaryOp node) throws Exception {
        visit(node.operand);

        String name = unary_methods[node.op];
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", name, "()" + $pyObj);
        return null;
    }


    static String[] aug_methods = new String[] {
        null,
        "__iadd__",
        "__isub__",
        "__imul__",
        "__idiv__",
        "__imod__",
        "__ipow__",
        "__ilshift__",
        "__irshift__",
        "__ior__",
        "__ixor__",
        "__iand__",
        "__ifloordiv__",
    };

    public Object visitAugAssign(AugAssign node) throws Exception {
        visit(node.value);
        int tmp = storeTop();

        augmode = expr_contextType.Load;
        visit(node.target);

        mv.visitVarInsn(ALOAD, tmp);

        String name = aug_methods[node.op];
        if (node.op == BinOp.Div && module.getFutures().areDivisionOn()) {
            name = "__itruediv__";
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
        mv.freeLocal(tmp);

        temporary = storeTop();
        augmode = expr_contextType.Store;
        visit(node.target);
        mv.freeLocal(temporary);

        return null;
    }


    public static void makeStrings(Code mv, String[] names, int n)
        throws IOException
    {
        mv.iconst(n);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        int strings = mv.getLocal("[java/lang/String");
        mv.visitVarInsn(ASTORE, strings);
        for (int i=0; i<n; i++) {
            mv.visitVarInsn(ALOAD, strings);
            mv.iconst(i);
            mv.visitLdcInsn(names[i]);
            mv.visitInsn(AASTORE);
        }
        mv.visitVarInsn(ALOAD, strings);
        mv.freeLocal(strings);
    }
    
    public int invokea0, invokea1, invokea2;
    public int invoke2;
    public Object Invoke(Attribute node, SimpleNode[] values)
        throws Exception
    {
        String name = getName(node.attr);
        visit(node.value);
        mv.visitLdcInsn(name);

        switch (values.length) {
        case 0:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "invoke", "(" + $str + ")" + $pyObj);
            break;
        case 1:
            visit(values[0]);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "invoke", "(" + $str + $pyObj + ")" + $pyObj);
            break;
        case 2:
            visit(values[0]);
            visit(values[1]);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "invoke", "(" + $str + $pyObj + $pyObj + ")" + $pyObj);
            break;
        default:
            makeArray(values);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "invoke", "(" + $str + $pyObjArr + ")" + $pyObj);
            break;
        }
        return null;
    }


    public int callextra;
    public int call1, call2;
    public int calla0, calla1, calla2, calla3, calla4;
    public Object visitCall(Call node) throws Exception {
        String[] keys = new String[node.keywords.length];
        exprType[] values = new exprType[node.args.length + keys.length];
        for (int i = 0; i < node.args.length; i++) {
            values[i] = node.args[i];
        }
        for (int i = 0; i < node.keywords.length; i++) {
            keys[i] = node.keywords[i].arg;
            values[node.args.length + i] = node.keywords[i].value;
        }

        // Detect a method invocation with no keywords
        if ((node.keywords == null || node.keywords.length == 0)&& node.starargs == null &&
            node.kwargs == null && node.func instanceof Attribute)
        {
            return Invoke((Attribute) node.func, values);
        }

        visit(node.func);

        if (node.starargs != null || node.kwargs != null) {
            makeArray(values);
            makeStrings(mv, keys, keys.length);
            if (node.starargs == null)
                mv.visitInsn(ACONST_NULL);
            else
                visit(node.starargs);
            if (node.kwargs == null)
                mv.visitInsn(ACONST_NULL);
            else
                visit(node.kwargs);

            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "_callextra", "(" + $pyObjArr + $strArr + $pyObj + $pyObj + ")" + $pyObj);
        } else if (keys.length > 0) {
            makeArray(values);
            makeStrings(mv, keys, keys.length);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObjArr + $strArr + ")" + $pyObj);
        } else {
            switch (values.length) {
            case 0:
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "()" + $pyObj);
                break;
            case 1:
                visit(values[0]);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObj + ")" + $pyObj);
                break;
            case 2:
                visit(values[0]);
                visit(values[1]);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + ")" + $pyObj);
                break;
            case 3:
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
                break;
            case 4:
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                visit(values[3]);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
                break;
            default:
                makeArray(values);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__call__", "(" + $pyObjArr + ")" + $pyObj);
                break;
            }
        }
        return null;
    }


    public int getslice, setslice, delslice;
    public Object Slice(Subscript node, Slice slice) throws Exception {
        int ctx = node.ctx;
        if (ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 4);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            if (slice.lower != null)
                visit(slice.lower);
            else
                mv.visitInsn(ACONST_NULL);
            if (slice.upper != null)
                visit(slice.upper);
            else
                mv.visitInsn(ACONST_NULL);
            if (slice.step != null)
                visit(slice.step);
            else
                mv.visitInsn(ACONST_NULL);

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 4);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Subscript.Del:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__delslice__", "(" + $pyObj + $pyObj + $pyObj + ")V");
            return null;
        case Subscript.Load:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__getslice__", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
            return null;
        case Subscript.Store:
            mv.visitVarInsn(ALOAD, temporary);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__setslice__", "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")V");
            return null;
        }
        return null;

    }

    public int getitem, delitem, setitem;
    public Object visitSubscript(Subscript node) throws Exception {
        if (node.slice instanceof Slice) {
            return Slice(node, (Slice) node.slice);
        }

        int ctx = node.ctx;
        if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            visit(node.slice);

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Subscript.Del:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__delitem__", "(" + $pyObj + ")V");
            return null;
        case Subscript.Load:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__getitem__", "(" + $pyObj + ")" + $pyObj);
            return null;
        case Subscript.Store:
            mv.visitVarInsn(ALOAD, temporary);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__setitem__", "(" + $pyObj + $pyObj + ")V");
            return null;
        }
        return null;
    }

    public Object visitIndex(Index node) throws Exception {
        traverse(node);
        return null;
    }

    public Object visitExtSlice(ExtSlice node) throws Exception {
        mv.visitTypeInsn(NEW, "org/python/core/PyTuple");
        mv.visitInsn(DUP);
        makeArray(node.dims);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyTuple", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    public int getattr, delattr, setattr;
    public Object visitAttribute(Attribute node) throws Exception {

        int ctx = node.ctx;
        if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            mv.visitLdcInsn(getName(node.attr));

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Attribute.Del:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__delattr__", "(" + $str + ")V");
            return null;
        case Attribute.Load:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__getattr__", "(" + $str + ")" + $pyObj);
            return null;
        case Attribute.Store:
            mv.visitVarInsn(ALOAD, temporary);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__setattr__", "(" + $str + $pyObj + ")V");
            return null;
        }
        return null;
    }

    public int getitem2, unpackSequence;
    public Object seqSet(exprType[] nodes) throws Exception {
        mv.visitVarInsn(ALOAD, temporary);
        mv.iconst(nodes.length);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "unpackSequence", "(" + $pyObj + "I)" + $pyObjArr);

        int tmp = mv.getLocal("[org/python/core/PyObject");
        mv.visitVarInsn(ASTORE, tmp);

        for (int i = 0; i < nodes.length; i++) {
            mv.visitVarInsn(ALOAD, tmp);
            mv.iconst(i);
            mv.visitInsn(AALOAD);
            set(nodes[i]);
        }
        mv.freeLocal(tmp);

        return null;
    }

    public Object seqDel(exprType[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) {
            visit(nodes[i]);
        }
        return null;
    }

    public int PyTuple_init, PyList_init, PyDictionary_init;
    public Object visitTuple(Tuple node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to tuple not possible", node); */
        if (node.ctx == expr_contextType.Store) return seqSet(node.elts);
        if (node.ctx == expr_contextType.Del) return seqDel(node.elts);

        mv.visitTypeInsn(NEW, "org/python/core/PyTuple");

        mv.visitInsn(DUP);
        makeArray(node.elts);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyTuple", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    public Object visitList(List node) throws Exception {
        if (node.ctx == expr_contextType.Store) return seqSet(node.elts);
        if (node.ctx == expr_contextType.Del) return seqDel(node.elts);

        mv.visitTypeInsn(NEW, "org/python/core/PyList");
        mv.visitInsn(DUP);
        makeArray(node.elts);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyList", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    int list_comprehension_count = 0;

    public int PyList_init2;
    public Object visitListComp(ListComp node) throws Exception {
        mv.visitTypeInsn(NEW, "org/python/core/PyList");

        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyList", "<init>", "()V");

        mv.visitInsn(DUP);

        mv.visitLdcInsn("append");

        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__getattr__", "(" + $str + ")" + $pyObj);

        String tmp_append = "_[" + (++list_comprehension_count) + "]";
            
        set(new Name(tmp_append, Name.Store, node));

        stmtType n = new Expr(new Call(new Name(tmp_append, Name.Load, node), 
                                       new exprType[] { node.elt },
                                       new keywordType[0], null, null, node),
                                            node);

        for (int i = node.generators.length - 1; i >= 0; i--) {
            listcompType lc = node.generators[i];
            for (int j = lc.ifs.length - 1; j >= 0; j--) {
                n = new If(lc.ifs[j], new stmtType[] { n }, null, lc.ifs[j]);
            }
            n = new For(lc.target, lc.iter, new stmtType[] { n }, null, lc);
        }
        visit(n);
        visit(new Delete(new exprType[] { new Name(tmp_append, Name.Del) }));

        return null;
    }

    public Object visitDict(Dict node) throws Exception {
        mv.visitTypeInsn(NEW, "org/python/core/PyDictionary");

        mv.visitInsn(DUP);
        SimpleNode[] elts = new SimpleNode[node.keys.length * 2];
        for (int i = 0; i < node.keys.length; i++) {
            elts[i * 2] = node.keys[i];
            elts[i * 2 + 1] = node.values[i];
        }
        makeArray(elts);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyDictionary", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    public Object visitRepr(Repr node) throws Exception {
        visit(node.value);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__repr__", "()" + $pyStr);
        return null;
    }

    public int PyFunction_init1,PyFunction_closure_init1;
    public Object visitLambda(Lambda node) throws Exception {
        String name = "<lambda>";

        //Add a return node onto the outside of suite;
        modType retSuite = new Suite(new stmtType[] {
            new Return(node.body, node) }, node);

        setline(node);

        mv.visitTypeInsn(NEW, "org/python/core/PyFunction");

        mv.visitInsn(DUP);
        loadFrame();
        mv.visitFieldInsn(GETFIELD, "org/python/core/PyFrame", "f_globals", $pyObj);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        scope.setup_closure();
        scope.dump();
        module.PyCode(retSuite, name, true, className,
                      false, false, node.beginLine, scope, cflags).get(mv);

        if (!makeClosure(scope)) {
            mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + ")V");
        } else {
            mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObjArr + ")V");
        }

        return null;
    }


    public int Ellipsis;
    public Object visitEllipsis(Ellipsis node) throws Exception {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/python/core/Py", "Ellipsis", "Lorg/python/core/PyObject;");
        return null;
    }

    public int PySlice_init;
    public Object visitSlice(Slice node) throws Exception {
        mv.visitTypeInsn(NEW, "org/python/core/PySlice");

        mv.visitInsn(DUP);
        if (node.lower == null) getNone(); else visit(node.lower);
        if (node.upper == null) getNone(); else visit(node.upper);
        if (node.step == null) getNone(); else visit(node.step);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PySlice", "<init>", "(" + $pyObj + $pyObj + $pyObj + ")V");
        return null;
    }

    public int makeClass,makeClass_closure;
    public Object visitClassDef(ClassDef node) throws Exception {
        setline(node);

        //Get class name
        String name = getName(node.name);
        mv.visitLdcInsn(name);

        makeArray(node.bases);

        ScopeInfo scope = module.getScopeInfo(node);

        scope.setup_closure();
        scope.dump();
        //Make code object out of suite
        module.PyCode(new Suite(node.body, node), name, false, name,
                      true, false, node.beginLine, scope, cflags).get(mv);

        //Get doc string (if there)
        getDocString(node.body);

        //Make class out of name, bases, and code
        if (!makeClosure(scope)) {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeClass", "(" + $str + $pyObjArr + $pyCode + $pyObj + ")" + $pyObj);
        } else {
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "makeClass", "(" + $str + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")" + $pyObj);
        }

        //Assign this new class to the given name
        set(new Name(node.name, Name.Store, node));
        return null;
    }

    public Object visitNum(Num node) throws Exception {
        if (node.n instanceof PyInteger) {
            module.PyInteger(((PyInteger) node.n).getValue()).get(mv);
        } else if (node.n instanceof PyLong) {
            module.PyLong(((PyObject)node.n).__str__().toString()).get(mv);
        } else if (node.n instanceof PyFloat) {
            module.PyFloat(((PyFloat) node.n).getValue()).get(mv);
        } else if (node.n instanceof PyComplex) {
            module.PyComplex(((PyComplex) node.n).imag).get(mv);
        }
        return null;
    }

    private String getName(String name) {
        if (className != null && name.startsWith("__") &&
            !name.endsWith("__"))
        {
            //remove leading '_' from classname
            int i = 0;
            while (className.charAt(i) == '_')
                i++;
            return "_"+className.substring(i)+name;
        }
        return name;
    }

    int getglobal, getlocal1, getlocal2;
    int setglobal, setlocal1, setlocal2;
    int delglobal, dellocal1, dellocal2;
    int getderef,setderef;

    void emitGetGlobal(String name) throws Exception {
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getglobal", "(" + $str + ")" + $pyObj);
    }

    public Object visitName(Name node) throws Exception {
        String name;
        if (fast_locals)
            name = node.id;
        else
            name = getName(node.id);

        SymInfo syminf = (SymInfo)tbl.get(name);

        int ctx = node.ctx;
        if (ctx == expr_contextType.AugStore) {
            ctx = augmode;
        }        

        switch (ctx) {
        case Name.Load:
            loadFrame();
            if (syminf != null) {
                int flags = syminf.flags;
                if ((flags&ScopeInfo.GLOBAL) !=0 || optimizeGlobals&&
                        (flags&(ScopeInfo.BOUND|ScopeInfo.CELL|
                                                        ScopeInfo.FREE))==0) {
                    emitGetGlobal(name);
                    return null;
                }
                if (fast_locals) {
                    if ((flags&ScopeInfo.CELL) != 0) {
                        mv.iconst(syminf.env_index);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getderef", "(I)" + $pyObj);

                        return null;
                    }
                    if ((flags&ScopeInfo.BOUND) != 0) {
                        mv.iconst(syminf.locals_index);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getlocal", "(I)" + $pyObj);
                        return null;
                    }
                }
                if ((flags&ScopeInfo.FREE) != 0 &&
                            (flags&ScopeInfo.BOUND) == 0) {
                    mv.iconst(syminf.env_index);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getderef", "(I)" + $pyObj);

                    return null;
                }
            }
            mv.visitLdcInsn(name);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "getname", "(" + $str + ")" + $pyObj);
            return null;

        case Name.Store:
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, temporary);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setglobal", "(" + $str + $pyObj + ")V");
            } else {
                if (!fast_locals) {
                    mv.visitLdcInsn(name);
                    mv.visitVarInsn(ALOAD, temporary);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setlocal", "(" + $str + $pyObj + ")V");
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        mv.iconst(syminf.env_index);
                        mv.visitVarInsn(ALOAD, temporary);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setderef", "(I" + $pyObj + ")V");
                    } else {
                        mv.iconst(syminf.locals_index);
                        mv.visitVarInsn(ALOAD, temporary);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "setlocal", "(I" + $pyObj + ")V");
                    }
                }
            }
            return null;
        case Name.Del: {
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "delglobal", "(" + $str + ")V");
            } else {
                if (!fast_locals) {
                    mv.visitLdcInsn(name);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "dellocal", "(" + $str + ")V");
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        module.error("can not delete variable '"+name+
                              "' referenced in nested scope",true,node);
                    }
                    mv.iconst(syminf.locals_index);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyFrame", "dellocal", "(I)V");
                }
            }
            return null; }
        }
        return null;
    }

    public Object visitUnicode(Unicode node) throws Exception {
        String s = node.s;
        if (s.length() > 32767) {
            throw new ParseException(
                "string constant too large (more than 32767 characters)",
                node);
        }
        module.PyUnicode(s).get(mv);
        return null;
    }

    public Object visitStr(Str node) throws Exception {
        String s = node.s;
        if (s.length() > 32767) {
            throw new ParseException(
                "string constant too large (more than 32767 characters)",
                node);
        }
        module.PyString(s).get(mv);
        return null;
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        throw new Exception("Unhandled node " + node);
    }

    /**
     *  Data about a given exception range whether a try:finally: or a
     *  try:except:.  The finally needs to inline the finally block for
     *  each exit of the try: section, so we carry around that data for it.
     *  
     *  Both of these need to stop exception coverage of an area that is either
     *  the inlined finally of a parent try:finally: or the reentry block after
     *  a yield.  Thus we keep around a set of exception ranges that the
     *  catch block will eventually handle.
     */
    class ExceptionHandler {
        /**
         *  Each handler gets several exception ranges, this is because inlined
         *  finally exit code shouldn't be covered by the exception handler of
         *  that finally block.  Thus each time we inline the finally code, we
         *  stop one range and then enter a new one.
         *
         *  We also need to stop coverage for the recovery of the locals after
         *  a yield.
         */
        public Vector exceptionStarts = new Vector();
        public Vector exceptionEnds = new Vector();

        public boolean bodyDone = false;

        public TryFinally node = null;

        public ExceptionHandler() {
        }

        public ExceptionHandler(TryFinally n) {
            node = n;
        }

        public boolean isFinallyHandler() {
            return node != null;
        }

        public void addExceptionHandlers(Label handlerStart) throws Exception {
            for (int i = 0; i < exceptionStarts.size(); ++i) {
                Label start = (Label)exceptionStarts.elementAt(i);
                Label end = (Label)exceptionEnds.elementAt(i);
                //FIXME: not at all sure that getOffset() test is correct or necessary.
                if (start.getOffset() != end.getOffset()) {
                    mv.visitTryCatchBlock(
                        (Label)exceptionStarts.elementAt(i),
                        (Label)exceptionEnds.elementAt(i),
                        handlerStart,
                        "java/lang/Throwable");
                }
            }
        }
    }
}
