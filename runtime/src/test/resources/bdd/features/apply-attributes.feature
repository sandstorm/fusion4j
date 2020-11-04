Feature: runtime evaluation with apply attributes

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
      prototype(Bdd.PrintDataStructure) {
        @class = 'io.neos.fusion4j.test.bdd.impl.TestPrintDataStructureImplementation'
      }

      prototype(Bdd.KeyPositions).@class = 'io.neos.fusion4j.test.bdd.impl.TestKeyPositionsImplementation'
      """

  Scenario: evaluation with apply values from context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.PrintDataStructure) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 2
        }
      }
      testPath = Foo {
        data = Bdd.DataStructure {
          @apply.varsFromContext = ${someVars}
          c = 3
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      a = 1
      b = 2
      c = 3
      """

  Scenario: evaluation with multiple apply values from context overlap
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.PrintDataStructure) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 1
        }
        @context.moreVars = Bdd.DataStructure {
          b = 2
          c = 3
        }
      }
      testPath = Foo {
        data = Bdd.DataStructure {
          @apply.x = ${someVars}
          @apply.y = ${moreVars}
          d = 4
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      a = 1
      b = 2
      c = 3
      d = 4
      """

  Scenario: default key positions with multiple apply values from context overlap
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.TestValue) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 1
        }
        @context.moreVars = Bdd.DataStructure {
          b = 2
          c = 3
        }
      }
      testPath = Foo {
        value = Bdd.KeyPositions {
          @apply.x = ${someVars}
          @apply.y = ${moreVars}
          d = 4
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
       #0 a
       #1 b
       #2 c
       #3 d
      """

  Scenario: if condition for applied attribute
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.PrintDataStructure) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 2
        }
      }
      testPath = Foo {
        data = Bdd.DataStructure {
          @apply.varsFromContext = ${someVars}
          a.@if.deactivate = false
          c = 3
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      b = 2
      c = 3
      """

  Scenario: post-processor for applied attribute
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.PrintDataStructure) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 2
        }
      }
      testPath = Foo {
        data = Bdd.DataStructure {
          @apply.varsFromContext = ${someVars}
          a.@process.timesHundred = ${value * 100}
          c = 3
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      a = 100
      b = 2
      c = 3
      """

  Scenario: position sorting of apply keys
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.PrintDataStructure) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 1
        }
        @context.moreVars = Bdd.DataStructure {
          b = 2
          c = 3
        }
      }
      testPath = Foo {
        data = Bdd.DataStructure {
          @apply.moreVarsFromContext = ${moreVars}
          @apply.moreVarsFromContext.@position = 'after varsFromContext'
          @apply.varsFromContext = ${someVars}
          d = 4
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.String"
    And the evaluated output for path "testPath" must be
      """
      a = 1
      b = 2
      c = 3
      d = 4
      """

  Scenario: accessing applied attributes via this-pointer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.TestValue) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 2
        }
      }
      testPath = Foo {
        value = Bdd.TestValue {
          @apply.varsFromContext = ${someVars}
          value = ${this.a + this.b}
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      3
      """

  Scenario: accessing applied attributes via this-pointer in context initialization
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) < prototype(Bdd.TestValue) {
        @context.someVars = Bdd.DataStructure {
          a = 1
          b = 2
        }
      }
      testPath = Foo {
        value = Bdd.TestValue {
          @apply.varsFromContext = ${someVars}
          @context.foo = ${this.a + this.b}
          value = ${foo}
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Integer"
    And the evaluated output for path "testPath" must be
      """
      3
      """
