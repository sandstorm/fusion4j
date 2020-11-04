Feature: Fusion element load order

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "File1.fusion" contains the following code
      """fusion
      content = "file 1"
      """
    Given the Fusion file "File2.fusion" contains the following code
      """fusion
      content = "file 2"
      """
    Given the Fusion file "File3.fusion" contains the following code
      """fusion
      content = "file 3"
      """

  Scenario: code line based load order - includes are overridden - last code line wins
    Given the Fusion file "Root.fusion" contains the following code
        """fusion
        content = "overridden value"
        include: File1.fusion
        include: File2.fusion
        include: File3.fusion
        content = "effective value"
        """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "content" must be "effective value" with type "[STRING]"


  Scenario: file based load order - include file load order file 1
    Given the Fusion file "Root.fusion" contains the following code
        """fusion
        content = "overridden value"
        include: File2.fusion
        include: File3.fusion
        include: File1.fusion
        """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "content" must be "file 1" with type "[STRING]"

  Scenario: file based load order - include file load order file 2
    Given the Fusion file "Root.fusion" contains the following code
        """fusion
        content = "overridden value"
        include: File3.fusion
        include: File1.fusion
        include: File2.fusion
        """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "content" must be "file 2" with type "[STRING]"

  Scenario: file based load order - include file load order file 3
    Given the Fusion file "Root.fusion" contains the following code
        """
        content = "overridden value"
        include: File2.fusion
        include: File1.fusion
        include: File3.fusion
        """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "content" must be "file 3" with type "[STRING]"

  Scenario: file and code line based load order
    Given the Fusion file "Root.fusion" contains the following code
        """
        content = "overridden value"
        include: File2.fusion
        include: File3.fusion
        include: File1.fusion
        include: File4.fusion
        """
    Given the Fusion file "File4.fusion" contains the following code
      """
      content = "file 4 overridden value"
      content = "file 4 effective value"
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "content" must be "file 4 effective value" with type "[STRING]"
