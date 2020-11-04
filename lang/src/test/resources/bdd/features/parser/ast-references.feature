Feature: syntax tree references are kept in the raw lang meta model

  For runtime and code evaluation use cases, references to the original code position
  are part of the language meta model public API.

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: AST reference for primitive values
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // string single quote
      stringSingleQuote = 'hello world!'
      // string double quote
      stringDoubleQuote = "something in double quotes"

      // null value
      nullValue = null

      // boolean values
      boolTrue = true
      boolFalse = false

      // number values
      intValue = 42
      doubleValue = 3.1415
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 15 at char 21
    And the element at "Root.fusion/stringSingleQuote[1]" must start in line 2 at char 1 and end in line 2 at char 35
    And the path name at "Root.fusion/stringSingleQuote[1]" must start in line 2 at char 1 and end in line 2 at char 18
    And the value at "Root.fusion/stringSingleQuote[1]" must start in line 2 at char 21 and end in line 2 at char 35
    And the element at "Root.fusion/stringDoubleQuote[3]" must start in line 4 at char 1 and end in line 4 at char 49
    And the path name at "Root.fusion/stringDoubleQuote[3]" must start in line 4 at char 1 and end in line 4 at char 18
    And the value at "Root.fusion/stringDoubleQuote[3]" must start in line 4 at char 21 and end in line 4 at char 49
    And the element at "Root.fusion/nullValue[5]" must start in line 7 at char 1 and end in line 7 at char 17
    And the path name at "Root.fusion/nullValue[5]" must start in line 7 at char 1 and end in line 7 at char 10
    And the value at "Root.fusion/nullValue[5]" must start in line 7 at char 13 and end in line 7 at char 17
    And the element at "Root.fusion/boolTrue[7]" must start in line 10 at char 1 and end in line 10 at char 16
    And the path name at "Root.fusion/boolTrue[7]" must start in line 10 at char 1 and end in line 10 at char 9
    And the value at "Root.fusion/boolTrue[7]" must start in line 10 at char 12 and end in line 10 at char 16
    And the element at "Root.fusion/boolFalse[8]" must start in line 11 at char 1 and end in line 11 at char 18
    And the path name at "Root.fusion/boolFalse[8]" must start in line 11 at char 1 and end in line 11 at char 10
    And the value at "Root.fusion/boolFalse[8]" must start in line 11 at char 13 and end in line 11 at char 18
    And the element at "Root.fusion/intValue[10]" must start in line 14 at char 1 and end in line 14 at char 14
    And the path name at "Root.fusion/intValue[10]" must start in line 14 at char 1 and end in line 14 at char 9
    And the value at "Root.fusion/intValue[10]" must start in line 14 at char 12 and end in line 14 at char 14
    And the element at "Root.fusion/doubleValue[11]" must start in line 15 at char 1 and end in line 15 at char 21
    And the path name at "Root.fusion/doubleValue[11]" must start in line 15 at char 1 and end in line 15 at char 12
    And the value at "Root.fusion/doubleValue[11]" must start in line 15 at char 15 and end in line 15 at char 21

  Scenario: AST reference for expression values
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // expression value assignment top level code layer
      foo = ${someFancyExpressionThatWeNeedToReimplementInJava('value')}
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 2 at char 67
    And the element at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 67
    And the path name at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 4
    And the value at "Root.fusion/foo[1]" must start in line 2 at char 7 and end in line 2 at char 67

  Scenario: AST reference for DSL delegate values
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // expression value assignment top level code layer
      foo = dsl`
          <div class={props.cssClass}>
              some nice AFX declaration
          </div>
      `

      singleLineDsl = dsl`<div>very simple</div>`
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And  the block at "Root.fusion" must start in line 1 at char 1 and end in line 8 at char 44
    And the element at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 6 at char 2
    And the path name at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 4
    And the value at "Root.fusion/foo[1]" must start in line 2 at char 7 and end in line 6 at char 2
    And the element at "Root.fusion/singleLineDsl[2]" must start in line 8 at char 1 and end in line 8 at char 44
    And the path name at "Root.fusion/singleLineDsl[2]" must start in line 8 at char 1 and end in line 8 at char 14
    And the value at "Root.fusion/singleLineDsl[2]" must start in line 8 at char 17 and end in line 8 at char 44

  Scenario: AST reference for single path segments on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // single regular segmented path
      foo = 'bar'

      // single prototype call / erasure segmented path
      prototype(Hello) >

      // single meta property path
      @context {
          // something in here
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 10 at char 2
    And the path name at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 4
    And the path name at "Root.fusion/foo[1]" must have the following segment AST references
      | name | type     | startLine | startChar | endLine | endChar |
      | foo  | PROPERTY | 2         | 1         | 2       | 4       |
    And the path name at "Root.fusion/prototype(Hello)[3]" must start in line 5 at char 1 and end in line 5 at char 17
    And the path name at "Root.fusion/prototype(Hello)[3]" must have the following segment AST references
      | name             | type           | startLine | startChar | endLine | endChar |
      | prototype(Hello) | PROTOTYPE_CALL | 5         | 1         | 5       | 17      |
    And the path name at "Root.fusion/@context[5]" must start in line 8 at char 1 and end in line 8 at char 9
    And the path name at "Root.fusion/@context[5]" must have the following segment AST references
      | name     | type          | startLine | startChar | endLine | endChar |
      | @context | META_PROPERTY | 8         | 1         | 8       | 9       |

  Scenario: AST reference for single path segments inside a fusion block
    Given the Fusion file "Root.fusion" contains the following code
      """
      // outer config
      someConfiguration {
          // single regular segmented path
          foo = 'bar'

          // single prototype call / erasure segmented path
          prototype(Hello) >

          // single meta property path
          @context {
              // something in here
          }
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 13 at char 2
    And the path name at "Root.fusion/someConfiguration[1]/foo[1]" must start in line 4 at char 5 and end in line 4 at char 8
    And the path name at "Root.fusion/someConfiguration[1]/foo[1]" must have the following segment AST references
      | name | type     | startLine | startChar | endLine | endChar |
      | foo  | PROPERTY | 4         | 5         | 4       | 8       |
    And the path name at "Root.fusion/someConfiguration[1]/prototype(Hello)[3]" must start in line 7 at char 5 and end in line 7 at char 21
    And the path name at "Root.fusion/someConfiguration[1]/prototype(Hello)[3]" must have the following segment AST references
      | name             | type           | startLine | startChar | endLine | endChar |
      | prototype(Hello) | PROTOTYPE_CALL | 7         | 5         | 7       | 21      |
    And the path name at "Root.fusion/someConfiguration[1]/@context[5]" must start in line 10 at char 5 and end in line 10 at char 13
    And the path name at "Root.fusion/someConfiguration[1]/@context[5]" must have the following segment AST references
      | name     | type          | startLine | startChar | endLine | endChar |
      | @context | META_PROPERTY | 10        | 5         | 10      | 13      |

  Scenario: AST reference for nested root paths
    Given the Fusion file "Root.fusion" contains the following code
      """
      // some complex path
      foo.prototype(Foo.Bar:Baz).@context.baz = 'bar'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 2 at char 48
    And the path name at "Root.fusion/foo.prototype(Foo.Bar:Baz).@context.baz[1]" must start in line 2 at char 1 and end in line 2 at char 40
    And the path name at "Root.fusion/foo.prototype(Foo.Bar:Baz).@context.baz[1]" must have the following segment AST references
      | name                   | type           | startLine | startChar | endLine | endChar |
      | foo                    | PROPERTY       | 2         | 1         | 2       | 4       |
      | prototype(Foo.Bar:Baz) | PROTOTYPE_CALL | 2         | 5         | 2       | 27      |
      | @context               | META_PROPERTY  | 2         | 28        | 2       | 36      |
      | baz                    | PROPERTY       | 2         | 37        | 2       | 40      |

  Scenario: AST reference for nested paths inside a fusion block
    Given the Fusion file "Root.fusion" contains the following code
      """
      // outer config
      myConfiguration {
          // some complex path
          foo.prototype(Foo.Bar:Baz).@context.baz = 'bar'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 5 at char 2
    And the path name at "Root.fusion/myConfiguration[1]/foo.prototype(Foo.Bar:Baz).@context.baz[1]" must start in line 4 at char 5 and end in line 4 at char 44
    And the path name at "Root.fusion/myConfiguration[1]/foo.prototype(Foo.Bar:Baz).@context.baz[1]" must have the following segment AST references
      | name                   | type           | startLine | startChar | endLine | endChar |
      | foo                    | PROPERTY       | 4         | 5         | 4       | 8       |
      | prototype(Foo.Bar:Baz) | PROTOTYPE_CALL | 4         | 9         | 4       | 31      |
      | @context               | META_PROPERTY  | 4         | 32        | 4       | 40      |
      | baz                    | PROPERTY       | 4         | 41        | 4       | 44      |

  Scenario: AST reference for root path assignment of a primitive
    Given the Fusion file "Root.fusion" contains the following code
      """
      // most simple assignment on top level code layer
      foo = 'bar'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And  the block at "Root.fusion" must start in line 1 at char 1 and end in line 2 at char 12
    And the element at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 12
    And the path name at "Root.fusion/foo[1]" must start in line 2 at char 1 and end in line 2 at char 4
    And the value at "Root.fusion/foo[1]" must start in line 2 at char 7 and end in line 2 at char 12

  Scenario: AST reference for root path assignment of a fusion object with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // fusion object value with multi-line body
      objectWithBody = SomeObject {
          bar = 'baz'
      }

      // fusion object values with single-line body
      objectWithBodySingleLine = SomeObject { bar = 'baz' }
      objectWithEmptyBodySingleLine = SomeObject { }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And  the block at "Root.fusion" must start in line 1 at char 1 and end in line 8 at char 47
    And the element at "Root.fusion/objectWithBody[1]" must start in line 2 at char 1 and end in line 4 at char 2
    And the path name at "Root.fusion/objectWithBody[1]" must start in line 2 at char 1 and end in line 2 at char 15
    And the path assignment at "Root.fusion/objectWithBody[1]" must have a body
    And the value at "Root.fusion/objectWithBody[1]" must start in line 2 at char 18 and end in line 2 at char 28
    And the assignment body at "Root.fusion/objectWithBody[1]" must start in line 2 at char 29 and end in line 4 at char 2
    And the element at "Root.fusion/objectWithBodySingleLine[3]" must start in line 7 at char 1 and end in line 7 at char 54
    And the path name at "Root.fusion/objectWithBodySingleLine[3]" must start in line 7 at char 1 and end in line 7 at char 25
    And the path assignment at "Root.fusion/objectWithBodySingleLine[3]" must have a body
    And the value at "Root.fusion/objectWithBodySingleLine[3]" must start in line 7 at char 28 and end in line 7 at char 38
    And the assignment body at "Root.fusion/objectWithBodySingleLine[3]" must start in line 7 at char 39 and end in line 7 at char 54
    And the element at "Root.fusion/objectWithEmptyBodySingleLine[4]" must start in line 8 at char 1 and end in line 8 at char 47
    And the path name at "Root.fusion/objectWithEmptyBodySingleLine[4]" must start in line 8 at char 1 and end in line 8 at char 30
    And the path assignment at "Root.fusion/objectWithEmptyBodySingleLine[4]" must have a body
    And the value at "Root.fusion/objectWithEmptyBodySingleLine[4]" must start in line 8 at char 33 and end in line 8 at char 43
    And the assignment body at "Root.fusion/objectWithEmptyBodySingleLine[4]" must start in line 8 at char 44 and end in line 8 at char 47

  Scenario: AST reference for root path assignment of a fusion object without body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // fusion object value with no body
      objectWithoutBody = SomeObject

      // fusion object value with no body at the last line with no NL
      objectWithoutBodyAtLastLine = SomeObject
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 5 at char 41
    And the element at "Root.fusion/objectWithoutBody[1]" must start in line 2 at char 1 and end in line 2 at char 31
    And the path name at "Root.fusion/objectWithoutBody[1]" must start in line 2 at char 1 and end in line 2 at char 18
    And the value at "Root.fusion/objectWithoutBody[1]" must start in line 2 at char 21 and end in line 2 at char 31
    And the element at "Root.fusion/objectWithoutBodyAtLastLine[3]" must start in line 5 at char 1 and end in line 5 at char 41
    And the path name at "Root.fusion/objectWithoutBodyAtLastLine[3]" must start in line 5 at char 1 and end in line 5 at char 28
    And the value at "Root.fusion/objectWithoutBodyAtLastLine[3]" must start in line 5 at char 31 and end in line 5 at char 41

  Scenario: AST reference for inner path assignment of a primitive
    Given the Fusion file "Root.fusion" contains the following code
      """
      // assignment inside the body of a fusion object value
      objectWithBody = SomeObject {
          bar = 'baz'
      }

      // assignment inside a configuration block
      configurationBlock {
          bar = 'baz'
      }

      // assignment inside of a prototype declaration
      prototype(Foo) < prototype(Bar) {
          bar = 'baz'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 14 at char 2
    And the element at "Root.fusion/objectWithBody[1]/bar[0]" must start in line 3 at char 5 and end in line 3 at char 16
    And the path name at "Root.fusion/objectWithBody[1]/bar[0]" must start in line 3 at char 5 and end in line 3 at char 8
    And the value at "Root.fusion/objectWithBody[1]/bar[0]" must start in line 3 at char 11 and end in line 3 at char 16
    And the element at "Root.fusion/configurationBlock[3]/bar[0]" must start in line 8 at char 5 and end in line 8 at char 16
    And the path name at "Root.fusion/configurationBlock[3]/bar[0]" must start in line 8 at char 5 and end in line 8 at char 8
    And the value at "Root.fusion/configurationBlock[3]/bar[0]" must start in line 8 at char 11 and end in line 8 at char 16
    And the element at "Root.fusion/prototype(Foo)[5]/bar[0]" must start in line 13 at char 5 and end in line 13 at char 16
    And the path name at "Root.fusion/prototype(Foo)[5]/bar[0]" must start in line 13 at char 5 and end in line 13 at char 8
    And the value at "Root.fusion/prototype(Foo)[5]/bar[0]" must start in line 13 at char 11 and end in line 13 at char 16

  Scenario: AST reference for root path configuration
    Given the Fusion file "Root.fusion" contains the following code
      """
      foo {
          // some empty body
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 3 at char 2
    And the element at "Root.fusion/foo[0]" must start in line 1 at char 1 and end in line 3 at char 2
    And the path name at "Root.fusion/foo[0]" must start in line 1 at char 1 and end in line 1 at char 4
    And the block at "Root.fusion/foo[0]" must start in line 1 at char 5 and end in line 3 at char 2

  Scenario: AST reference for inner path configuration
    Given the Fusion file "Root.fusion" contains the following code
      """
      // assignment inside the body of a fusion object value
      objectWithBody = SomeObject {
          foo {
              // some empty body
          }
      }

      // assignment inside a configuration block
      configurationBlock {
          foo {
              // some empty body
          }
      }

      // assignment inside of a prototype declaration
      prototype(Foo) < prototype(Bar) {
          foo {
              // some empty body
          }
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 20 at char 2
    And the element at "Root.fusion/objectWithBody[1]/foo[0]" must start in line 3 at char 5 and end in line 5 at char 6
    And the path name at "Root.fusion/objectWithBody[1]/foo[0]" must start in line 3 at char 5 and end in line 3 at char 8
    And the block at "Root.fusion/objectWithBody[1]/foo[0]" must start in line 3 at char 9 and end in line 5 at char 6
    And the element at "Root.fusion/configurationBlock[3]/foo[0]" must start in line 10 at char 5 and end in line 12 at char 6
    And the path name at "Root.fusion/configurationBlock[3]/foo[0]" must start in line 10 at char 5 and end in line 10 at char 8
    And the block at "Root.fusion/configurationBlock[3]/foo[0]" must start in line 10 at char 9 and end in line 12 at char 6
    And the element at "Root.fusion/prototype(Foo)[5]/foo[0]" must start in line 17 at char 5 and end in line 19 at char 6
    And the path name at "Root.fusion/prototype(Foo)[5]/foo[0]" must start in line 17 at char 5 and end in line 17 at char 8
    And the block at "Root.fusion/prototype(Foo)[5]/foo[0]" must start in line 17 at char 9 and end in line 19 at char 6

  Scenario: AST reference for prototype declaration with inheritance and body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // root prototype declaration
      prototype(Foo) < prototype(Bar) {
          // something inside a prototype declaration ...
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 4 at char 2
    And the prototype declaration at "Root.fusion/prototype(Foo)[1]" must have a body
    And the element at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 1 and end in line 4 at char 2
    And the block at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 33 and end in line 4 at char 2

  Scenario: AST reference for prototype declaration with inheritance and without body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // root prototype declaration
      prototype(Foo) < prototype(Bar)
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 2 at char 32
    And the prototype declaration at "Root.fusion/prototype(Foo)[1]" must have no body
    And the element at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 1 and end in line 2 at char 32

  Scenario: AST reference for prototype declaration without inheritance and body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // root prototype declaration
      prototype(Foo)
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 2 at char 15
    And the prototype declaration at "Root.fusion/prototype(Foo)[1]" must have no body
    And the element at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 1 and end in line 2 at char 15

  Scenario: AST reference for prototype declaration without inheritance and with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // root prototype declaration
      prototype(Foo) {
          // something inside a prototype declaration ...
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 4 at char 2
    And the prototype declaration at "Root.fusion/prototype(Foo)[1]" must have a body
    And the element at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 1 and end in line 4 at char 2
    And the block at "Root.fusion/prototype(Foo)[1]" must start in line 2 at char 16 and end in line 4 at char 2

  Scenario: AST reference for single line code comments
    Given the Fusion file "Root.fusion" contains the following code
      """
      // root layer comment

      objectAssignment = Foo {
          // code comment inside of a fusion object body
      }

      pathConfiguration {
          // code comment inside a path configuration
      }

      prototype(Foo) {
          // code comment inside a prototype declaration
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 13 at char 2
    And the element at "Root.fusion/[0]" must start in line 1 at char 1 and end in line 1 at char 22
    And the element at "Root.fusion/objectAssignment[1]/[0]" must start in line 4 at char 5 and end in line 4 at char 51
    And the element at "Root.fusion/pathConfiguration[2]/[0]" must start in line 8 at char 5 and end in line 8 at char 48
    And the element at "Root.fusion/prototype(Foo)[3]/[0]" must start in line 12 at char 5 and end in line 12 at char 51

  Scenario: AST reference for multi line code comments
    Given the Fusion file "Root.fusion" contains the following code
      """
      /*
       root layer comment
       */

      objectAssignment = Foo {
          /* code comment inside of a fusion object body */
      }

      pathConfiguration {
          /*
           * code comment inside a path configuration
           */
      }

      prototype(Foo) {
          /*
           code comment inside a prototype declaration

          */
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 20 at char 2
    And the element at "Root.fusion/[0]" must start in line 1 at char 1 and end in line 3 at char 4
    And the element at "Root.fusion/objectAssignment[1]/[0]" must start in line 6 at char 5 and end in line 6 at char 54
    And the element at "Root.fusion/pathConfiguration[2]/[0]" must start in line 10 at char 5 and end in line 12 at char 8
    And the element at "Root.fusion/prototype(Foo)[3]/[0]" must start in line 16 at char 5 and end in line 19 at char 7

  Scenario: AST reference for root path erasure
    Given the Fusion file "Root.fusion" contains the following code
      """
      something >
      another.path >
      something.prototype(Foo) >
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 3 at char 27
    And the element at "Root.fusion/something[0]" must start in line 1 at char 1 and end in line 1 at char 12
    And the path name at "Root.fusion/something[0]" must start in line 1 at char 1 and end in line 1 at char 10
    And the element at "Root.fusion/another.path[1]" must start in line 2 at char 1 and end in line 2 at char 15
    And the path name at "Root.fusion/another.path[1]" must start in line 2 at char 1 and end in line 2 at char 13
    And the element at "Root.fusion/something.prototype(Foo)[2]" must start in line 3 at char 1 and end in line 3 at char 27
    And the path name at "Root.fusion/something.prototype(Foo)[2]" must start in line 3 at char 1 and end in line 3 at char 25

  Scenario: AST reference for inner path erasure
    Given the Fusion file "Root.fusion" contains the following code
      """
      pathConfiguration {
          // erasure inside of a path configuration
          something >
      }

      objectValue = Foo {
          // erasure inside of a fusion object value body
          something >
      }

      prototype(Foo) < prototype(Bar) {
          // erasure inside of a prototype declaration body
          something >
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 14 at char 2
    And the element at "Root.fusion/pathConfiguration[0]/something[1]" must start in line 3 at char 5 and end in line 3 at char 16
    And the path name at "Root.fusion/pathConfiguration[0]/something[1]" must start in line 3 at char 5 and end in line 3 at char 14
    And the element at "Root.fusion/objectValue[1]/something[1]" must start in line 8 at char 5 and end in line 8 at char 16
    And the path name at "Root.fusion/objectValue[1]/something[1]" must start in line 8 at char 5 and end in line 8 at char 14
    And the element at "Root.fusion/prototype(Foo)[2]/something[1]" must start in line 13 at char 5 and end in line 13 at char 16
    And the path name at "Root.fusion/prototype(Foo)[2]/something[1]" must start in line 13 at char 5 and end in line 13 at char 14

  Scenario: AST reference for prototype erasure
    Given the Fusion file "Root.fusion" contains the following code
      """
      prototype(Foo) >
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the block at "Root.fusion" must start in line 1 at char 1 and end in line 1 at char 17
    And the element at "Root.fusion/prototype(Foo)[0]" must start in line 1 at char 1 and end in line 1 at char 17

  # TODO ast ref tests for file includes
  # TODO ast ref tests for namespace aliases