<template>
  <div class="w-[600px] container mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">用户管理</h1>
    
    <!-- 筛选和搜索区域 -->
    <div class="bg-white p-6 rounded-lg shadow mb-6">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">角色筛选</label>
          <select 
            v-model="queryForm.roleId" 
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">全部角色</option>
            <option value="1">管理员</option>
            <option value="2">普通用户</option>
          </select>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">状态筛选</label>
          <select 
            v-model="queryForm.status" 
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">全部状态</option>
            <option value="0">禁用</option>
            <option value="1">启用</option>
          </select>
        </div>
        
        <div class="flex items-end">
          <button 
            @click="searchUsers" 
            class="w-full bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            搜索
          </button>
        </div>
        
        <div class="flex items-end">
          <button 
            @click="resetFilter" 
            class="w-full bg-gray-300 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-500"
          >
            重置
          </button>
        </div>
      </div>
    </div>
    
    <!-- 用户列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户名</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">邮箱</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">创建时间</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">更新时间</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="user in users" :key="user.id">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.id }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ user.username }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.email }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  :class="{
                    'px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800': user.status === 1,
                    'px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800': user.status === 0
                  }"
                >
                  {{ user.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(user.createTime) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(user.updateTime) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <button
                  v-if="user.status === 1"
                  @click="updateUserStatus(user.id, 0)"
                  class="text-red-600 hover:text-red-900 mr-3"
                >
                  禁用
                </button>
                <button
                  v-else
                  @click="updateUserStatus(user.id, 1)"
                  class="text-green-600 hover:text-green-900 mr-3"
                >
                  启用
                </button>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="7" class="px-6 py-4 text-center text-gray-500">
                暂无用户数据
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页 -->
      <div class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
        <div class="flex-1 flex justify-between sm:hidden">
          <button
            @click="prevPage"
            :disabled="currentPage === 1"
            class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage === 1 }"
          >
            上一页
          </button>
          <button
            @click="nextPage"
            :disabled="currentPage === totalPages"
            class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            :class="{ 'opacity-50 cursor-not-allowed': currentPage === totalPages }"
          >
            下一页
          </button>
        </div>
        <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
          <div>
            <p class="text-sm text-gray-700">
              显示第 <span class="font-medium">{{ (currentPage - 1) * pageSize + 1 }}</span> 
              至 <span class="font-medium">{{ Math.min(currentPage * pageSize, total) }}</span> 条，
              共 <span class="font-medium">{{ total }}</span> 条
            </p>
          </div>
          <div>
            <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
              <button
                @click="prevPage"
                :disabled="currentPage === 1"
                class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 text-sm font-medium text-gray-500 bg-white hover:bg-gray-50"
                :class="{ 'opacity-50 cursor-not-allowed': currentPage === 1 }"
              >
                &lt;
              </button>
              
              <button
                v-for="page in visiblePages"
                :key="page"
                @click="goToPage(page)"
                :class="{
                  'z-10 bg-blue-50 border-blue-500 text-blue-600': page === currentPage,
                  'bg-white border-gray-300 text-gray-500 hover:bg-gray-50': page !== currentPage
                }"
                class="relative inline-flex items-center px-4 py-2 border text-sm font-medium"
              >
                {{ page }}
              </button>
              
              <button
                @click="nextPage"
                :disabled="currentPage === totalPages"
                class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 text-sm font-medium text-gray-500 bg-white hover:bg-gray-50"
                :class="{ 'opacity-50 cursor-not-allowed': currentPage === totalPages }"
              >
                &gt;
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import userApi from '../api/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed } from 'vue';

// 分页和查询参数
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);

// 查询条件
const queryForm = ref({
  roleId: null,
  status: null
});

// 用户列表
const users = ref([]);

// 用户行为统计
const userBehavior = ref({});

// 获取用户列表
const fetchUsers = async () => {
  try {
    // 将roleId和status转换为数字类型
    const roleId = queryForm.value.roleId ? Number(queryForm.value.roleId) : null;
    const status = queryForm.value.status ? Number(queryForm.value.status) : null;
    
    const response = await userApi.getUsersByPage(
      currentPage.value,
      pageSize.value,
      roleId,
      status
    );
    
    console.log('获取的用户数据:', response); 
    
    // 确保数据正确赋值
    users.value = response.data?.list || [];
    total.value = response.data?.total || 0;
    totalPages.value = response.data?.pages || 0;
    
    console.log('处理后的用户列表:', users.value); 
    console.log('总用户数:', total.value); 
  } catch (error) {
    console.error('获取用户列表失败:', error);
    ElMessage.error('获取用户列表失败');
  }
};

// 获取用户行为统计
const fetchUserBehavior = async () => {
  try {
    const response = await userApi.getUserBehavior();
    userBehavior.value = response.length > 0 ? response[0] : {};
  } catch (error) {
    console.error('获取用户行为统计失败:', error);
    ElMessage.error('获取用户行为统计失败');
  }
};

// 更新用户状态
const updateUserStatus = async (userId, status) => {
  try {
    await ElMessageBox.confirm(
      `确定要${status === 1 ? '启用' : '禁用'}该用户吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await userApi.updateUserStatus(userId, status);
    ElMessage.success(`${status === 1 ? '启用' : '禁用'}成功`);
    fetchUsers(); // 重新获取用户列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新用户状态失败:', error);
      ElMessage.error('更新用户状态失败');
    }
  }
};

// 搜索用户
const searchUsers = () => {
  currentPage.value = 1;
  fetchUsers();
};

// 重置筛选条件
const resetFilter = () => {
  queryForm.value = {
    roleId: '',
    status: ''
  };
  currentPage.value = 1;
  fetchUsers();
};

// 分页相关方法
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchUsers();
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchUsers();
  }
};

const goToPage = (page) => {
  currentPage.value = page;
  fetchUsers();
};

// 计算显示的页码
const visiblePages = computed(() => {
  const delta = 2;
  const range = [];
  
  for (let i = Math.max(2, currentPage.value - delta); i <= Math.min(totalPages.value - 1, currentPage.value + delta); i++) {
    range.push(i);
  }
  
  if (totalPages.value > 1) {
    if (currentPage.value - delta > 1) {
      range.unshift('...');
      range.unshift(1);
    }
    
    if (currentPage.value + delta < totalPages.value) {
      range.push('...');
      range.push(totalPages.value);
    }
  }
  
  return range;
});

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

onMounted(() => {
  fetchUsers();
  fetchUserBehavior();
});
</script>
