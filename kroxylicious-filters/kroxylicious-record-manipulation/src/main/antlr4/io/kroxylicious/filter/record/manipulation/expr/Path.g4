grammar Path;

@lexer::members { }

// I _think_ this is a fairly solid ANTLR-ification of the ABNF grammar given in
// https://www.rfc-editor.org/info/rfc9535/#appendix-A
// Some differences:
// String escaping for unicode is a bit looser
// -0 is allowed in slice and index where the ABNF would reject it
// these would need to be detected and rejected in the visitor/listener in order for a parser built on this to be a
// conforming implementation of the RFC.

jsonpathQuery:
    DOLLAR segments
    <EOF>
    ;

segments:
    (s segment)*
    ;

segment:
    childSegment
    | descendantSegment
    ;

childSegment:
    bracketedSelection
    | ( DOT ( STAR | memberNameShorthand ) )
    ;

descendantSegment:
    DOTDOT (bracketedSelection
        | STAR
        | memberNameShorthand)
    ;

bracketedSelection:
    LSQ s selector (s COMMA s selector)* s RSQ
    ;

selector:
    nameSelector
    | STAR
    | sliceSelector
    | indexSelector
    | filterSelector
    ;

nameSelector:
    stringLiteral
    ;

//stringLiteral:
//    DQUOT doubleQuoted* DQUOT   // "string"
//    | SQUOT singleQuoted* SQUOT // 'string'
//    ;

//doubleQuoted:
//    unescaped
//    | SQUOT         // '
//    | ESC DQUOT     // \"
//    | ESCAPED
//    ;
//
//singleQuoted:
//    unescaped
//    | DQUOT         // "
//    | ESC SQUOT     // \'
//    | ESCAPED
//    ;

indexSelector:
    intLiteral                        // decimal integer
    ;



//DIGIT              : '1..9';                    // 1-9 non-zero digit
sliceSelector:
    (start s)? COLON s (end s)? (COLON (s step)?)?
    ;

start:
    intLiteral       // included in selection
    ;

end:
    intLiteral       // not included in selection
    ;

step:
    intLiteral       // default: 1
    ;

filterSelector:
    QUEST s logicalExpr
    ;

logicalExpr:
    logicalOrExpr
    ;

logicalOrExpr: // disjunction
    logicalAndExpr (s OR s logicalAndExpr)* // binds less tightly than conjunction
    ;


logicalAndExpr: // conjunction
    basicExpr (s AND s basicExpr)* // binds more tightly than disjunction
    ;

basicExpr:
    parenExpr
    | comparisonExpr
    | testExpr
    ;

parenExpr: // parenthesized expression
    (logicalNotOp)? LPAR logicalExpr RPAR
    ;

logicalNotOp:
    NOT                // logical NOT operator
    ;

testExpr:
    (logicalNotOp s)?
    (
        filterQuery     // existence|non-existence
        | functionExpr  // LogicalType or NodesType
    )
    ;

filterQuery:
    relQuery
    | jsonpathQuery
    ;

relQuery:
    AT segments
    ;

comparisonExpr:
    comparable s comparisonOp s comparable
    ;

literal:
    intLiteral
    | numberLiteral
    | stringLiteral
    | booleanLiteral
    | NULL
    ;

comparable:
    literal
    | singularQuery  // singular query value
    | functionExpr   // ValueType
    ;

comparisonOp:
    EQ
    | NE
    | LE
    | GE
    | LT
    | GT
    ;

singularQuery:
    relSingularQuery
    | absSingularQuery
    ;

relSingularQuery:
    AT singularQuerySegments
    ;

absSingularQuery:
    DOLLAR singularQuerySegments
    ;

singularQuerySegments:
    ( s ( nameSegment | indexSegment ) )*
    ;

nameSegment:
    (LSQ nameSelector RSQ)
    | (DOT memberNameShorthand)
    ;

indexSegment:
    LSQ indexSelector RSQ
    ;





functionExpr:
    functionName LPAR ( s functionArgument ( s COMMA s functionArgument )* )? s RPAR
    ;

functionArgument:
    literal
    | filterQuery // (includes singular-query)
    | logicalExpr
    | functionExpr
    ;

stringLiteral:
    STRING
    ;
booleanLiteral:
    TRUE
    | FALSE
    ;
numberLiteral:
    NUMBER
    ;
intLiteral:
    DIGIT | INT
    ;


alpha:
    UCALPHA   // A-Z | a-z
    | LOWER
    ;

s                   : B* ;        // || optional blank space

memberNameShorthand:
    nameFirst nameChar*
    ;

nameFirst:
    alpha
    | UNDERSCORE
    | NON_ASCII
    ;

nameChar:
    nameFirst
    | DIGIT
    ;

functionName:
    functionNameFirst functionNameChar*
    ;

functionNameFirst:
    LOWER
    ;

functionNameChar:
    functionNameFirst | UNDERSCORE | DIGIT
    ;

// Lexer rules
//INT:
//    (MINUS? DIGIT+)      // - optional, octal-like 01 or -01 not allowed
//    ;
DIGIT:
    ZERO | DIGIT1     // 0-9
    ;
INT:
    ZERO
    | ( MINUS? DIGIT1 DIGIT* )
    ;
NUMBER:
    (INT | MINUS_ZERO) FRAC? EXP? // decimal number
    ;
fragment ZERO : '0' ;
fragment MINUS_ZERO : '-0' ;
fragment DIGIT1 : '1'..'9' ;
fragment FRAC:
    DOT DIGIT+                  // decimal fraction
    ;
fragment EXP:
    EXPONENT ( MINUS | PLUS )? DIGIT+ // decimal exponent
    ;
fragment EXPONENT: 'e' ;


//STRING : DSTRING | SSTRING;
//fragment DSTRING: '"' (DESC|.)*? '"';
//fragment SSTRING: '\'' (SESC|.)*? '\'';
//fragment DESC: '\\"' | ESC;
//fragment SESC: '\\\'' | ESC;
//fragment ESC: '\\\\' | '\\\n' | '\\\r' | '\\\f' | '\\/' | ('\\u' HEX HEX HEX HEX);
//fragment HEX: [0-9] | [A-F] | [a-f];



LOWER:
    EXPONENT
    | LCALPHA
    ;
fragment LCALPHA: 'a'..'z' ;



STRING : DSTRING | SSTRING ;
fragment DSTRING : '"' ( ~[\u0000-\u001F"\\] | ESC_SEQ )* '"' ;
fragment SSTRING : '\'' ( ~[\u0000-\u001F'\\] | ESC_SEQ )* '\'' ;
fragment ESC_SEQ
    : '\\' [bfrnt/\\'"]
    | '\\' 'u' HEX HEX HEX HEX
    ;
fragment HEX : [0-9a-fA-F] ;

NOT: '!';


UCALPHA: 'A'..'Z';

HI_ASCII : [\u007B-\u007F] ; // top of ascii range, above z
NON_ASCII : [\u0080-\uD7FF] // up to last non-surrogate pair
    | [\u{00E000}-\u{10FFFF}]; // high range

UNDERSCORE: '_' ;
MINUS : '-' ;

PLUS : '+' ;
DOT : '.' ;
DOTDOT : '..' ;

COMMA : ',' ;

OR : '||' ;
AND : '&&' ;
EQ : '==' ;
NE : '!=' ;
LT : '<' ;
LE : '<=' ;
GT : '>' ;
GE : '>=' ;
QUEST : '?' ;
COLON : ':' ;
RPAR : ')' ;
LPAR : '(' ;
LSQ : '[' ;
RSQ : ']' ;

DOLLAR : '$' ;
AT : '@' ;
STAR: '*' ;
TRUE: 'true' ;
FALSE: 'false' ;
NULL: 'null' ;

B: ' ' | //   || Space
   '\t' | //   || Horizontal tab
   '\n' | //   || Line feed or New line
   '\r'      // ||Carriage return
   ;

//ESC: '\\';                           // \ backslash
//ESCAPED
//    : ESC ( [\\/bfnrt] | 'u' HEX HEX HEX HEX )
//    ;
//fragment HEX
//    : [0-9a-fA-F]
//    ;
SQUOT: '\'' ;
DQUOT: '"' ;
