Feature: resolving of the '@class' meta property by the runtime to evaluate a Fusion object instance

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the test Fusion implementation "foo.Bar" with static string output "foo bar"
    Given the test Fusion implementation "foo.BeforeErasure" with static string output "before erasure"
    Given the test Fusion implementation "foo.AfterErasure" with static string output "after erasure"
    Given the Fusion file "Base.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: complex class sources for prototypes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(FooBar) {
        @class = 'foo.Bar'
      }

      prototype(FooBar).@class = 'foo.BeforeErasure'
      prototype(FooBar) >
      prototype(FooBar).@class = 'foo.AfterErasure'

      prototype(Some:Fancy.Prototype) {
        something = 'here'
      }

      prototype(Some:Fancy.Prototype).@class < prototype(FooBar).@class

      testPath = Some:Fancy.Prototype
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      after erasure
      """

  Scenario: class sources for prototypes from inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(Indirection) < prototype(Bdd.TestValue) {
        value = 'indirection'
      }

      prototype(Some:Fancy.Prototype) < prototype(Indirection) {
        value = 'extension'
      }

      testPath = Some:Fancy.Prototype {
        some = 'other'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      extension
      """
