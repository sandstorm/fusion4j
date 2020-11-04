Feature: Neos.Fusion:Renderer implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      """

  Scenario: renderer with object
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Renderer {
        renderer = Bdd.TestValue {
          value = ${1 + 1}
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      2
      """

  Scenario: renderer with primitive value
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Renderer {
        renderer = 22
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """

  Scenario: renderer in type mode
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.TestValue) {
        value = 22
      }
      foo = Neos.Fusion:Renderer {
        type = 'Foo'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """

  Scenario: renderer in absolute render path mode
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      bar = Bdd.TestValue {
        value = 22
      }
      foo = Neos.Fusion:Renderer {
        renderPath = '/bar'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """

  Scenario Outline: renderer in relative render path mode
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Renderer {
        renderPath = '<renderPath>'

        bar = Bdd.TestValue {
          value = 22
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """
    Examples:
      | renderPath |
      | .bar       |
      | bar        |