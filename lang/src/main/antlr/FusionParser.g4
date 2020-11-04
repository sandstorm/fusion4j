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

/*
 Path assignments (root and inside of a body)
 - a path may be nested
 - a prototype(XYZ) declaration/configuration can be a path segment
 - a prototype(XYZ) assignment can be a path segment, but never the last
*/

parser grammar FusionParser;

options { tokenVocab=FusionLexer; }

/*
--------- path only parser rule
*/
fusionPath
    : rootFusionConfigurationPathReference
      EOF
    ;

/*
--------- main parser rule (fragmented)
*/
fusionFile
    : rootFragment* EOF
    ;

rootFragment
    : rootPrototypeErasure
    | rootFusionConfigurationErasure
    | rootPrototypeDecl
    | rootFusionAssign
    | rootFusionConfiguration
    | rootFusionConfigurationCopy
    | codeComment
    | namespaceAlias
    | fileInclude
    | whitespace
    ;

fusionFragment
    : fusionAssign
    | fusionConfiguration
    | fusionConfigurationCopy
    | fusionConfigurationErasure
    | codeComment
    | whitespace
    ;
/*
--------- comments / whitespaces
*/

whitespace
    : WHITESPACE
    | BODY_WHITESPACE
    ;

codeComment
    : ROOT_CODE_COMMENT
    | CODE_COMMENT
    ;

/*
--------- file includes
*/
fileInclude
    : FILE_INCLUDE_KEYWORD_AND_OPERATOR
      fileIncludePattern
    ;

fileIncludePattern
    : FILE_INCLUDE_FILE_PATTERN
    | INVALID_FILE_INCLUDE_PATTERN {notifyErrorListeners("file include pattern does not allow whitespaces");}
    ;

/*
--------- namespace aliases
*/
namespaceAlias
    : NAMESPACE_ALIAS_KEYWORD_AND_OPERATOR
      NAMESPACE_ALIAS_NAMESPACE
      NAMESPACE_ALIAS_OPERATOR
      NAMESPACE_ALIAS_TARGET_NAMESPACE
    ;

/*
--------- root fusion declaration
For now, this is just boiler plate to support
the recursive push/pop mecanics of the lexer,
since there is no difference between root and
inner path declarations.
*/

rootFusionAssign
    : rootFusionAssignPath
      ROOT_FUSION_PATH_DECLARE_OPERATOR
      rootFusionValueDecl
    ;

rootFusionAssignPath
    : (rootPrototypeCall | rootFusionMetaPropPathSegment | rootFusionPathSegment)
    (
        (ROOT_FUSION_PATH_NESTING_SEPARATOR (rootPrototypeCall | rootFusionMetaPropPathSegment | rootFusionPathSegment))*
        ROOT_FUSION_PATH_NESTING_SEPARATOR (rootFusionMetaPropPathSegment | rootFusionPathSegment)
    )?
    ;

rootPrototypeCall
    : ROOT_PROTOTYPE_KEYWORD ROOT_PROTOTYPE_CALL_START PROTOTYPE_NAME PROTOTYPE_CALL_END WHITESPACE_NO_BR*
    ;

rootFusionConfiguration
    : rootFusionConfigurationPath
      rootFusionConfigurationBody
    ;

rootFusionConfigurationBody
    : ROOT_FUSION_BODY_START
      fusionFragment*
      FUSION_BODY_END
    ;

rootFusionConfigurationErasure
    : rootFusionConfigurationPath
      ROOT_FUSION_ERASURE
    ;

rootFusionConfigurationPath
    : (rootPrototypeCall | rootFusionMetaPropPathSegment | rootFusionPathSegment)
      (ROOT_FUSION_PATH_NESTING_SEPARATOR (rootPrototypeCall | rootFusionMetaPropPathSegment | rootFusionPathSegment))*
    ;

rootFusionPathSegment
    : ROOT_FUSION_PATH_SEGMENT
    ;

rootFusionMetaPropPathSegment
    : ROOT_FUSION_META_PROP_PREFIX ROOT_FUSION_PATH_SEGMENT
    ;

rootFusionConfigurationCopy
    : rootFusionConfigurationPath
      ROOT_COPY_OPERATOR
      rootFusionConfigurationPathReference
      rootFusionConfigurationBody?
    ;

rootFusionConfigurationPathReference
    : ROOT_FUSION_PATH_NESTING_SEPARATOR? rootFusionConfigurationPath
    ;

/*
--------- root prototype declarations
*/
rootPrototypeDecl
    : rootPrototypeCall
      prototypeInheritance?
      prototypeBody?
    ;

rootPrototypeErasure
    : rootPrototypeCall ROOT_FUSION_ERASURE
    ;

prototypeBody
    : ROOT_FUSION_BODY_START
    fusionFragment*
    FUSION_BODY_END
    ;

prototypeInheritance
    : ROOT_COPY_OPERATOR rootPrototypeCall
    ;

/*
--------- Fusion path declarations
*/

// everything with 'path = ...'
fusionAssign
    : fusionAssignPath
      FUSION_PATH_ASSIGN_OPERATOR
      fusionValueDecl
    ;

fusionAssignPath
    : (prototypeCall | fusionMetaPropPathSegment | fusionPathSegment)
    (
        (FUSION_PATH_NESTING_SEPARATOR (prototypeCall | fusionMetaPropPathSegment | fusionPathSegment))*
        FUSION_PATH_NESTING_SEPARATOR (fusionMetaPropPathSegment | fusionPathSegment)
    )?
    ;

prototypeCall
    : PROTOTYPE_KEYWORD PROTOTYPE_CALL_START PROTOTYPE_NAME PROTOTYPE_CALL_END WHITESPACE_NO_BR*
    ;


fusionConfigurationErasure
    : fusionConfigurationPath
      FUSION_ERASURE
    ;

// configuration declaration of a fusion path 'path { ... }'
fusionConfiguration
    : fusionConfigurationPath
      fusionConfigurationBody
    ;

fusionConfigurationBody
    : FUSION_BODY_START
      fusionFragment*
      FUSION_BODY_END
    ;

fusionConfigurationPath
    : (prototypeCall | fusionMetaPropPathSegment | fusionPathSegment)
      (FUSION_PATH_NESTING_SEPARATOR (prototypeCall | fusionMetaPropPathSegment | fusionPathSegment))*
    ;

fusionPathSegment
    : FUSION_PATH_SEGMENT
    ;

fusionMetaPropPathSegment
    : FUSION_META_PROP_PREFIX FUSION_PATH_SEGMENT
    ;

fusionConfigurationCopy
    : fusionConfigurationPath
      COPY_OPERATOR
      fusionConfigurationPathReference
      fusionConfigurationBody?
    ;

fusionConfigurationPathReference
    : FUSION_PATH_NESTING_SEPARATOR? fusionConfigurationPath
    ;

/*
--------- Fusion values
*/

rootFusionValueDecl
    : fusionValueLiteralDecl
      rootFusionValueBody?
    ;

fusionValueDecl
    : fusionValueLiteralDecl
      fusionValueBody?
    ;

rootFusionValueBody
    : ROOT_FUSION_BODY_START
      fusionFragment*
      FUSION_BODY_END
    ;

fusionValueBody
    : FUSION_BODY_START
      fusionFragment*
      FUSION_BODY_END
    ;

fusionValueLiteralDecl
    : fusionValueNull
    | fusionValueStringSingleQuote
    | fusionValueStringDoubleQuote
    | fusionValueNumber
    | fusionValueBoolean
    | fusionValueExpression
    | fusionValueDslDelegate
    | fusionValueObject
    ;

fusionValueNull: FUSION_VALUE_LITERAL_NULL;
fusionValueStringSingleQuote: FUSION_VALUE_STRING_SQUOTE;
fusionValueStringDoubleQuote: FUSION_VALUE_STRING_DQUOTE;
fusionValueNumber: FUSION_VALUE_NUMBER;
fusionValueBoolean: FUSION_VALUE_BOOLEAN;
fusionValueDslDelegate: FUSION_VALUE_DSL_DELEGATE;
fusionValueObject: FUSION_VALUE_OBJECT;
fusionValueExpression
    : FUSION_VALUE_EXPRESSION_START
      (EL_CODE_START_BLOCK | EL_CODE_END_BLOCK | EL_STRING | EL_CODE_FRAGMENT)*
      EL_CODE_END_BLOCK
    ;
