grammar Type;

type:
    intersectionType
    <EOF>
    ;

// TODO need a parenthesis operator so we can write (Foo | Bar)[]
// TODO need wildcard List<? exends Number>

intersectionType:
    unionType (INTERSECTION unionType)*
    ;

unionType:
    baseType (UNION baseType)*
    ;

baseType:
    IDENT (DOT IDENT)* typeArgs? (LSQ RSQ)*
    ;

typeArgs:
    LANG typeArg (COMMA typeArg)* RANG
    ;

typeArg:
    intersectionType (EXTENDS intersectionType)? (SUPER intersectionType)?
    ;

EXTENDS: 'extends';
SUPER: 'super';
IDENT: [a-zA-Z] [a-zA-Z0-9_$]*;
DOT: '.';
COMMA: ',';
LSQ: '[';
RSQ:']';
LANG: '<';
RANG:'>';
UNION: '|';
INTERSECTION: '&';
WS: [ \n\r\t]+ -> skip;