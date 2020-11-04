Feature: string values in fusion

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: string single quoted
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = 'bar something'
      withEscaping = 'bar  \' baz'

      inner {
          foo = 'bar something'
          withEscaping = 'bar  \' baz'
      }

      noNeedForEscape = 'bar  \\" baz'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignments
      | index | path            | value         | type        |
      | 0     | foo             | bar something | StringValue |
      | 1     | withEscaping    | bar  ' baz    | StringValue |
      | 3     | noNeedForEscape | bar  \" baz   | StringValue |
    And the model at "Root.fusion/inner[2]" must contain the path assignments
      | index | path         | value         | type        |
      | 0     | foo          | bar something | StringValue |
      | 1     | withEscaping | bar  ' baz    | StringValue |

  Scenario: string double quoted
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = "bar something"
      withEscaping = "bar  \" baz"

      inner {
          foo = "bar something"
          withEscaping = "bar  \" baz"
      }

      noNeedForEscape = "bar  \\' baz"
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignments
      | index | path            | value         | type        |
      | 0     | foo             | bar something | StringValue |
      | 1     | withEscaping    | bar  " baz    | StringValue |
      | 3     | noNeedForEscape | bar  \' baz   | StringValue |
    And the model at "Root.fusion/inner[2]" must contain the path assignments
      | index | path         | value         | type        |
      | 0     | foo          | bar something | StringValue |
      | 1     | withEscaping | bar  " baz    | StringValue |

  Scenario: multiline strings
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo = "bar

      something that is multiline"
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignments
      | index | path | value                              | type        |
      | 0     | foo  | bar\n\nsomething that is multiline | StringValue |

  Scenario: last char in string is a slash (escaping of escape char)
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      kaspersObject.value = "The end of this line is a backslash\\"
      kaspersObject.bar = "Here comes \\ a backslash in the middle"
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignments
      | index | path                | value                                  | type        |
      | 0     | kaspersObject.value | The end of this line is a backslash\   | StringValue |
      | 1     | kaspersObject.bar   | Here comes \ a backslash in the middle | StringValue |

  Scenario Outline: special whitespace characters in strings
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      singleQuotes = '<char>'
      doubleQuotes = "<char>"
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain the path assignments
      | index | path         | value  | type        |
      | 0     | singleQuotes | <char> | StringValue |
      | 1     | doubleQuotes | <char> | StringValue |
    Examples:
      | char |
      | \n   |
      | \r   |
      | \t   |
