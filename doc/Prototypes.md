# Prototypes and Fusion Object Instances

Fusion is a declarative language using tree-structured paths. The Fusion Prototype system provides a way to **reuse,
structure and name** sets of path declarations.

One of the main features of Prototypes and their instances is the delegation of output evaluation to the underlying
programming language at runtime. This is one of the possible ways to achieve dynamic output rendering
(besides EEL Expressions and runtime DSLs).

## Prototypes

The Fusion syntax provides a keyword for declaring a prototype: `prototype(<QualifiedPrototypeName>)`
Valid qualified prototype name:

```
[ <Namespace> ':' ] <PrototypeName>
```

Where `<Namespace>` and `<PrototypeName>` may contain only `0-1 a-z A-Z .`
The `.` is used for structuring. A common prototype name looks like this: `Vendor.Package:Folder.Prototype`
The namespace including the colon `:` is optional.

Example:

```neosfusion
prototype(Vendor.Package:Folder.Prototype) {
    // body
}
```

The evaluation is delegated to a class that is specified via the nested prototype path `@class`. Example:

```neosfusion
// PHP Fusion
prototype(Vendor.Package:Folder.Prototype) {
    @class = 'Vendor\\Package\\FusionObjects\\MyFusionObjectImplementation'
}
// fusion4j
prototype(Vendor.Package:Folder.Prototype) {
    @class = 'org.vendor.project.fusion.impl.MyFusionObjectImplementation'
}
```

Note, that evaluating a Fusion Object Instance at runtime **requires a delegation class**. A respective error is thrown
if you missed declaring a class or inheriting from a prototype that declares a class.

The `@class` attribute is a public API for the Fusion Runtime and can be used to define custom Fusion Object
implementations. Personal Opinion: For most cases **the default Fusion prototypes contained in the `Neos.Fusion` package
should be sufficient**. Before writing a custom Fusion object implementation, you may rather consider using an Eel
expression / EelHelper wrapped in default Fusion. Custom implementations should be the "last resort" for dynamic
rendering. Most rendering logic is notable using Fusion itself.

### nested Prototype paths and attributes

Nested paths can be declared in the prototype body or via nested prototype path. Direct child paths of prototypes are
also called "prototype attributes". Like regular Fusion paths, nested paths support all operations (assignment,
configuration, erasure and copy)
and will merge via element and load order if declared more than once. Personal opinion: I would recommend *not to use
path copy* inside nested prototype paths (or at all) but use prototypes instead for reusing and structuring
declarations.

Example:

```neosfusion
// ----- inside of body
prototype(Vendor.Package:Folder.Prototype) {
    foo = 123
    hello = 'world'
    nested {
        path = true
    }
    more.nested.paths = ${1 + 1}
    iAmDeclaredTwice = 1
    
    erasedPath >
}

// ----- via nested path
prototype(Vendor.Package:Folder.Prototype).bar = 345
prototype(Vendor.Package:Folder.Prototype).nested.mergedPath = false
// higher element order, wins over declaration above
prototype(Vendor.Package:Folder.Prototype).iAmDeclaredTwice = 2
```

As explained in [Fusion paths documentation](FusionPaths.md), child paths with a configuration block operation will be
interpreted as "untyped" Fusion values. That is true for attributes as well as all other child paths.

At this point, let's take a closer look into the inner workings of Fusion Object implementations. Independently of the
underlying technology (PHP Fusion / fusion4j), a Fusion Object implementation class must implement an interface with a
single method `evaluate`. At runtime, this method is called to calculate the rendering output of a Fusion Object
instance. That's simple, right?

The possibilities what you can do inside the evaluate method are basically unlimited. In most cases however, you can
generalize the algorithms to the following steps:

What is "usually" done in the evaluate method:

1. populate a new Fusion context
2. evaluate some relative, nested or direct child paths a.k.a. attributes of the Fusion Object instance (recursive
   runtime call)
3. execute custom (read-only) rendering code using the results of step 2 *that otherwise cannot be realized via pure
   Fusion code*

Other important APIs are:

* iterating all attributes of a Fusion Object instance
* evaluating relative Fusion child paths
* evaluating absolute Fusion paths

So prototypes - more concrete: their Fusion Object implementations - are a recursion layer in the overall evaluation of
a Fusion path, in case they evaluate *other paths* internally in the `evaluate` method. In other words: the Fusion
runtime API is meant to be called recursively in Fusion Object implementation classes.

Why is this important you may ask? One significant semantic concept in Fusion is iterating all attributes of a Fusion
Object instance. What exactly is an effective instance attribute is explained later in this document. Prototype
attributes are merged with instance attributes and loses over equal instance attributes.

### Prototype inheritance

Prototypes can extend *exactly one* other Prototype via inheritance. That effectively merges all nested path
declarations from the inherited prototype into the extending prototype. Paths from more concrete prototypes win over
paths from extended prototypes that are higher in the inheritance chain.

The following example has a simple inheritance chain containing four prototypes:

* `Some.Very:Basic.Component` -> most low level prototype
* `Base.Other` -> intermediate prototype, extending `Some.Very:Basic.Component`
* `Concrete.Prototype1` -> concrete prototype with body, extending `Base.Other`
* `Concrete.Prototype2` -> concrete prototype without body, extending `Base.Other`

Declaring prototype inheritance *with a body* is basically a shortcut for two operations. On the one hand, the
inheritance relation itself is declared. And on the other hand the body is interpreted as regular prototype body
declaration.

Example:

```neosfusion
prototype(Some.Very:Basic.Component) {
    // delegation class in most basic / abstract prototype
    @class = 'some.FusionObjectImplementation'
    
    mostBasic = true
}

prototype(Base.Other) < prototype(Some.Very:Basic.Component) {
    foo = 'bar'
    everything = 42
    
    // this declaration wins, since it is more concrete in the inheritance chain
    mostBasic = false
}

prototype(Concrete.Prototype1) < prototype(Base.Other) {
    someConcreteAttribute = ${1 + 1}
    // wins over inherited attribute declaration, 
    // even if this would be above the base prototype
    foo = 'baz'
}

// inheritence works without body as well
prototype(Concrete.Prototype2) < prototype(Base.Other)
```

When there are multiple inheritance declarations for the same prototype, the declaration with the highest element / load
order is taken in place. **All redundant inheritance declarations including their body are ignored**.

#### inheritance pitfalls

Prototype inheritance in Fusion is both a topic in the syntax (`prototype(A) < prototype(B)`) as well as in the semantic
of merging and prioritizing nested prototype and Fusion Object instance paths.

It is important to know, that the semantic in that case is way more restrictive than the syntax. Meaning, you are able
to write and parse valid Fusion code that will lead to paths that never get evaluated (a.k.a. unreachable code)
or even to hard Fusion index errors. Warning: the following Fusion code is very bad practice and only serves as example
what you should prevent.

Unreachable inheritance:

```neosfusion
// this line is "unreachable code"
prototype(A) < prototype(B)
// prototype A extends C but NOT B
prototype(A) < prototype(C)
```

Unreachable inheritance with body:

```neosfusion
// this whole block is "unreachable code" 
prototype(A) < prototype(B) {
    // all nested paths here are "unreachable code"
    foo = "bar"
    iWonderWhyThisPathDoesNotWork = true
}
// prototype A extends C but NOT B
prototype(A) < prototype(C)
```

Inheritance loops will lead to exceptions in the Fusion index (fusion4j). In PHP Fusion, this will lead to an endless
recursion causing a memory_limit error.

```neosfusion
prototype(A) < prototype(B)
prototype(B) < prototype(C)
// whoop whoop
prototype(C) < prototype(A)
```

Redundant duplicated inheritance declarations merge their bodies:

```neosfusion
prototype(A) < prototype(B) {
    a = 1
    b = 1
}
// this inheritance declaration is redundant...
prototype(A) < prototype(B) {
    // ... but its body is merged
    a = 2
    c = 1
}
```

Most likely you leave out the inheritance and simply configure the prototype:

```neosfusion
prototype(A) < prototype(B) {
    a = 1
    b = 1
}
// no redundant inheritance declaration here
prototype(A) {
    // ... and its body is merged
    a = 2
    c = 1
}
```

#### personal opinion on *when and how to use prototype inheritance*

Fusion is designed to be **component based**. Usually, the most low level prototype in your component's inheritance
chain should define the delegation class / `@class` attribute. Even though Fusion prototypes are meant to be inherited,
I would recommend **not to over-use inheritance** but keep your inheritance chain simple and understandable. Another way
to achieve that: use inheritance only to extend simple "base type" components that define the `@class` delegation and **
use composition to structure you domain components**. A great way to give your component a public API is to
extend `Neos.Fusion:Component`.
`Neos.Fusion` basically contains all you need in most cases. There are base prototypes for:

* data structures, arrays
* most string operations
* control structures (loops, conditions)
* structuring code (`Neos.Fusion:Component`, `Neos.Fusion:Value`)
* HTTP messages
* debugging

### namespace alias

TODO

## Fusion Object instances

There is a dedicated Fusion **data type** to declare the usage of a prototype, called "Fusion Object Instance". An
instance can have a body as well. Nested paths declared in the Fusion Object instance body will merge with prototype
paths and win over paths declared in the prototype.

Example

```neosfusion
somePath = Vendor.Package:Folder.Prototype {
    someInstanceAttribute = 1234 
}

// without body
otherPath = SomePrototype
```

### effective attributes of a Fusion Object instance

As already mentioned in the [Fusion path documentation](FusionPaths.md), a prototype attribute is a **direct child
path** of the prototype path. And so is the

```neosfusion
prototype(List.Attributes) {
    // let's pretend, this class lists, evaluates and 
    // concats all attributes of its instance
    @class = 'some.ListAttributesImplementation'
}

prototype(A) < prototype(List.Attributes) {
    a1 = 'a1'
    a2 = 'a2'
}

prototype(B) < prototype(List.Attributes) {
    b1 = 'b1'
    // das ist meine Frage ;)
    a.a3FromB = 'a3FromB'
}

instancePath = B {
    a = A
}
```

TODO