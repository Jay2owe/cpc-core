# cpc-core

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.21822703.svg)](https://doi.org/10.5281/zenodo.21822703)

The Centre-Particle Coincidence engine, as an embeddable module.

**Status:** built. 35 tests green. Shaded into CPC 1.4.0+ and verified on a bare
classpath.
**Pattern:** `../PLUGIN_CORE_PATTERN.md`
**Depends on:** `oc3d-core` 0.2.0
**Never shipped as a jar.**

**0.2.0 is a chassis bump only.** `src/main` is byte-identical to 0.1.0 — the
release exists so consumers can hold one chassis version rather than two. This
engine reads centroids and label semantics and touches none of the shape
features 0.2.0 of the chassis changed, so no coincidence number moves.

---

## What this is

CPC's analysis engine with the dialog, the tables and the entry class stripped
out, so any other plugin can compile it in and offer centroid coincidence
without the user installing CPC.

**The test:** is object A's centroid inside an object in channel B? Optionally
intensity-weighted (centre of mass rather than geometric centroid).

## Using it

The short version, for a plugin that has already measured its objects:

```java
// What the plugin already did for its own table.
LabelFeatureAccumulator.Result measured = LabelFeatureAccumulator.scan(labels, null);

// Two more lines to offer coincidence. No pixels of `labels` are re-read.
Channel mine = Channel.measured("cells", labels, measured, false);
DirectionResult hits = CentroidCoincidence.test(mine, Channel.of("puncta", partner));

// Appending a column: walk your own rows, ask about each label.
for (int label : myTableLabels) {
    table.addValue("Punctum", hits.partnerFor(label));
}
```

`hits.objects()` comes back in the same ascending-label order as
`measured.labelsSorted()`, so the rows line up without a join. That is the whole
point of taking centroids rather than an image: a plugin that recomputed here
could disagree with itself about how many objects there are, and the
disagreement would show up as a table with the wrong number of rows rather than
as an error.

For a standalone run over several channels:

```java
List<Channel> channels = Arrays.asList(
        Channel.of("ChA", labelsA), Channel.of("ChB", labelsB));
CoincidenceResult result = PairwiseCoincidenceRunner.run(channels, true);
List<MultiTargetResult> multi = MultiTargetSummary.run(channels);
```

## Scope

### In

```
cpc-core                        BSD-3, depends on oc3d-core only
  Channel                       a named label image plus its objects' positions
  CentroidCoincidence           the test: point-in-object, rounded, bounds-checked
  PairwiseCoincidenceRunner     all pairs, both directions, 2+ channels
  MultiTargetSummary            source-anchored combination patterns
  MultiTargetResult             per-object target map, plus anyCount and noneCount
  CombinationPattern            one combination and how many objects matched it
  CoincidenceResult             the pairwise model, looked up by channel pair
  DirectionResult               one direction, with the inverse "contains" view
  CoincidenceObject             one object's row, immutable
  CentroidMapBuilder            cross markers at partner centroids
```

### Out — lives in `oc3d-core`

`LabelUtils` / ROI ingest, `CentroidScan` and `LabelFeatureAccumulator`, macro
token parsing, `RegexGroupDiscovery`, `ToggleSwitch` and the shared dialog
widgets. These are chassis, needed by every plugin of this kind.

### Out — stays in the CPC plugin

Entry class, `plugins.config`, public API (`cpc.CPC`), `CPCDialog` and its
centroid-specific panels, auto-save tree wiring, ImageJ `ResultsTable`
construction.

**Cores return result models, not ImageJ tables.** The plugin decides how to
display. That is what makes the core embeddable in a plugin with a completely
different table layout — such as 3D Objects Counter+, which appends columns to
its existing per-object table rather than opening a new one.

## No dialog, no Swing, no `IJ.error`

`cpc-core` runs headless. It throws; the plugin decides how to present.
`EmbeddabilityTest` asserts this against the compiled bytecode rather than
trusting the source: no Swing, no dialogs, no `System.exit`, no `ResultsTable`
in any signature, no `java.io.File` in any signature, and no reflection by name.

## The `None` row

The earlier draft of this file said the `None` and `— Any —` rows were always
present. **That was true of Volumetric Colocalization and false of CPC** — its
multi-target summary emitted a `None` row only when some object matched nothing,
confirmed against the 1.4.0 goldens. So the row vanished on exactly the datasets
where everything colocalized, and a script indexing rows positionally read the
totals row instead.

Both halves now hold it. CPC always writes the row, at zero if need be, and the
model carries it unconditionally: `MultiTargetResult.noneCount()` and
`anyCount()` are always defined. A consumer should read those rather than search
`patterns()`, which still lists only combinations that actually occurred.

## Consuming it

```xml
<artifactSet>
  <includes>
    <include>io.github.jay2owe:cpc-core</include>   <!-- Maven COORDINATE -->
    <include>io.github.jay2owe:oc3d-core</include>
  </includes>
</artifactSet>
<relocations>
  <relocation>
    <pattern>sc.fiji.cpc.core</pattern>             <!-- Java PACKAGE -->
    <shadedPattern>sc.fiji.oc3dplus.internal.cpc</shadedPattern>
  </relocation>
</relocations>
```

Two different strings. A wrong coordinate in `<artifactSet>` does not error — it
matches nothing, the build succeeds, and the jar ships without the core classes,
surfacing as `NoClassDefFoundError` the moment a user clicks Run.

Never relocate `cpc.CPC` or anything else called from Java or macros.

**Add `<minimizeJar>true</minimizeJar>`.** `oc3d-core` carries the labeller, the
full measurement pass, object maps, CSV scoring and the dialog model; a plugin
that wants centroid coincidence reaches perhaps six of its classes. Without
minimisation CPC shipped 69 of them and the jar went from 89 KB to 227 KB;
with it, 18 classes and 139 KB. Safe because nothing in either core looks a
class up by name — `EmbeddabilityTest` asserts that against the bytecode.

**Known shading hazard.** Relocation rewrites strings in the constant pool that
look like the relocated package. `oc3d-core` names five tuning and image
properties with literals beginning `sc.fiji.oc3d.core.` — `maxDenseLabel`,
`maxOverlayLabels`, `optionalMapMemoryReserveBytes`, `overlaySkipped`,
`overlaySkippedReason` — and all five are silently renamed into the consumer's
namespace. CPC is unaffected because it calls neither `ObjectMapBuilder` nor
`LabelFeatureAccumulator`, but a consumer that does will find the documented
property name ignored, and two plugins will not read each other's image
properties. Fix before the first consumer relies on them: give those constants
names that do not begin with a relocated package.

## Licence

BSD-3-Clause — see `LICENSE`, with attribution in `NOTICE`. Links
`net.imagej:ij` only, via `oc3d-core`. Embedding it adds no obligation to any
consumer.

## Citation

This module is archived on Zenodo so that plugins which compile it in can be
rebuilt from their own archives alone, without depending on GitHub staying
reachable.

| | DOI | Built against |
| --- | --- | --- |
| Concept (always resolves to the latest release) | [`10.5281/zenodo.21822703`](https://doi.org/10.5281/zenodo.21822703) | — |
| v0.1.0 | [`10.5281/zenodo.21822704`](https://doi.org/10.5281/zenodo.21822704) | `oc3d-core` 0.1.0 ([`10.5281/zenodo.21822702`](https://doi.org/10.5281/zenodo.21822702)) |
| v0.2.0 | [`10.5281/zenodo.21933269`](https://doi.org/10.5281/zenodo.21933269) | `oc3d-core` 0.2.0 ([`10.5281/zenodo.21823678`](https://doi.org/10.5281/zenodo.21823678)) |

> Malcolm, J. (2026). *cpc-core: Embeddable centroid-in-object coincidence
> engine* (Version 0.2.0) [Computer software]. Zenodo.
> https://doi.org/10.5281/zenodo.21933269

`oc3d-core` is the only dependency other than `net.imagej:ij`, and rebuilding a
given version of this module needs the chassis version in the last column —
they move together, never independently.

Cite the **version** DOI when reproducibility is the point — the concept DOI
follows this module forward to releases a given plugin was never built against.

Most users should not cite this directly. It is a build-time library that is
never shipped as a jar and never appears on an update site; cite the plugin
that embeds it. CPC 1.5.0 embeds v0.1.0
([`10.5281/zenodo.21812272`](https://doi.org/10.5281/zenodo.21812272)).

## Ship gate

`../oc3d-core/EQUIVALENCE_HARNESS.md`, run from CPC —
`CPC/src/test/java/cpc/equivalence/`. 409 comparisons against goldens captured
from CPC 1.4.0 at `39895e0`: **zero Tier 1 differences**, with six declared
Tier 3 changes listed in `TierContract.java` and in CPC's CHANGELOG.

## Plan

`../../CPC/docs/CPC_CORE_MIGRATION_PLAN.md`
