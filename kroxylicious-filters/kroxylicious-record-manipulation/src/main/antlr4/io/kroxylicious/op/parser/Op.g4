/**
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
grammar Op;

rules:
    rule* block <EOF>
    ;
rule:
    assign block ? SEMI
    ;
assign:
    IDENT EQ expr
    ;
expr :
    newExpr
    | newArray
    | path
    | call
    | literal
    ;

call:
    IDENT LPAR exprList? RPAR
    ;
exprList:
    expr (COMMA expr)*
    ;

path:
    (DOLLAR | AT) pathSegment*
    ;
pathSegment:
    childSegment
    | descendantSegment
    ;
childSegment:
    LSQ selectorList RSQ
    | DOT selector
    ;
descendantSegment:
    DOTDOT LSQ selectorList RSQ
    | DOTDOT selector
    ;
selectorList:
    selector (COMMA selector)*
    ;
selector:
    IDENT
    | INT
    | STAR
    ;

newExpr:
    fqcn  block?
    | block
    ;
fqcn:
    (IDENT DOT) * IDENT
    ;
block:
    LBR (assign (SEMI assign)* SEMI?)? RBR
    ;

newArray:
    LSQ exprList RSQ
    ;

literal:
  STRING | INT | TRUE | FALSE | NULL;

// lexer rules
DOLLAR: '$';
AT: '@';
STAR: '*';
COMMA: ',';
RSQ: ']';
LSQ: '[';
RPAR: ')';
LPAR: '(';
LBR: '{';
RBR: '}';
SEMI: ';';
EQ: '=';
DOT: '.';
DOTDOT: '..';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
INT: '-'? [0-9]+;
STRING: '"' (STRING_ESC | .)*? '"';
fragment STRING_ESC: '\\"' | '\\\\';
REGEX: '/' (REGEX_ESC | .)*? '/';
fragment REGEX_ESC: '\\/' | '\\\\';
LINE_COMMENT: '//' .*? '\r'? '\n' -> skip;
COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;
IDENT: [A-Za-z][A-Za-z0-9_]*;