<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>工位</span>
          <el-button size="small" type="success" @click="openBay">新增工位</el-button>
          <span style="margin-left:auto;color:#909399">
            可用 {{ bays.filter(b => b.status === '可用').length }} 个 / 共 {{ bays.length }} 个
          </span>
        </div>
      </template>
      <el-table :data="bays" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="工位编号" width="120" />
        <el-table-column prop="name" label="工位名称" min-width="160" />
        <el-table-column prop="kind" label="类型" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '可用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名下设备" width="110">
          <template #default="{ row }">
            {{ equipments.filter(e => e.bayId === row.id).length }} 台
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link :type="row.status === '可用' ? 'danger' : 'primary'" @click="toggleBay(row)">
              {{ row.status === '可用' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>设备台账</span>
          <el-select v-model="query.bayId" placeholder="按工位" clearable size="small" style="width:170px">
            <el-option v-for="b in bays" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:130px">
            <el-option label="可用" value="可用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修中" value="维修中" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="编号或名称" clearable size="small" style="width:170px" />
          <el-button size="small" type="primary" @click="loadEquipments">查询</el-button>
          <el-button size="small" type="success" @click="openEquipment()">新增设备</el-button>
        </div>
      </template>
      <el-table :data="equipments" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="设备编号" width="120" />
        <el-table-column prop="name" label="设备名称" min-width="150" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="归属工位" width="150">
          <template #default="{ row }">{{ bayName(row.bayId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '可用' ? 'success' : row.status === '维修中' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEquipment(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="bayVisible" title="新增工位" width="440px">
      <el-form label-width="90px">
        <el-form-item label="工位编号"><el-input v-model="bayForm.code" placeholder="如 B-06" /></el-form-item>
        <el-form-item label="工位名称"><el-input v-model="bayForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="bayForm.kind" style="width:100%">
            <el-option v-for="k in ['举升', '地沟', '钣金', '喷漆']" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bayVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBay">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="equipmentVisible" :title="equipForm.id ? '调整设备' : '新增设备'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="设备编号">
          <el-input v-model="equipForm.code" :disabled="!!equipForm.id" placeholder="如 EQ-1009" />
        </el-form-item>
        <el-form-item label="设备名称"><el-input v-model="equipForm.name" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="equipForm.category" style="width:100%">
            <el-option v-for="c in ['举升', '检测', '拆装', '钣金', '钣喷']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属工位">
          <el-select v-model="equipForm.bayId" clearable placeholder="可先不归" style="width:100%">
            <el-option v-for="b in bays" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="equipForm.status" style="width:100%">
            <el-option label="可用" value="可用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修中" value="维修中" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="equipmentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEquipment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bayApi, equipmentApi } from '../api'

const bays = ref([])
const equipments = ref([])
const loading = ref(false)
const query = reactive({ bayId: null, status: '', keyword: '' })

const bayVisible = ref(false)
const bayForm = reactive({ code: '', name: '', kind: '举升' })
const equipmentVisible = ref(false)
const equipForm = reactive({ id: null, code: '', name: '', category: '拆装', bayId: null, status: '可用' })

const bayName = (id) => bays.value.find((b) => b.id === id)?.name || '未归位'

const loadBays = async () => {
  bays.value = await bayApi.list()
}

const loadEquipments = async () => {
  loading.value = true
  try {
    equipments.value = await equipmentApi.list({
      bayId: query.bayId || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openBay = () => {
  Object.assign(bayForm, { code: '', name: '', kind: '举升' })
  bayVisible.value = true
}

const submitBay = async () => {
  try {
    await bayApi.create({ ...bayForm })
    ElMessage.success('工位已新增')
    bayVisible.value = false
    await loadBays()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const toggleBay = async (row) => {
  try {
    await bayApi.setStatus(row.id, row.status === '可用' ? '停用' : '可用')
    ElMessage.success('已更新')
    await loadBays()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openEquipment = (row) => {
  if (row) {
    Object.assign(equipForm, row)
  } else {
    Object.assign(equipForm, { id: null, code: '', name: '', category: '拆装', bayId: null, status: '可用' })
  }
  equipmentVisible.value = true
}

const submitEquipment = async () => {
  try {
    if (equipForm.id) {
      await equipmentApi.update(equipForm.id, {
        name: equipForm.name,
        category: equipForm.category,
        bayId: equipForm.bayId,
        status: equipForm.status
      })
    } else {
      await equipmentApi.create({ ...equipForm })
    }
    ElMessage.success('已保存')
    equipmentVisible.value = false
    await loadEquipments()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    await loadBays()
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadEquipments()
})
</script>
