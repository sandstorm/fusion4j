Feature: AFX default HTML tags

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: simple empty HTML tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div></div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"

  Scenario: simple empty self-closing HTML tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div/>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "selfClosingTag" at index 1 with value "true" of type "BooleanValue"

  Scenario: simple nested HTML tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div>
          <p></p>
        </div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "tagName" at index 0 with value "p" of type "StringValue"

  Scenario: multiple nested HTML tags
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <main>
          <h1></h1>
          <div>
            <h2></h2>
            <br/>
            <p></p>
          </div>
        </main>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "main" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_1" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_1[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_1[0]" must contain the path assignment "tagName" at index 0 with value "h1" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]" must contain the path assignment "content" at index 1 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]" must contain the path assignment "item_1" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]" must contain the path assignment "item_3" at index 2 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_1[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_1[0]" must contain the path assignment "tagName" at index 0 with value "h2" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_2[1]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_2[1]" must contain the path assignment "tagName" at index 0 with value "br" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_2[1]" must contain the path assignment "selfClosingTag" at index 1 with value "true" of type "BooleanValue"
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_3[2]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/content[1]/item_2[1]/content[1]/item_3[2]" must contain the path assignment "tagName" at index 0 with value "p" of type "StringValue"

  Scenario: multiple root HTML tags
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <h1></h1>
        <main></main>
        <footer></footer>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/item_1[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/item_1[0]" must contain the path assignment "tagName" at index 0 with value "h1" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/item_2[1]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/item_2[1]" must contain the path assignment "tagName" at index 0 with value "main" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/item_3[2]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]/item_3[2]" must contain the path assignment "tagName" at index 0 with value "footer" of type "StringValue"

  Scenario: HTML tag with string property attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <div foo="bar" class='hallo' other=value novalue></div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 5 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.foo" at index 1 with value "bar" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.class" at index 2 with value "hallo" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.other" at index 3 with value "value" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.novalue" at index 4 with value "" of type "StringValue"

  Scenario: a tag with content and href
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <a href="https://sandstorm.de">Sandstorm</a>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have a body
    And the model at "Root.fusion/myPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "a" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "attributes.href" at index 1 with value "https://sandstorm.de" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 2 with value "Sandstorm" of type "StringValue"
