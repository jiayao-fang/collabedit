// src/api/document.js（更新后）
import http from "./http";

export default {
  // ===================== 原有文档基础操作 =====================
  create(data) {
    return http.post(`doc/create`, data);
  },
  edit(docId, data) {
    return http.post(`doc/edit/${docId}`, data);
  },
  delete(id) {
    return http.delete(`doc/delete/${id}`);
  },
  deletePer(id) {
    return http.delete(`doc/delPer/${id}`);
  },
  get(id) {
    return http.get(`doc/${id}`);
  },
  read(docId){
    return http.get(`doc/read/${docId}`);
  },
  getAll() {
    return http.get(`doc/all`);
  },

  // ===================== 文档权限与编辑者管理 =====================
  updateVisibility(docId, visibility) {
    return http.put(`doc/visibility/${docId}`, null, {
      params: { visibility }
    });
  },
  addEditor(docId, userId) {
    return http.post(`doc/addEditor/${docId}`, null, {
      params: { userId }
    });
  },
  removeEditor(docId, editorId) {
    return http.delete(`doc/delEditor/${docId}`, {
      params: { editorId }
    });
  },
  getEditors(docId) {
    return http.get(`doc/editors/${docId}`);
  },

   getInvitableContacts(docId) {
    return http.get(`contact/invitable/${docId}`);
  },

  // ===================== 回收站相关 =====================
  getRecycle() {
    return http.get(`doc/recycle`);
  },
  recover(docId) {
    return http.put(`doc/recover/${docId}`);
  },

  // ===================== 分类与搜索 =====================
  categoryByTag(tagId) {
    return http.get(`doc/categoryByTag`, {
      params: { tagId }
    });
  },
  categoryByFolder(folderId) {
    return http.get(`doc/categoryByFolder`, {
      params: { folderId }
    });
  },
  search(keyword) {
    return http.get(`doc/search`, {
      params: { keyword }
    });
  },
  folderSearch(keyword) {
    return http.get(`doc/folderSearch`, {
      params: { keyword }
    });
  },
  tagSearch(keyword) {
    return http.get(`tags/tagSearch`, {
      params: { keyword }
    });
  },
  advancedSearch(params) {
    return http.get(`doc/advancedSearch`, {params});
  },

  // ===================== 文件夹操作 =====================
  createFolder(data) {
    return http.post(`doc/createFolder`, data);
  },
  getFolders(parentId) {
    return http.get(`doc/folders`, {
      params: { parentId }
    });
  },
  getAllFolders() {
    return http.get(`doc/allFolders`);
  },
  getFolderDocCount(folderId) {
    return http.get(`/doc/${folderId}/doc-count`);
  },
  getTagDocCount(tagId){
    return http.get(`/tags/${tagId}/doc-count`);
  },
  getAllTags() {
    return http.get(`/tags`);
  },
  createTag(data){
    return http.post(`/tags/createTag`,data)
  },
  deleteFolder(id) {
    return http.delete(`doc/delFolder/${id}`);
  },

  // ===================== 辅助接口 =====================
  getAuthorName(authorId) {
    return http.get(`/doc/author/${authorId}`);
  },
  getFolderName(folderId) {
    return http.get(`/doc/folder/${folderId}`);
  },
  getTags(tagIds) {
    return http.get(`/doc/tags/${tagIds}`);
  },
  uploadCover(formData) {
    return http.post(`doc/upload-cover`, formData, {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    });
  },

  // ===================== 评论/批注相关接口 =====================
  /**
   * 添加评论/批注
   * @param {Object} data - 评论参数
   * @param {number} data.docId - 文档ID
   * @param {string} data.content - 评论内容（支持@用户）
   * @param {Object} data.position - 批注位置（JSON格式，如{line:5, column:3}）
   * @returns {Promise}
   */
  addComment(data) {
    return http.post(`collab/comment`, data);
  },

  /**
   * 回复评论
   * @param {number} parentId - 父评论ID
   * @param {Object} data - 回复内容
   * @param {string} data.content - 回复内容
   * @param {Object} [data.position] - 批注位置（可选）
   * @returns {Promise}
   */
  replyComment(parentId, data) {
    return http.post(`collab/comment/reply`, data, {
      params: { parentId }
    });
  },

  /**
   * 获取文档的所有评论
   * @param {number} docId - 文档ID
   * @returns {Promise}
   */
  getCommentsByDocId(docId) {
    return http.get(`collab/comment/doc/${docId}`);
  },

  /**
   * 获取某条评论的所有回复
   * @param {number} parentId - 父评论ID
   * @returns {Promise}
   */
  getCommentReplies(parentId) {
    return http.get(`collab/comment/replies/${parentId}`);
  },

  /**
   * 删除评论/回复
   * @param {number} commentId - 评论ID
   * @returns {Promise}
   */
  deleteComment(commentId) {
    return http.delete(`collab/comment/${commentId}`);
  },

  // ===================== 通知相关接口 =====================
  /**
   * 获取当前用户的通知列表
   * @param {Object} params - 查询参数
   * @param {number} [params.isRead] - 0-未读，1-已读，不传查全部
   * @param {number} [params.pageNum=1] - 页码
   * @param {number} [params.pageSize=10] - 每页条数
   * @returns {Promise}
   */
  getNotifications(params = {}) {
    const { isRead, pageNum = 1, pageSize = 10 } = params;
    return http.get(`notification`, {
      params: { isRead, pageNum, pageSize }
    });
  },

  /**
   * 标记单条通知为已读
   * @param {number} notificationId - 通知ID
   * @returns {Promise}
   */
  markNotificationAsRead(notificationId) {
    return http.put(`notification/${notificationId}/read`);
  },

  /**
   * 批量标记通知为已读
   * @param {number[]} notificationIds - 通知ID数组
   * @returns {Promise}
   */
  batchMarkNotificationAsRead(notificationIds) {
    return http.put(`notification/batch/read`, notificationIds);
  },

  /**
   * 获取当前用户未读通知数量
   * @returns {Promise}
   */
  getUnreadNotificationCount() {
    return http.get(`notification/unread/count`);
  },

  /**
   * 删除通知
   * @param {number} notificationId - 通知ID
   * @returns {Promise}
   */
  deleteNotification(notificationId) {
    return http.delete(`notification/${notificationId}`);
  },

  // ===================== 版本控制相关接口 =====================
  /**
   * 创建文档版本快照
   * @param {number} docId - 文档ID
   * @param {Object} data - 版本信息
   * @param {string} [data.changeDescription] - 变更描述
   * @returns {Promise}
   */
  createVersion(docId, data = {}) {
    return http.post(`doc/version/create/${docId}`, data);
  },

  /**
   * 获取文档版本历史
   * @param {number} docId - 文档ID
   * @returns {Promise}
   */
  getVersionHistory(docId) {
    return http.get(`doc/version/history/${docId}`);
  },

  /**
   * 获取指定版本详情
   * @param {number} versionId - 版本ID
   * @returns {Promise}
   */
  getVersionById(versionId) {
    return http.get(`doc/version/${versionId}`);
  },

  /**
   * 回滚到指定版本
   * @param {number} docId - 文档ID
   * @param {number} versionId - 版本ID
   * @returns {Promise}
   */
  rollbackToVersion(docId, versionId) {
    return http.post(`doc/version/rollback/${docId}/${versionId}`);
  },

  /**
   * 删除指定版本
   * @param {number} versionId - 版本ID
   * @returns {Promise}
   */
  deleteVersion(versionId) {
    return http.delete(`doc/version/${versionId}`);
  },

  /**
   * 比较两个版本的差异
   * @param {number} versionId1 - 版本1 ID
   * @param {number} versionId2 - 版本2 ID
   * @returns {Promise}
   */
  compareVersions(versionId1, versionId2) {
    return http.get(`doc/version/compare/${versionId1}/${versionId2}`);
  },

  /**
   * 锁定文档
   * @param {number} docId - 文档ID
   * @returns {Promise}
   */
  lockDocument(docId) {
    return http.post(`doc/lock/${docId}`);
  },

  /**
   * 解锁文档
   * @param {number} docId - 文档ID
   * @returns {Promise}
   */
  unlockDocument(docId) {
    return http.post(`doc/unlock/${docId}`);
  },

  /**
   * 检查文档锁定状态
   * @param {number} docId - 文档ID
   * @returns {Promise}
   */
  getDocumentLockStatus(docId) {
    return http.get(`doc/lock-status/${docId}`);
  }
};