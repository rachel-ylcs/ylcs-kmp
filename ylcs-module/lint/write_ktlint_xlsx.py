import csv
import sys
from collections import Counter
from pathlib import Path
from tempfile import TemporaryDirectory

from xlsxwriter import Workbook


def write_workbook(destination, scanned, engine_errors, findings):
    headers = ("分类", "目录", "文件", "行", "列", "规则", "级别", "说明")
    widths = (18, 16, 60, 8, 8, 48, 11, 58)
    levels = Counter()
    row_count = 0
    with TemporaryDirectory(dir=destination.parent) as directory:
        temporary = Path(directory) / destination.name
        options = {
            "constant_memory": True,
            "tmpdir": directory,
            "strings_to_formulas": False,
            "strings_to_urls": False,
        }
        with Workbook(str(temporary), options) as workbook:
            header = workbook.add_format({
                "bold": True,
                "font_color": "#FFFFFF",
                "bg_color": "#17365D",
                "align": "center",
                "valign": "vcenter",
                "right": 1,
                "right_color": "#FFFFFF",
            })
            body = workbook.add_format({"valign": "top"})
            wrapped = workbook.add_format({"text_wrap": True, "valign": "top"})
            details = workbook.add_worksheet("检查明细")
            details.hide_gridlines(2)
            details.write_row(0, 0, headers, header)
            details.set_row(0, 24)
            for index, width in enumerate(widths):
                details.set_column(index, index, width, wrapped if index in (2, 7) else body)
            details.freeze_panes(1, 0)
            for row_count, (category, area, path, line, column, rule, level, message) in enumerate(findings, 1):
                row = (category, area, path, int(line), int(column), rule, level, message)
                if details.write_row(row_count, 0, row) != 0:
                    raise ValueError(f"Excel limits exceeded at finding {row_count}")
                levels[level] += 1
            details.autofilter(0, 0, row_count, len(headers) - 1)

            overview = workbook.add_worksheet("检查概览")
            overview.hide_gridlines(2)
            overview.write_row(0, 0, ("统计项", "数量"), header)
            overview.set_row(0, 24)
            overview.set_column(0, 0, 26)
            overview.set_column(1, 1, 12)
            summary = (
                ("扫描文件", scanned),
                ("发现总数", row_count),
                ("解析或规则执行错误", engine_errors),
                *((level, levels[level]) for level in ("一般", "警告", "严重")),
            )
            for index, row in enumerate(summary, 1):
                overview.write_row(index, 0, row)
        temporary.replace(destination)


def main():
    sys.stdin.reconfigure(encoding="utf-8", newline="")
    data = csv.reader(sys.stdin)
    scanned, engine_errors = map(int, next(data))
    write_workbook(Path(sys.argv[1]), scanned, engine_errors, data)


if __name__ == "__main__":
    main()
