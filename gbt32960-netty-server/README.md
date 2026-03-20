# gbt32960-netty-server

> A runnable Java + Netty demo for GB/T 32960 TCP access and protocol parsing.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Maven](https://img.shields.io/badge/build-Maven-blue)
![Netty](https://img.shields.io/badge/network-Netty-00A3E0)
![License](https://img.shields.io/badge/license-MIT-green)

`gbt32960-netty-server` 是一个 **可直接启动、可测试、可继续扩展** 的 GB/T 32960 接入示例项目，适合用来做：

- 32960 TCP 服务端原型
- 协议学习与联调
- 粘包/半包处理示例
- Java / Netty 协议接入骨架
- 后续补齐 2025 正式字段表的基础工程

---

## 特性

当前版本已经具备这些能力：

- 基于 **Netty** 的 TCP 长连接服务端
- 处理 **半包 / 粘包**
- 解析 `##` 包头格式基础报文
- 执行 **BCC 异或校验**
- 支持基础命令分发：
  - 车辆登录
  - 实时上报
  - 补发上报
  - 车辆登出
- 维护 `VIN -> Channel` 会话映射
- 提供最小可运行的平台应答
- 内置 **demo client**，便于本地联调
- 提供 **JUnit 5** 测试样例

---

## 项目定位

这个仓库当前是：

> **工程骨架 + 可运行示例**

它不是 GB/T 32960.3-2025 正式标准的逐字段最终实现。README 里直接把这点说死，免得后面有人看两眼就开始乱引用。

当前实现会在解析结果里显式标记：

- `parserMode = compatibility-skeleton`

意思就是：

- 这是兼容性骨架实现
- 可以跑、可以测、可以扩展
- 但不能假装它已经完整覆盖 2025 正式字段定义

---

## 已实现 / 预留能力

### 已实现

- Netty TCP 服务端
- `ByteToMessageDecoder` 拆包
- `MessageToByteEncoder` 编码
- BCC 校验
- VIN 会话管理
- 登录 / 实时 / 补发 / 登出基础解析
- Demo 报文构造
- 基础测试覆盖

### 已预留但未完整落地

- 2025 正式字段表
- 分帧重组完整实现
- BMS / pack / cell 结构化模型
- 事故信息正式字段解析
- 正式平台应答格式
- RSA / AES 真正解密逻辑

---

## 技术选型

- **Java 17**：稳定，长期支持，适合作为服务端基础版本
- **Maven**：简单直接，构建和 CI 友好
- **Netty 4.1.x**：非常适合做 TCP 长连接、拆包和会话管理
- **JUnit 5**：主流测试框架，足够干净

---

## 最小必要假设

由于当前 workspace 中的文档没有提供完整的 2025 官方字段表，所以项目采用了这些最小必要假设：

1. 总包结构沿用常见 32960 格式：
   `## + cmd + ack + vin + encryptFlag + dataLen + data + bcc`
2. BCC 校验采用从 `cmd` 到数据单元末尾的异或校验
3. 命令字采用文档中的常见值
4. 信息项解析先实现为可运行骨架，后续再替换为正式字段版
5. 应答先使用最小可运行实现，不伪装成正式平台协议终版

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+

### 启动服务端

```bash
mvn -q exec:java
```

默认监听：

- Host: `0.0.0.0`
- Port: `32960`

### 启动服务端并运行内置 demo client

```bash
mvn -q exec:java -Dexec.args="--demo-client"
```

这个命令会：

1. 启动服务端
2. 自动启动一个本地客户端
3. 发送登录报文和实时上报报文
4. 在控制台输出服务端解析结果和客户端收到的应答

---

## 测试与构建

### 运行测试

```bash
mvn test
```

### 打包构建

```bash
mvn package
```

### 一次性跑测试并打包

```bash
mvn -q test package
```

当前测试覆盖：

- 编码 / 解码往返验证
- 半包处理验证
- 错误 BCC 拒绝验证
- 粘包多帧验证
- 登录解析验证
- 实时报文解析验证

---

## 项目结构

```text
src/main/java/demo/gbt32960/
├── Application.java        # 启动入口，支持 demo client
├── codec/                  # 编解码与协议工具
├── handler/                # Netty Handler
├── model/                  # 报文/命令/信息项模型
├── parser/                 # 数据单元解析器
├── response/               # 平台应答构造
├── session/                # VIN 会话管理
└── util/                   # Hex / JSON 等工具
```

---

## 适合怎么用

如果你现在要的是：

- 一个能跑起来的 Java 协议接入项目
- 一个 32960 学习和联调样板
- 一个后续继续补正式字段的基础骨架

这个仓库是合适的。

如果你要的是：

- 严格对齐 GB/T 32960.3-2025 正文的正式解析器
- 完整事故信息 / 分帧 / BMS / pack / cell 生产级模型
- 完整平台应答语义与异常处理规范

那还需要继续往前做，不要把当前版本当成终稿。别骗自己。

---

## 后续优化方向

1. 把 `BodyParser` 改成**严格按 2025 正文字段表**解析
2. 引入配置化字段定义，减少魔法数字
3. 实现完整的分帧重组模块
4. 增加事故信息、超级电容、燃料电池、BMS/Pack/Cell 结构化模型
5. 补齐正式平台应答和错误码语义
6. 增加更完整的联调样例与集成测试

---

## License

本项目采用 [MIT License](./LICENSE)。
