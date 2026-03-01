<template>
  <div class="flex justify-center items-center h-screen">
    <div class="bg-white p-8 shadow-2xl rounded-2xl w-[420px]">
      <h2 class="text-3xl font-bold mb-6 text-center text-gray-800">注册账号</h2>

      <!-- 注册方式选择 -->
      <div class="flex gap-2 mb-4">
        <button 
          @click="registerType = 'phone'" 
          :class="registerType === 'phone' ? 'btn-tab-active' : 'btn-tab'"
          class="flex-1">
          手机号注册
        </button>
        <button 
          @click="registerType = 'email'" 
          :class="registerType === 'email' ? 'btn-tab-active' : 'btn-tab'"
          class="flex-1">
          邮箱注册
        </button>
      </div>

      <input class="input" placeholder="用户名" v-model="form.username" />
      
      <!-- 手机号注册 -->
      <div v-if="registerType === 'phone'">
        <input class="input" placeholder="手机号" v-model="form.phone" maxlength="11" />
        <div class="flex gap-2">
          <input class="input flex-1" placeholder="验证码" v-model="form.verifyCode" maxlength="6" />
          <button 
            @click="sendCode" 
            :disabled="countdown > 0"
            class="btn-code">
            {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
          </button>
        </div>
      </div>
      
      <!-- 邮箱注册 -->
      <div v-else>
        <input class="input" placeholder="邮箱" v-model="form.email" />
        <div class="flex gap-2">
          <input class="input flex-1" placeholder="验证码" v-model="form.verifyCode" maxlength="6" />
          <button 
            @click="sendCode" 
            :disabled="countdown > 0"
            class="btn-code">
            {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
          </button>
        </div>
      </div>

      <input class="input" placeholder="密码（6-20位）" type="password" v-model="form.password" />

      <button class="btn-primary mt-6 w-full" @click="register">
        注册
      </button>
      
      <div class="text-center mt-4">
        <span class="text-gray-600">已有账号？</span>
        <router-link to="/login" class="text-indigo-600 hover:text-indigo-800 font-medium">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import api from "../api/user";
export default {
  data() {
    return {
      registerType: 'phone', // 默认手机号注册
      form: {
        username: "",
        email: "",
        phone: "",
        password: "",
        verifyCode: ""
      },
      countdown: 0, // 倒计时
      timer: null
    };
  },
  methods: {
    // 发送验证码
    async sendCode() {
      const receiver = this.registerType === 'phone' ? this.form.phone : this.form.email;
      
      // 校验
      if (!receiver) {
        alert(this.registerType === 'phone' ? '请输入手机号' : '请输入邮箱');
        return;
      }
      
      if (this.registerType === 'phone' && !/^1[3-9]\d{9}$/.test(receiver)) {
        alert('手机号格式不正确');
        return;
      }
      
      if (this.registerType === 'email' && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(receiver)) {
        alert('邮箱格式不正确');
        return;
      }
      
      try {
        await api.sendVerifyCode({
          receiver: receiver,
          type: this.registerType
        });
        alert('验证码已发送，请注意查收');
        
        // 开始倒计时
        this.countdown = 60;
        this.timer = setInterval(() => {
          this.countdown--;
          if (this.countdown <= 0) {
            clearInterval(this.timer);
          }
        }, 1000);
      } catch (error) {
        console.error('发送验证码失败：', error);
        alert(error.response?.data?.message || '发送验证码失败');
      }
    },
    
    // 注册
    async register() {
      // 基础校验
      if (!this.form.username || !this.form.password) {
        alert('用户名和密码不能为空');
        return;
      }
      
      if (this.form.password.length < 6 || this.form.password.length > 20) {
        alert('密码长度必须在6-20位之间');
        return;
      }
      
      if (!this.form.verifyCode) {
        alert('请输入验证码');
        return;
      }
      
      const receiver = this.registerType === 'phone' ? this.form.phone : this.form.email;
      if (!receiver) {
        alert(this.registerType === 'phone' ? '请输入手机号' : '请输入邮箱');
        return;
      }
      
      try {
        console.log('发送注册请求，参数：', this.form);
        const response = await api.register({
          username: this.form.username,
          password: this.form.password,
          email: this.registerType === 'email' ? this.form.email : '',
          phone: this.registerType === 'phone' ? this.form.phone : '',
          verifyCode: this.form.verifyCode,
          registerType: this.registerType
        });
        console.log('注册成功，响应：', response.data);
        alert('注册成功，请登录');
        this.$router.push('/login');
      } catch (error) {
        console.error('注册错误详情：', error);
        alert(error.response?.data?.message || "注册失败");
      }
    }
  },
  beforeUnmount() {
    // 清理定时器
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
};
</script>
<style scoped>
.input {
  @apply w-full p-3 mt-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition;
}
.btn-primary {
  @apply bg-indigo-600 text-white py-3 rounded-lg hover:bg-indigo-700 transition font-medium shadow-lg hover:shadow-xl;
}
.btn-tab {
  @apply py-2 px-4 rounded-lg border border-gray-300 text-gray-600 transition hover:border-indigo-400;
}
.btn-tab-active {
  @apply py-2 px-4 rounded-lg bg-indigo-600 text-white font-medium shadow-md;
}
.btn-code {
  @apply mt-3 px-4 py-3 bg-indigo-100 text-indigo-700 rounded-lg hover:bg-indigo-200 transition font-medium whitespace-nowrap disabled:bg-gray-200 disabled:text-gray-500 disabled:cursor-not-allowed;
}
</style>
