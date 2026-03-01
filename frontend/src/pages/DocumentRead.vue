<template>
  <div class="p-6 h-screen bg-gray-50 w-[600px] mx-auto relative">
    <div class="flex justify-between items-center border-b pb-4 mb-6">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="$router.go(-1)"
        class="text-gray-700"
      >
        返回
      </el-button>
      
      <h2 class="text-xl font-bold">文档详情</h2>
      
      <div class="flex items-center gap-3">
        <el-dropdown @command="handleDropdownCommand">
          <el-button type="text" icon="el-icon-more" class="text-gray-700">
            …
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑文档</el-dropdown-item>
              <el-dropdown-item command="viewEditors">查看编辑者</el-dropdown-item>
              <el-dropdown-item command="assignTask" v-if="currentUserId === doc.authorId">布置任务</el-dropdown-item>
              <el-dropdown-item command="viewTasks">查看任务</el-dropdown-item>
              <el-dropdown-item command="versionHistory">查看版本历史</el-dropdown-item>
              <el-dropdown-item command="delete">删除文档</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 文档详情 -->
    <div class="max-w-3xl mx-auto bg-white rounded-lg p-6 shadow-sm relative">
      <div class="mb-6">
        <div class="text-xl font-medium text-gray-800">
          {{ doc.title || '无标题' }}
        </div>
      </div>
      
      <div class="flex justify-between text-sm text-gray-500 mb-4">
        <div>作者: {{ doc.authorName || '未知' }}</div>
      </div>
      <div class="flex justify-between text-sm text-gray-500 mb-4">
        <div>发布时间: {{ formatDate(doc.createTime) || '未知' }}</div>
      </div>

      <!-- 文档内容区域 -->
      <div class="mb-6 flex text-left relative">
        <div 
          ref="contentRef"
          class="border rounded p-4 min-h-[400px] w-full relative"
          v-html="doc.content || '无内容'"
          @mouseup="handleTextSelect"
        ></div>
        
        <!-- 行内评论按钮 -->
        <div 
          v-if="selectedText"
          class="absolute z-10 bg-white shadow-md rounded-md p-2"
          :style="{ top: commentBtnTop + 'px', left: commentBtnLeft + 'px' }"
        >
          <el-button 
            type="primary" 
            size="small" 
            @click="openCommentDialog"
          >
            评论
          </el-button>
        </div>
      </div>

      <!-- 评论列表区域 -->
      <div class="mt-8 border-t pt-6">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-medium">评论区 ({{ commentList.length }})</h3>
          <el-button 
            type="text" 
            size="small" 
            @click="refreshComments"
            icon="el-icon-refresh"
          >
            刷新
          </el-button>
        </div>
        
        <!-- 无评论提示 -->
        <div v-if="commentList.length === 0" class="text-center text-gray-500 py-8">
          <el-empty description="暂无评论，快来发表第一条评论吧～"></el-empty>
        </div>
        
        <!-- 评论列表 -->
        <div v-else class="space-y-6 max-h-[500px] overflow-auto pr-2">
          <!-- 一级评论 -->
          <div v-for="comment in commentList" :key="comment.id" class="border-b pb-4">
            <!-- 评论头部 -->
            <div class="flex justify-between items-start mb-2">
              <div class="flex items-center gap-2">
                <el-avatar :src="comment.avatar || defaultAvatar">
                  {{ comment.userName?.charAt(0) || '评' }}
                </el-avatar>
                <span class="font-medium">{{ comment.userName || '未知用户' }}</span>
                <span v-if="comment.position" class="text-xs text-gray-500 ml-2">
                  位置：第{{ JSON.parse(comment.position).line }}行第{{ JSON.parse(comment.position).column }}列
                </span>
              </div>
              <span class="text-xs text-gray-500">{{ formatDate(comment.commentTime) }}</span>
            </div>
            
            <!-- 评论内容 -->
            <div class="ml-10 mb-3 break-all items-start">{{ comment.content }}</div>
            
            <!-- 评论操作 -->
            <div class="ml-10 flex gap-3 text-sm">
              <el-button 
                type="text" 
                size="middle" 
                @click="openReplyDialog(comment.id)"
              >
                回复 ({{ comment.replies?.length || 0 }})
              </el-button>
              <el-button 
                v-if="comment.userId === currentUserId || doc.authorId === currentUserId"
                type="text" 
                size="middle" 
                text="danger"
                @click="deleteComment(comment.id)"
              >删除
              </el-button>
            </div>
            
            <!-- 回复输入框 -->
            <div v-if="replyCommentId === comment.id" class="ml-10 mt-3">
              <el-input
                v-model="replyContent"
                type="textarea"
                placeholder="输入回复内容，支持@用户"
                rows="3"
                @keyup="onKeyup($event, 'reply')"
              ></el-input>
              <div class="flex justify-end gap-2 mt-2">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button size="small" type="primary" @click="submitReply">提交回复</el-button>
              </div>
            </div>
            
            <!-- 二级回复列表 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="ml-10 mt-3 space-y-3 border-l-2 pl-3">
              <div v-for="reply in comment.replies" :key="reply.id" class="pb-2 border-b">
                <div class="flex justify-between items-start mb-1">
                  <div class="flex items-center gap-2">
                    <el-avatar size="small" :src="reply.avatar ||defaultAvatar">
                      {{ reply.userName?.charAt(0) || '回' }}
                    </el-avatar>
                    <span class="font-medium text-sm">{{ reply.userName || '未知用户' }}</span>
                  </div>
                  <span class="text-xs text-gray-500">{{ formatDate(reply.commentTime) }}</span>
                </div>
                <div class="ml-8 text-sm break-all">{{ reply.content }}</div>
                <div class="ml-8 flex gap-3 text-xs mt-1">
                  <el-button 
                    type="text" 
                    size="small" 
                    @click="openReplyDialog(comment.id, reply.id)"
                  >
                    回复
                  </el-button>
                  <el-button 
                    v-if="reply.userId === currentUserId || doc.authorId === currentUserId"
                    type="text" 
                    size="small" 
                    text="danger"
                    @click="deleteComment(reply.id)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 行内评论弹窗 -->
    <el-dialog 
      title="添加评论" 
      v-model="commentDialogVisible"
      width="500px"
      @close="resetCommentForm"
    >
      <div class="mb-2">
        <span class="text-gray-500 text-sm">选中的文本：</span>
        <span class="text-sm font-medium">{{ selectedText }}</span>
      </div>
      <el-input
        v-model="commentContent"
        type="textarea"
        placeholder="输入评论内容，支持@用户"
        rows="4"
        @keyup="onKeyup($event, 'comment')"
      ></el-input>
      <template #footer>
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment">提交评论</el-button>
      </template>
    </el-dialog>

    <!-- @用户选择弹窗 -->
    <el-dialog 
      title="选择要@的用户" 
      v-model="mentionDialogVisible"
      width="400px"
    >
      <el-input
        v-model="searchUserText"
        placeholder="搜索联系人"
        prefix-icon="el-icon-search"
        class="mb-4"
      ></el-input>
      <div class="max-h-[300px] overflow-auto">
        <div 
          v-for="contact in filteredContactsForMention" 
          :key="contact.id"
          @click="insertMentionUser(contact.username)"
          class="cursor-pointer hover:bg-gray-50 p-2 flex items-center rounded"
        >
          <el-avatar size="small" :src="contact.avatar || defaultAvatar">
            {{ contact.username?.charAt(0) }}
          </el-avatar>
          <span class="ml-2">{{ contact.username }}</span>
        </div>
        <div v-if="filteredContactsForMention.length === 0" class="text-center text-gray-500 py-4">
          未找到联系人
        </div>
      </div>
    </el-dialog>

    <!-- 1. 查看编辑者弹窗 -->
    <el-dialog 
      title="文档编辑者" 
      v-model="editorDialogVisible"
      width="600px"
      destroy-on-close
    >
      <template #header>
        <div class="flex justify-between items-center w-full" style="padding-left: 20px;">
          <span>文档编辑者</span>
          <!-- 邀请编辑者按钮 -->
          <el-button 
            type="primary" 
            size="small" 
            @click="openInviteEditorDialog"
            v-if="currentUserId === doc.authorId"
          >
            邀请编辑者
          </el-button>
        </div>
      </template>

      <div v-if="editors.length > 0" class="space-y-4">
        <div 
          v-for="editor in editors" 
          :key="editor.id"
          class="flex items-center justify-between p-2 hover:bg-gray-50 rounded"
        >
          <div class="flex items-center gap-3">
            <el-avatar :src="editor.avatar || defaultAvatar">
              {{ editor.username?.charAt(0) || '未知用户' }}
            </el-avatar>
            <span class="text-gray-800">{{ editor.username || '未知用户' }}</span>
          </div>
          <button
            class="text-red-500 cursor-pointer hover:text-red-600"
            @click="deleteEditor(editor.id)"
            v-if="currentUserId === doc.authorId"
          >
            删除
          </button> 
        </div>
      </div>
      <div v-else class="text-center text-gray-500 py-4">
        暂无其他编辑者
      </div>

      <template #footer>
        <el-button @click="editorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 2. 布置任务弹窗 -->
    <el-dialog
      width="600px"
      destroy-on-close
      title="布置任务"
      v-model="assignTaskDialogVisible"
    >
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="任务标题" required>
          <el-input
            v-model="taskForm.title"
            placeholder="请输入任务标题"
            maxlength="100"
            show-word-limit
          ></el-input>
        </el-form-item>
        
        <el-form-item label="任务描述">
          <el-input
            v-model="taskForm.content"
            type="textarea"
            placeholder="请输入任务描述（支持@用户）"
            :rows="4"
            @keyup="onTaskKeyup"
          ></el-input>
        </el-form-item>
        
        <el-form-item label="分配给" required>
          <el-input
            v-model="taskAssigneeSearch"
            placeholder="搜索联系人..."
            prefix-icon="el-icon-search"
            class="mb-2"
            @input="searchTaskAssignee"
          ></el-input>
          <div v-if="taskAssigneeSearch && filteredTaskAssignees.length > 0" class="border rounded p-2 max-h-40 overflow-auto">
            <div
              v-for="contact in filteredTaskAssignees"
              :key="contact.id"
              @click="selectTaskAssignee(contact)"
              class="cursor-pointer hover:bg-gray-50 p-2 flex items-center"
            >
              <el-avatar size="small" :src="contact.avatar || defaultAvatar">
                {{ contact.username?.charAt(0) }}
              </el-avatar>
              <span class="ml-2">{{ contact.username }}</span>
              <el-checkbox
                :model-value="isAssigneeSelected(contact.id)"
                class="ml-auto"
                @click.stop
                @change="toggleTaskAssignee(contact)"
              ></el-checkbox>
            </div>
          </div>
          <div v-if="selectedTaskAssignees.length > 0" class="mt-2 flex flex-wrap items-center gap-2">
            <el-tag
              v-for="assignee in selectedTaskAssignees"
              :key="assignee.id"
              closable
              @close="removeTaskAssignee(assignee.id)"
            >
              <el-avatar size="small" :src="assignee.avatar || defaultAvatar" class="mr-1">
                {{ assignee.username?.charAt(0) }}
              </el-avatar>
              {{ assignee.username }}
            </el-tag>
          </div>
        </el-form-item>
        
        <el-form-item label="截止日期" required>
          <el-date-picker
            v-model="taskForm.deadline"
            type="datetime"
            placeholder="选择截止日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="assignTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask" :loading="taskSubmitting">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 查看任务弹窗 - 使用独立组件 -->
    <TaskListDialog
      v-model="viewTasksDialogVisible"
      :doc-id="doc.id"
      :current-user-id="currentUserId"
    />

    <!-- 2. 邀请编辑者弹窗 -->
    <el-dialog
      width="500px"
      destroy-on-close
      title="邀请编辑者"
      v-model="inviteEditorDialogVisible" 
    >
      <el-input
        v-model="editorSearchKeyword"
        placeholder="搜索联系人..."
        class="mb-4"
        prefix-icon="el-icon-search"
      ></el-input>
      
      <el-card class="mb-4">
        <div v-for="contact in filteredContacts" :key="contact.id" class="flex items-center justify-between p-2 hover:bg-gray-50">
          <div class="flex items-center">
            <el-avatar :src="contact.avatar || defaultAvatar">
              {{ contact.username?.charAt(0) || '用' }}
            </el-avatar>
            <div class="ml-3">
              <div>{{ contact.username }}</div>
              <div class="text-sm text-gray-500">{{ contact.email || '无邮箱' }}</div>
            </div>
          </div>
          <el-checkbox 
            v-model="selectedEditors" 
            :label="contact.id"
            :checked="editors.some(e => e.id === contact.id)"
            :disabled="editors.some(e => e.id === contact.id)"
          ></el-checkbox>
        </div>
      </el-card>
      
      <template #footer>
        <el-button @click="inviteEditorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmInviteEditors">确认邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>


<script>
import api from "../api/document";
import userApi from "../api/user";
import contactApi from "../api/contact";
import taskApi from "../api/task";
import TaskListDialog from "../pages/TaskListDiaLog.vue";
import { onMounted, ref, reactive, computed, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";

export default {
  components: {
    TaskListDialog
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const loading = ref(false);
    const defaultAvatar = 'https://picsum.photos/200/200?random=avatar';
    const contentRef = ref(null); // 文档内容DOM引用
    
    const doc = reactive({ 
      id: null, 
      title: "", 
      content: "",
      authorId: "",   
      authorName: "", 
      createTime: ""  
    });
    const editorDialogVisible = ref(false); // 查看编辑者弹窗显隐
    const inviteEditorDialogVisible = ref(false); // 新增：邀请编辑者弹窗单独的显隐变量
    const editors = ref([]);
    const currentUserId = ref(null);
    // 查看任务弹窗相关
    const viewTasksDialogVisible = ref(false); // 查看任务弹窗显隐

    // 动态获取当前用户信息
    const getCurrentUserInfo = async () => {
      try {
        const res = await userApi.getCurrentUser();
        if (res && res.data.id) {
          currentUserId.value = res.data.id;
        } else {
          // 如果无法获取用户信息，使用默认值但标记为未登录状态
          currentUserId.value = null;
          console.warn('无法获取当前用户信息');
        }
      } catch (error) {
        console.error('获取当前用户信息失败:', error);
        currentUserId.value = null;
      }
    };


    // ========== 行内评论相关 ==========
    const selectedText = ref(""); // 选中的文本
    const commentBtnTop = ref(0); // 评论按钮top位置
    const commentBtnLeft = ref(0); // 评论按钮left位置
    const commentPosition = ref({ line: 0, column: 0 }); // 评论位置（行/列）
    const commentDialogVisible = ref(false); // 评论弹窗显示
    const commentContent = ref(""); // 评论内容
    
    // ========== 回复评论相关 ==========
    const replyCommentId = ref(null); // 回复的父评论ID
    const replyContent = ref(""); // 回复内容
    const replyTargetId = ref(null); // 回复的目标用户ID（可选）
    
    // ========== @用户相关 ==========
    const mentionDialogVisible = ref(false); // @用户弹窗
    const searchUserText = ref(""); // 搜索用户名
    const mentionType = ref(""); // @的类型：comment/reply
    
    // ========== 评论列表（核心优化：确保显示所有评论） ==========
    const commentList = ref([]); // 该文档的所有一级评论（包含回复）
    
    // ========== 通知相关（仅保留未读数量） ==========
    const notificationCount = ref(0);
    let notificationTimer = null; // 定时刷新未读数量

    // 联系人相关
    const contacts = ref([]);
    const editorSearchKeyword = ref('');
    const selectedEditors = ref([]);

    // 加载联系人列表
    const loadContacts = async () => {
      try {
        const res = await api.getInvitableContacts(doc.id);
        contacts.value = res.data || [];
      } catch (error) {
        ElMessage.error('加载联系人失败');
        console.error(error);
      }
    };

    // 筛选联系人（用于邀请编辑者）
    const filteredContacts = computed(() => {
      if (!editorSearchKeyword.value) return contacts.value;
      const keyword = editorSearchKeyword.value.toLowerCase();
      return contacts.value.filter(contact => 
        contact.username.toLowerCase().includes(keyword) ||
        contact.email?.toLowerCase().includes(keyword)
      );
    });

    // 筛选联系人（用于@用户）
    const filteredContactsForMention = computed(() => {
      if (!searchUserText.value) return contacts.value;
      const keyword = searchUserText.value.toLowerCase();
      return contacts.value.filter(contact => 
        contact.username.toLowerCase().includes(keyword) ||
        contact.email?.toLowerCase().includes(keyword)
      );
    });

    // 确认邀请编辑者
    const confirmInviteEditors = async () => {
      if (selectedEditors.value.length === 0) {
        ElMessage.warning('请选择要邀请的编辑者');
        return;
      }
      
      try {
        for (const userId of selectedEditors.value) {
          await api.addEditor(doc.id, userId);
        }
        ElMessage.success('邀请成功');
        inviteEditorDialogVisible.value = false; // 关闭邀请弹窗
        loadEditors(); // 重新加载编辑者列表
        selectedEditors.value = [];
      } catch (error) {
        ElMessage.error('邀请失败');
        console.error(error);
      }
    };

    // 打开邀请编辑者弹窗
    const openInviteEditorDialog = () => {
      inviteEditorDialogVisible.value = true;
      loadContacts(); // 打开弹窗时加载联系人
    };

    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return Number.isNaN(date.getTime()) ? dateString : date.toLocaleString();
    };

    const getAuthorNameById = async (authorId) => {
      if (!authorId) return;
      try {
        const res = await api.getAuthorName(authorId);
        doc.authorName = typeof res.data === 'string' ? res.data : res.data?.name || '未知';
      } catch (error) {
        ElMessage.warning('获取作者名称失败，显示为"未知"');
        doc.authorName = '未知';
      }
    };

    const loadDocDetail = async () => {
      const docId = route.params.id; 
      if (!docId) {
        ElMessage.error('文档ID不存在');
        return;
      }
      
      loading.value = true;
      try {
        const res = await api.get(docId);
        Object.assign(doc, res.data);
        if (doc.authorId) {
          await getAuthorNameById(doc.authorId);
        } else {
          doc.authorName = '未知';
        }
        // 加载该文档所有评论
        await loadAllDocComments();
      } catch (error) {
      } finally {
        loading.value = false;
      }
    };

    const getUserInfoByIds = async (editorIds) => {
      if (!editorIds || editorIds.length === 0) return [];
      try {
        const res = await userApi.getUsersByIds(editorIds);
        return res.data || [];
      } catch (error) {
        ElMessage.error('获取编辑者信息失败');
        return [];
      }
    };

    const loadEditors = async () => {
      const docId = route.params.id;
      if (!docId) return;

      loading.value = true;
      try {
        const editorRes = await api.getEditors(docId);
        const editorIds = (editorRes.data || [])
          .filter(id => id && id !== doc.authorId);

        if (editorIds.length === 0) {
          editors.value = [];
          return;
        }

        const userRes = await userApi.getUsersByIds(editorIds);
        const userList = userRes.data || [];

        editors.value = userList.map(user => ({
          id: user.id,
          username: user.username,
          avatar: user.avatar
        }));

      } catch (error) {
      } finally {
        loading.value = false;
      }
    };

    const inviteEditor = async () => {
      ElMessage.info('请通过「查看编辑者」弹窗内的按钮邀请');
    };

    const deleteEditor = async (editorId) => {
      const docId = route.params.id;
      if (!docId || !editorId) return;
      
      try {
        await ElMessageBox.confirm(
          '此操作将移除该编辑者，是否继续？',
          '提示',
          { type: 'warning' }
        );
        await api.removeEditor(docId, editorId);
        editors.value = editors.value.filter(item => item.id !== editorId);
        ElMessage.success('已移除该编辑者');
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败：' + (error.message || '服务器错误'));
        } else {
          ElMessage.info('已取消操作');
        }
      }
    };

    const deleteDocument = async () => {
      const docId = route.params.id;
      if (!docId) {
        ElMessage.error('文档ID不存在，无法删除');
        return;
      }

      try {
        await ElMessageBox.confirm(
          '确定要删除该文档吗？',
          '删除确认',
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning',
            draggable: true
          }
        );

        loading.value = true;
        const res = await api.delete(docId);
        
        if (res.data === 'success') {
          ElMessage.success("删除成功！可以在回收站找回该文档。");
          router.push('/main/all');
        } else if (res.data === '无权限删除此文档') {
          ElMessage.error("您暂时没有权限删除该文档");
        }
      } catch (error) {
        if (error === 'cancel') {
          ElMessage.info('已取消删除操作');
        } else {
          const errorMsg = error.response?.data || '删除文档失败，请稍后重试';
          ElMessage.error(errorMsg);
        }
      } finally {
        loading.value = false;
      }
    };

    // ========== 布置任务相关 ==========
    const assignTaskDialogVisible = ref(false);
    const taskForm = reactive({
      title: '',
      content: '',
      deadline: ''
    });
    const taskAssigneeSearch = ref('');
    const selectedTaskAssignees = ref([]);
    const filteredTaskAssignees = ref([]);
    const taskSubmitting = ref(false);

    // 搜索任务被分配人
    const searchTaskAssignee = () => {
      if (!taskAssigneeSearch.value.trim()) {
        filteredTaskAssignees.value = [];
        return;
      }
      const keyword = taskAssigneeSearch.value.toLowerCase();
      filteredTaskAssignees.value = contacts.value.filter(contact =>
        contact.username.toLowerCase().includes(keyword) ||
        contact.email?.toLowerCase().includes(keyword)
      );
    }

    // 检查接收者是否已选择
    const isAssigneeSelected = (contactId) => {
      return selectedTaskAssignees.value.some(a => a.id === contactId);
    }

    // 切换任务被分配人选择
    const toggleTaskAssignee = (contact) => {
      const index = selectedTaskAssignees.value.findIndex(a => a.id === contact.id);
      if (index > -1) {
        selectedTaskAssignees.value.splice(index, 1);
      } else {
        selectedTaskAssignees.value.push(contact);
      }
    }

    // 选择任务被分配人（点击列表项时）
    const selectTaskAssignee = (contact) => {
      if (!isAssigneeSelected(contact.id)) {
        selectedTaskAssignees.value.push(contact);
      }
    }

    // 移除任务被分配人
    const removeTaskAssignee = (contactId) => {
      const index = selectedTaskAssignees.value.findIndex(a => a.id === contactId);
      if (index > -1) {
        selectedTaskAssignees.value.splice(index, 1);
      }
    }

    // 任务内容中@用户
    const onTaskKeyup = (event) => {
      if (event.key === '@' && !event.repeat) {
        openMentionDialog('task');
      }
    }

    // 禁用过去的日期
    const disabledDate = (time) => {
      return time.getTime() < Date.now() - 24 * 60 * 60 * 1000; // 不能选择今天之前的日期
    }

    // 提交任务
    const submitTask = async () => {
      if (!taskForm.title.trim()) {
        ElMessage.warning('请输入任务标题');
        return;
      }
      if (selectedTaskAssignees.value.length === 0) {
        ElMessage.warning('请至少选择一个被分配人');
        return;
      }
      if (!taskForm.deadline) {
        ElMessage.warning('请选择截止日期');
        return;
      }

      // 检查截止日期不能早于当前时间
      const deadline = new Date(taskForm.deadline);
      if (deadline < new Date()) {
        ElMessage.warning('截止日期不能早于当前时间');
        return;
      }

      try {
        taskSubmitting.value = true;
        const docId = route.params.id;
        
        // 提取被分配人ID列表
        const assigneeIds = selectedTaskAssignees.value.map(a => a.id);
        
        await taskApi.createTask({
          docId: docId,
          title: taskForm.title.trim(),
          content: taskForm.content.trim(),
          assigneeIds: assigneeIds,
          deadline: taskForm.deadline
        });

        ElMessage.success('任务发布成功');
        assignTaskDialogVisible.value = false;
        
        // 重置表单
        taskForm.title = '';
        taskForm.content = '';
        taskForm.deadline = '';
        selectedTaskAssignees.value = [];
        taskAssigneeSearch.value = '';
        filteredTaskAssignees.value = [];
        
        // 刷新通知
        await getUnreadNotificationCount();
        window.dispatchEvent(new CustomEvent('notificationUpdated'));
      } catch (error) {
        ElMessage.error('发布任务失败：' + (error.response?.data?.message || error.message || '服务器错误'));
      } finally {
        taskSubmitting.value = false;
      }
    }

    // 从内容中提取@的用户ID
    const extractMentionedUserIds = (content) => {
      if (!content) return [];
      const mentions = content.match(/@(\w+)/g);
      if (!mentions) return [];
      
      const userIds = [];
      mentions.forEach(mention => {
        const username = mention.substring(1);
        const contact = contacts.value.find(c => c.username === username);
        if (contact) {
          userIds.push(contact.id);
        }
      });
      return [...new Set(userIds)]; // 去重
    }

    const handleDropdownCommand = (command) => {
      const docId = route.params.id;
      if (!docId) return;
      
      switch (command) {
        case 'edit':
          router.push(`/documents/edit/${docId}`);
          break;
        case 'viewEditors':
          editorDialogVisible.value = true;
          loadEditors();
          break;
        case 'versionHistory':
          router.push(`/documents/${docId}/version-history`);
          break;
        case 'assignTask':
          assignTaskDialogVisible.value = true;
          loadContacts(); // 加载联系人列表
          break;
        case 'viewTasks':
          viewTasksDialogVisible.value = true;
          break;
        case 'delete':
          deleteDocument();
          break;
      }
    };

    // ========== 行内文本选中处理 ==========
    const handleTextSelect = () => {
      const selection = window.getSelection();
      const text = selection.toString().trim();
      
      if (text) {
        selectedText.value = text;
        // 获取选中位置的坐标
        const range = selection.getRangeAt(0);
        const rect = range.getBoundingClientRect();
        const contentRect = contentRef.value.getBoundingClientRect();
        
        // 计算评论按钮位置（相对文档内容区域）
        commentBtnTop.value = rect.top - contentRect.top - 30;
        commentBtnLeft.value = rect.left - contentRect.left + rect.width / 2 - 40;
        
        // 简易计算行/列
        const content = contentRef.value.innerText;
        const beforeText = content.substring(0, selection.anchorOffset);
        const line = beforeText.split('\n').length;
        const column = beforeText.split('\n').pop().length + 1;
        commentPosition.value = { line, column };
      } else {
        selectedText.value = "";
      }
    };

    // ========== 加载联系人列表（用于@功能） ==========
    const loadContactsForMention = async () => {
      try {
        const res = await contactApi.getContacts();
        contacts.value = res.data || [];
      } catch (error) {
        ElMessage.error('@功能加载联系人失败');
        contacts.value = [];
      }
    };

    // ========== @用户相关 ==========
    const openMentionDialog = (type) => {
      mentionType.value = type;
      mentionDialogVisible.value = true;
      searchUserText.value = "";
    };

    // 添加键盘事件处理函数
    const onKeyup = (event, type) => {
      if (event.key === '@' && !event.repeat) {
        openMentionDialog(type);
      }
    };

    const insertMentionUser = (username) => {
      const mentionText = `@${username} `;
      if (mentionType.value === 'comment') {
        // 检查commentContent的最后一个字符是否是@，如果是则替换掉
        if (commentContent.value.endsWith('@')) {
          commentContent.value = commentContent.value.slice(0, -1) + mentionText;
        } else {
          commentContent.value += mentionText;
        }
      } else if (mentionType.value === 'task') {
        // 任务内容中的@
        if (taskForm.content.endsWith('@')) {
          taskForm.content = taskForm.content.slice(0, -1) + mentionText;
        } else {
          taskForm.content += mentionText;
        }
      } else {
        // 检查replyContent的最后一个字符是否是@，如果是则替换掉
        if (replyContent.value.endsWith('@')) {
          replyContent.value = replyContent.value.slice(0, -1) + mentionText;
        } else {
          replyContent.value += mentionText;
        }
      }
      mentionDialogVisible.value = false;
    };
    
    // ========== 评论核心逻辑（优化：加载该文档所有评论） ==========
    const loadAllDocComments = async () => {
      const docId = route.params.id;
      if (!docId) return;
  
      try {
        // 1. 获取该文档所有评论（包括一级评论和回复）
        const commentRes = await api.getCommentsByDocId(docId);
        const allComments = commentRes.data || [];
        
        // 2. 分离一级评论和回复
        const primaryComments = allComments.filter(comment => !comment.parentId);
        const replies = allComments.filter(comment => comment.parentId);
        
        // 3. 将回复按 parentID 分组
        const repliesGroupedByParent = {};
        replies.forEach(reply => {
          if (!repliesGroupedByParent[reply.parentId]) {
            repliesGroupedByParent[reply.parentId] = [];
          }
          repliesGroupedByParent[reply.parentId].push(reply);
        });
        
        // 4. 为每个评论加载用户信息
        const commentsWithUserInfo = await Promise.all(
          primaryComments.map(async (comment) => {
            // 加载评论者信息
            if (comment.userId) {
              try {
                const userRes = await userApi.getUserInfoById(comment.userId);
                comment.userName = userRes.data.username || '未知用户';
                comment.avatar=userRes.data.avatar;
              } catch (error) {
                comment.userName = '未知用户';
              }
            }
            
            // 添加该评论的回复列表
            comment.replies = repliesGroupedByParent[comment.id] || [];
            
            // 为每个回复加载用户信息
            comment.replies = await Promise.all(
              comment.replies.map(async (reply) => {
                if (reply.userId) {
                  try {
                    const userRes = await userApi.getUserInfoById(reply.userId);
                    reply.userName = userRes.data.username || '未知用户';
                    reply.avatar=userRes.data.avatar;
                  } catch (error) {
                    reply.userName = '未知用户';
                  }
                }
                return reply;
              })
            );
            
            return comment;
          })
        );
        
        commentList.value = commentsWithUserInfo;
      } catch (error) {
        ElMessage.error('加载评论列表失败：' + (error.message || '服务器错误'));
        commentList.value = [];
      }
    };

    // 刷新评论列表（绑定到刷新按钮）
    const refreshComments = async () => {
      await loadAllDocComments();
      ElMessage.success('评论列表已刷新');
    };

    // ========== 评论操作 ==========
    const openCommentDialog = () => {
      commentDialogVisible.value = true;
      commentContent.value = `「${selectedText.value}」`;
    };

    const resetCommentForm = () => {
      commentContent.value = "";
      selectedText.value = "";
      commentPosition.value = { line: 0, column: 0 };
    };

    const submitComment = async () => {
      if (!commentContent.value.trim()) {
        ElMessage.warning('评论内容不能为空');
        return;
      }
      
      // 确保用户已登录且能获取到用户ID
      if (!currentUserId.value) {
        ElMessage.error('请先登录后再发表评论');
        return;
      }
      
      const docId = route.params.id;
      if (!docId) return;
      
      try {
        loading.value = true;
        await api.addComment({
          docId,
          content: commentContent.value.trim(),
          position: JSON.stringify(commentPosition.value)
        });
        
        ElMessage.success('评论添加成功');
        commentDialogVisible.value = false;
        resetCommentForm();
        await loadAllDocComments();
        await getUnreadNotificationCount();
        if(currentUserId.value !== doc.authorId){
          window.dispatchEvent(new CustomEvent('notificationUpdated'));
        }
      } catch (error) {
        ElMessage.error('添加评论失败：' + (error.message || '服务器错误'));
      } finally {
        loading.value = false;
      }
    };

    // ========== 回复操作 ==========
    const openReplyDialog = (parentId, targetId) => {
      // 确保用户已登录且能获取到用户ID
      if (!currentUserId.value) {
        ElMessage.error('请先登录后再发表回复');
        return;
      }
      
      replyCommentId.value = parentId;
      replyTargetId.value = targetId;
      replyContent.value = "";
    };

    const cancelReply = () => {
      replyCommentId.value = null;
      replyContent.value = "";
    };

    const submitReply = async () => {
      if (!replyContent.value.trim()) {
        ElMessage.warning('回复内容不能为空');
        return;
      }
      
      // 确保用户已登录且能获取到用户ID
      if (!currentUserId.value) {
        ElMessage.error('请先登录后再发表回复');
        return;
      }
      
      if (!replyCommentId.value) return;
      
      try {
        loading.value = true;
        await api.replyComment(replyCommentId.value, {
          content: replyContent.value.trim(),
          position: JSON.stringify(commentPosition.value)
        });
        
        ElMessage.success('回复成功');
        // 使用修复后的函数重新加载所有评论
        await loadAllDocComments();
        await getUnreadNotificationCount();
        window.dispatchEvent(new CustomEvent('notificationUpdated'));
        cancelReply();
      } catch (error) {
        ElMessage.error('回复失败：' + (error.message || '服务器错误'));
      } finally {
        loading.value = false;
      }
    };

    // ========== 删除评论 ==========
    const deleteComment = async (commentId) => {
      try {
        await ElMessageBox.confirm(
          '确定要删除该评论/回复吗？',
          '删除确认',
          { type: 'warning' }
        );
        
        await api.deleteComment(commentId);
        ElMessage.success('删除成功');
        // 重新加载所有评论
        await loadAllDocComments();
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败：' + (error.message || '服务器错误'));
        }
      }
    };

    // ========== 通知相关（仅保留未读数量） ==========
    const getUnreadNotificationCount = async () => {
      try {
        const res = await api.getUnreadNotificationCount();
        notificationCount.value = res.data || 0;
        localStorage.setItem("notificationUnreadCount", res.data || 0);
      } catch (error) {
        console.error('获取未读通知数量失败', error);
      }
    };

    // ========== 初始化与销毁 ==========
    onMounted(async () => {
      // 页面加载时首先获取当前用户信息
      await getCurrentUserInfo();
      loadDocDetail();
      await loadContactsForMention();
      getUnreadNotificationCount();
      // 监听其他组件发出的通知更新事件
      const handleNotificationUpdate = () => {
        getUnreadNotificationCount();
      };

      window.addEventListener('notificationUpdated', handleNotificationUpdate);
    
      // 定时刷新未读数量
      notificationTimer = setInterval(() => {
        getUnreadNotificationCount();
      }, 30000);
    });

    onUnmounted(() => {
      if (notificationTimer) clearInterval(notificationTimer);
      window.removeEventListener('notificationUpdated', () => getUnreadNotificationCount());
    });

    return {
      // 原有返回值
      doc,
      editorDialogVisible,
      inviteEditorDialogVisible, 
      editors,
      defaultAvatar,
      formatDate,
      handleDropdownCommand,
      inviteEditor,
      deleteEditor,
      openInviteEditorDialog,
      contentRef,
      selectedText,
      commentBtnTop,
      commentBtnLeft,
      commentDialogVisible,
      commentContent,
      replyCommentId,
      replyContent,
      mentionDialogVisible,
      searchUserText,
      filteredContacts,
      filteredContactsForMention,
      commentList,
      currentUserId,
      notificationCount,
      contacts,
      editorSearchKeyword,
      selectedEditors,
      onKeyup,
      handleTextSelect,
      openMentionDialog,
      insertMentionUser,
      openCommentDialog,
      submitComment,
      openReplyDialog,
      cancelReply,
      submitReply,
      deleteComment,
      confirmInviteEditors,
      refreshComments,
      assignTaskDialogVisible,
      taskForm,
      taskAssigneeSearch,
      selectedTaskAssignees,
      filteredTaskAssignees,
      taskSubmitting,
      searchTaskAssignee,
      isAssigneeSelected,
      toggleTaskAssignee,
      selectTaskAssignee,
      removeTaskAssignee,
      onTaskKeyup,
      disabledDate,
      submitTask,
      viewTasksDialogVisible
    };
  }
};
</script>