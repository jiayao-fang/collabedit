import axios from "axios";
import { ElMessage } from "element-plus";

const http = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 15000
});

// 请求拦截器：携带Token
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      // 格式必须是「Bearer + 空格 + token」
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

http.interceptors.response.use(
  response => response,
  error => {
    const msg =
      error.response?.data?.message ||
      error.message ||
      '请求失败'

    ElMessage.error(msg)

    return Promise.reject(error)
  }
);
export default http;
