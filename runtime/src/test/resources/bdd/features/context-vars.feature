Feature: declaring context variables via "@context.varName"

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: simple context
    Given the Fusion file "Root.fusion" contains the following code
      """
      testPath.@context.foo = 1
      testPath = ${foo + 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      2
      """

  Scenario: simple context from Fusion object
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      testPath.@context.foo = Bdd.TestValue {
        value = 1
      }
      testPath = ${foo + 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      2
      """

  Scenario: fusion object context and this
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *

      testPath = Bdd.TestValue {
        bar = 2
        @context.foo = 1
        @context.bar = ${this.bar}
        value = ${foo + bar}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      3
      """
