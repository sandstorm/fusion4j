Feature: raw path operations

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: effective value of nested overridden declarations
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(FooBar) {
        value {
          foo = "bar"
        }
      }
      prototype(FooBar).value {
        foo = 123
      }
      prototype(FooBar).value.foo = 'effective value'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "prototype(FooBar).value.foo" should contain the following assignments
      | value           | type      | source                                |
      | effective value | [STRING]  | in-memory://MyTestPackage/Root.fusion |
      | 123             | [INTEGER] | in-memory://MyTestPackage/Root.fusion |
      | bar             | [STRING]  | in-memory://MyTestPackage/Root.fusion |
    Then the effective Fusion value of path "prototype(FooBar).value.foo" must be "effective value" with type "[STRING]"
