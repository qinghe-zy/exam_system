# 本地启动说明

## 一、适用范围
用于开发环境或验收环境中启动当前在线考试系统前后端。

## 二、启动前准备
- 安装 Java 17+
- 安装 Maven
- 安装 Node.js 与 npm
- 如需使用 MySQL 模式，请确保本地 MySQL 服务可用

## 三、后端启动
### 方案 A：默认 H2 快速模式
1. 进入 `backend`
2. 执行 `mvn -q -DskipTests package`
3. 执行 `java -jar target/exam-system-backend-0.1.0-SNAPSHOT.jar`

### 方案 B：MySQL 模式
需要先设置：
- `SPRING_PROFILES_ACTIVE=mysql`
- `MYSQL_HOST=127.0.0.1`
- `MYSQL_PORT=3306`
- `MYSQL_DATABASE=exam_system`
- `MYSQL_USERNAME=root`
- `MYSQL_PASSWORD=本地密码`

然后：
1. 进入 `backend`
2. 执行 `mvn -q -DskipTests package`
3. 执行 `java -jar target/exam-system-backend-0.1.0-SNAPSHOT.jar`

## 四、前端启动
1. 进入 `frontend`
2. 执行 `npm.cmd install`
3. 执行 `npm.cmd run dev`

## 五、访问地址
- 后端：`http://localhost:8083`
- 前端：`http://localhost:5173`

## 六、验证方式
- 登录页可打开
- 可用测试账号登录
- 管理端能读取组织、用户、题库、考试等页面数据
