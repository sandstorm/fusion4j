# fusion4j - declarative rendering language for the JVM based on Neos.Fusion

Supports the Neos Fusion syntax/semantic as
described [in the official Neos Fusion documentation](https://docs.neos.io/cms/manual/rendering/fusion).

- full Fusion language parser with out-of-the-box [AFX](https://github.com/neos/fusion-afx) support
- Fusion runtime for the JVM usable as standalone template engine
- default Fusion objects from Neos.Fusion
- written in Kotlin
- functional, library styled API - modules are usable standalone
- antlr parser grammar for Fusion / AFX
- runtime and parser fully stateless, immutable and thread-safe
- runtime evaluation happens lazy everywhere
- works great with the IntelliJ [Neos-Support Plugin](https://plugins.jetbrains.com/plugin/9362-neos-support) (except jump-to-class)
- coming soon: Spring boot integration
- coming soon: a [Monocle-like](https://github.com/sitegeist/Sitegeist.Monocle) Fusion styleguide for presentational components (also serves as Spring boot example / how to use)

Check out the BDD feature tests for:

- [lang module](/lang/src/test/resources/bdd/features)
- [runtime module](/runtime/src/test/resources/bdd/features)
- [default Fusion module](/default-fusion/src/test/resources/bdd/features)

## Spring integration

Currently, I develop the Spring Boot integration inside the styleguide sub-package before generalizing it.

Features:
- FusionView for Fusion runtime delegation of Spring view rendering
- Immutable runtime container for PROD mode (loaded once at startup)
- Reloadable runtime container for local dev mode (with hot-Fusion-code-reloading on file changes)
- runtime configuration integration via externalized properties
- CDI integration: custom EEL Helpers and Fusion Object Implementation as Spring beans
- Fusion object for HTTP Response

This is currently WIP, so there is no doc for now :/ but check out: 
- an [example application.yml](/styleguide/src/main/resources/application.yml)
- an [example controller]()

## Info on development state

I consider the current development state *early alpha*. Expect some missing edge-case features, 
bugs and performance issues. Pls report, I need input ;)

The basic functionality of the Fusion runtime is mainly done. The parser already supports all Fusion syntax features.
The lang module contains most logic to implement the Fusion semantics. Only a small part is actually implemented in the
runtime itself.

Missing features that I'm aware of:

- Bugfix: PositionalArraySorter -> keep track of declared key order instead of alphanumeric key sorting!
- `@position` sorting in some places: (inside `@context`, `@if`) - WIP
- Neos.Fusion:Augmenter basic Fusion Object
- include ant paths, for now just `*` (same folder) and `**` (include all sub-folders) works
- caching
- configurable processing of AFX HTML comments / style tags / script tags -> parsing already done

I actively develop this package, feel free to contact me and/or contribute ;)

Things to validate/finish:

- multiline EEL expression tests / error message with offending symbol

## Use-Cases

- use the Neos Fusion declarative rendering approach as standalone template engine in your JVM application!
    - structured & testable rendering components
    - no more messy template includes ;)
- write awesome code analysis based tools for the Fusion language, e.g.
    - linters and validators
    - performance/complexity analysis tools
    - pre-compilers
    - documentation tools
- re-using rendering logic / template integration in a two-stack CMS approach
    - with Neos as CMS content editing stack (providing both data and template), and
    - JVM based applications as delivery stack
    - "single source of template"

### two stack CMS with Neos

There are several reasons for architectures, where the PHP / Neos CMS is *not* front-facing but used as an internal
content editing tool. Content is instead delivered by more "enterprisy" technology like JVM web server frameworks (f.e.
Spring Boot). An architecture approach to separate those concerns is sometimes called "two-stack CMS". One stack
provides an editing platform
(Neos CMS) and the other stack delivers a somehow statically released content snapshot. Those two stacks may run in
separate networks, where the delivery stack focuses on security, reliability, performance and scalability. The editing
stack may not have such high availability requirements but focuses more on the editing experience. That's probably why
you chose Neos in the first place ;) <3

TODO write concept on how to share templates between two-stacks

## Usage

### include via Maven / Gradle

TODO when released for now compile locally and write tests for contribution :*

### code examples

TODO - I will complete this when the API is more stable.
For now, checkout the BDD Steps

- [parser usage](/test-utils/src/main/kotlin/io/neos/fusion4j/test/bdd/steps/FusionParserSteps.kt)
- [runtime usage](/test-utils/src/main/kotlin/io/neos/fusion4j/test/bdd/steps/FusionRuntimeSteps.kt)

or the Spring integration

- [runtime factory](/styleguide/src/main/kotlin/io/neos/fusion4j/spring/FusionRuntimeFactory.kt)

## Modules

All parts of fusion4j are designed to give you the experience of *using a library* instead of being caged in a
framework. There is some code to be written to get started with fusion4j (see usage), but you can use the single aspects
of this library pretty easily in a standalone way. F.e. you can only parse Fusion code and perform custom logic like
analysing it.

Note, that if you want to reuse the BDD step definitions, they are found

### Language module

This is implemented in the `lang` sub-package.

- grammar definition of Fusion in antlr4
- validated, consistent and immutable domain model
    - raw language meta model
        - parsing of Fusion code into a usable raw AST-like, but Fusion-domain-specific model
        - AST code references
        - code comments are part of the raw meta-model!
        - code comments to fusion element correlation (e.g. for code analysis or a "Fusion-Doc")
    - semantic meta-model / normalization of loaded Fusion files
        - merging of prototype declarations
        - merging of path configurations
        - nested path normalization
        - application of path erasures
        - file includes / load order
        - loading of Fusion object instances
- DSL support
    - domain specific language parser API
    - default DSLs:
        - AFX
- Fusion file abstraction
    - filesystem
    - classpath
    - in-memory (String)
- Reloading Fusion code at runtime is possible by creating a new instance of the Runtime (it's immutable)

### Runtime module (WIP)

This is implemented in the `runtime` sub-package.

- FusionRuntime implemented for usage in Java
- immutable, thread-safe Runtime
- almost everything is evaluated *lazy*
- TODO/DISCUSS: reactive implementation for reactor / kotlin coroutines
- IMPORTANT: for now, the runtime is blocking code. If you use Spring Webflux or any other reactive / non-blocking
  technology, you should load your model first, then use the Fusion Runtime as blocking mapper. -> for now there is no
  way to use non-blocking code in Fusion Object implementations or EEL Helpers!

Important classes (also entry points for class documentation):

- FusionRuntimeImplementationAccess
- FusionRuntime

### Default Fusion module (WIP)

Basic Fusion library - prototypes + implementations of Neos.Fusion

Implemented Fusion prototypes:
- Neos.Fusion:Join
- Neos.Fusion:DataStructure
- Neos.Fusion:Case
- Neos.Fusion:Matcher
- Neos.Fusion:Renderer
- Neos.Fusion:Value
- Neos.Fusion:Component
- Neos.Fusion:CanRender
- Neos.Fusion:Tag
- Neos.Fusion:Map
- Neos.Fusion:Loop
- Neos.Fusion:Reduce
- Neos.Fusion:Augmenter

Deprecated Fusion objects (will throw an exception on usage with alternative message):
- Neos.Fusion:Array
- Neos.Fusion:RawArray
- Neos.Fusion:Template
- Neos.Fusion:Collection
- Neos.Fusion:RawCollection
- Neos.Fusion:Attributes

Unsupported Fusion objects (will throw an exception on usage with explanation message):
- Neos.Fusion:Http.ResponseHead
- Neos.Fusion:Http.Message
- Neos.Fusion:UriBuilder
- Neos.Fusion:ResourceUri
- Neos.Fusion:Link.Resource

## development

generate antlr code with:
`gradlew clean generateGrammarSource`

run all tests
`gradlew clean test`

run styleguide css resources sass compiler
```
cd styleguide
npm run sass-dev
```

## further ideas

- (TODO) code analysis API
    - possible use cases: validators / linters, performance analysis, documentation
    - API for analysing fusion code on both:
        - raw lang model
        - normalized meta model
    - (TODO / Discuss / Ideas welcome) default analysis for:
        - unused/unreachable code
        - raw to normalized "declaration application path" debugging (a.k.a. "which code overrides my root path"?)
        - recognition of possible runtime errors / pre-compile-like validations on normalized model, e.g.
            - type checks, e.g.
                - warnings on type morphing
                - render evaluation result type check ()
            - no implementation class set for prototypes
        - side effect detection (a.k.a. "is my component side effect free"?), looks for:
            - context mutations via @context meta property
            - global state access via expression analysis

discuss: will there be a reactive/non-blocking IO implementation? do we need one? For now, if you want to use fusion4j
with reactor/Spring Boot Webflux or another non-blocking IO library, the rendering logic (more specific: the Fusion
object implementations or EEL helper calls) should not perform any blocking IO code. To work around this, perform all
IO operations before hands (in the controller) and put the emitted object in the Fusion context before rendering.
