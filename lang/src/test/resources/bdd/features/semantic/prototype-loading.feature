Feature: loading of a fully described Fusion prototype

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: simple prototype with some attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Vendor:Foo.Bar) {
        hello = ${world}
        foo = 'bar'
        // deeply nested paths are not considered to be prototype attributes,
        // but the first level "some" is an untyped attribute
        some.nested = 'nested value'
        // config blocks are "untyped"
        config {
          // deeply nested paths are not considered to be prototype attributes
          nested = true
          bar = 123
        }
        config.bar = 234
      }
      prototype(Vendor:Foo.Bar).foo = 'some value'
      prototype(Vendor:Foo.Bar).someNumber = 123
      prototype(Vendor:Foo.Bar).bar = null
      prototype(Vendor:Foo.Bar).baz = Foo.Baz {
        a = 1
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have the following attributes
      | relativePath | absolutePath                         | value      | type            |
      | hello        | prototype(Vendor:Foo.Bar).hello      | ${world}   | [EEL]           |
      | foo          | prototype(Vendor:Foo.Bar).foo        | some value | [STRING]        |
      | bar          | prototype(Vendor:Foo.Bar).bar        | NULL       | [NULL]          |
      | baz          | prototype(Vendor:Foo.Bar).baz        | <Foo.Baz>  | [FUSION_OBJECT] |
      | someNumber   | prototype(Vendor:Foo.Bar).someNumber | 123        | [INTEGER]       |
      | config       | prototype(Vendor:Foo.Bar).config     | [NO-VALUE] | [UNTYPED]       |
      | some         | prototype(Vendor:Foo.Bar).some       | [NO-VALUE] | [UNTYPED]       |

  Scenario: untyped config blocks in prototypes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Vendor:Foo.Bar) {
        foo = 'bar'
        bar = 'some value'
      }
      prototype(Vendor:Foo.Bar).foo >
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have the following attributes
      | relativePath | absolutePath                  | value      | type     |
      | bar          | prototype(Vendor:Foo.Bar).bar | some value | [STRING] |

  Scenario: prototype with erased attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Vendor:Foo.Bar) {
        foo = 'bar'
        bar = 'some value'
      }
      prototype(Vendor:Foo.Bar).foo >
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have the following attributes
      | relativePath | absolutePath                  | value      | type     |
      | bar          | prototype(Vendor:Foo.Bar).bar | some value | [STRING] |

  Scenario: erased prototype must have no attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Vendor:Foo.Bar) {
        foo = 'bar'
        bar = 'some value'
      }
      prototype(Vendor:Foo.Bar) >
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have no attributes

  Scenario: attributes are merged from inherited prototypes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(SomeValues) {
        someValues = true

        nested {
          ignoredSinceIAmNested = true
        }
      }

      prototype(RecursionLayer) < prototype(SomeValues) {
        rootLevel = true
        moreValues = 'values'
      }

      prototype(Vendor:Foo.Bar) {
        foo = 'bar'
        someOther = 'other value'
      }
      prototype(Vendor:Foo.Bar) < prototype(RecursionLayer) {
        andEvenMoreInBody = 1234
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have the following attributes
      | relativePath      | absolutePath                                | value       | type      |
      | foo               | prototype(Vendor:Foo.Bar).foo               | bar         | [STRING]  |
      | someOther         | prototype(Vendor:Foo.Bar).someOther         | other value | [STRING]  |
      | andEvenMoreInBody | prototype(Vendor:Foo.Bar).andEvenMoreInBody | 1234        | [INTEGER] |
      | rootLevel         | prototype(RecursionLayer).rootLevel         | true        | [BOOLEAN] |
      | moreValues        | prototype(RecursionLayer).moreValues        | values      | [STRING]  |
      | someValues        | prototype(SomeValues).someValues            | true        | [BOOLEAN] |
      | nested            | prototype(SomeValues).nested                | [NO-VALUE]  | [UNTYPED] |

  Scenario: attributes from more concrete inherited prototypes wins over more abstract ones
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(ABitMoreConcrete) < prototype(VeryAbstract) {
        value = "a bit more concrete"
      }

      // declared after ABitMoreConcrete to show, that the code order
      // is less relevant than the inheritance depth order
      prototype(VeryAbstract) {
        veryAbstractValue = 42
        value = null
      }
      prototype(VeryAbstract) {
        value = "very abstract"
      }

      prototype(Vendor:Foo.Bar) < prototype(ABitMoreConcrete)
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Vendor:Foo.Bar" must have the following attributes
      | relativePath      | absolutePath                              | value               | type      |
      | veryAbstractValue | prototype(VeryAbstract).veryAbstractValue | 42                  | [INTEGER] |
      | value             | prototype(ABitMoreConcrete).value         | a bit more concrete | [STRING]  |

  Scenario: attributes are merged from nested copies
    Given the Fusion file "Root.fusion" contains the following code
      # FIXME see below
      #"""fusion
      """
      prototype(FooBar) {
        @class = 'foo.Bar'
      }

      prototype(FooBar).@class = 'foo.BeforeErasure'
      prototype(FooBar) >
      prototype(FooBar).@class = 'foo.AfterErasure'

      prototype(Some:Fancy.Prototype) {
        something = 'here'
      }

      // FIXME imho the line below is valid Fusion, this is shown as parse error in Neos IntelliJ plugin
      prototype(Some:Fancy.Prototype).@class < prototype(FooBar).@class
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Some:Fancy.Prototype" must have the following attributes
      | relativePath | absolutePath                              | value            | type     |
      | something    | prototype(Some:Fancy.Prototype).something | here             | [STRING] |
      | @class       | prototype(FooBar).@class                  | foo.AfterErasure | [STRING] |

  Scenario: erased instance attributes resolve null
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath = Some:Fancy.Prototype {
        value >
      }

      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue) {
        foo = 'bar'
      }

      prototype(Bdd.TestValue) {
        value = 'default value'
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    When I load the Fusion object instance for evaluation path "testPath<Some:Fancy.Prototype>"
    Then the loaded Fusion object instance for path "testPath<Some:Fancy.Prototype>" must be of prototype "Some:Fancy.Prototype"
    And the loaded Fusion object instance for path "testPath<Some:Fancy.Prototype>" must have the following attributes
      | relativePath | absolutePath                        | value | type     |
      | foo          | prototype(Some:Fancy.Prototype).foo | bar   | [STRING] |

  Scenario: erased prototype values via inheritance
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Some:Fancy.Prototype) < prototype(Bdd.TestValue) {
        value >
        foo = 'bar'
      }

      prototype(Bdd.TestValue) {
        value = 'default value'
      }
      """
    And all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the loaded prototype "Some:Fancy.Prototype" must have the following attributes
      | relativePath | absolutePath                        | value | type     |
      | foo          | prototype(Some:Fancy.Prototype).foo | bar   | [STRING] |
