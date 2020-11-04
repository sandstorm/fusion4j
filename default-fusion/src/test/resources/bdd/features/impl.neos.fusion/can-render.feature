Feature: Neos.Fusion:CanRender implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario Outline: can render default components
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:CanRender {
        type = "<prototype>"
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Boolean"
    Then the evaluated output for path "foo" must be
      """
      true
      """
    Examples:
      | prototype                 |
      | Neos.Fusion:CanRender     |
      | Neos.Fusion:Case          |
      | Neos.Fusion:Component     |
      | Neos.Fusion:DataStructure |
      | Neos.Fusion:Join          |
      | Neos.Fusion:Matcher       |
      | Neos.Fusion:Renderer      |
      | Neos.Fusion:Tag           |
      | Neos.Fusion:Value         |
    # TODO more

  Scenario Outline: can render declared and unknown components
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) {
      }

      prototype(Vendor:Bar) {
      }

      foo = Neos.Fusion:CanRender {
        type = "<prototype>"
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Boolean"
    Then the evaluated output for path "foo" must be
      """
      <result>
      """
    Examples:
      | prototype   | result |
      | Foo         | true   |
      | Vendor:Bar  | true   |
      | Unknown:Foo | false  |
      | BlaBlubb    | false  |
