import http from "./http";

export default {
  /**
   * 创建任务
   * @param {Object} data - 任务数据
   * @param {number} [data.docId] - 关联文档ID（可选）
   * @param {string} data.title - 任务标题
   * @param {string} [data.content] - 任务描述
   * @param {number[]} data.assigneeIds - 被分配人ID列表
   * @param {string} data.deadline - 截止日期（格式：yyyy-MM-dd HH:mm:ss）
   * @returns {Promise}
   */
  createTask(data) {
    return http.post(`/task`, data);
  },

  /**
   * 分配任务（添加接收者）
   * @param {number} taskId - 任务ID
   * @param {number[]} assigneeIds - 被分配人ID列表
   * @returns {Promise}
   */
  assignTask(taskId, assigneeIds) {
    return http.put(`/task/assign/${taskId}`, assigneeIds);
  },

  /**
   * 更新任务状态
   * @param {number} taskId - 任务ID
   * @param {string} status - 任务状态：PENDING, IN_PROGRESS, COMPLETED
   * @param {number} [assigneeId] - 可选：指定要更新状态的接收者ID（只有创建者可以指定）
   * @returns {Promise}
   */
  updateTaskStatus(taskId, status, assigneeId = null) {
    const data = { status };
    if (assigneeId !== null) {
      data.assigneeId = assigneeId;
    }
    return http.put(`/task/status/${taskId}`, data);
  },

  /**
   * 查询个人任务
   * @param {Object} [params] - 查询参数
   * @param {string} [params.status] - 任务状态筛选
   * @param {string} [params.deadline] - 截止日期筛选（格式：yyyy-MM-dd HH:mm:ss）
   * @returns {Promise}
   */
  getMyTasks(params = {}) {
    return http.get(`/task/my`, { params });
  },

  /**
   * 查询文档关联的任务
   * @param {number} docId - 文档ID
   * @param {Object} [params] - 查询参数
   * @param {string} [params.status] - 任务状态筛选
   * @param {string} [params.deadline] - 截止日期筛选
   * @returns {Promise}
   */
  getTasksByDocId(docId, params = {}) {
    return http.get(`/task/doc/${docId}`, { params });
  },

  /**
   * 根据ID查询任务
   * @param {number} taskId - 任务ID
   * @returns {Promise}
   */
  getTaskById(taskId) {
    return http.get(`/task/${taskId}`);
  }
};

