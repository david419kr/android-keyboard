"""Run the actual Java return-flick mapping without an Android device."""
from pathlib import Path
import subprocess
import tempfile


root = Path(__file__).resolve().parents[1]
source = (root / "java/src/org/futo/inputmethod/keyboard/PointerTracker.java").read_text(encoding="utf-8")
start = source.index("    private static String getMoakiYoonText(")
end = source.index("\n    }", start) + len("\n    }")
method = source[start:end]

checks = []
for vowels, stem in [
    ("かくけこ", "き"), ("がぐげご", "ぎ"), ("さすせそ", "し"),
    ("ざずぜぞ", "じ"), ("たつてと", "ち"), ("だづでど", "ぢ"),
    ("なぬねの", "に"), ("はふへほ", "ひ"), ("ばぶべぼ", "び"),
    ("ぱぷぺぽ", "ぴ"), ("まむめも", "み"), ("らるれろ", "り"),
]:
    for vowel, small in zip(vowels, "ゃゅぇょ"):
        checks.append(f'check("{stem + small}", getMoakiYoonText(new Key({ord(vowel)}, null), base));')
for vowel, output in [("あ", "や"), ("う", "ゆ"), ("え", "いぇ"), ("お", "よ")]:
    checks.append(f'check("{output}", getMoakiYoonText(new Key({ord(vowel)}, null), base));')
for vowel, output in [("わ", "ヴぁ"), ("う", "ヴ"), ("を", "ヴぉ")]:
    checks.append(f'check("{output}", getMoakiYoonText(new Key({ord(vowel)}, null), w));')
for vowel, output in [("うぃ", "ヴぃ"), ("うぇ", "ヴぇ")]:
    checks.append(f'check("{output}", getMoakiYoonText(new Key(-1, "{vowel}"), w));')
checks.extend([
    'check(null, getMoakiYoonText(null, base));',
    'check(null, getMoakiYoonText(new Key(12366, null), base));',  # ordinary i-column
    'check(null, getMoakiYoonText(new Key(-1, "ちぇ"), base));',
    'check(null, getMoakiYoonText(new Key(-1, "ふぇ"), base));',
])
harness = """
import java.util.Objects;
class MoakiReturnFlickCheck {
    static class Constants { static final int CODE_OUTPUT_TEXT = -1; }
    record Key(int code, String text) {
        int getCode() { return code; }
        String getOutputText() { return text; }
    }
    static void check(String expected, String actual) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("Expected " + expected + ", got " + actual);
    }
""" + method + """
    public static void main(String[] args) {
        Key base = new Key(0x3063, null);
        Key w = new Key(0xFF57, null);
""" + "\n".join(checks) + '\n    }\n}\n'
with tempfile.TemporaryDirectory(prefix="moaki-check-") as directory:
    path = Path(directory) / "MoakiReturnFlickCheck.java"
    path.write_text(harness, encoding="utf-8")
    subprocess.run(["java", "-Dfile.encoding=UTF-8", str(path)], check=True)
print(f"PASS: {len(checks)} actual Java return-flick mapping checks")
