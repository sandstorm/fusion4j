Feature: file includes in raw Fusion index

  - load order
  - endless recursion detection

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: includes of entrypoint must throw an error
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: 'A.fusion'
      """
    And the Fusion file "A.fusion" contains the following code
      """fusion
      include: 'B.fusion'
      """
    And the Fusion file "B.fusion" contains the following code
      """fusion
      include: 'Root.fusion'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed
    Then there should be an index error "Recursion loop detected resolving file includes of in-memory://MyTestPackage/B.fusion; including the entrypoint file Root.fusion is not allowed"

  Scenario: recursive includes must throw an error
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: 'A.fusion'
      """
    And the Fusion file "A.fusion" contains the following code
      """fusion
      include: 'B.fusion'
      """
    And the Fusion file "B.fusion" contains the following code
      """fusion
      include: 'C.fusion'
      """
    And the Fusion file "C.fusion" contains the following code
      """fusion
      include: 'A.fusion'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed
    Then there should be an index error "Recursion loop detected resolving file includes of package 'MyTestPackage' and file C.fusion; loop: [C.fusion, B.fusion, A.fusion, Root.fusion], offending: FusionFileIncludeDecl"

  Scenario: multiple includes sources
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // include chain: []
      include: '*'
      include: 'A.fusion'
      include: 'B.fusion'
      """
    And the Fusion file "A.fusion" contains the following code
      """
      // include chain: [Root.fusion, B.fusion]
      value = 'A'
      include: 'C.fusion'
      """
    And the Fusion file "B.fusion" contains the following code
      """
      // include chain: [Root.fusion]
      value = 'B'
      include: 'A.fusion'
      """
    And the Fusion file "C.fusion" contains the following code
      """
      // include chain: [Root.fusion, B.fusion, A.fusion]
      value = 'C'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "value" should contain the following assignments
      | value | type     | source                             |
      | C     | [STRING] | in-memory://MyTestPackage/C.fusion |
      | A     | [STRING] | in-memory://MyTestPackage/A.fusion |
      | B     | [STRING] | in-memory://MyTestPackage/B.fusion |

  Scenario: multiple includes sources part 2
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: '*'
      include: 'A.fusion'
      include: 'B.fusion'
      """
    And the Fusion file "A.fusion" contains the following code
      """
      value = 'A'
      include: 'C.fusion'
      """
    And the Fusion file "B.fusion" contains the following code
      """
      include: 'A.fusion'
      value = 'B'
      """
    And the Fusion file "C.fusion" contains the following code
      """
      value = 'C'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "value" should contain the following assignments
      | value | type     | source                             |
      | B     | [STRING] | in-memory://MyTestPackage/B.fusion |
      | C     | [STRING] | in-memory://MyTestPackage/C.fusion |
      | A     | [STRING] | in-memory://MyTestPackage/A.fusion |

  Scenario: last of multiple file includes wins
    Given the Fusion file "Root.fusion" contains the following code
      """
      // all three includes will match A.fusion -> this should throw no errors
      include: '*'
      include: 'A.fusion'
      some = 'base value'
      // the last one is taken
      include: 'A.fusion'
      """
    And the Fusion file "A.fusion" contains the following code
      """
      some = 'value'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "some" should contain the following assignments
      | value      | type     | source                                |
      | value      | [STRING] | in-memory://MyTestPackage/A.fusion    |
      | base value | [STRING] | in-memory://MyTestPackage/Root.fusion |

  Scenario: equal file includes fallback to file name order
    Given the Fusion file "Root.fusion" contains the following code
      """
      some = 'base value'
      include: '*'
      """
    And the Fusion file "B.fusion" contains the following code
      """
      some = 'value B'
      """
    And the Fusion file "A.fusion" contains the following code
      """
      some = 'value A'
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the raw path index for "some" should contain the following assignments
      | value      | type     | source                                |
      | value B    | [STRING] | in-memory://MyTestPackage/B.fusion    |
      | value A    | [STRING] | in-memory://MyTestPackage/A.fusion    |
      | base value | [STRING] | in-memory://MyTestPackage/Root.fusion |
