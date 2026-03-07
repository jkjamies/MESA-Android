---
name: security-check
description: Security audit of current changes for Android/Kotlin vulnerabilities and best practices
disable-model-invocation: true
argument-hint: "[--staged | --uncommitted]"
---

# Security Check

Perform a focused security audit on the current changes. Analyze the diff and the full content of changed files for security vulnerabilities, insecure patterns, and Android-specific risks.

**Scope:** $ARGUMENTS

---

## Step 1: Gather Changes

Determine which changes to audit:
- `--staged` → `git diff --cached` (only staged changes)
- `--uncommitted` → `git diff HEAD` (only uncommitted changes)
- No flag → Default to `git diff main` (all changes on the branch vs main, committed and uncommitted)

Also run `git status` to identify new untracked files.

Read the diff output AND the full content of every changed file. Security issues often depend on context beyond the changed lines.

---

## Step 2: Secrets & Credentials

- [ ] No hardcoded API keys, tokens, passwords, or secrets
- [ ] No secrets in string resources, `BuildConfig` fields, or `gradle.properties` committed to source
- [ ] No secrets logged or included in error messages
- [ ] No private keys or keystores committed
- [ ] `.gitignore` covers sensitive files (`local.properties`, `*.jks`, `*.keystore`)

---

## Step 3: Data Security

- [ ] Sensitive data not stored in plain `SharedPreferences` (use `EncryptedSharedPreferences` or equivalent)
- [ ] No sensitive data written to external storage without encryption
- [ ] No sensitive data in `Parcelable`/`Serializable` objects passed via `Intent` extras without validation
- [ ] Database queries use parameterized statements (no string concatenation in Room `@RawQuery` or `SimpleSQLiteQuery`)
- [ ] No sensitive data in `rememberSaveable` state that persists across process death

---

## Step 4: Network Security

- [ ] No cleartext HTTP traffic (check `android:usesCleartextTraffic` and network security config)
- [ ] No disabled certificate validation or custom `TrustManager` that accepts all certs
- [ ] No sensitive data in URL query parameters (use request body instead)
- [ ] API responses validated before use (no blind trust of server data)
- [ ] No hardcoded URLs that should be configurable per environment

---

## Step 5: Android Component Security

- [ ] Activities, Services, BroadcastReceivers, and ContentProviders not exported unnecessarily (`android:exported="false"` where appropriate)
- [ ] `Intent` data validated before use (no unguarded `getStringExtra`, `getParcelableExtra`, etc.)
- [ ] Deep link handlers validate and sanitize URL parameters
- [ ] No `PendingIntent` created with `FLAG_MUTABLE` unnecessarily
- [ ] FileProvider paths scoped as narrowly as possible

---

## Step 6: WebView Security (if applicable)

- [ ] JavaScript not enabled unless strictly necessary
- [ ] No `addJavascriptInterface` exposing sensitive methods
- [ ] `WebViewClient.shouldOverrideUrlLoading` validates URLs
- [ ] `setAllowFileAccess(false)` for WebViews loading remote content
- [ ] No mixed content allowed (`MIXED_CONTENT_NEVER_ALLOW`)

---

## Step 7: Code Injection & Input Validation

- [ ] No `Runtime.exec()` or `ProcessBuilder` with unsanitized input
- [ ] No dynamic class loading (`Class.forName`, `DexClassLoader`) with untrusted input
- [ ] No `eval`-style execution of user-provided data
- [ ] Input from external sources (Intents, deep links, user input) validated and sanitized at system boundaries
- [ ] No unsafe deserialization of untrusted data

---

## Step 8: Logging & Error Handling

- [ ] No sensitive data in log statements (`Log.d`, `Log.e`, `Timber`, etc.)
- [ ] No stack traces or internal error details exposed to the UI in production
- [ ] Error messages do not leak implementation details (class names, SQL structure, file paths)
- [ ] `ProGuard`/`R8` rules do not expose sensitive class or method names

---

## Step 9: Dependency Security

- [ ] No dependencies with known critical CVEs (check version against known vulnerabilities)
- [ ] Dependencies sourced from trusted repositories (Maven Central, Google Maven)
- [ ] No wildcard or dynamic version declarations that could pull compromised versions
- [ ] Third-party SDKs reviewed for excessive permissions or data collection

---

## Step 10: Coroutine & Concurrency Security

- [ ] No race conditions that could lead to auth bypass or privilege escalation
- [ ] Token/session state not shared mutably across coroutines without synchronization
- [ ] Cancellation does not leave security-sensitive operations in an incomplete state (e.g., half-written encrypted data)

---

## Step 11: Report

### Summary
Brief assessment of the security posture of the changes.

### Security Checklist
Show all completed checklists from Steps 2-10 with pass/fail/not-applicable indicators. Skip entire sections that are not applicable to the changed files.

### Vulnerabilities Found
List by severity:
- **Critical:** Exploitable vulnerabilities that must be fixed immediately
- **High:** Significant risks that should be fixed before merge
- **Medium:** Potential issues that should be addressed
- **Low:** Minor concerns or hardening opportunities

For each vulnerability:
- File path and line reference
- Description of the issue
- Recommended fix

### Clean
If no issues are found, state that the changes pass the security audit.
