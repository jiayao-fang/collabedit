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
    <h2 class="text-xl font-bold">编辑个人资料</h2>
  </div>

  <!-- 主体内容 -->
  <div class="p-6 max-w-md mx-auto w-600">
    <!-- 头像上传区域 -->
    <div class="flex flex-col items-center mb-8">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :http-request="customUpload" 
        :before-upload="beforeAvatarUpload"
      >
        <!-- 添加userInfo空值保护，避免undefined报错 -->
        <img :src="avatarUrl" 
          class="w-24 h-24 rounded-full object-cover border-2 border-gray-200"
        >

        <div class="absolute -bottom-2 -right-2 w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white">
          <i class="el-icon-camera">📷</i>
        </div>
      </el-upload>
    </div>

    <!-- 个人信息表单 -->
    <el-form 
      ref="profileForm" 
      :model="form" 
      label-width="80px"
      class="mb-10"
    >
      <!-- 用户名 -->
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>

      <!-- 邮箱 -->
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" type="email" placeholder="请输入邮箱" />
      </el-form-item>

      <!-- 手机号 -->
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>

      <!-- 个性签名 -->
      <el-form-item label="个性签名">
        <el-input 
          v-model="form.signature" 
          placeholder="填写个性签名更容易被了解..." 
          type="textarea" 
          :rows="2"
        />
      </el-form-item>

      <!-- 提交按钮 -->
      <el-form-item>
        <el-button 
          type="primary" 
          class="w-full"
          @click="submitProfile"
        >
          保存修改
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 退出登录 -->
    <el-form-item>
    <el-button 
      type="text" 
      class="w-full text-red-600 mt-8"
      @click="logout"
    >
      退出登录
    </el-button>
  </el-form-item>
  </div>
</div>
</template>

<script>
import { useUserStore } from "../store/user";
import api from "../api/user";
import { ElUpload, ElButton, ElInput, ElForm, ElFormItem, ElSelect, ElOption, ElMessage } from "element-plus"; // 【修改】新增ElMessage导入
import axios from 'axios';


export default {
  components: {
    ElUpload, ElButton, ElInput, ElForm, ElFormItem, ElSelect, ElOption
  },
  data() {
    return {
      form: {
        username: "",
        email: "",
        phone: "",
        signature: "",
      },
      userInfo: {}
    };
  },
  
  computed: {
  avatarUrl() {
    const API_BASE_URL = 'http://localhost:8080';
    const DEFAULT_AVATAR = '/uploads/avatars/default-avatar.jpg';

    if (!this.userInfo?.avatar) {
      return API_BASE_URL + DEFAULT_AVATAR;
    }

    if (this.userInfo.avatar.startsWith('http')) {
      return this.userInfo.avatar;
    }

    return API_BASE_URL + this.userInfo.avatar;
  }
},

  setup() {
    const userStore = useUserStore();
   
    return {userStore};
  },
  async created() {
    try {
      // 1. 先从Store取数据（兜底），添加空值保护
      const storeUserInfo = this.userStore.userInfo || {};
      this.userInfo = storeUserInfo;

      // 2. 主动调用后端接口获取最新用户信息（核心：不依赖Store）
      const res = await api.getCurrentUser();
      this.userInfo = res.data || {}; // 接口返回数据覆盖Store数据
      console.log("用户信息：", this.userInfo);
      this.userStore.setUser(this.userInfo); // 更新Store，保持数据同步
      // 初始化表单：添加空值保护（可选链）
      this.form = {
        username: this.userInfo?.username || "",
        email: this.userInfo?.email || "",
        phone: this.userInfo?.phone || "",
        signature: this.userInfo?.signature || "",
      };

    } catch (error) {
      console.error("获取用户信息失败：", error);
      // 表单兜底初始化（避免页面空白）
      this.form = {
        username: "",
        email: "",
        phone: "",
        signature: "",
        avatar: ""
      };

      // token失效则跳回登录页
      if (error.response?.status === 401) {
        localStorage.removeItem("token");
        this.$router.push("/");
        ElMessage.error("登录已过期，请重新登录"); 
      }
    }
  },

  methods: {
    async customUpload(options) {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          ElMessage.error("请先登录"); 
          options.onError("未登录");
          return;
        }

        // 校验文件是否存在
        if (!options.file) {
          ElMessage.error("未选择有效文件");
          options.onError("文件为空");
          return;
        }

        // 兼容Element Plus文件对象
        const realFile = options.file.raw || options.file;
        const formData = new FormData();
        formData.append("file", realFile); 
        
        const response = await axios.post('http://localhost:8080/api/user/upload-avatar', formData, {
          headers: {
            'Authorization': `Bearer ${token}`, 
          },
          withCredentials: true 
        });

        console.log("头像上传成功", response);
        this.handleAvatarSuccess(response); 
        options.onSuccess(response); // 通知ElUpload上传成功
      } catch (error) {
        console.error("头像上传失败", error);
        ElMessage.error("头像上传失败：" + (error.message || "服务器错误")); // 【修改】替换为ElMessage
        options.onError(error);
      }
    },

    // 头像上传前校验方法
    beforeAvatarUpload(file) {
      const isJPG = file.type === "image/jpeg" || file.type === "image/png";
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isJPG) ElMessage.error("头像只能是 JPG/PNG 格式!"); 
      if (!isLt2M) ElMessage.error("头像大小不能超过 2MB!"); 
      return isJPG && isLt2M;
    },
    // 头像上传成功回调
    handleAvatarSuccess(res) {
      this.userInfo.avatar = res.data.avatarUrl; // 后端返回的头像URL
      this.userStore.setUser(this.userInfo); // 更新store
      ElMessage.success("头像上传成功!"); 
    },

    // 提交个人信息修改
    async submitProfile() {
      try {
        await api.updateUserInfo(this.form); // 调用后端更新接口
        // 更新store中的用户信息
        this.userStore.setUser({ ...this.userInfo, ...this.form });
        ElMessage.success("个人信息修改成功!"); 
      } catch (error) {
        ElMessage.error(error.response?.data?.message || "修改失败"); 
      }
    },
    // 退出登录
    logout() {
      localStorage.removeItem("token"); // 清除token
      this.userStore.resetUser(); // 重置store
      this.$router.push("/"); // 跳转到登录页
      ElMessage.success("已退出登录"); 
    }
  }
};
</script>

<style scoped>
.avatar-uploader {
  position: relative;
  cursor: pointer;
}
</style>