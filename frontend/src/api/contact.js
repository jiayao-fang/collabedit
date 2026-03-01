import http from "./http";

export default {
  // 获取联系人列表
  getContacts() {
    return http.get(`/contact/list`);
  },
  
  // 添加联系人
  addContact(receiverId) {
    return http.post(`/contact/request/${receiverId}`);
  },
  
  // 删除联系人
  deleteContact(contactId) {
    return http.delete(`/contact/${contactId}`);
  },
  
  // 更新联系人备注
  updateContactRemark(contactId, remark) {
    return http.put(`/contact/${contactId}/remark`, { remark });
  },
  
  // 搜索用户
  searchContacts(keyword) {
    return http.get(`/contact/searchUser`, {
      params: { keyword }
    });
  },

   // 获取待处理的联系人请求
  getPendingRequests() {
    return http.get(`/contact/pending-requests`);
  },
  
  // 处理联系人请求
  handleContactRequest(requestId, status) {
    return http.post(`/contact/handle-request/${requestId}`, null, {
      params: { status }
    });
  }
};