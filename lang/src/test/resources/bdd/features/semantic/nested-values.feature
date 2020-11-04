Feature: nested fusion path values

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: untyped nested paths for deep assignments
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      some.nested.path = 123
      some.other.path = true
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "some" must have the following attributes
      | path   | value      | type      |
      | nested | [NO-VALUE] | [UNTYPED] |
      | other  | [NO-VALUE] | [UNTYPED] |

  Scenario: nested Fusion path values with erasures
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      outer {
        inner {
          key1 = 1
          key2 = 2
        }
      }
      outer >
      outer.inner.key3 = 3
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "outer.inner" must have the following attributes
      | path | value | type      |
      | key3 | 3     | [INTEGER] |

  Scenario: untyped nested Fusion path values
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      outer >
      outer {
        foo {
          // ...
        }
        foo >
        inner >
        inner {
          key1 = 1
          key2 = 2
        }
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "outer" must have the following attributes
      | path  | value      | type      |
      | inner | [NO-VALUE] | [UNTYPED] |

  Scenario: untyped nested Fusion path values via copy
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      source {
        inner {
          key1 = 1
          key2 = 2
        }
      }
      target < source
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "target" must have the following attributes
      | path  | value      | type      |
      | inner | [NO-VALUE] | [UNTYPED] |
    And the path "target.inner" must have the following attributes
      | path | value | type      |
      | key1 | 1     | [INTEGER] |
      | key2 | 2     | [INTEGER] |
