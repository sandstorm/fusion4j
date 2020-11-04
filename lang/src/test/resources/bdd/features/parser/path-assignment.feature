Feature: raw 'lang' model parsing for fusion path assignment syntax

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: value assignment on root code layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = "hallo"
      my.nestedPath = "hallo"
      @rootMetaProp = 1234
      test.prototype(ABC.XYZ).@context.test = false
      // some comment to increase the index of the next assignment
      prototype(Test).prototype(ABC.XYZ).test = 'Hallo'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 5 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path                                    | value | type         |
      | 0     | myPath                                  | hallo | StringValue  |
      | 1     | my.nestedPath                           | hallo | StringValue  |
      | 2     | @rootMetaProp                           | 1234  | IntegerValue |
      | 3     | test.prototype(ABC.XYZ).@context.test   | false | BooleanValue |
      | 5     | prototype(Test).prototype(ABC.XYZ).test | Hallo | StringValue  |

  Scenario: value assignment inside of a fusion configuration
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // outer configuration block
      outer {
          myPath = "hallo"
          my.nestedPath = "hallo"
          @rootMetaProp = 1234
          test.prototype(ABC.XYZ).@context.test = false
          // some comment to increase the index of the next assignment
          prototype(Test).prototype(ABC.XYZ).test = 'Hallo'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/outer[1]" must contain 5 path assignments
    And the model at "Root.fusion/outer[1]" must contain the path assignments
      | index | path                                    | value | type         |
      | 0     | myPath                                  | hallo | StringValue  |
      | 1     | my.nestedPath                           | hallo | StringValue  |
      | 2     | @rootMetaProp                           | 1234  | IntegerValue |
      | 3     | test.prototype(ABC.XYZ).@context.test   | false | BooleanValue |
      | 5     | prototype(Test).prototype(ABC.XYZ).test | Hallo | StringValue  |

  Scenario: value assignment inside of a fusion object assignment body
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // outer fusion object assignment
      outer = SomeFusionObject {
          myPath = "hallo"
          my.nestedPath = "hallo"
          @rootMetaProp = 1234
          test.prototype(ABC.XYZ).@context.test = false
          // some comment to increase the index of the next assignment
          prototype(Test).prototype(ABC.XYZ).test = 'Hallo'
      }
      """
    When all Fusion packages are parsed
    Then the model at "Root.fusion/outer[1]" must contain 5 path assignments
    And the model at "Root.fusion/outer[1]" must contain the path assignments
      | index | path                                    | value | type         |
      | 0     | myPath                                  | hallo | StringValue  |
      | 1     | my.nestedPath                           | hallo | StringValue  |
      | 2     | @rootMetaProp                           | 1234  | IntegerValue |
      | 3     | test.prototype(ABC.XYZ).@context.test   | false | BooleanValue |
      | 5     | prototype(Test).prototype(ABC.XYZ).test | Hallo | StringValue  |

  Scenario: value assignment inside of a top level prototype declaration
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // top level prototype declaration
      prototype(SomePrototypeName) {
          myPath = "hallo"
          my.nestedPath = "hallo"
          @rootMetaProp = 1234
          test.prototype(ABC.XYZ).@context.test = false
          // some comment to increase the index of the next assignment
          prototype(Test).prototype(ABC.XYZ).test = 'Hallo'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]" must contain 5 path assignments
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]" must contain the path assignments
      | index | path                                    | value | type         |
      | 0     | myPath                                  | hallo | StringValue  |
      | 1     | my.nestedPath                           | hallo | StringValue  |
      | 2     | @rootMetaProp                           | 1234  | IntegerValue |
      | 3     | test.prototype(ABC.XYZ).@context.test   | false | BooleanValue |
      | 5     | prototype(Test).prototype(ABC.XYZ).test | Hallo | StringValue  |

  Scenario: value assignment inside of a double nested fusion block
    Given the Fusion file "Root.fusion" contains the following code
      """
      // top level prototype declaration
      prototype(SomePrototypeName) {
          // inner path configuration
          myConfig {
              myPath = "hallo"
              my.nestedPath = "hallo"
              @rootMetaProp = 1234
              test.prototype(ABC.XYZ).@context.test = false
              // some comment to increase the index of the next assignment
              prototype(Test).prototype(ABC.XYZ).test = 'Hallo'
          }
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/myConfig[1]" must contain 5 path assignments
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/myConfig[1]" must contain the path assignments
      | index | path                                    | value | type         |
      | 0     | myPath                                  | hallo | StringValue  |
      | 1     | my.nestedPath                           | hallo | StringValue  |
      | 2     | @rootMetaProp                           | 1234  | IntegerValue |
      | 3     | test.prototype(ABC.XYZ).@context.test   | false | BooleanValue |
      | 5     | prototype(Test).prototype(ABC.XYZ).test | Hallo | StringValue  |

  Scenario: primitive value assignments
    Given the Fusion file "Root.fusion" contains the following code
      """
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
    And the model at "Root.fusion" must contain 7 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path              | value                      | type         |
      | 1     | stringSingleQuote | hello world!               | StringValue  |
      | 3     | stringDoubleQuote | something in double quotes | StringValue  |
      | 5     | nullValue         | NULL                       | NullValue    |
      | 7     | boolTrue          | true                       | BooleanValue |
      | 8     | boolFalse         | false                      | BooleanValue |
      | 10    | intValue          | 42                         | IntegerValue |
      | 11    | doubleValue       | 3.1415                     | DoubleValue  |

  Scenario: negative number assignments
    Given the Fusion file "Root.fusion" contains the following code
      """
      // negative number values
      intValue = -42
      doubleValue = -273.15
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path        | value   | type         |
      | 1     | intValue    | -42     | IntegerValue |
      | 2     | doubleValue | -273.15 | DoubleValue  |

  Scenario: case-insensitive boolean value assignments
    Given the Fusion file "Root.fusion" contains the following code
      """
      // 'true' and 'false' should work case-insensitive
      boolTrue1 = true
      boolTrue2 = TRUE
      boolTrue3 = TruE
      boolFalse1 = false
      boolFalse2 = FALSE
      boolFalse3 = fAlSe
      """
    When all Fusion packages are parsed
    Then the model at "Root.fusion" must contain 6 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path       | value | type         |
      | 1     | boolTrue1  | true  | BooleanValue |
      | 2     | boolTrue2  | true  | BooleanValue |
      | 3     | boolTrue3  | true  | BooleanValue |
      | 4     | boolFalse1 | false | BooleanValue |
      | 5     | boolFalse2 | false | BooleanValue |
      | 6     | boolFalse3 | false | BooleanValue |

  Scenario: case-insensitive null value assignments
    Given the Fusion file "Root.fusion" contains the following code
      """
      // 'null' should work case-insensitive
      nullValue1 = null
      nullValue2 = NULL
      nullValue3 = nUlL
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path       | value | type      |
      | 1     | nullValue1 | NULL  | NullValue |
      | 2     | nullValue2 | NULL  | NullValue |
      | 3     | nullValue3 | NULL  | NullValue |

  Scenario: string value assignment with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // single quote with non empty body
      stringSingleQuote = 'hello world!' {
          // something inside here
          @if.something = false
      }
      // single quote with empty body
      stringSingleQuoteEmpty = 'hello world!' {

      }
      // single quote with empty body single line
      stringSingleQuoteEmptySingle = 'hello world!' {}

      // string double quote
      stringDoubleQuote = "something in double quotes" {
          // something inside here
          @process.wrap = ${'[wrap]' + value + '[/wrap]'}
      }
      // string double quote empty body
      stringDoubleQuoteEmpty = "something in double quotes" {
      }
      // string double quote empty body single line
      stringDoubleQuoteEmptySingle = "something in double quotes" { }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 path assignments
    And the path assignment at "Root.fusion/stringSingleQuote[1]" must have a body
    And the model at "Root.fusion/stringSingleQuote[1]" must contain 2 elements
    And the model at "Root.fusion/stringSingleQuote[1]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/stringSingleQuoteEmpty[3]" must have a body
    And the model at "Root.fusion/stringSingleQuoteEmpty[3]" must contain 0 elements
    And the path assignment at "Root.fusion/stringSingleQuoteEmptySingle[5]" must have a body
    And the model at "Root.fusion/stringSingleQuoteEmptySingle[5]" must contain 0 elements
    And the path assignment at "Root.fusion/stringDoubleQuote[7]" must have a body
    And the model at "Root.fusion/stringDoubleQuote[7]" must contain 2 elements
    And the model at "Root.fusion/stringDoubleQuote[7]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/stringDoubleQuoteEmpty[9]" must have a body
    And the model at "Root.fusion/stringDoubleQuoteEmpty[9]" must contain 0 elements
    And the path assignment at "Root.fusion/stringDoubleQuoteEmptySingle[11]" must have a body
    And the model at "Root.fusion/stringDoubleQuoteEmptySingle[11]" must contain 0 elements

  Scenario: boolean value assignment with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // 'true' with non empty body
      boolTrue = true {
          // something inside here
          @if.something = false
      }
      // 'true' with empty body
      boolTrueEmpty = true {

      }
      // 'true' with empty body single line
      boolTrueEmptySingle = true {}

      // 'false' with non empty body
      boolFalse = false {
          // something inside here
          @process.wrap = ${'[wrap]' + value + '[/wrap]'}
      }
      // 'false' with empty body
      boolFalseEmpty = false {
      }
      // 'false' with empty body single line
      boolFalseEmptySingle = false { }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 path assignments
    And the path assignment at "Root.fusion/boolTrue[1]" must have a body
    And the model at "Root.fusion/boolTrue[1]" must contain 2 elements
    And the model at "Root.fusion/boolTrue[1]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/boolTrueEmpty[3]" must have a body
    And the model at "Root.fusion/boolTrueEmpty[3]" must contain 0 elements
    And the path assignment at "Root.fusion/boolTrueEmptySingle[5]" must have a body
    And the model at "Root.fusion/boolTrueEmptySingle[5]" must contain 0 elements
    And the path assignment at "Root.fusion/boolFalse[7]" must have a body
    And the model at "Root.fusion/boolFalse[7]" must contain 2 elements
    And the model at "Root.fusion/boolFalse[7]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/boolFalseEmpty[9]" must have a body
    And the model at "Root.fusion/boolFalseEmpty[9]" must contain 0 elements
    And the path assignment at "Root.fusion/boolFalseEmptySingle[11]" must have a body
    And the model at "Root.fusion/boolFalseEmptySingle[11]" must contain 0 elements

  Scenario: number value assignment with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // integer with non empty body
      intValue = 42 {
          // something inside here
          @if.something = -42
      }
      // negative double with empty body
      doubleValueEmpty = -273.15 {

      }
      // zero with empty body single line
      zeroEmptySingle = 0 {}
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 path assignments
    And the path assignment at "Root.fusion/intValue[1]" must have a body
    And the model at "Root.fusion/intValue[1]" must contain 2 elements
    And the model at "Root.fusion/intValue[1]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/doubleValueEmpty[3]" must have a body
    And the model at "Root.fusion/doubleValueEmpty[3]" must contain 0 elements
    And the path assignment at "Root.fusion/zeroEmptySingle[5]" must have a body
    And the model at "Root.fusion/zeroEmptySingle[5]" must contain 0 elements

  Scenario: null value assignment with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // null with non empty body
      nullValue = null {
          // something inside here
          @if.something = false
      }
      // null with empty body
      nullValueEmpty = null {

      }
      // null with empty body single line
      nullValueEmptySingle = null {}
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 path assignments
    And the path assignment at "Root.fusion/nullValue[1]" must have a body
    And the model at "Root.fusion/nullValue[1]" must contain 2 elements
    And the model at "Root.fusion/nullValue[1]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/nullValueEmpty[3]" must have a body
    And the model at "Root.fusion/nullValueEmpty[3]" must contain 0 elements
    And the path assignment at "Root.fusion/nullValueEmptySingle[5]" must have a body
    And the model at "Root.fusion/nullValueEmptySingle[5]" must contain 0 elements

  Scenario: expression value assignment with body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // expression with non empty body
      expressionValue = ${'some-expression'} {
          // something inside here
          @if.something = false
      }
      // null with empty body
      expressionValueEmpty = ${someOther('call')} {

      }
      // null with empty body single line
      expressionValueEmptySingle = ${false} {}
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 path assignments
    And the path assignment at "Root.fusion/expressionValue[1]" must have a body
    And the model at "Root.fusion/expressionValue[1]" must contain 2 elements
    And the model at "Root.fusion/expressionValue[1]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/expressionValueEmpty[3]" must have a body
    And the model at "Root.fusion/expressionValueEmpty[3]" must contain 0 elements
    And the path assignment at "Root.fusion/expressionValueEmptySingle[5]" must have a body
    And the model at "Root.fusion/expressionValueEmptySingle[5]" must contain 0 elements
