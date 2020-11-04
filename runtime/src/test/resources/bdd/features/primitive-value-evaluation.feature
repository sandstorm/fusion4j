Feature: evaluation of simple values

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // string
      rootLevelStringProp = "root level String property"
      rootLevelNewlineChar = "\n"
      // boolean
      rootLevelBoolTrue = true
      rootLevelBoolFalse = false
      // integer
      rootLevelIntZero = 0
      rootLevelInt = 12
      rootLevelNInt = -200
      // double
      rootLevelDoubleZero = 0.0000
      rootLevelDouble = 3.1415
      rootLevelNDouble = -273.15
      // null
      rootLevelNull = null

      nested {
        // string
        innerString = 'inner String property'
        innerNewlineChar = '\n'
        // boolean
        innerBoolTrue = TRUE
        innerBoolFalse = FALSE
        // integer
        innerIntZero = 0
        innerInt = 42
        innerNInt = -999999999
        // double
        innerDoubleZero = 0.0000
        innerDouble = 3.1415
        innerNDouble = -273.15
        // null
        innerNull = NULL
      }
      """
    Given all Fusion packages are parsed
    And a Fusion runtime

  Scenario: string root level property is evaluated
    When I evaluate the Fusion path "rootLevelStringProp"
    Then the evaluated output for path "rootLevelStringProp" must be of type "java.lang.String"
    And the evaluated output for path "rootLevelStringProp" must be
      """
      root level String property
      """

  Scenario: string nested property is evaluated
    When I evaluate the Fusion path "nested.innerString"
    Then the evaluated output for path "nested.innerString" must be of type "java.lang.String"
    And the evaluated output for path "nested.innerString" must be
      """
      inner String property
      """

  Scenario: newline string root level property is evaluated
    When I evaluate the Fusion path "rootLevelNewlineChar"
    Then the evaluated output for path "rootLevelNewlineChar" must be of type "java.lang.String"
    And the evaluated output for path "rootLevelNewlineChar" must be a special whitespace char "\n"

  Scenario: boolean false root level property is evaluated
    When I evaluate the Fusion path "rootLevelBoolFalse"
    Then the evaluated output for path "rootLevelBoolFalse" must be of type "java.lang.Boolean"
    And the evaluated output for path "rootLevelBoolFalse" must be
      """
      false
      """

  Scenario: boolean true root level property is evaluated
    When I evaluate the Fusion path "rootLevelBoolTrue"
    Then the evaluated output for path "rootLevelBoolTrue" must be of type "java.lang.Boolean"
    And the evaluated output for path "rootLevelBoolTrue" must be
      """
      true
      """

  Scenario: boolean false nested property is evaluated
    When I evaluate the Fusion path "nested.innerBoolFalse"
    Then the evaluated output for path "nested.innerBoolFalse" must be of type "java.lang.Boolean"
    And the evaluated output for path "nested.innerBoolFalse" must be
      """
      false
      """

  Scenario: boolean true nested property is evaluated
    When I evaluate the Fusion path "nested.innerBoolTrue"
    Then the evaluated output for path "nested.innerBoolTrue" must be of type "java.lang.Boolean"
    And the evaluated output for path "nested.innerBoolTrue" must be
      """
      true
      """

  Scenario: integer zero root level property is evaluated
    When I evaluate the Fusion path "rootLevelIntZero"
    Then the evaluated output for path "rootLevelIntZero" must be of type "java.lang.Integer"
    And the evaluated output for path "rootLevelIntZero" must be
      """
      0
      """

  Scenario: integer positive value root level property is evaluated
    When I evaluate the Fusion path "rootLevelInt"
    Then the evaluated output for path "rootLevelInt" must be of type "java.lang.Integer"
    And the evaluated output for path "rootLevelInt" must be
      """
      12
      """

  Scenario: integer negative value root level property is evaluated
    When I evaluate the Fusion path "rootLevelNInt"
    Then the evaluated output for path "rootLevelNInt" must be of type "java.lang.Integer"
    And the evaluated output for path "rootLevelNInt" must be
      """
      -200
      """

  Scenario: integer zero nested property is evaluated
    When I evaluate the Fusion path "nested.innerIntZero"
    Then the evaluated output for path "nested.innerIntZero" must be of type "java.lang.Integer"
    And the evaluated output for path "nested.innerIntZero" must be
      """
      0
      """

  Scenario: integer positive value nested property is evaluated
    When I evaluate the Fusion path "nested.innerInt"
    Then the evaluated output for path "nested.innerInt" must be of type "java.lang.Integer"
    And the evaluated output for path "nested.innerInt" must be
      """
      42
      """

  Scenario: integer negative value nested property is evaluated
    When I evaluate the Fusion path "nested.innerNInt"
    Then the evaluated output for path "nested.innerNInt" must be of type "java.lang.Integer"
    And the evaluated output for path "nested.innerNInt" must be
      """
      -999999999
      """

  Scenario: double zero root level property is evaluated
    When I evaluate the Fusion path "rootLevelDoubleZero"
    Then the evaluated output for path "rootLevelDoubleZero" must be of type "java.lang.Double"
    And the evaluated output for path "rootLevelDoubleZero" must be
      """
      0.0
      """

  Scenario: double positive value root level property is evaluated
    When I evaluate the Fusion path "rootLevelDouble"
    Then the evaluated output for path "rootLevelDouble" must be of type "java.lang.Double"
    And the evaluated output for path "rootLevelDouble" must be
      """
      3.1415
      """

  Scenario: double negative value root level property is evaluated
    When I evaluate the Fusion path "rootLevelNDouble"
    Then the evaluated output for path "rootLevelNDouble" must be of type "java.lang.Double"
    And the evaluated output for path "rootLevelNDouble" must be
      """
      -273.15
      """

  Scenario: double zero nested property is evaluated
    When I evaluate the Fusion path "nested.innerDoubleZero"
    Then the evaluated output for path "nested.innerDoubleZero" must be of type "java.lang.Double"
    And the evaluated output for path "nested.innerDoubleZero" must be
      """
      0.0
      """

  Scenario: integer positive value nested property is evaluated
    When I evaluate the Fusion path "nested.innerDouble"
    Then the evaluated output for path "nested.innerDouble" must be of type "java.lang.Double"
    And the evaluated output for path "nested.innerDouble" must be
      """
      3.1415
      """

  Scenario: integer negative value nested property is evaluated
    When I evaluate the Fusion path "nested.innerNDouble"
    Then the evaluated output for path "nested.innerNDouble" must be of type "java.lang.Double"
    And the evaluated output for path "nested.innerNDouble" must be
      """
      -273.15
      """

  Scenario: null value root level property is evaluated
    When I evaluate the Fusion path "rootLevelNull"
    Then the evaluated output for path "rootLevelNull" must be null

  Scenario: null value nested property is evaluated
    When I evaluate the Fusion path "nested.innerNull"
    Then the evaluated output for path "nested.innerNull" must be null
