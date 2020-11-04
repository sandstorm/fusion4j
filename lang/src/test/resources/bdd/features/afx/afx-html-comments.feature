Feature: AFX HTML comments

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: AFX HTML comment on root level
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <!-- some html comment -->
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 of type "StringValue" with the following value
      """html
      <!-- some html comment -->
      """
