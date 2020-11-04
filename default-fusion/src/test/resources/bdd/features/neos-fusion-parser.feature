Feature: parsing of the Neos.Fusion package code

  Scenario: parsing of Neos.Fusion should be successful
    Given the default Fusion package "Neos.Fusion" is registered
    When all Fusion packages are parsed
    Then there must be no parse errors
    And there must be 5 successfully parsed Fusion files

  Scenario: parsing of Neos.Neos should be successful
    Given the default Fusion package "Neos.Neos" is registered
    When all Fusion packages are parsed
    Then there must be no parse errors
    And there must be 29 successfully parsed Fusion files

  Scenario: parsing of Neos.NodeTypes should be successful
    Given the default Fusion package "Neos.NodeTypes" is registered
    When all Fusion packages are parsed
    Then there must be no parse errors
    And there must be 6 successfully parsed Fusion files
