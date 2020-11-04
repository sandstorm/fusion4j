Feature: Neos.Fusion:Case implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: value defaults to null via Fusion code
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be null

  Scenario: value with null
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value {
        value = null
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be null

  Scenario Outline: value with primitive
    Given the Fusion file "Root.fusion" contains the following code
      """
      foo = Neos.Fusion:Value {
        value = <value>
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    And the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | value            | type              | output         |
      | 123              | java.lang.Integer | 123            |
      | -123.45          | java.lang.Double  | -123.45        |
      | true             | java.lang.Boolean | true           |
      | false            | java.lang.Boolean | false          |
      | "hallo Tammy <3" | java.lang.String  | hallo Tammy <3 |
      | 'some string'    | java.lang.String  | some string    |

  Scenario: nested value with context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value {
        @context.varX = 10
        value = Neos.Fusion:Value {
          @context.varY = 10
          value = Neos.Fusion:Value {
            value = ${varX + varY + 2}
          }
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    And the evaluated output for path "foo" must be
      """
      22
      """
