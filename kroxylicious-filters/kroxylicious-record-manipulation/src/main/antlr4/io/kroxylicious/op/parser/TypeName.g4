grammar TypeName;

typeName:
    type <EOF>
    ;
type:
    rawType ( LA typeArgumentList RA ) ? BOX*
    ;
typeArgumentList:
    typeArgument (COMMA typeArgument)*
    ;
rawType:
    IDENT (DOT IDENT)*
    ;
typeArgument:
    wildcardType
    | type
    ;
wildcardType:
    Q (SUPER type)? (EXTENDS type)?
    ;

Q: '?';
SUPER: 'super';
EXTENDS: 'extends';
LA: '<';
RA: '>';
COMMA: ',';
DOT: '.';
BOX: '[]';
IDENT: [A-Za-z][A-Za-z0-9_]*;
WS: [ \t\r\n]+ -> skip;