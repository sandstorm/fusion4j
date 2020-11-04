Feature: raw 'lang' model parsing for fusion path erasure syntax

  A Fusion path erasure is the declaration of removing all previously declared
  values or configurations for a specific path or root prototype.

  The literal '>' marks a path erasure and can occur after:

  1. root prototype declarations
  2. root fusion paths
  3. inner fusion paths

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: most simple path erasure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo >
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path erasures
    And the model at "Root.fusion" must contain the path erasure "foo" at index 0

  Scenario: root prototype erasure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // something gets defined ...
      prototype(Foo.Bar:SomeFusionObject) {
          // the whole block will be erased later
          somePath = 'whose parent path is about to be erased later'
          willIBe.declaredLater = false
      }

      // ... and will be erased later
      prototype(Foo.Bar:SomeFusionObject) >
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 root prototype declarations
    And the model at "Root.fusion" must contain 1 path erasures
    And the model at "Root.fusion" must contain the path erasure "prototype(Foo.Bar:SomeFusionObject)" at index 3

  Scenario: root path erasure
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // simple root path
      simple >
      // nested
      some.nested >

      // meta prop
      @metaProperty >

      // hierarchical prototype erasure
      some.prototype(That.Is:Nested) >
      @metaProp.prototype(That.Is:Nested) >
      prototype(Some).nestedPath >

      // mixed
      some.prototype(That.Is:Nested).prototype(And.Nested:Again) >
      prototype(Some).nestedPath.prototype(Inner) >
      prototype(Some).@nestedMeta.prototype(Inner).path >
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 14 elements
    And the model at "Root.fusion" must contain 9 path erasures
    And the model at "Root.fusion" must contain the path erasures
      | index | path                                                       |
      | 1     | simple                                                     |
      | 3     | some.nested                                                |
      | 5     | @metaProperty                                              |
      | 7     | some.prototype(That.Is:Nested)                             |
      | 8     | @metaProp.prototype(That.Is:Nested)                        |
      | 9     | prototype(Some).nestedPath                                 |
      | 11    | some.prototype(That.Is:Nested).prototype(And.Nested:Again) |
      | 12    | prototype(Some).nestedPath.prototype(Inner)                |
      | 13    | prototype(Some).@nestedMeta.prototype(Inner).path          |

  Scenario: path erasure inside of a path configuration block
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // the outer path (complex)
      outer.@metaPath.prototype(Foo.Bar).inner {
          // simple root path
          simple >
          // nested
          some.nested >

          // meta prop
          @metaProperty >

          // hierarchical prototype erasure
          some.prototype(That.Is:Nested) >
          @metaProp.prototype(That.Is:Nested) >
          prototype(Some).nestedPath >

          // mixed
          some.prototype(That.Is:Nested).prototype(And.Nested:Again) >
          prototype(Some).nestedPath.prototype(Inner) >
          prototype(Some).@nestedMeta.prototype(Inner).path >
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                    |
      | 0     | CodeComment             |
      | 1     | FusionPathConfiguration |
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain 14 elements
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain 9 path erasures
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain the path erasures
      | index | path                                                       |
      | 1     | simple                                                     |
      | 3     | some.nested                                                |
      | 5     | @metaProperty                                              |
      | 7     | some.prototype(That.Is:Nested)                             |
      | 8     | @metaProp.prototype(That.Is:Nested)                        |
      | 9     | prototype(Some).nestedPath                                 |
      | 11    | some.prototype(That.Is:Nested).prototype(And.Nested:Again) |
      | 12    | prototype(Some).nestedPath.prototype(Inner)                |
      | 13    | prototype(Some).@nestedMeta.prototype(Inner).path          |

  Scenario: path erasure inside of a fusion object assignment body
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // the outer path (complex)
      outer.@metaPath.prototype(Foo.Bar).inner = My.Awesome:FusionObject {
          // simple root path
          simple >
          // nested
          some.nested >

          // meta prop
          @metaProperty >

          // hierarchical prototype erasure
          some.prototype(That.Is:Nested) >
          @metaProp.prototype(That.Is:Nested) >
          prototype(Some).nestedPath >

          // mixed
          some.prototype(That.Is:Nested).prototype(And.Nested:Again) >
          prototype(Some).nestedPath.prototype(Inner) >
          prototype(Some).@nestedMeta.prototype(Inner).path >
      }
      """
    When all Fusion packages are parsed
    Then the model at "Root.fusion" must contain 2 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the path assignment at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must have a body
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain 14 elements
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain 9 path erasures
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[1]" must contain the path erasures
      | index | path                                                       |
      | 1     | simple                                                     |
      | 3     | some.nested                                                |
      | 5     | @metaProperty                                              |
      | 7     | some.prototype(That.Is:Nested)                             |
      | 8     | @metaProp.prototype(That.Is:Nested)                        |
      | 9     | prototype(Some).nestedPath                                 |
      | 11    | some.prototype(That.Is:Nested).prototype(And.Nested:Again) |
      | 12    | prototype(Some).nestedPath.prototype(Inner)                |
      | 13    | prototype(Some).@nestedMeta.prototype(Inner).path          |

  Scenario: path erasure inside of a root prototype declaration body
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // the root prototype declaration
      prototype(My.Awesome:FusionObject) < prototype(Something) {
          // simple root path
          simple >
          // nested
          some.nested >

          // meta prop
          @metaProperty >

          // hierarchical prototype erasure
          some.prototype(That.Is:Nested) >
          @metaProp.prototype(That.Is:Nested) >
          prototype(Some).nestedPath >

          // mixed
          some.prototype(That.Is:Nested).prototype(And.Nested:Again) >
          prototype(Some).nestedPath.prototype(Inner) >
          prototype(Some).@nestedMeta.prototype(Inner).path >
      }
      """
    When all Fusion packages are parsed
    Then the model at "Root.fusion" must contain 2 elements
    And the model at "Root.fusion" must contain the elements
      | index | type        |
      | 0     | CodeComment |
      | 1     | Prototype   |
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                    | inherit   |
      | 1     | My.Awesome:FusionObject | Something |
    And the model at "Root.fusion/prototype(My.Awesome:FusionObject)[1]" must contain 14 elements
    And the model at "Root.fusion/prototype(My.Awesome:FusionObject)[1]" must contain 9 path erasures
    And the model at "Root.fusion/prototype(My.Awesome:FusionObject)[1]" must contain the path erasures
      | index | path                                                       |
      | 1     | simple                                                     |
      | 3     | some.nested                                                |
      | 5     | @metaProperty                                              |
      | 7     | some.prototype(That.Is:Nested)                             |
      | 8     | @metaProp.prototype(That.Is:Nested)                        |
      | 9     | prototype(Some).nestedPath                                 |
      | 11    | some.prototype(That.Is:Nested).prototype(And.Nested:Again) |
      | 12    | prototype(Some).nestedPath.prototype(Inner)                |
      | 13    | prototype(Some).@nestedMeta.prototype(Inner).path          |