// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class While extends stmtType {
    public exprType test;
    public stmtType[] body;
    public stmtType[] orelse;

    public static final String[] _fields = new String[]
    {"test","body","orelse"};

    public While(Token token, exprType test, stmtType[] body, stmtType[]
    orelse) {
        super(token);
        this.test = test;
        this.body = body;
        for(int ibody=0;ibody<body.length;ibody++) {
            addChild(body[ibody]);
        }
        this.orelse = orelse;
        for(int iorelse=0;iorelse<orelse.length;iorelse++) {
            addChild(orelse[iorelse]);
        }
    }

    public While(PythonTree tree, exprType test, stmtType[] body, stmtType[]
    orelse) {
        super(tree);
        this.test = test;
        this.body = body;
        for(int ibody=0;ibody<body.length;ibody++) {
            addChild(body[ibody]);
        }
        this.orelse = orelse;
        for(int iorelse=0;iorelse<orelse.length;iorelse++) {
            addChild(orelse[iorelse]);
        }
    }

    public String toString() {
        return "While";
    }

}
