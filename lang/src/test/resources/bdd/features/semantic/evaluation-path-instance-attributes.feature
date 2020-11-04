Feature: prototype attributes based on the previous evaluation path

  ... are called "evaluation path instance attributes"

  Prototypes can override their nested sub paths for evaluation of nested Fusion objects. Like with prototypes
  in general, you declare instance attributes in nested Fusion objects as well.

  IMPORTANT - Even though Fusion supports defining nested child paths in (nested) Fusion objects,
  this is generally considered "bad practice"! The reason for that is: usually a component
  should not be aware of the implementation details of its sub-components, thus not overriding them.

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: nested Fusion object instances
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(FooBar) {
        value = true
      }
      prototype(Outer) {
        outerValue = FooBar {
          nested1 = true
        }
        outerValue.nested2 = true
        outerValue.untyped.foo = 'foo'
      }

      myPath = Outer {
        outerValue.instanceAttribute = true
      }
      myPath.outerValue.fromRootInstance = true
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "myPath<Outer>/outerValue<FooBar>"
    Then the loaded Fusion object instance for path "myPath<Outer>/outerValue<FooBar>" must be of prototype "FooBar"
    And the loaded Fusion object instance for path "myPath<Outer>/outerValue<FooBar>" must have the following attributes
      | relativePath      | absolutePath                        | value      | type      |
      | value             | prototype(FooBar).value             | true       | [BOOLEAN] |
      | nested1           | prototype(Outer).outerValue.nested1 | true       | [BOOLEAN] |
      | nested2           | prototype(Outer).outerValue.nested2 | true       | [BOOLEAN] |
      | untyped           | prototype(Outer).outerValue.untyped | [NO-VALUE] | [UNTYPED] |
      | instanceAttribute | myPath.outerValue.instanceAttribute | true       | [BOOLEAN] |
      | fromRootInstance  | myPath.outerValue.fromRootInstance  | true       | [BOOLEAN] |

