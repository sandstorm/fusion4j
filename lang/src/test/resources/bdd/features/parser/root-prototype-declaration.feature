Feature: raw 'lang' model parsing for root prototype declaration syntax

  The main difference between a root prototype declaration body and a "regular"
  fusion path configuration block is, the prototype inheritance syntax feature.
  Those to fusion code blocks should theoretically not be handled differently,
  but should be tested separately (due to different internal code paths).

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: most simple root prototype declaration
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      /*
       the most simple fusion prototype declaration
        - no prototype inheritance
        - no body
        - no erasure
      */
      prototype(ABC)

      // supported prototype name characters
      prototype(abc.123:ABC)
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 root prototype declarations
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name        | hasBody |
      | 1     | ABC         | false   |
      | 3     | abc.123:ABC | false   |

  Scenario: root prototype declaration with body
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // empty body should be supported
      prototype(Some.EmptyBody) {}
      prototype(Some.EmptyBody2) {

      }

      // body with assignment, configuration and comments
      prototype(Some.Body:WithStuff) {
          // some assignment
          foo = null
          // some configuration
          bar {
              test = 123
          }
      }

      // prototype with inner prototype -> outer counts as top level prototype declaration
      prototype(Some.Body:WithInnerPrototype) {
          // inner counts as path configuration
          prototype(FooBar) {
              // nothing here but one comment
          }
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 4 root prototype declarations
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                         |
      | 1     | Some.EmptyBody               |
      | 2     | Some.EmptyBody2              |
      | 4     | Some.Body:WithStuff          |
      | 6     | Some.Body:WithInnerPrototype |
    And the model at "Root.fusion/prototype(Some.EmptyBody)[1]" must contain 0 elements
    And the model at "Root.fusion/prototype(Some.EmptyBody2)[2]" must contain 0 elements
    And the model at "Root.fusion/prototype(Some.Body:WithStuff)[4]" must contain 4 elements
    And the model at "Root.fusion/prototype(Some.Body:WithInnerPrototype)[6]" must contain 2 elements
    And the model at "Root.fusion/prototype(Some.Body:WithInnerPrototype)[6]" must contain 1 path configurations
    And the model at "Root.fusion/prototype(Some.Body:WithInnerPrototype)[6]/prototype(FooBar)[1]" must contain 1 elements

  Scenario: root prototype declaration with inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // only inheritance, no body
      prototype(Some.Concrete) < prototype(Some.Basic)

      // inheritance with empty body
      prototype(Some.Concrete2) < prototype(Some.Basic) {

      }

      // inheritance with body
      prototype(Some.Concrete:WithStuff) < prototype(Some.Other:Basic) {
          // some assignment
          foo = null
          // some configuration
          bar {
              test = 123
          }
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 root prototype declarations
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                    | inherit          | hasBody |
      | 1     | Some.Concrete           | Some.Basic       | false   |
      | 3     | Some.Concrete2          | Some.Basic       | true    |
      | 5     | Some.Concrete:WithStuff | Some.Other:Basic | true    |
    And the prototype declaration at "Root.fusion/prototype(Some.Concrete)[1]" must have no body
    And the model at "Root.fusion/prototype(Some.Concrete2)[3]" must contain 0 elements
    And the model at "Root.fusion/prototype(Some.Concrete:WithStuff)[5]" must contain 4 elements
    And the model at "Root.fusion/prototype(Some.Concrete:WithStuff)[5]" must contain the elements
      | index | type                    |
      | 0     | CodeComment             |
      | 1     | FusionPathAssignment    |
      | 2     | CodeComment             |
      | 3     | FusionPathConfiguration |
    And the model at "Root.fusion/prototype(Some.Concrete:WithStuff)[5]" must contain the path assignment "foo" at index 1 with value "NULL" of type "NullValue"
    And the model at "Root.fusion/prototype(Some.Concrete:WithStuff)[5]/bar[3]" must contain the path assignment "test" at index 0 with value "123" of type "IntegerValue"
