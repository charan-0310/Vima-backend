# Vima-backend static analysis report

**Bundled HTML + logs:** [`reports/static-analysis-bundle.md`](reports/static-analysis-bundle.md) (includes [`reports/spotbugs-report.html`](reports/spotbugs-report.html) and [`reports/dependency-analyze.log`](reports/dependency-analyze.log)).

**Master plan:** [Tech debt and quality plan](tech-debt-and-quality-plan.md) — combines this narrative with refactor backlog and tooling priorities.

**Generated:** 2026-05-13 (local run)  
**Module:** `com.vimainsurance:vimaadmin` (`Vima-backend/pom.xml`)  
**Java:** 21 (Spring Boot 3.4.6 parent)

This document records **one-off** static analysis runs. It is not a substitute for CI quality gates (Sonar, OWASP Dependency-Check, Error Prone). Regenerate artifacts with the commands in [How to reproduce](#how-to-reproduce).

---

## Summary

| Signal | Result |
| --- | --- |
| **Compile** | `mvn -DskipTests compile` — success |
| **Dependency tree** | Written to `target/dependency-tree.txt` |
| **`dependency:analyze`** | Completed with warnings (see [Maven dependency analyze](#maven-dependency-analyze)); many items are **false positives** for Spring Boot fat usage |
| **SpotBugs** | **963** `BugInstance` findings in `target/spotbugsXml.xml` (plugin `4.8.6.4`, default detector set) |
| **OWASP Dependency-Check** | **Not run** here (NVD download / runtime); recommend in CI or manual `dependency-check:check` with org credentials |
| **SonarQube / Error Prone** | **Not configured** in `pom.xml` at time of report |

---

## Compiler notes (from `dependency:analyze` compile phase)

Maven logged the following **informational** compiler hints (not failures):

- **Deprecation:** `RateLimitAspect.java` — uses or overrides a deprecated API (`-Xlint:deprecation` for detail).
- **Unchecked:** `EmployeeService.java` — unchecked or unsafe operations (`-Xlint:unchecked` for detail).
- **Tests unchecked:** `ClaimsServiceImplTest.java` — same.

**Suggestion:** add a dedicated profile or CI step with `-Xlint:deprecation -Xlint:unchecked` (and optionally `-Werror` only after backlog is cleared).

---

## Maven dependency analyze

Command: `mvn dependency:analyze -DfailOnWarning=false` (log: `target/dependency-analyze.log`).

### Interpretation for Spring Boot

- **“Used undeclared dependencies”** — code references types from transitive JARs (e.g. Spring, Tomcat, AWS SDK internals). Fix by **declaring** direct dependencies only where you intend a stable API contract; many entries are normal for Spring apps.
- **“Unused declared dependencies”** — Maven’s bytecode analysis often marks **Spring Boot starters** as “unused” because usage is indirect via auto-configuration. **Do not remove starters** based on this output alone.
- **“Non-test scoped test only dependencies”** — flags `junit-jupiter-api` as compile-scoped via transitives; verify `spring-boot-starter-test` / BOM alignment if you tighten scopes.

### Direct dependencies worth manual review (security / hygiene)

These are **declared** in `pom.xml` and merit periodic CVE and compatibility review (not an automated verdict in this report):

| Artifact | Note |
| --- | --- |
| `org.apache.httpcomponents:httpclient:4.5.14` | Legacy Apache HttpClient 4.x line; consider migrating callers to **HttpClient 5** or Spring `RestClient` / WebClient only |
| `javax.servlet:javax.servlet-api:4.0.1` (provided) | **javax** namespace alongside **Jakarta** Spring Boot 3 stack — confirm still required; prefer removal if unused |
| `org.json:json` | Third-party JSON lib; ensure no overlap with Jackson-only usage |
| `io.jsonwebtoken:jjwt-*:0.11.5` | Track jjwt advisories; align `jjwt-api` / `impl` / `jackson` versions |
| `org.keycloak:keycloak-admin-client:26.0.0` | Large surface; track Keycloak / client CVEs |

---

## SpotBugs (default rules, main + dependencies on aux classpath)

**Artifact:** `target/spotbugsXml.xml` (~3 MB single-line XML).  
**Count:** **963** findings.

### By SpotBugs `priority` attribute

| Priority | Count | Typical meaning |
| --- | --- | --- |
| 1 | 18 | High confidence / serious |
| 2 | 945 | Medium |

### Top bug types (all priorities)

| Count | Type | Short description |
| --- | --- | --- |
| 515 | `EI_EXPOSE_REP2` | May expose internal representation by storing mutable object in field |
| 277 | `EI_EXPOSE_REP` | May expose internal representation by returning reference to mutable object |
| 55 | `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | Possible null dereference from return value |
| 20 | `DLS_DEAD_LOCAL_STORE` | Dead store to local |
| 19 | `REC_CATCH_EXCEPTION` | Catches `Exception` |
| 18 | `PA_PUBLIC_PRIMITIVE_ATTRIBUTE` | Mutable public primitive array field |
| 12 | `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` | Redundant nullcheck on non-null |
| 6 | `RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE` | Redundant nullcheck |
| 6 | `BX_UNBOXED_AND_COERCED_FOR_TERNARY_OPERATOR` | Unboxing/coercion in ternary |
| 6 | `DM_DEFAULT_ENCODING` | Reliance on default encoding |

Remaining types are **≤4** occurrences each (e.g. `DMI_HARDCODED_ABSOLUTE_FILENAME`, `RV_RETURN_VALUE_IGNORED_BAD_PRACTICE`, `CT_CONSTRUCTOR_THROW`, …).

### Top classes by finding count (first `Class` element per instance)

| Count | Class / inner class |
| --- | --- |
| 32 | `VendorMasterDataConfig$VendorMasterData` |
| 30 | `Claim` |
| 21 | `Endorsement` |
| 18 | `EmployeeUploadDto` |
| 17 | `DocumentServiceImpl` |
| 15 | `EmailRequest` |
| 15 | `EnrollmentContextDto` |
| 15 | `Deals` |
| 15 | `EnrollmentSubmission` |
| 15 | `EnrollmentUploadParserUtil$EnrollmentParseResult` |
| 14 | `CustomerServiceImpl` |
| 12 | `PolicyResponseDto` |
| 12 | `Policy` |
| 12 | `PolicyServiceImpl` |

### Recommended follow-up for SpotBugs

1. **Triage `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`** — Very common on **entities/DTOs** with getters returning mutable collections or arrays. Options: defensive copies in getters, immutable collections, SpotBugs filter/exclude for generated or persistence-only types (document rationale).
2. **Address `REC_CATCH_EXCEPTION`** — Replace broad `catch (Exception)` in hot paths with typed handling or `@ControllerAdvice` mapping.
3. **Null safety (`NP_*`)** — Fix or add `@Nullable` / Optional discipline where high confidence.
4. **CI integration** — Add `spotbugs-maven-plugin` with `check` goal and a **baseline file** or gradual `effort` threshold to avoid blocking all PRs until backlog is triaged.

---

## Viewing SpotBugs in a pretty form

`target/spotbugsXml.xml` is dense, single-line XML meant for tools. For a **human-readable** report:

1. Run SpotBugs (from `Vima-backend`):  
   `mvn -q -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs`
2. Open **`target/site/spotbugs.html`** in a browser (default SpotBugs HTML: grouped bugs, expandable explanations).
   - macOS: `open target/site/spotbugs.html`
   - Or open `Vima-backend/target/site/spotbugs.html` via Finder / drag into Chrome.

**Alternatives**

- **Format XML in the editor**: open `spotbugsXml.xml` in Cursor and run **Format Document** (large files may lag).
- **Command-line pretty print**:  
  `xmllint --format target/spotbugsXml.xml > target/spotbugs-pretty.xml`
- **SpotBugs desktop**: [spotbugs.github.io](https://spotbugs.github.io/) → install → **File → Open** `spotbugsXml.xml`.

---

## How to reproduce

From repository root:

```bash
cd Vima-backend

# Compile
mvn -q -DskipTests compile

# Dependency tree (for supply-chain review)
mvn -q dependency:tree -DoutputFile=target/dependency-tree.txt

# Dependency analyze (noisy on Spring Boot — read with care)
mvn dependency:analyze -DfailOnWarning=false | tee target/dependency-analyze.log

# SpotBugs report (one-shot plugin version; upgrade over time)
mvn -q -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs
# XML: target/spotbugsXml.xml  |  Pretty HTML: target/site/spotbugs.html
```

**OWASP Dependency-Check** (not executed for this report):

```bash
mvn org.owasp:dependency-check-maven:check
```

Use a **suppression file** with expiry and CI caching for NVD data.

---

## Next steps (aligned with tech-debt plan)

1. Add **OWASP Dependency-Check** or **Dependabot** + periodic `dependency:tree` diff on releases.
2. Add **SpotBugs** (and optionally **FindSecBugs**) to `pom.xml` under a `ci` profile with `failOnError` phased in after baseline triage.
3. Add **SonarQube** or **SonarLint** + quality gate for security hotspots and duplication on large services.
4. Consider **Error Prone** after P0 supply-chain tooling is stable.

---

## Appendix: report files (local, gitignored under `target/`)

| Path | Description |
| --- | --- |
| `target/dependency-tree.txt` | Full Maven dependency tree |
| `target/dependency-analyze.log` | `dependency:analyze` Maven log |
| `target/spotbugsXml.xml` | SpotBugs XML report (963 issues) |
| `target/site/spotbugs.html` | SpotBugs HTML report (open in browser) |

Regenerate after dependency or code changes; do not commit `target/` outputs to git.
