Feature: if conditions on paths via '@if.conditionName = ...'

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: simple static condition for primitive path - true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.always = true
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: multiple conditions logical AND - true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.first = true
      somePath.@if.second = true
      somePath.@if.third = true
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: multiple conditions logical AND - false
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.first = true
      somePath.@if.second = false
      somePath.@if.third = true
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: simple static condition for primitive path - false
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.never = false
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: static EEL condition for primitive path - true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.always = ${true}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: static EEL condition for primitive path - false
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.never = ${false}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: EEL condition with context value access - true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.checkWithContextAccess = ${value == 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: EEL condition with context value access - false
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath = 1
      somePath.@if.checkWithContextAccess = ${value > 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: EEL condition with custom context value access - true
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath = 1
      somePath.@context.foo = 10
      somePath.@if.checkWithContextAccess = ${value < foo}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: EEL condition with custom context value access - false
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath = 1
      somePath.@context.foo = 10
      somePath.@if.checkWithContextAccess = ${value > foo}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: nested if condition disables outer conditions - true
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath = 1
      somePath.@if.outer = false
      somePath.@if.outer.@if.inner = true
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be null

  Scenario: nested if condition enables outer conditions - false
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath = 1
      somePath.@if.outer = true
      somePath.@if.outer.@if.inner = false
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      1
      """

  Scenario: if condition and this pointer for simple path evaluation
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath {
        value.condition = true
        value = 1
        value.@if.conditionWithThisPointer = ${this.condition}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.value"
    Then the evaluated output for path "somePath.value" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath.value" must be
      """
      1
      """
