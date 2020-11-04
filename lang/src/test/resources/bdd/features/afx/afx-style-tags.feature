Feature: AFX CSS style HTML tags

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: AFX style tag on root level
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <style>
          a {
            display: block;
          }
        </style>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 of type "StringValue" with the following value
      """
      <style>
        a {
          display: block;
        }
      </style>
      """

  Scenario: AFX style tag in prototype nested
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      prototype(Foo) {
        foo = afx`
          <style>
            a {
              display: block;
            }
          </style>
        `
      }

      prototype(Bar) {
        someValue = Foo
      }

      myPath = Bar
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/prototype(Foo)[0]" must contain 1 path assignments
    And the model at "Root.fusion/prototype(Foo)[0]" must contain the path assignment "foo" at index 0 of type "StringValue" with the following value
      """html
      <style>
        a {
          display: block;
        }
      </style>
      """
    When I load the Fusion object instance for evaluation path "myPath<Bar>/someValue<Foo>"
    Then the loaded Fusion object instance for path "myPath<Bar>/someValue<Foo>" must have the attribute "foo" with absolute path "prototype(Foo).foo" of type "[STRING]" with the following value
      """html
      <style>
        a {
          display: block;
        }
      </style>
      """

