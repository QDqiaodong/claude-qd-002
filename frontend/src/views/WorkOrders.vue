<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:140px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="按进场日期" clearable style="width:170px" />
        <el-input v-model="query.keyword" placeholder="车牌 / 工单号 / 客户" clearable style="width:200px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openOrder">接车开单</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 张</span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="orderNo" label="工单号" width="110" />
      <el-table-column prop="plate" label="车牌" width="100" />
      <el-table-column prop="model" label="车型" min-width="120" />
      <el-table-column prop="customer" label="客户" width="90" />
      <el-table-column prop="kind" label="类型" width="80" />
      <el-table-column label="工位" width="130">
        <template #default="{ row }">{{ bayName(row.bayId) }}</template>
      </el-table-column>
      <el-table-column label="技师" width="100">
        <template #default="{ row }">{{ techName(row.technicianId) }}</template>
      </el-table-column>
      <el-table-column label="计划时段" width="170">
        <template #default="{ row }">
          <span v-if="row.startMin != null">{{ row.planDate }} {{ hm(row.startMin) }}-{{ hm(row.endMin) }}</span>
          <span v-else style="color:#c0c4cc">未排</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="质检" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.qcResult === '合格'" type="success" size="small">合格</el-tag>
          <template v-else-if="row.failedItems">
            <el-tag type="danger" size="small">返工</el-tag>
            <div style="color:#f56c6c;font-size:12px;line-height:1.4;margin-top:2px">
              不过项：{{ row.failedItems }}
            </div>
          </template>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button v-if="row.status === '待派工'" link type="primary" @click="openAssign(row)">派工</el-button>
          <el-button v-if="row.status === '施工中'" link type="primary" @click="advance(row, 'finish')">完工送检</el-button>
          <el-button
            v-if="row.status === '待质检'"
            link
            type="warning"
            @click="openQc(row)"
          >质检交车</el-button>
          <el-button
            v-if="row.status === '待派工' || row.status === '施工中'"
            link
            type="info"
            @click="cancel(row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="orderVisible" title="接车开单" width="480px">
      <el-form label-width="90px">
        <el-form-item label="车牌"><el-input v-model="orderForm.plate" placeholder="如 京A8D26" /></el-form-item>
        <el-form-item label="车型"><el-input v-model="orderForm.model" /></el-form-item>
        <el-form-item label="客户"><el-input v-model="orderForm.customer" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="orderForm.phone" /></el-form-item>
        <el-form-item label="业务类型">
          <el-radio-group v-model="orderForm.kind">
            <el-radio v-for="k in ['保养', '维修', '钣金', '喷漆']" :key="k" :label="k">{{ k }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="orderForm.faultDesc" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderVisible = false">取消</el-button>
        <el-button type="primary" @click="submitOrder">开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="派工" width="500px">
      <el-form label-width="100px">
        <el-form-item label="工单">
          <el-input :model-value="`${assignForm.orderNo} · ${assignForm.plate}`" disabled />
        </el-form-item>
        <el-form-item label="工位">
          <el-select v-model="assignForm.bayId" placeholder="只列可用工位" style="width:100%">
            <el-option
              v-for="b in freeBays"
              :key="b.id"
              :label="`${b.name}（${b.code} · ${b.kind}）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="技师">
          <el-select v-model="assignForm.technicianId" placeholder="只列在岗技师" style="width:100%">
            <el-option
              v-for="t in onDutyTechs"
              :key="t.id"
              :label="`${t.name}（${t.code} · ${t.level}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划进场日期">
          <el-date-picker v-model="assignForm.planDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="assignForm.start" start="08:00" step="00:30" end="20:00" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="assignForm.end" start="08:00" step="00:30" end="20:00" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">派工</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qcVisible" title="质检交车 · 逐项记过或不过" width="640px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom:12px"
        title="制动、灯光、路试三项全过才能交车；只要有一项不过，这张单退回施工中，并记下不过项。"
      />
      <el-form label-width="80px">
        <el-form-item label="工单">
          <el-input :model-value="`${qcForm.orderNo} · ${qcForm.plate} · ${qcForm.model || ''}`" disabled />
        </el-form-item>
      </el-form>
      <el-table :data="qcForm.items" border size="small">
        <el-table-column prop="item" label="质检项" width="100">
          <template #default="{ row }"><strong>{{ row.item }}</strong></template>
        </el-table-column>
        <el-table-column label="结论" width="200">
          <template #default="{ row }">
            <el-radio-group v-model="row.result">
              <el-radio label="过">过</el-radio>
              <el-radio label="不过">不过</el-radio>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="备注（不过请写明原因）">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="如：右前大灯不亮" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="qcVisible = false">取消</el-button>
        <el-button type="primary" :loading="qcSubmitting" @click="submitQc">提交质检结论</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bayApi, orderApi, technicianApi } from '../api'

const statuses = ['待派工', '施工中', '待质检', '已交车', '已取消']

const rows = ref([])
const bays = ref([])
const techs = ref([])
const loading = ref(false)
const query = reactive({ status: '', date: null, keyword: '' })

const orderVisible = ref(false)
const orderForm = reactive({ plate: '', model: '', customer: '', phone: '', kind: '保养', faultDesc: '' })

const assignVisible = ref(false)
const assignForm = reactive({
  id: null, orderNo: '', plate: '',
  bayId: null, technicianId: null, planDate: '', start: '09:00', end: '10:00'
})

// 固定三项，和后端 WorkOrderService.QC_ITEMS 一致；空项表（结论没选）不能提交
const QC_ITEM_NAMES = ['制动', '灯光', '路试']
const qcVisible = ref(false)
const qcSubmitting = ref(false)
const qcForm = reactive({ id: null, orderNo: '', plate: '', model: '', items: [] })

const freeBays = computed(() => bays.value.filter((b) => b.status === '可用'))
const onDutyTechs = computed(() => techs.value.filter((t) => t.status === '在岗'))

const hm = (min) => `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
const toMin = (t) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5))
const bayName = (id) => (id ? bays.value.find((b) => b.id === id)?.name || `#${id}` : '未派')
const techName = (id) => (id ? techs.value.find((t) => t.id === id)?.name || `#${id}` : '未派')
const tagType = (s) =>
  s === '已交车' ? 'success' : s === '待质检' ? 'warning' : s === '已取消' ? 'info' : s === '施工中' ? 'danger' : ''

const load = async () => {
  loading.value = true
  try {
    rows.value = await orderApi.list({
      status: query.status || undefined,
      date: query.date || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.status = ''
  query.date = null
  query.keyword = ''
  load()
}

const openOrder = () => {
  Object.assign(orderForm, { plate: '', model: '', customer: '', phone: '', kind: '保养', faultDesc: '' })
  orderVisible.value = true
}

const submitOrder = async () => {
  try {
    await orderApi.open({ ...orderForm })
    ElMessage.success('已开单，接下来请派工')
    orderVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openAssign = (row) => {
  Object.assign(assignForm, {
    id: row.id, orderNo: row.orderNo, plate: row.plate,
    bayId: null, technicianId: null,
    planDate: new Date().toISOString().slice(0, 10), start: '09:00', end: '10:00'
  })
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!assignForm.start || !assignForm.end) {
    ElMessage.warning('请把计划时段选完整')
    return
  }
  try {
    await orderApi.assign(assignForm.id, {
      bayId: assignForm.bayId,
      technicianId: assignForm.technicianId,
      planDate: assignForm.planDate,
      startMin: toMin(assignForm.start),
      endMin: toMin(assignForm.end)
    })
    ElMessage.success('派工完成，已进施工中')
    assignVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const advance = async (row, action, qcResult) => {
  try {
    await orderApi.advance(row.id, action, qcResult)
    ElMessage.success('已更新')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openQc = async (row) => {
  Object.assign(qcForm, {
    id: row.id, orderNo: row.orderNo, plate: row.plate, model: row.model || '',
    items: QC_ITEM_NAMES.map((name) => ({ item: name, result: '', remark: '' }))
  })
  qcVisible.value = true
  try {
    // 拉这张单的项表（旧的待质检单缺行时后端会补齐），把已有结论 / 备注回填
    const serverItems = await orderApi.qcItems(row.id)
    for (const local of qcForm.items) {
      const hit = serverItems.find((s) => s.item === local.item)
      if (hit) {
        local.result = hit.result || ''
        local.remark = hit.remark || ''
      }
    }
  } catch (e) {
    ElMessage.error(e.message)
    qcVisible.value = false
  }
}

const submitQc = async () => {
  const missing = qcForm.items.filter((i) => !i.result).map((i) => i.item)
  if (missing.length > 0) {
    ElMessage.warning(`项表还没记完：${missing.join('、')} 还没选过 / 不过，空项表不能交车`)
    return
  }
  const failed = qcForm.items.filter((i) => i.result === '不过')
  if (failed.length > 0) {
    try {
      await ElMessageBox.confirm(
        `${failed.map((i) => i.item).join('、')} 判了不过，提交后这张单退回施工中，确定？`,
        '有项不过，退回施工',
        { type: 'warning', confirmButtonText: '退回施工', cancelButtonText: '再查查' }
      )
    } catch {
      return
    }
  }
  qcSubmitting.value = true
  try {
    // 项表和状态在后端一个事务里提交：网断在半路也不会出现已交车但项表没齐
    const updated = await orderApi.submitQc(qcForm.id, qcForm.items.map((i) => ({
      item: i.item, result: i.result, remark: i.remark || null
    })))
    qcVisible.value = false
    if (updated.status === '已交车') {
      ElMessage.success('三项全过，已交车')
    } else {
      ElMessage.warning(`已退回施工中：${updated.qcResult || ''}`)
    }
    load()
  } catch (e) {
    // 重复点交车 / 状态已变 / 项没齐，后端都会拦下，错误直接透出来
    ElMessage.error(e.message)
  } finally {
    qcSubmitting.value = false
  }
}

const cancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消工单 ${row.orderNo}？`, '提示')
  } catch {
    return
  }
  advance(row, 'cancel')
}

onMounted(async () => {
  try {
    const [b, t] = await Promise.all([bayApi.list(), technicianApi.list()])
    bays.value = b
    techs.value = t
  } catch (e) {
    ElMessage.error(e.message)
  }
  load()
})
</script>
