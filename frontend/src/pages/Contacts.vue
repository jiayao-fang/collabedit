<template>
  <div class="p-6 min-h-screen bg-gray-50 w-[600px] mx-auto">
    <div class="flex justify-between items-center mb-6">
      <el-button 
        type="text" 
        icon="el-icon-arrow-left" 
        @click="$router.go(-1)"
        class="text-gray-700"
      >
        返回
      </el-button>
      
      <h2 class="text-xl font-bold">联系人</h2>
      <el-button type="primary" @click="showAddContactDialog = true">
        <i class="el-icon-plus"></i> 添加联系人
      </el-button>
    </div>

    <!-- 联系人列表 -->
    <el-card v-loading="loading">
      <div v-if="contacts.length === 0 && !loading" class="text-center py-10">
        <el-empty description="暂无联系人"></el-empty>
      </div>
      
      <div class="space-y-3">
        <div 
          v-for="contact in contacts" 
          :key="contact.id"
          class="flex items-center p-3 hover:bg-gray-50 rounded-lg cursor-pointer"
          @click="handleContactClick(contact)"
        >
          <el-avatar :src="contact.avatar || defaultAvatar">
            {{ contact.username?.charAt(0) || '用' }}
          </el-avatar>
          <div class="ml-3 flex-1">
            <div class="font-medium">{{ contact.username }}</div>
            <div class="text-sm text-gray-500">{{ contact.email || '无邮箱信息' }}</div>
          </div>
          <el-button 
            type="text" 
            size="small"
            @click.stop="showActionMenu(contact, $event)"
          >
            <i class="el-icon-more"></i>
          </el-button> 
        </div>
      </div>
    </el-card>

    <!-- 添加联系人弹窗 -->
    <el-dialog
      title="添加联系人"
      v-model="showAddContactDialog"
      width="400px"
    >
      <el-input
        v-model="searchKeyword"
        placeholder="请输入用户名或邮箱搜索用户"
        class="mb-4"
        prefix-icon="el-icon-search"
        @input="searchUsers"
      ></el-input>
      
      <div v-if="searchResults.length > 0" class="max-h-60 overflow-y-auto">
        <div 
          v-for="user in searchResults" 
          :key="user.id"
          class="flex items-center p-3 hover:bg-gray-50 rounded-lg border-b"
        >
          <el-avatar :src="user.avatar || defaultAvatar">
            {{ user.username?.charAt(0) || '用' }}
          </el-avatar>
          <div class="ml-3 flex-1">
            <div class="font-medium">{{ user.username }}</div>
            <div class="text-sm text-gray-500">{{ user.email || '无邮箱信息' }}</div>
          </div>
          <!-- 添加按钮 -->
          <el-button 
            type="primary" 
            size="small"
            @click="addContact(user.id)"
          >
            添加
          </el-button>
        </div>
      </div>
      
      <div v-else-if="searchKeyword && !searchLoading" class="text-center py-4 text-gray-500">
        未找到相关用户
      </div>
      
      <template #footer>
        <el-button @click="showAddContactDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 联系人操作菜单 -->
    <el-dropdown 
      v-model:visible="actionMenuVisible"
      :teleported="true"
    >
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="sendMessage">发送消息</el-dropdown-item>
          <el-dropdown-item @click="editContact">编辑备注</el-dropdown-item>
          <el-dropdown-item @click="deleteContact" type="danger">删除联系人</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script>
import { ref} from 'vue';
import { useRouter } from 'vue-router';
import contactApi from '../api/contact';
import { ElMessage } from 'element-plus';
import { onMounted } from 'vue';

export default {
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const searchLoading = ref(false);
    const searchKeyword = ref('');
    const contacts = ref([]);
    const showAddContactDialog = ref(false);
    const actionMenuVisible = ref(false);
    const currentContact = ref(null);
    const searchResults = ref([]);
    const defaultAvatar = 'https://picsum.photos/200/200?random=avatar';

    // 加载联系人列表
    const loadContacts = async () => {
      try {
        loading.value = true;
        const res = await contactApi.getContacts();
        contacts.value = res.data || [];
      } catch (error) {
        ElMessage.error('加载联系人失败');
        console.error(error);
      } finally {
        loading.value = false;
      }
    };

    // 搜索用户
    const searchUsers = async () => {
      if (!searchKeyword.value.trim()) {
        searchResults.value = [];
        return;
      }
      try {
        searchLoading.value = true;
        const res = await contactApi.searchContacts(searchKeyword.value);
        searchResults.value = res.data || [];
      } catch (error) {
        ElMessage.error('搜索用户失败');
        console.error(error);
      } finally {
        searchLoading.value = false;
      }
    };

    // 添加联系人
    const addContact = async (userId) => {
      try {
        await contactApi.addContact(userId);
        ElMessage.success('添加联系人申请已发送');
        showAddContactDialog.value = false;
        loadContacts();
        // 重置搜索状态
        searchKeyword.value = '';
        searchResults.value = [];
      } catch (error) {
      console.error(error);
      }
    };

    // 显示操作菜单
    const showActionMenu = (contact, event) => {
      event.stopPropagation();
      currentContact.value = contact;
      actionMenuVisible.value = true;
    };

    
    // 确认删除联系人
    const confirmDeleteContact = async () => {
      try {
        await ElMessageBox.confirm(
          `确定要删除联系人 "${currentContact.value.username}" 吗？`,
          '删除联系人',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        await deleteContact();
      } catch (error) {
        console.log('用户取消删除');
      }
    };

    // 删除联系人
    const deleteContact = async () => {
      try {
        await contactApi.deleteContact(currentContact.value.id);
        ElMessage.success('删除联系人成功');
        actionMenuVisible.value = false;
        loadContacts();
      } catch (error) {
        ElMessage.error('删除联系人失败');
      }
    };

    // 编辑联系人
    const editContact = () => {
      // 这里可以实现编辑联系人功能
      actionMenuVisible.value = false;
      // 跳转到编辑页面或显示编辑弹窗
    };

    // 发送消息
    const sendMessage = () => {
      actionMenuVisible.value = false;
      router.push(`/messages/${currentContact.value.id}`);
    };

    // 点击联系人
    const handleContactClick = (contact) => {
      router.push(`/contacts/${contact.id}`);
    };

    onMounted(() => {
      loadContacts();
    });

    return {
      loading,
      searchLoading,
      searchKeyword,
      contacts,
      showAddContactDialog,
      actionMenuVisible,
      currentContact,
      searchResults,
      defaultAvatar,
      loadContacts,
      searchUsers,
      addContact,
      showActionMenu,
      deleteContact,
      editContact,
      sendMessage,
      handleContactClick
    };
  }
};
</script>
