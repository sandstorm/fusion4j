Feature: AFX HTML text

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: root HTML string transpiles to Fusion string
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        some string
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "some string" of type "StringValue"

  Scenario: root multiline HTML string whitespaces are collapsed to single space
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        This is a string literal
        with multiple lines
        that shall collapse
        to spaces.
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "This is a string literal with multiple lines that shall collapse to spaces." of type "StringValue"

  Scenario: root multiline HTML string whitespaces are collapsed to single space
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div>
          This is a string literal
          with multiple lines
          that shall collapse
          to spaces.
        </div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "This is a string literal with multiple lines that shall collapse to spaces." of type "StringValue"

  Scenario: single quotes in HTML text
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        some 'single quoted' string
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "some 'single quoted' string" of type "StringValue"

  Scenario: double quotes in HTML text
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        some "double quoted" string
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "some \"double quoted\" string" of type "StringValue"

  Scenario: whitespace before tags are considered meaningful
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        some text    <span>and tag</span>   text after tag
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "some text " of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value " text after tag" of type "StringValue"

  Scenario: whitespace in newlines are not considered meaningful
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        some text
        <span>and tag</span>
        text after tag
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "some text" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value "text after tag" of type "StringValue"

