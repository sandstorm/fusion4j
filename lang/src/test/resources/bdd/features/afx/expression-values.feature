Feature: expression values in AFX

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: HTML tag with expression property attribute
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div foo={props.bar}></div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.foo" at index 1 with value "${props.bar}" of type "ExpressionValue"

  Scenario: body expression value root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        {props.bar}
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "${props.bar}" of type "ExpressionValue"

  Scenario: body expression value in HTML tag content
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div>{props.bar}</div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "${props.bar}" of type "ExpressionValue"

  Scenario: multiple body expression values root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        {props.foo}
        {props.bar}
        {props.baz}
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "${props.foo}" of type "ExpressionValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value "${props.bar}" of type "ExpressionValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value "${props.baz}" of type "ExpressionValue"

  Scenario: meaningful whitespaces on expression values root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        {props.foo} {props.bar}
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "${props.foo}" of type "ExpressionValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value " " of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value "${props.bar}" of type "ExpressionValue"

  Scenario: body expression value in HTML tag content
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <div>{props.bar}</div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "${props.bar}" of type "ExpressionValue"

  Scenario: multiple body expression values in HTML tag content
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <div>
          {props.bar}
          {props.meaningful} {props.whitespace}
        </div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain 4 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_1" at index 0 with value "${props.bar}" of type "ExpressionValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_2" at index 1 with value "${props.meaningful}" of type "ExpressionValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_3" at index 2 with value " " of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_4" at index 3 with value "${props.whitespace}" of type "ExpressionValue"

