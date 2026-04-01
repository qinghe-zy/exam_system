# 后端说明

## 当前定位

当前后端已从 00-base-admin 派生，重点承担 在线考试系统 的核心业务逻辑。

## 当前实现重点

- 保留基础认证与系统管理基线，并扩展考试业务模块
- 继承统一响应、统一异常、JWT、系统管理基线
- 新增项目业务实体、DTO、VO、Mapper、Service、Controller

## 当前验证

- mvn -q clean -DskipTests package
- mvn -q test
- MySQL profile 测试上下文通过