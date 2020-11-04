Feature: Neos.Fusion:Map implementation

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

  Scenario: type of simple map with data structure and most default API
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        itemRenderer = ${item + '-mapped'}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "io.neos.fusion4j.runtime.model.FusionDataStructure"
    And the evaluated output for path "foo" must have the collection size 4

  Scenario: simple map with data structure and most default API
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        itemRenderer = ${item + '-mapped'}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a-mapped
      b = b-mapped
      c = c-mapped
      d = d-mapped
      """

  Scenario: content key as fallback item renderer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        content = ${item + '-mapped'}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a-mapped
      b = b-mapped
      c = c-mapped
      d = d-mapped
      """

  Scenario: itemRenderer wins over fallback content key
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        content = ${item + '-foobar'}
        itemRenderer = ${item + '-mapped'}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a-mapped
      b = b-mapped
      c = c-mapped
      d = d-mapped
      """

  Scenario: map with data structure and itemName
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        itemName = ${'element'}

        itemRenderer = ${element + '-mapped'}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a-mapped
      b = b-mapped
      c = c-mapped
      d = d-mapped
      """

  Scenario: map keys with data structure - default context key name itemKey
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemRenderer = ${itemKey + " -> " + item}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a -> 1
      b = b -> 2
      c = c -> 3
      d = d -> 4
      """

  Scenario: map keys with data structure - explicit context key name
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemKey = 'someKeyName'

        itemRenderer = ${someKeyName + " -> " + item}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a -> 1
      b = b -> 2
      c = c -> 3
      d = d -> 4
      """

  Scenario: key renderer with data structure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemRenderer = ${itemKey + " -> " + item}
        keyRenderer = ${itemKey + iterator.cycle}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a1 = a -> 1
      b2 = b -> 2
      c3 = c -> 3
      d4 = d -> 4
      """

  Scenario: iteration index for map
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        itemRenderer = ${item + iterator.index}
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a0
      b = b1
      c = c2
      d = d3
      """

  Scenario: full iteration information and custom iteration name
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Map {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        iterationName = ${"i" + "t"}

        itemRenderer = Neos.Fusion:Join {
          0 = ${item}
          1 = ${it.index}
          2 = ${it.cycle}
          3 = ${it.size}
          4 = ${it.isFirst}
          5 = ${it.isLast}
          6 = ${it.isEven}
          7 = ${it.isOdd}
          @glue = " "
        }
      }
      foo.@process.print = Bdd.PrintDataStructure
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a = a 0 1 4 true false false true
      b = b 1 2 4 false false true false
      c = c 2 3 4 false false false true
      d = d 3 4 4 false true true false
      """

