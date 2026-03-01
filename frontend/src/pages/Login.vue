<template>
  <div class="flex justify-center items-center h-screen">
    <div class="bg-white p-8 shadow-lg rounded-xl w-96">
      <h2 class="text-2xl font-bold mb-4 text-center">登录</h2>

      <input class="input" placeholder="用户名" v-model="form.username" />
      <input class="input" placeholder="密码" type="password" v-model="form.password" />

      <button class="btn-primary mt-4 w-full" @click="login">登录</button>

      <button class="text-blue-600 mt-3 mr-2" @click="$router.push('/register')">
        注册账号
      </button>

      <button class="text-blue-600 mt-3" @click="$router.push('/find-password')">
        忘记密码？
      </button>
      
    </div>
  </div>
</template>

<script>
import api from "../api/user";
import { useUserStore } from "../store/user";
import { ElMessage } from "element-plus";

export default {
  data() {
    return {
      form: { username: "", password: "" }
    };
  },
  setup() {
    const store = useUserStore();
    return { store };
  },
  methods: {
    async login() {
      // 基础校验
      if (!this.form.username || !this.form.password) {
        alert('用户名和密码不能为空'); 
        return;
      }
      try{
        const res = await api.login(this.form);
        // 保存用户信息到Pinia仓库
        this.store.setUser(res.data.userInfo);
        // 保存token到本地存储,方便请求拦截器使用
        localStorage.setItem('token', res.data.token);
        // 保存用户ID到本地存储，用于协作编辑
        localStorage.setItem('userId', res.data.userInfo.id);
        // 保存用户名到本地存储
        localStorage.setItem('username', res.data.userInfo.username);
        
        // 检查用户角色并跳转到相应页面
        const userRole = res.data.userInfo.role;
        const isAdmin = userRole && (userRole === 'admin')
          if (isAdmin) {
          // 管理员跳转到用户管理页面
          this.$router.push("/admin/users");
        } else {
          // 普通用户跳转到主页面
          this.$router.push("/main/all");
        }
        
        ElMessage.success('登录成功');
      } catch (error) {
      }

    }
  }
};
</script>

<style scoped>
.input {
  @apply w-full p-2 mt-3 border rounded-lg focus:ring-2 focus:ring-blue-400;
}
.btn-primary {
  @apply bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition;
}
</style>