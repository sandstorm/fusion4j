Feature: the Fusion prototype store

  Holds all root prototype declarations.

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: multiple prototype declarations of the same prototype in the same file
    Given the Fusion file "Root.fusion" contains the following code
      """
      prototype(Vendor:SomePrototype) {
        value1 = "hallo"
        value2.nested = true
        config.ofPath {
          foo = 123
          bar = "baz"
        }
      }

      prototype(Vendor:SomePrototype) {
        valueA = "hallo"
        value2.nested2 = true
        config.ofPath {
          hallo = "welt"
          // this path should override the declaration above (higher code line index)
          bar = false
        }
      }

      include: *
      """
    Given the Fusion file "FooBar.fusion" contains the following code
      """
      prototype(Vendor:Some.Other) {
        // empty
      }

      prototype(Vendor:SomePrototype) < prototype(Vendor:Some.Other)
      // not in prototype index
      prototype(Vendor:SomePrototype).foobar = 'abc'
      inner {
        prototype(Vendor:SomePrototype) {
          // something bad here
        }
      }
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the prototype store for "Vendor:Some.Other" must contain 1 declarations
    Then the prototype store for "Vendor:SomePrototype" must contain 3 declarations
    Then the prototype store must contain 2 prototypes
