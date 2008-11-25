// Autogenerated AST node
package org.python.antlr.ast;
import java.util.ArrayList;
import org.python.antlr.PythonTree;
import org.python.antlr.adapter.AstAdapters;
import org.python.antlr.adapter.ListWrapper;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

public abstract class exprType extends PythonTree {

    private final static String[] attributes = new String[] {"lineno",
                                                              "col_offset"};
    public String[] get_attributes() { return attributes; }

    public exprType() {
    }

    public exprType(int ttype, Token token) {
        super(ttype, token);
    }

    public exprType(Token token) {
        super(token);
    }

    public exprType(PythonTree node) {
        super(node);
    }

}
