Feature: Neos.Fusion:Case implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: empty case evaluates to null
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Case
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be null

  Scenario: no cases match evaluates to null
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Case {
        @context.foo = 1
        case1 {
          condition = false
          renderer = 'value 1'
        }
        case2 {
          condition = ${foo == 0}
          renderer = 'value 2'
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be null

  Scenario Outline: case with a few matchers and default case
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Case {
        @context.someVar = "<someVar>"
        case1 {
          condition = ${"1" == someVar}
          renderer = "value 1"
        }
        case2 {
          condition = ${"2" == someVar}
          renderer = "value 2"
        }
        case3 {
          condition = ${"3" == someVar}
          renderer = "value 3"
        }
        default {
          condition = true
          renderer = "default value"
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | someVar | type             | output        |
      | 1       | java.lang.String | value 1       |
      | 2       | java.lang.String | value 2       |
      | 3       | java.lang.String | value 3       |
      | other   | java.lang.String | default value |

  Scenario Outline: case in component and AFX with props access
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(MyCaseComponent) < prototype(Neos.Fusion:Component) {
        mode = null

        renderer = Neos.Fusion:Case {
          case1 {
            condition = ${props.mode == 'case1'}
            renderer = 'value 1'
          }
          case2 {
            condition = ${props.mode == 'case2'}
            renderer = 'value 2'
          }
          default {
            @position = 'end'
            condition = true
            renderer = 'default'
          }
        }
      }
      foo = MyCaseComponent {
        mode = '<mode>'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | mode    | output  |
      | case1   | value 1 |
      | case2   | value 2 |
      | unknown | default |

  Scenario Outline: case in component and AFX with props access of nested data structure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(MyCaseComponent) < prototype(Neos.Fusion:Component) {
        config = Neos.Fusion:DataStructure

        renderer = Neos.Fusion:Case {
          case1 {
            condition = ${props.config.mode == 'case1'}
            renderer = 'value 1'
          }
          case2 {
            condition = ${props.config.mode == 'case2'}
            renderer = 'value 2'
          }
          default {
            @position = 'end'
            condition = true
            renderer = 'default'
          }
        }
      }
      foo = MyCaseComponent {
        config = Neos.Fusion:DataStructure {
          mode = '<mode>'
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | mode    | output  |
      | case1   | value 1 |
      | case2   | value 2 |
      | unknown | default |

