<template>
  <div class="p-6 min-h-screen bg-gray-50 w-[600px] mx-auto">
    <!-- 头部 -->
    <div class="flex justify-between items-center mb-6 border-b pb-4">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="goBack"
        class="text-gray-700"
      >
        返回
      </el-button>
      <h2 class="text-xl font-bold">版本历史 - {{ documentTitle }}</h2>
      <div class="flex gap-2">
        <span v-if="isLocked" class="text-sm text-orange-600 bg-orange-50 px-3 py-1 rounded-full flex items-center">
          🔒 已锁定
        </span>
        <el-button 
          type="primary" 
          size="small"
          @click="createSnapshot" 
          :disabled="isLocked"
        >
          创建快照
        </el-button>
        <el-button 
          :type="isLocked ? 'warning' : 'default'"
          size="small"
          @click="toggleLock"
        >
          {{ isLocked ? '解锁' : '锁定' }}
        </el-button>
      </div>
    </div>

    <!-- 版本列表 -->
    <div v-if="versions.length > 0" class="space-y-4">
      <div 
        v-for="(version, index) in versions" 
        :key="version.id"
        class="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow p-4"
        :class="{ 
          'ring-2 ring-blue-500': selectedVersion?.id === version.id,
          'ring-2 ring-green-500': index === 0
        }"
        @click="selectVersion(version)"
      >
        <div class="flex justify-between items-start mb-3">
          <div>
            <div class="flex items-center gap-2">
              <h4 class="font-bold text-blue-600 text-lg">版本 {{ version.versionNumber }}</h4>
              <span v-if="index === 0" class="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">
                当前版本
              </span>
            </div>
            <p class="text-sm text-gray-500 mt-1">{{ formatDate(version.createTime) }}</p>
          </div>
          <div class="flex gap-2">
            <el-button 
              type="text" 
              size="small"
              @click.stop="viewVersion(version)"
              title="查看"
            >
              查看
            </el-button>
            <el-button 
              v-if="index !== 0"
              type="text" 
              size="small"
              @click.stop="rollback(version)" 
              :disabled="isLocked"
              title="回滚"
            >
              回滚
            </el-button>
            <el-button 
              type="text" 
              size="small"
              class="text-red-600"
              @click.stop="deleteVer(version)"
              title="删除"
            >
              删除
            </el-button>
          </div>
        </div>
        
        <div class="space-y-1 text-sm text-gray-600">
          <div v-if="version.changeDescription" class="flex items-start">
            <span class="font-medium mr-2">变更说明：</span>
            <span>{{ version.changeDescription }}</span>
          </div>
          <div class="flex items-center">
            <span class="font-medium mr-2">文件大小：</span>
            <span>{{ formatSize(version.fileSize) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="bg-white rounded-lg shadow-sm p-12 text-center">
      <div class="text-gray-400 mb-4">
        <i class="el-icon-document text-6xl"></i>
      </div>
      <p class="text-gray-500 mb-4">暂无版本历史</p>
      <el-button type="primary" @click="createSnapshot">创建第一个版本快照</el-button>
    </div>

    <!-- 版本预览弹窗 -->
    <el-dialog 
      v-model="showPreview"
      title="版本预览"
      width="70%"
      destroy-on-close
    >
      <div v-if="previewVersion">
        <div class="bg-gray-50 rounded p-4 mb-4">
          <p class="mb-2"><strong>版本号：</strong>{{ previewVersion.versionNumber }}</p>
          <p class="mb-2"><strong>标题：</strong>{{ previewVersion.title }}</p>
          <p class="mb-2"><strong>创建时间：</strong>{{ formatDate(previewVersion.createTime) }}</p>
          <p v-if="previewVersion.changeDescription" class="mb-2">
            <strong>变更说明：</strong>{{ previewVersion.changeDescription }}
          </p>
        </div>
        <div class="border rounded p-4 max-h-96 overflow-auto" v-html="previewVersion.content"></div>
      </div>
    </el-dialog>

    <!-- 创建快照弹窗 -->
    <el-dialog 
      v-model="showCreateModal"
      title="创建版本快照"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="变更说明">
          <el-input 
            v-model="changeDescription" 
            type="textarea"
            placeholder="描述本次变更的内容（可选）..."
            :rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeCreateModal">取消</el-button>
        <el-button type="primary" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import documentApi from '../api/document';
import { ElMessage, ElMessageBox } from 'element-plus';

export default {
  name: 'DocumentVersionHistory',
  data() {
    return {
      docId: null,
      documentTitle: '',
      versions: [],
      selectedVersion: null,
      showPreview: false,
      previewVersion: null,
      showCreateModal: false,
      changeDescription: '',
      isLocked: false
    };
  },
  mounted() {
    this.docId = this.$route.params.id;
    this.loadDocumentInfo();
    this.loadVersionHistory();
    this.checkLockStatus();
  },
  methods: {
    async loadDocumentInfo() {
      try {
        const res = await documentApi.get(this.docId);
        this.documentTitle = res.data.title || '未命名文档';
      } catch (error) {
        console.error('加载文档信息失败:', error);
      }
    },
    async loadVersionHistory() {
      try {
        const res = await documentApi.getVersionHistory(this.docId);
        if (res.data.code === 200) {
          this.versions = res.data.data;
        }
      } catch (error) {
        console.error('加载版本历史失败:', error);
        ElMessage.error('加载版本历史失败');
      }
    },
    async checkLockStatus() {
      try {
        const res = await documentApi.getDocumentLockStatus(this.docId);
        if (res.data.code === 200) {
          this.isLocked = res.data.data;
        }
      } catch (error) {
        console.error('检查锁定状态失败:', error);
      }
    },
    async toggleLock() {
      try {
        if (this.isLocked) {
          const res = await documentApi.unlockDocument(this.docId);
          if (res.data.code === 200) {
            this.isLocked = false;
            ElMessage.success('文档已解锁');
          }
        } else {
          const res = await documentApi.lockDocument(this.docId);
          if (res.data.code === 200) {
            this.isLocked = true;
            ElMessage.success('文档已锁定');
          }
        }
      } catch (error) {
        console.error('切换锁定状态失败:', error);
        ElMessage.error(error.response?.data?.message || '操作失败');
      }
    },
    selectVersion(version) {
      this.selectedVersion = version;
    },
    async viewVersion(version) {
      try {
        const res = await documentApi.getVersionById(version.id);
        if (res.data.code === 200) {
          this.previewVersion = res.data.data;
          this.showPreview = true;
        }
      } catch (error) {
        console.error('查看版本失败:', error);
        ElMessage.error('查看版本失败');
      }
    },
    async rollback(version) {
      if (this.isLocked) {
        ElMessage.warning('文档已锁定，无法回滚');
        return;
      }
      
      try {
        await ElMessageBox.confirm(
          `确定要恢复到版本 ${version.versionNumber} 吗？`,
          '确认恢复',
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        const res = await documentApi.rollbackToVersion(this.docId, version.id);
        if (res.data.code === 200) {
          ElMessage.success('版本恢复成功');
          // 重新加载版本历史
          await this.loadVersionHistory();
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('回滚失败:', error);
          ElMessage.error(error.response?.data?.message || '回滚失败');
        }
      }
    },
    async deleteVer(version) {
      try {
        await ElMessageBox.confirm(
          `确定要删除版本 ${version.versionNumber} 吗？`,
          '确认删除',
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        const res = await documentApi.deleteVersion(version.id);
        if (res.data.code === 200) {
          ElMessage.success('删除成功');
          this.loadVersionHistory();
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error);
          ElMessage.error(error.response?.data?.message || '删除失败');
        }
      }
    },
    createSnapshot() {
      if (this.isLocked) {
        ElMessage.warning('文档已锁定，无法创建快照');
        return;
      }
      this.showCreateModal = true;
    },
    async confirmCreate() {
      try {
        const res = await documentApi.createVersion(this.docId, {
          changeDescription: this.changeDescription || '手动创建快照'
        });
        if (res.data.code === 200) {
          ElMessage.success('版本快照创建成功');
          this.closeCreateModal();
          this.loadVersionHistory();
        }
      } catch (error) {
        console.error('创建快照失败:', error);
        ElMessage.error('创建快照失败');
      }
    },
    closePreview() {
      this.showPreview = false;
      this.previewVersion = null;
    },
    closeCreateModal() {
      this.showCreateModal = false;
      this.changeDescription = '';
    },
    goBack() {
      this.$router.back();
    },
    formatDate(dateStr) {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    formatSize(bytes) {
      if (!bytes) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
  }
};
</script>

<style scoped>
/* 使用与现有页面一致的简洁风格 */
</style>

