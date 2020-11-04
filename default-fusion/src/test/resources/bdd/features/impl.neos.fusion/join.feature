Feature: Neos.Fusion:Join implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: simple join of strings
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
         a = 'a'
         b = 'b'
         c = 'c'
      }
      foo.d = 'd'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      abcd
      """

  Scenario: simple join of strings with glue
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
        @glue = ${" " + " "}
        a = 'a'
        b = 'b'
        c = 'c'
      }
      foo.d = 'd'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      a  b  c  d
      """

  Scenario: simple join of strings with newline glue
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
        @glue = ${"\n"}
        a = 'a'
        b = 'b'
        c = 'c'
      }
      foo.d = 'd'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      a
      b
      c
      d
      """

  Scenario: simple join of strings with newline
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
        a = 'a'
        b = ${"\n"}
        c = 'b'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      a
      b
      """

  Scenario: nested join of strings
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
         a = 'a'
         b = 'b'
         cAndD = Neos.Fusion:Join {
           c = 'c'
           d = 'd'
         }
      }
      foo.e = 'e'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      abcde
      """

  Scenario: nested join of strings with untyped path
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Join {
         a = 'a'
         b = 'b'
         cAndD {
           c = 'c'
           d = 'd'
         }
      }
      foo.e = 'e'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      abcde
      """
