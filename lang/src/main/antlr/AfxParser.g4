/*
 [The "BSD licence"]
 Copyright (c) 2013 Tom Everett
 All rights reserved.
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.
 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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

parser grammar AfxParser;

options { tokenVocab=AfxLexer; }

afxCode
    : afxFragment* EOF
    ;

afxFragment
    : fusionObjectTagStart
    | fusionObjectTagEnd
    | tagStart
    | tagEnd
    | bodyExpressionValue
    | htmlChardata
    | htmlComment
    | scriptlet
    | script
    | style
    | xhtmlCDATA
    | xml
    | dtd
    ;

bodyExpressionValue
    : FUSION_VALUE_EXPRESSION_START
      (EL_CODE_START_BLOCK | EL_CODE_END_BLOCK | EL_STRING | EL_CODE_FRAGMENT)*
      EL_CODE_END_BLOCK
    ;

tagAttributeExpressionValue
    : ATTVALUE_FUSION_VALUE_EXPRESSION_START
      (EL_CODE_START_BLOCK | EL_CODE_END_BLOCK | EL_STRING | EL_CODE_FRAGMENT)*
      EL_CODE_END_BLOCK
    ;

fusionObjectTagStart
    : TAG_OPEN fusionObjectTagName TAG_WHITESPACE* htmlAttribute* TAG_WHITESPACE* (TAG_CLOSE | TAG_SLASH_CLOSE)
    ;

// An end tag normally must not contain attributes,
// but due to invalid tags (e.g. invalid whitespace and tag soup from JSDom in prerendering)
// we can end up with that situation and the parser should recognize it.
fusionObjectTagEnd
    : TAG_OPEN TAG_SLASH fusionObjectTagName TAG_WHITESPACE* htmlAttribute* TAG_WHITESPACE* TAG_CLOSE
    ;

fusionObjectTagName
    : TAG_NAME TAG_PROTOTYPE_NAMESPACE_SEPARATOR TAG_NAME
    ;

tagStart
    : TAG_OPEN htmlTagName TAG_WHITESPACE* htmlAttribute* TAG_WHITESPACE* (TAG_CLOSE | TAG_SLASH_CLOSE)
    ;

// An end tag normally must not contain attributes,
// but due to invalid tags (e.g. invalid whitespace and tag soup from JSDom in prerendering)
// we can end up with that situation and the parser should recognize it.
tagEnd
    : TAG_OPEN TAG_SLASH htmlTagName TAG_WHITESPACE* htmlAttribute* TAG_WHITESPACE* TAG_CLOSE
    ;

htmlAttribute
    : htmlAttributeName TAG_EQUALS htmlAttributeValue TAG_WHITESPACE*
    | htmlAttributeName TAG_WHITESPACE*
    | tagAttributeSpreadExpression TAG_WHITESPACE*
    ;

htmlAttributeName
    : TAG_NAME
    ;

htmlAttributeValue
    : tagAttributeExpressionValue
    | ATTVALUE_DOUBLE_QUOTED_VALUE
    | ATTVALUE_SINGLE_QUOTE_VALUE
    | ATTVALUE_OTHER_VALUE
    ;

tagAttributeSpreadExpression
    : TAG_ATTRIBUTE_SPREAD_EXPRESSION_START
      (EL_CODE_START_BLOCK | EL_CODE_END_BLOCK | EL_STRING | EL_CODE_FRAGMENT)*
      EL_CODE_END_BLOCK
    ;

htmlTagName
    : TAG_NAME
    ;

htmlChardata
    : HTML_TEXT
    | SEA_WS
    ;

htmlComment
    : HTML_COMMENT
    | HTML_CONDITIONAL_COMMENT
    ;

xhtmlCDATA
    : CDATA
    ;

dtd
    : DTD
    ;

xml
    : XML_DECLARATION
    ;

scriptlet
    : SCRIPTLET
    ;

script
    : SCRIPT_OPEN ( SCRIPT_BODY | SCRIPT_SHORT_BODY)
    ;

style
    : STYLE_OPEN ( STYLE_BODY | STYLE_SHORT_BODY)
    ;