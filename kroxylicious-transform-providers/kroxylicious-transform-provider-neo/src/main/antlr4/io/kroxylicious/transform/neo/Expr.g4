grammar Expr;
import Path;

expr:
    expr1 <EOF>
    ;

expr1:
    jsonpathQuery
    | logicalExpr
    | literal
    ;