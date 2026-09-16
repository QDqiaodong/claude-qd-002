import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const bayApi = {
  list: () => http.get('/bays'),
  create: (data) => http.post('/bays', data),
  setStatus: (id, status) => http.put(`/bays/${id}/status?status=${encodeURIComponent(status)}`)
}

export const equipmentApi = {
  list: (params) => http.get('/equipments', { params }),
  create: (data) => http.post('/equipments', data),
  update: (id, data) => http.put(`/equipments/${id}`, data)
}

export const technicianApi = {
  list: () => http.get('/technicians'),
  create: (data) => http.post('/technicians', data),
  setStatus: (id, status) =>
    http.put(`/technicians/${id}/status?status=${encodeURIComponent(status)}`)
}

export const orderApi = {
  list: (params) => http.get('/orders', { params }),
  open: (data) => http.post('/orders', data),
  assign: (id, data) => http.put(`/orders/${id}/assign`, data),
  advance: (id, action, qcResult) =>
    http.post(`/orders/${id}/advance`, null, { params: { action, qcResult } }),
  qcItems: (id) => http.get(`/orders/${id}/qc-items`),
  submitQc: (id, items) => http.post(`/orders/${id}/qc`, { items })
}

export const partApi = {
  list: (params) => http.get('/parts', { params }),
  create: (data) => http.post('/parts', data),
  update: (id, data) => http.put(`/parts/${id}`, data)
}

export const issueApi = {
  list: (orderId) => http.get('/issues', { params: orderId ? { orderId } : {} }),
  create: (data) => http.post('/issues', data)
}

export default http
