# 需求管理规范

## 目录结构

```
docs/requirements/
├── README.md                          ← 本文件（规范说明）
├── _template/                         ← 需求文档模板
│   └── requirement-template.md
├── wecom/                             ← 企业微信相关需求
│   ├── feature/                       ← 新功能
│   ├── optimization/                  ← 优化改进
│   └── bug/                           ← 缺陷修复
├── tencent-meeting/                   ← 腾讯会议相关需求
│   ├── feature/
│   ├── optimization/
│   └── bug/
├── crm/                               ← 勤策CRM相关需求
│   └── integration/                   ← 集成对接
├── oa/                                ← 致远OA相关需求
│   ├── feature/                       ← 新功能
│   ├── optimization/                  ← 优化改进
│   ├── bug/                           ← 缺陷修复
│   └── integration/                   ← 集成对接
└── platform/                          ← 平台基础（部署、配置、基础设施）
    ├── feature/
    ├── optimization/
    └── bug/
```

## 需求编号规则

```
REQ-{系统缩写}-{3位序号}
```

| 系统缩写 | 系统 | 示例 |
|----------|------|------|
| WECOM | 企业微信 | REQ-WECOM-001 |
| TMEET | 腾讯会议 | REQ-TMEET-001 |
| CRM | 勤策CRM | REQ-CRM-001 |
| OA | 致远OA | REQ-OA-001 |
| PLAT | 平台基础 | REQ-PLAT-001 |

> 序号按各系统独立递增，不补零对齐（001 → 002 → ... → 999）

## 文件命名规则

```
REQ-{系统缩写}-{序号}-{简短英文或中文描述}.md
```

示例：
- `REQ-CRM-001-CRM订单同步OA表单.md`
- `REQ-WECOM-001-智能表格人员忙闲状态.md`
- `REQ-PLAT-001-正式服务器部署.md`

## 需求类型说明

| 类型 | 目录名 | 说明 |
|------|--------|------|
| 新功能 | `feature/` | 全新的功能开发 |
| 优化改进 | `optimization/` | 对现有功能的改进、性能优化、体验提升 |
| 缺陷修复 | `bug/` | 线上问题修复 |
| 集成对接 | `integration/` | 与外部系统的API对接、数据同步 |

## 需求状态流转

```
draft → reviewing → approved → in-progress → testing → done → closed
 草稿     评审中      已确认      开发中        测试中    已完成   已关闭
```

| 状态 | 说明 | 谁操作 |
|------|------|--------|
| draft | 需求梳理中，文档编写阶段 | 提出人 |
| reviewing | 需求评审中，待确认 | 评审人 |
| approved | 需求已确认，待开发 | 评审人 |
| in-progress | 开发中 | 开发人员 |
| testing | 开发完成，测试中 | 测试人员 |
| done | 测试通过，已上线 | 开发人员 |
| closed | 需求关闭（上线后验证通过/取消） | 提出人 |

## 优先级定义

| 优先级 | 说明 | 响应时间 |
|--------|------|----------|
| P0 | 紧急，阻塞性问题 | 立即处理 |
| P1 | 高优先级，核心需求 | 本迭代完成 |
| P2 | 中优先级，重要需求 | 下迭代排期 |
| P3 | 低优先级，优化需求 | 按需排期 |

## 工作流程

1. **接到需求** → 在对应系统/类型目录下复制模板
2. **梳理需求** → 填写背景、描述、流程、验收标准
3. **需求评审** → 状态改为 `reviewing`，补充技术方案
4. **确认开发** → 状态改为 `approved` → `in-progress`
5. **开发测试** → 状态改为 `testing`，补充代码变更记录
6. **上线关闭** → 状态改为 `done` → `closed`

## 已有需求索引

| 编号 | 标题 | 系统 | 类型 | 状态 | 文件 |
|------|------|------|------|------|------|
| REQ-CRM-001 | CRM订单同步OA表单 | CRM | integration | reviewing | [crm/integration/REQ-CRM-001-CRM订单同步OA表单.md](./crm/integration/REQ-CRM-001-CRM订单同步OA表单.md) |
| REQ-OA-001 | 国内登记报告资料列表附件上传 | OA | feature | draft | [oa/feature/REQ-OA-001-国内登记报告资料列表附件上传.md](./oa/feature/REQ-OA-001-国内登记报告资料列表附件上传.md) |
| REQ-WECOM-001 | 智能表格人员忙闲状态 | WECOM | feature | draft | wecom/feature/ (待创建) |
