# 后端说明

## 定位

后端负责在线考试系统的认证、权限、题库、试卷、考试发布、学生答题、阅卷治理、通知协同、监考与运维接口。

## 当前重点

- Spring Boot 3 单体架构，模块边界按认证、系统管理、考试核心、治理与通知划分
- JWT 登录、组织范围隔离、服务端接口鉴权
- MySQL 为正式交付口径，H2 用于本地快速启动和集成测试

## 常用命令

```powershell
cd backend
mvn -q test
mvn -q -DskipTests package
```

## 验证口径

- 后端集成测试通过
- Swagger 可访问
- MySQL profile 可正常连接本地 `exam_system`
