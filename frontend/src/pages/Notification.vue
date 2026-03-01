<template>
  <div class="p-6 min-h-screen bg-gray-50 w-[600px] mx-auto">
    <!-- 头部：返回按钮 + 标题 -->
    <div class="flex justify-between items-center border-b pb-4 mb-6">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="$router.go(-1)"
        class="text-gray-700"
      >
        返回
      </el-button>
      
      <h2 class="text-xl font-bold">通知中心</h2>
      
      <el-button 
        type="text" 
        size="small" 
        @click="batchMarkNotificationRead"
        :disabled="unreadCount === 0"
      >
        全部标为已读
      </el-button>
    </div>

    <!-- 通知统计 -->
    <div class="mb-6 bg-white rounded-lg p-4 shadow-sm">
      <div class="flex gap-4">
        <div class="flex-1 text-center p-2 border rounded">
          <div class="text-sm text-gray-500">全部通知</div>
          <div class="text-xl font-bold mt-1">{{ totalCount }}</div>
        </div>
        <div class="flex-1 text-center p-2 border rounded">
          <div class="text-sm text-gray-500">未读通知</div>
          <div class="text-xl font-bold mt-1 text-primary">{{ unreadCount }}</div>
        </div>
        <div class="flex-1 text-center p-2 border rounded">
          <div class="text-sm text-gray-500">已读通知</div>
          <div class="text-xl font-bold mt-1">{{ totalCount - unreadCount }}</div>
        </div>
      </div>
    </div>

    <!-- 通知筛选 -->
    <div class="mb-6 bg-white rounded-lg p-4 shadow-sm flex gap-4">
      <el-select 
        v-model="notificationType" 
        placeholder="筛选通知类型" 
        clearable
        @change="getNotificationList"
      >
        <el-option label="全部通知" value=""></el-option>
        <el-option label="新评论" value="COMMENT"></el-option>
        <el-option label="评论回复" value="REPLY"></el-option>
        <el-option label="@提及" value="MENTION"></el-option>
        <el-option label="联系人请求" value="CONTACT_REQUEST"></el-option>
        <el-option label="联系人同意" value="CONTACT_APPROVED"></el-option>
        <el-option label="联系人删除" value="CONTACT_DELETED"></el-option>
        <el-option label="编辑邀请" value="EDIT_INVITATION"></el-option>
        <el-option label="任务分配" value="TASK_ASSIGNMENT"></el-option>
        <el-option label="任务状态变更" value="TASK_STATUS_CHANGE"></el-option>
        <el-option label="任务截止提醒" value="TASK_DEADLINE_APPROACHING"></el-option>
      </el-select>
      
      <el-select 
        v-model="readStatus" 
        placeholder="筛选阅读状态" 
        clearable
        @change="getNotificationList"
      >
        <el-option label="全部" value=""></el-option>
        <el-option label="未读" value="0"></el-option>
        <el-option label="已读" value="1"></el-option>
      </el-select>
    </div>

    <!-- 通知列表 -->
    <div class="bg-white rounded-lg shadow-sm">
      <!-- 空列表提示 -->
      <el-empty 
        v-if="notificationList.length === 0 && !loading"
        description="暂无通知数据"
        class="py-10"
      ></el-empty>

      <!-- 列表加载中 -->
      <div v-if="loading" class="text-center py-10">
        <el-icon size="24" color="#409eff"><loading></loading></el-icon>
        <p class="mt-2 text-gray-500">加载中...</p>
      </div>

      <!-- 通知列表 -->
     <div v-else class="h-[600px] overflow-auto divide-y">
  <el-card
    v-for="item in notificationList"
    :key="item.id"
    shadow="never"
    class="cursor-pointer"
    :class="{ 'bg-gray-50': item.isRead === 0 }"
    @click="markNotificationRead(item.id)"
  >
    <div class="flex items-start gap-4">
      <!-- 头像 -->
      <el-avatar :src="defaultAvatar">
        {{ getNotificationAvatar(item.type) }}
      </el-avatar>

      <!-- 内容 -->
      <div class="flex-1">
        <div class="flex justify-between items-center">
          <span class="font-medium">{{ item.content }}</span>
          <el-tag v-if="item.isRead === 0" size="small" type="primary">
            未读
          </el-tag>
        </div>

        <div class="flex justify-between text-sm text-gray-500 mt-1">
          <!-- 联系人请求特殊处理 -->
          <div v-if="item.type === 'CONTACT_REQUEST'" class="flex items-center gap-2">
            <span v-if="item.status === 1" class="text-green-600">已接受</span>
            <span v-else-if="item.status === 2" class="text-red-600">已拒绝</span>
            <template v-else>
              <el-button 
                type="primary" 
                size="small" 
                @click.stop="handleContactRequest(item.relatedId, 1)"
              >
                接受
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click.stop="handleContactRequest(item.relatedId, 2)"
              >
                拒绝
              </el-button>
            </template>
          </div>
          
          <!-- 其他通知类型 -->
          <div v-else class="flex flex-col">
            <div>
              <span>关联文档：
                <el-button 
                  v-if="item.docId" 
                  type="text" 
                  size="small" 
                  class="text-blue-500 hover:text-blue-700 p-0"
                  @click.stop="goToDocument(item.docId)"
                >
                  {{ item.docTitle || '未知文档' }}
                </el-button>
                <span v-else>{{ item.docTitle || '未知文档' }}</span>
              </span>
            </div>
            <!-- 任务通知显示截止时间 -->
            <div v-if="isTaskNotification(item.type) && item.taskDeadline" class="text-xs text-orange-600 mt-1">
              截止时间：{{ formatDate(item.taskDeadline) }}
            </div>
          </div>
          
          <span>{{ formatDate(item.createTime) }}</span>
        </div>
      </div>

      <!-- 删除 -->
      <el-button
        type="text"
        size="small"
        @click.stop="deleteNotification(item.id)"
      >
        删除
      </el-button>
    </div>
  </el-card>
</div>

      
      <!-- 分页（仅当有数据时显示） -->
      <el-pagination
        v-if="totalCount > 0"
        @size-change="handleNotificationSizeChange"
        @current-change="handleNotificationCurrentChange"
        :current-page="pageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalCount"
        class="mt-4 p-4"
      >
      </el-pagination>
    </div>
  </div>
</template>

<script>
import api from "../api/document";
import contactApi from "../api/contact"; 
import taskApi from "../api/task"; 
import { onMounted, ref, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Loading } from '@element-plus/icons-vue'

export default {
  components: {
    Loading // 注册图标组件
  },
  setup() {
    const router = useRouter();
    const loading = ref(false); // 加载状态
    const defaultAvatar = 'https://picsum.photos/200/200?random=avatar';
    
    // 通知筛选条件
    const notificationType = ref(""); 
    const readStatus = ref(""); 
    
    // 分页参数
    const pageNum = ref(1);
    const pageSize = ref(10);
    const totalCount = ref(0);
    const unreadCount = ref(0);
    
    // 通知列表
    const notificationList = ref([]);
    let notificationTimer = null;

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return Number.isNaN(date.getTime()) ? dateString : date.toLocaleString();
    };

    // 获取通知头像文字
    const getNotificationAvatar = (type) => {
      switch(type) {
        case 'COMMENT': return '评';
        case 'REPLY': return '回';
        case 'MENTION': return '@';
        case 'CONTACT_REQUEST': return '请';
        case 'CONTACT_APPROVED': return '同';
        case 'CONTACT_DELETED': return '删';
        case 'EDIT_INVITATION': return '编';
        case 'TASK_ASSIGNMENT': return '任';
        case 'TASK_STATUS_CHANGE': return '更';
        case 'TASK_DEADLINE_APPROACHING': return '急';
        default: return '通';
      }
    };
    
    // 判断是否为任务相关通知
    const isTaskNotification = (type) => {
      return type === 'TASK_ASSIGNMENT' || 
             type === 'TASK_STATUS_CHANGE' || 
             type === 'TASK_DEADLINE_APPROACHING';
    };
    
    // 获取任务信息
    const getTaskInfo = async (taskId) => {
      if (!taskId) return null;
      try {
        const res = await taskApi.getTaskById(taskId);
        return res.data;
      } catch (error) {
        console.error('获取任务信息失败:', error);
        return null;
      }
    };

    // 获取文档标题
    const getDocumentTitle = async (docId) => {
      if (!docId) return '未知文档';
      try {
        const res = await api.get(docId);
        return res.data?.title || '未知文档';
      } catch (error) {
        console.error('获取文档信息失败:', error);
        return '未知文档';
      }
    };

    // 获取通知列表并填充文档标题
    const getNotificationList = async () => {
      try {
        loading.value = true;
        const params = {
          pageNum: pageNum.value,
          pageSize: pageSize.value,
          type: notificationType.value || undefined,
          isRead: readStatus.value || undefined
        };
        console.log("请求通知列表参数：", params); 
        const res = await api.getNotifications(params);
        console.log("通知列表接口返回：", res); 
        

        let notifications = [];
        if (res.data && res.data.records) {
          notifications = res.data.records;
          totalCount.value = res.data.total;
        } else if (Array.isArray(res.data)) {
          notifications = res.data;
          totalCount.value = res.data.length;
        } else {
          notifications = [];
          totalCount.value = 0;
        }

        // 为每个通知获取文档标题和任务信息
        const notificationsWithTitles = await Promise.all(notifications.map(async (item) => {
          if (item.docId) {
            item.docTitle = await getDocumentTitle(item.docId);
          } else {
            item.docTitle = '未知文档';
          }
          
          // 如果是任务相关通知，获取任务信息
          if (isTaskNotification(item.type) && item.relatedId) {
            const taskInfo = await getTaskInfo(item.relatedId);
            if (taskInfo && taskInfo.deadline) {
              item.taskDeadline = taskInfo.deadline;
            }
          }
          
          return item;
        }));

        notificationList.value = notificationsWithTitles;
      } catch (error) {
        console.error("加载通知列表失败：", error);
        ElMessage.error('加载通知列表失败：' + (error.message || '服务器错误'));
        notificationList.value = [];
        totalCount.value = 0;
      } finally {
        loading.value = false;
      }
    };

       // 同步未读数（
    const syncUnreadCount = (newVal) => {
      const oldVal = Number(localStorage.getItem("notificationUnreadCount") || 0);

      //只有变化才派发事件
      if (newVal !== oldVal) {
        localStorage.setItem("notificationUnreadCount", newVal.toString());
        window.dispatchEvent(new Event("notificationUpdated"));
      }
    };


    // 获取未读通知数量
    const getUnreadNotificationCount = async () => {
      try {
        const res = await api.getUnreadNotificationCount();
        unreadCount.value = res.data || 0;
        syncUnreadCount(unreadCount.value);
      } catch (error) {
        console.error('获取未读通知数量失败', error);
      }
    };

    // 标记单条通知已读
    const markNotificationRead = async (notificationId) => {
      try {
        await api.markNotificationAsRead(notificationId);
        const index = notificationList.value.findIndex(item => item.id === notificationId);
        if (index !== -1) {
          notificationList.value[index].isRead = 1;
        }
        await getUnreadNotificationCount();
      } catch (error) {
        ElMessage.error('标记已读失败：' + (error.message || '服务器错误'));
      }
    };

    // 处理联系人请求（接受/拒绝）
    const handleContactRequest = async (requestId, status) => {
      try {
        await contactApi.handleContactRequest(requestId, status);
        const action = status === 1 ? '接受' : '拒绝';
        ElMessage.success(`${action}联系人请求成功`);
        
        // 重新加载通知列表
        await getNotificationList();
        await getUnreadNotificationCount();
      } catch (error) {
        const action = status === 1 ? '接受' : '拒绝';
        ElMessage.error(`${action}联系人请求失败：` + (error.message || '服务器错误'));
      }
    };

    // 批量标记通知已读
    const batchMarkNotificationRead = async () => {
      try {
        if (unreadCount.value === 0) {
          ElMessage.info('暂无未读通知');
          return;
        }
        
        await ElMessageBox.confirm(
          '确定要将所有未读通知标记为已读吗？',
          '确认操作',
          { type: 'info' }
        );
        
        const unreadIds = notificationList.value
          .filter(item => item.isRead === 0)
          .map(item => item.id);
        
        if (unreadIds.length > 0) {
          await api.batchMarkNotificationAsRead(unreadIds);
          notificationList.value.forEach(item => {
            if (item.isRead === 0) item.isRead = 1;
          });
          unreadCount.value = 0;
          syncUnreadCount(0);
          ElMessage.success('全部标记为已读');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量标记失败：' + (error.message || '服务器错误'));
        }
      }
    };

    // 删除通知
    const deleteNotification = async (notificationId) => {
      try {
        await ElMessageBox.confirm(
          '确定要删除该通知吗？',
          '删除确认',
          { type: 'warning' }
        );
        
        await api.deleteNotification(notificationId);
        notificationList.value = notificationList.value.filter(item => item.id !== notificationId);
        await getUnreadNotificationCount();
        totalCount.value -= 1;
        unreadCount.value = Math.max(0, unreadCount.value - 1); // 同步未读数量
        ElMessage.success('通知已删除');
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败：' + (error.message || '服务器错误'));
        }
      }
    };

     // 跳转到文档详情页
    const goToDocument = (docId) => {
      if (docId) {
        router.push(`/documents/${docId}`);
      }
    };

    // 处理通知点击事件
    const handleNotificationClick = (notification) => {
      // 如果是未读通知，先标记为已读
      if (notification.isRead === 0) {
        markNotificationRead(notification.id);
      }
      
      // 如果有文档ID，则跳转到文档详情页
      if (notification.docId) {
        goToDocument(notification.docId);
      }
    };

    // 分页事件处理
    const handleNotificationSizeChange = (val) => {
      pageSize.value = val;
      getNotificationList();
    };

    const handleNotificationCurrentChange = (val) => {
      pageNum.value = val;
      getNotificationList();
    };

     // 监听通知更新事件
    const handleNotificationUpdate = () => {
      getUnreadNotificationCount();
      // 如果当前在第一页，也更新通知列表
      if (pageNum.value === 1) {
        getNotificationList();
      }
    };

    // 初始化
    onMounted(() => {
      getNotificationList();
      getUnreadNotificationCount();
      notificationTimer = setInterval(() => {
        getUnreadNotificationCount();
      }, 30000);

      window.addEventListener('notificationUpdated', handleNotificationUpdate);
    });

    // 销毁
    onUnmounted(() => {
      if (notificationTimer) clearInterval(notificationTimer);
      window.removeEventListener('notificationUpdated', handleNotificationUpdate);
    });

    return {
      defaultAvatar,
      notificationType,
      readStatus,
      pageNum,
      pageSize,
      totalCount,
      unreadCount,
      notificationList,
      loading, // 暴露加载状态
      formatDate,
      getNotificationAvatar, // 暴露头像文字函数
      isTaskNotification, // 暴露任务通知判断函数
      getNotificationList,
      markNotificationRead,
      handleContactRequest, // 暴露联系人请求处理函数
      batchMarkNotificationRead,
      deleteNotification,
      goToDocument,
      handleNotificationClick,
      handleNotificationSizeChange,
      handleNotificationCurrentChange
    };
  }
};
</script>