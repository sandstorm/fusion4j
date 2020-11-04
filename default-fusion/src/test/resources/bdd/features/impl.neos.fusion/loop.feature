Feature: Neos.Fusion:Loop implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: simple loop with data structure and most default API
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        itemRenderer = ${item + '-mapped'}
        @glue = ' | '
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

  Scenario: simple loop with data structure AFX
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(MyLoop) < prototype(Neos.Fusion:Component) {
        items = Neos.Fusion:DataStructure {
            a = '1'
            b = '2'
            c = '3'
        }
        renderer = afx`
          <Neos.Fusion:Loop items={props.items} @glue=" | ">{itemKey}-mapped</Neos.Fusion:Loop>
        `
      }
      foo = MyLoop
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a-mapped | b-mapped | c-mapped
      """

  Scenario: loop content key as fallback item renderer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        content = ${item + '-mapped'}
        @glue = ' | '
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

  Scenario: loop itemRenderer wins over fallback content key
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        content = ${item + '-foobar'}
        itemRenderer = ${item + '-mapped'}
        @glue = ' | '
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

  Scenario: loop with data structure and itemName
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        itemName = ${'element'}

        itemRenderer = ${element + '-mapped'}
        @glue = ' | '
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

  Scenario: loop keys with data structure - default context key name itemKey
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemRenderer = ${itemKey + " -> " + item}
        @glue = ' | '
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

  Scenario: loop keys with data structure - explicit context key name
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemKey = 'someKeyName'

        itemRenderer = ${someKeyName + " -> " + item}
        @glue = ' | '
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

  Scenario: loop key renderer with data structure does not effect output
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = '1'
          b = '2'
          c = '3'
        }
        items.d = '4'

        itemRenderer = ${itemKey + " -> " + item}
        keyRenderer = ${itemKey + iterator.cycle}
        @glue = ' | '
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

  Scenario: iteration index for loop
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'

        itemRenderer = ${item + iterator.index}
        @glue = ' | '
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

  Scenario: full iteration information and custom iteration name for loop
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a = 'a'
          b = 'b'
          c = 'c'
        }
        items.d = 'd'
        iterationName = 'it'

        itemRenderer = Neos.Fusion:Join {
          0 = ${item}
          1 = ${it.index}
          2 = ${it.cycle}
          3 = ${it.size}
          4 = ${it.isFirst}
          5 = ${it.isLast}
          6 = ${it.isEven}
          7 = ${it.isOdd}
          @glue = ' '
        }
        @glue = '\n'
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

  Scenario: loop over nested data structure with inner case handling
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a {
            type = '1'
            value = 'a'
          }
          b {
            type = '1'
            value = 'b'
          }
          c {
            type = '2'
            value = 'c'
          }
          d {
            type = '2'
            value = 'd'
          }
          e {
            type = '3'
            value = 'e'
          }
        }

        itemRenderer = Neos.Fusion:Case {
          type1 {
            condition = ${item.type == '1'}
            renderer = ${item.value + '--type-1'}
          }
          type2 {
            condition = ${item.type == '2'}
            renderer = ${item.value + '--type-2'}
          }
          default {
            @position = 'end'
            condition = true
            renderer = ${item.value + '--type-default'}
          }
        }
        @glue = ' | '
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a--type-1 | b--type-1 | c--type-2 | d--type-2 | e--type-default
      """

  Scenario: loop over nested data structure with inner case handling in own component
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(ItemRenderer) < prototype(Neos.Fusion:Component) {
        loopItem = ${item}

        renderer = Neos.Fusion:Case {
          type1 {
            condition = ${props.loopItem.type == '1'}
            renderer = ${props.loopItem.value + '--type-1'}
          }
          type2 {
            condition = ${props.loopItem.type == '2'}
            renderer = ${props.loopItem.value + '--type-2'}
          }
          default {
            @position = 'end'
            condition = true
            renderer = ${props.loopItem.value + '--type-default'}
          }
        }
      }

      foo = Neos.Fusion:Loop {
        items = Neos.Fusion:DataStructure {
          a {
            type = '1'
            value = 'a'
          }
          b {
            type = '1'
            value = 'b'
          }
          c {
            type = '2'
            value = 'c'
          }
          d {
            type = '2'
            value = 'd'
          }
          e {
            type = '3'
            value = 'e'
          }
        }

        itemRenderer = ItemRenderer
        @glue = ' | '
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      a--type-1 | b--type-1 | c--type-2 | d--type-2 | e--type-default
      """

  Scenario: recursive loop over nested data structure with inner case handling in own component
    Given the Fusion file "Root.fusion" contains the following code
      """fusion

      prototype(RecursiveComponent) < prototype(Neos.Fusion:Component) {
        items = Neos.Fusion:DataStructure
        renderer = Neos.Fusion:Loop {
          items = ${props.items}
          itemRenderer = ItemRenderer {
            loopItem = ${item}
          }
          @glue = ' / '
        }
        renderer.@process.wrapGroup = ${'group(' + value + ')'}
      }

      prototype(ItemRenderer) < prototype(Neos.Fusion:Component) {
        loopItem = null

        renderer = Neos.Fusion:Case {
          item {
            condition = ${props.loopItem.type == 'item'}
            renderer = ${'item-' + props.loopItem.value}
          }
          group {
            condition = ${props.loopItem.type == 'group'}
            renderer = RecursiveComponent {
              items = ${props.loopItem.subItems}
            }
          }
          default {
            @position = 'end'
            condition = true
            renderer = ${'unknown'}
          }
        }
      }

      foo = Neos.Fusion:Loop {
        itemRenderer = ItemRenderer {
          loopItem = ${item}
        }
        @glue = ' | '

        items = Neos.Fusion:DataStructure {
          a {
            type = 'item'
            value = 'a'
          }
          b {
            type = 'group'
            subItems {
              b1 {
                type = 'item'
                value = 'b1'
              }
              b2 {
                type = 'item'
                value = 'b2'
              }
            }
          }
          c {
            type = 'item'
            value = 'c'
          }
          d {
            type = 'group'
            subItems {
              d1 {
                type = 'item'
                value = 'd1'
              }
              d2 {
                type = 'group'
                subItems {
                  d21 {
                    type = 'item'
                    value = 'd21'
                  }
                  d22 {
                    type = 'item'
                    value = 'd22'
                  }
                }
              }
            }
          }
          e {
            type = 'item'
            value = 'e'
          }
        }

      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    And the evaluated output for path "foo" must be
      """
      item-a | group(item-b1 / item-b2) | item-c | group(item-d1 / group(item-d21 / item-d22)) | item-e
      """

