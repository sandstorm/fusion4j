Feature: Neos.Fusion:DataStructure implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |
    Given the Fusion file "BddHelper.fusion" contains the following code
      """fusion
      prototype(Bdd.PrintDataStructure) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestPrintDataStructureImplementation'
        data = ${value}
      }
      """

  Scenario: empty data structure evaluates to empty list
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:DataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "io.neos.fusion4j.runtime.model.FusionDataStructure"
    And the evaluated output for path "foo" must have the collection size 0

  Scenario: type of simple data structure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:DataStructure {
         a = 'a'
         b = 'b'
         c = 'c'
      }
      foo.d = 'd'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "io.neos.fusion4j.runtime.model.FusionDataStructure"
    And the evaluated output for path "foo" must have the collection size 4

  Scenario: simple data structure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:DataStructure {
         a = 'a'
         b = 'b'
         c = 'c'
         cancelled = 'cancelled'
         cancelled.@if.alwaysFalse = false
      }
      foo.d = 'd'
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a
      b = b
      c = c
      d = d
      """

  Scenario: nested data structure context access
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:DataStructure {
         a = 'a'
         b {
            x = 'x'
            y = 'y'
         }
      }
      foo.@process.print = ${value.b.x}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      x
      """

  Scenario: type of data structure with nested untyped path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:DataStructure {
        a = 'a'
        b = 'b'
        c {
          x = 1
          y = 2
        }
      }
      foo.d = 'd'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "io.neos.fusion4j.runtime.model.FusionDataStructure"
    And the evaluated output for path "foo" must have the collection size 4

  Scenario: data structure with nested untyped path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:DataStructure {
        a = 'a'
        b = 'b'
        c {
          x = 1
          y = 2
        }
      }
      foo.d = 'd'
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      a = a
      b = b
      c {
          x = 1
          y = 2
      }
      d = d
      """

  Scenario: data structure in component with applied attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(NavigationItem) < prototype(Neos.Fusion:Component) {
        linkUrl = ''
        linkText = ''
        active = false

        renderer = Neos.Fusion:DataStructure {
            @apply.props = ${props}
            type = 'item'
            // sorted for deterministic test output
            linkUrl.@position = 1
            linkText.@position = 2
            active.@position = 3
            type.@position = 4
        }
      }

      foo = NavigationItem {
        linkUrl = 'https://sandstorm.de'
        linkText = 'Sandstorm'
        @process.print = Bdd.PrintDataStructure
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      linkUrl = https://sandstorm.de
      linkText = Sandstorm
      active = false
      type = item
      """
