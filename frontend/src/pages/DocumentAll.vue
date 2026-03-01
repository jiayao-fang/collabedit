<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
    <div
      v-for="doc in documents"
      :key="doc.id"
      class="card bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow relative"
    >
      <!-- 封面 -->
      <div class="h-48 bg-gray-100 relative overflow-hidden">
        <img
          :src="doc.coverImage || ('https://picsum.photos/400/300?random=' + doc.id)"
          class="w-full h-full object-cover"
        />
        <!-- 显示文件夹名 -->
        <div class="absolute top-2 right-2 bg-blue-600 text-white text-xs px-2 py-1 rounded">
          {{ doc.folderName || '未分类' }}
        </div>
      </div>

      <!-- 内容 -->
      <div class="p-4">
        <h3 class="font-bold text-lg mb-2 line-clamp-2">{{ doc.title }}</h3>
        <div class="flex justify-between items-center text-xs text-gray-500 mb-3">
          <span>作者: {{ doc.authorName }}</span>
          <span>{{ formatDate(doc.createTime) }}</span>
        </div>

         <div class="flex flex-wrap gap-3 mb-2 mt=5">
          <span
            v-for="tag in (doc.tags || [])"
            :key="tag.id || tag"
            class="text-xs bg-gray-100 px-2 py-0.5 mt=4 rounded-full"
          >
            {{ tag.tagName || tag }}
          </span>
        </div>
      </div>

      <!-- 点击覆盖层 -->
      <div
        class="absolute inset-0 cursor-pointer"
        @click="openDoc(doc.id)"
      ></div>
    </div>
  </div>
</template>

<script>
import api from "../api/document";
import { ref, onMounted } from "vue";

export default {
  setup() {
    const documents = ref([]);
    // 组装文档的关联数据
    const assembleDocumentData = async (doc) => {
      try {
        // 1. 查询作者名
        if (doc.authorId) {
          const authorRes = await api.getAuthorName(doc.authorId);
          doc.authorName = authorRes.data;
        } else {
          doc.authorName = "未知";
        }

        // 2. 查询文件夹名
        if (doc.folderId) {
          const folderRes = await api.getFolderName(doc.folderId);
          doc.folderName = folderRes.data;
        } else {
          doc.folderName = "未分类";
        }

        // 3. 批量查询标签列表
        if (doc.tagIds && doc.tagIds.trim()) {
          const tagRes = await api.getTags(doc.tagIds);
          doc.tags = tagRes.data || [];
        } else {
          doc.tags = [];
        }
        return doc;
      } catch (error) {
        console.error(`组装文档${doc.id}数据失败:`, error);
        // 异常时赋值默认值
        doc.authorName = "未知";
        doc.folderName = "未分类";
        doc.tags = [];
        return doc;
      }
    };

     // 加载文档并组装数据
    const loadDocuments = async () => {
      try {
        // 1. 获取原有基础文档列表
        const res = await api.getAll();
        const list = res.data.items ?? res.data ?? [];

        // 2. 遍历每个文档，组装关联数据
        const promiseList = list.map(item => assembleDocumentData({
          id: item.id,
          title: item.title,
          summary: item.summary,
          coverImage: item.coverImage,
          tagIds: item.tagIds, 
          authorId: item.authorId, 
          folderId: item.folderId,
          createTime: item.createTime
        }));

        // 3. 等待所有组装完成
        documents.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('加载文档失败:', error);
        documents.value = [];
      }
    };

     // 格式化日期
    const formatDate = val => {
      if (!val) return '';
      const d = new Date(val);
      return isNaN(d.getTime()) ? '' : d.toLocaleDateString();
    };

    const openDoc = id => {
      window.location.href = `/documents/${id}`;
    };

    onMounted(loadDocuments);

    return { documents, formatDate, openDoc };
  }
};
</script>
