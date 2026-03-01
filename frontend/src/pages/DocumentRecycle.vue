<template>
  <div class="p-6">
    <h2 class="text-xl font-bold mb-6">回收站</h2>
    
    <!-- 空状态提示 -->
    <div v-if="deletedDocuments.length === 0" class="text-center py-10 text-gray-500">
      回收站暂无文档
    </div>

    <!-- 回收站文档列表（与普通文章展示样式一致） -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="doc in deletedDocuments"
        :key="doc.id"
        class="card bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow relative"
      >
        <!-- 封面 -->
        <div class="h-48 bg-gray-100 relative overflow-hidden">
          <img
            :src="doc.coverImage || ('https://picsum.photos/400/300?random=' + doc.id)"
            class="w-full h-full object-cover"
          />
          <!-- 文件夹名称 -->
          <div class="absolute top-2 right-2 bg-blue-600 text-white text-xs px-2 py-1 rounded">
            {{ doc.folderName || '未分类' }}
          </div>
          <!-- 回收站标记 -->
          <div class="absolute top-2 left-2 bg-red-600 text-white text-xs px-2 py-1 rounded">
            已删除
          </div>
        </div>

        <!-- 内容 -->
        <div class="p-4">
          <!-- 文档标题 + 摘要 -->
          <h3 class="font-bold text-lg mb-2 line-clamp-2">{{ doc.title }}</h3>
        
          <!-- 作者 + 删除时间 -->
          <div class="flex justify-between items-center text-xs text-gray-500 mb-3">
            <span> {{ doc.authorName || '作者未知' }}</span>
            <span> {{ formatDate(doc.updateTime) }}</span>
          </div>
           <!-- 标签列表 -->
          <div class="flex flex-wrap gap-1 mb-2">
            <span
              v-for="tag in (doc.tags || [])"
              :key="tag.id || tag"
              class="text-xs bg-gray-100 px-2 py-0.5 rounded-full"
            >
              {{ tag.tagName || tag }}
            </span>
          </div>

          <!-- 回收站操作按钮 -->
          <div class="flex gap-4 text-xs">
            <el-button 
              type="primary" 
              size="small" 
              @click.stop="restoreDocument(doc.id)"
            >恢复
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click.stop="deletePermanently(doc.id)"
            >永久删除
            </el-button>
          </div>
        </div>

        <div
          class="absolute inset-0 cursor-pointer"
           style="pointer-events: none; z-index: -1;" -->
          @click="openDoc(doc.id)"
        ></div>
      </div>
    </div>
  </div>
</template>

<script>
import api from "../api/document";
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

export default {
  setup() {
    const deletedDocuments = ref([]);

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      if (Number.isNaN(date.getTime())) return dateString;
      return date.toLocaleString();
    };

    // 组装回收站文档的关联数据（作者、文件夹、标签）
    const assembleRecycleDocData = async (doc) => {
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

        // 3. 查询标签列表
        if (doc.tagIds && doc.tagIds.trim()) {
          const tagRes = await api.getTags(doc.tagIds);
          doc.tags = tagRes.data || [];
        } else {
          doc.tags = [];
        }

        return doc;
      } catch (error) {
        console.error(`组装回收站文档${doc.id}数据失败:`, error);
        doc.authorName = "未知";
        doc.folderName = "未分类";
        doc.tags = [];
        return doc;
      }
    };

    // 加载回收站文档
    const loadDeletedDocuments = async () => {
      try {
        const res = await api.getRecycle();
        const list = res.data.items ?? res.data ?? [];

        // 批量组装关联数据
        const promiseList = list.map(item => assembleRecycleDocData({
          id: item.id,
          title: item.title,
          content:item.content,
          coverImage: item.coverImage,
          tagIds: item.tagIds,
          authorId: item.authorId,
          folderId: item.folderId,
          createTime: item.createTime,
          updateTime: item.updateTime, 
          isDeleted: item.isDeleted
        }));

        deletedDocuments.value = await Promise.all(promiseList);
      } catch (error) {
        ElMessage.error('加载回收站文档失败');
        console.error('加载回收站文档失败', error);
      }
    };

    // 恢复文档
    const restoreDocument = async (id) => {
      try {
        await api.recover(id);
        ElMessage.success('文档已恢复');
        loadDeletedDocuments(); // 刷新列表
      } catch (error) {
        ElMessage.error('恢复失败：' + (error.response?.data?.message || '服务器错误'));
        console.error(error);
      }
    };

    // 永久删除（增加二次确认）
    const deletePermanently = async (id) => {
      try {
        await ElMessageBox.confirm(
          '确定要永久删除该文档吗？此操作不可恢复！',
          '永久删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        await api.deletePer(id); // 调用后端永久删除接口
        ElMessage.success('文档已永久删除');
        loadDeletedDocuments(); // 刷新列表
      } catch (error) {
        if (error !== 'cancel') { // 排除取消操作的异常
          ElMessage.error('永久删除失败：' + (error.response?.data?.message || '服务器错误'));
          console.error(error);
        }
      }
    };

    // 打开文档详情（可选）
    const openDoc = (id) => {
      window.location.href = `/documents/${id}`; 
    };

    onMounted(() => {
      loadDeletedDocuments();
    });

    return {
      deletedDocuments,
      formatDate,
      restoreDocument,
      deletePermanently,
      openDoc
    };
  }
};
</script>

<style scoped>
/* 覆盖层调整：避免操作按钮被点击覆盖层遮挡 */
.card >>> .el-button {
  position: relative;
  z-index: 10;
}
</style>