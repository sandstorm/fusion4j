Feature: prototype extension loading

  The *load path* of a Fusion object instance is the Fusion path pointing to the parent of the FusionObject value that declared
  an instance of this prototype. Since you can override prototypes inside of nested paths (via path configurations
  or nested assignments) the load path is used to determine effective attribute declarations.

  Declarations from deeper load paths are considered more specific and win over declarations from their parent paths.

  Also the runtime prototype evaluation stack is considered for prototype attribute extensions.

  Some examples:

  ```
  prototype(FooBar) {
  a = 1
  }

  myInstance = FooBar
  someNestedPath {
  otherInstance = FooBar
  }

  // nested prototype extension
  someNestedPath.prototype(FooBar).a = 2
  ```
  load path of FooBar at path `myInstance` is root (/)
  `myInstance.a` would resolve 1

  load path of FooBar at path `someNestedPath.myInstance` is `someNestedPath`
  `someNestedPath.myInstance.a` would resolve 2

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: path specific prototype extension
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(FooBar).value = 42
      prototype(FooBar) {
        overriddenAttribute = 'root prototype'
        itsLate = true
      }
      // prototype extension for nested path
      somePath.prototype(FooBar) {
        overriddenAttribute = 'extension prototype'
        other = 123
      }
      somePath {
        prototype(FooBar).something = true
      }
      somePath.prototype(FooBar).foo = 'bar'

      dontCare {
        prototype(FooBar) {
          dontCare = true
        }
      }

      somePath.myInstance = FooBar {
        fromInstance = 'something'
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "somePath.myInstance<FooBar>"
    Then the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must be of prototype "FooBar"
    And the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must have the following attributes
      | relativePath        | absolutePath                                   | value               | type      |
      | value               | prototype(FooBar).value                        | 42                  | [INTEGER] |
      | itsLate             | prototype(FooBar).itsLate                      | true                | [BOOLEAN] |
      | overriddenAttribute | somePath.prototype(FooBar).overriddenAttribute | extension prototype | [STRING]  |
      | other               | somePath.prototype(FooBar).other               | 123                 | [INTEGER] |
      | something           | somePath.prototype(FooBar).something           | true                | [BOOLEAN] |
      | foo                 | somePath.prototype(FooBar).foo                 | bar                 | [STRING]  |
      | fromInstance        | somePath.myInstance.fromInstance               | something           | [STRING]  |

  Scenario: nested prototype extension attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(FooBar) {
        foo = 'bar'
      }
      // prototype extension for nested path
      somePath.prototype(FooBar) {
        some.nested = 'nested'
      }

      somePath.myInstance = FooBar
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "somePath.myInstance<FooBar>"
    Then the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must be of prototype "FooBar"
    And the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must have the following attributes
      | relativePath | absolutePath                    | value      | type      |
      | foo          | prototype(FooBar).foo           | bar        | [STRING]  |
      | some         | somePath.prototype(FooBar).some | [NO-VALUE] | [UNTYPED] |

  Scenario: nested prototypes and extension attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Baz) {
        // nothing
      }
      prototype(FooBar) {
        foo = 'bar'
        some = Baz
      }
      // prototype extension for nested path
      somePath.prototype(FooBar) {
        some.nested = 'nested'
        more.nested = 'nested'
      }

      somePath.myInstance = FooBar
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "somePath.myInstance<FooBar>"
    Then the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must be of prototype "FooBar"
    And the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must have the following attributes
      | relativePath | absolutePath                    | value      | type            |
      | foo          | prototype(FooBar).foo           | bar        | [STRING]        |
      | some         | prototype(FooBar).some          | <Baz>      | [FUSION_OBJECT] |
      | more         | somePath.prototype(FooBar).more | [NO-VALUE] | [UNTYPED]       |

  Scenario: prototype extension load path with inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(FooBar) < prototype(Base) {
        fromFooBar = true
      }

      prototype(Base) {
        overridden = 'from base'
        base = true
      }

      somePath.prototype(Base) {
        overridden = true
        baseExt = false
      }

      somePath.myInstance = FooBar
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "somePath.myInstance<FooBar>"
    Then the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must be of prototype "FooBar"
    And the loaded Fusion object instance for path "somePath.myInstance<FooBar>" must have the following attributes
      | relativePath | absolutePath                        | value | type      |
      | fromFooBar   | prototype(FooBar).fromFooBar        | true  | [BOOLEAN] |
      | base         | prototype(Base).base                | true  | [BOOLEAN] |
      | overridden   | somePath.prototype(Base).overridden | true  | [BOOLEAN] |
      | baseExt      | somePath.prototype(Base).baseExt    | false | [BOOLEAN] |

  # different variations are unit-tested
  Scenario: multiple matching prototype extensions
    Given the Fusion file "Root.fusion" contains the following code
      """
      prototype(Foo) {
        foo = true
      }

      somePath.moreConcrete.prototype(Foo) {
        // wins over more general extension below
        ext2 = true
      }

      somePath.prototype(Foo) {
        ext1 = true
        // loses over more concrete extension above
        ext2 = false
      }

      somePath.moreConcrete.myInstance = FooBar
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "somePath.moreConcrete.myInstance<Foo>"
    Then the loaded Fusion object instance for path "somePath.moreConcrete.myInstance<Foo>" must be of prototype "Foo"
    And the loaded Fusion object instance for path "somePath.moreConcrete.myInstance<Foo>" must have the following attributes
      | relativePath | absolutePath                              | value | type      |
      | foo          | prototype(Foo).foo                        | true  | [BOOLEAN] |
      | ext1         | somePath.prototype(Foo).ext1              | true  | [BOOLEAN] |
      | ext2         | somePath.moreConcrete.prototype(Foo).ext2 | true  | [BOOLEAN] |
