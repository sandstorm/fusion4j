Feature: sorting of keys with 'at'-position and number index

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: sorting with number based keys
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath {
        e = 'e'
        e.@position = 1
        d = 'd'
        d.@position = '2'
        c = 'c'
        c.@position = 3
        b = 'b'
        b.@position = "4"
        a = 'a'
        a.@position = 5
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "somePath" must have the following attribute key positions
      | key | type   | subject | numericIndex |
      | a   | Middle | 5       | 5            |
      | b   | Middle | 4       | 4            |
      | c   | Middle | 3       | 3            |
      | d   | Middle | 2       | 2            |
      | e   | Middle | 1       | 1            |

  Scenario: sorting with before
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath {
        a = 'a'
        b = 'b'
        b.@position = 'before a'
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "somePath" must have the following attribute key positions
      | key | type   | subject  | refKey |
      | b   | Before | before a | a      |

  Scenario: sorting with after
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath {
        a = 'a'
        a.@position = 'after b'
        b = 'b'
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "somePath" must have the following attribute key positions
      | key | type  | subject | refKey |
      | a   | After | after b | b      |

  Scenario: sorting with start
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath {
        a = 'a'
        b = 'b'
        b.@position = 'start'
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "somePath" must have the following attribute key positions
      | key | type  | subject |
      | b   | Start | start   |

  Scenario: sorting with end
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath {
        a = 'a'
        a.@position = 'end'
        b = 'b'
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the path "somePath" must have the following attribute key positions
      | key | type | subject |
      | a   | End  | end     |

  Scenario: prototype instance key positions
    Given the Fusion file "Root.fusion" contains the following code
      """
      testPath = Some:Fancy.Prototype {
        a = 1
        a.@position = 'end 99999'
        d.@position = 'after b'
      }

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue) {
        b = 2
        b.@position = 3
      }

      prototype(Bdd.TestValue) {
        c = 3
        c.@position = 'start'
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "testPath<Some:Fancy.Prototype>"
    Then the loaded Fusion object instance for path "testPath<Some:Fancy.Prototype>" must be of prototype "Some:Fancy.Prototype"
    And the loaded Fusion object instance for path "testPath<Some:Fancy.Prototype>" must have the following attribute key positions
      | key | type   | subject   | weight | refKey |
      | a   | End    | end 99999 | 99999  |        |
      | b   | Middle | 3         |        |        |
      | c   | Start  | start     | 0      |        |
      | d   | After  | after b   | 0      | b      |
