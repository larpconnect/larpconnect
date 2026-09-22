---
name: njall-tool-guide
description: Standards for WSL execution, direct command invocation, approved POSIX tools (jq, find, tree, xargs, sed, awk, grep), and strict avoidance of shell antipatterns in Njall.
---

# Project Njall: WSL & CLI Tooling Guide

This skill provides mandatory standards and best practices for bridging between the Windows agent
environment and the WSL2 Linux development subsystem in **Njall**. It details command-line
execution rules, highlights approved and encouraged POSIX tools, documents strict prohibitions,
and provides recipes for common development workflows.

---

## 1. Direct WSL Invocation Standard

When running commands inside the Linux subsystem from a Windows host shell (PowerShell),
**invoke binaries directly through `wsl <binary> [args...]`**.

```
+───────────────────────────────────────────────────────────────────────────────+
|                                 WINDOWS HOST                                  |
|  $ wsl ./gradlew check                                                        |
+───────────────────────────────────────┬───────────────────────────────────────+
                                        │ (Direct Process Argument Passing)
+───────────────────────────────────────▼───────────────────────────────────────+
|                               WSL LINUX GUEST                                 |
|  $ ./gradlew check                                                            |
+───────────────────────────────────────────────────────────────────────────────+
```

### Why to Avoid `wsl bash -c "..."`

> [!WARNING]
> **Do not wrap commands in `wsl bash -c "..."` when they can be run directly.**

1. **Quote Collisions & Escaping Hazards**: Passing quotes inside PowerShell to `bash -c "..."`
   creates a double-quoting nested string where quotes get stripped or misinterpreted (`\"` vs `'`).
2. **Subshell Overhead**: Spawns an unnecessary intermediate shell process.
3. **Broken Sandbox Approvals**: Prefix-based command matchers in IDE sandboxes fail on arbitrary
   nested strings, forcing manual confirmation for every invocation.

#### Comparison

| Scenario | ❌ Antipattern (`bash -c`) |  Correct Direct Invocation |
| :--- | :--- | :--- |
| **Run Quality Suite** | `wsl bash -c "./gradlew check"` | `wsl ./gradlew check` |
| **Run Module Tests** | `wsl bash -c "./gradlew :server:test"` | `wsl ./gradlew :server:test` |
| **Search Code** | `wsl bash -c "grep -rn 'class ' src/"` | `wsl grep -rn "class " src/` |
| **Format Check** | `wsl bash -c "./gradlew spotlessCheck"` | `wsl ./gradlew spotlessCheck` |

*Exception*: Use `wsl bash -c "..."` **only** when shell features such as Unix pipes (`|`),
redirections (`>`), or compound operators (`&&`) are required and cannot be handled by a dedicated
script.

---

## 2. The No-`.exe` Rule in WSL

> [!IMPORTANT]
> **Linux binaries DO NOT have `.exe` extensions, and Windows batch scripts (`.bat`/`.cmd`) must not be run inside WSL.**

Calling `wsl gradlew.bat` or `wsl java.exe` instructs WSL to reach *back out* into Windows to run
the Windows binary/batch script via interop. This breaks Linux path resolution, targets the wrong
operating system ABI, and degrades performance. In WSL, always invoke the Linux wrapper script
`./gradlew` or native Linux binaries directly.

```powershell
# ❌ INCORRECT (Invokes Windows binaries or batch scripts via interop)
wsl gradlew.bat check
wsl java.exe --version
wsl git.exe status

#  CORRECT (Invokes native Linux scripts and ELF binaries)
wsl ./gradlew check
wsl java --version
wsl git status
```

---

## 3. Encouraged POSIX Toolchain

Leverage native, high-performance Unix tools for text manipulation, stream filtering, and
filesystem discovery.

### `jq`: JSON Parsing & Inspection
Use `jq` to inspect JSON manifests, OpenAPI specs, and configuration dumps:
```powershell
# Query specific fields from OpenSpec status JSON
wsl jq '.artifacts.specs.status' openspec/changes/current/status.json

# Parse API specification details or OpenAPI definitions
wsl jq '.info.title, .info.version' api/src/main/resources/openapi.json
```

### `find`: Filesystem Searching
Use `find` to discover source files, fixtures, or configurations without traversal scripts:
```powershell
# Find all Java source files excluding build and gradle caches
wsl find . -name "*.java" -not -path "*/build/*" -not -path "*/.gradle/*"

# Find Cucumber feature specification files
wsl find . -name "*.feature"
```

### `tree`: Visualizing Workspace Topologies
Use `tree` with depth limiting and filtering to inspect Gradle module layouts:
```powershell
# View directory structure 2 levels deep, ignoring build and git
wsl tree -L 2 --dirsfirst -I "build|.gradle|.git"
```

### `xargs`: Composing Batch Pipelines
Use `xargs` to batch file lists into CLI tools:
```powershell
# Run grep across all Java files discovered by find
wsl find src -name "*.java" | wsl xargs grep -n "class "
```

### `sed`, `awk`, `grep`, `cut`: Stream Processing
```powershell
# Fast pattern searching with line numbers
wsl grep -rn "public class" src/

# Extended regex search for type declarations
wsl grep -E "(record|class|interface|enum) [A-Za-z0-9_]+" src/

# Stream editing / line replacement
wsl sed -E 's/old_identifier/new_identifier/g' file.txt

# Column slicing from space/tab-delimited output
wsl awk '{print $1, $3}' metrics.txt

# Delimiter field slicing
wsl cut -d: -f1,2 error_log.txt
```

---

## 4. Strict Prohibitions & Discouraged Patterns

### ❌ FORBIDDEN: Dynamic Variable Executable Indirection

> [!CAUTION]
> **Never write or execute dynamic command indirection patterns such as:**
> ```bash
> # FORBIDDEN PATTERN
> CLI_TOOL=${CLI_TOOL:-/path/to/tool}
> $CLI_TOOL (args)
> ```

**Why this is strictly forbidden**:
1. **Breaks Sandbox Auto-Approval**: The sandbox security policy inspects command prefixes (e.g.
   `./gradlew build`). Variable-expanded executables force the sandbox to re-prompt for approval
   on every invocation.
2. **Obscures Auditability**: Prevents deterministic logging and inspection of what process is
   actually being executed.
3. **Word Splitting Vulnerabilities**: Invoking unquoted `$CLI_TOOL` introduces subtle whitespace
   and argument-splitting errors in shell interpreters.

**Rule**: Always invoke the target binary directly by name or explicit path:
```powershell
#  CORRECT
wsl ./gradlew test
wsl ./gradlew check
```

---

### ⚠️ DISCOURAGED: `which` and Inline Script Interpreters

| Pattern | Status | Reason & Better Alternative |
| :--- | :--- | :--- |
| `which <tool>` | **Discouraged** | Non-standard behavior across shells and distros. Rely directly on `PATH` resolution or use `command -v <tool>` in scripts if presence checking is required. |
| `bash -c "..."` | **Discouraged** | Avoid unless pipes/redirections are strictly required. Use direct binary execution (`wsl <tool>`). |
| `python -c "..."` / `python3 -c "..."` | **Discouraged** | Heavyweight startup, quote escaping hazards, breaks sandboxing permissions. Use `jq`, `sed`, `awk`, or `grep`. |
| `node -e "..."` | **Discouraged** | Unnecessary runtime overhead for simple text transformations. |

---

## 5. Path Resolution & Working Directory Conventions

WSL automatically inherits the Windows current working directory when invoked:

```powershell
# If your CWD in PowerShell is:
# C:\Users\<username>\larpconnect

# WSL automatically executes within:
# /mnt/c/Users/<username>/larpconnect
```

### Guidelines for Paths

1. **Prefer Relative Paths with Forward Slashes**:
   ```powershell
   #  CORRECT
   wsl ./gradlew :server:test
   wsl cat settings.gradle.kts

   # ❌ INCORRECT (Windows backslashes fail in Linux binaries)
   wsl ./gradlew :server\test
   ```
2. **Absolute Paths in WSL**:
   - Windows path `C:\Users\<username>\larpconnect\build.gradle.kts` maps to
     `/mnt/c/Users/<username>/larpconnect/build.gradle.kts`.
   - Never use Windows drive letters (`C:`) inside Linux arguments.

---

## 6. Sandbox Permissions & Auto-Approval Compatibility

1. **`BypassSandbox: true` Requirement**:
   Calling the `wsl` executable crosses the Windows Hyper-V VM boundary. When using agent execution
   tools, always specify `BypassSandbox: true` for `wsl` commands to prevent `Wsl/E_ACCESSDENIED`
   sandbox violations.

2. **Prefix-Matchable Command Shapes**:
   Keep command shapes clean and static so the user's approval allowlist remains valid:
   - Prefer literal arguments over shell expansions.
   - Avoid wrapper binaries (`eval`, `sudo`, `env`).
   - Split multi-stage commands into sequential tool calls rather than giant chained strings where
     practical.

---

## 7. Quick Command Translation & Practical Cheatsheet

| Task | ❌ Antipattern |  Idiomatic WSL Command |
| :--- | :--- | :--- |
| **Run full quality suite** | `wsl bash -c "./gradlew check"` | `wsl ./gradlew check` |
| **Check formatting** | `wsl gradlew.bat spotlessCheck` | `wsl ./gradlew spotlessCheck` |
| **Apply code formatting** | `wsl bash -c "./gradlew spotlessApply"` | `wsl ./gradlew spotlessApply` |
| **Build entire project** | `wsl gradlew.bat build` | `wsl ./gradlew build` |
| **Run all tests** | `wsl bash -c "./gradlew test"` | `wsl ./gradlew test` |
| **Run module tests** | `wsl bash -c "./gradlew :server:test"` | `wsl ./gradlew :server:test` |
| **Run integration tests** | `wsl bash -c "./gradlew :integration:test"` | `wsl ./gradlew :integration:test` |
| **Debug with stacktrace** | `wsl bash -c "./gradlew test --stacktrace"` | `wsl ./gradlew test --stacktrace` |
| **Inspect directory tree** | `wsl python3 -c "import os; ..."` | `wsl tree -L 2 --dirsfirst -I "build\|.gradle"` |
| **Find Java source files** | `wsl python3 -c "..."` | `wsl find . -name "*.java" -not -path "*/build/*"` |
| **Filter text / tokens** | `wsl bash -c "grep ... \| python ..."` | `wsl grep -rn "pattern" src/` |
| **Parse JSON output** | `wsl python3 -c "import json; ..."` | `wsl jq '.artifacts.specs.status' openspec/changes/current/status.json` |
