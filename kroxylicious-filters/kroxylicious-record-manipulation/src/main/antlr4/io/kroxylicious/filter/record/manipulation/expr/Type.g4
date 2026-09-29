grammar Type;

type:
    intersectionType
    <EOF>
    ;

// TODO need a parenthesis operator so we can write (Foo | Bar)[]

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
    intersectionType
    | WILDCARD (EXTENDS intersectionType)? (SUPER intersectionType)?
    // Java also allows a type variable (with optional bounds) here, but I don't think we need that?
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
WILDCARD: '?';
WS: [ \n\r\t]+ -> skip;