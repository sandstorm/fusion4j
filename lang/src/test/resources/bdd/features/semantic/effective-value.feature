Feature: language indexing of a parsed raw fusion model

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: multiple root path assignments
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      multipleAssignments = 'first value'
      multipleAssignments = 2
      multipleAssignments = Some:FusionObject

      include: '*.fusion'
      """
    Given the Fusion file "FooBar.fusion" contains the following code
      """fusion
      multipleAssignments = 'forth value'
      multipleAssignments = true
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "multipleAssignments" should contain the following assignments
      | value               | type            | source                                  |
      | true                | [BOOLEAN]       | in-memory://MyTestPackage/FooBar.fusion |
      | forth value         | [STRING]        | in-memory://MyTestPackage/FooBar.fusion |
      | <Some:FusionObject> | [FUSION_OBJECT] | in-memory://MyTestPackage/Root.fusion   |
      | 2                   | [INTEGER]       | in-memory://MyTestPackage/Root.fusion   |
      | first value         | [STRING]        | in-memory://MyTestPackage/Root.fusion   |

  Scenario: path configurations after assignments are not untyped
    Given the Fusion file "Root.fusion" contains the following code
      """
      foo >
      foo = 'actual value'
      foo {
        some = 'inner value'
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "foo" must be "actual value" with type "[STRING]"

  Scenario: multiple root path configurations
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: '*.fusion'
      multipleConfig {
        // block 1
      }
      multipleConfig {
        // block 2
      }
      """
    Given the Fusion file "FooBar.fusion" contains the following code
      """
      multipleConfig {
        // block 3
      }
      multipleConfig {
        // block 4
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "multipleConfig" must contain 4 configurations

  Scenario: multiple root path erasures
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: '*.fusion'
      multipleConfig >
      multipleConfig >
      """
    Given the Fusion file "FooBar.fusion" contains the following code
      """
      multipleConfig >
      multipleConfig >
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "multipleConfig" must contain 4 erasures

  Scenario: nested and block configurations merge in index
    Given the Fusion file "Root.fusion" contains the following code
      """
      nestedConfig {
        foo = "bar"
      }
      nestedConfig.foo = "baz"
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "nestedConfig" must contain 1 configurations
    And the raw path index for "nestedConfig.foo" should contain the following assignments
      | value | type     | source                                |
      | baz   | [STRING] | in-memory://MyTestPackage/Root.fusion |
      | bar   | [STRING] | in-memory://MyTestPackage/Root.fusion |

  Scenario: root prototype configurations and assignments merge in index
    Given the Fusion file "Root.fusion" contains the following code
      """
      prototype(Vendor:Foo.Bar) {
        value.foo = "bar"
      }
      prototype(Vendor:Foo.Bar).value.foo = 123
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "prototype(Vendor:Foo.Bar).value.foo" should contain the following assignments
      | value | type      | source                                |
      | 123   | [INTEGER] | in-memory://MyTestPackage/Root.fusion |
      | bar   | [STRING]  | in-memory://MyTestPackage/Root.fusion |
