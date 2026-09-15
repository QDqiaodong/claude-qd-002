<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <span>看哪天</span>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width:170px" @change="load" />
        <el-button type="primary" @click="load">刷新</el-button>
        <span style="margin-left:auto;color:#909399">
          当日 {{ dayOrders.length }} 张单 · 占用工位 {{ busyBays.length }} 个 · 在岗技师 {{ onDuty.length }} 人
        </span>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>工位占用</template>
      <el-table :data="bayRows" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="工位" width="100" />
        <el-table-column prop="name" label="工位名称" min-width="140" />
        <el-table-column label="工位状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '可用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名下设备状态" width="150">
          <template #default="{ row }">
            <span v-if="row.badEquipments.length" style="color:#e6a23c">
              {{ row.badEquipments.join('、') }}
            </span>
            <span v-else style="color:#67c23a">全部可用</span>
          </template>
        </el-table-column>
        <el-table-column label="当天在这口工位上的单" min-width="320">
          <template #default="{ row }">
            <div v-if="row.orders.length">
              <div v-for="o in row.orders" :key="o.id" style="line-height:22px">
                <el-tag size="small">{{ o.startMin != null ? `${hm(o.startMin)}-${hm(o.endMin)}` : '未排' }}</el-tag>
                {{ o.orderNo }} · {{ o.plate }} · {{ techName(o.technicianId) }}
                <el-tag size="small" :type="tagType(o.status)">{{ o.status }}</el-tag>
              </div>
            </div>
            <span v-else style="color:#c0c4cc">空着</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>技师当日在做</template>
      <el-table :data="techRows" border stripe size="small">
        <el-table-column prop="code" label="工号" width="100" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="level" label="等级" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在岗' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当天排的单" min-width="320">
          <template #default="{ row }">
            <div v-if="row.orders.length">
              <div v-for="o in row.orders" :key="o.id" style="line-height:22px">
                <el-tag size="small">{{ o.startMin != null ? `${hm(o.startMin)}-${hm(o.endMin)}` : '未排' }}</el-tag>
                {{ o.orderNo }} · {{ o.plate }} · {{ bayName(o.bayId) }}
              </div>
            </div>
            <span v-else style="color:#c0c4cc">当天没排单</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bayApi, equipmentApi, orderApi, technicianApi } from '../api'

const date = ref(new Date().toISOString().slice(0, 10))
const orders = ref([])
const bays = ref([])
const techs = ref([])
const equipments = ref([])
const loading = ref(false)

const dayOrders = computed(() =>
  orders.value.filter((o) => o.planDate === date.value && o.status !== '已取消' && o.status !== '已交车')
)
const busyBays = computed(() => new Set(dayOrders.value.map((o) => o.bayId)).size)
const onDuty = computed(() => techs.value.filter((t) => t.status === '在岗'))

const hm = (min) => `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
const techName = (id) => (id ? techs.value.find((t) => t.id === id)?.name || `#${id}` : '未派')
const bayName = (id) => (id ? bays.value.find((b) => b.id === id)?.name || `#${id}` : '未派')
const tagType = (s) =>
  s === '待质检' ? 'warning' : s === '施工中' ? 'danger' : s === '已交车' ? 'success' : ''

const bayRows = computed(() =>
  bays.value.map((b) => ({
    ...b,
    orders: dayOrders.value.filter((o) => o.bayId === b.id),
    badEquipments: equipments.value
      .filter((e) => e.bayId === b.id && e.status !== '可用')
      .map((e) => `${e.name}(${e.status})`)
  }))
)

const techRows = computed(() =>
  techs.value.map((t) => ({
    ...t,
    orders: dayOrders.value.filter((o) => o.technicianId === t.id)
  }))
)

const load = async () => {
  loading.value = true
  try {
    orders.value = await orderApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const [b, t, e] = await Promise.all([bayApi.list(), technicianApi.list(), equipmentApi.list({})])
    bays.value = b
    techs.value = t
    equipments.value = e
  } catch (err) {
    ElMessage.error(err.message)
  }
  load()
})
</script>
