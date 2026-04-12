# Modularity Canonical Fixtures (Java twin)

Java-side mirror of
`reticulate/tests/benchmarks/modularity/`. Per
`feedback_java_python_parity` the two directories must carry the
same three fixtures with character-identical session type strings:

| Fixture | Lattice class | `isModular()` | `isDistributive()` | certificate |
|---|---|---|---|---|
| `distributive_reference.session` | Distributive 3×3 grid | ✅ | ✅ | `"syntactic"` |
| `m3_canonical.session` | Modular, not distributive | ✅ | ❌ | `"semantic"` |
| `n5_canonical.session` | Not modular (pentagon) | ❌ | ❌ | `"semantic"` |

> **Task 44 fixed:** the false-N5 bug in
> `DistributivityChecker.checkDistributive` on grid lattices is
> closed. The fix added the missing sublattice-closure check
> ({@code meet(a,c) = bot && join(b,c) = top}) to the pentagon
> search, mirroring the Python reference. The
> `distributive_reference` fixture is now the 9-state 3×3 grid
> `(a . b . end || c . d . end)`. See
> `docs/observatory/campaigns/modularity/logbook.md` entry
> dated 2026-04-12 for the full root cause and fix narrative.

The loader is
`bica/src/test/java/com/bica/reborn/modularity/ModularityFixtures.java`
and the loader tests live in
`bica/src/test/java/com/bica/reborn/modularity/ModularityFixtureTest.java`.

See the Python-side `README.md` for the full Birkhoff-hierarchy
rationale and the regression policy; the two documents are
structurally identical and should be updated together.
