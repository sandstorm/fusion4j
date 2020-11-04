/*
 * MIT License
 *
 * Copyright (c) 2022 Sandstorm Media GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Authors:
 *  - Eric Kloss
 */

lexer grammar FusionLexer;

// case insensitive (CI_) letters (for 'true', 'false', 'null')
fragment CI_T: [tT];
fragment CI_R: [rR];
fragment CI_U: [uU];
fragment CI_E: [eE];
fragment CI_F: [fF];
fragment CI_A: [aA];
fragment CI_L: [lL];
fragment CI_S: [sS];
fragment CI_N: [nN];

fragment WS_Space: ' '|'\t';
fragment WS_Newline: '\r'? '\n';
fragment WS_EOF_Space: WS_Space* (WS_Newline+ | EOF);
fragment WS_EOF_NoSpace: (WS_Newline+ | EOF);

fragment INCLUDE_Keyword: 'include';
fragment INCLUDE_PatternChar: ~('\r'|'\n'|' '|'\t');

fragment NAMESPACE_Keyword: 'namespace';

fragment PROTOTYPE_Keyword: 'prototype';
fragment PROTOTYPE_NameNestingSeparator: '.';
fragment PROTOTYPE_NameChar
    : [a-zA-Z0-9]
    ;
fragment PROTOTYPE_SegmentedName
    : PROTOTYPE_NameChar+ (PROTOTYPE_NameNestingSeparator PROTOTYPE_NameChar+)*
    ;
fragment PROTOTYPE_Name: PROTOTYPE_SegmentedName (':' PROTOTYPE_SegmentedName)?;

// TODO why ':'???
fragment FUSION_PATH_NameChar: [a-zA-Z0-9] | '_' | '-' | ':';
fragment FUSION_PATH_NestingSeparator: '.';
fragment FUSION_PATH_SegmentPath: FUSION_PATH_SegmentPathQuoted | FUSION_PATH_SegmentPathNoQuotes;
fragment FUSION_PATH_SegmentPathNoQuotes: FUSION_PATH_NameChar+;
fragment FUSION_PATH_SegmentPathQuoted: FUSION_PATH_SegmentPathSingleQuoted | FUSION_PATH_SegmentPathDoubleQuoted;
fragment FUSION_PATH_SegmentPathSingleQuoted: '\'' ~('\r'|'\n')+? '\'';
fragment FUSION_PATH_SegmentPathDoubleQuoted: '"' ~('\r'|'\n')+? '"';

fragment FUSION_PATH_MetaPropPrefix: '@';

fragment FUSION_PATH_AssignOperator: WS_Space* '=' WS_Space*;
fragment FUSION_Erasure: WS_Space* '>' WS_EOF_Space?;

fragment FUSION_VALUE_BooleanTrue: CI_T CI_R CI_U CI_E;
fragment FUSION_VALUE_BooleanFalse: CI_F CI_A CI_L CI_S CI_E;
fragment FUSION_VALUE_Null: CI_N CI_U CI_L CI_L;

fragment FUSION_VALUE_ExpressionOpen: '${';
fragment FUSION_VALUE_ExpressionClose: '}';

fragment FUSION_VALUE_DslNameChar: [a-zA-Z0-9_\\-];

fragment COMMENT_Line
    : ('//'|'#') .*? WS_EOF_NoSpace
    ;

fragment COMMENT_Multi
    : '/*' .*? '*/'
    ;

/*
-------- root code layer
*/
WHITESPACE
    :  (WS_Space|WS_Newline)+
    //-> skip
    ;

WHITESPACE_NO_BR
    :  WS_Space+
    //-> skip
    ;

NAMESPACE_ALIAS_KEYWORD_AND_OPERATOR
    : NAMESPACE_Keyword WS_Space* ':' WS_Space*
    -> pushMode(NAMESPACE_ALIAS_MODE)
    ;

FILE_INCLUDE_KEYWORD_AND_OPERATOR
    : INCLUDE_Keyword WS_Space* ':' WS_Space*
    -> pushMode(FILE_INCLUDE_MODE)
    ;

ROOT_CODE_COMMENT
    : COMMENT_Line
    | COMMENT_Multi
    ;

ROOT_COPY_OPERATOR
    : WS_Space* '<' WS_Space*
    ;

ROOT_FUSION_ERASURE
    : FUSION_Erasure
    ;

ROOT_FUSION_BODY_START
    : WS_Space* '{'
    -> pushMode(FUSION_BODY_DECLARE_MODE)
    ;

ROOT_PROTOTYPE_KEYWORD
    : PROTOTYPE_Keyword {_input.LA(1) == '('}?
    ;

ROOT_PROTOTYPE_CALL_START
    : '('
    -> pushMode(PROTOTYPE_CALL_MODE)
    ;

ROOT_FUSION_PATH_SEGMENT
    : FUSION_PATH_SegmentPath
    ;

ROOT_FUSION_META_PROP_PREFIX
    : FUSION_PATH_MetaPropPrefix
    ;

ROOT_FUSION_PATH_NESTING_SEPARATOR
    : FUSION_PATH_NestingSeparator
    ;

ROOT_FUSION_PATH_DECLARE_OPERATOR
    : FUSION_PATH_AssignOperator
    -> pushMode(FUSION_VALUE_DECLARE_MODE)
    ;

/*
-------- inside a file include declaration
*/
mode FILE_INCLUDE_MODE;

FILE_INCLUDE_FILE_PATTERN
    : (INCLUDE_PatternChar+ WS_EOF_Space)
    -> popMode
    ;

INVALID_FILE_INCLUDE_PATTERN
    : .+? WS_EOF_Space
    -> popMode
    ;

/*
-------- inside a namespace alias declaration
*/
mode NAMESPACE_ALIAS_MODE;

NAMESPACE_ALIAS_TARGET_NAMESPACE
    : PROTOTYPE_SegmentedName WS_EOF_Space
    -> popMode
    ;

NAMESPACE_ALIAS_NAMESPACE
    : PROTOTYPE_SegmentedName
    ;

NAMESPACE_ALIAS_WS_SPACE
    : WS_Space+
    -> skip
    ;

NAMESPACE_ALIAS_OPERATOR
    : '='
    ;

/*
-------- inside a prototype "function" call 'prototype(you are here)'
*/
mode PROTOTYPE_CALL_MODE;

PROTOTYPE_NAME
    : PROTOTYPE_Name
    ;

PROTOTYPE_CALL_END
    : ')' -> popMode
    ;

/*
-------- inside a fusion code block declaration
*/
mode FUSION_BODY_DECLARE_MODE;

BODY_WHITESPACE
    :  (WS_Space|WS_Newline)+
    //-> skip
    ;

BODY_WHITESPACE_NO_BR
    :  WS_Space+
    //-> skip
    ;

CODE_COMMENT
    : COMMENT_Line
    | COMMENT_Multi
    ;

FUSION_BODY_START
    : WS_Space* '{'
    -> pushMode(FUSION_BODY_DECLARE_MODE)
    ;

FUSION_BODY_END
    : BODY_WHITESPACE* '}'
    -> popMode
    ;

PROTOTYPE_KEYWORD
    : PROTOTYPE_Keyword {_input.LA(1) == '('}?
    ;

PROTOTYPE_CALL_START
    : '('
    -> pushMode(PROTOTYPE_CALL_MODE)
    ;

COPY_OPERATOR
    : WS_Space* '<' WS_Space*
    ;

FUSION_PATH_SEGMENT
    : FUSION_PATH_SegmentPath
    ;

FUSION_ERASURE
    : FUSION_Erasure
    ;

FUSION_META_PROP_PREFIX
    : FUSION_PATH_MetaPropPrefix
    ;

FUSION_PATH_NESTING_SEPARATOR
    : FUSION_PATH_NestingSeparator
    ;

FUSION_PATH_ASSIGN_OPERATOR
    : FUSION_PATH_AssignOperator
    -> pushMode(FUSION_VALUE_DECLARE_MODE)
    ;

/*
-------- inside a fusion value declaration
*/
mode FUSION_VALUE_DECLARE_MODE;

// literals
FUSION_VALUE_LITERAL_NULL
    : FUSION_VALUE_Null WS_EOF_Space?
    -> popMode
    ;

// strings (single and double quoted)
FUSION_VALUE_STRING_DQUOTE
    : '"' (~('"' | '\\') | '\\' ('"' | '\\' | 'n' | 'r' | 't'))* '"'
    -> popMode
    ;

FUSION_VALUE_STRING_SQUOTE
    : '\'' (~('\'' | '\\') | '\\' ('\'' | '\\' | 'n' | 'r' | 't'))* '\''
    -> popMode
    ;

// numbers
FUSION_VALUE_NUMBER
    : '-'? [0-9]+ ('.' [0-9]+)? WS_EOF_Space?
    -> popMode
    ;

// boolean
FUSION_VALUE_BOOLEAN
    : (FUSION_VALUE_BooleanTrue | FUSION_VALUE_BooleanFalse) WS_EOF_Space?
    -> popMode
    ;

// expression
FUSION_VALUE_EXPRESSION_START
    : FUSION_VALUE_ExpressionOpen
    -> popMode, pushMode(EXPRESSION_LANGUAGE_MODE)
    ;

// DSL delegate
FUSION_VALUE_DSL_DELEGATE
    : FUSION_VALUE_DslNameChar+ '`'
    ( '\\`' | ~('`') )*?
    '`'
    ->popMode
    ;

// fusion object value
// -> "instantiation" of a prototype (or better: declaration of a prototype usage)
FUSION_VALUE_OBJECT
    : PROTOTYPE_Name
    -> popMode
    ;

mode EXPRESSION_LANGUAGE_MODE;

EL_STRING
    : '"' ( '\\"' | . )*? '"'
    | '\'' ( '\\\'' | . )*? '\''
    ;

EL_CODE_FRAGMENT
    : ~('{'|'}'|'"'|'\'')+
    ;

EL_CODE_START_BLOCK
    : '{'
    -> pushMode(EXPRESSION_LANGUAGE_MODE)
    ;

EL_CODE_END_BLOCK
    : '}'
    -> popMode
    ;
