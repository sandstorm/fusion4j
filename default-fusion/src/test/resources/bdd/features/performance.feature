@performance
Feature: rendering performance

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Fixtures.fusion" contains the following code
      """fusion
      prototype(Fusion4j.Styleguide:Components.Header) < prototype(Neos.Fusion:Component) {
        mainNavigation = null
        headerLogoLinkUrl = null
        sandstormLogoImageUri = 'sandstorm-logo.svg'
        neosLogoImageUri = 'neos-logo.svg'
        fusion4jLogoImageUri = 'fusion4j-logo.png'

        renderer = afx`
            <div class="header-content">
                <a class="main-logo" href={props.headerLogoLinkUrl}>
                    <img src={props.fusion4jLogoImageUri} alt="fusion4j Logo" />
                </a>
                <div class="main-navigation">
                    {props.mainNavigation}
                </div>
                <div class="thanks-to-logos">
                    <div class="thanks-to-logo sandstorm-logo">
                        <span class="label">provided by</span>
                        <a href="https://sandstorm.de" target="_blank">
                            <img src={props.sandstormLogoImageUri} alt="Sandstorm Logo" />
                        </a>
                    </div>
                    <div class="thanks-to-logo neos-logo">
                        <span class="label">check out</span>
                        <a href="https://neos.io" target="_blank">
                            <img src={props.neosLogoImageUri} alt="Neos Logo" />
                        </a>
                    </div>
                </div>
            </div>
        `
      }
      """
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: lots of stuff to render
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Loop {
        items = ${(1 .. 10000).toArray()}
        itemRenderer = Fusion4j.Styleguide:Components.Header
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 10 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"

  Scenario: lots of stuff to render with if and context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Neos.Fusion:Loop {
        @context.var1 = 1
        @if.foo = ${size(value) > 0}
        items = ${(1 .. 10000).toArray()}
        itemRenderer = Fusion4j.Styleguide:Components.Header
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 10 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"

  Scenario: big AFX to render
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      foo = Fusion4j.Styleguide:Components.Header
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 10 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"


  Scenario: big loop of strings
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = ${(1 .. 10000).toArray()}
        itemRenderer = 'a'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 5 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"

  Scenario: big loop of tags
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Loop {
        items = ${(1 .. 10000).toArray()}
        itemRenderer = Neos.Fusion:Tag {
          tagName = 'p'
          content = 'a'
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 5 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"


  Scenario: string
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = 'a'
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo" 10 times
    Then the evaluated output for path "foo" must be of type "java.lang.String"

  Scenario: big data structure in context with loop
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(Test:A) < prototype(Neos.Fusion:Component) {
        items = null
        renderer = Neos.Fusion:Loop {
          items = ${props.items}
          itemRenderer = Test:B {
            item = ${item}
          }
        }
      }

      prototype(Test:B) < prototype(Neos.Fusion:Component) {
        item = null
        renderer = afx`
          <p>{props.item.foo}</p>
        `
      }

      foo = Test:A {
        items = ${bigDataStructure}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    And a Fusion data structure context variable "bigDataStructure" with the following JSON value 100000 times
      """
      {
        "foo": "bar",
        "someBool": true,
        "nested": {
          "hello": "world"
        }
      }
      """
    When I evaluate the Fusion path "foo" 5 times with context vars
      | bigDataStructure |
    Then the evaluated output for path "foo" must be of type "java.lang.String"

  Scenario: big data structure in context no loop
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *

      prototype(Test:A) < prototype(Neos.Fusion:Component) {
        items = null
        renderer = Test:B {
          item1 = ${size(items)}
        }
      }
      prototype(Test:B) < prototype(Neos.Fusion:Component) {
        item1 = null
        renderer = afx`
          <p><Test:C item2={props.item1}/></p>
        `
      }
      prototype(Test:C) < prototype(Neos.Fusion:Component) {
        item2 = null
        renderer = afx`
          <p>{props.item2}</p>
        `
      }

      foo = Test:A {
        items = ${bigDataStructure}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    And a Fusion data structure context variable "bigDataStructure" with the following JSON value 100 times
      """
      {
        "foo": "bar",
        "someBool": true,
        "nested": {
          "hello": "world"
        }
      }
      """
    When I evaluate the Fusion path "foo" with context vars
      | bigDataStructure |
    Then the evaluated output for path "foo" must be of type "java.lang.String"

