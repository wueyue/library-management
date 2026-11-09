"""Check project line lengths and Java brace pairs without parsing string contents."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA_TOKENS = re.compile(
    r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[{}]',
    re.S,
)


def check(path):
    text = path.read_text(encoding="utf-8")
    errors = []
    for number, line in enumerate(text.splitlines(), 1):
        if len(line) > 150:
            errors.append(f"{path.relative_to(ROOT)}:{number}: line exceeds 150 characters")
    if path.suffix == ".java":
        stack = []
        for match in JAVA_TOKENS.finditer(text):
            if match.group() == "{":
                stack.append(match.start())
            elif match.group() == "}":
                opening = stack.pop()
                if "\n" not in text[opening:match.start()]:
                    number = text.count("\n", 0, opening) + 1
                    errors.append(f"{path.relative_to(ROOT)}:{number}: expand brace pair")
    return errors


files = list((ROOT / "src").rglob("*.java"))
files += list((ROOT / "src/main/resources").rglob("*.sql"))
files += list((ROOT / "src/main/resources").rglob("*.yml"))
files += list((ROOT / "tools").glob("*.py"))
files += [ROOT / name for name in ["pom.xml", "seed.sql", "compose.yaml", "README.md", ".editorconfig"]]
errors = [error for path in files for error in check(path)]
if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Format check passed: {len(files)} files")
