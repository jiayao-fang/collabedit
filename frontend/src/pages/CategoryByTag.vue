<template>
  <div class="p-6">
    <div class="mb-6">
      <h2 class="text-xl font-bold mt-2">
        {{ currentTagId ? `标签: ${currentTagName}` : '所有标签' }}
      </h2>
    </div>
    
    <!-- 加载状态：标签列表加载中 -->
    <div v-if="loading && !currentTagId" class="col-span-full text-center py-10">
      <el-skeleton :rows="3" animated />
    </div>
    
    <!-- 如果没有选择具体标签，显示标签列表 -->
    <div v-if="!currentTagId && !loading" class="flex flex-wrap gap-3">
      <div 
        v-for="tag in tags" 
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
    
    <!-- 如果选择了具体标签，显示该标签下的文档 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
      <!-- 文档加载中 -->
      <div v-if="loading" v-for="i in 6" :key="i" class="bg-white rounded-xl shadow-md overflow-hidden">
        <el-skeleton :rows="6" animated>
          <template #template>
            <div class="h-48 bg-gray-100"></div>
            <div class="p-4"></div>
          </template>
        </el-skeleton>
      </div>
      
      <!-- 文档列表 -->
      <div 
        v-else v-for="doc in documents" 
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
      
      <!-- 空状态 -->
      <div v-if="!loading && documents.length === 0" class="col-span-full text-center py-10">
        <p class="text-gray-500">该标签下没有文档</p>
      </div>
    </div>

    <!-- 创建标签按钮 -->
    <div class="m-10 text-center " v-if="!currentTagId">
      <el-button
        type="primary"
        @click="openCreateTagDialog"
      >
        创建标签
      </el-button>
    </div>

    <!-- 创建标签弹窗 -->
    <el-dialog
      title="创建新标签"
      v-model="tagDialogVisible"
      width="400px"
    >
      <el-form
        ref="tagFormRef"
        :model="tagForm"
        :rules="tagRules"
        label-width="90px"
      >
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="tagForm.tagName" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="tagForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>

      <!-- Dialog Footer -->
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTag">
          确定
        </el-button>
      </template>
    </el-dialog>

     <div class="mt-4">
       <el-button 
        type="text" 
        @click="$router.go(-1)"
        class="text-gray-700"
      >
        返回
      </el-button>
      </div>
  </div>
</template>

<script>
import api from "../api/document";
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElSkeleton } from 'element-plus'; // 导入需要的组件/提示

export default {
  components: { ElSkeleton }, 
  setup() {
    const tags = ref([]);
    const documents = ref([]);
    const currentTagId = ref('');
    const currentTagName = ref('');
    const loading = ref(false); 
    const route = useRoute();
    const router = useRouter();

    // 新增：创建标签相关
    const tagDialogVisible = ref(false); 
    const tagFormRef = ref(null); // 表单ref
    const tagForm = ref({ // 表单数据
      tagName: '',
      remark: ''
    });
    // 表单校验规则
    const tagRules = ref({
      tagName: [
        { required: true, message: '请输入标签名称', trigger: 'blur' },
        { min: 1, max: 20, message: '名称长度在 1 到 20 个字符', trigger: 'blur' }
      ]
    });
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      // 兼容数字型时间戳
      const timestamp = typeof dateString === 'number' ? dateString : new Date(dateString).getTime();
      if (Number.isNaN(timestamp)) return dateString;
      const date = new Date(timestamp);
      // 统一格式：YYYY-MM-DD
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
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

        // 兜底字段
        doc.title = doc.title || '无标题';
        doc.createTime = doc.createTime || '';
        return doc;
      } catch (error) {
        console.error(`组装文档${doc.id}数据失败:`, error);
        // 异常兜底
        doc.authorName = '未知';
        doc.folderName = '未分类';
        doc.tags = [];
        doc.title = doc.title || '无标题';
        return doc;
      }
    };
    
    // 加载所有标签
    const loadTags = async () => {
      loading.value = true;
      try {
        const res = await api.getAllTags();
        const rawTags = res.data || [];
        // 格式化标签数据：统一ID为字符串、docCount为数字
        tags.value = rawTags.map(tag => ({
          ...tag,
          id: String(tag.id),
          docCount: Number(tag.docCount) || 0
        }));
        
        // 初始化路由参数
        if (route.query.tagId) {
          const tagId = route.query.tagId;
          currentTagId.value = tagId;
          const targetTag = tags.value.find(t => t.id === tagId);
          if (targetTag) currentTagName.value = targetTag.tagName;
          await loadDocumentsByTag(tagId);
        }
      } catch (error) {
        console.error('加载标签失败', error);
        ElMessage.error('加载标签失败，请稍后重试');
        tags.value = [];
      } finally {
        loading.value = false;
      }
    };
    
    // 加载指定标签下的文档
    const loadDocumentsByTag = async (tagId) => {
      loading.value = true;
      try {
        const res = await api.categoryByTag(tagId);
        const rawDocs = res.data.items ?? res.data ?? [];
        
        // 遍历组装每个文档的关联数据
        const promiseList = rawDocs.map(item => assembleDocumentData({
          id: item.id,
          title: item.title,
          coverImage: item.coverImage,
          tagIds: item.tagIds, // 用于查询标签列表
          authorId: item.authorId, // 用于查询作者名
          editorName:item.editorName,
          folderId: item.folderId, // 用于查询文件夹名
          createTime: item.createTime
        }));
        
        // 等待所有组装完成
        documents.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('加载标签文档失败', error);
        ElMessage.error('加载文档失败，请稍后重试');
        documents.value = [];
      } finally {
        loading.value = false;
      }
    };
    
    // 点击标签
    const handleTagClick = (tagId, tagName) => {
      currentTagId.value = tagId;
      currentTagName.value = tagName;
      loadDocumentsByTag(tagId);
      // 更新路由参数
      router.push({
        name: 'CategoryByTag',
        query: { tagId }
      });
    };

    // 打开创建标签弹窗
    const openCreateTagDialog = () => {
      // 重置表单
      tagForm.value = { tagName: '', remark: '' };
      tagDialogVisible.value = true;
    };

    // 提交创建标签
    const handleCreateTag = async () => {
      try {
        // 表单校验
        await tagFormRef.value.validate();
        // 调用创建接口
        const res = await api.createTag(tagForm.value);
        ElMessage.success('标签创建成功！');
        // 关闭弹窗
        tagDialogVisible.value = false;
        // 重新加载标签列表
        loadTags();
      } catch (error) {
        console.error('创建标签失败', error);
        ElMessage.error('标签创建失败，请重试！');
      }
    };

    // 监听路由参数变化
    watch(() => route.query.tagId, async (newTagId) => {
      if (newTagId) {
        currentTagId.value = newTagId;
        // 查找标签名称
        const targetTag = tags.value.find(t => t.id === newTagId);
        if (targetTag) {
          currentTagName.value = targetTag.tagName;
        } else {
          // 标签不存在时刷新标签列表再查找
          await loadTags();
          const refreshTag = tags.value.find(t => t.id === newTagId);
          if (refreshTag) currentTagName.value = refreshTag.tagName;
        }
        await loadDocumentsByTag(newTagId);
      } else {
        // 清空标签选择状态
        currentTagId.value = '';
        currentTagName.value = '';
        documents.value = [];
      }
    }, { immediate: false });
    
    onMounted(() => {
      loadTags();
    });
    
    return {
      tags,
      documents,
      currentTagId,
      currentTagName,
      loading, 
      formatDate,
      handleTagClick,
      tagDialogVisible,
      tagFormRef,
      tagForm,
      tagRules,
      openCreateTagDialog,
      handleCreateTag
    };
  }
};
</script>