# Graph Report - .  (2026-11-09)

## Corpus Check
- Corpus is ~1,915 words - fits in a single context window. You may not need a graph.

## Summary
- 101 nodes · 95 edges · 13 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output
- Edge kinds: method: 63 · contains: 18 · imports: 6 · inherits: 6 · implements: 2


## Input Scope
- Requested: auto
- Resolved: all (source: default-auto)
- Included files: 15 · Candidates: recursive
- Excluded: 0 untracked · 0 ignored · 0 sensitive · 0 missing committed
## God Nodes (most connected - your core abstractions)
1. `ProductEvent` - 23 edges
2. `ProductEvent` - 23 edges
3. `ProductEventRepository` - 6 edges
4. `ProductEventRepository` - 6 edges
5. `ReportServiceTest` - 6 edges
6. `ReportService` - 3 edges
7. `ReportService` - 3 edges
8. `ReportControllerTest` - 3 edges
9. `ReportController` - 2 edges
10. `ReportServicePort` - 2 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 1 - "Community 1"
Cohesion: 0.08
Nodes (1): ProductEvent

### Community 3 - "Community 3"
Cohesion: 0.27
Nodes (5): ProductEventRepository, ReactiveMongoRepository, ProductEvent, String, ProductEventRepository

### Community 7 - "Community 7"
Cohesion: 0.67
Nodes (1): ReportController

### Community 2 - "Community 2"
Cohesion: 0.18
Nodes (6): ReportService, ReportServicePort, DateRange, AggregatedReportResponse, ReportController, ReportService

### Community 8 - "Community 8"
Cohesion: 0.67
Nodes (1): ReportServicePort

### Community 9 - "Community 9"
Cohesion: 0.67
Nodes (1): ReportsServiceApplication

### Community 11 - "Community 11"
Cohesion: 1.00
Nodes (1): AggregatedReportResponse

### Community 12 - "Community 12"
Cohesion: 1.00
Nodes (1): DateRange

### Community 6 - "Community 6"
Cohesion: 0.67
Nodes (1): ReportServicePort

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (1): ProductEvent

### Community 5 - "Community 5"
Cohesion: 0.50
Nodes (1): ReportControllerTest

### Community 4 - "Community 4"
Cohesion: 0.29
Nodes (1): ReportServiceTest

### Community 10 - "Community 10"
Cohesion: 0.67
Nodes (1): ReportsServiceApplication

## Knowledge Gaps
- **4 isolated node(s):** `DateRange`, `AggregatedReportResponse`, `AggregatedReportResponse`, `DateRange`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 1`** (1 nodes): `ProductEvent`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 7`** (1 nodes): `ReportController`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 8`** (1 nodes): `ReportServicePort`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 9`** (1 nodes): `ReportsServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 11`** (1 nodes): `AggregatedReportResponse`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 12`** (1 nodes): `DateRange`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 6`** (1 nodes): `ReportServicePort`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 0`** (1 nodes): `ProductEvent`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 5`** (1 nodes): `ReportControllerTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 4`** (1 nodes): `ReportServiceTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 10`** (1 nodes): `ReportsServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What connects `DateRange`, `AggregatedReportResponse`, `AggregatedReportResponse` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.08333333333333333 - nodes in this community are weakly interconnected._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.08333333333333333 - nodes in this community are weakly interconnected._