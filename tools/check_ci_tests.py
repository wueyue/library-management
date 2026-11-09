"""CI must execute integration tests instead of silently accepting skipped tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

reports = list(Path("target/surefire-reports").glob("TEST-*.xml"))
if not reports:
    raise SystemExit("No test reports found")
for path in reports:
    suite = ET.parse(path).getroot()
    if any(int(suite.get(key, "0")) for key in ["skipped", "errors", "failures"]):
        raise SystemExit(f"CI requires all tests to run successfully: {path}")
print("All CI test suites passed without skips")
