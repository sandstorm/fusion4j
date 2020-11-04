Feature: raw 'lang' model parsing for fusion data types

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: primitive values on top level assignment
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // null
    myNull = null

    // strings
    myStringSingleQuotes = 'hello single quoted string'
    myStringDoubleQuotes = "hello double quoted string"

    // boolean
    myBoolTrue = true
    myBoolFalse = false

    // numbers
    myInt = 42
    myDouble = 3.1415
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "//Root.fusion" must contain 7 path assignments
    And the model at "//Root.fusion" must contain the path assignments
      | index | path                 | value                      | type         |
      | 1     | myNull               | NULL                       | NullValue    |
      | 3     | myStringSingleQuotes | hello single quoted string | StringValue  |
      | 4     | myStringDoubleQuotes | hello double quoted string | StringValue  |
      | 6     | myBoolTrue           | true                       | BooleanValue |
      | 7     | myBoolFalse          | false                      | BooleanValue |
      | 9     | myInt                | 42                         | IntegerValue |
      | 10    | myDouble             | 3.1415                     | DoubleValue  |

  Scenario: primitive values on inner fusion block assignment
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // outer path
    myPath {
        // null
        myNull = null

        // strings
        myStringSingleQuotes = 'hello single quoted string'
        myStringDoubleQuotes = "hello double quoted string"

        // boolean
        myBoolTrue = true
        myBoolFalse = false

        // numbers
        myInt = 42
        myDouble = 3.1415
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/myPath[1]" must contain 7 path assignments
    And the model at "Root.fusion/myPath[1]" must contain the path assignments
      | index | path                 | value                      | type         |
      | 1     | myNull               | NULL                       | NullValue    |
      | 3     | myStringSingleQuotes | hello single quoted string | StringValue  |
      | 4     | myStringDoubleQuotes | hello double quoted string | StringValue  |
      | 6     | myBoolTrue           | true                       | BooleanValue |
      | 7     | myBoolFalse          | false                      | BooleanValue |
      | 9     | myInt                | 42                         | IntegerValue |
      | 10    | myDouble             | 3.1415                     | DoubleValue  |

  Scenario: expressions on top level assignment
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // expression
    myExpression = ${someExpressionCall('hallo', 123, true)}
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path         | value                                     | type            |
      | 1     | myExpression | ${someExpressionCall('hallo', 123, true)} | ExpressionValue |

  Scenario: expressions on inner fusion block assignment
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // outer fusion
    prototype(FooBar) {
        // expression
        myExpression = ${someExpressionCall('hallo', 123, true)}
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion/prototype(FooBar)[1]" must contain 1 path assignments
    And the model at "Root.fusion/prototype(FooBar)[1]" must contain the path assignments
      | index | path         | value                                     | type            |
      | 1     | myExpression | ${someExpressionCall('hallo', 123, true)} | ExpressionValue |

  Scenario: fusion objects on top level assignment
    Given the Fusion file "Root.fusion" contains the following code
    """fusion
    // fusion object instantiation

    // no body
    myObjectNoBody = Some:Fusion.Object

    // with body
    myObjectWithBody = AnotherObject {
        // this is a fusion body
        someInnerPath = 'foo'
        someOtherInnerPath = 1234
    }
    """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 path assignments
    And the model at "Root.fusion" must contain the path assignments
      | index | path             | value                | type              |
      | 2     | myObjectNoBody   | <Some:Fusion.Object> | FusionObjectValue |
      | 4     | myObjectWithBody | <AnotherObject>      | FusionObjectValue |
    And the path assignment at "Root.fusion/myObjectNoBody[2]" must have no body
    And the path assignment at "Root.fusion/myObjectWithBody[4]" must have a body
    And the model at "Root.fusion/myObjectWithBody[4]" must contain 2 path assignments
    And the model at "Root.fusion/myObjectWithBody[4]" must contain the path assignments
      | index | path               | value | type         |
      | 1     | someInnerPath      | foo   | StringValue  |
      | 2     | someOtherInnerPath | 1234  | IntegerValue |
