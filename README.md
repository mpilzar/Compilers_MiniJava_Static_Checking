# MiniJava Semantic Analyzer

Part 2 of the projects for the Compilers class on implementing MiniJava compilers.

A static checker for **MiniJava**, an object-oriented subset of Java. It is the front end of a
compiler: it parses a source file, builds a symbol table, enforces the type system (single
inheritance, method overriding and a restricted form of overloading), and computes the
**memory layout** of every class: field offsets and virtual-method-table slots.

Written in Java. The parser is generated with **JavaCC** and **JTB** (Java Tree Builder), and
semantic analysis runs as a pipeline of **visitors** over the generated syntax tree.

```text
$ java -jar build/minijava-checker.jar examples/Ambiguous.java
examples/Ambiguous.java:16:16: error: ambiguous overload: 'foo(Q)' in class 'Test' cannot be distinguished from 'foo(P)' (line 12); every parameter type is related by subtyping
        public int foo(Q q) {
                   ^
```

`Q` extends `P`, so neither overload is more specific than the other. The analyzer catches
this even though `P` and `Q` are declared *after* the class that uses them.

### Highlights

- **Three-pass architecture.** Pass 1 collects declarations, pass 2 validates them and computes
  layouts, and pass 3 type-checks code. Each pass relies on guarantees from the one before.
- **Full MiniJava type system.** Subtyping across class hierarchies, override checking with
  invariant return types, and overload resolution with ambiguity detection.
- **Forward references.** A class can be used as a type before it is declared. This includes
  using it to decide whether two overloads conflict.
- **Object layout.** Each field gets a byte offset (`int` 4 B, `boolean` 1 B, references and
  arrays 8 B). Each method gets a v-table slot, and an override reuses its parent's slot.
- **Compiler-style diagnostics.** Errors are reported as `file:line:column: error: message`,
  followed by the source line and a caret. Columns are correct even in tab-indented files.
- **Test suite.** 135 MiniJava programs: 46 valid programs checked against exact expected
  output, and 89 invalid ones that must be rejected *on a specific line*. An optional
  cross-check compiles every accepted program with `javac`.
- **No dependencies beyond a JDK (11+).** The parser generators are included in `lib/`.

---

## Quick start

```bash
make                                    # generate parser, compile, build the jar
java -jar build/minijava-checker.jar examples/Layout.java
make test                               # run the regression suite
```

Any number of files can be checked in one run, using the classic invocation:

```bash
java -cp build/classes Main file1.java file2.java ... fileN.java
```

| Exit status | Meaning |
|---|---|
| `0` | every file is a valid MiniJava program |
| `1` | at least one file has a lexical, syntax or semantic error |
| `2` | bad usage (no input files) |

Each file is analyzed independently, so an error in one file does not stop the others. When
several files are given, each file's output starts with a `==> file <==` header. Offsets go to
standard output and diagnostics go to standard error.

## Example: class layout

For every valid program, the analyzer prints the offset of each field and method that a class
*introduces*. Inherited fields and overriding methods are omitted, because they keep the offset
they already have in the superclass.

<table>
<tr><th>Input (<code>examples/Layout.java</code>, abridged)</th><th>Output</th></tr>
<tr><td>

```java
class A {
    int i;
    boolean flag;
    int j;
    public int foo() { ... }
    public boolean fa() { ... }
}

class B extends A {
    A type;
    int k;
    public int foo() { ... }   // override
    public boolean bla() { ... }
}
```

</td><td>

```text
-----------Class A-----------
--Variables---
A.i : 0
A.flag : 4
A.j : 5
---Methods---
A.foo : 0
A.fa : 8

-----------Class B-----------
--Variables---
B.type : 9
B.k : 17
---Methods---
B.bla : 16
```

</td></tr>
</table>

`B`'s fields continue where `A`'s end (byte 9). `B.foo` overrides `A.foo`, so it takes over
slot 0 and gets no new slot. `B.bla` is new and gets the next free slot, 16.

## Example: diagnostics

Analysis stops at the first error in each file and points to the token that caused it. The
`// ERROR` comments are markers in the test files, explained under [Testing](#testing):

```text
tests/invalid/NoOverloadMatchesInherited.java:6:15: error: no method 'f' in class 'B' is applicable to arguments (B); candidates: f(int), f(int[]), f(boolean)
            r = b.f(b); // ERROR
                  ^
tests/invalid/OverrideDifferentReturnType.java:13:12: error: 'foo(int)' in class 'B' cannot override 'foo(int)' in class 'A': return type boolean differs from int
        public boolean foo(int x) { // ERROR
               ^
tests/invalid/SuperclassDeclaredLater.java:6:17: error: superclass 'A' must be declared before its subclass 'B'
    class B extends A { // ERROR
                    ^
tests/invalid/ErrorInsideNestedBlock.java:11:21: error: incompatible types: boolean cannot be assigned to 'i' of type int
                    i = false; // ERROR
                        ^
```

Syntax and lexical errors use the same format, for example
`syntax error: unexpected '}', expected "else"`.

---

## Architecture

```text
 source.java
     │
     ▼
┌──────────────────────────┐   grammar/minijava.jj ──JTB──► syntax-tree classes + visitor base classes
│ MiniJavaParser (JavaCC)  │                       ──JavaCC──► parser
└──────────────────────────┘
     │  syntax tree (Goal)
     ▼
┌──────────────────────────┐  Pass 1 · visitor
│ DeclarationCollector     │  classes, fields, methods, parameters, locals → SymbolTable
│                          │  ✗ duplicate class / field / parameter / local
│                          │  ✗ superclass missing or declared after its subclass
└──────────────────────────┘
     │  SymbolTable
     ▼
┌──────────────────────────┐  Pass 2
│ DeclarationValidator     │  ✗ unknown type in any declaration
│                          │  ✗ illegal overload / override (whole hierarchy)
│ ClassSymbol.computeLayout│  → field offsets and v-table slots
└──────────────────────────┘
     │
     ▼
┌──────────────────────────┐  Pass 3 · visitor (every visit returns the expression's Type)
│ TypeChecker              │  ✗ every statement and expression rule
└──────────────────────────┘
     │
     ▼
 OffsetPrinter  /  Diagnostic
```

**Why a separate validation pass?** In MiniJava a class may refer to classes declared later in
the file. For example, `class A { B b; }` is legal even when `B` appears below `A`. Whether two
overloads `foo(P)` and `foo(Q)` conflict depends on whether `Q extends P`, and that is only
known once every class has been seen. If the hierarchy-dependent checks ran while declarations
were still being collected, the analyzer would silently accept some illegal programs.
`tests/invalid/AmbiguousForwardTypes.java` is one of them.

**Why the layout comes out right.** A superclass must be declared before its subclasses, and
classes are validated in source order. So when a class is laid out, its superclass already
has final offsets. New fields continue after the parent's fields, and new methods continue
after the parent's v-table.

### Overloading and overriding

For methods with the same name, visible in the same class or its ancestors:

| Situation | Verdict |
|---|---|
| Different numbers of parameters | allowed (overload) |
| Same count, and at least one position has **unrelated** types (neither is a subtype of the other) | allowed (overload) |
| Identical parameter types, in a superclass and a subclass | **override**: the return type must be identical |
| Identical parameter types in the same class | error: duplicate method |
| Same count, every position related by subtyping, not identical | error: ambiguous overload |

These rules make call resolution simple. MiniJava has single inheritance, so the class
hierarchy is a tree. If an argument of type `T` fits both `P1` and `P2`, then `P1` and `P2` are
both ancestors of `T`, which puts them on the same chain and makes them related. The rules
above guarantee that every pair of overloads has a position where the types are *unrelated*,
so no argument can fit both. **A call can therefore match at most one signature.** The checker
searches the receiver's class and then its ancestors, and the first applicable method it finds
is the right one, which is also the most-derived override.

### Static rules enforced

| Area | Checks |
|---|---|
| Declarations | unique class names; unique field names per class; unique parameter and local names per method; superclass declared before use; no self-inheritance; every type names `int`, `boolean`, `int[]` or a declared class |
| Scoping | locals and parameters shadow fields; a subclass sees inherited fields; a class cannot see subclass or unrelated-class fields; the static `main` sees only its own locals and cannot use `this` |
| Statements | assignment compatibility with subtyping (`A a = new B();` when `B extends A`); `int[]` element assignment; `boolean` conditions for `if` / `while`; `System.out.println` takes `int` only |
| Expressions | `&&` and `!` on `boolean`; `<`, `+`, `-`, `*` on `int`; array indexing, allocation and `.length` on `int[]`; `new C()` only for declared classes; integer literals within `int` range |
| Methods | calls only on class-typed receivers; overload resolution with subtyping on arguments; the returned expression must be a subtype of the declared return type |

---

## Testing

```bash
make test                      # 135 programs + command-line behaviour checks
tests/run_tests.sh -v          # list every test with the diagnostic it produced
tests/run_tests.sh --javac     # also compile every valid program with javac
```

```text
Valid programs (accepted, offsets match expected output)
  46/46 passed

Invalid programs (rejected, error reported on the marked line)
  89/89 passed

Command line

javac cross-check (every accepted program is legal Java)
  46/46 compiled

All 185 tests passed.
```

- **`tests/valid/`** contains programs that must be accepted. The output must match
  `tests/expected/<name>.txt` byte for byte. These range from the classic MiniJava benchmarks
  (BinaryTree, QuickSort, TreeVisitor, LinkedList, …) to focused cases for forward
  references, mixed-size field layout, overload resolution, override chains and shadowing.
- **`tests/invalid/`** contains programs with exactly one error each. The expected line is
  marked with a `// ERROR` comment. The runner checks that the program is rejected *and* that
  the diagnostic points at that line, so a test can't pass because of some unrelated error.
  The cases cover all three passes plus the parser: duplicate and missing declarations,
  every overloading and overriding rule, and every expression and statement rule.
- **`--javac`** compiles every valid program with `javac`. MiniJava is a subset of Java, so
  anything the analyzer accepts must also compile as Java.
- A GitHub Actions workflow (`.github/workflows/ci.yml`) runs the suite on JDK 11, 17 and 21.

## Project structure

```text
├── grammar/minijava.jj              MiniJava grammar (JavaCC format)
├── lib/                             jtb133di.jar, javacc5.jar (parser generators)
├── src/
│   ├── Main.java                    CLI driver: read → parse → analyze → report
│   ├── semantic/
│   │   ├── SemanticAnalyzer.java    runs the three passes
│   │   ├── SemanticException.java   first-error reporting with source position
│   │   ├── SourcePosition.java      line/column of any syntax-tree node
│   │   ├── symbols/                 Type, VariableSymbol, MethodSymbol, ClassSymbol, SymbolTable
│   │   └── passes/                  DeclarationCollector, DeclarationValidator, TypeChecker,
│   │                                TypeNodeResolver
│   └── report/
│       ├── OffsetPrinter.java       class layout output
│       └── Diagnostic.java          file:line:col rendering with source line and caret
├── tests/                           valid/, invalid/, expected/, run_tests.sh
├── examples/                        small programs used in this README
└── Makefile                         build/ holds all generated code, classes and the jar
```

## Design notes

- **Visitors over the generated tree.** The two traversal passes extend JTB's generated
  `DepthFirstVisitor` and `GJNoArguDepthFirst<Type>`. The type checker returns each
  expression's `Type` directly from `visit`. Identifiers that name classes or methods are read
  from their parent node, so an `Identifier` reached through `visit` is always a variable use.
  This avoids passing context flags through the traversal.
- **Types are value objects.** `Type` covers `int`, `boolean`, `int[]` and class types, plus
  an internal `String[]` used only for the unused `main` parameter, which no operation
  accepts. Layout sizes live on the type itself.
- **Overloaded methods are keyed by declaration.** Names are not unique once overloading
  exists, so the symbol table maps each method's syntax node to its `MethodSymbol` by
  identity, instead of building mangled names like `foo_int_A`.
- **Generated code stays out of the source tree.** JTB and JavaCC write into
  `build/generated/`, and `make clean` restores a pristine checkout. The grammar is the
  standard course grammar, unmodified.
- **Accurate columns.** JavaCC counts a tab as a jump to the next multiple of 8. Diagnostics
  convert that back to a character index for the `line:col` header, and draw the caret
  against a tab-expanded copy of the line, so it lines up in any file.

## The MiniJava language, briefly

MiniJava programs are valid Java programs. A program is a main class holding
`public static void main(String[] a)`, followed by any number of classes with fields and
methods. Types are `int`, `boolean`, `int[]` and class types. There are no interfaces, static
members, constructors or `void` methods, and every method ends with a single `return`.
Expressions allow one operator at a time (`&&`, `<`, `+`, `-`, `*`, `!`, array indexing,
`.length`, method calls and `new`), so `1 + 2 + 3` must be parenthesized. `if` always has an
`else`. All methods are virtual and public, and all fields are protected.
