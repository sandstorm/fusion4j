Feature: raw 'lang' model parsing for fusion namespace aliases

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: namespace alias
    Given the Fusion file "Root.fusion" contains the following code
      # FIXME namespace alias is buggy in Neos IntelliJ plugin
      #"""fusion
      """

      // simple namespace alias
      namespace: Foo = Bar

      // nested values
      namespace: Foo.Bar = Some.Longer.Namespace.That.I.Dont.Want.To.Repeat
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 namespace aliases
    And the namespace alias at "Root.fusion/[1]" must declare "Foo" as alias for "Bar"
    And the namespace alias at "Root.fusion/[3]" must declare "Foo.Bar" as alias for "Some.Longer.Namespace.That.I.Dont.Want.To.Repeat"

  Scenario: namespace alias - invalid namespace names
    Given the Fusion file "Root.fusion" contains the following code
      # FIXME namespace alias is buggy in Neos IntelliJ plugin
      #"""fusion
      """
      namespace: Foo:Bar = Hallo
      """
    When all Fusion packages are parsed
    Then there must be the following parse errors in file "Root.fusion"
      | line | char | message                              | text | offendingLine | offendingChar |
      | 1    | 15   | extraneous input 'Bar' expecting '=' | Bar  | 1             | 15            |
