Feature: low level semantically correlation between code comments and elements

  The model provides basic functionality to access "correlated" code comments
  of other language elements. The sub list of code comment elements that appear
  directly before a non-comment element are considered "correlated".

  See also 'code-comments.feature' for regular comment syntax features.

  Background:
    Given the package "MyTestPackage" with entrypoint "Root.fusion"

  Scenario: most simple code comment correlation
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // some correlated comment
      foo = 'bar'
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 2 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
    And the element at "Root.fusion" and index 1 has one correlated comment
    And the element at "Root.fusion" and index 1 has the correlated comment "// some correlated comment" with index 0

  Scenario: code comment correlation on root code layer
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // [0] some correlated comment
      foo.bar = 'bar'

      // [2] correlated path1 1
      // correlated path1 2
      path1 = null

      // [5] correlated path2 1 - single
      /*
      correlated path2 2 - multi
      */
      path2 = null


      /* [8]
      correlated path3 1 - multi
      */
      /*
      correlated path3 2 - multi
      */
      path3 = null

      /**** [11]
      mixed example
       - path4 1 - multi
      */
      // correlated path4 2 - single
      /*
      correlated path4 3 - multi
      */

      // correlated path4 4 - single
      path4 = false

      // [16] not correlated
      /* [17]
        not correlated at all
      */

      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 18 elements
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
      | 10    | FusionPathAssignment |
      | 11    | CodeComment          |
      | 12    | CodeComment          |
      | 13    | CodeComment          |
      | 14    | CodeComment          |
      | 15    | FusionPathAssignment |
      | 16    | CodeComment          |
      | 17    | CodeComment          |
    And the element at "Root.fusion" and index 1 has one correlated comment
    And the element at "Root.fusion" and index 1 has the correlated comment "// [0] some correlated comment" with index 0
    And the element at "Root.fusion" and index 4 has 2 correlated comments
    And the element at "Root.fusion" and index 4 has the correlated comment "// [2] correlated path1 1" with index 2
    And the element at "Root.fusion" and index 4 has the correlated comment "// correlated path1 2" with index 3
    And the element at "Root.fusion" and index 7 has 2 correlated comments
    And the element at "Root.fusion" and index 7 has the correlated comment "// [5] correlated path2 1 - single" with index 5
    And the element at "Root.fusion" and index 7 has the following correlated comment with index 6
      """
      /*
      correlated path2 2 - multi
      */
      """
    And the element at "Root.fusion" and index 10 has 2 correlated comments
    And the element at "Root.fusion" and index 10 has the following correlated comment with index 8
      """
      /* [8]
      correlated path3 1 - multi
      */
      """
    And the element at "Root.fusion" and index 10 has the following correlated comment with index 9
      """
      /*
      correlated path3 2 - multi
      */
      """
    And the element at "Root.fusion" and index 15 has 4 correlated comments
    And the element at "Root.fusion" and index 15 has the following correlated comment with index 11
      """
      /**** [11]
      mixed example
       - path4 1 - multi
      */
      """
    And the element at "Root.fusion" and index 15 has the correlated comment "// correlated path4 2 - single" with index 12
    And the element at "Root.fusion" and index 15 has the following correlated comment with index 13
      """
      /*
      correlated path4 3 - multi
      */
      """
    And the element at "Root.fusion" and index 15 has the correlated comment "// correlated path4 4 - single" with index 14

  Scenario: code comment correlation in path configuration block
    Given the Fusion file "Root.fusion" contains the following code
      """fusion
      // my outer config
      myConfig {
          // [0] some correlated comment
          foo.bar = 'bar'

          // [2] correlated path1 1
          // correlated path1 2
          path1 = null

          // [5] correlated path2 1 - single
          /*
          correlated path2 2 - multi
          */
          path2 = null


          /* [8]
          correlated path3 1 - multi
          */
          /*
          correlated path3 2 - multi
          */
          path3 = null

          /**** [11]
          mixed example
           - path4 1 - multi
          */
          // correlated path4 2 - single
          /*
          correlated path4 3 - multi
          */

          // correlated path4 4 - single
          path4 = false

          // [16] not correlated
          /* [17]
            not correlated at all
          */
      }

      // [2] not correlated
      /* [3]
        not correlated at all
      */
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 4 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                    |
      | 0     | CodeComment             |
      | 1     | FusionPathConfiguration |
      | 2     | CodeComment             |
      | 3     | CodeComment             |
    And the element at "Root.fusion" and index 1 has one correlated comment
    And the element at "Root.fusion" and index 1 has the correlated comment "// my outer config" with index 0
    And the model at "Root.fusion/myConfig[1]" must contain 18 elements
    And the model at "Root.fusion/myConfig[1]" must contain the elements
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
      | 10    | FusionPathAssignment |
      | 11    | CodeComment          |
      | 12    | CodeComment          |
      | 13    | CodeComment          |
      | 14    | CodeComment          |
      | 15    | FusionPathAssignment |
      | 16    | CodeComment          |
      | 17    | CodeComment          |
    And the element at "Root.fusion/myConfig[1]" and index 1 has one correlated comment
    And the element at "Root.fusion/myConfig[1]" and index 1 has the correlated comment "// [0] some correlated comment" with index 0
    And the element at "Root.fusion/myConfig[1]" and index 4 has 2 correlated comments
    And the element at "Root.fusion/myConfig[1]" and index 4 has the correlated comment "// [2] correlated path1 1" with index 2
    And the element at "Root.fusion/myConfig[1]" and index 4 has the correlated comment "// correlated path1 2" with index 3
    And the element at "Root.fusion/myConfig[1]" and index 7 has 2 correlated comments
    And the element at "Root.fusion/myConfig[1]" and index 7 has the correlated comment "// [5] correlated path2 1 - single" with index 5
    And the element at "Root.fusion/myConfig[1]" and index 7 has the following correlated comment with index 6
      """
          /*
          correlated path2 2 - multi
          */
      """
    And the element at "Root.fusion/myConfig[1]" and index 10 has 2 correlated comments
    And the element at "Root.fusion/myConfig[1]" and index 10 has the following correlated comment with index 8
      """
          /* [8]
          correlated path3 1 - multi
          */
      """
    And the element at "Root.fusion/myConfig[1]" and index 10 has the following correlated comment with index 9
      """
          /*
          correlated path3 2 - multi
          */
      """
    And the element at "Root.fusion/myConfig[1]" and index 15 has 4 correlated comments
    And the element at "Root.fusion/myConfig[1]" and index 15 has the following correlated comment with index 11
      """
          /**** [11]
          mixed example
           - path4 1 - multi
          */
      """
    And the element at "Root.fusion/myConfig[1]" and index 15 has the correlated comment "// correlated path4 2 - single" with index 12
    And the element at "Root.fusion/myConfig[1]" and index 15 has the following correlated comment with index 13
      """
          /*
          correlated path4 3 - multi
          */
      """
    And the element at "Root.fusion/myConfig[1]" and index 15 has the correlated comment "// correlated path4 4 - single" with index 14

  Scenario: code comment correlation in fusion object assignment body
    Given the Fusion file "Root.fusion" contains the following code
      """
      // my outer assignment
      myFusionObject = Some.Commented:FusionObject {
          // [0] some correlated comment
          foo.bar = 'bar'

          // [2] correlated path1 1
          // correlated path1 2
          path1 = null

          // [5] correlated path2 1 - single
          /*
          correlated path2 2 - multi
          */
          path2 = null


          /* [8]
          correlated path3 1 - multi
          */
          /*
          correlated path3 2 - multi
          */
          path3 = null

          /**** [11]
          mixed example
           - path4 1 - multi
          */
          // correlated path4 2 - single
          /*
          correlated path4 3 - multi
          */

          // correlated path4 4 - single
          path4 = false

          // [16] not correlated
          /* [17]
            not correlated at all
          */
      }

      // [2] not correlated
      /* [3]
        not correlated at all
      */
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 4 elements
    And the model at "Root.fusion" must contain the elements
      | index | type                 |
      | 0     | CodeComment          |
      | 1     | FusionPathAssignment |
      | 2     | CodeComment          |
      | 3     | CodeComment          |
    And the element at "Root.fusion" and index 1 has one correlated comment
    And the element at "Root.fusion" and index 1 has the correlated comment "// my outer assignment" with index 0
    And the model at "Root.fusion/myFusionObject[1]" must contain 18 elements
    And the model at "Root.fusion/myFusionObject[1]" must contain the elements
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
      | 10    | FusionPathAssignment |
      | 11    | CodeComment          |
      | 12    | CodeComment          |
      | 13    | CodeComment          |
      | 14    | CodeComment          |
      | 15    | FusionPathAssignment |
      | 16    | CodeComment          |
      | 17    | CodeComment          |
    And the element at "Root.fusion/myFusionObject[1]" and index 1 has one correlated comment
    And the element at "Root.fusion/myFusionObject[1]" and index 1 has the correlated comment "// [0] some correlated comment" with index 0
    And the element at "Root.fusion/myFusionObject[1]" and index 4 has 2 correlated comments
    And the element at "Root.fusion/myFusionObject[1]" and index 4 has the correlated comment "// [2] correlated path1 1" with index 2
    And the element at "Root.fusion/myFusionObject[1]" and index 4 has the correlated comment "// correlated path1 2" with index 3
    And the element at "Root.fusion/myFusionObject[1]" and index 7 has 2 correlated comments
    And the element at "Root.fusion/myFusionObject[1]" and index 7 has the correlated comment "// [5] correlated path2 1 - single" with index 5
    And the element at "Root.fusion/myFusionObject[1]" and index 7 has the following correlated comment with index 6
      """
          /*
          correlated path2 2 - multi
          */
      """
    And the element at "Root.fusion/myFusionObject[1]" and index 10 has 2 correlated comments
    And the element at "Root.fusion/myFusionObject[1]" and index 10 has the following correlated comment with index 8
      """
          /* [8]
          correlated path3 1 - multi
          */
      """
    And the element at "Root.fusion/myFusionObject[1]" and index 10 has the following correlated comment with index 9
      """
          /*
          correlated path3 2 - multi
          */
      """
    And the element at "Root.fusion/myFusionObject[1]" and index 15 has 4 correlated comments
    And the element at "Root.fusion/myFusionObject[1]" and index 15 has the following correlated comment with index 11
      """
          /**** [11]
          mixed example
           - path4 1 - multi
          */
      """
    And the element at "Root.fusion/myFusionObject[1]" and index 15 has the correlated comment "// correlated path4 2 - single" with index 12
    And the element at "Root.fusion/myFusionObject[1]" and index 15 has the following correlated comment with index 13
      """
          /*
          correlated path4 3 - multi
          */
      """
    And the element at "Root.fusion/myFusionObject[1]" and index 15 has the correlated comment "// correlated path4 4 - single" with index 14

  Scenario: code comment correlation in root prototype declaration body
    Given the Fusion file "Root.fusion" contains the following code
      """
      /**
       * my documented prototype declaration
       *
       * @api path1 String?
       *        description of the path1 api path
       * @api path2 Boolean
       *        description of the path2 api path
       */
      prototype(Some.Commented:FusionObject) < prototype(Inherit.Base:FusionObject) {
          // [0] some correlated comment
          foo.bar = 'bar'

          // [2] correlated path1 1
          // correlated path1 2
          path1 = null

          // [5] correlated path2 1 - single
          /*
          correlated path2 2 - multi
          */
          path2 = null


          /* [8]
          correlated path3 1 - multi
          */
          /*
          correlated path3 2 - multi
          */
          path3 = null

          /**** [11]
          mixed example
           - path4 1 - multi
          */
          // correlated path4 2 - single
          /*
          correlated path4 3 - multi
          */

          // correlated path4 4 - single
          path4 = false

          // [16] not correlated
          /* [17]
            not correlated at all
          */
      }

      // [2] not correlated
      /* [3]
        not correlated at all
      */
      """
    When all Fusion packages are parsed
    Then there must be no parse errors in file "Root.fusion"
    And the model at "Root.fusion" must contain 4 elements
    And the model at "Root.fusion" must contain the elements
      | index | type        |
      | 0     | CodeComment |
      | 1     | Prototype   |
      | 2     | CodeComment |
      | 3     | CodeComment |
    And the element at "Root.fusion" and index 1 has one correlated comment
    And the element at "Root.fusion" and index 1 has the following correlated comment with index 0
      """
      /**
       * my documented prototype declaration
       *
       * @api path1 String?
       *        description of the path1 api path
       * @api path2 Boolean
       *        description of the path2 api path
       */
      """
    And the model at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" must contain 18 elements
    And the model at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" must contain the elements
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
      | 10    | FusionPathAssignment |
      | 11    | CodeComment          |
      | 12    | CodeComment          |
      | 13    | CodeComment          |
      | 14    | CodeComment          |
      | 15    | FusionPathAssignment |
      | 16    | CodeComment          |
      | 17    | CodeComment          |
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 1 has one correlated comment
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 1 has the correlated comment "// [0] some correlated comment" with index 0
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 4 has 2 correlated comments
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 4 has the correlated comment "// [2] correlated path1 1" with index 2
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 4 has the correlated comment "// correlated path1 2" with index 3
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 7 has 2 correlated comments
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 7 has the correlated comment "// [5] correlated path2 1 - single" with index 5
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 7 has the following correlated comment with index 6
      """
          /*
          correlated path2 2 - multi
          */
      """
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 10 has 2 correlated comments
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 10 has the following correlated comment with index 8
      """
          /* [8]
          correlated path3 1 - multi
          */
      """
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 10 has the following correlated comment with index 9
      """
          /*
          correlated path3 2 - multi
          */
      """
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 15 has 4 correlated comments
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 15 has the following correlated comment with index 11
      """
          /**** [11]
          mixed example
           - path4 1 - multi
          */
      """
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 15 has the correlated comment "// correlated path4 2 - single" with index 12
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 15 has the following correlated comment with index 13
      """
          /*
          correlated path4 3 - multi
          */
      """
    And the element at "Root.fusion/prototype(Some.Commented:FusionObject)[1]" and index 15 has the correlated comment "// correlated path4 4 - single" with index 14

