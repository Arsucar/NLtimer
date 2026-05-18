# Agent 文档索引

> AI Agent 快速上下文填充文档。按需加载，从上到下信息密度递增。

## 加载顺序

1. **[01-project-overview.md](01-project-overview.md)** — 先读，建立基本认知（~50 token）
2. **[02-architecture-digest.md](02-architecture-digest.md)** — 架构与数据流（~100 token）
3. **[03-module-map.md](03-module-map.md)** — 需要定位代码时读（查找表）
4. **[04-patterns.md](04-patterns.md)** — 需要写代码时读（约定与模式）
5. **[05-data-model.md](05-data-model.md)** — 涉及数据层时读（速查表）

## 设计原则

- 每个文件独立可读，无交叉引用
- 表格为主，无废话
- 文件路径相对于项目根目录
- 内容与代码同步维护
