<template>
  <div class="app-container h-screen flex flex-col bg-gray-50 w-[600px] mx-auto">
    <div class="border-b p-4 flex items-center justify-center bg-white">
      <h1 class="text-xl font-bold text-gray-800">协作编辑平台</h1>
    </div>
    <div class="flex-1 overflow-auto p-4">
      <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold">文档中心</h1>
      </div>
      <div class="mb-6">
        <!-- 文档搜索 -->
        <el-input
          v-model="docKeyword"
          placeholder="搜索文档或作者..."
          class="mb-4"
          suffix-icon="el-icon-search"
          @keyup.enter="handleSearch('document')"
        />

        <!-- 文件夹搜索 -->
        <el-input
          v-model="folderKeyword"
          placeholder="搜索文件夹..."
          class="mb-4"
          suffix-icon="el-icon-search"
          @keyup.enter="handleSearch('folder')"
        />

        <!-- 标签搜索 -->
        <el-input
          v-model="tagKeyword"
          placeholder="搜索标签..."
          class="mb-4"
          suffix-icon="el-icon-search"
          @keyup.enter="handleSearch('tag')"
        />
      </div>

      <!-- 文档视图切换 -->
      <el-tabs v-model="activeTab" class="mb-6">
        <el-tab-pane label="所有文档" name="all" />
        <el-tab-pane label="回收站" name="recycle" />
        <el-tab-pane label="文件夹" name="categoryByFolder" />
        <el-tab-pane label="标签" name="categoryByTag" />
      </el-tabs>

      <!-- 子路由视图 -->
      <router-view />
    </div>

    <!-- 底部导航栏 -->
    <div class="border-t p-2 bg-white fixed bottom-0 w-full max-w-[600px] h-[60px] flex items-center">
      <div class="flex justify-around items-center w-full">
        <div
          class="flex flex-col items-center py-2 px-4 text-gray-600 hover:text-blue-600"
          :class="{ 'text-blue-600': $route.path.includes('/main') }"
          @click="$router.push('/main/all')"
        >
          📄<span class="text-sm">文档</span>
        </div>

        <router-link 
          to="/contacts" 
          class="flex flex-col items-center py-2 px-4 text-gray-600 hover:text-blue-600"
        >
          <el-badge :value="contactUnreadCount" :hidden="contactUnreadCount === 0">
            <span class="text-xl">👥</span>
          </el-badge>
          <span class="text-sm">联系人</span>
        </router-link>

        <button
          class="w-14 h-14 rounded-full bg-blue-600 text-white flex items-center justify-center shadow-lg relative -top-6"
          @click="$router.push('/documents/publish')"
        >
          ╋
        </button>

        <div
          class="flex flex-col items-center py-2 px-4 cursor-pointer text-gray-600 hover:text-blue-600"
          @click="$router.push('/notifications')"
        >
          <el-badge
            :value="notificationUnreadCount"
            :hidden="notificationUnreadCount === 0"
            type="danger"
          >
            🔔
          </el-badge>
          <span class="text-sm">通知</span>
        </div>

        <router-link 
          to="/personal" 
          class="flex flex-col items-center py-2 px-4 text-gray-600 hover:text-blue-600"
        >
          👤<span class="text-sm">我的</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch,onMounted,onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import documentApi from "../api/document"; 

export default {
  setup() {
    const activeTab = ref("all");

    // 搜索框
    const docKeyword = ref("");
    const folderKeyword = ref("");
    const tagKeyword = ref("");

    const route = useRoute();
    const router = useRouter();
    const notificationUnreadCount = ref(0);
    const contactUnreadCount = ref(0);

    // 同步 tab 与路由
    watch(
      () => route.path,
      (path) => {
        if (path.includes("recycle")) activeTab.value = "recycle";
        else if (path.includes("categoryByFolder")) activeTab.value = "categoryByFolder";
        else if (path.includes("categoryByTag")) activeTab.value = "categoryByTag";
        else activeTab.value = "all";
      },
      { immediate: true }
    );

    watch(activeTab, (val) => {
      router.push(`/main/${val}`);
    });

    // 搜索跳转
    const handleSearch = (type) => {
      if (type === "document" && docKeyword.value.trim()) {
        router.push({ path: "/documents/search", query: { q: docKeyword.value } });
      }

      if (type === "folder" && folderKeyword.value.trim()) {
        router.push({ path: "/folder/search", query: { q: folderKeyword.value } });
      }

      if (type === "tag" && tagKeyword.value.trim()) {
        router.push({ path: "/tag/search", query: { q: tagKeyword.value } });
      }
    };

    // 从服务器获取未读通知数
    const fetchUnreadNotificationCount = async () => {
      try {
        const res = await documentApi.getUnreadNotificationCount();
        notificationUnreadCount.value = res.data || 0;
        // 同时更新localStorage中的值
        localStorage.setItem("notificationUnreadCount", res.data || 0);
      } catch (error) {
        console.error("获取未读通知数量失败:", error);
        loadUnreadCount();
      }
    };

    // 从 localStorage 读取未读数
    const loadUnreadCount = () => {
      notificationUnreadCount.value =
        Number(localStorage.getItem("notificationUnreadCount")) || 0;
    };

    const loadContactUnreadCount = () => {
      contactUnreadCount.value = Number(localStorage.getItem("contactUnreadCount")) || 0;
    };

    // 监听全局通知更新事件
    onMounted(() => {
      // 组件挂载时主动获取未读通知数
      fetchUnreadNotificationCount();
      window.addEventListener("notificationUpdated", fetchUnreadNotificationCount);
      // 加载联系人未读消息数
      loadContactUnreadCount();
      window.addEventListener("contactUpdated", loadContactUnreadCount);
    });

    onUnmounted(() => {
      window.removeEventListener("notificationUpdated", fetchUnreadNotificationCount);
      window.removeEventListener("contactUpdated", loadContactUnreadCount);
    });

    return {
      activeTab,
      docKeyword,
      folderKeyword,
      tagKeyword,
      handleSearch,
      notificationUnreadCount,
      contactUnreadCount
    };
  }
};
</script>

<style scoped>
.app-container {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
.flex-1 {
  min-height: 0;
  padding-bottom: 70px;
}
/* 通知角标样式 */
:deep(.el-badge__content) {
  transform: translate(50%, -50%);
}
</style>
