<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">汽车维修车间 · 工单与工位管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/bays">工位与设备</el-menu-item>
        <el-menu-item index="/orders">维修工单</el-menu-item>
        <el-menu-item index="/dispatch">派工与占用</el-menu-item>
        <el-menu-item index="/parts">配件领用</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
