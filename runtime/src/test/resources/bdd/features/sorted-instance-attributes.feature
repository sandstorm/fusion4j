Feature: accessing sorted Fusion object instance attributes

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "ClassMapping.fusion" contains the following code
      """fusion
      prototype(Bdd.KeyPositions).@class = 'io.neos.fusion4j.test.bdd.impl.TestKeyPositionsImplementation'

      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = 'default value'
      }
      """

  Scenario: numeric key name index based sorting
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        100 = 'd'
        1 = 'a'
        2 = 'b'
        10 = 'c'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 1
       #1 2
       #2 10
       #3 100
      """

  Scenario: numeric key name index and middle positions
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        100 = 'b'
        10 = 'a'
        c = 'c'
        c.@position = 22
        d = 'd'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 10
       #1 c
       #2 100
       #3 d
      """

  Scenario: numeric keys go before the unspecified ones
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        100 = 'b'
        10 = 'a'
        foo = 'c'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 10
       #1 100
       #2 foo
      """

  Scenario: start position with weight
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        a = 'a'
        a.@position = 'start'
        b = 'b'
        b.@position = 'start 99999'
        c = 'c'
        22 = 'd'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 b
       #1 a
       #2 22
       #3 c
      """

  Scenario: end position with weight
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        a = 'a'
        a.@position = 'end'
        b = 'b'
        b.@position = 'end 99999'
        c = 'c'
        22 = 'd'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 22
       #1 c
       #2 a
       #3 b
      """

  Scenario: simple before position
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        a = 'a'
        b = 'b'
        b.@position = 'before a'
        c = 'c'
        c.@position = 'before b'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 c
       #1 b
       #2 a
      """

  Scenario: cycle detection in before position
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      testPath = Bdd.KeyPositions {
        a = 'a'
        b = 'b'
        c = 'c'
        b.@position = 'before a'
        c.@position = 'before b'
        a.@position = 'before c'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then there should be an error for evaluation of path "testPath" containing the following message
      """
      Cycle detected in key position before reference: .a, cycle: [(.a, .c), (.c, .b), (.b, .a)]
      """
