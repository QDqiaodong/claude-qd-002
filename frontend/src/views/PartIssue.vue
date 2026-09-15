<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>配件台账</span>
          <el-input v-model="query.keyword" placeholder="编号或名称" clearable size="small" style="width:170px" />
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:120px">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
          <el-button size="small" type="primary" @click="loadParts">查询</el-button>
          <el-button size="small" type="success" @click="openPart()">新增配件</el-button>
          <el-button size="small" type="warning" @click="openIssue('领用')">领料</el-button>
          <el-button size="small" @click="openIssue('退料')">退料</el-button>
        </div>
      </template>
      <el-table :data="parts" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="配件编号" width="120" />
        <el-table-column prop="name" label="配件名称" min-width="150" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.stock <= row.warnStock ? '#e6a23c' : '' }">
              {{ row.stock }}{{ row.stock <= row.warnStock ? ' ↓' : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="warnStock" label="预警线" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPart(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>领用流水</span>
          <el-select v-model="filterOrderId" placeholder="只看某张工单" clearable size="small" style="width:200px">
            <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} · ${o.plate}`" :value="o.id" />
          </el-select>
          <el-button size="small" type="primary" @click="loadIssues">查询</el-button>
          <span style="margin-left:auto;color:#909399">共 {{ issues.length }} 条</span>
        </div>
      </template>
      <el-table :data="issues" border stripe size="small" v-loading="loadingIssues">
        <el-table-column label="工单" width="150">
          <template #default="{ row }">{{ orderLabel(row.orderId) }}</template>
        </el-table-column>
        <el-table-column label="配件" min-width="150">
          <template #default="{ row }">{{ partName(row.partId) }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.kind === '领用' ? 'danger' : 'success'" size="small">{{ row.kind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="经手人" width="110" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="partVisible" :title="partForm.id ? '调整配件' : '新增配件'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="配件编号">
          <el-input v-model="partForm.code" :disabled="!!partForm.id" placeholder="如 PT-1008" />
        </el-form-item>
        <el-form-item label="配件名称"><el-input v-model="partForm.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="partForm.spec" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="partForm.stock" :min="0" /></el-form-item>
        <el-form-item label="预警线"><el-input-number v-model="partForm.warnStock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="partForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="partVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPart">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" :title="issueForm.kind === '领用' ? '领料' : '退料'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="工单">
          <el-select v-model="issueForm.orderId" placeholder="选一张施工中的工单" style="width:100%">
            <el-option
              v-for="o in pickableOrders"
              :key="o.id"
              :label="`${o.orderNo} · ${o.plate} · ${o.status}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="配件">
          <el-select v-model="issueForm.partId" placeholder="选一个在用配件" style="width:100%">
            <el-option
              v-for="p in usableParts"
              :key="p.id"
              :label="`${p.name}（${p.code} · 库存 ${p.stock}）`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="issueForm.qty" :min="1" /></el-form-item>
        <el-form-item label="经手人"><el-input v-model="issueForm.operator" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIssue">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { issueApi, orderApi, partApi } from '../api'

const parts = ref([])
const orders = ref([])
const issues = ref([])
const loading = ref(false)
const loadingIssues = ref(false)
const query = reactive({ keyword: '', status: '' })
const filterOrderId = ref(null)

const partVisible = ref(false)
const partForm = reactive({ id: null, code: '', name: '', spec: '', stock: 0, warnStock: 0, status: '在用' })

const issueVisible = ref(false)
const issueForm = reactive({ orderId: null, partId: null, qty: 1, kind: '领用', operator: '' })

const usableParts = computed(() => parts.value.filter((p) => p.status === '在用'))
const pickableOrders = computed(() => orders.value.filter((o) => o.status === '施工中' || o.status === '待质检'))

const partName = (id) => parts.value.find((p) => p.id === id)?.name || `#${id}`
const orderLabel = (id) => {
  const o = orders.value.find((x) => x.id === id)
  return o ? `${o.orderNo} · ${o.plate}` : `#${id}`
}

const loadParts = async () => {
  loading.value = true
  try {
    parts.value = await partApi.list({
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const loadIssues = async () => {
  loadingIssues.value = true
  try {
    issues.value = await issueApi.list(filterOrderId.value || undefined)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loadingIssues.value = false
  }
}

const openPart = (row) => {
  if (row) {
    Object.assign(partForm, row)
  } else {
    Object.assign(partForm, { id: null, code: '', name: '', spec: '', stock: 0, warnStock: 0, status: '在用' })
  }
  partVisible.value = true
}

const submitPart = async () => {
  try {
    if (partForm.id) {
      await partApi.update(partForm.id, {
        name: partForm.name,
        spec: partForm.spec,
        stock: partForm.stock,
        warnStock: partForm.warnStock,
        status: partForm.status
      })
    } else {
      await partApi.create({ ...partForm })
    }
    ElMessage.success('已保存')
    partVisible.value = false
    await loadParts()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openIssue = (kind) => {
  Object.assign(issueForm, { orderId: null, partId: null, qty: 1, kind, operator: '' })
  issueVisible.value = true
}

const submitIssue = async () => {
  try {
    await issueApi.create({ ...issueForm })
    ElMessage.success(issueForm.kind === '领用' ? '已领料' : '已退料')
    issueVisible.value = false
    await loadParts()
    await loadIssues()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    orders.value = await orderApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadParts()
  await loadIssues()
})
</script>
