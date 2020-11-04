Feature: Neos.Fusion:Reduce implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: simple reduce with data structure and most default API
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        initialValue = ""
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + item + "-mapped"}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a-mapped | b-mapped | c-mapped | d-mapped
      """

  Scenario: reduce with data structure and itemName
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        itemName = ${'element'}

        initialValue = ""
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + element + "-mapped"}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a-mapped | b-mapped | c-mapped | d-mapped
      """

  Scenario: reduce with data structure - default context key name itemKey
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        initialValue = ""
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + itemKey + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a -> 1 | b -> 2 | c -> 3 | d -> 4
      """

  Scenario: reduce with data structure - explicit context key name
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemKey = 'someKeyName'

        initialValue = ""
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + someKeyName + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a -> 1 | b -> 2 | c -> 3 | d -> 4
      """

  Scenario: reduce with data structure - explicit carry name
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        carryName = 'customCarryName'

        initialValue = ""
        itemReducer = ${customCarryName + (iterator.isFirst ? "" : " | ") + itemKey + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a -> 1 | b -> 2 | c -> 3 | d -> 4
      """

  Scenario: reduce keys with data structure - null items fallback to default initial value
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + someKeyName + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    And the evaluated output for path "foo" must be null

  Scenario: reduce keys with data structure - empty items fallback to default initial value
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure
        itemKey = 'someKeyName'
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + someKeyName + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    And the evaluated output for path "foo" must be null

  Scenario: reduce keys with data structure - null items fallback to initial value
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        initialValue = "init"
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + itemKey + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    And the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      init
      """

  Scenario: reduce keys with data structure - empty items fallback to initial value
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure
        initialValue = "init"
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + itemKey + " -> " + item}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    And the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      init
      """

  Scenario: iteration index for reduce
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        initialValue = ""
        itemReducer = ${carry + (iterator.isFirst ? "" : " | ") + item + iterator.index}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a0 | b1 | c2 | d3
      """

  Scenario: full iteration information and custom iteration name for reduce
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Reduce {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        iterationName = 'it'

        initialValue = ""
        itemReducer = Neos.Fusion:Join {
          0 = ${carry + item}
          1 = ${it.index}
          2 = ${it.cycle}
          3 = ${it.size}
          4 = ${it.isFirst}
          5 = ${it.isLast}
          6 = ${it.isEven}
          7 = ${it.isOdd}
          @glue = ' '
        }
        itemReducer.@process.newLines = ${value + (!it.isLast ? "\n" : '')}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a 0 1 4 true false false true
      b 1 2 4 false false true false
      c 2 3 4 false false false true
      d 3 4 4 false true true false
      """

