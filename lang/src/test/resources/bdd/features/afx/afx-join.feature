Feature: AFX Fusion join

  multiple tag contents / root AFX join elements

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    And the Fusion file "AfxTestDefault.fusion" contains the following code
      """
      // empty mock declarations to enable prototype loading
      // we test no runtime here anyways ;)
      prototype(Neos.Fusion:Tag) {
      }
      prototype(Neos.Fusion:Join) {
      }
      prototype(Neos.Fusion:Component) {
      }
      """

  Scenario: afx join in prototype attribute
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
      prototype(Foo) {
        values = afx`
          some text and <p>a tag</p>
        `
      }

      myPath = Foo
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 2 with value "<Foo>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[2]" must have no body
    And the model at "Root.fusion/prototype(Foo)[1]" must contain 1 path assignments
    And the model at "Root.fusion/prototype(Foo)[1]" must contain the path assignment "values" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/prototype(Foo)[1]/values[0]" must contain 2 path assignments
    And the model at "Root.fusion/prototype(Foo)[1]/values[0]" must contain the path assignment "item_1" at index 0 with value "some text and " of type "StringValue"
    And the model at "Root.fusion/prototype(Foo)[1]/values[0]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    When I load the Fusion object instance for evaluation path "myPath<Foo>/values<Neos.Fusion:Tag>"
    Then the loaded Fusion object instance for path "myPath<Foo>/values<Neos.Fusion:Tag>" must have the following attributes
      | relativePath | absolutePath                 | value             | type            |
      | item_1       | prototype(Foo).values.item_1 | "some text and "  | [STRING]        |
      | item_2       | prototype(Foo).values.item_2 | <Neos.Fusion:Tag> | [FUSION_OBJECT] |

  Scenario: afx join nested component example
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      include: *
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
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then there must be no parse errors in file "Root.fusion"
    When I load the Fusion object instance for evaluation path "foo<PageExample>/renderer<PageLayout>/footer<Neos.Fusion:Join>"
    Then the loaded Fusion object instance for path "foo<PageExample>/renderer<PageLayout>/footer<Neos.Fusion:Join>" must have the following attributes
      | relativePath | absolutePath                                  | value             | type            |
      | item_1       | prototype(PageExample).renderer.footer.item_1 | "provided by "    | [STRING]        |
      | item_2       | prototype(PageExample).renderer.footer.item_2 | <Neos.Fusion:Tag> | [FUSION_OBJECT] |
