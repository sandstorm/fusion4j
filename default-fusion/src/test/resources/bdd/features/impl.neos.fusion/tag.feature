Feature: Neos.Fusion:Tag implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: simple div tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = 'div'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div></div>
      """

  Scenario: simple self closing div tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = 'div'
         selfClosingTag = true
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div/>
      """

  Scenario: omit closing div tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = 'div'
         omitClosingTag = true
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div>
      """

  Scenario Outline: default self closing tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = '<tagname>'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """
      <<tagname>/>
      """
    Examples:
      | tagname |
      | area    |
      | base    |
      | br      |
      | col     |
      | command |
      | embed   |
      | hr      |
      | img     |
      | input   |
      | keygen  |
      | link    |
      | meta    |
      | param   |
      | source  |
      | track   |
      | wbr     |

  Scenario: attributes on tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = 'div'
         attributes.empty = true
         attributes.class = "class_1 class_2"
         attributes.ignoreMe = false
         attributes.conditionallyIgnored = 22
         attributes.conditionallyIgnored.@if.alwaysFalse = false
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div class="class_1 class_2" empty></div>
      """

  Scenario: disallow empty attributes on tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
         tagName = 'div'
         allowEmptyAttributes = false
         attributes.empty = true
         attributes.otherEmpty = ""
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div empty="" otherEmpty=""></div>
      """

  Scenario: tag with content
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Tag {
        tagName = 'div'
        content = Neos.Fusion:Tag {
          tagName = 'p'
          content = ${20 + 2}
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div><p>22</p></div>
      """
