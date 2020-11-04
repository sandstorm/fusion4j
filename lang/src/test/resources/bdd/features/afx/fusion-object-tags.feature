Feature: AFX Fusion object tags

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: simple object tag on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <My:Component></My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have no body

  Scenario: simple self-closing object tag on root layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <My:Component />
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]" must have no body

  Scenario: simple object tag inside HTML tag
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      myPath = afx`
        <div>
          <My:Component></My:Component>
        </div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "<My:Component>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]/content[1]" must have no body

  Scenario: simple self-closing object tag inside HTML tag
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <div>
          <My:Component />
        </div>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 1 with value "<My:Component>" of type "FusionObjectValue"
    And the path assignment at "Root.fusion/myPath[0]/content[1]" must have no body

  Scenario: multiple object tags on root layer mixed with HTML tags
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component></My:Component>
        <br />
        <Some:Foo />
        <div></div>
        <Some:Bar></Some:Bar>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 5 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_1" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_2" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_3" at index 2 with value "<Some:Foo>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_4" at index 3 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "item_5" at index 4 with value "<Some:Bar>" of type "FusionObjectValue"

  Scenario: object tag with simple inner content
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component>
          <div>I should transpile the path 'content'</div>
        </My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "content" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/content[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/content[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/content[0]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'content'" of type "StringValue"

  Scenario: object tag with spread expression attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component {...props} />
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "@apply.spread_1" at index 0 with value "${props}" of type "ExpressionValue"

  Scenario: object tag with simple inner content and path attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component>
          <div @path="someCustomPath">I should transpile the path 'someCustomPath'</div>
        </My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "someCustomPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath'" of type "StringValue"

  Scenario: object tag with simple inner content and children attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component @children="someCustomPath">
          <div>I should transpile the path 'someCustomPath'</div>
        </My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "someCustomPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath'" of type "StringValue"

  Scenario: object tag with simple inner content path wins over children attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component @children="someOtherCustomPath">
          <div @path="someCustomPath">I should transpile the path 'someCustomPath'</div>
        </My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "someCustomPath" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "tagName" at index 0 with value "div" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath'" of type "StringValue"

  Scenario: object tag with multiple explicit key content elements and children attribute
    Given the Fusion file "Root.fusion" contains the following code
      """
      myPath = afx`
        <My:Component @children="someCustomPath">
          <header @key="headerKey">I should transpile the path 'someCustomPath.headerKey'</header>
          <main @key="mainKey">I should transpile the path 'someCustomPath.mainKey'</main>
          <footer @key="footerKey">I should transpile the path 'someCustomPath.footerKey'</footer>
        </My:Component>
      `
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignment "myPath" at index 0 with value "<My:Component>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]" must contain 1 path assignments
    And the model at "Root.fusion/myPath[0]" must contain the path assignment "someCustomPath" at index 0 with value "<Neos.Fusion:Join>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain 3 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "headerKey" at index 0 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/headerKey[0]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/headerKey[0]" must contain the path assignment "tagName" at index 0 with value "header" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/headerKey[0]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath.headerKey'" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "mainKey" at index 1 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/mainKey[1]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/mainKey[1]" must contain the path assignment "tagName" at index 0 with value "main" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/mainKey[1]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath.mainKey'" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]" must contain the path assignment "footerKey" at index 2 with value "<Neos.Fusion:Tag>" of type "FusionObjectValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/footerKey[2]" must contain 2 path assignments
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/footerKey[2]" must contain the path assignment "tagName" at index 0 with value "footer" of type "StringValue"
    And the model at "Root.fusion/myPath[0]/someCustomPath[0]/footerKey[2]" must contain the path assignment "content" at index 1 with value "I should transpile the path 'someCustomPath.footerKey'" of type "StringValue"
