import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')
const outputDir = path.join(repoRoot, 'docs', 'assets', 'diagrams')

fs.mkdirSync(outputDir, { recursive: true })

const styles = {
  actor: 'shape=mxgraph.flowchart.terminator;whiteSpace=wrap;html=1;rounded=1;fillColor=#d5e8d4;strokeColor=#82b366;fontSize=14;fontStyle=1;',
  app: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=14;',
  service: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=13;',
  store: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;size=15;fillColor=#f8cecc;strokeColor=#b85450;fontSize=13;',
  note: 'shape=note;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontSize=12;',
  edge: 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#4a4a4a;fontSize=12;',
  dashed: 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#7f8c8d;dashed=1;fontSize=12;'
}

function esc(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function drawioXml({ name, width = 1600, height = 960, nodes, edges }) {
  const cells = [
    '<mxCell id="0"/>',
    '<mxCell id="1" parent="0"/>'
  ]

  nodes.forEach((node, index) => {
    const id = node.id || `n${index + 2}`
    node.id = id
    cells.push(
      `<mxCell id="${id}" value="${esc(node.label)}" style="${node.style}" vertex="1" parent="1">` +
        `<mxGeometry x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" as="geometry"/>` +
      '</mxCell>'
    )
  })

  edges.forEach((edge, index) => {
    const id = `e${index + 200}`
    const label = edge.label ? ` value="${esc(edge.label)}"` : ''
    cells.push(
      `<mxCell id="${id}"${label} style="${edge.style || styles.edge}" edge="1" parent="1" source="${edge.source}" target="${edge.target}">` +
        '<mxGeometry relative="1" as="geometry"/>' +
      '</mxCell>'
    )
  })

  return `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<mxfile host="app.diagrams.net" modified="2026-04-09T00:00:00.000Z" agent="Codex" version="24.7.17" type="device">\n` +
    `  <diagram id="${name}" name="Page-1">\n` +
    `    <mxGraphModel dx="${width}" dy="${height}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${width}" pageHeight="${height}" math="0" shadow="0">\n` +
    `      <root>\n        ${cells.join('\n        ')}\n      </root>\n` +
    `    </mxGraphModel>\n` +
    `  </diagram>\n` +
    `</mxfile>\n`
}

const diagrams = [
  {
    file: 'system-architecture.drawio',
    spec: {
      name: 'system-architecture',
      nodes: [
        { label: '管理员 / 教师 / 学生 / 监考员', x: 40, y: 180, w: 220, h: 72, style: styles.actor },
        { label: '浏览器访问层\nVue 3 + Vite + Element Plus', x: 330, y: 180, w: 250, h: 78, style: styles.app },
        { label: '认证与权限\n登录、注册、找回密码、按钮权限、组织范围过滤', x: 660, y: 60, w: 260, h: 88, style: styles.service },
        { label: '考试核心\n题库、组卷、考试发布、学生答题、自动保存、交卷', x: 660, y: 180, w: 260, h: 88, style: styles.service },
        { label: '治理与运营\n阅卷、复核重判、成绩分析、消息通知、配置中心', x: 660, y: 300, w: 260, h: 88, style: styles.service },
        { label: '反作弊与审计\n设备检测、行为事件、登录风险、审计日志', x: 660, y: 420, w: 260, h: 88, style: styles.service },
        { label: 'Spring Boot 单体后端\nController / Service / Mapper / Security', x: 1010, y: 180, w: 290, h: 88, style: styles.app },
        { label: 'MySQL 正式数据源\n用户、题库、试卷、考试、成绩、通知、日志', x: 1370, y: 120, w: 220, h: 86, style: styles.store },
        { label: 'H2 回归数据源\n本地快速启动与集成测试', x: 1370, y: 250, w: 220, h: 76, style: styles.store },
        { label: '脚本与运维资源\n初始化、备份恢复、压测、Playwright 回归', x: 1370, y: 380, w: 220, h: 86, style: styles.note },
        { label: 'AI 题库辅助接口\n仅通过环境变量读取外部模型配置', x: 1010, y: 60, w: 290, h: 78, style: styles.note }
      ],
      edges: [
        { source: 'n2', target: 'n3' },
        { source: 'n3', target: 'n7', label: 'REST API / JWT' },
        { source: 'n4', target: 'n7' },
        { source: 'n5', target: 'n7' },
        { source: 'n6', target: 'n7' },
        { source: 'n7', target: 'n8' },
        { source: 'n7', target: 'n9', style: styles.dashed, label: '本地测试 / E2E' },
        { source: 'n10', target: 'n7', style: styles.dashed, label: '脚本驱动' },
        { source: 'n11', target: 'n7', style: styles.dashed, label: '可选外部能力' }
      ]
    }
  },
  {
    file: 'core-business-flow.drawio',
    spec: {
      name: 'core-business-flow',
      nodes: [
        { label: '题库管理\n录题、导入、AI 草稿、知识点治理', x: 40, y: 180, w: 200, h: 78, style: styles.app },
        { label: '试卷组装\n手工 / 随机 / 策略组卷', x: 290, y: 180, w: 200, h: 78, style: styles.app },
        { label: '考试发布\n时间窗口、考生分配、签到与准考证', x: 540, y: 180, w: 220, h: 78, style: styles.app },
        { label: '通知协同\n开考提醒、站内消息、MOCK_SMS', x: 820, y: 70, w: 220, h: 76, style: styles.service },
        { label: '学生考试工作区\n设备检测、作答、自动保存、交卷', x: 820, y: 180, w: 240, h: 78, style: styles.app },
        { label: '阅卷治理\n自动判分、人工评分、复核、重判、申诉', x: 1110, y: 180, w: 240, h: 78, style: styles.app },
        { label: '成绩发布与查询\n消息跳转、成绩详情、答卷回看、错题本', x: 1400, y: 180, w: 240, h: 78, style: styles.app },
        { label: '分析与质量报告\n排行榜、分布、知识点分析、质量报告', x: 1690, y: 180, w: 240, h: 78, style: styles.app },
        { label: '监考与审计\n风险事件、登录风险、操作留痕', x: 1110, y: 320, w: 240, h: 76, style: styles.service }
      ],
      edges: [
        { source: 'n2', target: 'n3', label: '题目入卷' },
        { source: 'n3', target: 'n4', label: '生成考试计划' },
        { source: 'n4', target: 'n5', label: '发布 / 提醒' },
        { source: 'n4', target: 'n6', label: '进入考试' },
        { source: 'n6', target: 'n7', label: '提交答卷' },
        { source: 'n7', target: 'n8', label: '发布成绩' },
        { source: 'n8', target: 'n9', label: '统计与报告' },
        { source: 'n6', target: 'n9', style: styles.dashed, label: '事件留痕' }
      ]
    }
  },
  {
    file: 'role-permission-map.drawio',
    spec: {
      name: 'role-permission-map',
      nodes: [
        { label: 'ADMIN\n全局配置、组织与用户、全局审计', x: 60, y: 60, w: 220, h: 76, style: styles.actor },
        { label: 'ORG_ADMIN\n组织范围用户与内容治理', x: 60, y: 170, w: 220, h: 76, style: styles.actor },
        { label: 'TEACHER\n题库、试卷、考试发布、分析', x: 60, y: 280, w: 220, h: 76, style: styles.actor },
        { label: 'GRADER\n阅卷、复核、成绩治理', x: 60, y: 390, w: 220, h: 76, style: styles.actor },
        { label: 'PROCTOR\n监考事件与风险核查', x: 60, y: 500, w: 220, h: 76, style: styles.actor },
        { label: 'STUDENT\n待考、答题、成绩、答卷回看', x: 60, y: 610, w: 220, h: 76, style: styles.actor },
        { label: '系统管理域\n组织 / 用户 / 角色 / 菜单 / 配置 / 审计', x: 400, y: 70, w: 320, h: 82, style: styles.app },
        { label: '考试业务域\n题库 / 试卷 / 考试发布 / 成绩分析', x: 400, y: 230, w: 320, h: 82, style: styles.app },
        { label: '治理域\n阅卷 / 申诉 / 通知 / 监考 / 风险', x: 400, y: 390, w: 320, h: 82, style: styles.app },
        { label: '学生端域\n待考列表 / 工作区 / 成绩中心 / 回看', x: 400, y: 550, w: 320, h: 82, style: styles.app },
        { label: '数据边界\n组织范围过滤 + 按钮权限 + 接口鉴权', x: 840, y: 300, w: 320, h: 96, style: styles.note }
      ],
      edges: [
        { source: 'n2', target: 'n8' },
        { source: 'n2', target: 'n7' },
        { source: 'n3', target: 'n7' },
        { source: 'n3', target: 'n8' },
        { source: 'n4', target: 'n8' },
        { source: 'n4', target: 'n9' },
        { source: 'n5', target: 'n9' },
        { source: 'n6', target: 'n10' },
        { source: 'n7', target: 'n11', style: styles.dashed },
        { source: 'n8', target: 'n11', style: styles.dashed },
        { source: 'n9', target: 'n11', style: styles.dashed },
        { source: 'n10', target: 'n11', style: styles.dashed }
      ]
    }
  },
  {
    file: 'deployment-topology.drawio',
    spec: {
      name: 'deployment-topology',
      nodes: [
        { label: '教师 / 学生 / 管理员浏览器', x: 40, y: 220, w: 240, h: 76, style: styles.actor },
        { label: '前端应用\nVue 3 + Vite + Element Plus', x: 360, y: 220, w: 280, h: 82, style: styles.app },
        { label: '后端应用\nSpring Boot 3 + Spring Security + MyBatis-Plus', x: 740, y: 220, w: 340, h: 82, style: styles.app },
        { label: 'MySQL\n正式业务数据', x: 1180, y: 150, w: 220, h: 82, style: styles.store },
        { label: '本地 H2\n快速启动 / 集成测试', x: 1180, y: 290, w: 220, h: 76, style: styles.store },
        { label: '文档与脚本\n初始化、备份恢复、压测、Playwright', x: 740, y: 390, w: 340, h: 88, style: styles.note },
        { label: '可选 AI 网关\n仅环境变量接入，不提交真实 Key', x: 1180, y: 420, w: 220, h: 82, style: styles.note }
      ],
      edges: [
        { source: 'n2', target: 'n3', label: 'HTTPS / SPA' },
        { source: 'n3', target: 'n4', label: 'REST API / JWT' },
        { source: 'n4', target: 'n5' },
        { source: 'n4', target: 'n6', style: styles.dashed, label: '本地开发 / 测试' },
        { source: 'n7', target: 'n4', style: styles.dashed, label: '运维支持' },
        { source: 'n8', target: 'n4', style: styles.dashed, label: '可选外部调用' }
      ]
    }
  },
  {
    file: 'init-startup-flow.drawio',
    spec: {
      name: 'init-startup-flow',
      nodes: [
        { label: '拉取仓库代码', x: 60, y: 200, w: 180, h: 68, style: styles.actor },
        { label: '准备环境\nJava 17+ / Maven / Node.js / MySQL', x: 300, y: 200, w: 220, h: 78, style: styles.app },
        { label: '执行数据库初始化\nsql/mysql/init.sql 或 verify-mysql-init.ps1', x: 580, y: 200, w: 260, h: 78, style: styles.app },
        { label: '启动后端\nH2 快速模式 或 MySQL profile', x: 920, y: 120, w: 240, h: 78, style: styles.app },
        { label: '启动前端\nnpm install / npm run dev', x: 920, y: 280, w: 240, h: 78, style: styles.app },
        { label: '健康检查与登录验证\n/api/system/runtime/health + 账号登录', x: 1240, y: 200, w: 260, h: 86, style: styles.service },
        { label: '主流程验收\n题库 → 组卷 → 发布 → 考试 → 阅卷 → 成绩', x: 1580, y: 200, w: 300, h: 86, style: styles.service }
      ],
      edges: [
        { source: 'n2', target: 'n3' },
        { source: 'n3', target: 'n4' },
        { source: 'n4', target: 'n5' },
        { source: 'n4', target: 'n6' },
        { source: 'n5', target: 'n6' },
        { source: 'n6', target: 'n7' }
      ]
    }
  }
]

for (const diagram of diagrams) {
  fs.writeFileSync(path.join(outputDir, diagram.file), drawioXml(diagram.spec), 'utf8')
}

console.log(`Generated ${diagrams.length} drawio sources in ${outputDir}`)
