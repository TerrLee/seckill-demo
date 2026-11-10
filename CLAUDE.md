# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

这是一个秒杀（seckill）系统的后端脚手架：Spring Boot 4.1.2 + MyBatis-Plus 3.5.16 + MySQL + Redis + Spring Security + JWT，编译目标 Java 17，根包为 `com.example.seckilldemo`。

当前状态：只实现了用户注册/登录、JWT 登录态、统一返回与全局异常处理。`ProductServiceImpl` 和 `SecKillOrderServiceImpl` 是空壳，秒杀核心流程（下单、扣库存、防超卖）尚未实现；`OrderDetailDTO` + `SeckkillOrderMapper.xml` 中的订单详情联表查询已就绪但还没有对应的 Controller。

## 运行依赖

- MySQL：`localhost:3306/seckill_db`，账号 `root/123456`（见 `src/main/resources/application.yaml`），依赖表 `sys_user`、`product`、`seckill_order`。仓库中**没有建表 SQL**，需自行准备数据库结构。
- Redis：`localhost:6379`，用于存储登录 Token。

## 常用命令

仓库只提交了 `.mvn/wrapper/maven-wrapper.properties`，没有 `mvnw` 脚本，且本机未安装 `mvn`。可以在 IntelliJ IDEA 中直接使用其内置 Maven，或先 `brew install maven`，然后：

```bash
mvn spring-boot:run                                   # 启动应用（8080 端口）
mvn clean package                                     # 打包
mvn test                                              # 运行全部测试
mvn test -Dtest=SeckillDemoApplicationTests           # 运行单个测试类
mvn test -Dtest=SeckillDemoApplicationTests#contextLoads  # 运行单个测试方法
```

## 架构

**分层结构**：Controller → Service 接口（继承 MyBatis-Plus `IService<T>`）→ ServiceImpl（继承 `ServiceImpl<Mapper, Entity>`）→ Mapper（继承 `BaseMapper<T>`）。`SeckillDemoApplication` 上的 `@MapperScan("com.example.seckilldemo.mapper")` 负责扫描 Mapper。

- 实体通过 `@TableName` 映射表名（如 `User` → `sys_user`），主键均为 `IdType.AUTO`。
- 自定义 SQL 放在 `src/main/resources/mapper/`，`mapper-locations: classpath*:mapper/**/*.xml`。注意现有文件名为 `SeckkillOrderMapper.xml`（多了一个 k），但其中的 `namespace` 是正确拼写 `SeckillOrderMapper`，按 namespace 匹配，不要按文件名重命名而不改引用。
- 分页插件在 `config/Config.java` 中注册（`PaginationInnerInterceptor(DbType.MYSQL)`）。

**认证链路**（改登录相关逻辑时必须整体考虑）：

1. `POST /seckill/register`、`POST /seckill/login` 由 `UserController` 暴露；密码用 `DigestUtils.md5DigestAsHex` 做 MD5 加密。
2. 登录成功后 `JwtUtils.generateToken(username, role)` 生成 JWT（subject=用户名，自定义 claim `role`），并以 `login:token:<jwt>` 为 key 存入 Redis，TTL 30 分钟。
3. `TokenFilter`（Spring Security 过滤器）读取 `Authorization: Bearer <token>`，校验 JWT 签名与过期时间 + Redis 中是否存在该登录态，然后向 `SecurityContextHolder` 写入认证信息，权限为 `ROLE_<角色小写>`。
4. `SecurityConfig` 采用无状态会话，白名单仅 `/seckill/register`、`/seckill/login`、`/error`，其余请求一律要求认证。新增公开接口时必须同步修改白名单。
5. Token 滑动续期：Redis 剩余 TTL 小于 600 秒时续到 30 分钟。

**统一响应与异常**：接口统一返回 `Result<T>`（`code/message/data`，`code=200` 表示成功）。Service 层抛出 `BusinessException`，由 `GlobalExceptionHandler` 转成 `Result.error(message)`；其余异常兜底返回固定文案。

## 已知不一致（修改相关代码时注意）

- Redis key 两端不一致：`UserServiceImpl` 写入的是 `"login:token" + token`（**缺少冒号**），而 `TokenFilter` 读取的是 `"login:token:" + token`，约定应统一为 `login:token:<jwt>`。
- `LogAspect` 的切点是 `com.example.demo2.service.*`，与实际包 `com.example.seckilldemo.service` 不符，当前不会拦截任何方法。
- Spring Boot 4 的 starter 命名与 Boot 3 不同（如 `spring-boot-starter-webmvc`、`spring-boot-starter-aspectj`、`spring-boot-starter-webmvc-test`），这是有意为之，不要改回 `spring-boot-starter-web`。
