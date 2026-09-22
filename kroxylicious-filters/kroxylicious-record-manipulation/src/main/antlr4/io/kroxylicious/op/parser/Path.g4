grammar Path;

// I _think_ this is a fairly solid ANTLR-ification of the ABNF grammar given in
// https://www.rfc-editor.org/info/rfc9535/#appendix-A
// with the notable exception of whitespace handling.
// Here we're just skipping all whitespace
// but the grammar in the RFC is very selective about where whitespace is allowed

jsonpathQuery:
    DOLLAR segments
    <EOF>
    ;

segments:
    (segment)*
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

stringLiteral:
    DQUOT doubleQuoted* DQUOT   // "string"
    | SQUOT singleQuoted* SQUOT // 'string'
    ;

doubleQuoted:
    unescaped
    | SQUOT         // '
    | ESC DQUOT     // \"
    | ESCAPED
    ;

singleQuoted:
    unescaped
    | DQUOT         // "
    | ESC SQUOT     // \'
    | ESCAPED
    ;

indexSelector:
    int                        // decimal integer
    ;

int:
    ZERO
    | (MINUS? NON_0_DIGIT digit*)      // - optional, octal-like 01 or -01 not allowed
    ;

//DIGIT              : '1..9';                    // 1-9 non-zero digit
sliceSelector:
    (start)? COLON (end)? (COLON (step)?)?
    ;

start:
    int       // included in selection
    ;

end:
    int       // not included in selection
    ;

step:
    int       // default: 1
    ;

filterSelector:
    QUEST logicalExpr
    ;

logicalExpr:
    logicalOrExpr
    ;

logicalOrExpr: // disjunction
    logicalAndExpr (OR logicalAndExpr)* // binds less tightly than conjunction
    ;


logicalAndExpr: // conjunction
    basicExpr (AND basicExpr)* // binds more tightly than disjunction
    ;

basicExpr:
    parenExpr
    | comparisonExpr
    | testExpr
    ;

parenExpr:
    (logicalNotOp)? LPAR logicalExpr RPAR
    ;
                                        // parenthesized expression
logicalNotOp: NOT                // logical NOT operator
    ;

testExpr:
    (logicalNotOp)? (
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
    comparable comparisonOp comparable
    ;

literal:
    number
    | stringLiteral
    | TRUE
    | FALSE
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
    ( nameSegment | indexSegment )*
    ;

nameSegment:
    (LSQ nameSelector RSQ)
    | (DOT memberNameShorthand)
    ;

indexSegment:
    LSQ indexSelector RSQ
    ;

number:
    (int | MINUS_ZERO) frac? exp? // decimal number
    ;

frac:
    DOT digit+                  // decimal fraction
    ;

exp:
    EXPONENT ( MINUS | PLUS )? digit+ // decimal exponent
    ;

functionName:
    functionNameFirst *functionNameChar
    ;

functionNameFirst:
    lower
    ;

functionNameChar:
    functionNameFirst | UNDERSCORE | digit
    ;

functionExpr:
    functionName LPAR ( functionArgument ( COMMA functionArgument )* )? RPAR
    ;

functionArgument:
    literal
    | filterQuery // (includes singular-query)
    | logicalExpr
    | functionExpr
    ;

segment:
    childSegment
    | descendantSegment
    ;

childSegment:
    bracketedSelection
    | ( DOT ( STAR | memberNameShorthand ) )
    ;

bracketedSelection:
    LSQ selector (COMMA selector)* RSQ
    ;

memberNameShorthand:
    nameFirst *nameChar
    ;

nameFirst:
    alpha
    | UNDERSCORE
    | NON_ASCII
    ;

nameChar:
    nameFirst
    | digit
    ;

descendantSegment:
    DOTDOT (bracketedSelection
        | STAR
        | memberNameShorthand)
    ;

digit:
    ZERO | NON_0_DIGIT     // 0-9
    ;

unescaped:
    SPACE | NOT | '#' | DOLLAR | '%' | '&'
    | LPAR | RPAR | STAR | PLUS | COMMA | MINUS | DOT | '/'
    | digit | COLON | ';' | LT | '=' | GT | QUEST | AT
    | UCALPHA | LSQ | '\\' | RSQ | '^' | UNDERSCORE | '`'
    | lower
    | HI_ASCII
    | NON_ASCII
    | MINUS_ZERO
    ;

alpha:
    UCALPHA   // A-Z | a-z
    | lower
    ;

lower:
    LCALPHA
    | EXPONENT
    ;

// Lexer rules

NOT: '!';
EXPONENT: 'e' ;
LCALPHA: 'a'..'z' ;
UCALPHA: 'A'..'Z';

HI_ASCII : [\u007B-\u007F] ; // top of ascii range, above z
NON_ASCII : [\u0080-\uD7FF] // up to last non-surrogate pair
    | [\u{00E000}-\u{10FFFF}]; // high range

SPACE: ' ';

UNDERSCORE: '_' ;
MINUS : '-' ;
MINUS_ZERO : '-0' ;
PLUS : '+' ;
DOT : '.' ;
DOTDOT : '..' ;

COMMA : ',' ;

OR : '||' ;
AND : '&&' ;
EQ : '::' ;
NE : '!:' ;
LT : '<' ;
LE : '<:' ;
GT : '>' ;
GE : '>:' ;

QUEST : '?' ;

COLON : ':' ;


RPAR : ')' ;
LPAR : '(' ;
LSQ : '[' ;
RSQ : ']' ;

ZERO : '0' ;
NON_0_DIGIT: '1'..'9' ;

DOLLAR : '$' ;
AT : '@' ;
STAR: '*' ;
TRUE: 'true' ;
FALSE: 'false' ;
NULL: 'null' ;






//B: ' ' | //   || Space
//   '\t' | //   || Horizontal tab
//   '\n' | //   || Line feed or New line
//   '\r'      // ||Carriage return
//   ;
//fragment S                   : B* ;        // || optional blank space

WS: (SPACE | [\t\r\n]) + -> skip;
ESC: '\\';                           // \ backslash
ESCAPED
    : ESC ( [\\/bfnrt] | 'u' HEX HEX HEX HEX )
    ;
fragment HEX
    : [0-9a-fA-F]
    ;
SQUOT: '\'' ;
DQUOT: '"' ;
