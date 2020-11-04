Feature: raw 'lang' model parsing for fusion file includes

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: file includes
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // regular pattern include
      include: "*/**"
      include: '*/**'
      include: */**

      // include with some whitespace noise
      include   : 	    "*/**"

      // include file
      include: "something.fusion"
      include: something.fusion

      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 6 file includes
    And the file include at "Root.fusion/[1]" must have the pattern "*/**"
    And the file include at "Root.fusion/[2]" must have the pattern "*/**"
    And the file include at "Root.fusion/[3]" must have the pattern "*/**"
    And the file include at "Root.fusion/[5]" must have the pattern "*/**"
    And the file include at "Root.fusion/[7]" must have the pattern "something.fusion"
    And the file include at "Root.fusion/[8]" must have the pattern "something.fusion"

  Scenario: file include - invalid pattern
    Given the Fusion file "Root.fusion" contains the following code
      """
      include: "whitespace invalid"
      """
    When all Fusion packages are parsed
    Then there must be the following parse errors in file "Root.fusion"
      | line | char | message                                         | text  | offendingLine | offendingChar |
      | 1    | 29   | file include pattern does not allow whitespaces | <EOF> | 1             | 29            |
