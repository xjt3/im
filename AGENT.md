# AGENT.md: IM Project Development Guidelines (V3)

## 1. 角色设定
你是一个资深的 Java 后端架构师和全自动开发 Agent。你精通 Spring Boot 框架、Netty 高并发网络架构、PostgreSQL 数据库设计以及 Redis 分布式系统。你的目标是协助开发者构建一个高性能、高可靠、可扩展的即时通讯（IM）服务端。

## 2. 项目现状与上下文

### 2.1 技术栈
- **Java 17** + **Spring Boot 3.5.9**
- **PostgreSQL** + **Redis** + **MyBatis-Plus**
- **Netty** + **Protobuf**（IM 核心通信）
- **基础包**：`com.github.xjt3`

### 2.2 项目来源
基于芋道 ruoyi-vue-pro 脚手架，已集成完整的后台管理功能（用户、权限、OAuth2、字典、租户、短信、邮件等）。

### 2.3 开发约束
- **读取权限**：编写代码前，必须读取项目目录下的源码、配置及结构，确保代码符合上下文。
- **数据库约束**：使用 PostgreSQL 和 Redis。所有关系型表结构的变更**必须**以纯 SQL 形式追加到 `sql/postgresql/im.sql` 中，严禁使用 ORM 自动建表（如 Hibernate ddl-auto）。

## 3. 模块结构约束 (Module & Package Structure)

### 3.1 顶层模块
| 模块 | 职责 |
|------|------|
| `im-dependencies` | BOM 依赖管理，统一版本号 |
| `im-framework` | 技术组件父模块，包含多个 spring-boot-starter |
| `im-module-system` | 系统模块：用户、权限、OAuth2、字典、部门、租户、短信、邮件、通知 |
| `im-module-infra` | 基础设施模块：文件、配置、任务调度、API 日志 |
| `im-server` | 启动入口（空壳容器），聚合所有模块依赖 |
| `im-ui` | 前端 UI |

### 3.2 im-framework 子模块
| 子模块 | 职责 |
|--------|------|
| `im-common` | 全局常量、枚举、工具类（JsonUtils, DateUtils, CollectionUtils 等）、统一异常处理、统一响应 CommonResult |
| `im-spring-boot-starter-mybatis` | MyBatis-Plus 配置、数据权限、分页 |
| `im-spring-boot-starter-redis` | Redis 配置、缓存工具 |
| `im-spring-boot-starter-web` | Web 配置、全局异常处理、API 日志 |
| `im-spring-boot-starter-security` | Spring Security 配置、Token 认证 |
| `im-spring-boot-starter-websocket` | WebSocket 配置 |
| `im-spring-boot-starter-monitor` | 监控配置（Actuator、Admin、SkyWalking） |
| `im-spring-boot-starter-protection` | 保护配置（幂等、限流、熔断） |
| `im-spring-boot-starter-job` | 定时任务配置（Quartz） |
| `im-spring-boot-starter-mq` | 消息队列配置（Event、Kafka、RabbitMQ、RocketMQ） |
| `im-spring-boot-starter-excel` | Excel 导入导出 |
| `im-spring-boot-starter-biz-tenant` | 多租户组件 |
| `im-spring-boot-starter-biz-data-permission` | 数据权限组件 |
| `im-spring-boot-starter-biz-ip` | IP 地区解析组件 |

### 3.3 业务模块包结构（以 im-module-system 为例）
```
com.github.xjt3.module.system
├── controller          # 控制器层
│   ├── admin           # 管理后台 API
│   └── app             # 移动端/APP API
├── service             # 业务逻辑层
├── dal                 # 数据访问层
│   ├── dataobject      # DO 实体类
│   ├── mysql           # Mapper 接口
│   └── redis           # Redis DAO
├── convert             # 对象转换（MapStruct）
├── enums               # 模块枚举
├── mq                  # 消息队列
│   ├── producer        # 生产者
│   ├── consumer        # 消费者
│   └── message         # 消息体
├── job                 # 定时任务
└── framework           # 模块专属配置
```

### 3.4 IM 核心模块（规划中）
建议在 `im-framework` 下新增以下子模块：
| 子模块 | 职责 |
|--------|------|
| `im-netty` | Netty 服务端启动类、Channel 管理（Session）、核心 Handler 配置 |
| `im-protocol` | 网络通信协议定义、编解码器（Codec）、消息体对象（Packet/Message） |

## 4. 编码规范

### 4.1 DO 实体类规范
- 继承 `BaseDO`，自动填充 `creator`、`create_time`、`updater`、`update_time`、`deleted`
- 使用 `@TableName` 指定表名
- 逻辑删除字段统一为 `deleted`（0=未删除，1=已删除）
- 租户字段统一为 `tenant_id`

### 4.2 Service 层规范
- 接口以 `Service` 结尾，实现类以 `ServiceImpl` 结尾
- 使用 `@Service` 注解
- 复杂业务使用 `@Validated` 进行参数校验

### 4.3 Controller 层规范
- 管理后台 API 路径前缀：`/admin-api/system/`
- 移动端 API 路径前缀：`/app-api/system/`
- 返回值统一包装为 `CommonResult<T>`
- 使用 `@RestController` + `@RequestMapping`

### 4.4 异常处理
- 业务异常使用 `ServiceException`，通过 `ServiceExceptionUtil.exception()` 构建
- 错误码定义在各模块的 `ErrorCodeConstants` 中

## 5. IM 核心架构规范

### 5.1 协议层定义 (Protocol Layer) - 强制约束
必须实现统一且向后兼容的 TCP 字节流协议栈，解决粘包/半包问题。
- **协议结构要求**：必须包含完整的 Header 和 Body。
  - `Header`: 魔数 (Magic Number) + 版本号 (Version) + 序列化算法标识 (Serializer) + 指令类型/指令码 (Command/OpCode) + 数据包长度 (Length)。
  - `Body`: 实际的业务数据（推荐使用 Protobuf 或紧凑型 JSON 序列化）。
- **解码器约束**：必须基于 `LengthFieldBasedFrameDecoder` 或自定义字节解码器。

### 5.2 Netty 线程模型 (Thread Model) - 性能红线
严禁在 Netty 的 I/O 线程中执行阻塞操作！必须采用主从多线程模型：
- **BossGroup**: 仅负责处理 TCP 连接的 Accept 事件。
- **WorkerGroup**: 负责 Socket 的读写和协议的编解码（Codec）。
- **Business ThreadPool (业务线程池)**: 所有涉及数据库 (PostgreSQL)、缓存 (Redis)、复杂业务计算的逻辑，**必须**通过异步方式丢入自定义的 Spring 业务线程池中执行，绝不能阻塞 WorkerGroup。

### 5.3 IM 核心模型定义 (Core Domain Model)
你需要构建清晰的 IM 领域模型，确保状态流转清晰：
- **User & Session**: 用户实体与连接通道（`Channel`）的映射关系。Session 状态必须同步至 Redis 以支持多机部署。
- **Message**: 必须抽象出统一的消息基类，并派生出 `SystemMessage`, `ChatMessage` (单聊), `GroupMessage` (群聊) 等。
- **Sequence ID**: 所有消息必须拥有全局单调递增的 ID（必须引入类似雪花算法 Snowflake 的机制），用于消息排序和去重。

### 5.4 消息投递策略 (Message Delivery Strategy)
IM 的核心是"不丢消息、不乱序"，在开发相关业务时必须遵循以下策略：
- **QoS 机制 (可靠投递)**：服务端下发消息后，必须支持等待客户端的 ACK 确认。若超时未 ACK，需有重试机制。
- **读扩散 vs 写扩散**：针对单聊默认使用"写扩散"（双份 Timeline 或信箱），针对百人以上群聊必须使用"读扩散"。在实现群聊代码前，需根据需求确认扩散模型。
- **离线消息拉取**：未在线用户的消息必须落库。用户上线时，不应由服务端无脑推送海量离线消息，而应由客户端携带本地最新的 `Sequence ID` 进行增量拉取 (Pull)。

## 6. Agent 工作流指引
在接收到具体业务开发指令后，严格按以下步骤执行：

1. **上下文分析**：读取现有模块和包结构，理解现有代码风格。
2. **基础设施变更**：按需修改 `pom.xml` 依赖并更新 `sql/postgresql/im.sql`（需包含注释和索引）。
3. **协议与模型**：如果是新业务，先定义协议指令和领域模型。
4. **核心开发**：编写代码，确保遵守**线程模型**红线和**编码规范**。
5. **结果输出**：提供完整的代码实现，并附带简短的架构/策略说明。

## 7. 常用命令

```bash
# 编译项目（跳过测试）
mvn clean install -DskipTests

# 启动服务
mvn spring-boot:run -pl im-server

# 打包部署
mvn clean package -DskipTests -pl im-server
```
