<template>
  <div class="p-6">
    <div class="border-b pb-4 mb-6">
      <h2 class="text-xl font-bold mt-2">发布文档</h2>
    </div>

    <el-form 
      ref="docFormRef" 
      :model="docForm" 
      label-width="80px"
      class="max-w-3xl mx-auto"
      :disabled="loading"
    >
      <!-- 文档标题 -->
      <el-form-item label="标题" prop="title">
        <el-input 
          v-model="docForm.title" 
          placeholder="请输入文档标题（必填）" 
          class="text-lg"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <!-- 封面图上传 -->
      <el-form-item label="封面图">
        <el-upload
          class="avatar-uploader"
          action=""
          :show-file-list="false"
          :http-request="uploadCover"
          :before-upload="beforeUpload"
          :disabled="loading"
        >
          <img 
            v-if="docForm.coverImage" 
            :src="docForm.coverImage" 
            class="w-48 h-32 object-cover rounded border"
          >
          <div v-else class="w-48 h-32 bg-gray-100 flex items-center justify-center rounded border border-dashed">
            <i class="el-icon-plus text-xl text-gray-400"></i>
            <span class="ml-2 text-xs text-gray-400">点击上传封面</span>
          </div>
        </el-upload>
        <div class="text-xs text-gray-500 mt-1">支持JPG/PNG格式，大小不超过2MB</div>
      </el-form-item>

      <el-form-item label="权限">
        <el-select 
          v-model="docForm.visibility" 
          placeholder="设置权限"
          clearable  
        >
          <el-option label="仅自己可见" value="0"></el-option>
          <el-option label="仅编辑者可见" value="1"></el-option>
          <el-option label="所有人可见" value="2"></el-option>
      </el-select>
      </el-form-item>

      <!-- 文件夹选择 -->
      <el-form-item label="文件夹">
        <el-select 
          v-model="docForm.folderId" 
          placeholder="选择文件夹（可选）"
          clearable  
          @change="handleFolderChange"
        >
          <el-option label="未分类" value=""></el-option>
          <el-option 
            v-for="folder in folders" 
            :key="folder.id" 
            :label="folder.folderName || folder.name"  
            :value="folder.id"
          ></el-option>
        </el-select>
      </el-form-item>

      <!-- 标签选择 -->
      <el-form-item label="标签">
        <el-select 
          v-model="docForm.tagIds" 
          placeholder="选择标签（可多选）" 
          multiple
          clearable    
          @change="handleTagChange"
        >
          <el-option 
            v-for="tag in tags" 
            :key="tag.id" 
            :label="tag.name || tag.tagName || '未命名标签'" 
            :value="tag.id"
          ></el-option>
        </el-select>
        <div class="text-xs text-gray-500 mt-1">可选择多个标签，点击已选标签可取消</div>
      </el-form-item>

      <!-- 文档内容 -->
    <el-form-item label="内容" prop="content">
      <!-- 1. 父容器强制固定高度 + 溢出自动滚动 -->
      <div class="quill-wrapper" style="height: 400px; border: 1px solid #dcdfe6; border-radius: 4px; overflow: hidden;">
        <QuillEditor
        v-model:content="docForm.content"
        contentType="html"
        class="w-full h-full"
        :options="editorOptions"
        :style="{ height: '100%' }"
      />
      </div>
    </el-form-item>


      <!-- 提交按钮 -->
      <el-form-item>
        <el-button 
          type="primary" 
          @click="submitForm"
          :loading="loading"
        >
          发布文档
        </el-button>
        <el-button 
          type="default" 
          @click="resetForm"
        >
          重置
        </el-button>
        <el-button 
          type="text" 
          @click="$router.go(-1)"
          icon="el-icon-arrow-left"
        >
          返回
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import api from "../api/document";
import { onMounted, ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage} from "element-plus";

export default {
  components: { QuillEditor },
  setup() {
    // 表单实例
    const docFormRef = ref(null);
    // 加载状态
    const loading = ref(false);
    // 路由实例
    const router = useRouter();

    // 表单数据（优化默认值，统一字段类型）
    const docForm = reactive({
      title: '',
      content: '<p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p>',
      coverImage: '',
      folderId: '',  // 字符串类型，兼容空值
      tagIds: [] ,    // 数组类型，存储选中的标签ID
      visibility:'0'
    });

    // 文件夹/标签列表
    const folders = ref([]);
    const tags = ref([]);

    // 富文本编辑器配置
    const editorOptions = {
      placeholder: '请输入文档内容（必填）',
      modules: {
        toolbar: [
          ['bold', 'italic', 'underline', 'strike'],
          ['blockquote', 'code-block'],
          [{ 'header': 1 }, { 'header': 2 }],
          [{ 'list': 'ordered' }, { 'list': 'bullet' }],
          [{ 'align': [] }],
          ['link', 'image'],
          ['clean']
        ]
      },
      theme: 'snow',
    bounds: document.body,
    style: {
      height: '100%'
    }
  };

    // 加载当前用户的文件夹 + 所有标签
    const loadCategories = async () => {
      loading.value = true;
      try {
        const [foldersRes, tagsRes] = await Promise.all([
          api.getFolders(),      
          api.getAllTags(),     

        ]);
        
        // 格式化文件夹数据
        folders.value = (foldersRes.data || []).map(folder => ({
          id:folder.id,
          folderName:folder.folderName || '未命名文件夹'
          }
        ));
        
        // 格式化标签数据
        tags.value = (tagsRes.data || []).map(tag => ({
          id: tag.id || tag.tagId || '',
          name: tag.tagName || tag.name || '未命名标签'
        }));

        if (folders.value.length === 0) {
          ElMessage.info('暂无已创建的文件夹，默认选择「未分类」');
        }
      } catch (error) {
        ElMessage.error('加载分类数据失败：' + (error.message || '服务器错误'));
        console.error('加载分类失败', error);
      } finally {
        loading.value = false;
      }
    };

    // 封面图上传前校验
    const beforeUpload = (file) => {
      const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg';
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isImage) {
        ElMessage.error('封面图仅支持 JPG/PNG 格式！');
        return false;
      }
      if (!isLt2M) {
        ElMessage.error('封面图大小不能超过 2MB！');
        return false;
      }
      return true;
    };

    // 自定义封面上传
    const uploadCover = async (options) => {
      loading.value = true;
      try {
        const formData = new FormData();
        formData.append('file', options.file);
        const res = await api.uploadCover(formData);
        
        if (res.data && res.data.url) {
          docForm.coverImage = res.data.url;
          ElMessage.success('封面上传成功');
          options.onSuccess();
        } else {
          throw new Error('上传结果异常');
        }
      } catch (error) {
        ElMessage.error('封面上传失败：' + (error.message || '请重试'));
        options.onError();
      } finally {
        loading.value = false;
      }
    };

    // 文件夹选择变更
      const handleFolderChange = (val) => {
      console.log('选中文件夹：', val || '未分类');
      // 空值处理
      docForm.folderId = val || '';
    };

    // 标签选择变更
    const handleTagChange = (val) => {
      console.log('选中标签ID：', val);
      // 确保tagIds为数组类型
      docForm.tagIds = val || [];
    };

    // 重置表单
    const resetForm = () => {
      if (docFormRef.value) {
        docFormRef.value.resetFields();
      }
      // 手动重置自定义字段
      docForm.title = '';
      docForm.content = '';
      docForm.coverImage = '';
      docForm.folderId = '';
      docForm.tagIds = [];
      docForm.visibility = '0'; 
      ElMessage.info('表单已重置');
    };

    // 提交表单
    const submitForm = async () => {
      // 1. 前端基础校验
      if (!docForm.title.trim()) {
        ElMessage.warning('请输入文档标题！');
        return;
      }
      if (!docForm.content.trim()) {
        ElMessage.warning('请输入文档内容！');
        return;
      }
      if (!docForm.visibility) {
        ElMessage.warning('请选择文档可见范围！');
        return;
      }

      loading.value = true;
      try {
        // 2. 格式化提交数据
        const submitData = {
          ...docForm,
          tagIds: docForm.tagIds.length > 0 ? docForm.tagIds.join(',') : '',
          folderId: docForm.folderId || null,
          visibility: Number(docForm.visibility)
        };

        // 3. 调用发布接口
        await api.create(submitData);
        
        ElMessage.success('文档发布成功！');
        // 4. 跳转至文档列表页
        setTimeout(() => {
          router.push('/main/all');
        }, 1000);

      } catch (error) {
        const errMsg = error.response?.data?.message || error.message || '发布失败';
        ElMessage.error(`文档发布失败：${errMsg}`);
        console.error('发布失败', error);
      } finally {
        loading.value = false;
      }
    };

    // 初始化加载分类数据
    onMounted(() => {
      loadCategories();
    });

    return {
      docFormRef,
      docForm,
      folders,
      tags,
      loading,
      editorOptions,
      beforeUpload,
      uploadCover,
      handleFolderChange,
      handleTagChange,
      resetForm,
      submitForm
    };
  }
};
</script>

<style scoped>
.avatar-uploader {
  display: inline-block;
  cursor: pointer;
}

/* 富文本编辑器样式优化 */
:deep(.ql-container) {
  height: calc(100% - 42px) !important; /* 减去工具栏高度，确保占满父容器 */
  min-height: calc(100% - 42px) !important;
}
:deep(.ql-editor) {
  min-height: 100% !important;
  height: 100% !important;
  line-height: 1.5; 
}
:deep(.ql-toolbar) {
  border-bottom: 1px solid #dcdfe6 !important;
}
/* 可见性选择器样式优化 */
:deep(.el-select) {
  width: 100%;
}
</style>
