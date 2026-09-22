grammar Expr;
import Path;

expr:
    expr1 <EOF>
;

expr1:
    (functionExpr | jsonpathQuery)
;