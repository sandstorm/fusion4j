Feature: post processing a Fusion evaluation result via "@process.processorName"

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: simple processor
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath.@process.makeFoo = "foo"
      testPath = 123
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      foo
      """

  Scenario: simple conditional processor - true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath.@process.makeFoo = "foo"
      testPath.@process.makeFoo.@if.always = ${true}
      testPath = 123
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      foo
      """

  Scenario: simple conditional processor - false
    Given the Fusion file "Root.fusion" contains the following code
      """
      testPath.@process.makeFoo = "foo"
      testPath.@process.makeFoo.@if.never = ${1 > 10}
      testPath = 123
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      123
      """
