<template>
  <div class="p-6" v-loading="loading" element-loading-text="搜索中...">
    <div>
      <h2 class="text-xl font-bold">
        搜索结果: "{{ searchQuery }}"
      </h2>
      <p class="text-gray-500 text-sm">找到 {{ results.length }} 个相关文档</p>
    </div>
    <div class="border-b p-4 flex items-center justify-center bg-white mb-5">
    </div>
    
    <!-- 空状态提示 -->
    <div v-if="results.length === 0 && !loading" class="text-center py-10 text-gray-500">
      暂无匹配的文档
    </div>
    
    <!-- 搜索结果列表 -->
    <div v-if="!loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">
      <div 
        v-for="doc in results" 
        :key="doc.id" 
        class="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow relative cursor-pointer"
        @click="$router.push(`/documents/${doc.id}`)"
      >
        <!-- 原有卡片内容不变 -->
        <div class="w-30 h-48 bg-gray-100 relative overflow-hidden">
          <img 
            :src="doc.coverImage || 'https://picsum.photos/400/300?random=' + doc.id" 
            alt="文档封面" 
            class="w-full h-full object-cover"
          >
          <div class="absolute top-2 right-2 bg-blue-600 text-white text-xs px-2 py-1 rounded">
            {{ doc.folderName || '未分类' }}
          </div>
        </div>
        
        <div class="p-4">
          <div class="flex flex-wrap gap-1 mb-2">
            <span 
              v-for="tag in doc.tags" 
              :key="tag.id || tag" 
              class="text-xs bg-gray-100 px-2 py-0.5 rounded-full"
            >
              {{ tag.tagName || tag }}
            </span>
          </div>
          
          <h3 class="font-bold text-lg mb-2 line-clamp-2">{{ doc.title }}</h3>
          
          <div class="flex justify-between items-center text-xs text-gray-500">
            <span>作者: {{ doc.authorName || '未知' }}</span>
            <span>{{ formatDate(doc.createTime) }}</span>
          </div>
        </div>
      </div>

      
    </div>
     <button 
          type="text" 
          icon="el-icon-arrow-left" 
          @click="$router.go(-1)"
        >
          返回
    </button>
  </div>
</template>

<script>
import api from "../api/document";
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";

export default {
  setup() {
    const results = ref([]);
    const searchQuery = ref('');
    const loading = ref(false);
    const route = useRoute();
    
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return Number.isNaN(date.getTime()) ? dateString : date.toLocaleDateString();
    };

    const assembleDocData = async (doc) => {
      try {
        if (doc.authorId) {
          const authorRes = await api.getAuthorName(doc.authorId);
          doc.authorName = authorRes.data || '未知';
        } else {
          doc.authorName = '未知';
        }
        if (doc.folderId) {
          const folderRes = await api.getFolderName(doc.folderId);
          doc.folderName = folderRes.data || '未分类';
        } else {
          doc.folderName = '未分类';
        }
        if (doc.tagIds) {
          const tagRes = await api.getTags(doc.tagIds);
          doc.tags = tagRes.data || [];
        } else {
          doc.tags = [];
        }
        return doc;
      } catch (error) {
        console.error(`组装文档${doc.id}数据失败`, error);
        doc.authorName = '未知';
        doc.folderName = '未分类';
        doc.tags = [];
        return doc;
      }
    };
    
    const loadSearchResults = async () => {
  try {
    const q = route.query.q;
    searchQuery.value = q || '';

    // 1. 查文档
    const res = await api.search(q);
    const docs = res.data || [];

    // 2. 查作者 / 文件夹 / 标签
    const promiseList = docs.map(doc => assembleDocData(doc));
    results.value = await Promise.all(promiseList);

  } catch (error) {
    console.error('搜索失败', error);
  }
};

    
    onMounted(() => {
      loadSearchResults();
    });
    
    return {
      results,
      searchQuery,
      loading,
      formatDate
    };
  }
};
</script>