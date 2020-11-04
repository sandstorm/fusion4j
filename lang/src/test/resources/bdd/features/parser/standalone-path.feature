Feature: parsing of standalone fusion path names

  Scenario: simple path name can be parsed
    When I parse the standalone Fusion path "myFusionPath"
    Then there must be no parse errors for standalone Fusion path "myFusionPath"
    And the parsed standalone Fusion path "myFusionPath" must have the following path segments
      | name         | type     | quoting   |
      | myFusionPath | Property | NO_QUOTES |

  Scenario: nested path name can be parsed
    When I parse the standalone Fusion path "my.fusion.Path"
    Then there must be no parse errors for standalone Fusion path "my.fusion.Path"
    And the parsed standalone Fusion path "my.fusion.Path" must have the following path segments
      | name   | type     | quoting   |
      | my     | Property | NO_QUOTES |
      | fusion | Property | NO_QUOTES |
      | Path   | Property | NO_QUOTES |

  Scenario: single quoted path name can be parsed
    When I parse the standalone Fusion path "'myFusi onPath'"
    Then there must be no parse errors for standalone Fusion path "'myFusi onPath'"
    And the parsed standalone Fusion path "'myFusi onPath'" must have the following path segments
      | name          | type     | quoting       |
      | myFusi onPath | Property | SINGLE_QUOTED |

  Scenario: single quoted nested path name can be parsed
    When I parse the standalone Fusion path "some.'myFusi onPath'.path"
    Then there must be no parse errors for standalone Fusion path "some.'myFusi onPath'.path"
    And the parsed standalone Fusion path "some.'myFusi onPath'.path" must have the following path segments
      | name          | type     | quoting       |
      | some          | Property | NO_QUOTES     |
      | myFusi onPath | Property | SINGLE_QUOTED |
      | path          | Property | NO_QUOTES     |

  Scenario: double quoted path name can be parsed
    When I parse the standalone Fusion path "\"myFusi onPath\""
    Then there must be no parse errors for standalone Fusion path "\"myFusi onPath\""
    And the parsed standalone Fusion path "\"myFusi onPath\"" must have the following path segments
      | name          | type     | quoting       |
      | myFusi onPath | Property | DOUBLE_QUOTED |

  Scenario: double quoted nested path name can be parsed
    When I parse the standalone Fusion path "some.\"myFusi onPath\".path"
    Then there must be no parse errors for standalone Fusion path "some.\"myFusi onPath\".path"
    And the parsed standalone Fusion path "some.\"myFusi onPath\".path" must have the following path segments
      | name          | type     | quoting       |
      | some          | Property | NO_QUOTES     |
      | myFusi onPath | Property | DOUBLE_QUOTED |
      | path          | Property | NO_QUOTES     |

  Scenario: meta property path name can be parsed
    When I parse the standalone Fusion path "@myFusionPath"
    Then there must be no parse errors for standalone Fusion path "@myFusionPath"
    And the parsed standalone Fusion path "@myFusionPath" must have the following path segments
      | name         | type | quoting   |
      | myFusionPath | Meta | NO_QUOTES |

  Scenario: nested meta property path name can be parsed
    When I parse the standalone Fusion path "my.@nestedFusion.path"
    Then there must be no parse errors for standalone Fusion path "my.@nestedFusion.path"
    And the parsed standalone Fusion path "my.@nestedFusion.path" must have the following path segments
      | name         | type     | quoting   |
      | my           | Property | NO_QUOTES |
      | nestedFusion | Meta     | NO_QUOTES |
      | path         | Property | NO_QUOTES |

  Scenario: complex Fusion path name can be parsed
    When I parse the standalone Fusion path "my.@nestedFusion.path.'withSingle'.\"and double quotes\".andSpecialChars.'öäüÖÄÜß!€@?§$%&\/()=*\"+#-.:,;'"
    Then there must be no parse errors for standalone Fusion path "my.@nestedFusion.path.'withSingle'.\"and double quotes\".andSpecialChars.'öäüÖÄÜß!€@?§$%&\/()=*\"+#-.:,;'"
    And the parsed standalone Fusion path "my.@nestedFusion.path.'withSingle'.\"and double quotes\".andSpecialChars.'öäüÖÄÜß!€@?§$%&\/()=*\"+#-.:,;'" must have the following path segments
      | name                          | type     | quoting       |
      | my                            | Property | NO_QUOTES     |
      | nestedFusion                  | Meta     | NO_QUOTES     |
      | path                          | Property | NO_QUOTES     |
      | withSingle                    | Property | SINGLE_QUOTED |
      | and double quotes             | Property | DOUBLE_QUOTED |
      | andSpecialChars               | Property | NO_QUOTES     |
      | öäüÖÄÜß!€@?§$%&\/()=*"+#-.:,; | Property | SINGLE_QUOTED |

