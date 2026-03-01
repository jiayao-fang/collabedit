<template>
  <div class="p-6" v-loading="loading" element-loading-text="加载中...">
    <!-- 返回按钮与标题（根据是否选中文件夹动态切换） -->
    <div class="mb-6">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="handleBackClick"
      >
        返回
      </el-button>
      <h2 class="text-xl font-bold mt-2">
        {{ currentFolderId ? `文件夹: ${currentFolderName}` : `文件夹搜索结果: "${searchKeyword}"` }}
      </h2>
      <p class="text-gray-500 text-sm">
        {{ currentFolderId 
          ? `该文件夹下有 ${documents.length} 个文档` 
          : `找到 ${results.length} 个相关文件夹` 
        }}
      </p>
    </div>

    <!-- 1. 文件夹搜索结果列表（未选中文件夹时显示） -->
    <div v-if="!currentFolderId && !loading">
      <!-- 文件夹搜索空状态 -->
      <div v-if="results.length === 0" class="text-center py-10 text-gray-500">
        暂无匹配的文件夹
      </div>
      <!-- 文件夹列表 -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-10" >
        <div 
          v-for="folder in results" 
          :key="folder.id"
          class="bg-white rounded-xl shadow-md p-4 hover:shadow-lg transition-shadow cursor-pointer"
          @click="handleFolderClick(folder.id, folder.folderName)"
        >
          <div class="flex items-center">
            <div class="w-10 h-10 rounded bg-blue-100 flex items-center justify-center text-blue-600 mr-3">
              <i class="el-icon-folder text-xl"></i>
            </div>
            <div>
              <h3 class="font-medium">{{ folder.folderName }}</h3>
              <p class="text-sm text-gray-500 mt-1">{{ folder.docCount || 0 }} 个文档</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 文件夹下的文档列表（选中文件夹时显示） -->
    <div v-if="currentFolderId && !loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
      <!-- 文档卡片 -->
      <div 
        v-for="doc in documents" 
        :key="doc.id" 
        class="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow relative"
      >
        <div class="h-48 bg-gray-100 relative overflow-hidden">
          <img 
            :src="doc.coverImage || 'https://picsum.photos/400/300?random=' + doc.id" 
            alt="文档封面" 
            class="w-full h-full object-cover"
          >
        </div>
        
        <div class="p-4">
          <div class="flex flex-wrap gap-1 mb-2">
            <span 
              v-for="tag in doc.tags" 
              :key="tag.id" 
              class="text-xs bg-gray-100 px-2 py-0.5 rounded-full"
            >
              {{ tag.tagName || '未知' }}
            </span>
          </div>
          
          <h3 class="font-bold text-lg mb-2 line-clamp-2">{{ doc.title || '无标题' }}</h3>
          <p class="text-gray-600 text-sm mb-3 line-clamp-2"></p>
          
          <div class="flex justify-between items-center text-xs text-gray-500">
            <span>作者: {{ doc.editorName || '未知' }}</span>
            <span>{{ formatDate(doc.createTime) }}</span>
          </div>
        </div>
        
        <!-- 点击查看详情 -->
        <div 
          class="absolute inset-0 cursor-pointer"
          @click="$router.push(`/documents/${doc.id}`)"
        ></div>
      </div>
      
      <!-- 文档空状态 -->
      <div v-if="documents.length === 0" class="col-span-full text-center py-10">
        <p class="text-gray-500">该文件夹中没有文档</p>
        <el-button 
          type="text" 
          class="mt-2"
          @click="$router.push('/documents/publish')"
        >
          发布新文档
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import api from "../api/document";
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";

export default {
  setup() {
    // 搜索相关
    const results = ref([]);
    const searchKeyword = ref('');
    const loading = ref(false);
    
    // 文件夹文档相关
    const documents = ref([]);
    const currentFolderId = ref('');
    const currentFolderName = ref('');
    
    const route = useRoute();
    const router = useRouter();

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      // 兼容数字型时间戳
      const timestamp = typeof dateString === 'number' ? dateString : new Date(dateString).getTime();
      return Number.isNaN(timestamp) ? dateString : new Date(timestamp).toLocaleDateString();
    };

    // 组装文件夹数据
    const assembleFolderData = async (folder) => {
      try {
        if (folder.creatorId) {
          const authorRes = await api.getAuthorName(folder.creatorId);
          folder.creatorName = authorRes.data || '未知';
        } else {
          folder.creatorName = '未知';
        }
        if (folder.id) {
          const docCountRes = await api.getFolderDocCount(folder.id);
          folder.docCount = Number(docCountRes.data) || 0; // 统一为数字
          folder.id = String(folder.id); // 修正大写Id为小写id
        } else {
          folder.docCount = 0;
        }
        return folder;
      } catch (error) {
        console.error('加载文件夹失败', error);
        folder.creatorName = '未知';
        folder.docCount = 0;
        return folder;
      }
    };

    // 组装文档数据
    const assembleDocumentData = async (doc) => {
      try {
        // 1. 查询作者名
        if (doc.authorId) {
          const authorRes = await api.getAuthorName(doc.authorId);
          doc.authorName = authorRes.data || '未知';
        } else {
          doc.authorName = '未知';
        }

        // 2. 查询文件夹名
        if (doc.folderId) {
          const folderRes = await api.getFolderName(doc.folderId);
          doc.folderName = folderRes.data || '未分类';
        } else {
          doc.folderName = '未分类';
        }

        // 3. 查询标签列表
        if (doc.tagIds && doc.tagIds.trim()) {
          const tagRes = await api.getTags(doc.tagIds);
          doc.tags = tagRes.data || [];
        } else {
          doc.tags = [];
        }

        // 字段兜底
        doc.title = doc.title || '无标题';
        doc.createTime = doc.createTime || '';
        return doc;
      } catch (error) {
        console.error(`组装文档${doc.id}数据失败:`, error);
        doc.authorName = '未知';
        doc.folderName = '未分类';
        doc.tags = [];
        doc.title = doc.title || '无标题';
        return doc;
      }
    };

    // 加载文件夹搜索结果
    const loadFolderSearchResults = async () => {
      try {
        loading.value = true;
        const keyword = route.query.q || '';
        searchKeyword.value = keyword;

        if (!keyword) {
          results.value = [];
          return;
        }

        const res = await api.folderSearch(keyword);
        const folders = res.data || [];
        const promiseList = folders.map(folder => assembleFolderData(folder));
        results.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('搜索文件夹失败', error);
        ElMessage.error('搜索文件夹失败，请稍后重试');
        results.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 加载指定文件夹中的文档
    const loadDocumentsInFolder = async (folderId) => {
      try {
        loading.value = true;
        const res = await api.categoryByFolder(folderId);
        const rawDocs = res.data.items ?? res.data ?? [];

        // 批量组装文档数据
        const promiseList = rawDocs.map(item => assembleDocumentData({
          id: item.id,
          title: item.title,
          coverImage: item.coverImage,
          tagIds: item.tagIds,
          authorId: item.authorId,
          editorName:item.editorName,
          folderId: item.folderId,
          createTime: item.createTime
        }));

        documents.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('加载文件夹文档失败', error);
        ElMessage.error('加载文档失败，请稍后重试');
        documents.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 点击文件夹
    const handleFolderClick = (folderId, folderName) => {
      currentFolderId.value = folderId;
      currentFolderName.value = folderName;
      loadDocumentsInFolder(folderId);
      // 路由保留搜索关键词，同时添加folderId
      router.push({
        name: route.name, // 保留当前页面路由名称
        query: {
          q: searchKeyword.value,
          folderId
        }
      });
    };

    // 返回逻辑优化
    const handleBackClick = () => {
      if (currentFolderId.value) {
        // 选中文件夹时，先返回搜索结果页
        currentFolderId.value = '';
        currentFolderName.value = '';
        documents.value = [];
        router.push({
          name: route.name,
          query: { q: searchKeyword.value }
        });
      } else {
        // 未选中文件夹时，返回上一页
        router.push('/main/all')
      }
    };

    // 监听路由参数（新增：支持刷新/直接访问带folderId的搜索页）
    watch(() => [route.query.q, route.query.folderId], async ([newQ, newFolderId]) => {
      // 搜索关键词变化，重新加载搜索结果
      if (newQ !== searchKeyword.value) {
        await loadFolderSearchResults();
      }
      // 文件夹ID变化，加载对应文档
      if (newFolderId) {
        currentFolderId.value = newFolderId;
        // 查找文件夹名称
        const targetFolder = results.value.find(f => f.id === newFolderId);
        if (targetFolder) {
          currentFolderName.value = targetFolder.folderName;
        }
        await loadDocumentsInFolder(newFolderId);
      } else {
        currentFolderId.value = '';
        currentFolderName.value = '';
        documents.value = [];
      }
    }, { immediate: true });

    onMounted(() => {
      loadFolderSearchResults();
    });

    return {
      results,
      searchKeyword,
      loading,
      documents,
      currentFolderId,
      currentFolderName,
      formatDate,
      handleFolderClick,
      handleBackClick
    };
  }
};
</script>