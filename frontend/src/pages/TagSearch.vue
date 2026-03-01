<template>
  <div class="p-6" v-loading="loading" element-loading-text="加载中...">
    <!-- 返回按钮与标题（根据是否选中标签动态切换） -->
    <div class="mb-6">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="handleBackClick"
      >
        返回
      </el-button>
      <h2 class="text-xl font-bold mt-2">
        {{ currentTagId ? `标签: ${currentTagName}` : `标签搜索结果: "${searchKeyword}"` }}
      </h2>
      <p class="text-gray-500 text-sm">
        {{ currentTagId 
          ? `该标签下有 ${documents.length} 个文档` 
          : `找到 ${results.length} 个相关标签` 
        }}
      </p>
    </div>

    <!-- 1. 标签搜索结果列表（未选中标签时显示） -->
    <div v-if="!currentTagId && !loading">
      <!-- 标签搜索空状态 -->
      <div v-if="results.length === 0" class="text-center py-10 text-gray-500">
        暂无匹配的标签
      </div>
      <!-- 标签列表 -->
      <div v-else class="flex flex-wrap gap-3">
        <div 
          v-for="tag in results" 
          :key="tag.id"
          class="bg-white rounded-xl shadow-md px-4 py-3 hover:shadow-lg transition-shadow cursor-pointer inline-flex items-center"
          @click="handleTagClick(tag.id, tag.tagName)"
        >
          <span class="text-sm bg-gray-100 px-2 py-0.5 rounded-full mr-2">
            {{ tag.tagName }}
          </span>
          <span class="text-xs text-gray-500">{{ tag.docCount || 0 }} 个文档</span>
        </div>
      </div>
    </div>

    <!-- 2. 标签下的文档列表（选中标签时显示） -->
    <div v-if="currentTagId && !loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
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
          <p class="text-gray-600 text-sm mb-3 line-clamp-2 "></p>
          
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
        <p class="text-gray-500">该标签下没有文档</p>
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
    
    // 标签文档相关（新增）
    const documents = ref([]);
    const currentTagId = ref('');
    const currentTagName = ref('');
    
    const route = useRoute();
    const router = useRouter();

    // 格式化日期（增强鲁棒性，兼容时间戳）
    const formatDate = (dateString) => {
      if (!dateString) return '';
      // 兼容数字型时间戳
      const timestamp = typeof dateString === 'number' ? dateString : new Date(dateString).getTime();
      return Number.isNaN(timestamp) ? dateString : new Date(timestamp).toLocaleDateString();
    };

    // 组装标签数据（优化：docCount统一为数字，ID转为字符串）
    const assembleTagData = async (tag) => {
      try {
        if (tag.id) {
          tag.id = String(tag.id); // 统一ID为字符串
          const docCountRes = await api.getTagDocCount(tag.id);
          tag.docCount = Number(docCountRes.data) || 0; // 统一为数字
        } else {
          tag.docCount = 0;
        }
        return tag;
      } catch (error) {
        console.error(`组装标签${tag.id || '未知'}数据失败`, error);
        tag.docCount = 0;
        return tag;
      }
    };

    // 组装文档数据（复用统一逻辑）
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

    // 加载标签搜索结果
    const loadTagSearchResults = async () => {
      try {
        loading.value = true;
        const keyword = route.query.q || '';
        searchKeyword.value = keyword;

        if (!keyword) {
          results.value = [];
          return;
        }

        const res = await api.tagSearch(keyword);
        const tags = res.data || [];
        const promiseList = tags.map(tag => assembleTagData(tag));
        results.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('搜索标签失败', error);
        ElMessage.error('搜索标签失败，请稍后重试');
        results.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 加载指定标签下的文档（改造：加入数据组装）
    const loadDocumentsByTag = async (tagId) => {
      try {
        loading.value = true;
        const res = await api.categoryByTag(tagId);
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
        console.error('加载标签文档失败', error);
        ElMessage.error('加载文档失败，请稍后重试');
        documents.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 点击标签（改造：保留搜索关键词，不跳转其他页面）
    const handleTagClick = (tagId, tagName) => {
      currentTagId.value = tagId;
      currentTagName.value = tagName;
      loadDocumentsByTag(tagId);
      // 路由保留搜索关键词+添加tagId，支持刷新保留状态
      router.push({
        name: route.name,
        query: {
          q: searchKeyword.value,
          tagId
        }
      });
    };

    // 返回逻辑优化（新增）
    const handleBackClick = () => {
      if (currentTagId.value) {
        // 选中标签时，先返回搜索结果页
        currentTagId.value = '';
        currentTagName.value = '';
        documents.value = [];
        router.push({
          name: route.name,
          query: { q: searchKeyword.value }
        });
      } else {
        // 未选中标签时，返回上一页
        router.push('/main/all');
      }
    };

    // 监听路由参数（新增：支持刷新/直接访问带参数的URL）
    watch(() => [route.query.q, route.query.tagId], async ([newQ, newTagId]) => {
      // 搜索关键词变化，重新加载搜索结果
      if (newQ !== searchKeyword.value) {
        await loadTagSearchResults();
      }
      // 标签ID变化，加载对应文档
      if (newTagId) {
        currentTagId.value = newTagId;
        // 查找标签名称
        const targetTag = results.value.find(t => t.id === newTagId);
        if (targetTag) {
          currentTagName.value = targetTag.tagName;
        }
        await loadDocumentsByTag(newTagId);
      } else {
        currentTagId.value = '';
        currentTagName.value = '';
        documents.value = [];
      }
    }, { immediate: true });

    onMounted(() => {
      loadTagSearchResults();
    });

    return {
      results,
      searchKeyword,
      loading,
      documents,
      currentTagId,
      currentTagName,
      formatDate,
      handleTagClick,
      handleBackClick
    };
  }
};
</script>