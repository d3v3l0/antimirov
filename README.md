[![Build Status](https://api.travis-ci.org/non/antimirov.svg)](https://travis-ci.org/non/antimirov)
[![codecov.io](http://codecov.io/github/non/antimirov/coverage.svg?branch=master)](http://codecov.io/github/non/antimirov?branch=master)

## Antimirov

### Dedication

This project is named after Valentin Antimirov (1961 - 1995). His work
on [partial derivatives](https://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.56.2509)
of regular expressions is fundmental to this project.

### Overview

Antimirov is a Scala package for working with
[regular expressions](https://en.wikipedia.org/wiki/Regular_expression).

Antimirov defines an `Rx` type, which supports the following regular
expression combinators:

 - Unmatchable regular expressions (`Rx.Phi`)
 - Matching the empty string (`Rx.Empty`)
 - Matching single characters (`Rx.Letter(c)`)
 - Matching sets of characters (`Rx.Letters(cs)`)
 - Alternation (`Rx.Choice(x, y)`, i.e. `x + y` or `x | y`)
 - Concatenation (`Rx.Concat(x, y)`, i.e. `x * y`)
 - Kleene Star (`Rx.Star(x)`, i.e. `x.star`)
 - Repetition (`Rx.Repeat(r, m, n)`, i.e. `r.repeat(m, n)`)

In addition to the previous combinators which are reified as algebraic
data types, `Rx` supports additional operations:

 - Exponentiation (`x.pow(k)`)
 - Optionality (`x.optional`)
 - Intersection (`x & y`)
 - Exclusive-or (XOR, i.e. `x ^ y`)
 - Difference (`x - y`)
 - Complement (`~x`)
 - Equality (`x === y`)
 - Partial-ordering (`x < y`, `x partialCompare y`, etc.)
 - Derivatives (`x.deriv(c)`, `x.partialDeriv(c)`)

These operations are consistent with the corresponding set operations.
What this means is that each `Rx` value has a corresponding set of
strings it accepts, and that these operations produce new `Rx` values
whose sets are consistent with the corresponding set operations.

Finaly, `Rx` values can be compiled down to an automaton for more
efficient matching (either a `Dfa` or `Nfa`).

### Getting Antimirov

Antimirov supports Scala 2.13 and 2.12. It is not yet published.

You can check out Antimirov locally using [SBT](https://www.scala-sbt.org/1.x/docs/).
After cloning the repository, run `sbt` and then at the prompt run `console`:

```
$ git clone https://github.com/non/antimirov.git
$ cd antimirov
$ sbt
[info] Loading settings for project global-plugins from javap.sbt ...
[info] Loading global plugins from /Users/erik/.sbt/1.0/plugins
[info] Loading settings for project antimirov-build from plugins.sbt ...
[info] Loading project definition from /Users/erik/w/antimirov/project
[info] Loading settings for project root from build.sbt ...
[info] Set current project to root (in build file:/Users/erik/w/antimirov/)
[info] sbt server started at local:///Users/erik/.sbt/1.0/server/24f52b77357d3981e1c8/sock
sbt:root> console
[info] Starting scala interpreter...
Welcome to Scala 2.13.2 (OpenJDK 64-Bit Server VM, Java 1.8.0_212).
Type in expressions for evaluation. Or try :help.

scala> antimirov.Rx.parse("([a-c][d-f])*").deriv('a')
val res0: antimirov.Rx = [d-f]([a-c][d-f])*
```

### Details

Antimirov provides an algebraic interface for building regular
expressions, as well as testing them for equality, subset/superset
relationships, and more:

```scala
import antimirov.Rx

val x: Rx = Rx.parse("[1-9][0-9]*")

x.accepts("0")      // false
x.accepts("1")      // true
x.accepts("19")     // true
x.accepts("09")     // false

val y: Rx = Rx.parse("[0-9a-f][0-9a-f]")

y.accepts("af")     // true
y.accepts("09")     // true
y.accepts("099")    // false

// set operations
//
// note that the full Char range is:
//   ['\u0000', ..., '/', '0', ... '9', ':', ... '\uffff']

val z1: Rx = x | y  // [1-9][0-9]*|[0-9a-f][0-9a-f]
val z2: Rx = x & y  // [1-9][0-9]
val z3: Rx = x ^ y  // 0[0-9a-f]|[1-9][0-9][0-9][0-9]*|[1-9][a-f]|[1-9]|[a-f][0-9a-f]
val z4: Rx = x - y  // [1-9][0-9][0-9][0-9]*|[1-9]
val z5: Rx = ~x     // [^1-9].*|[1-9][0-9]*[^0-9].*|

// equality, subset, and superset comparisons

val xx: Rx = Rx.parse("[1-4][0-9]*|[5-9][0-9]*")
x == xx  // false
x === xx // true
x <= xx  // true
x < xx   // false

val U: Rx = Rx.parse(".*")
x == U   // false
x === U  // false
x <= U   // true
x < U    // true
```

An `antimirov.Rx` value can be converted to an `antimirov.Nfa`, a
`java.util.regex.Pattern`, or a `scala.util.matching.Regex`.

Note that unlike many modern regex libraries, Antimirov's regular
expressions do not contain non-regular features (such as
back-references, zero-width assertions, and so on), and are solely
focused on matching, not on searching or extraction.

Concretely, this means that:

    1. Patterns are matched against the entire string
    2. No subgroup extraction is possible
    3. The only primitive operations are alternation, concatenation, and Kleene star

In exchange for giving up these modern affordances, Antimirov can do
things that most regular expression libraries can't, such as
intersection, exclusive-or, negation, semantic equality checks, set
comparisons (e.g. inclusion), and more.

### ScalaCheck support

The `antimirov-check` package support ScalaCheck, adding the ability
to generate strings according to a regular expression. There are two
ways to use it:

```scala
package demo

import antimirov.Rx
import org.scalacheck.{Prop, Properties}

object Demo extends Properties("Demo") {

  property("Arbitrary-based usage") = {
    val r1 = antimirov.check.Regex("-?(0|[1-9][0-9]*)")
    Prop.forAll { (w: r1.Word) =>
      val s: String = w.value
      // s is guaranteed to be accepted by r1
      ???
    }
  }

  property("Gen-based usage") = {
    val r2 = Rx.parse("-?(0|[1-9][0-9]*)")
    val mygen: Gen[String] = antimirov.gen.rx(r2)
    Prop.forAll(mygen) { s: String =>
      // s is guaranteed to be accepted by r2
      ???
    }
  }
}
```

One thing to note here is that `antimirov.Rx` has no direct dependency
on ScalaCheck, which is why we introduce `antimirov.check.Regex` to
gain `Arbitrary` support.

`Regex` is wrapper type around `Rx` that adds the path-dependent type
`Word` as well as implementations of `Gen` and `Arbitrary` used by
ScalaCheck. `Regex` does not support the full suit of Antimirov
operations (such as `&`) and is really just meant for use with
ScalaCheck. (For other usages, prefer its embedded `Rx` value).

### Web/JS tool

Antimirov has an interactive JS/HTML tool for working with regular
expressions. You can try it out at
[http://phylactery.org/antimirov/](http://phylactery.org/antimirov/).

To visit this page locally, simply run `sbt web/fullOptJS` and then
visit `web/index.html` in your web browser.

### Known Issues

The biggest issue with this library is that the problems are
exponential in the general case. This means there are plenty of
expressions for which Antimirov's operations (equality, inclusion,
intersection, and so on) are prohibitively slow.

There are some good strategies for dealing with this complexity
through heuristics and optimizations. But some constructions (such as
very wide alternations contained within a Kleene star) will probably
never perform very well.

Here's a list of other known problems:

    1. Antimirov doesn't preserve user-specified expression syntax
    2. Antimirov cannot (yet) check constant expressions at compile-time
    3. Synthetic operators such as + and ? are not reified in the AST
    4. There could be more work on simplification/canonicalization

### Future Work

Since the general problem is exponential, there is likely a lot of
future work around chipping away at the margins: heuristics that cover
most interesting regular expressions users are interested in.

There is major room for improvement for the HTML/JS tool (the current
version was optimized for what was easy for the author to produce).

Additionally, we could provide an interactive/command-line tool to
help work with regular expressions (potentially using Graal or Scala
Native to produce a native executable).

We could add support for other kinds of generators and test frameworks
(e.g. Hedgehog, Scalaprops, etc.)

There is signficant room for improvement of the NFA/DFA
implementations (they have not been the primary focus of the project
so far). Relatedly, we could add support for searching instead of just
matching.

It would be interesting to see how many of Antimirov's features we can
preserve if we allow extracting subgroup matches.

### Copyright and License

All code is available to you under the Apache 2 license, available at
https://opensource.org/licenses/Apache-2.0.

Copyright Erik Osheim, 2020.
