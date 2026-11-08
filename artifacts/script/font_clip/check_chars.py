"""检查 UTF-8 文本中的字符是否被 dic 下的字表覆盖。"""

import argparse
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="检查文本的字表覆盖情况，输出缺失字符及其 Unicode 码位。")
    parser.add_argument("text_file", type=Path, help="待检查的 UTF-8 文本文件路径")
    args = parser.parse_args()

    dictionary_dir = Path(__file__).resolve().parent / "dic"
    dictionary_files = sorted(dictionary_dir.glob("*.txt"))
    if not dictionary_files:
        parser.error(f"找不到字表：{dictionary_dir}")

    covered = set()
    try:
        for path in dictionary_files:
            covered.update(path.read_text(encoding="utf-8-sig"))
        path = args.text_file
        content = path.read_text(encoding="utf-8-sig")
    except (OSError, UnicodeError) as error:
        parser.error(f"无法读取 {path}：{error}")

    # 字表包含普通空格和全角空格，检查时只跳过换行和制表符。
    missing = dict.fromkeys(char for char in content if char not in "\r\n\t" and char not in covered)
    if not missing:
        print("文本中的字符均已被字表覆盖。")
        return 0

    for char in missing:
        print(f"{char!r} (U+{ord(char):04X})")
    return 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    raise SystemExit(main())
