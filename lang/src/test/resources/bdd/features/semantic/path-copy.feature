Feature: effective Fusion value of a path

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"
    Given the Fusion file "Base.fusion" contains the following code
      """fusion
      my {
        config {
          value = 'value from base'
        }
      }
      """

  Scenario: path copy of direct values from config blocks
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      my.config.value = 'value from root'
      include: Base.fusion
      // direct value copy
      evaluate.me < my.config.value
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "evaluate.me" must be "value from base" with type "[STRING]"

  Scenario: path copy of direct values from assignments
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      foo.bar = 'copy value'
      // direct value copy
      evaluate.me < foo.bar
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "evaluate.me" must be "copy value" with type "[STRING]"

  Scenario: path copy of nested configurations from blocks
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      some {
        config {
          value = 'value from config'
        }
      }
      evaluate < some
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "evaluate.config.value" must be "value from config" with type "[STRING]"

  Scenario: multiple path copy merging configurations from blocks
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      base {
        config {
          value = 'value from base config'
        }
      }
      some {
        config {
          foo = 'foo'
          value = 'overridden in some'
        }
      }
      other {
        config {
          bar = 'bar'
        }
      }
      evaluate < base
      evaluate < some
      evaluate < other
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "evaluate.config.value" must be "overridden in some" with type "[STRING]"
    Then the effective Fusion value of path "evaluate.config.foo" must be "foo" with type "[STRING]"
    Then the effective Fusion value of path "evaluate.config.bar" must be "bar" with type "[STRING]"

  Scenario: nested path copy with different nesting levels
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      some {
        deepConfig {
          me = 'copy value'
        }
      }

      evaluate {
        me = 'original value'
      }

      // nested config block merge copy
      evaluate < some.deepConfig
      """
    When all Fusion packages are parsed
    And all Fusion files are indexed without errors
    Then the effective Fusion value of path "evaluate.me" must be "copy value" with type "[STRING]"
