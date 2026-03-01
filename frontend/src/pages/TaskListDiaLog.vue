<template>
  <el-dialog
    width="400px"
    destroy-on-close
    title="文档任务列表"
    v-model="visible"
    @close="handleClose"
  >
    <div v-if="loading" class="text-center py-8">
      <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
      <p class="text-gray-500 mt-2">加载中...</p>
    </div>
    
    <div v-else-if="tasks.length === 0" class="text-center py-12">
      <el-empty description="暂无任务"></el-empty>
    </div>
    
    <div v-else class="space-y-4 max-h-[600px] overflow-auto pr-2">
      <div 
        v-for="task in tasks" 
        :key="task.id"
        class="border rounded-lg p-4 hover:shadow-md transition-shadow bg-white"
      >
        <!-- 任务头部 -->
        <div class="flex justify-between items-start mb-3">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <h4 class="font-bold text-lg text-gray-800">{{ task.title }}</h4>
              <span :class="['text-xs px-3 py-1 rounded-full', getOverallStatusClass(task.overallStatus)]">
                {{ getOverallStatusText(task.overallStatus) }}
              </span>
              <span :class="['text-xs px-3 py-1 rounded-full', getDeadlineStatusClass(task.deadline)]">
                {{ getDeadlineStatusText(task.deadline) }}
              </span>
            </div>
            <p class="text-sm text-gray-600">{{ task.content || '暂无描述' }}</p>
          </div>
        </div>

        <!-- 任务进度 -->
        <div class="mb-3">
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600">完成进度</span>
            <span class="font-medium text-blue-600">{{ task.progress }}</span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-2">
            <div 
              class="bg-blue-600 h-2 rounded-full transition-all"
              :style="{ width: getProgressPercent(task.progress) + '%' }"
            ></div>
          </div>
        </div>

        <!-- 任务信息 -->
        <div class="grid grid-cols-2 gap-2 text-sm text-gray-600 mb-3">
          <div><span class="font-medium">截止日期：</span>{{ formatDate(task.deadline) }}</div>
          <div><span class="font-medium">创建时间：</span>{{ formatDate(task.createTime) }}</div>
        </div>

        <!-- 被分配人列表 -->
        <div class="border-t pt-3">
          <div class="text-sm font-medium text-gray-700 mb-2">被分配人：</div>
          <div class="space-y-2">
            <div 
              v-for="assignee in task.assignees" 
              :key="assignee.id"
              class="flex items-center justify-between p-2 bg-gray-50 rounded hover:bg-gray-100"
            >
              <div class="flex items-center gap-2 flex-1">
                <el-avatar size="small" :src="assignee.avatar || defaultAvatar">
                  {{ getUserInitial(assignee.assigneeId) }}
                </el-avatar>
                <span class="text-sm">{{ getUserName(assignee.assigneeId) }}</span>
              </div>
              
              <div class="flex items-center gap-3">
                <span v-if="assignee.completeTime" class="text-xs text-gray-500">
                  {{ formatDate(assignee.completeTime) }}
                </span>
                <el-select
                  v-model="assignee.status"
                  size="small"
                  @change="handleStatusChange(task, assignee)"
                  :disabled="!canUpdateStatus(task, assignee)"
                  class="w-32"
                >
                  <el-option label="待处理" value="PENDING"></el-option>
                  <el-option label="进行中" value="IN_PROGRESS"></el-option>
                  <el-option label="已完成" value="COMPLETED"></el-option>
                </el-select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import taskApi from '../api/task';
import userApi from '../api/user';

export default {
  name: 'TaskListDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    docId: {
      type: [Number, String],
      required: true
    },
    currentUserId: {
      type: [Number, String],
      required: true
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    });

    const loading = ref(false);
    const tasks = ref([]);
    const userCache = ref({}); // 用户信息缓存
    const defaultAvatar = 'https://picsum.photos/200/200?random=avatar';

    // 加载任务列表
    const loadTasks = async () => {
      if (!props.docId) return;
      
      loading.value = true;
      try {
        const res = await taskApi.getTasksByDocId(props.docId);
        tasks.value = res.data || [];
        
        // 预加载所有相关用户信息
        await loadUsersInfo();
      } catch (error) {
        console.error('加载任务失败:', error);
        ElMessage.error('加载任务失败');
        tasks.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 加载用户信息
    const loadUsersInfo = async () => {
      const userIds = new Set();
      
      tasks.value.forEach(task => {
        if (task.creatorId) userIds.add(task.creatorId);
        if (task.assignees) {
          task.assignees.forEach(assignee => {
            if (assignee.assigneeId) userIds.add(assignee.assigneeId);
          });
        }
      });

      const userIdArray = Array.from(userIds);
      if (userIdArray.length === 0) return;

      try {
        const res = await userApi.getUsersByIds(userIdArray);
        const users = res.data || [];
        
        users.forEach(user => {
          userCache.value[user.id] = {
            username: user.username,
            avatar: user.avatar
          };
        });
      } catch (error) {
        console.error('加载用户信息失败:', error);
      }
    };

    // 获取用户名
    const getUserName = (userId) => {
      return userCache.value[userId]?.username || `用户${userId}`;
    };

    // 获取用户首字母
    const getUserInitial = (userId) => {
      const username = getUserName(userId);
      return username.charAt(0);
    };

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    // 获取整体状态样式
    const getOverallStatusClass = (status) => {
      switch (status) {
        case 'ALL_COMPLETED':
          return 'bg-green-100 text-green-700';
        case 'PARTIAL_COMPLETED':
          return 'bg-blue-100 text-blue-700';
        case 'IN_PROGRESS':
          return 'bg-yellow-100 text-yellow-700';
        case 'PENDING':
          return 'bg-gray-100 text-gray-700';
        default:
          return 'bg-gray-100 text-gray-700';
      }
    };

    // 获取整体状态文本
    const getOverallStatusText = (status) => {
      switch (status) {
        case 'ALL_COMPLETED':
          return '全部完成';
        case 'PARTIAL_COMPLETED':
          return '部分完成';
        case 'IN_PROGRESS':
          return '进行中';
        case 'PENDING':
          return '待处理';
        default:
          return '未知';
      }
    };

    // 获取截止日期状态样式
    const getDeadlineStatusClass = (deadline) => {
      if (!deadline) return 'bg-gray-100 text-gray-700';
      
      const now = new Date();
      const deadlineDate = new Date(deadline);
      
      if (deadlineDate < now) {
        return 'bg-red-100 text-red-700';
      } else if (deadlineDate - now < 24 * 60 * 60 * 1000) {
        return 'bg-orange-100 text-orange-700';
      } else {
        return 'bg-green-100 text-green-700';
      }
    };

    // 获取截止日期状态文本
    const getDeadlineStatusText = (deadline) => {
      if (!deadline) return '无截止日期';
      
      const now = new Date();
      const deadlineDate = new Date(deadline);
      
      if (deadlineDate < now) {
        return '已过期';
      } else if (deadlineDate - now < 24 * 60 * 60 * 1000) {
        return '即将到期';
      } else {
        return '未过期';
      }
    };

    // 计算进度百分比
    const getProgressPercent = (progress) => {
      if (!progress) return 0;
      const [completed, total] = progress.split('/').map(Number);
      if (total === 0) return 0;
      return Math.round((completed / total) * 100);
    };

    // 判断是否可以更新状态
    const canUpdateStatus = (task, assignee) => {
      // 任务创建人可以更新任何人的状态
      if (task.creatorId === props.currentUserId) {
        return true;
      }
      // 被分配人只能更新自己的状态
      if (assignee.assigneeId === props.currentUserId) {
        return true;
      }
      return false;
    };

    // 处理状态变更
    const handleStatusChange = async (task, assignee) => {
      try {
        // 如果是创建者更新其他人的状态，需要传递 assigneeId
        const isCreator = task.creatorId === props.currentUserId;
        const targetAssigneeId = isCreator && assignee.assigneeId !== props.currentUserId 
          ? assignee.assigneeId 
          : null;
        
        await taskApi.updateTaskStatus(task.id, assignee.status, targetAssigneeId);
        ElMessage.success('任务状态更新成功');
        
        // 重新加载任务列表以获取最新的进度和整体状态
        await loadTasks();
      } catch (error) {
        console.error('更新任务状态失败:', error);
        ElMessage.error(error.response?.data?.message || '更新任务状态失败');
        
        // 恢复原状态
        await loadTasks();
      }
    };

    // 关闭弹窗
    const handleClose = () => {
      emit('update:modelValue', false);
    };

    // 监听弹窗打开
    watch(visible, (newVal) => {
      if (newVal) {
        loadTasks();
      }
    });

    return {
      visible,
      loading,
      tasks,
      defaultAvatar,
      getUserName,
      getUserInitial,
      formatDate,
      getOverallStatusClass,
      getOverallStatusText,
      getDeadlineStatusClass,
      getDeadlineStatusText,
      getProgressPercent,
      canUpdateStatus,
      handleStatusChange,
      handleClose
    };
  }
};
</script>

<style scoped>
.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>

