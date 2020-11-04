Feature: special path names

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: single quoted one segment path name on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      'aaa' = 123
      'hello world' = 'foo'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 path assignments
    And the model at "Root.fusion" must contain the path assignment "'aaa'" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "'hello world'" at index 1 with value "foo" of type "StringValue"

  Scenario: double quoted one segment path name on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      "aaa" = 123
      "hello world" = 'foo'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 path assignments
    And the model at "Root.fusion" must contain the path assignment "\"aaa\"" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "\"hello world\"" at index 1 with value "foo" of type "StringValue"

  Scenario: quoted one segment path name with special characters on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      "Content-Type" = "with dash and case sensitive"
      'öäüÖÄÜß!€@?§$%&\/()=*"+#-.:,;' = 'all special chars that I found on my keyboard'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 path assignments
    And the model at "Root.fusion" must contain the path assignment "\"Content-Type\"" at index 0 with value "with dash and case sensitive" of type "StringValue"
    And the model at "Root.fusion" must contain the path assignment "'öäüÖÄÜß!€@?§$%&\/()=*\"+#-.:,;'" at index 1 with value "all special chars that I found on my keyboard" of type "StringValue"

  Scenario: single quoted multi segment path name on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """
      some.'aaa' = 123
      'aaa'.some = true
      bbb.'aaa'.some = 42

      some.'hello world' = 'foo'
      'hello world'.some = true
      bbb.'hello world'.some = 42
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 path assignments
    And the model at "Root.fusion" must contain the path assignment "some.'aaa'" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "'aaa'.some" at index 1 with value "true" of type "BooleanValue"
    And the model at "Root.fusion" must contain the path assignment "bbb.'aaa'.some" at index 2 with value "42" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "some.'hello world'" at index 3 with value "foo" of type "StringValue"
    And the model at "Root.fusion" must contain the path assignment "'hello world'.some" at index 4 with value "true" of type "BooleanValue"
    And the model at "Root.fusion" must contain the path assignment "bbb.'hello world'.some" at index 5 with value "42" of type "IntegerValue"

  Scenario: double quoted multi segment path name on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """
      some."aaa" = 123
      "aaa".some = true
      bbb."aaa".some = 42

      some."hello world" = "foo"
      "hello world".some = true
      bbb."hello world".some = 42
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 path assignments
    And the model at "Root.fusion" must contain the path assignment "some.\"aaa\"" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "\"aaa\".some" at index 1 with value "true" of type "BooleanValue"
    And the model at "Root.fusion" must contain the path assignment "bbb.\"aaa\".some" at index 2 with value "42" of type "IntegerValue"
    And the model at "Root.fusion" must contain the path assignment "some.\"hello world\"" at index 3 with value "foo" of type "StringValue"
    And the model at "Root.fusion" must contain the path assignment "\"hello world\".some" at index 4 with value "true" of type "BooleanValue"
    And the model at "Root.fusion" must contain the path assignment "bbb.\"hello world\".some" at index 5 with value "42" of type "IntegerValue"

  Scenario: single quoted one segment path name in nested fusion blocks
    Given the Fusion file "Root.fusion" contains the following code
      """
      pathConfig.'with-quoted pathsegment' {
        'aaa' = 123
        'hello world' = 'foo'
      }

      'prototype usage declaration' = Some.Value {
        'aaa' = 123
        'hello world' = 'foo'
      }

      prototype(Vendor:Some.RootPrototype) {
        'aaa' = 123
        'hello world' = 'foo'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                    |
      | 0     | FusionPathConfiguration |
      | 1     | FusionPathAssignment    |
      | 2     | Prototype               |
    And the model at "Root.fusion" must contain the path configuration "pathConfig.'with-quoted pathsegment'" at index 0
    And the model at "Root.fusion" must contain the path assignment "'prototype usage declaration'" at index 1 with value "<Some.Value>" of type "FusionObjectValue"
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                      | hasBody |
      | 2     | Vendor:Some.RootPrototype | true    |
    And the model at "Root.fusion/pathConfig.'with-quoted pathsegment'[0]" must contain the path assignment "'aaa'" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/pathConfig.'with-quoted pathsegment'[0]" must contain the path assignment "'hello world'" at index 1 with value "foo" of type "StringValue"
    And the model at "Root.fusion/'prototype usage declaration'[1]" must contain the path assignment "'aaa'" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/'prototype usage declaration'[1]" must contain the path assignment "'hello world'" at index 1 with value "foo" of type "StringValue"
    And the model at "Root.fusion/prototype(Vendor:Some.RootPrototype)[2]" must contain the path assignment "'aaa'" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/prototype(Vendor:Some.RootPrototype)[2]" must contain the path assignment "'hello world'" at index 1 with value "foo" of type "StringValue"

  Scenario: single quoted one segment path name in nested fusion blocks
    Given the Fusion file "Root.fusion" contains the following code
      """
      pathConfig."with-quoted pathsegment" {
        "aaa" = 123
        "hello world" = 'foo'
      }

      "prototype usage declaration" = Some.Value {
        "aaa" = 123
        "hello world" = 'foo'
      }

      prototype(Vendor:Some.RootPrototype) {
        "aaa" = 123
        "hello world" = 'foo'
      }
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 3 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                    |
      | 0     | FusionPathConfiguration |
      | 1     | FusionPathAssignment    |
      | 2     | Prototype               |
    And the model at "Root.fusion" must contain the path configuration "pathConfig.\"with-quoted pathsegment\"" at index 0
    And the model at "Root.fusion" must contain the path assignment "\"prototype usage declaration\"" at index 1 with value "<Some.Value>" of type "FusionObjectValue"
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                      | hasBody |
      | 2     | Vendor:Some.RootPrototype | true    |
    And the model at "Root.fusion/pathConfig.\"with-quoted pathsegment\"[0]" must contain the path assignment "\"aaa\"" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/pathConfig.\"with-quoted pathsegment\"[0]" must contain the path assignment "\"hello world\"" at index 1 with value "foo" of type "StringValue"
    And the model at "Root.fusion/\"prototype usage declaration\"[1]" must contain the path assignment "\"aaa\"" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/\"prototype usage declaration\"[1]" must contain the path assignment "\"hello world\"" at index 1 with value "foo" of type "StringValue"
    And the model at "Root.fusion/prototype(Vendor:Some.RootPrototype)[2]" must contain the path assignment "\"aaa\"" at index 0 with value "123" of type "IntegerValue"
    And the model at "Root.fusion/prototype(Vendor:Some.RootPrototype)[2]" must contain the path assignment "\"hello world\"" at index 1 with value "foo" of type "StringValue"
    