import http from "./http";

function sendVerifyCode(data) {
  return http.post("user/send-verify-code", data);
}

function register(data) {
  return http.post("user/register", data);
}

function login(data) {
  return http.post("user/login", data);
}

function resetPassword(data) {
  return http.post("user/reset-password", data);
}

function getCurrentUser() {
  return http.get("/user/current"); 
}

function updateUserInfo(data) {
  return http.put("/user/update-info", data);
}

function getUsersByIds(userIds) {
  return http.post("/user/batch", userIds);
}

function getUsernameById(userId) {
  return http.post(`/user/getName/${userId}`, userId);
}

function getAvatarById(userId) {
  return http.post(`/user/getAvatar/${userId}`, userId);
}

function getAllUsers() {
  return http.get("/user/all");
}

function getUserInfo(userId) {
  return http.get(`/user/${userId}`);
}


// 头像上传需用form-data格式
function uploadAvatar(file) {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/user/upload-avatar", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
}

function getUserInfoById(userId) {
  return http.get(`/user/info/${userId}`);
}

// 管理员相关接口
function getUsersByPage(page, size, roleId, status) {
  let params = { page, size };
  if (roleId !== undefined) params.roleId = roleId;
  if (status !== undefined) params.status = status;
  return http.get("/admin/users", { params });
}

function updateUserStatus(userId, status) {
  return http.put("/admin/user/status", { userId, status });
}

function getUserBehavior() {
  return http.get("/admin/user/behavior");
}

export default {
  sendVerifyCode,
  register,
  login,
  resetPassword,
  updateUserInfo,
  uploadAvatar,
  getCurrentUser,
  getUsersByIds,
  getAllUsers,
  getUserInfo,
  getUsernameById,
  getAvatarById,
  getUserInfoById,
  // 管理员相关接口
  getUsersByPage,
  updateUserStatus,
  getUserBehavior
};