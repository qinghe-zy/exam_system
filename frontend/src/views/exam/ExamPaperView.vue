<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { usePermission } from '../../hooks/usePermission'
import { deletePaper, fetchPapers } from '../../api/exam'
import type { ExamPaper } from '../../types/exam'
import { labelPaperMode } from '../../utils/labels'

const router = useRouter()
const { hasPermission } = usePermission()
const loading = ref(false)
const papers = ref<ExamPaper[]>([])

async function loadData() {
  loading.value = true
  try {
    papers.value = await fetchPapers()
  } finally {
    loading.value = false
  }
}

function goCreate(mode: 'MANUAL' | 'RANDOM' | 'STRATEGY' = 'MANUAL') {
  router.push({ path: '/exam/papers/create', query: { mode } })
}

function goEdit(id: number) {
  router.push(`/exam/papers/${id}/edit`)
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该试卷？删除后考试发布将无法继续引用该卷。', '提示', { type: 'warning' })
  await deletePaper(id)
  ElMessage.success('试卷已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="试卷管理"
    title="试卷总览：列表页只看结果，建卷页专注完成创建"
    description="试卷管理入口已拆分为“列表页 + 独立建卷页”。基础信息、手工选题、随机组卷、策略组卷和卷面预览不再堆在同一个大弹窗中。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button v-if="hasPermission('exam:paper:create')" @click="goCreate('RANDOM')">新建随机组卷</el-button>
        <el-button v-if="hasPermission('exam:paper:create')" @click="goCreate('STRATEGY')">新建策略组卷</el-button>
        <el-button v-if="hasPermission('exam:paper:create')" type="primary" @click="goCreate('MANUAL')">新建试卷</el-button>
      </div>
    </template>

    <section class="intro-grid">
      <article class="panel-card intro-card">
        <strong>步骤 1：基础信息</strong>
        <p>先确定试卷编码、名称、学科、版本、时长、及格线和卷面说明。</p>
      </article>
      <article class="panel-card intro-card">
        <strong>步骤 2：组卷方式</strong>
        <p>在独立建卷页中切换手工选题、随机组卷、策略组卷，不再被大弹窗干扰。</p>
      </article>
      <article class="panel-card intro-card">
        <strong>步骤 3：预览保存</strong>
        <p>统一预览卷面组成、题型分布、难度分布和总分，确认无误后保存。</p>
      </article>
    </section>

    <section class="panel-card section-card">
      <el-table :data="papers" v-loading="loading">
        <el-table-column prop="paperCode" label="试卷编码" min-width="140" />
        <el-table-column prop="paperName" label="试卷名称" min-width="220" />
        <el-table-column prop="subject" label="学科" min-width="120" />
        <el-table-column prop="paperVersion" label="版本" min-width="120" />
        <el-table-column label="组卷方式" min-width="120">
          <template #default="{ row }">{{ labelPaperMode(row.assemblyMode) }}</template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题量" min-width="90" />
        <el-table-column prop="totalScore" label="总分" min-width="90" />
        <el-table-column prop="passScore" label="及格线" min-width="90" />
        <el-table-column label="发布状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('exam:paper:update')" link type="primary" @click="goEdit(row.id)">进入编辑</el-button>
            <el-button v-if="hasPermission('exam:paper:delete')" link type="danger" @click="removeItem(row.id)">删除</el-button>
            <span v-if="!hasPermission('exam:paper:update') && !hasPermission('exam:paper:delete')" class="muted">仅查看</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.hero-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.intro-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.intro-card {
  padding: 1rem;
}

.intro-card p {
  margin: 0.45rem 0 0;
  color: var(--muted);
  line-height: 1.65;
}

.section-card {
  padding: 1rem;
}

@media (max-width: 1080px) {
  .intro-grid {
    grid-template-columns: 1fr;
  }
}
</style>
