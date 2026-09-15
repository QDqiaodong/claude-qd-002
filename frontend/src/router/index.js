import { createRouter, createWebHistory } from 'vue-router'
import BayEquipment from '../views/BayEquipment.vue'
import WorkOrders from '../views/WorkOrders.vue'
import Dispatch from '../views/Dispatch.vue'
import PartIssue from '../views/PartIssue.vue'

const routes = [
  { path: '/', redirect: '/bays' },
  { path: '/bays', component: BayEquipment, meta: { title: '工位与设备' } },
  { path: '/orders', component: WorkOrders, meta: { title: '维修工单' } },
  { path: '/dispatch', component: Dispatch, meta: { title: '派工与占用' } },
  { path: '/parts', component: PartIssue, meta: { title: '配件领用' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
