Feature: path copy evaluation

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: path copy
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      my {
        config {
          value = 'copy value'
        }
      }

      evaluate {
        me = 'original value'
      }

      // direct value copy
      evaluate.me < my.config.value
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "evaluate.me"
    Then the evaluated output for path "evaluate.me" must be of type "java.lang.String"
    And the evaluated output for path "evaluate.me" must be
    """
    copy value
    """

  Scenario: path copy nested
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      my {
        config {
          me = 'copy value'
        }
      }

      evaluate {
        me = 'original value'
      }

      // nested config block merge copy
      evaluate < my.config
      """
    Given all Fusion packages are parsed
    And a Fusion runtime
    When I evaluate the Fusion path "evaluate.me"
    Then the evaluated output for path "evaluate.me" must be of type "java.lang.String"
    And the evaluated output for path "evaluate.me" must be
    """
    copy value
    """