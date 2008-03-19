// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class Lambda extends exprType {
    public argumentsType args;
    public exprType body;

    public static final String[] _fields = new String[] {"args","body"};

    public Lambda(Token token, argumentsType args, exprType body) {
        super(token);
        this.args = args;
        this.body = body;
    }

    public Lambda(PythonTree tree, argumentsType args, exprType body) {
        super(tree);
        this.args = args;
        this.body = body;
    }

    public String toString() {
        return "Lambda";
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitLambda(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (args != null)
            args.accept(visitor);
        if (body != null)
            body.accept(visitor);
    }

}
