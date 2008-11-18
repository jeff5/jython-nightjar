// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.antlr.Visitor;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expression;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.With;
import org.python.antlr.ast.Yield;
import org.python.antlr.ast.comprehensionType;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.stmtType;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.List;

public class ScopesCompiler extends Visitor implements ScopeConstants {

    private CompilationContext code_compiler;

    private Stack scopes;
    private ScopeInfo cur = null;
    private Hashtable nodeScopes;

    private int level = 0;
    private int func_level = 0;

    public ScopesCompiler(CompilationContext code_compiler, Hashtable nodeScopes) {
        this.code_compiler = code_compiler;
        this.nodeScopes = nodeScopes;
        scopes = new Stack();
    }

    public void beginScope(String name, int kind, PythonTree node,
            ArgListCompiler ac) {
        if (cur != null) {
            scopes.push(cur);
        }
        if (kind == FUNCSCOPE) {
            func_level++;
        }
        cur = new ScopeInfo(name, node, level++, kind, func_level, ac);
        nodeScopes.put(node, cur);
    }

    public void endScope() throws Exception {
        if (cur.kind == FUNCSCOPE) {
            func_level--;
        }
        level--;
        ScopeInfo up = null;
        if (!scopes.empty()) {
            up = (ScopeInfo) scopes.pop();
        }
        //Go into the stack to find a non class containing scope to use making the closure
        //See PEP 227
        int dist = 1;
        ScopeInfo referenceable = up;
        for (int i = scopes.size() - 1; i >= 0
                && referenceable.kind == CLASSSCOPE; i--, dist++) {
            referenceable = ((ScopeInfo) scopes.get(i));
        }
        cur.cook(referenceable, dist, code_compiler);
        cur.dump(); // debug
        cur = up;
    }

    public void parse(PythonTree node) throws Exception {
        try {
            visit(node);
        } catch (Throwable t) {
            throw org.python.core.ParserFacade.fixParseError(null, t,
                    code_compiler.getFilename());
        }
    }

    @Override
    public Object visitInteractive(Interactive node) throws Exception {
        beginScope("<single-top>", TOPSCOPE, node, null);
        suite(node.getBody());
        endScope();
        return null;
    }

    @Override
    public Object visitModule(org.python.antlr.ast.Module node)
            throws Exception {
        beginScope("<file-top>", TOPSCOPE, node, null);
        suite(node.getBody());
        endScope();
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        beginScope("<eval-top>", TOPSCOPE, node, null);
        visit(new Return(node,node.getBody()));
        endScope();
        return null;
    }

    private void def(String name) {
        cur.addBound(name);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        def(node.getName());
        ArgListCompiler ac = new ArgListCompiler();
        ac.visitArgs(node.getArgs());

        List<exprType> defaults = ac.getDefaults();
        for (int i = 0; i < defaults.size(); i++) {
            visit(defaults.get(i));
        }

        List<exprType> decs = node.getDecorators();
        for (int i = decs.size() - 1; i >= 0; i--) {
            visit(decs.get(i));
        }

        beginScope(node.getName(), FUNCSCOPE, node, ac);
        int n = ac.names.size();
        for (int i = 0; i < n; i++) {
            cur.addParam((String) ac.names.get(i));
        }
        for (int i = 0; i < ac.init_code.size(); i++) {
            visit((stmtType) ac.init_code.get(i));
        }
        cur.markFromParam();
        suite(node.getBody());
        endScope();
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        ArgListCompiler ac = new ArgListCompiler();
        ac.visitArgs(node.getArgs());

        List<? extends PythonTree> defaults = ac.getDefaults();
        for (int i = 0; i < defaults.size(); i++) {
            visit(defaults.get(i));
        }

        beginScope("<lambda>", FUNCSCOPE, node, ac);
        for (Object o : ac.names) {
            cur.addParam((String) o);
        }
        for (Object o : ac.init_code) {
            visit((stmtType) o);
        }
        cur.markFromParam();
        visit(node.getBody());
        endScope();
        return null;
    }

    public void suite(List<stmtType> stmts) throws Exception {
        for (int i = 0; i < stmts.size(); i++)
            visit(stmts.get(i));
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        for (int i = 0; i < node.getNames().size(); i++) {
            if (node.getNames().get(i).getAsname() != null) {
                cur.addBound(node.getNames().get(i).getAsname());
            } else {
                String name = node.getNames().get(i).getName();
                if (name.indexOf('.') > 0) {
                    name = name.substring(0, name.indexOf('.'));
                }
                cur.addBound(name);
            }
        }
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        int n = node.getNames().size();
        if (n == 0) {
            cur.from_import_star = true;
            return null;
        }
        for (int i = 0; i < n; i++) {
            if (node.getNames().get(i).getAsname() != null) {
                cur.addBound(node.getNames().get(i).getAsname());
            } else {
                cur.addBound(node.getNames().get(i).getName());
            }
        }
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        int n = node.getNames().size();
        for (int i = 0; i < n; i++) {
            String name = node.getNames().get(i);
            int prev = cur.addGlobal(name);
            if (prev >= 0) {
                if ((prev & FROM_PARAM) != 0) {
                    code_compiler.error("name '" + name
                            + "' is local and global", true, node);
                }
                if ((prev & GLOBAL) != 0) {
                    continue;
                }
                String what;
                if ((prev & BOUND) != 0) {
                    what = "assignment";
                } else {
                    what = "use";
                }
                code_compiler.error("name '" + name
                        + "' declared global after " + what, false, node);
            }
        }
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        cur.exec = true;
        if (node.getGlobals() == null && node.getLocals() == null) {
            cur.unqual_exec = true;
        }
        traverse(node);
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        def(node.getName());
        int n = node.getBases().size();
        for (int i = 0; i < n; i++) {
            visit(node.getBases().get(i));
        }
        beginScope(node.getName(), CLASSSCOPE, node, null);
        suite(node.getBody());
        endScope();
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        String name = node.getId();
        if (node.getCtx() != expr_contextType.Load) {
            if (name.equals("__debug__")) {
                code_compiler.error("can not assign to __debug__", true, node);
            }
            cur.addBound(name);
        } else {
            cur.addUsed(name);
        }
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        String tmp = "_[" + node.getLine() + "_" + node.getCharPositionInLine()
                + "]";
        cur.addBound(tmp);
        traverse(node);
        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        cur.defineAsGenerator(node);
        cur.yield_count++;
        traverse(node);
        return null;
    }
    
    @Override
    public Object visitReturn(Return node) throws Exception {
        if (node.getValue() != null) {
            cur.noteReturnValue(node);
        }
        traverse(node);
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        // The first iterator is evaluated in the outer scope
        if (node.getGenerators() != null && node.getGenerators().size() > 0) {
            visit(node.getGenerators().get(0).getIter());
        }
        String bound_exp = "_(x)";
        String tmp = "_(" + node.getLine() + "_" + node.getCharPositionInLine()
                + ")";
        def(tmp);
        ArgListCompiler ac = new ArgListCompiler();
        List<exprType> args = new ArrayList<exprType>();
        args.add(new Name(node.token, bound_exp, expr_contextType.Param));
        ac.visitArgs(new argumentsType(node, args, null, null, new ArrayList<exprType>()));
        beginScope(tmp, FUNCSCOPE, node, ac);
        cur.addParam(bound_exp);
        cur.markFromParam();

        cur.defineAsGenerator(node);
        cur.yield_count++;
        // The reset of the iterators are evaluated in the inner scope
        if (node.getElt() != null) {
            visit(node.getElt());
        }
        if (node.getGenerators() != null) {
            for (int i = 0; i < node.getGenerators().size(); i++) {
                if (node.getGenerators().get(i) != null) {
                    if (i == 0) {
                        visit(node.getGenerators().get(i).getTarget());
                        if (node.getGenerators().get(i).getIfs() != null) {
                            for (exprType cond : node.getGenerators().get(i).getIfs()) {
                                if (cond != null) {
                                    visit(cond);
                                }
                            }
                        }
                    } else {
                        visit(node.getGenerators().get(i));
                    }
                }
            }
        }

        endScope();
        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {
        cur.max_with_count++;
        traverse(node);

        return null;
    }

}
