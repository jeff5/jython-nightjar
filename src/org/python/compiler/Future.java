// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.antlr.ParseException;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.stmtType;

import java.util.List;

public class Future {

    private boolean division = false;
    private boolean with_statement = false;
    private boolean absolute_import = false;

    private static final String FUTURE = "__future__";

    private boolean check(ImportFrom cand) throws Exception {
        if (!cand.module.equals(FUTURE))
            return false;
        int n = cand.names.size();
        if (n == 0) {
            throw new ParseException(
                    "future statement does not support import *",cand);
        }
        for (int i = 0; i < n; i++) {
            String feature = cand.names.get(i).name;
            // *known* features
            if (feature.equals("nested_scopes")) {
                continue;
            }
            if (feature.equals("division")) {
                division = true;
                continue;
            }
            if (feature.equals("generators")) {
                continue;
            }
            if (feature.equals("with_statement")) {
                with_statement = true;
                continue;
            }
            if (feature.equals("absolute_import")) {
                absolute_import = true;
                continue;
            }
            if (feature.equals("braces")) {
                throw new ParseException("not a chance", cand);
            }
            if (feature.equals("GIL") || feature.equals("global_interpreter_lock")) {
                throw new ParseException("Never going to happen!", cand);
            }
            throw new ParseException("future feature "+feature+
                                     " is not defined",cand);
        }
        return true;
    }

    public void preprocessFutures(modType node,
                                  org.python.core.CompilerFlags cflags)
        throws Exception
    {
        if (cflags != null) {
            division = cflags.division;
        }
        int beg = 0;
        List<stmtType> suite = null;
        if (node instanceof Module) {
            suite = ((Module) node).body;
            if (suite.size() > 0 && suite.get(0) instanceof Expr &&
                            ((Expr) suite.get(0)).value instanceof Str) {
                beg++;
            }
        } else if (node instanceof Interactive) {
            suite = ((Interactive) node).body;
        } else {
            return;
        }

        for (int i = beg; i < suite.size(); i++) {
            stmtType stmt = suite.get(i);
            if (!(stmt instanceof ImportFrom))
                break;
            stmt.from_future_checked = true;
            if (!check((ImportFrom) stmt))
                break;
        }

        if (cflags != null) {
            cflags.division = cflags.division || division;
        }
        if (cflags != null) {
            cflags.with_statement = cflags.with_statement || with_statement;
        }
        if (cflags != null) {
            cflags.absolute_import = cflags.absolute_import || absolute_import;
        }
    }


    public static void checkFromFuture(ImportFrom node) throws Exception {
        if (node.from_future_checked)
            return;
        if (node.module.equals(FUTURE)) {
            throw new ParseException("from __future__ imports must occur " +
                                     "at the beginning of the file",node);
        }
        node.from_future_checked = true;
    }

    public boolean areDivisionOn() {
        return division;
    }
    
    public boolean withStatementSupported() {
        return with_statement;
    }

    public boolean isAbsoluteImportOn() {
        return absolute_import;
    }

}
