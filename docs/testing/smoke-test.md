# 烟雾测试说明

## 一、目标
验证当前系统最关键的构建、数据库与主流程接口是否正常。

## 二、后端验证
1. `mvn -q -DskipTests compile`
2. `mvn -q test`
3. `mvn -q -DskipTests package`

## 三、前端验证
1. `npm.cmd run build`

## 四、数据库回归
1. 删除并重建 `exam_system`
2. 导入 `sql/mysql/init.sql`
3. 检查核心表计数
4. 使用 MySQL 模式启动后端

## 五、关键接口 smoke
- 管理员登录
- 学生登录
- 组织列表
- 用户列表
- 审计日志列表
- 题库导出
- 待考列表
- 分析概览

## 六、预期结果
- 接口返回 `code = 0`
- 系统能读取 MySQL 中的种子数据
- 关键主链路不因数据库缺失而报错
