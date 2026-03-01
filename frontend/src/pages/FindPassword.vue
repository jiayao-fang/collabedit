<template>
  <div class="flex justify-center items-center h-screen">
    <div class="bg-white p-8 shadow-2xl rounded-2xl w-[420px]">
      <h2 class="text-3xl font-bold mb-6 text-center text-gray-800">找回密码</h2>
      <!-- 找回方式选择 -->
      <div class="flex gap-2 mb-4">
        <button 
          @click="resetType = 'phone'" 
          :class="resetType === 'phone' ? 'btn-tab-active' : 'btn-tab'"
          class="flex-1">
          手机号找回
        </button>
        <button 
          @click="resetType = 'email'" 
          :class="resetType === 'email' ? 'btn-tab-active' : 'btn-tab'"
          class="flex-1">
          邮箱找回
        </button>
      </div>

      <!-- 手机号找回 -->
      <div v-if="resetType === 'phone'">
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
      
      <!-- 邮箱找回 -->
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

      <input class="input" placeholder="新密码（6-20位）" type="password" v-model="form.newPassword" />
      <input class="input" placeholder="确认新密码" type="password" v-model="form.confirmPassword" />

      <button class="btn-primary mt-6 w-full" @click="resetPassword">
        重置密码
      </button>
      
      <div class="text-center mt-4">
        <router-link to="/login" class="text-blue-600 hover:text-blue-800 font-medium">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import api from "../api/user";

export default {
  data() {
    return {
      resetType: 'phone', // 默认手机号找回
      form: {
        email: "",
        phone: "",
        verifyCode: "",
        newPassword: "",
        confirmPassword: ""
      },
      countdown: 0, // 倒计时
      timer: null
    };
  },
  methods: {
    // 发送验证码
    async sendCode() {
      const receiver = this.resetType === 'phone' ? this.form.phone : this.form.email;
      
      // 校验
      if (!receiver) {
        alert(this.resetType === 'phone' ? '请输入手机号' : '请输入邮箱');
        return;
      }
      
      if (this.resetType === 'phone' && !/^1[3-9]\d{9}$/.test(receiver)) {
        alert('手机号格式不正确');
        return;
      }
      
      if (this.resetType === 'email' && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(receiver)) {
        alert('邮箱格式不正确');
        return;
      }
      
      try {
        await api.sendVerifyCode({
          receiver: receiver,
          type: this.resetType
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
    
    // 重置密码
    async resetPassword() {
      // 基础校验
      if (!this.form.verifyCode) {
        alert('请输入验证码');
        return;
      }
      
      if (!this.form.newPassword) {
        alert('请输入新密码');
        return;
      }
      
      if (this.form.newPassword.length < 6 || this.form.newPassword.length > 20) {
        alert('密码长度必须在6-20位之间');
        return;
      }
      
      if (this.form.newPassword !== this.form.confirmPassword) {
        alert('两次输入的密码不一致');
        return;
      }
      
      const receiver = this.resetType === 'phone' ? this.form.phone : this.form.email;
      if (!receiver) {
        alert(this.resetType === 'phone' ? '请输入手机号' : '请输入邮箱');
        return;
      }
      
      try {
        console.log('发送重置密码请求，参数：', this.form);
        const response = await api.resetPassword({
          receiver: receiver,
          verifyCode: this.form.verifyCode,
          newPassword: this.form.newPassword,
          resetType: this.resetType
        });
        console.log('重置密码成功，响应：', response.data);
        alert('密码重置成功，请登录');
        this.$router.push('/login');
      } catch (error) {
        console.error('重置密码错误详情：', error);
        alert(error.response?.data?.message || "重置密码失败");
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
  @apply w-full p-3 mt-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition;
}
.btn-primary {
  @apply bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition font-medium shadow-lg hover:shadow-xl;
}
.btn-tab {
  @apply py-2 px-4 rounded-lg border border-gray-300 text-gray-600 transition hover:border-blue-400;
}
.btn-tab-active {
  @apply py-2 px-4 rounded-lg bg-blue-600 text-white font-medium shadow-md;
}
.btn-code {
  @apply mt-3 px-4 py-3 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition font-medium whitespace-nowrap disabled:bg-gray-200 disabled:text-gray-500 disabled:cursor-not-allowed;
}
</style>
