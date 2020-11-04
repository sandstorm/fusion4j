Feature: Neos.Fusion:Matcher implementation

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

  Scenario Outline: matcher with renderer and object
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      foo = Neos.Fusion:Matcher {
        condition = ${<condition>}
        renderer = Bdd.TestValue {
          value = ${1 + 1}
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | condition | type                                            | output                |
      | 10 > 1    | java.lang.Integer                               | 2                     |
      | 10 < 1    | io.neos.fusion4j.neos.fusion.impl.NoMatchResult | ---NO-MATCH-RESULT--- |

  Scenario Outline: matcher and renderer with primitive value
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      foo = Neos.Fusion:Matcher {
        condition = ${<condition>}
        renderer = 22
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | condition | type                                            | output                |
      | 10 > 1    | java.lang.Integer                               | 22                    |
      | 10 < 1    | io.neos.fusion4j.neos.fusion.impl.NoMatchResult | ---NO-MATCH-RESULT--- |

  Scenario Outline: matcher and renderer in type mode
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      prototype(Foo) < prototype(Bdd.TestValue) {
        value = 22
      }
      foo = Neos.Fusion:Matcher {
        condition = ${<condition>}
        type = 'Foo'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | condition | type                                            | output                |
      | 10 > 1    | java.lang.Integer                               | 22                    |
      | 10 < 1    | io.neos.fusion4j.neos.fusion.impl.NoMatchResult | ---NO-MATCH-RESULT--- |

  Scenario Outline: matcher and renderer in absolute render path mode
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      bar = Bdd.TestValue {
        value = 22
      }
      foo = Neos.Fusion:Matcher {
        condition = ${<condition>}
        renderPath = '/bar'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | condition | type                                            | output                |
      | 10 > 1    | java.lang.Integer                               | 22                    |
      | 10 < 1    | io.neos.fusion4j.neos.fusion.impl.NoMatchResult | ---NO-MATCH-RESULT--- |

  Scenario Outline: matcher and renderer in relative render path mode
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: *
      foo = Neos.Fusion:Matcher {
        condition = ${<condition>}
        renderPath = '.bar'

        bar = Bdd.TestValue {
          value = 22
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "<type>"
    Then the evaluated output for path "foo" must be
      """
      <output>
      """
    Examples:
      | condition | type                                            | output                |
      | 10 > 1    | java.lang.Integer                               | 22                    |
      | 10 < 1    | io.neos.fusion4j.neos.fusion.impl.NoMatchResult | ---NO-MATCH-RESULT--- |
