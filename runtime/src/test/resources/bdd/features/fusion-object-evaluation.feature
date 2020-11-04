Feature: evaluation of Fusion objects via @class delegation

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "ClassMapping.fusion" contains the following code
      """fusion
      prototype(FooBar).@class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'

      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = 'default value'
      }
      """

  Scenario: evaluation of property path from prototype
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(FooBar) {
        value = 'value from prototype'
      }
      instancePath = FooBar
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      value from prototype
      """

  Scenario: evaluation of property path from instance body
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(FooBar) {
        value = 'value from prototype'
      }
      instancePath = FooBar {
        value = 'value from instance'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      value from instance
      """

  Scenario: evaluation of property path from instance path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(FooBar) {
        value = 'value from prototype'
      }
      instancePath = FooBar {
        value = 'value from instance'
      }
      instancePath.value = 'value from path'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      value from path
      """

  Scenario: nested evaluation of objects with outer path definition
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Inner) < prototype(Bdd.TestValue) {
        value = 'inner'
      }
      prototype(Outer) < prototype(Bdd.TestValue) {
        value = Inner
        value.value = 'outer'
      }
      instancePath = Outer
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      outer
      """

  Scenario: nested evaluation of objects with outer path definition via inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      prototype(Inner) < prototype(Bdd.TestValue) {
        value = 'inner'
      }
      prototype(Outer.Base) < prototype(Bdd.TestValue) {
        value.value = 'outer'
      }
      prototype(Outer) < prototype(Outer.Base) {
        value = Inner
      }
      instancePath = Outer
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      outer
      """

  Scenario: nested evaluation of objects
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      prototype(FooBar) {
        value = 'value from prototype'
      }
      prototype(SomeOther) < prototype(FooBar) {
        value = FooBar
      }
      instancePath = SomeOther
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "instancePath"
    Then the evaluated output for path "instancePath" must be of type "java.lang.String"
    And the evaluated output for path "instancePath" must be
      """
      value from prototype
      """

  Scenario: attributes from sources for prototypes from instance wins over inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      testPath = Some:Fancy.Prototype {
        value = 'from instance'
      }

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue) {
        value = 'from prototype'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      from instance
      """

  Scenario: erased prototype attributes are not indexed
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      testPath = Some:Fancy.Prototype

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue) {
        value >
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    # see TestValueImplementation
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      [UNDEFINED ATTRIBUTE ACCESS 'value']
      """

  Scenario: erased instance attributes are not indexed
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      testPath = Some:Fancy.Prototype {
        value >
      }

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue)
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    # see TestValueImplementation
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      [UNDEFINED ATTRIBUTE ACCESS 'value']
      """

  Scenario: erased extension attributes are not indexed
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *

      testPath.prototype(Some:Fancy.Prototype).value >
      testPath.foo = Some:Fancy.Prototype

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue)
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath.foo"
    # see TestValueImplementation
    Then the evaluated output for path "testPath.foo" must be of type "java.lang.String"
    And the evaluated output for path "testPath.foo" must be
      """
      [UNDEFINED ATTRIBUTE ACCESS 'value']
      """

  Scenario: context variables from prototype
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *

      prototype(WithContext) < prototype(FooBar) {
        value = ${fromPrototype + fromInstance + fromExtension}

        @context.fromPrototype = 2
      }

      // testPath = WithContext {
      testPath.something = WithContext {
        @context.fromInstance = 3
      }

      testPath.prototype(FooBar).@context.fromExtension = 4
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath.something"
    Then the evaluated output for path "testPath.something" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath.something" must be
      """
      9
      """