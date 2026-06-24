# cacch-integration-platform 多系统集成平台

## 一、项目概述
cacch-integration-platform 是 Cacch 公司统一的企业级多系统集成中台，基于 Spring Boot 4 技术栈构建，采用**模块化单体架构**设计：当前以单项目方式启动部署，降低开发与运维成本；模块边界严格划分、依赖单向可控，后续业务扩张时可快速垂直拆分为微服务架构，无需大规模代码重构。

平台负责标准化对接内外部第三方业务系统，提供统一的接口适配、数据同步、消息转发与能力封装，实现各系统间的数据互通、业务协同与能力复用，降低跨系统对接的重复开发成本与运维复杂度。

## 二、技术栈总览
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 LTS | 推荐 Amazon Corretto 21 / Eclipse Temurin 21 |
| Spring Boot | 4.0.5 | 基于 Jakarta EE 11，核心业务框架 |
| MyBatis-Plus | 3.5.16 | 持久层增强框架，简化 CRUD 开发 |
| PostgreSQL | 12.22 | 主数据库，x86_64 Linux 环境，GCC 4.8.5 编译 |
| Flyway | 12.4.0 | 数据库 Schema 版本化迁移工具 |
| AWS SDK v2 | 2.42.36 | 对接 S3 兼容存储（MinIO）、SQS 消息队列 |
| Caffeine | 3.1.8 | JVM 堆内内存缓存，用于 Dashboard 高频统计 |
| Redis | Lettuce 客户端 | 分布式缓存，支撑 API Key 认证、统计快照、JWT 状态管理 |
| MapStruct | 1.6.3 | 编译期对象转换工具，实现 DO ↔ DTO 高性能转换 |
| BCrypt | at.favre.lib | API Key 安全哈希存储 |

## 三、环境要求
### 3.1 开发环境
- JDK：21 LTS 及以上
- 构建工具：Maven 3.9+
- IDE：IntelliJ IDEA 2023+（需安装 Lombok、MapStruct 插件）
- 本地辅助环境：
    - PostgreSQL 12.22
    - Redis 6.x / 7.x
    - MinIO（可选，兼容 S3 协议）

### 3.2 运行环境
- 操作系统：Linux x86_64（推荐 CentOS 7+ / Rocky Linux 9）
- JRE：21 LTS
- 数据库：PostgreSQL 12.22
- 缓存：Redis 6.x 及以上，支持集群模式

## 四、项目结构
### 4.1 模块划分原则
采用**分层+领域**结合的模块化设计，模块职责单一、依赖单向清晰：
- 横向按技术分层：公共层、持久层、业务层、编排层、接入层、启动层
- 纵向按业务域隔离，后续拆分微服务时，可按业务域直接抽离对应代码形成独立服务
- 所有模块统一继承父工程，依赖版本全局管控，避免版本冲突

### 4.2 模块职责说明
| 模块名称 | 模块类型 | 核心职责 |
|---------|---------|---------|
| cacch-integration-platform | 父工程（POM） | 统一管理所有子模块依赖版本、打包规则，不包含业务代码 |
| cacch-integration-common | 公共基础模块 | 全局常量、通用工具类、统一异常、统一返回体、通用注解、基础DTO，无业务逻辑，所有模块的底层依赖 |
| cacch-integration-dao | 数据持久模块 | 数据库实体DO、Mapper接口、MyBatis-Plus配置、数据库交互逻辑，仅负责数据读写 |
| cacch-integration-service | 单聚合业务模块 | 单一业务域内的核心业务逻辑，仅操作对应聚合根数据，不做跨域流程编排 |
| cacch-integration-manager | 业务编排模块 | 跨聚合流程组装、全局事务控制、多服务聚合查询、调用第三方适配层，是复杂业务的入口 |
| cacch-integration-integration | 第三方适配模块 | 各外部系统的客户端封装、协议转换、接口适配，屏蔽第三方系统的协议与参数差异 |
| cacch-integration-async | 异步处理模块 | SQS消息消费、异步任务调度、批量数据处理，解耦主流程与异步逻辑 |
| cacch-integration-web | Web启动模块 | Controller控制层、配置文件、项目启动类，单体模式下的唯一启动入口，整合所有业务模块 |

### 4.3 完整工程目录
```
cacch-integration-platform                    # 父工程根目录
├── pom.xml                                   # 父POM：统一依赖管理
├── cacch-integration-common                  # 公共基础模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration/common
│               ├── constant                  # 全局常量
│               ├── utils                     # 通用工具类
│               ├── exception                 # 统一异常定义
│               ├── result                    # 统一返回封装
│               └── annotation                # 通用注解
├── cacch-integration-dao                     # 数据持久模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration
│               ├── entity                    # 数据库实体DO
│               ├── mapper                    # MyBatis Mapper接口
│               └── config                    # 数据源、MyBatis-Plus配置
├── cacch-integration-service                 # 单聚合业务模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration/service
│               ├── api                       # 业务接口定义
│               └── impl                      # 单聚合业务实现
├── cacch-integration-manager                 # 业务编排模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration/manager
│               ├── api                       # 编排接口定义
│               └── impl                      # 跨域编排、事务控制实现
├── cacch-integration-integration             # 第三方适配模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration/integration
│               ├── client                    # 第三方系统调用客户端
│               └── adapter                   # 协议转换与系统适配
├── cacch-integration-async                   # 异步处理模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/integration/async
│               ├── consumer                  # SQS消息消费者
│               └── task                      # 异步任务处理
└── cacch-integration-web                     # Web启动模块（单体启动入口）
    ├── pom.xml
    └── src
        ├── main
        │   ├── java/com/cacch/integration
        │   │   ├── CacchIntegrationApplication.java  # 启动类
        │   │   ├── controller              # 控制层：OpenAPI、管理接口
        │   │   ├── convert                 # MapStruct对象转换
        │   │   ├── dto                     # 请求/响应DTO
        │   │   ├── security                # 安全认证：API Key、JWT
        │   │   └── config                  # Web层、缓存、安全等配置
        │   └── resources
        │       ├── application.yml         # 主配置文件
        │       ├── application-dev.yml     # 开发环境配置
        │       ├── application-test.yml    # 测试环境配置
        │       ├── application-prod.yml    # 生产环境配置
        │       └── db/migration            # Flyway数据库迁移脚本
        │           └── V1__init_schema.sql
        └── test                            # 单元测试、集成测试
```

### 4.4 模块依赖规则
- 依赖流向严格单向，禁止循环依赖：`web → manager → service → dao → common`
- `integration` 模块依赖 `common`，被 `manager` / `service` 按需依赖
- `async` 模块依赖 `service` / `manager`，由 `web` 模块统一加载启动
- 下层模块不得反向依赖上层模块，例如 `service` 不能依赖 `manager`，`dao` 不能依赖 `service`
- 后续拆分微服务时，按业务域拆分对应 `dao/service/manager/controller`，复用 `common` 与基础配置，即可快速形成独立服务

### 4.5 命名规范
- **父工程 groupId**：`com.cacch`
- **所有子模块 artifactId**：统一前缀 `cacch-integration-` + 模块功能定位
- **顶层包名**：`com.cacch.integration`，各模块按职责划分子包
- 数据库表名：统一前缀 `t_integration_`，全小写，下划线分隔，例如 `t_integration_api_key`
- Flyway 脚本：`V{版本号}__{描述}.sql`，版本号递增，禁止修改已执行脚本

## 五、快速开始
### 5.1 环境准备
1. 本地安装 JDK 21 与 Maven 3.9+，配置环境变量
2. 启动 PostgreSQL 12.22，创建数据库 `cacch_integration`
3. 启动 Redis 服务
4. （可选）启动 MinIO 服务，配置访问密钥

### 5.2 配置修改
修改 `cacch-integration-web` 模块下 `application-dev.yml` 核心配置：
```yaml
# 数据源配置
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/cacch_integration
    username: your_username
    password: your_password

  # Redis配置
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_redis_password

  # Flyway配置
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 5.3 启动项目
1. 在父工程根目录执行全量编译：
```bash
mvn clean compile
```
2. 运行 `cacch-integration-web` 模块下的启动类 `CacchIntegrationApplication`
3. 启动成功后访问健康检查地址：`http://127.0.0.1:8080/actuator/health`

## 六、核心能力模块
1. **统一集成适配层**
   封装多系统对接协议，支持 HTTP、消息队列等多种对接方式，统一异常处理、重试机制与日志埋点，降低各系统对接的重复开发量。

2. **API 密钥认证体系**
   基于 BCrypt 哈希存储 API Key，配合 Redis 实现高频认证缓存，为对外 OpenAPI 提供安全、高性能的鉴权能力。

3. **异步数据处理**
   基于 AWS SQS 实现异步入库与数据解耦，支持高并发数据同步场景，避免下游系统波动影响主服务稳定性。

4. **两级缓存架构**
   Caffeine 堆内缓存承载 Dashboard 高频统计查询，Redis 分布式缓存承载认证信息、全表统计快照，兼顾性能与分布式一致性。

5. **数据库版本管控**
   Flyway 全量管控 Schema 变更，保障开发、测试、生产环境数据结构一致，降低发布故障风险。

6. **对象存储集成**
   基于 AWS SDK v2 对接 S3 兼容存储，支持集成报文、附件文件的统一存储与管理。

## 七、开发规范
### 7.1 代码分层规范
采用「Controller → Manager → Service → Mapper」四层架构，各层职责严格隔离，禁止越界：
- **Controller 层**：仅做参数校验、请求转发、结果统一封装，不包含业务逻辑与流程编排
- **Manager 层**：业务编排入口，负责跨聚合流程组装、全局事务控制、多服务结果聚合查询；不直接操作数据库，不编写单表 CRUD 逻辑
- **Service 层**：仅承载**单聚合内**的核心业务逻辑，处理单一业务域的数据操作，不做跨域流程编排，不控制跨服务事务
- **Mapper 层**：仅做数据库交互，禁止拼接业务逻辑
- 所有 DO 与 DTO 转换必须通过 MapStruct 实现，禁止手动 setter/getter 堆砌

### 7.2 模块开发规范
1. 新增业务功能优先按领域归类到对应模块，不得跨模块随意放置代码
2. 模块间调用必须通过接口层交互，禁止直接依赖实现类，便于后续拆分与替换
3. 公共能力必须下沉到 `common` 模块，禁止在各业务模块重复实现通用逻辑
4. 第三方系统对接逻辑必须收敛在 `integration` 模块，业务层不得直接调用第三方SDK

### 7.3 数据库开发规范
1. 所有表必须包含主键 `id`、创建时间、更新时间、逻辑删除标记
2. 禁止直接修改数据库结构，所有变更必须通过 Flyway 脚本提交
3. PostgreSQL 12 兼容约束：
    - 禁止使用 `MERGE` 语法，幂等写入统一使用 `INSERT ... ON CONFLICT`
    - 分区表避免复杂嵌套设计，减少运维复杂度
    - 大表需提前配置 autovacuum 参数，定期检查表膨胀情况

### 7.4 安全规范
- API Key、密钥等敏感信息禁止硬编码，通过配置文件或配置中心注入
- 所有对外接口必须做鉴权校验与参数合法性校验
- 敏感数据存储必须做哈希或加密处理，禁止明文存储

## 八、部署与运维
### 8.1 项目打包
在父工程根目录执行命令，生成可执行 Jar 包：
```bash
mvn clean package -DskipTests
```
产物路径：`cacch-integration-web/target/cacch-integration-web.jar`

### 8.2 配置外置
生产环境推荐使用外置配置文件覆盖默认配置，启动命令示例：
```bash
java -jar cacch-integration-web.jar --spring.profiles.active=prod --spring.config.location=/opt/config/
```

### 8.3 运维注意事项
1. **数据库运维**：PostgreSQL 12 无官方安全补丁，需严格限制数据库访问白名单，定期执行 vacuum 分析，监控表膨胀率。
2. **缓存运维**：Redis 需配置持久化策略，关键认证数据设置合理过期时间，避免缓存穿透与雪崩。
3. **版本升级**：Flyway 会在服务启动时自动执行未运行的迁移脚本，升级前需在测试环境完整验证脚本兼容性。

## 九、可选扩展增强
根据项目迭代需要，可按需引入以下主流组件提升可维护性与稳定性：
- **服务容错**：Resilience4j + Spring Retry，实现熔断、降级、重试，抵御下游系统故障
- **可观测性**：Spring Boot Actuator + Micrometer + SkyWalking，实现指标监控、链路追踪
- **接口文档**：SpringDoc OpenAPI 3，自动生成对外接口文档，降低对接沟通成本
- **幂等增强**：完善 SQS 消息幂等消费、死信队列机制，保障异步数据可靠性
- **微服务演进**：业务扩张时引入 Spring Cloud 相关组件，将各业务模块拆分为独立微服务，复用现有业务代码