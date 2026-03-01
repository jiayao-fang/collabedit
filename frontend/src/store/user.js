import { defineStore } from "pinia";

export const useUserStore = defineStore("user", {
  state: () => ({
    userInfo: {} 
  }),
  actions: {
    setUser(userInfo) {
      this.userInfo = userInfo || {};
    },
   resetUser() {
      this.userInfo = {}; // 重置用户信息
    }
  }
});
