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
                "Have you ever called .read() on a file you forgot to open? I have. That frustration led me to something unexpected.",
                ARC1_POST1, 1, 1,
                "session-types,introduction,protocols,objects",
                "Alexandre Zua Caldeira");

            seed(repo, "the-grammar",
                "The Grammar — How to Write a Protocol",
                "How do you describe a conversation? Not the content — the structure. Six building blocks is all it takes.",
                ARC1_POST2, 1, 2,
                "grammar,syntax,branch,select,parallel,recursion",
                "Alexandre Zua Caldeira");

            seed(repo, "parsing-from-text-to-structure",
                "Parsing — From Text to Structure",
                "Your tools can't work with strings. They need structure. Here's how a flat line of text becomes a tree you can reason about.",
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
Have you ever called `.read()` on a file you forgot to open?

I have. More than once, actually. The compiler didn't complain. The IDE didn't underline anything in red. Everything looked fine — until runtime, when the whole thing blew up with an exception that could have been caught before I even hit "run."

## The gap the compiler doesn't see

Here's the thing that always bothered me about that: the compiler *knows* what methods exist on my object. It checks the types of my arguments, the return values, even whether I'm handling exceptions. But it has no idea whether I'm calling those methods *in the right order*.

And order matters. You can't read before you open. You can't send data on a closed socket. You can't call `.next()` on a Java Iterator without first checking `.hasNext()`.

Every object has a protocol — a set of rules about which methods you can call, when, and in what sequence. We all learn these protocols by reading documentation, studying examples, or (let's be honest) crashing at runtime and working backwards.

What if that protocol were part of the type itself?

## What if the protocol were the type?

That's exactly what a **session type** is. It's a type that describes not what data an object holds, but what you're *allowed to do with it* — and in what order.

Take that Java Iterator. You know the drill: check `hasNext()`, and if it says yes, call `next()`. Repeat until it says no. We all carry this protocol in our heads. But written as a session type, it looks like this:

```session-type
rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
```

Don't worry about the syntax yet — we'll unpack it in the next post. What matters right now is what this *does*: it captures the entire lifecycle of an Iterator in a single expression. The looping, the branching, the termination — it's all there.

And once it's there, a tool can *check* it. Automatically. Before your code runs.

## We have types for data — but nothing for behaviour

I started exploring session types because I was frustrated with a gap in how we think about objects. We have types for data — `int`, `String`, `List<User>`. We have types for functions — input types, output types, checked exceptions. But we have nothing for *behaviour*. Nothing that says "this object goes through these phases, and you must respect them."

Most session type research focuses on message-passing channels — processes sending data back and forth. That's important work, but it's not how most of us write code day to day. We write code that calls methods on objects. We open files, query databases, iterate over collections, authenticate users.

So I asked a different question: what happens when you apply session types to *objects*?

## Something unexpected: every protocol forms a lattice

Something unexpected happened. When I built the state space of a session type — the graph of all possible states an object can be in, connected by method-call transitions — it didn't form just any structure. It formed a **lattice**.

A lattice is a mathematical structure where every pair of elements has a natural "meet" (greatest lower bound) and "join" (least upper bound). If you've used `git merge`, you've implicitly worked with a lattice — the commit history forms one. If you've ever found the "most specific common supertype" of two Java classes, that's a lattice operation too.

But here's what surprised me: this wasn't a coincidence. It wasn't a special property of certain carefully chosen protocols. Every session type I tested — simple ones, complex ones, recursive ones, parallel ones — produced a lattice. Every single one.

## Reticulates: why the lattice structure matters

I call these lattice-shaped state spaces **reticulates**, from the Latin *reticulatum* — net-shaped. And it turns out that the lattice structure isn't just mathematically pretty. It's *useful*.

It gives you subtyping for free — one protocol fits inside another exactly when one lattice embeds into another. It gives you safe parallel composition — two concurrent protocols on the same object form a product lattice. And it opens the door to algebraic tools — eigenvalues, polynomials, spectral analysis — that can tell you things about your protocol that no amount of testing could reveal.

## This blog is that story

I'm going to take you from the basics — what session types are, how to write them, how to parse them — all the way to the deep algebraic structure hiding inside the protocols you already use every day.

We're going to start simple. Next up: the grammar — six small building blocks that can describe any protocol.

*Part 1 of Arc 1 — Foundations*
""";

    // -----------------------------------------------------------------------
    // Arc 1, Post 2: The Grammar
    // -----------------------------------------------------------------------
    private static final String ARC1_POST2 = """
How do you describe a conversation?

Not the content — the *structure*. The fact that first you say hello, then you ask a question, then the other person either answers or says "I don't know," and depending on that, you either follow up or move on.

We do this instinctively when we talk. But when we write code, we lose the structure. We just... call methods and hope for the best.

Session types give us a way to write down that structure precisely. And it turns out you only need six building blocks to describe any protocol, no matter how complex. Let me walk you through them.

## Every conversation ends: `end`

Every conversation eventually stops. In session types, that's `end`. It means "we're done here — nothing left to do." Think of it as hanging up the phone.

## You choose what to do: `&{...}`

This is called a **branch**, and it's where things get real. When you write:

```session-type
&{read: end, write: end}
```

you're saying: "the object offers two methods — `read` and `write`. The caller picks one." It's like a waiter handing you a menu. *You* decide what to order.

You can chain branches to create sequences. A file protocol might look like:

```session-type
&{open: &{read: end, write: end}}
```

First you open, *then* you choose between reading and writing. The ordering is baked right in.

## The object chooses what happens: `+{...}`

This looks almost identical to branch, but the meaning is completely different:

```session-type
+{OK: &{getData: end}, DENIED: end}
```

This is a **selection**. The *object* decides the outcome — not you. Maybe you sent a login request, and the server comes back with either OK or DENIED. You don't get to pick. You have to handle both.

Here's the key distinction that trips people up: with `&{...}`, **you** choose which method to call. With `+{...}`, **the object** chooses what happens next. Mixing these up is one of the most common sources of protocol bugs. It's the difference between "pick a door" and "a door opens for you."

## Two things happen at once: `(S1 || S2)`

This is **parallel composition** — two things happening concurrently on the same object:

```session-type
(&{read: end} || &{write: end})
```

One thread reads, another writes, and they interleave freely. This is the constructor I'm most excited about, because it's what forces the mathematical structure I mentioned in the previous post. When you run two protocols in parallel, their combined state space forms a *product* — and the product of two lattices is always a lattice.

But we'll get to that. For now, just know that `||` means "both of these happen, and they don't have to take turns."

## Protocols that repeat: `rec X . S`

Protocols that repeat need recursion. The `rec X` part says "I'm defining a loop called X," and whenever you write `X` inside the body, it means "go back to the start."

Remember the Iterator from the last post?

```session-type
rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}
```

Read it as a story: "Check if there's a next element. The iterator tells you TRUE or FALSE. If TRUE, grab the element and loop back. If FALSE, you're done."

There's one rule: every loop must have an exit. There has to be a path that reaches `end` without going through `X`. Otherwise, your protocol would run forever — and that's not a protocol, that's a bug.

## Building a real protocol: SMTP

Here's where it gets fun. Let me build a real protocol from scratch — SMTP, the email protocol.

At its simplest, sending an email is a sequence of steps:

```session-type
&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}
```

Connect, say who you are, say who you're writing to, send the message, done. Four method calls in order.

But real SMTP isn't that clean. The server can reject you at every step. So the realistic version interleaves branches with selections:

```session-type
&{EHLO: +{250: &{MAIL: +{250: &{RCPT: +{250: &{DATA: end}, 550: end}}, 550: end}}, 500: end}}
```

It's dense, I know. But read it like a conversation: "You say EHLO. The server responds with 250 (ok) or 500 (error). If ok, you send MAIL. The server responds 250 or 550." And so on. Every possible path through the conversation is captured, including every possible rejection.

No documentation needed. No guessing. The protocol *is* the specification.

## The one question that matters: who decides?

Whenever you're reading a session type, there's really only one question to ask at each step: **who decides?**

If you see `&{...}`, you decide. You're the one picking which method to call.

If you see `+{...}`, the object decides. You just have to be ready for any outcome.

That's it. That's the whole game. Who decides, and what happens next.

Try reading this one — it's OAuth 2.0:

```session-type
&{authorize: +{CODE: &{token: +{ACCESS: end, ERROR: end}}, DENIED: end}}
```

You authorize. The server gives you a code or denies you. If you got a code, you request a token. The server grants access or errors out. At every `&`, you're acting. At every `+`, you're reacting.

In the next post, I'll show you how a parser turns these strings into structured trees — and why that transformation is the first step toward everything else we're going to build.

*Part 2 of Arc 1 — Foundations*
""";

    // -----------------------------------------------------------------------
    // Arc 1, Post 3: Parsing — From Text to Structure
    // -----------------------------------------------------------------------
    private static final String ARC1_POST3 = """
So you can write `&{open: +{OK: end, ERROR: end}}` and you know what it means. But your tools don't. To a computer, that's just a string of characters — twenty-nine bytes with no structure, no meaning, no way to reason about what's inside.

How do you get from a flat string to something you can actually *work with*?

That's the parser's job. And it's more interesting than it sounds.

---

## Chopping strings into meaningful chunks

The first thing the parser does is **tokenization** — chopping the string into meaningful chunks. Think of it like reading a sentence: you don't process it letter by letter, you recognize words.

When the tokenizer sees `&{open: +{OK: end, ERROR: end}}`, it produces a stream of tokens: `&`, `{`, `open`, `:`, `+`, `{`, `OK`, `:`, `end`, `,`, `ERROR`, `:`, `end`, `}`, `}`.

Each token carries a type — is this a brace? A colon? A method name? The keyword `end`? The tokenizer doesn't care about what the protocol *means*. It just identifies the pieces.

There's one small subtlety I like: the `||` operator. When the tokenizer sees a `|`, it has to peek ahead — is the next character also a `|`? If so, it's a single parallel operator, not two separate symbols. It's a tiny decision, but it's the only place where one character of lookahead matters during tokenization.

## The first token always tells you what to do

Here's something that surprised me when I first built this: session types are *really* easy to parse.

Most programming languages need complex parsing strategies — backtracking, precedence climbing, operator tables. Session types don't. And the reason is elegant: **the first token always tells you what you're looking at.**

See `&`? You're parsing a branch — read the `{`, then pairs of `name: type` separated by commas, then `}`.

See `+`? Same structure, but it's a selection.

See `(`? You're in a group that might contain `||` — parse the left side, check for the parallel operator, parse the right side.

See `rec`? Read the variable name, the dot, and the body.

See `end`? You're done. Return a terminal node.

See a plain word like `X`? That's a variable — a reference back to some enclosing `rec`.

That's it. One token of lookahead is always enough. In parsing theory, this makes session types an *LL(1)* grammar — the simplest kind to parse, the kind you can write by hand in an afternoon without any parser generators or grammar tools.

I find that satisfying. The grammar is expressive enough to describe any protocol — SMTP, OAuth, database connections, iterators — but simple enough that a parser for it fits in about a hundred lines of code.

## What comes out: a tree that mirrors the protocol

The parser produces a tree. Not a string, not a flat list — a tree where the structure mirrors the protocol structure exactly.

Take `&{open: +{OK: end, ERROR: end}}`. The parser turns it into:

```tree
Branch
 └─ "open" → Select
               ├─ "OK"    → End
               └─ "ERROR" → End
```

The outer branch becomes the root. Its single method "open" leads to a select node with two outcomes. Each outcome leads to a terminal.

For the Iterator — `rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}` — the tree is deeper:

```tree
Rec(var="X")
 └─ Branch
      └─ "hasNext" → Select
                       ├─ "TRUE"  → Branch
                       │             └─ "next" → Var("X")
                       └─ "FALSE" → End
```

Notice the `Var("X")` at the bottom. It points back up to the `Rec` at the top — that's how the tree represents a loop. The structure isn't really a tree at all; it's a graph with a cycle. And that cycle is exactly the "check-then-iterate" loop you'd write in Java.

## When things go wrong: error messages that actually help

I spent more time on error messages than I'd like to admit. Early on, the parser would just say "parse error at position 23" — completely useless when your protocol is a nested mess of braces and colons.

Now it tells you what it expected and what it got. If you write `&{open end}`, it says: "Expected ':' after method name 'open', got 'end'." If you write `rec . body`, it says: "Expected variable name after 'rec', got '.'."

These messages matter more than they seem. When you're modelling a complex API and something doesn't parse, the difference between "syntax error" and "I expected a colon after 'authenticate'" is the difference between five seconds of debugging and five minutes of staring at the screen.

## One job, nothing more: syntax in, tree out

Here's a design decision I'm proud of: the parser does *one thing* and refuses to do anything else.

It checks syntax. Period. It doesn't check whether your recursive types actually terminate. It doesn't check whether your variables are bound. It doesn't check whether your parallel compositions are well-formed.

All of that happens later, in separate stages of the pipeline — the termination checker, the scope analyzer, the well-formedness validator. Each stage has a single responsibility, and the parser's responsibility is narrow: text in, tree out, nothing more.

This means you can parse a session type that's syntactically valid but semantically broken — like `rec X . X`, which would loop forever, or `&{a: Y}` where `Y` is never defined. The parser accepts both of those. Later stages catch them.

I know some people prefer to catch everything in one pass. But separating concerns made every individual piece easier to test, easier to understand, and easier to change when the language evolved.

## The round-trip test: parse, print, parse again

The parser has an inverse: a pretty printer. You can parse a string into a tree, then print the tree back into a string. And the result is always the same — `parse(print(parse(s))) == parse(s)` for every valid input.

That sounds like a trivial property, but it's actually a powerful testing tool. If the round-trip ever breaks, something fundamental is wrong. I run this check across thousands of generated session types, and it catches bugs that unit tests miss — subtle issues with operator precedence, whitespace handling, or corner cases in how commas are placed inside nested branches.

## Next: where the lattice appears

In the next post, we leave the world of syntax behind. We're going to take these trees and build something from them — a *state space*, a graph where every node is a possible state of the protocol, and every edge is a method call that moves you forward.

That's where the lattice appears. And honestly? That's where I got hooked.

*Part 3 of Arc 1 — Foundations*
""";
}
