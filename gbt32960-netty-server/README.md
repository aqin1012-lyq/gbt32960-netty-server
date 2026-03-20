# gbt32960-netty-server

GB/T 32960 TCP 接入与协议解析示例工程（Java 17 + Netty）。

## 项目目标

基于 `ChatGPT-1GBT32960-2025 协议解析.md` 中的设计要求，提供一个**可直接启动**的 Java 项目，用于：

- 处理 GB/T 32960 风格 TCP 长连接报文
- 完成半包/粘包处理
- 解析 `##` 包头格式的基础报文
- 执行 BCC 校验
- 支持登录、实时上报、补发、登出命令分发
- 提供一个最小可运行的 Netty 服务端和本地 demo client
- 给后续补齐 2025 正式字段表留出清晰扩展点

## 当前实现范围

当前代码实现的是**工程骨架 + 可运行示例**，不是 2025 标准正文逐字段最终版。

已实现：

- Netty TCP 服务端
- `ByteToMessageDecoder` 拆包
- `MessageToByteEncoder` 编码
- BCC 异或校验
- VIN 会话管理
- 登录/实时/补发/登出基础解析
- Demo 报文构造
- JUnit 测试样例

未完全实现但已预留：

- 2025 正式字段表
- 分帧重组完整落地
- BMS / pack / cell 结构化模型
- 事故信息正式字段解析
- 正式平台应答格式
- RSA / AES 真正解密逻辑

## 技术选型

- **Java 17**：成熟稳定，长期支持
- **Maven**：Java 项目最常见的构建方式之一
- **Netty 4.1.x**：处理 TCP 长连接、半包/粘包、并发连接非常合适
- **JUnit 5**：主流测试框架

## 最小必要假设

由于原文档明确提到“字段、长度、取值范围应以 2025 正文为准”，但 workspace 中提供的文档本身没有完整官方字段表，所以本项目做了这些最小假设：

1. 总包结构沿用常见 32960 报文格式：`## + cmd + ack + vin + encryptFlag + dataLen + data + bcc`
2. BCC 校验规则采用从 `cmd` 到数据单元末尾的异或校验
3. 登录 / 实时 / 补发 / 登出命令字采用文档中的常见值
4. 信息项解析先按示例骨架实现，后续再替换为正式字段表
5. 应答先做最小可运行版本，不假装自己已经百分百符合正式平台协议

## 项目结构

```text
src/main/java/demo/gbt32960/
├── Application.java
├── codec/
├── handler/
├── model/
├── parser/
├── response/
├── session/
└── util/
```

## 启动方式

### 1. 启动服务端

```bash
mvn -q exec:java
```

默认监听：

- Host: `0.0.0.0`
- Port: `32960`

### 2. 启动服务端并附带本地 demo client

```bash
mvn -q exec:java -Dexec.args="--demo-client"
```

这会：

1. 启动服务端
2. 自动连接一个本地客户端
3. 发送登录和实时上报示例报文
4. 在控制台看到服务端解析结果和客户端收到的应答

## 测试方式

```bash
mvn test
```

当前测试覆盖：

- 编码/解码往返验证
- 半包处理验证
- 实时报文解析验证

## 打包方式

```bash
mvn package
```

## 后续优化建议

1. 把 `BodyParser` 改成**严格按 2025 正文字段表**解析
2. 引入配置化字段定义，而不是继续把字段写死在 Java 里
3. 实现完整的分帧重组模块
4. 增加事故信息、超级电容、燃料电池、BMS/Pack/Cell 的结构化模型
5. 补充正式平台应答和错误码语义
6. 增加 Integration Test 和 GitHub Actions
