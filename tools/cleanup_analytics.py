"""Одноразовая чистка: удаляет автономные вызовы AppAnalytics.logEvent(...)
(балансировкой скобок, включая многострочные) и осиротевшие импорты
AppAnalytics/AnalyticsEvents. Инлайн-вызовы (if (...) / ?.let { }) не трогает —
их правим руками."""
import pathlib
import re
import sys

ROOTS = [pathlib.Path("shared/src"), pathlib.Path("androidApp/src")]
CALL = "AppAnalytics.logEvent("
IMPORTS = [
    "import com.jetbrains.kmpapp.data.analytics.AppAnalytics",
    "import com.jetbrains.kmpapp.data.analytics.AnalyticsEvents",
]

def cut_calls(text: str) -> str:
    out = []
    i = 0
    while True:
        idx = text.find(CALL, i)
        if idx == -1:
            out.append(text[i:])
            break
        # автономный вызов: начало строки (с пробелами) перед вызовом
        line_start = text.rfind("\n", 0, idx) + 1
        if text[line_start:idx].strip() != "":
            out.append(text[i:idx + len(CALL)])
            i = idx + len(CALL)
            continue
        depth = 0
        j = idx + len(CALL) - 1  # позиция '('
        while j < len(text):
            c = text[j]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        else:
            print("UNBALANCED, skip", idx)
            out.append(text[i:idx + len(CALL)])
            i = idx + len(CALL)
            continue
        end = j + 1
        if end < len(text) and text[end] == "\n":
            end += 1
        out.append(text[i:line_start])
        i = end
    return "".join(out)

def main() -> int:
    changed = 0
    for root in ROOTS:
        for p in root.rglob("*.kt"):
            src = p.read_text(encoding="utf-8")
            new = cut_calls(src)
            for imp in IMPORTS:
                token = imp.rsplit(".", 1)[-1]
                if imp in new and not re.search(r"\b" + token + r"\b", new.replace(imp, "")):
                    new = new.replace(imp + "\n", "")
            if new != src:
                p.write_text(new, encoding="utf-8")
                changed += 1
                print("cleaned", p)
    print("files changed:", changed)
    return 0

if __name__ == "__main__":
    sys.exit(main())
