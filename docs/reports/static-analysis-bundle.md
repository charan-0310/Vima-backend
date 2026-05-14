# Vima-backend static analysis bundle

**Master plan:** [../tech-debt-and-quality-plan.md](../tech-debt-and-quality-plan.md) — tech debt backlog, P0–P3 tooling, and refactor split map.

**Generated:** 2026-05-13  
**Module:** `com.vimainsurance:vimaadmin` (`Vima-backend/pom.xml`)  
**Java:** 21 (Spring Boot 3.4.6 parent)

This folder bundles the **written analysis** with **committed artifacts** from the same run:

| Artifact | Path (this folder) |
| --- | --- |
| **SpotBugs HTML (pretty view)** | [`spotbugs-report.html`](spotbugs-report.html) — open in a browser (double-click or `open spotbugs-report.html` on macOS) |
| **Maven dependency analyze log** | [`dependency-analyze.log`](dependency-analyze.log) |
| **Narrative report (duplicate of repo doc)** | This file; the same content also lives at [`../static-analysis-report.md`](../static-analysis-report.md) for the main docs index |

Regenerate SpotBugs HTML and logs from `Vima-backend` with the commands in [How to reproduce](#how-to-reproduce), then copy into `docs/reports/` if you want an updated snapshot in git.

---

## Summary

| Signal | Result |
| --- | --- |
| **Compile** | `mvn -DskipTests compile` — success |
| **Dependency tree** | Generated under `target/dependency-tree.txt` (not copied here; large) |
| **`dependency:analyze`** | Log copied as [`dependency-analyze.log`](dependency-analyze.log); many warnings are **false positives** for Spring Boot |
| **SpotBugs** | **963** findings; HTML snapshot: [`spotbugs-report.html`](spotbugs-report.html) |
| **OWASP Dependency-Check** | Not run in this bundle; use CI or `dependency-check:check` |
| **SonarQube / Error Prone** | Not configured in `pom.xml` at time of report |

---

## Compiler notes (from `dependency:analyze` compile phase)

Maven logged the following **informational** compiler hints (not failures):

- **Deprecation:** `RateLimitAspect.java` — uses or overrides a deprecated API (`-Xlint:deprecation` for detail).
- **Unchecked:** `EmployeeService.java` — unchecked or unsafe operations (`-Xlint:unchecked` for detail).
- **Tests unchecked:** `ClaimsServiceImplTest.java` — same.

**Suggestion:** add a CI profile with `-Xlint:deprecation -Xlint:unchecked` (and optionally `-Werror` after backlog is cleared).

---

## Maven dependency analyze

See the full Maven output in [`dependency-analyze.log`](dependency-analyze.log).

### Interpretation for Spring Boot

- **“Used undeclared dependencies”** — references to transitive types; many are normal. Declare direct deps only where you want a stable API contract.
- **“Unused declared dependencies”** — bytecode analysis often marks **Spring Boot starters** as unused (usage is indirect). **Do not remove starters** from this output alone.
- **“Non-test scoped test only dependencies”** — e.g. JUnit on compile path via transitives; verify BOM / scopes if tightening.

### Direct dependencies worth manual review (security / hygiene)

| Artifact | Note |
| --- | --- |
| `org.apache.httpcomponents:httpclient:4.5.14` | Legacy HttpClient 4.x; consider HttpClient 5 or Spring `RestClient` / WebClient |
| `javax.servlet:javax.servlet-api:4.0.1` (provided) | **javax** alongside **Jakarta** — confirm still required |
| `org.json:json` | Third-party JSON; avoid overlap with Jackson-only usage |
| `io.jsonwebtoken:jjwt-*:0.11.5` | Track advisories; keep api/impl/jackson aligned |
| `org.keycloak:keycloak-admin-client:26.0.0` | Track Keycloak / client CVEs |

---

## SpotBugs (default rules)

**Count:** **963** `BugInstance` entries in the XML produced by the same run (XML stays under `target/spotbugsXml.xml` when you rebuild).

### By priority

| Priority | Count | Typical meaning |
| --- | --- | --- |
| 1 | 18 | High confidence / serious |
| 2 | 945 | Medium |

### Top bug types

| Count | Type | Short description |
| --- | --- | --- |
| 515 | `EI_EXPOSE_REP2` | May expose internal representation by storing mutable object in field |
| 277 | `EI_EXPOSE_REP` | May expose internal representation by returning mutable object |
| 55 | `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | Possible null dereference from return value |
| 20 | `DLS_DEAD_LOCAL_STORE` | Dead store to local |
| 19 | `REC_CATCH_EXCEPTION` | Catches `Exception` |
| 18 | `PA_PUBLIC_PRIMITIVE_ATTRIBUTE` | Mutable public primitive array field |
| 12 | `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` | Redundant nullcheck on non-null |
| 6 | `RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE` | Redundant nullcheck |
| 6 | `BX_UNBOXED_AND_COERCED_FOR_TERNARY_OPERATOR` | Unboxing/coercion in ternary |
| 6 | `DM_DEFAULT_ENCODING` | Reliance on default encoding |

### Top classes by finding count

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

### Recommended follow-up

1. Triage **`EI_EXPOSE_REP` / `EI_EXPOSE_REP2`** on entities/DTOs (defensive copies, immutable collections, or documented SpotBugs filters).
2. Reduce **`REC_CATCH_EXCEPTION`** (narrow catches, `@ControllerAdvice`).
3. Fix high-confidence **`NP_*`** or improve null annotations.
4. Add SpotBugs to CI with a baseline or phased `failOnError`.

---

## Viewing SpotBugs (pretty HTML)

Open **[`spotbugs-report.html`](spotbugs-report.html)** in a browser (same content SpotBugs writes to `target/site/spotbugs.html` after a run).

---

## How to reproduce and refresh this bundle

```bash
cd Vima-backend

mvn -q -DskipTests compile
mvn -q dependency:tree -DoutputFile=target/dependency-tree.txt
mvn dependency:analyze -DfailOnWarning=false | tee target/dependency-analyze.log
mvn -q -DskipTests compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs

# Refresh committed copies under docs/reports/
cp target/site/spotbugs.html docs/reports/spotbugs-report.html
cp target/dependency-analyze.log docs/reports/dependency-analyze.log
```

**OWASP Dependency-Check** (optional):

```bash
mvn org.owasp:dependency-check-maven:check
```

---

## Next steps

1. OWASP Dependency-Check or Dependabot on releases.
2. SpotBugs (+ optional FindSecBugs) in `pom.xml` under a `ci` profile.
3. SonarQube or SonarLint quality gate.
4. Error Prone after supply-chain tooling is stable.

---

## Files in this folder

| File | Description |
| --- | --- |
| `static-analysis-bundle.md` | This narrative + links to HTML and logs |
| `spotbugs-report.html` | SpotBugs HTML report snapshot |
| `dependency-analyze.log` | Maven `dependency:analyze` log snapshot |
