Feature: apply syntax for declaring multiple prototype attributes from expression array

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: single apply expression from prototype
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) {
        @apply.spreadMe = ${someContextArrayVar}
      }
      myPath = Foo
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "myPath<Foo>"
    Then the loaded Fusion object instance for path "myPath<Foo>" must be of prototype "Foo"
    And the loaded Fusion object instance for path "myPath<Foo>" must have the following apply attributes
      | applyName | absolutePath                   | expression          |
      | spreadMe  | prototype(Foo).@apply.spreadMe | someContextArrayVar |

  Scenario: single apply expression from instance
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) {
        // nothing
      }
      myPath = Foo {
        @apply.spreadMe = ${someContextArrayVar}
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "myPath<Foo>"
    Then the loaded Fusion object instance for path "myPath<Foo>" must be of prototype "Foo"
    And the loaded Fusion object instance for path "myPath<Foo>" must have the following apply attributes
      | applyName | absolutePath           | expression          |
      | spreadMe  | myPath.@apply.spreadMe | someContextArrayVar |

  Scenario: single apply expression from prototype extension
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) {
        // nothing
      }
      // prototype extension
      myPath.prototype(Foo) {
        @apply.spreadMe = ${someContextArrayVar}
      }
      myPath = Foo
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "myPath<Foo>"
    Then the loaded Fusion object instance for path "myPath<Foo>" must be of prototype "Foo"
    And the loaded Fusion object instance for path "myPath<Foo>" must have the following apply attributes
      | applyName | absolutePath                          | expression          |
      | spreadMe  | myPath.prototype(Foo).@apply.spreadMe | someContextArrayVar |

  Scenario: apply expression with prototype inheritance and instance overriding
    Given the Fusion file "Root.fusion" contains the following code
      """
      prototype(Foo) {
        @apply.foo = ${fromFoo}
        @apply.bar = ${fromFoo}
        @apply.instance = ${fromFoo}
      }
      // prototype inheritance
      prototype(Bar) < prototype(Foo) {
        @apply.bar = ${fromBar}
      }
      myPath = Bar {
        @apply.instance = ${fromInstance}
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "myPath<Bar>"
    Then the loaded Fusion object instance for path "myPath<Bar>" must be of prototype "Bar"
    And the loaded Fusion object instance for path "myPath<Bar>" must have the following apply attributes
      | applyName | absolutePath              | expression   |
      | foo       | prototype(Foo).@apply.foo | fromFoo      |
      | bar       | prototype(Bar).@apply.bar | fromBar      |
      | instance  | myPath.@apply.instance    | fromInstance |
