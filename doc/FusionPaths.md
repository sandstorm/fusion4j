# Fusion paths

... and what they are all about.

For the basic understanding, let's differentiate between two main types of Fusion paths:

1. Declared Fusion paths
2. Evaluation paths

Independent of that, Fusion paths can be *absolute* or *relative*. Paths are a list of *path segments*, usually
seperated by a dot `.`
The most simple path contains only one path segment. Thus, a path segment itself can also be seen as a Fusion path in
most cases.

## Declared Fusion paths

A declared Fusion path lives in a Fusion file written by you. It can be seen as the
*left side* (or the left operand) of a declaration operation in the Fusion language syntax.

There are the following operations for declared paths:

* assignment `=`
* configuration block `{ ... }`
* path erasure `>`
* path copy `<`

TODO: link in-depth doc for path operations or inline here?

### absolute, relative and "virtual" declared paths

Paths in the top-layer (or root-layer -> but NOT to be confused with the Fusion path `root`)
of a Fusion file always declare *absolute* paths. Configuration blocks are a recursion layer in the Fusion syntax. As
soon as you declare paths inside a configuration block, meaning as soon as you enter the first recursion layer, those
paths always declare *relative* paths. Relative paths plus their parents, result in implicitly declared
"virtual" *absolute* and *relative* paths, though. Example:

```neosfusion
outer {
    nested {
        inner = true
    }
}
```

Here we see **three** directly declared paths:

* `outer` absolute, configuration operator
* `nested` relative, configuration operator
* `inner` relative, assignment operator They result in the following implicitly declared "virtual" paths:
* `outer.nested` absolute, configuration operator
* `outer.nested.inner` absolute, assignment operator
* `nested.inner` relative, assignment operator

Assignment example:

```neosfusion
myPath = 'some value'
```

In that case, the `=` is the declaration operator for a primitive Fusion value (string) and `myPath` is the declared
Fusion path.

Configuration:

```neosfusion
myPath {
    inner = true
    // some more here
}
```

A configuration itself - independent of its sub paths - is a declared Fusion path `myPath` as well.

### declaring nested paths

TODO

```neosfusion
some.nested.path = 'nice'
```

All parents of a nested path result in "virtual" configurations.

```neosfusion
myPath.inner = true
```

In that example `myPath` is declared indirectly as virtual configuration. This is important later: when asking the
Fusion index for all nested paths, configurations (including virtual configurations) will resolve as *untyped* nested
paths.

### declared prototype paths

The Fusion syntax keyword `prototype(<QualifiedPrototypeName>)` is mostly used for root-level prototype declarations.
Looking at it a more generic way, *all* possible occurrences of the prototype keyword including the qualified prototype
name are **Fusion path segments**. Being segments makes them also Fusion paths itself (one-segmented path). Taking this
further, a *prototype body declaration* is basically a configuration operation for a path, ending with a prototype
segment.

Prototype example:

```neosfusion
prototype(FooBar) {
    foo = 42
}
// equal to
prototype(FooBar).foo = 42
```

Here the `=` is the declaration operator for the declared Fusion path `prototype(FooBar).foo`. Since the short
form `prototype(FooBar).foo = 42` results in a virtual path configuration for `prototype(FooBar)`, this is equal to an
explicit configuration of `prototype(FooBar)` and assignment of relative nested path `foo`.

It is important to notice, that declared Fusion paths *ending* with a prototype segment have special meanings and syntax
rules:

**the assignment operator is not supported**, meaning `foo.bar.prototype(FooBar) = 42` is no valid Fusion.

**configurations are usually called "prototype body declarations"**

```neosfusion
// 
foo.bar.prototype(FooBar) {
    // body
}
prototype(RootLevel.Prototype) {
    // body
}
```

**path erasures are usually called "prototype erasures"**

```neosfusion
foo.bar.prototype(IsErasedHere) >
prototype(NotNeeded) >
```

**path copy is usually called "prototype inheritance"**

1. is only supported on root-layer (meaning on absolute, one-segmented paths)
2. only supports other one-segmented prototype paths as copy source

```neosfusion
// without prototype body
prototype(FooBar) < prototype(Some.Base)
// with body
prototype(MoreConcrete) < prototype(Value) {
    value = 42
}
```

Declaring prototype inheritance with body is a very special case in the Fusion syntax. Usually, the copy and
configuration operator can not be combined.

### Prototype extension paths

TODO

```neosfusion
foo.prototype(Foo).bar.baz.prototype(Value).fooBar = 42
```

## Evaluation paths

The main difference to a declared Fusion path is, that during Fusion Runtime evaluation, the actual prototype of a
Fusion object instance can be changed via public API. For example, this is used in `Neos.Fusion:Case`
and `Neos.Fusion:DataStructure` to handle untyped fusion path declarations. This has a significant impact: the effective
nested paths of a Fusion object instance can not be determined by a path given in the pure form of a declared path.
Additionally, the prototype of Fusion object instances must be specified in all parent path segments.

Example: the path `myPath.value.value` could be evaluated with Fusion code:

```neosfusion
myPath = Value {
    value = Value {
        value = 42
    }
}
```

Or with Fusion Code:

```neosfusion
myPath.value.value = 42
```

Where all prototypes of untyped paths are set via API.

We need a way to ask the Fusion index for all nested paths using a more concrete evaluation path like:
`myPath<Value>.value<Value>.value`
