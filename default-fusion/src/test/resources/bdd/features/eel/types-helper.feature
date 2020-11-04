Feature: Types EEL helper

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    And the following EEL helper class mapping
      | name  | class                                    |
      | Types | io.neos.fusion4j.neos.eel.TypesEelHelper |

  Scenario: types helper isString true
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      testPath.@context.foo = 'some string'
      testPath = ${Types.isString(foo)}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Boolean"
    And the evaluated output for path "testPath" must be
      """
      true
      """

  Scenario: types helper isString false
    Given the Fusion file "Root.fusion" contains the following code
      """
      testPath.@context.foo = 123
      testPath = ${Types.isString(foo)}
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "testPath"
    Then the evaluated output for path "testPath" must be of type "java.lang.Boolean"
    And the evaluated output for path "testPath" must be
      """
      false
      """
