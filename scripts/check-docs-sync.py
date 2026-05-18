#!/usr/bin/env python3
"""
check-docs-sync.py — 校验 docs/agent/ 文档与实际代码的同步状态。

检查项：
  1. 模块检查：对比 feature/*/ 和 core/*/ 构建文件与 03-module-map.md 的 ## 标题
  2. 数据模型检查：对比 Entity 源码字段与 05-data-model.md 表格字段

退出码：0 = 无问题，1 = 有差异
"""

import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------

ROOT = Path(__file__).resolve().parent.parent
MODULE_MAP_DOC = ROOT / "docs" / "agent" / "03-module-map.md"
DATA_MODEL_DOC = ROOT / "docs" / "agent" / "05-data-model.md"
ENTITY_DIR = ROOT / "core" / "data" / "src" / "main" / "java" / "com" / "nltimer" / "core" / "data" / "database" / "entity"

# 忽略的 Entity（关系表只有简单字段，不在文档"实体字段"章节中详细列出）
SKIP_ENTITIES: Set[str] = set()  # 如需跳过可在此添加

# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

def ok(msg: str) -> None:
    print(f"  ✓ {msg}")

def fail(msg: str) -> None:
    print(f"  ✗ {msg}")


def discover_actual_modules() -> Set[str]:
    """扫描 feature/*/build.gradle.kts 和 core/*/build.gradle.kts，返回模块标识集合。"""
    modules: Set[str] = set()
    for group in ("feature", "core"):
        group_dir = ROOT / group
        if not group_dir.is_dir():
            continue
        for child in sorted(group_dir.iterdir()):
            if child.is_dir() and (child / "build.gradle.kts").exists():
                modules.add(f"{group}:{child.name}")
    return modules


def parse_doc_module_headings(doc: Path) -> Set[str]:
    """从 03-module-map.md 提取 ## 标题，转换为 module:name 格式。"""
    modules: Set[str] = set()
    if not doc.is_file():
        return modules
    for line in doc.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^##\s+(.+)$", line.strip())
        if m:
            heading = m.group(1).strip()
            # "core:data" → "core:data"，"feature:home" → "feature:home"，"app" → "app"
            modules.add(heading)
    return modules


def parse_entity_fields(filepath: Path) -> List[str]:
    """从 Kotlin Entity 文件中提取 val/var 字段名列表。"""
    if not filepath.is_file():
        return []
    text = filepath.read_text(encoding="utf-8")
    fields: List[str] = []
    for m in re.finditer(r"^\s+(?:val|var)\s+(\w+)\s*:", text, re.MULTILINE):
        fields.append(m.group(1))
    return fields


def parse_doc_entity_fields(doc: Path, entity_name: str) -> List[str]:
    """从 05-data-model.md 提取指定 Entity 章节的字段名列表。"""
    if not doc.is_file():
        return []
    text = doc.read_text(encoding="utf-8")
    # 找到 ### EntityName 标题
    pattern = re.compile(
        rf"^###\s+{re.escape(entity_name)}\s*$",
        re.MULTILINE,
    )
    m = pattern.search(text)
    if not m:
        return []
    start = m.end()
    # 截取到下一个 ### 或 ## 或文件结尾
    rest = text[start:]
    next_section = re.search(r"^(?:###|##)\s+", rest, re.MULTILINE)
    block = rest[: next_section.start()] if next_section else rest

    # 解析表格行  | fieldName | ... |
    fields: List[str] = []
    for line in block.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 2 and cells[0] and not cells[0].startswith("-"):
            # 排除表头行（"字段"）
            if cells[0] not in ("字段", "表名", "表", "值"):
                fields.append(cells[0])
    return fields


def discover_entity_files() -> Dict[str, Path]:
    """返回 {EntityName: filepath} 映射。"""
    result: Dict[str, Path] = {}
    if not ENTITY_DIR.is_dir():
        return result
    for f in sorted(ENTITY_DIR.glob("*.kt")):
        name = f.stem  # e.g. "ActivityEntity"
        result[name] = f
    return result


# ---------------------------------------------------------------------------
# 检查函数
# ---------------------------------------------------------------------------

def check_modules() -> bool:
    """检查模块映射文档同步状态，返回 True 表示通过。"""
    print("\n=== 模块检查 ===\n")

    actual = discover_actual_modules()
    documented = parse_doc_module_headings(MODULE_MAP_DOC)

    # app 模块没有 build.gradle.kts 在根目录的标准路径，单独处理
    # 扫描 app/build.gradle.kts
    if (ROOT / "app" / "build.gradle.kts").exists():
        actual.add("app")

    passed = True

    # 未记录的模块
    undocumented = sorted(actual - documented)
    if undocumented:
        fail(f"未记录的模块（存在于代码但不在文档中）: {', '.join(undocumented)}")
        passed = False
    else:
        ok("所有实际模块均已记录在文档中")

    # 已删除的模块（文档中有但代码中不存在，排除 "app" 因为它可能有特殊处理）
    removed = sorted(documented - actual)
    if removed:
        fail(f"已删除的模块（在文档中但代码中不存在）: {', '.join(removed)}")
        passed = False
    else:
        ok("文档中无已删除的模块")

    print(f"\n  实际模块数: {len(actual)}, 文档记录数: {len(documented)}")
    return passed


def check_data_models() -> bool:
    """检查数据模型文档同步状态，返回 True 表示通过。"""
    print("\n=== 数据模型检查 ===\n")

    entities = discover_entity_files()
    if not entities:
        fail(f"未找到 Entity 目录: {ENTITY_DIR}")
        return False

    passed = True

    for entity_name, filepath in entities.items():
        if entity_name in SKIP_ENTITIES:
            continue

        code_fields = parse_entity_fields(filepath)
        doc_fields = parse_doc_entity_fields(DATA_MODEL_DOC, entity_name)

        if not doc_fields and not code_fields:
            ok(f"{entity_name}: 源码无字段，文档无记录（可能为空实体）")
            continue

        if not doc_fields and code_fields:
            fail(f"{entity_name}: 文档中无字段记录（源码有 {len(code_fields)} 个字段）")
            passed = False
            continue

        code_set = set(code_fields)
        doc_set = set(doc_fields)

        missing_in_doc = sorted(code_set - doc_set)
        extra_in_doc = sorted(doc_set - code_set)

        if not missing_in_doc and not extra_in_doc:
            ok(f"{entity_name}: 字段一致 ({len(code_fields)} 个)")
        else:
            if missing_in_doc:
                fail(f"{entity_name}: 源码有但文档未记录: {', '.join(missing_in_doc)}")
                passed = False
            if extra_in_doc:
                fail(f"{entity_name}: 文档有但源码不存在: {', '.join(extra_in_doc)}")
                passed = False
            if not missing_in_doc and not extra_in_doc:
                ok(f"{entity_name}: 字段一致")

    return passed


# ---------------------------------------------------------------------------
# 主入口
# ---------------------------------------------------------------------------

def main() -> int:
    root_ok = ROOT.is_dir()
    if not root_ok:
        print(f"错误：项目根目录不存在: {ROOT}")
        return 1

    print(f"项目根目录: {ROOT}")

    all_passed = True
    all_passed &= check_modules()
    all_passed &= check_data_models()

    print()
    if all_passed:
        print("✓ 所有检查通过，文档与代码同步。")
        return 0
    else:
        print("✗ 发现差异，请更新文档。")
        return 1


if __name__ == "__main__":
    sys.exit(main())
