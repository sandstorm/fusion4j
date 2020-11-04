Feature: runtime evaluation of prototypes with extensions

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Base.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: prototype extension in nested path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(FooBar) < prototype(Bdd.TestValue) {
        value = 'default'
      }

      somePath.prototype(FooBar).value = 'extended'

      somePath.foo.bar = FooBar
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.foo.bar"
    Then the evaluated output for path "somePath.foo.bar" must be of type "java.lang.String"
    And the evaluated output for path "somePath.foo.bar" must be
      """
      extended
      """

  Scenario: nested prototype extension attribute evaluation
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(FooBar) < prototype(Bdd.TestValue) {
        value = 'bar'
      }
      // prototype extension for nested path
      somePath.prototype(FooBar) {
        value = 'extended'
      }

      somePath.myInstance = FooBar
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.myInstance"
    Then the evaluated output for path "somePath.myInstance" must be of type "java.lang.String"
    And the evaluated output for path "somePath.myInstance" must be
      """
      extended
      """

  Scenario: more concrete prototype extensions win over common ones
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *

      prototype(FooBar) < prototype(Bdd.TestValue) {
        value = 'default'
      }

      somePath.prototype(FooBar).value = 'commonly extended'
      somePath.foo.prototype(FooBar).value = 'more concrete extended'

      somePath.foo.bar = FooBar
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.foo.bar"
    Then the evaluated output for path "somePath.foo.bar" must be of type "java.lang.String"
    And the evaluated output for path "somePath.foo.bar" must be
      """
      more concrete extended
      """

  Scenario: prototype extensions from inherited extensions
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *

      prototype(FooBar) < prototype(Bdd.TestValue) {
        value = 'default'
      }

      prototype(Baz) < prototype(FooBar)

      somePath.prototype(FooBar).value = 'extended with base class'

      somePath.foo.bar = Baz
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.foo.bar"
    Then the evaluated output for path "somePath.foo.bar" must be of type "java.lang.String"
    And the evaluated output for path "somePath.foo.bar" must be
      """
      extended with base class
      """
