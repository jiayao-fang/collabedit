<template>
  <div class="p-6">
    <!-- 返回按钮 -->
    <div class="mb-6">
      <h2 class="text-xl font-bold mt-2">
        {{ currentFolderId ? `文件夹: ${currentFolderName}` : '所有文件夹' }}
      </h2>
    </div> 
    <!-- 如果没有选择具体文件夹，显示文件夹列表 -->
    <div v-if="!currentFolderId" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div 
        v-for="folder in folders" 
        :key="folder.id"
        class="bg-white rounded-xl shadow-md p-4 hover:shadow-lg transition-shadow cursor-pointer"
        @click="handleFolderClick(folder.id, folder.folderName)"
      >
        <div class="flex items-center ">
          <div class="w-10 h-10 rounded bg-blue-100 flex items-center justify-center text-blue-600 mr-3">
            <i class="el-icon-folder text-xl"></i>
          </div>
          <div>
            <h3 class="font-medium">{{ folder.folderName }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ folder.docCount }} 个文档</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 如果选择了具体文件夹，显示该文件夹下的文档 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <div 
        v-for="doc in documents" 
        :key="doc.id" 
        class="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow relative"
      >
        <!-- 复用文档卡片样式 -->
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
              :key="tag.id||tag" 
              class="text-xs bg-gray-100 px-2 py-0.5 rounded-full"
            >
              {{ tag.tagName||tag||'未知' }}
            </span>
          </div>
          
          <h3 class="font-bold text-lg mb-2 line-clamp-2">{{ doc.title }}</h3>
          <p class="text-gray-600 text-sm mb-3 line-clamp-2"></p>
          
        <div class="flex justify-between items-center text-xs text-gray-500">
            <span>作者: {{ doc.editorName }}</span>
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

    <div class="m-10 text-center" v-if="!currentFolderId">
      <el-button
        type="primary"
        @click="openCreateFolderDialog"
      >
        创建文件夹
      </el-button>
    </div>

     <div class="m-10 text-center"> 
      <el-button 
        type="text" 
        @click="$router.go(-1)"
        class="text-gray-700"
      >
        返回
      </el-button>
    </div>

    <!-- 创建文件夹弹窗 -->
    <el-dialog
      title="创建新文件夹"
      v-model="folderDialogVisible"
      width="400px"
    >
      <el-form
        ref="folderFormRef"
        :model="folderForm"
        :rules="folderRules"
        label-width="90px"
      >
        <el-form-item label="文件夹名称" prop="folderName">
          <el-input v-model="folderForm.folderName" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="folderForm.remark"
            type="textarea"
            rows="2"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from "../api/document";
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from 'element-plus';

export default {
  setup() {
    const folders = ref([]);
    const documents = ref([]);
    const currentFolderId = ref('');
    const currentFolderName = ref('');
    const route = useRoute();
    const router = useRouter();
    const loading = ref(false); 
    const folderDialogVisible = ref(false); // 弹窗显隐
    const folderFormRef = ref(null); // 表单ref
    const folderForm = ref({ // 表单数据
      folderName: ''
    });
    // 表单校验规则
    const folderRules = ref({
      folderName: [
        { required: true, message: '请输入文件夹名称', trigger: 'blur' },
        { min: 1, max: 30, message: '名称长度在 1 到 30 个字符', trigger: 'blur' }
      ]
    });

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      // 兼容时间戳
      const timestamp = typeof dateString === 'number' ? dateString : new Date(dateString).getTime();
      if (Number.isNaN(timestamp)) return dateString;
      const date = new Date(timestamp);
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    };

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
          console.log('docCountRes.data:', docCountRes.data);
          folder.docCount = docCountRes.data || 0; 
          folder.id = String(folder.id); 
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

    // 加载所有文件夹
    const loadFolders = async () => {
      loading.value = true;
      try {
        const res = await api.getAllFolders(); 
        const rawFolders = res.data || [];
        const promiseList = rawFolders.map(folder => assembleFolderData(folder));
        const formattedFolders = await Promise.all(promiseList);
        folders.value = formattedFolders;
      } catch (error) {
        console.error('加载文件夹失败', error);
      } finally {
        loading.value = false;
      }
    };

    const assembleDocumentData = async (doc) => {
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
        if (doc.tagIds && doc.tagIds.trim()) {
          const tagRes = await api.getTags(doc.tagIds);
          doc.tags = tagRes.data || [];
        } else {
          doc.tags = [];
        }
        doc.editorName = doc.editorName || '未知';
        return doc;
      } catch (error) {
        console.error(`组装文档${doc.id}数据失败:`, error);
        doc.authorName = '未知';
        doc.folderName = '未分类';
        doc.tags = [];
        doc.editorName = '未知';
        return doc;
      }
    };

    const loadDocumentsInFolder = async (folderId) => {
      loading.value = true;
      try {
        const res = await api.categoryByFolder(folderId);
        const list = res.data.items ?? res.data ?? [];
        const promiseList = list.map(item => assembleDocumentData({
          id: item.id,
          title: item.title,
          coverImage: item.coverImage,
          tagIds: item.tagIds,
          authorId: item.authorId,
          editorName: item.editorName,
          folderId: item.folderId,
          createTime: item.createTime,
        }));
        documents.value = await Promise.all(promiseList);
      } catch (error) {
        console.error('加载文档失败:', error);
        documents.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 打开创建文件夹弹窗
    const openCreateFolderDialog = () => {
      // 重置表单
      folderForm.value = { folderName: ''};
      folderDialogVisible.value = true;
    };

    // 提交创建文件夹
    const handleCreateFolder = async () => {
      try {
        // 表单校验
        await folderFormRef.value.validate();
        // 调用创建接口
        const res = await api.createFolder(folderForm.value);
        ElMessage.success('文件夹创建成功！');
        // 关闭弹窗
        folderDialogVisible.value = false;
        // 重新加载文件夹列表
        loadFolders();
      } catch (error) {
        console.error('创建文件夹失败', error);
        ElMessage.error('文件夹创建失败，请重试！');
      }
    };

    const handleFolderClick = (folderId, folderName) => {
      currentFolderId.value = folderId;
      currentFolderName.value = folderName;
      loadDocumentsInFolder(folderId);
      router.push({
        name: 'CategoryByFolder',
        query: { folderId }
      });
    };

    onMounted(() => {
      loadFolders();
      // 初始化时读取路由中的folderId
      const folderId = route.query.folderId;
      if (folderId) {
        // 先找文件夹名称，再加载文档
        const targetFolder = folders.value.find(f => f.id === folderId);
        if (targetFolder) {
          currentFolderName.value = targetFolder.folderName;
        }
        currentFolderId.value = folderId;
        loadDocumentsInFolder(folderId);
      }
    });

    // 4. 监听路由参数变化
    watch(() => route.query.folderId, (newFolderId) => {
      if (newFolderId) {
        const targetFolder = folders.value.find(f => f.id === newFolderId);
        if (targetFolder) {
          currentFolderName.value = targetFolder.folderName;
        }
        currentFolderId.value = newFolderId;
        loadDocumentsInFolder(newFolderId);
      } else {
        // 无folderId时重置状态
        currentFolderId.value = '';
        currentFolderName.value = '';
        documents.value = [];
      }
    });

    return {
      folders,
      documents,
      currentFolderId,
      currentFolderName,
      formatDate,
      handleFolderClick,
      loading, 
      folderDialogVisible,
      handleCreateFolder,
      folderFormRef,
      folderForm,
      folderRules,openCreateFolderDialog
    };
  }
};
</script>