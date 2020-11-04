Feature: Neos.Fusion:Component implementation

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: simple inline component
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Component {
        a = 20
        b = 2
        renderer = ${props.a + props.b}
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """

  Scenario: common component usage pattern with prototype inheritance and API
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) < prototype(Neos.Fusion:Component) {
        a = 10
        b = 2
        renderer = ${props.a + props.b}
      }
      foo = Foo {
        a = 20
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.Integer"
    Then the evaluated output for path "foo" must be
      """
      22
      """

  Scenario: common component usage pattern with prototype inheritance and API and AFX
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(PageLayout) < prototype(Neos.Fusion:Component) {
        header = null
        main = null
        footer = null
        linkUrl = 'https://neos.io'

        renderer = afx`
          <div class="page-layout">
            <nav>
              <a href={props.linkUrl}>Love Neos</a>
            </nav>
            <header>{props.header}</header>
            <main>{props.main}</header>
            <footer>{props.footer}</footer>
          </div>
        `
      }

      prototype(PageExample) < prototype(Neos.Fusion:Component) {
        pageLinkUrl = null

        renderer = PageLayout {
          header = afx`
            <h1>Page Example</h1>
          `
          main = afx`
            <p>Some content!</p>
          `
          footer = afx`
            provided by <a href={props.pageLinkUrl}>Sandstorm</a>
          `
        }
      }
      foo = PageExample {
        pageLinkUrl = 'https://sandstorm.de'
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div class="page-layout"><nav><a href="https://neos.io">Love Neos</a></nav><header><h1>Page Example</h1></header><main><p>Some content!</p></main><footer>provided by <a href="https://sandstorm.de">Sandstorm</a></footer></div>
      """
