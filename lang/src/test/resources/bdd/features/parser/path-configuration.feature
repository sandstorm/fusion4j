Feature: raw 'lang' model parsing for fusion path configuration syntax

  A fusion configuration is basically everything inside a '{' ... '}' code block
  *except* for root prototype declaration bodies. Note: this must not be confused with
  the *occurrence*. In other words: a fusion configuration *can* occur inside of a root
  prototype declaration. The reason is, a root prototype declaration body fusion code
  block is handled specially in the syntax (as tested in root-prototype-declaration.feature).

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: path configuration on top level code layer
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // a simple fusion path configuration
    myPath {
        // inner assignments
        foo = 'bar'
    }

    // nested fusion path configuration
    some.nested.path {
        // inner assignments
        foo = 'bar'
    }

    // nested fusion path configuration for hierarchic prototypes
    some.prototype(FooBar) {
        // inner assignments
        foo = 'bar'
    }

    // nested fusion path configuration for hierarchic prototype inner configurations
    some.prototype(HelloWorld).nestedConfig {
        // inner assignments
        foo = 'bar'
    }

    // nested fusion path configuration for complex hierarchic prototypes
    some.prototype(HelloWorld).prototype(FooBar) {
        // inner assignments
        foo = 'bar'
    }

    // nested fusion path configuration for complex hierarchic prototypes inner configurations
    some.prototype(HelloWorld).prototype(FooBar).nestedConfig {
        // inner assignments
        foo = 'bar'
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 path configurations
    And the model at "Root.fusion/myPath[1]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/some.nested.path[3]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/some.prototype(FooBar)[5]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/some.prototype(HelloWorld).nestedConfig[7]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/some.prototype(HelloWorld).prototype(FooBar)[9]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/some.prototype(HelloWorld).prototype(FooBar).nestedConfig[11]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"

  Scenario: path configuration inside of a root prototype declaration
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // most outer configuration
    prototype(SomePrototypeName) {
        // a simple fusion path configuration
        myPath {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration
        some.nested.path {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototypes
        some.prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototype inner configurations
        some.prototype(HelloWorld).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes
        some.prototype(HelloWorld).prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes inner configurations
        some.prototype(HelloWorld).prototype(FooBar).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // end of most outer configuration block
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 root prototype declarations
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name              |
      | 1     | SomePrototypeName |
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]" must contain 6 path configurations
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/myPath[1]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/some.nested.path[3]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/some.prototype(FooBar)[5]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/some.prototype(HelloWorld).nestedConfig[7]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/some.prototype(HelloWorld).prototype(FooBar)[9]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/prototype(SomePrototypeName)[1]/some.prototype(HelloWorld).prototype(FooBar).nestedConfig[11]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"

  Scenario: path configuration inside of a path configuration
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // most outer configuration
    outer {
        // a simple fusion path configuration
        myPath {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration
        some.nested.path {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototypes
        some.prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototype inner configurations
        some.prototype(HelloWorld).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes
        some.prototype(HelloWorld).prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes inner configurations
        some.prototype(HelloWorld).prototype(FooBar).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // end of most outer configuration block
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path configurations
    And the model at "Root.fusion/outer[1]" must contain 6 path configurations
    And the model at "Root.fusion/outer[1]/myPath[1]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.nested.path[3]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(FooBar)[5]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).nestedConfig[7]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).prototype(FooBar)[9]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).prototype(FooBar).nestedConfig[11]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"

  Scenario: path configuration inside of a fusion object assignment body
    Given the Fusion file "Root.fusion" contains the following code
    """
    // most outer configuration
    outer = Some.FusionObject {
        // a simple fusion path configuration
        myPath {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration
        some.nested.path {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototypes
        some.prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for hierarchic prototype inner configurations
        some.prototype(HelloWorld).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes
        some.prototype(HelloWorld).prototype(FooBar) {
            // inner assignments
            foo = 'bar'
        }

        // nested fusion path configuration for complex hierarchic prototypes inner configurations
        some.prototype(HelloWorld).prototype(FooBar).nestedConfig {
            // inner assignments
            foo = 'bar'
        }

        // end of most outer configuration block
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the path assignment at "Root.fusion/outer[1]" must have a body
    And the model at "Root.fusion/outer[1]" must contain 6 path configurations
    And the model at "Root.fusion/outer[1]/myPath[1]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.nested.path[3]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(FooBar)[5]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).nestedConfig[7]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).prototype(FooBar)[9]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/outer[1]/some.prototype(HelloWorld).prototype(FooBar).nestedConfig[11]" must contain the path assignment "foo" at index 1 with value "bar" of type "StringValue"
