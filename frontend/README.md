# 前端说明

## 当前定位

当前前端已从 00-base-admin 派生，重点承担 在线考试系统 的管理端页面。

## 当前实现重点

- 在共享管理端壳层上增加题库、试卷、考试记录页面
- 保留登录、布局、系统管理与仪表盘基线
- 新增项目业务 API、路由与视图页面

## 当前验证

- 
pm.cmd install
- 
pm.cmd run build
"@

  Write-Utf8NoBom "D:\Projectexample\projects\exam-system\docs\module-summary.md" @"
# 模块说明

## 当前已实现模块

- 题库、试卷管理、考试记录与结果

## 当前模块边界

- 共享能力继续来自 00-base-admin
- 业务模块保留在当前项目内部
- 后续如出现重复模式，再考虑抽取到 shared/ 或蓝图层