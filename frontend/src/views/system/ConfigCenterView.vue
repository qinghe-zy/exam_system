<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import {
  createConfigItem,
  createDictionaryItem,
  deleteConfigItem,
  deleteDictionaryItem,
  fetchConfigItems,
  fetchDictionaryItems,
  type ConfigItemRecord,
  type DictionaryItemRecord,
  updateConfigItem,
  updateDictionaryItem
} from '../../api/system'

const loading = ref(false)
const configs = ref<ConfigItemRecord[]>([])
const dictionaries = ref<DictionaryItemRecord[]>([])
const configDialogVisible = ref(false)
const dictionaryDialogVisible = ref(false)
const configMode = ref<'create' | 'edit'>('create')
const dictionaryMode = ref<'create' | 'edit'>('create')
const currentConfigId = ref<number | null>(null)
const currentDictionaryId = ref<number | null>(null)

const configForm = reactive<Omit<ConfigItemRecord, 'id'>>({
  configKey: '',
  configName: '',
  configGroup: 'exam',
  configValue: '',
  descriptionText: '',
  status: 1
})

const dictionaryForm = reactive<Omit<DictionaryItemRecord, 'id'>>({
  dictType: 'question_type',
  itemCode: '',
  itemLabel: '',
  itemValue: '',
  sortNo: 1,
  status: 1
})

async function loadData() {
  loading.value = true
  try {
    const [configList, dictList] = await Promise.all([fetchConfigItems(), fetchDictionaryItems()])
    configs.value = configList
    dictionaries.value = dictList
  } finally {
    loading.value = false
  }
}

function resetConfig() {
  Object.assign(configForm, {
    configKey: '',
    configName: '',
    configGroup: 'exam',
    configValue: '',
    descriptionText: '',
    status: 1
  })
  currentConfigId.value = null
}

function resetDictionary() {
  Object.assign(dictionaryForm, {
    dictType: 'question_type',
    itemCode: '',
    itemLabel: '',
    itemValue: '',
    sortNo: 1,
    status: 1
  })
  currentDictionaryId.value = null
}

function openCreateConfig() {
  configMode.value = 'create'
  resetConfig()
  configDialogVisible.value = true
}

function openEditConfig(row: ConfigItemRecord) {
  configMode.value = 'edit'
  currentConfigId.value = row.id
  Object.assign(configForm, { ...row })
  configDialogVisible.value = true
}

function openCreateDictionary() {
  dictionaryMode.value = 'create'
  resetDictionary()
  dictionaryDialogVisible.value = true
}

function openEditDictionary(row: DictionaryItemRecord) {
  dictionaryMode.value = 'edit'
  currentDictionaryId.value = row.id
  Object.assign(dictionaryForm, { ...row })
  dictionaryDialogVisible.value = true
}

async function submitConfig() {
  if (configMode.value === 'create') await createConfigItem(configForm)
  else if (currentConfigId.value) await updateConfigItem(currentConfigId.value, configForm)
  ElMessage.success(configMode.value === 'create' ? '系统参数已创建' : '系统参数已更新')
  configDialogVisible.value = false
  await loadData()
}

async function submitDictionary() {
  if (dictionaryMode.value === 'create') await createDictionaryItem(dictionaryForm)
  else if (currentDictionaryId.value) await updateDictionaryItem(currentDictionaryId.value, dictionaryForm)
  ElMessage.success(dictionaryMode.value === 'create' ? '字典项已创建' : '字典项已更新')
  dictionaryDialogVisible.value = false
  await loadData()
}

async function removeConfig(id: number) {
  await ElMessageBox.confirm('确认删除该系统参数？', '提示', { type: 'warning' })
  await deleteConfigItem(id)
  ElMessage.success('系统参数已删除')
  await loadData()
}

async function removeDictionary(id: number) {
  await ElMessageBox.confirm('确认删除该字典项？', '提示', { type: 'warning' })
  await deleteDictionaryItem(id)
  ElMessage.success('字典项已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="配置中心与字典中心"
    description="该页面集中管理系统参数、题型、标签、通知类别等基础配置，用于支撑考试规则、题库元数据和通知协同的统一管理。"
  >
    <section class="config-grid">
      <article class="panel-card section-card">
        <div class="section-header">
          <h3>系统参数</h3>
          <el-button type="primary" @click="openCreateConfig">新增参数</el-button>
        </div>
        <el-table :data="configs" v-loading="loading">
          <el-table-column prop="configKey" label="参数键" min-width="180" />
          <el-table-column prop="configName" label="参数名称" min-width="160" />
          <el-table-column prop="configGroup" label="分组" min-width="120" />
          <el-table-column prop="configValue" label="参数值" min-width="160" />
          <el-table-column prop="descriptionText" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" min-width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditConfig(row)">编辑</el-button>
              <el-button link type="danger" @click="removeConfig(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="panel-card section-card">
        <div class="section-header">
          <h3>字典项</h3>
          <el-button type="primary" @click="openCreateDictionary">新增字典项</el-button>
        </div>
        <el-table :data="dictionaries" v-loading="loading">
          <el-table-column prop="dictType" label="字典类型" min-width="140" />
          <el-table-column prop="itemCode" label="编码" min-width="140" />
          <el-table-column prop="itemLabel" label="名称" min-width="160" />
          <el-table-column prop="itemValue" label="值" min-width="140" />
          <el-table-column prop="sortNo" label="排序" min-width="90" />
          <el-table-column label="操作" min-width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditDictionary(row)">编辑</el-button>
              <el-button link type="danger" @click="removeDictionary(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>

    <el-dialog v-model="configDialogVisible" :title="configMode === 'create' ? '新增系统参数' : '编辑系统参数'" width="min(720px, 94vw)" @closed="resetConfig">
      <div class="grid-two">
        <el-form-item label="参数键"><el-input v-model="configForm.configKey" /></el-form-item>
        <el-form-item label="参数名称"><el-input v-model="configForm.configName" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="configForm.configGroup" /></el-form-item>
        <el-form-item label="参数值"><el-input v-model="configForm.configValue" /></el-form-item>
      </div>
      <el-form-item label="说明"><el-input v-model="configForm.descriptionText" type="textarea" :rows="3" /></el-form-item>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitConfig">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dictionaryDialogVisible" :title="dictionaryMode === 'create' ? '新增字典项' : '编辑字典项'" width="min(720px, 94vw)" @closed="resetDictionary">
      <div class="grid-two">
        <el-form-item label="字典类型"><el-input v-model="dictionaryForm.dictType" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="dictionaryForm.itemCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="dictionaryForm.itemLabel" /></el-form-item>
        <el-form-item label="值"><el-input v-model="dictionaryForm.itemValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dictionaryForm.sortNo" :min="1" /></el-form-item>
      </div>
      <template #footer>
        <el-button @click="dictionaryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDictionary">保存</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.config-grid {
  display: grid;
  gap: 1rem;
}

.section-card {
  padding: 1rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.8rem;
}

.section-header h3 {
  margin: 0;
  font-family: 'Literata', Georgia, serif;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 760px) {
  .grid-two {
    grid-template-columns: 1fr;
  }
}
</style>
