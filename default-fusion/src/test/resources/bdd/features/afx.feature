Feature: AFX transpiler DSL runtime rendering

  Background:
    Given the default Fusion package "Neos.Fusion" is registered
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the following Fusion package load order
      | Neos.Fusion   |
      | MyTestPackage |

  Scenario: transpiled AFX tags
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        <div>
          <p>22</p>
        </div>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div><p>22</p></div>
      """

  Scenario: transpiled AFX tags with attributes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        <div class={'a' + 'b'}>
          <p style="abcd">22</p>
        </div>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div class="ab"><p style="abcd">22</p></div>
      """

  Scenario: transpiled AFX tags with multiple content elements
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        <div class="main">
          <h1>Title</h1>
          <p>Header</p>
          <br/>
          <div>Hallo AFX!</div>
        </div>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div class="main"><h1>Title</h1><p>Header</p><br/><div>Hallo AFX!</div></div>
      """

  Scenario: transpiled AFX root join
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        provided by <a href="https://sandstorm.de">Sandstorm</a>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      provided by <a href="https://sandstorm.de">Sandstorm</a>
      """

  Scenario: evaluation of a tag with content and href
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        <a href="https://sandstorm.de">Sandstorm</a>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <a href="https://sandstorm.de">Sandstorm</a>
      """

  Scenario: evaluation of a tag with content and href from context
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value {
        @context.linkUrl = 'https://sandstorm.de'
        value = afx`
          <a href={linkUrl}>Sandstorm</a>
        `
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <a href="https://sandstorm.de">Sandstorm</a>
      """

  Scenario: evaluation of AFX containing a script tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value {
        @context.linkUrl = 'https://sandstorm.de'
        value = Neos.Fusion:Value {
          @context.injectFusionValueInScript = Neos.Fusion:Tag {
            tagName = 'script'
            content = ${'window.ACCESS_FUSION_VALUE_API = "' + linkUrl + '";'}
          }
          value = afx`
            <div>
              <a href={linkUrl}>Sandstorm</a>
              {injectFusionValueInScript}
              <script>
                var callback = function() {
                  console.log("visit: " + window.ACCESS_FUSION_VALUE_API);
                }
              </script>
            </div>
          `
        }
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be like
      """html
      <div>
        <a href="https://sandstorm.de">Sandstorm</a>
        <script>window.ACCESS_FUSION_VALUE_API = "https://sandstorm.de";</script>
        <script>
          var callback = function() {
            console.log("visit: " + window.ACCESS_FUSION_VALUE_API);
          }
        </script>
      </div>
      """

  Scenario: evaluation of AFX containing a style tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = Neos.Fusion:Value {
        @context.linkUrl = 'https://sandstorm.de'
        value = afx`
          <div>
            <a href={linkUrl}>Sandstorm</a>
            <style>
              a {
                display: block;
              }
            </style>
          </div>
        `
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be like
      """html
      <div>
        <a href="https://sandstorm.de">Sandstorm</a>
        <style>
          a {
            display: block;
          }
        </style>
      </div>
      """

  Scenario: transpiled AFX tags with HTML comments
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = afx`
        <div>
          <!-- this is a comment -->
          <p>22</p>
        </div>
      `
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "foo"
    Then the evaluated output for path "foo" must be of type "java.lang.String"
    Then the evaluated output for path "foo" must be
      """html
      <div><!-- this is a comment --><p>22</p></div>
      """

