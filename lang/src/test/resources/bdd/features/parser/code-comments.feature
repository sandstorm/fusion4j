Feature: raw 'lang' model parsing for fusion code comments

  This language implementation keeps the code comments in its raw meta model
  for later analysis.

  Code comments can be one of two types:

  1. single line comments, syntax:
  '//' [CODE_COMMENT_NO_BR] NEWLINE

  2. multiline comments, syntax:
  '/*'
  [CODE_COMMENT_WITH_BR]
  '*/'

  See also 'code-comment-correlation.feature' for code comment to element correlation test scenarios.

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: most simple single line code comment
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // some comment
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 code comments
    And the model at "Root.fusion" must contain the code comment "// some comment" at index 0

  Scenario: most simple multiline code comment
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      /*
       some multiline comment
       */
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 code comments
    And the model at "Root.fusion" must contain the following code comment at index 0
      """fusion
      /*
       some multiline comment
       */
      """

  Scenario: root code comments
    Given the Fusion file "Root.fusion" contains the following code
        """fusion
        // some path
        path = 'foo'

        /*
          multi
          line
          comment
         */
        // and single line
        path2 = null

        // two single line comments
        // in a row

        path3 = false

        /*
         two
         multiline comments
         */

        /*
         in a row
         */
        """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 10 elements
    And the model at "Root.fusion" must contain 7 code comments
    And the model at "Root.fusion" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
      | 2     | CodeComment          |
      | 3     | CodeComment          |
      | 4     | FusionPathAssignment |
      | 5     | CodeComment          |
      | 6     | CodeComment          |
      | 7     | FusionPathAssignment |
      | 8     | CodeComment          |
      | 9     | CodeComment          |
    And the model at "Root.fusion" must contain the code comment "// some path" at index 0
    And the model at "Root.fusion" must contain the following code comment at index 2
      """fusion
      /*
        multi
        line
        comment
       */
      """
    And the model at "Root.fusion" must contain the code comment "// and single line" at index 3
    And the model at "Root.fusion" must contain the code comment "// two single line comments" at index 5
    And the model at "Root.fusion" must contain the code comment "// in a row" at index 6
    And the model at "Root.fusion" must contain the following code comment at index 8
      """fusion
      /*
       two
       multiline comments
       */
      """
    And the model at "Root.fusion" must contain the following code comment at index 9
      """fusion
      /*
       in a row
       */
      """

  Scenario: code comments inside of a path configuration block
    Given the Fusion file "Root.fusion" contains the following code
        """
        outer.@metaPath.prototype(Foo.Bar).inner {
            // some path
            path = 'foo'

            /*
              multi
              line
              comment
             */
            // and single line
            path2 = null

            // two single line comments
            // in a row

            path3 = false

            /*
             two
             multiline comments
             */

            /*
             in a row
             */
        }
        """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 elements
    And the element at "Root.fusion" and index 0 must be of type "FusionPathConfiguration"
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain 10 elements
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain 7 code comments
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
      | 2     | CodeComment          |
      | 3     | CodeComment          |
      | 4     | FusionPathAssignment |
      | 5     | CodeComment          |
      | 6     | CodeComment          |
      | 7     | FusionPathAssignment |
      | 8     | CodeComment          |
      | 9     | CodeComment          |
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// some path" at index 0
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 2
      """
          /*
            multi
            line
            comment
           */
      """
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// and single line" at index 3
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// two single line comments" at index 5
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// in a row" at index 6
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 8
      """
          /*
           two
           multiline comments
           */
      """
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 9
      """
          /*
           in a row
           */
      """

  Scenario: code comments inside of a fusion object assignment body
    Given the Fusion file "Root.fusion" contains the following code
        """
        outer.@metaPath.prototype(Foo.Bar).inner = Some.Cool:FusionObject {
            // some path
            path = 'foo'

            /*
              multi
              line
              comment
             */
            // and single line
            path2 = null

            // two single line comments
            // in a row

            path3 = false

            /*
             two
             multiline comments
             */

            /*
             in a row
             */
        }
        """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 elements
    And the element at "Root.fusion" and index 0 must be of type "FusionPathAssignment"
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain 10 elements
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain 7 code comments
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
      | 2     | CodeComment          |
      | 3     | CodeComment          |
      | 4     | FusionPathAssignment |
      | 5     | CodeComment          |
      | 6     | CodeComment          |
      | 7     | FusionPathAssignment |
      | 8     | CodeComment          |
      | 9     | CodeComment          |
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// some path" at index 0
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 2
      """
          /*
            multi
            line
            comment
           */
      """
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// and single line" at index 3
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// two single line comments" at index 5
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the code comment "// in a row" at index 6
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 8
      """
          /*
           two
           multiline comments
           */
      """
    And the model at "Root.fusion/outer.@metaPath.prototype(Foo.Bar).inner[0]" must contain the following code comment at index 9
      """
          /*
           in a row
           */
      """

  Scenario: code comments inside of a root prototype declaration body
    Given the Fusion file "Root.fusion" contains the following code
        """
        prototype(Some.Cool:FusionObject) < prototype(Some.Base:Object) {
            // some path
            path = 'foo'

            /*
              multi
              line
              comment
             */
            // and single line
            path2 = null

            // two single line comments
            // in a row

            path3 = false

            /*
             two
             multiline comments
             */

            /*
             in a row
             */
        }
        """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 1 elements
    And the model at "Root.fusion" must contain the root prototype declarations
      | index | name                   | inherit          |
      | 0     | Some.Cool:FusionObject | Some.Base:Object |
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain 10 elements
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain 7 code comments
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
      | 2     | CodeComment          |
      | 3     | CodeComment          |
      | 4     | FusionPathAssignment |
      | 5     | CodeComment          |
      | 6     | CodeComment          |
      | 7     | FusionPathAssignment |
      | 8     | CodeComment          |
      | 9     | CodeComment          |
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the code comment "// some path" at index 0
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the following code comment at index 2
      """
          /*
            multi
            line
            comment
           */
      """
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the code comment "// and single line" at index 3
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the code comment "// two single line comments" at index 5
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the code comment "// in a row" at index 6
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the following code comment at index 8
      """
          /*
           two
           multiline comments
           */
      """
    And the model at "Root.fusion/prototype(Some.Cool:FusionObject)[0]" must contain the following code comment at index 9
      """
          /*
           in a row
           */
      """

