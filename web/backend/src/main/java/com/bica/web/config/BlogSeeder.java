package com.bica.web.config;

import com.bica.web.entity.BlogPost;
import com.bica.web.repository.BlogPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Seeds the blog with initial Arc 1 (Foundations) posts on first run.
 * Only inserts if the blog_posts table is empty.
 */
@Configuration
public class BlogSeeder {

    private static final Logger log = LoggerFactory.getLogger(BlogSeeder.class);

    @Bean
    ApplicationRunner seedBlog(BlogPostRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Blog already seeded ({} posts), skipping.", repo.count());
                return;
            }

            log.info("Seeding blog with Arc 1 — Foundations posts...");

            seed(repo, "what-are-session-types",
                "What Are Session Types?",
                "Objects have protocols. Session types make those protocols explicit, checkable, and safe.",
                ARC1_POST1, 1, 1,
                "session-types,introduction,protocols,objects",
                "Alexandre Zua Caldeira");

            seed(repo, "the-grammar",
                "The Grammar — How to Write a Protocol",
                "Six constructors, one grammar, infinite protocols. Learn to read and write session types.",
                ARC1_POST2, 1, 2,
                "grammar,syntax,branch,select,parallel,recursion",
                "Alexandre Zua Caldeira");

            seed(repo, "parsing-from-text-to-structure",
                "Parsing — From Text to Structure",
                "How a string like '&{open: +{OK: end, ERROR: end}}' becomes a tree your tools can reason about.",
                ARC1_POST3, 1, 3,
                "parser,AST,tokenizer,recursive-descent",
                "Alexandre Zua Caldeira");

            log.info("Blog seeded with 3 posts.");
        };
    }

    private void seed(BlogPostRepository repo, String slug, String title, String summary,
                      String content, int arc, int seq, String tags, String author) {
        BlogPost post = new BlogPost();
        post.setSlug(slug);
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setArc(arc);
        post.setSequence(seq);
        post.setTags(tags);
        post.setAuthor(author);
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        repo.save(post);
    }

    // -----------------------------------------------------------------------
    // Arc 1, Post 1: What Are Session Types?
    // -----------------------------------------------------------------------
    private static final String ARC1_POST1 = """
## The Problem: Objects Used Wrong

Every programmer has seen it. You call `.read()` on a file that was never opened. You send data on a socket that's already closed. You call `.next()` on an iterator without checking `.hasNext()` first.

The object is there. The method exists. The compiler is happy. But at runtime — crash.

The root cause is always the same: **the object has a protocol**, and you violated it. There's an order in which methods should be called, and you got it wrong.

What if the compiler could catch that?

## Types That Describe Behaviour

A *session type* is a type that describes not what data an object holds, but **what you're allowed to do with it, and in what order**.

Consider a Java `Iterator`. Its protocol is:

1. Call `hasNext()`
2. If it returns `TRUE`, you may call `next()` — then go back to step 1
3. If it returns `FALSE`, you're done

As a session type, this becomes:

```
rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
```

Read it like this:
- `rec X` — "this protocol can repeat" (recursion)
- `&{hasNext: ...}` — "the object offers the method `hasNext`" (branch)
- `+{TRUE: ..., FALSE: ...}` — "the object makes a choice" (selection)
- `end` — "the protocol is finished"

The session type captures the *entire lifecycle* of the object in one expression.

## Why This Matters

With session types, a compiler (or a static checker) can verify:

- **Completeness**: You handle every method the object offers
- **Safety**: You never call a method that isn't available in the current state
- **Termination**: The protocol eventually reaches `end`
- **Compatibility**: A client's usage matches the object's protocol

This isn't hypothetical. Session types have been studied in academia since the 1990s, starting with Honda, Vasconcelos, and Kubo's work on communication protocols. Our project brings them to *objects* — the things you actually program with every day.

## A Simple Example

Here's a file protocol:

```
&{open: &{read: end, write: end}}
```

This says: "first call `open`, then you may call either `read` or `write`, and then you're done."

What you **can't** do:
- Call `read` before `open` — the method isn't available yet
- Call both `read` and `write` — you must choose one
- Call `open` twice — after opening, the protocol moves forward

The session type makes all of this explicit. No documentation to read and hope you understood correctly. No runtime surprises. The protocol **is** the type.

## What Makes Our Approach Different

Most session type research focuses on *channels* — communication between processes. We focus on **objects**: Java classes, Python modules, API endpoints. Things with methods, not message channels.

This shift leads to a surprising discovery: when you build the *state space* of a session type (all possible states and transitions), it forms a mathematical structure called a **lattice**.

A lattice is a partially ordered set where every pair of elements has a least upper bound (join) and a greatest lower bound (meet). This structure is not just elegant — it's *useful*. It gives us:

- **Subtyping for free**: One protocol fits inside another when one lattice embeds into another
- **Parallel composition**: Two concurrent protocols on the same object form a *product lattice*
- **Algebraic invariants**: Eigenvalues, polynomials, and spectral properties that characterise protocols

We call these lattice-shaped state spaces **reticulates** (from the Latin *reticulatum* — net-shaped). The entire research programme is built on this single insight: **session types form lattices, and lattice theory gives us powerful tools to analyse them**.

## What's Next

In the next post, we'll learn the grammar — the six constructors that let you write any session type. By the end, you'll be reading session types as fluently as you read regular expressions.

*This post is part of Arc 1 — Foundations, a progressive introduction to session types as algebraic reticulates.*
""";

    // -----------------------------------------------------------------------
    // Arc 1, Post 2: The Grammar
    // -----------------------------------------------------------------------
    private static final String ARC1_POST2 = """
## Six Constructors, Infinite Protocols

Every session type is built from just six constructors. With these six pieces, you can describe any object protocol — from a simple file to a complex multi-phase API.

Let's meet them one by one.

## 1. End — The Terminal

```
end
```

The simplest session type. It means "the protocol is finished — there's nothing left to do." Every protocol eventually reaches `end`.

Think of it as the period at the end of a sentence.

## 2. Branch — External Choice (&)

```
&{method1: S1, method2: S2, ..., methodN: SN}
```

A branch says: "the object offers these methods. Pick one." The *environment* (the caller, the client) decides which method to call.

Example — a vending machine:

```
&{insertCoin: &{selectDrink: end}}
```

"First insert a coin, then select a drink."

Branches can offer multiple methods at the same level:

```
&{read: end, write: end, close: end}
```

"You may read, write, or close — pick one."

## 3. Selection — Internal Choice (+)

```
+{label1: S1, label2: S2, ..., labelN: SN}
```

A selection says: "the object makes a decision. You must handle all possible outcomes." The *object itself* decides which path to take.

Example — an authentication check:

```
+{OK: &{getData: end}, DENIED: end}
```

"The server decides: either OK (and you can get data) or DENIED (and you're done)."

The crucial distinction: in `&{...}`, **you** choose. In `+{...}`, **the object** chooses. Getting this wrong is a common source of protocol errors.

## 4. Parallel — Concurrent Composition (||)

```
(S1 || S2)
```

Parallel says: "two sub-protocols execute concurrently on the same object." Both must complete, but their steps can interleave.

Example — a database connection:

```
(&{read: end} || &{write: end})
```

"One thread can read while another writes." The resulting state space is the *product* of the two sub-protocols — every combination of read-progress and write-progress is a valid state.

This is the constructor that makes lattice structure *necessary*, not just convenient. The product of two lattices is always a lattice.

## 5. Recursion — Repeating Protocols (rec X . S)

```
rec X . S
```

Recursion lets protocols repeat. The variable `X` marks the loop point, and using `X` inside `S` means "go back to the start."

The Iterator protocol from the previous post:

```
rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
```

Every recursive protocol must have an exit path — a branch that leads to `end` without going through `X`. Otherwise, the protocol would never terminate.

## 6. Variables — Loop Points (X)

```
X
```

A variable by itself means "jump back to where this variable was bound by `rec`." You'll never write a bare `X` at the top level — it always appears inside a `rec X . ...` body.

## Combining Constructors

The power comes from combining these six pieces. Let's build a real protocol — **SMTP** (simplified):

```
&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}
```

"Connect (EHLO), specify sender (MAIL), specify recipient (RCPT), send data (DATA), done."

A more realistic version with error handling:

```
&{EHLO: +{250: &{MAIL: +{250: &{RCPT: +{250: &{DATA: end}, 550: end}}, 550: end}}, 500: end}}
```

"At each step, the server can accept (250) or reject (5xx). You must handle both outcomes."

## Reading Session Types

Here's a cheat sheet for reading session types:

| Symbol | Meaning | Who decides? |
|--------|---------|-------------|
| `&{m: S}` | "Offer methods" | The caller chooses |
| `+{l: S}` | "Make a decision" | The object chooses |
| `(S1 \\|\\| S2)` | "Run concurrently" | Both happen |
| `rec X . S` | "This repeats" | Loop structure |
| `X` | "Go back" | Jump to rec |
| `end` | "Done" | Protocol finished |

When reading a complex type, start from the outside and work inward. Each constructor tells you one thing about what happens next.

## Try It Yourself

Here are some protocols to practice reading:

**Two-buyer protocol:**
```
&{price: +{OK: &{pay: end}, REJECT: end}}
```

**OAuth 2.0 (simplified):**
```
&{authorize: +{CODE: &{token: +{ACCESS: end, ERROR: end}}, DENIED: end}}
```

**Java Iterator:**
```
rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
```

Can you trace through each one? At every step, ask: "Who decides?" and "What happens next?"

## What's Next

Now that you can read and write session types, the next post shows how a parser turns these strings into structured trees that tools can analyse. We'll see tokenization, recursive descent, and how the grammar maps to an AST.

*This post is part of Arc 1 — Foundations, a progressive introduction to session types as algebraic reticulates.*
""";

    // -----------------------------------------------------------------------
    // Arc 1, Post 3: Parsing — From Text to Structure
    // -----------------------------------------------------------------------
    private static final String ARC1_POST3 = """
## From Strings to Trees

You've learned the grammar. You can read `&{open: +{OK: end, ERROR: end}}` and understand what it means. But a tool can't work with a string — it needs *structure*. That's what a parser does: it transforms text into a tree that algorithms can traverse, analyse, and transform.

In this post, we'll walk through exactly how our parser works — from raw characters to a fully typed Abstract Syntax Tree (AST).

## Step 1: Tokenization

The first pass breaks the input string into *tokens* — the meaningful pieces:

```
Input:  &{open: +{OK: end, ERROR: end}}

Tokens: & { open : + { OK : end , ERROR : end } }
```

Each token has a type:
- `&` → BRANCH
- `+` → SELECT
- `{` → LBRACE
- `}` → RBRACE
- `:` → COLON
- `,` → COMMA
- `(` → LPAREN
- `)` → RPAREN
- `||` → PAR
- `.` → DOT
- `rec` → REC
- `end` → END
- anything else → IDENT (a method name, label, or variable)

The tokenizer is simple: skip whitespace, match the longest prefix, emit a token. The only subtle case is `||` — two characters that form a single token.

## Step 2: Recursive Descent

The parser reads tokens left to right and builds the AST using a technique called *recursive descent*. Each grammar rule becomes a function:

```
parseType()      → parsePrimary(), then check for || or .
parsePrimary()   → parseAtom()
parseAtom()      → end | variable | rec X . S | &{...} | +{...} | (S)
parseBranch()    → &{ m1: S1, m2: S2, ... }
parseSelect()    → +{ l1: S1, l2: S2, ... }
```

The key insight: **the first token tells you which rule to apply.**

- See `&`? Parse a branch.
- See `+`? Parse a selection.
- See `(`? Parse a group (could be parallel).
- See `rec`? Parse a recursion.
- See `end`? Return the End node.
- See an identifier? Return a Variable node.

This is why session types are easy to parse — the grammar is *LL(1)*, meaning one token of lookahead is always enough to decide what to do.

## Step 3: The AST

The parser produces an Abstract Syntax Tree made of frozen (immutable) dataclasses. Here are the node types:

```python
End()                              # Terminal
Var(name="X")                      # Variable reference
Branch(choices={"open": S, ...})   # External choice
Select(choices={"OK": S, ...})     # Internal choice
Parallel(left=S1, right=S2)        # Concurrent composition
Rec(var="X", body=S)               # Recursion binding
```

For the input `&{open: +{OK: end, ERROR: end}}`, the AST is:

```
Branch
  └─ "open" → Select
                ├─ "OK"    → End
                └─ "ERROR" → End
```

The tree structure mirrors the protocol structure exactly. Every node is a decision point, every leaf is a terminal.

## A Real Example: The Iterator

Let's trace `rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}`:

1. See `rec` → enter parseRec, consume `X`, consume `.`
2. Parse body → see `&` → enter parseBranch
3. Parse `hasNext:` → parse continuation → see `+` → enter parseSelect
4. Parse `TRUE:` → see `&` → parse branch `next:` → see `X` → return Var("X")
5. Parse `FALSE:` → see `end` → return End
6. Assemble the tree

Result:
```
Rec(var="X")
  └─ Branch
       └─ "hasNext" → Select
                        ├─ "TRUE"  → Branch
                        │             └─ "next" → Var("X")
                        └─ "FALSE" → End
```

The `Var("X")` at the bottom creates a cycle back to the `Rec` node — this is how recursion is represented structurally.

## Error Handling

The parser produces clear error messages when the input is malformed:

- `&{open end}` → "Expected ':' after method name 'open', got 'end'"
- `&{open: }` → "Expected session type after ':', got '}'"
- `rec . body` → "Expected variable name after 'rec', got '.'"

Good error messages matter. When a protocol is wrong, you need to know *where* and *why*, not just "parse error at position 23."

## What the Parser Doesn't Do

The parser only checks *syntax*. It doesn't check whether:

- Recursive types actually terminate (that's the termination checker)
- Variables are bound (that's a scope check)
- Parallel compositions are well-formed (that's the WF-Par check)

These semantic checks happen in later pipeline stages. The parser's job is narrow and clean: text in, tree out.

## The Pretty Printer

The parser has an inverse: the pretty printer. It takes an AST and produces a canonical string representation. This is useful for:

- Displaying normalized types (consistent formatting)
- Round-trip testing (parse → print → parse → check equality)
- Debugging (seeing exactly what the parser produced)

Every AST round-trips cleanly: `parse(print(parse(s))) == parse(s)` for all valid inputs.

## Why This Matters

The parser is the front door of the entire pipeline. Everything downstream — state space construction, lattice checking, subtyping, test generation — starts with the AST the parser produces. Getting the parser right means:

- **Exact grammar support**: Every constructor from the specification is parsed correctly
- **Immutable output**: The AST can't be accidentally mutated by downstream stages
- **Deterministic**: The same input always produces the same tree
- **Fast**: Linear time, single pass, no backtracking

In the next post, we'll see what happens *after* parsing — how the AST becomes a state space, and how that state space reveals the mathematical structure hiding inside every protocol.

*This post is part of Arc 1 — Foundations, a progressive introduction to session types as algebraic reticulates.*
""";
}
