Feature: evaluation of EEL Expressions

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Value.fusion" contains the following code
      """fusion
      prototype(Bdd.TestValue) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestValueImplementation'
        value = null
      }
      prototype(Bdd.DataStructure) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestDataStructureImplementation'
      }
      """

  Scenario: simple EEL
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath = ${1 + 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      2
      """

  Scenario: simple EEL with context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath = ${1 + foo}
      testPath.@context.foo = 2
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      3
      """

  Scenario: simple EEL string with context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath = ${1 + foo}
      testPath.@context.foo = "2"
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      12
      """

  Scenario: inline evaluation this pointer root path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      value = ${this.otherValue + 1}
      value.otherValue = 1
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "value"
    Then the evaluated output for path "value" must be of type "java.lang.Integer"
    And the evaluated output for path "value" must be
      """
      2
      """

  Scenario: inline evaluation this pointer nested path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath.value = ${this.otherValue + 1}
      somePath.value.otherValue = 1
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.value"
    Then the evaluated output for path "somePath.value" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath.value" must be
      """
      2
      """

  Scenario: evaluation ternary operator with data structure from context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      somePath = Bdd.TestValue {
        @context.data = Bdd.DataStructure {
          conditionTrue = ${true}
          conditionFalse = ${false}
        }
        value = ${(data.conditionTrue ? 'yes' : 'no') + ' | ' + (data.conditionFalse ? 'yes' : 'no')}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.String"
    And the evaluated output for path "somePath" must be
      """
      yes | no
      """

  Scenario: evaluation ternary operator with props with condition from context expression
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      somePath = Bdd.TestValue {
        @context.conditionTrue = ${true}
        @context.conditionFalse = ${false}
        value = ${(conditionTrue ? 'yes' : 'no') + ' | ' + (conditionFalse ? 'yes' : 'no')}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.String"
    And the evaluated output for path "somePath" must be
      """
      yes | no
      """

  Scenario: evaluation ternary operator with props with condition from context primitive
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      somePath = Bdd.TestValue {
        @context.conditionTrue = true
        @context.conditionFalse = false
        value = ${(conditionTrue ? 'yes' : 'no') + ' | ' + (conditionFalse ? 'yes' : 'no')}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.String"
    And the evaluated output for path "somePath" must be
      """
      yes | no
      """

  Scenario: inline evaluation nested context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(AccessViaThisPointer) < prototype(Bdd.TestValue) {
        @context.accessViaContext = 20
        value = ${accessViaContext + 1}
      }

      prototype(Test) < prototype(Bdd.TestValue) {
        otherPath = AccessViaThisPointer
      }

      somePath = Test {
        value = ${this.otherPath + 1}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      22
      """

  Scenario: inline evaluation inherited context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(AccessViaThisPointer) < prototype(Bdd.TestValue) {
        @context.accessViaContext = 20
      }

      prototype(Test) < prototype(AccessViaThisPointer) {
        otherPath = ${accessViaContext + 1}
      }

      somePath = Test {
        value = ${this.otherPath + 1}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      22
      """

  Scenario: inline evaluation this pointer prototype
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(InheritanceLayer) < prototype(Bdd.TestValue) {
        fromInheritanceLayer = 1
        // gets overridden
        fromFoo = 10000
      }

      prototype(Foo) < prototype(InheritanceLayer) {
        @context.foo = 1
        fromFoo = ${foo + 1}
      }

      somePath = Foo {
        fromInstance = Bdd.TestValue {
          value = ${1 + 1}
        }
        value = ${this.fromInheritanceLayer + this.fromFoo + this.fromInstance}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath"
    Then the evaluated output for path "somePath" must be of type "java.lang.Integer"
    And the evaluated output for path "somePath" must be
      """
      5
      """

  # TODO configure strict mode and add two new tests for strict on/off
  #Scenario: inline evaluation this pointer non existing nested path non strict mode resolves to zero
  #  Given the Fusion file "Root.fusion" contains the following code
  #    """
  #    somePath.value = ${this.nonExisting + 1}
  #    """
  #  Given all Fusion packages are parsed
  #  And a Fusion runtime
  #  When I evaluate the Fusion path "somePath.value"
  #  Then the evaluated output for path "somePath.value" must be of type "java.lang.Integer"
  #  And the evaluated output for path "somePath.value" must be
  #    """
  #    1
  #    """

  Scenario: this pointer non existing nested path strict mode throws error null operand
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      somePath.value = ${this.nonExisting + 1}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.value"
    Then there should be an error for evaluation of path "somePath.value" containing the following message
      """
      Could not evaluate EEL expression
          EEL operation '+' with null operand 'this.nonExisting' is not allowed in strict mode
          source: in-memory://MyTestPackage/Root.fusion
          hints: [thread=[*]]
          expression: ${this.nonExisting + 1}
          offending: 'this.nonExisting' at line 1 char 38
          problem: + error -> EEL operation '+' with null operand 'this.nonExisting' is not allowed in strict mode
          element: in-memory://MyTestPackage/Root.fusion/somePath.value<PathAssignment>[0]/<PathAssignmentValue|Expression>
          code: expression value, from line 1 char 18 to line 1 char 41
      """

  Scenario Outline: strict mode throws error when comparing incompatible data types
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = ${<left> > <right>}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "myPath"
    Then there should be an error for evaluation of path "myPath" containing the following message
      """
      Could not evaluate EEL expression
          arithmetic error: Cannot compare objects of different types in strict EEL mode; left: <lType>, right: <rType>
          source: in-memory://MyTestPackage/Root.fusion
          hints: [thread=[*]]
          expression: ${<left> > <right>}
          offending: '<right>' at line 1 char <offendingChar>
          problem: > error -> arithmetic error: Cannot compare objects of different types in strict EEL mode; left: <lType>, right: <rType>
          element: in-memory://MyTestPackage/Root.fusion/myPath<PathAssignment>[0]/<PathAssignmentValue|Expression>
          code: expression value, from line 1 char 10 to line 1 char <endChar>
      """
    Examples:
      | left  | right | lType             | rType             | offendingChar | endChar |
      | "abc" | 123   | java.lang.String  | java.lang.Integer | 19            | 24      |
      | 123   | "abc" | java.lang.Integer | java.lang.String  | 17            | 24      |
      | 123   | true  | java.lang.Integer | java.lang.Boolean | 17            | 23      |
      | "abc" | true  | java.lang.String  | java.lang.Boolean | 19            | 25      |

  Scenario Outline: comparing unequal but compatible data types
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = ${<left> > <right>}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "myPath"
    Then the evaluated output for path "myPath" must be of type "java.lang.Boolean"
    Then the evaluated output for path "myPath" must be
      """
      <result>
      """
    Examples:
      | left            | right                                                                           | result |
      # float and int
      | 234.567         | 123                                                                             | true   |
      # int and float
      | 123             | 234.567                                                                         | false  |
      # long and int
      | 50000000000     | 1                                                                               | true   |
      # double and int
      | 1.1234567891011 | 1                                                                               | true   |
      # int and big int
      | 0               | 111123432343453456456756767867978968465745674567456456456456456456456456456     | false  |
      # int and big decimal
      | 0               | 111123432343453456456756767867978968465745674567456456456456456456456456456.123 | false  |

  Scenario: invalid EEL syntax gives parse error at runtime
    Given the Fusion file "Root.fusion" contains the following code
      """
      somePath.value = ${thiß.ötherValue}
      somePath.otherValue = 1
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "somePath.value"
    Then there should be an error for evaluation of path "somePath.value" containing the following message
      """
      Could not evaluate EEL expression
          source: in-memory://MyTestPackage/Root.fusion
          hints: [thread=[*]]
          expression: ${thiß.ötherValue}
          offending: '(thi)ß.ötherValue ...' at line 2 char 22
          problem: Could not parse expression; cause:
          element: in-memory://MyTestPackage/Root.fusion/somePath.value<PathAssignment>[0]/<PathAssignmentValue|Expression>
          code: expression value, from line 1 char 18 to line 1 char 36
      """
