lexer grammar HaskellLexer;

SHEBANG: '#!' ~[\r\n]*;

fragment LINE_COMMENT: '--' ~[\r\n]*;

fragment DELIMITED_COMMENT: '{-' ( DELIMITED_COMMENT | . )*? '-}';

fragment WS : [\u0020\u0009\u000C];
fragment NL: '\n' | '\r' '\n'?;
SPACES : (WS | NL | LINE_COMMENT | DELIMITED_COMMENT)+;

LEFT_CURLY : '{' ;
RIGHT_CURLY : '}' ;
SEMICOLON : ';';
LEFT_PAREN : '(';
RIGHT_PAREN : ')';
LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
COMMA : ',';
INFIX_QUOTE : '`';

UNDERSCORE: '_';
CASE: 'case';
CLASS: 'class';
DATA: 'data';
DEFAULT: 'default';
DERIVING: 'deriving';
DO: 'do';
ELSE: 'else';
IF: 'if';
IMPORT: 'import';
IN: 'in';
INFIX: 'infix';
INFIXL: 'infixl';
INFIXR: 'infixr';
INSTANCE: 'instance';
LET: 'let';
MODULE: 'module';
NEWTYPE: 'newtype';
OF: 'of';
THEN: 'then';
TYPE: 'type';
WHERE: 'where';

DOTDOT: '..';
COLONCOLON: '::' | '\u2237';
EQ: '=';
BACKSLASH: '\\';
BAR: '|';
LEFT_ARROW: '<-' | '\u2190';
RIGHT_ARROW: '->'| '\u2192';
ATCOLON: '@:';
AT: '@';
TILDE: '~';
DOUBLE_ARROW: '=>' | '\u21d2';
PAR_ARRAY_LEFT_SQUARE: '[:';
PAR_ARRAY_RIGHT_SQUARE: ':]';
LEFT_ARROW_TAIL: '-<' | '\u2919';
RIGHT_ARROW_TAIL: '>-' | '\u291a';
LEFT_DOUBLE_ARROW_TAIL: '-<<' | '\u291b';
RIGHT_DOUBLE_ARROW_TAIL: '>>-' | '\u291c';

UFORALL: '\u2200';

fragment DIGIT : '0'..'9';
fragment HEXIT : DIGIT | 'A'..'F' | 'a'..'f' ;
fragment OCTIT : '0'..'7' ;

fragment DECIMAL : DIGIT DIGIT*;
fragment OCTAL : OCTIT OCTIT*;
fragment HEXADECIMAL : HEXIT HEXIT*;

INTEGER : DECIMAL | '0o' OCTAL | '0O' OCTAL | '0x' HEXADECIMAL | '0X' HEXADECIMAL;

fragment EXPONENT : ('e' | 'E') ('+' | '-')? DECIMAL;

FLOAT : DECIMAL '.' DECIMAL EXPONENT? | DECIMAL EXPONENT;

fragment ESCAPE : '\\' ([abfnrtv\\"'&] | DECIMAL | 'o' OCTAL | 'x' HEXADECIMAL);
fragment GAP : '\\' (WS | NL)+ '\\';

CHAR : '\'' (~[\\'] | ESCAPE) '\'';
STRING :  '"' (~[\\'] | ESCAPE | GAP)* '"';

fragment UPPER_CASE : 'A'..'Z';
fragment LOWER_CASE : 'a'..'z' | '_';
fragment LETTER : UPPER_CASE | LOWER_CASE;

fragment CONSTRUCTOR_ID : UPPER_CASE	(LETTER | DIGIT | '\'' )*;
fragment VARIABLE_ID : LOWER_CASE (LETTER | DIGIT | '\'' )*;

fragment BODYSYMBOL : '!' | '#' | '$' | '%' | '&' | '*'
                | '+' | '.' | '/' | '<' | '=' | '>'
                | '?' | '@' | '\\' | '^' | '|' | '-' | '~' | [\p{Symbol}];
fragment STARTSYMBOL : ':';

fragment SYMBOL : STARTSYMBOL | BODYSYMBOL;

fragment CONSTRUCTOR_SYM : STARTSYMBOL SYMBOL*;
fragment VARIABLE_SYM : BODYSYMBOL SYMBOL*;

fragment MOD_ID : (CONSTRUCTOR_ID '.')* CONSTRUCTOR_ID;

QCONSTRUCTOR_ID : (MOD_ID '.')? CONSTRUCTOR_ID;
QVARIABLE_ID : (MOD_ID '.')? VARIABLE_ID;
QCONSTRUCTOR_SYM : (MOD_ID '.')? CONSTRUCTOR_SYM;
QVARIABLE_SYM : (MOD_ID '.')? VARIABLE_SYM;

ERROR: .;
