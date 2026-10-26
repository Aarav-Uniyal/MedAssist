#!/usr/bin/env python3
"""Run fast, offline static readiness checks for MedAssist.

This script does not compile Kotlin or replace Gradle, Android UI, Firebase, or emulator tests.
Run from any working directory: python scripts/validate_release.py
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
errors: list[str] = []
passed: list[str] = []


def ok(message: str) -> None:
    passed.append(message)


def fail(message: str) -> None:
    errors.append(message)


def named_resources(path: Path, element_names: set[str]) -> dict[str, int]:
    root = ET.parse(path).getroot()
    found: dict[str, int] = {}
    for node in root:
        if node.tag in element_names and node.attrib.get("name"):
            name = node.attrib["name"]
            found[name] = found.get(name, 0) + 1
    return found


# 1. Parse source XML resources and manifests only. Gradle/AGP creates XML-like
# intermediate reports under app/build/intermediates; those are generated outputs,
# not source files, and must not be included in this source-validation check.
xml_files = sorted((APP / "src").rglob("*.xml"))
try:
    for path in xml_files:
        ET.parse(path)
    ok(f"Parsed {len(xml_files)} XML files")
except ET.ParseError as exc:
    fail(f"Malformed XML: {path.relative_to(ROOT)}: {exc}")

# 2. English/Hindi user-facing string keys must be unique and kept in parity.
en_path = APP / "src/main/res/values/strings.xml"
hi_path = APP / "src/main/res/values-hi/strings.xml"
try:
    en = named_resources(en_path, {"string", "string-array", "plurals"})
    hi = named_resources(hi_path, {"string", "string-array", "plurals"})
    dup_en = sorted(k for k, v in en.items() if v > 1)
    dup_hi = sorted(k for k, v in hi.items() if v > 1)
    if dup_en or dup_hi:
        fail(f"Duplicate resource names: English={dup_en}; Hindi={dup_hi}")
    elif set(en) != set(hi):
        fail(f"English/Hindi resource mismatch: missing Hindi={sorted(set(en)-set(hi))}; missing English={sorted(set(hi)-set(en))}")
    else:
        ok(f"English/Hindi resource keys match ({len(en)} named resources)")
except (ET.ParseError, FileNotFoundError) as exc:
    fail(f"Could not validate language resources: {exc}")

# 3. Every statically referenced R.string name in app/test Kotlin sources must exist.
resource_file = APP / "src/main/res/values/strings.xml"
try:
    available = set(named_resources(resource_file, {"string"}))
    source_files = list((APP / "src").rglob("*.kt"))
    referenced: set[str] = set()
    for source in source_files:
        referenced.update(re.findall(r"R\.string\.([A-Za-z0-9_]+)", source.read_text(encoding="utf-8")))
    missing = sorted(referenced - available)
    if missing:
        fail(f"Missing R.string definitions: {missing}")
    else:
        ok(f"Checked {len(referenced)} referenced string IDs across {len(source_files)} Kotlin files")
except (ET.ParseError, OSError) as exc:
    fail(f"Could not validate Kotlin string references: {exc}")

# 4. Basic source hygiene: no recognizable Google API-key literal in Kotlin/Gradle source;
# Firebase client configuration stays separate and ignored by Git.
secret_pattern = re.compile(r"AIza[0-9A-Za-z_-]{35}")
source_candidates = list((APP / "src").rglob("*.kt")) + list(ROOT.glob("*.gradle.kts")) + list((ROOT / "app").glob("*.gradle.kts"))
leaked = [str(p.relative_to(ROOT)) for p in source_candidates if secret_pattern.search(p.read_text(encoding="utf-8", errors="replace"))]
if leaked:
    fail(f"Possible hard-coded Google API key in source: {leaked}")
else:
    ok("No Google API-key-shaped literal found in Kotlin/Gradle source")

gitignore = (ROOT / ".gitignore").read_text(encoding="utf-8", errors="replace")
required_ignores = ["google-services.json", "*.jks", "*.keystore"]
missing_ignores = [entry for entry in required_ignores if entry not in gitignore]
if missing_ignores:
    fail(f".gitignore is missing credential/key patterns: {missing_ignores}")
else:
    ok("Firebase config and signing-key ignore patterns are present")

# 5. Security/navigation regression tripwires. Full authorization needs the emulator suite.
rules_path = ROOT / "firestore.rules"
rules = rules_path.read_text(encoding="utf-8", errors="replace")
if "function isOwner(uid)" in rules and "request.auth != null" in rules and "request.auth.uid == uid" in rules and "allow read, write: if true" not in rules:
    ok("Firestore rules contain an owner check and no blanket allow-all rule")
else:
    fail("Firestore rule static tripwire failed; inspect firestore.rules")

nav_test = APP / "src/androidTest/java/com/example/medassist/MedAssistAppTest.kt"
nav_source = (APP / "src/main/java/com/example/medassist/navigation/MedAssistNavHost.kt").read_text(encoding="utf-8")
nav_test_text = nav_test.read_text(encoding="utf-8")
analyzer_screen = (APP / "src/main/java/com/example/medassist/ui/analyzer/AnalyzerScreen.kt").read_text(encoding="utf-8")
if ("analyzerQuickAccess_thenHomeTabReturnsToHome" in nav_test_text
        and "onNodeWithContentDescription(text(R.string.action_back)).assertDoesNotExist()" in nav_test_text
        and "navController.navigateToTab(Screen.Analyzer.route)" in nav_source):
    ok("Analyzer quick-access → Home navigation regression test is present")
else:
    fail("Analyzer navigation regression test or shared navigation route is missing")
if "MedAssistTopBar(title = stringResource(R.string.title_analyzer), narrationText" in analyzer_screen and "onBack" not in analyzer_screen:
    ok("Analyzer uses a standard no-back-button top bar")
else:
    fail("Analyzer screen top bar needs review for a custom back action")

# 6. Make sure the removed extracted-text UI hasn't accidentally returned.
if "analyzer_extracted_text_heading" in analyzer_screen or "Text extracted from the original PDF" in analyzer_screen:
    fail("Removed extracted-text UI appears to have returned")
else:
    ok("Analyzer UI does not render the extracted source text panel")

print("MedAssist static release validation")
for item in passed:
    print(f"PASS  {item}")
for item in errors:
    print(f"FAIL  {item}")
print(f"\nResult: {len(passed)} passed, {len(errors)} failed")
sys.exit(1 if errors else 0)
