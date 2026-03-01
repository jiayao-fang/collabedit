import { createRouter, createWebHistory } from "vue-router";
import Login from "../pages/Login.vue";
import Register from "../pages/Register.vue";
import FindPassword from "../pages/FindPassword.vue";
import DocumentEdit from "../pages/DocumentEdit.vue";
import MainLayout from '../pages/MainLayout.vue';
import PersonalInfo from '../pages/PersonalInfo.vue';
import DocumentSearch from "../pages/DocumentSearch.vue";
import DocumentPublish from "../pages/DocumentPublish.vue";
import DocumentAll from '../pages/DocumentAll.vue';
import DocumentRecycle from '../pages/DocumentRecycle.vue';
import CategoryByFolder from '../pages/CategoryByFolder.vue'; 
import CategoryByTag from '../pages/CategoryByTag.vue'; 
import DocumentRead from '../pages/DocumentRead.vue'; 
import FolderSearch from "../pages/FolderSearch.vue";
import TagSearch from "../pages/TagSearch.vue";
import Notification from "../pages/Notification.vue";
import Contacts from "../pages/Contacts.vue";
import AdminUserManagement from '../pages/AdminUserManagement.vue';
import DocumentVersionHistory from '../pages/DocumentVersionHistory.vue';
//import ContactDetail from "../pages/ContactDetail.vue";
const routes = [
  { path: "/", redirect: "/login" },
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/find-password", component: FindPassword },
  
  { 
    path: "/main", 
    component: MainLayout,
    children: [
      { path: "/", redirect: "all"},
      { path: "all", name: "AllDocuments", component: DocumentAll },
      { path: "recycle", name: "RecycleDocuments", component: DocumentRecycle },
      { path: "categoryByFolder", name: "CategoryByFolder", component: CategoryByFolder },
      { path: "categoryByTag", name: "CategoryByTag", component: CategoryByTag }
    ]
  },
  { path: "/personal", component: PersonalInfo },
  { path: "/documents/search", name: "DocumentSearch", component: DocumentSearch },
  { path: "/documents/publish", name: "DocumentPublish", component: DocumentPublish },
  { path: "/documents/edit/:id", name: "DocumentEdit", component: DocumentEdit },
  { path: "/documents/:id", component: DocumentRead },
  { path: "/documents/:id/version-history", name: "DocumentVersionHistory", component: DocumentVersionHistory },
  {path:"/folder/search",name:"FolderSearch",component:FolderSearch},
  {path:"/tag/search",name:"tagSearch",component:TagSearch},
  { path: "/contacts", component: Contacts, meta: { requiresAuth: true }}, 
  { path: "/notifications", component:Notification },
  { path: "/admin/users", component: AdminUserManagement, meta: { requiresAuth: true, requiresAdmin: true }}
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const isLogin = localStorage.getItem('token');
  const publicPages = ['/login', '/register', '/find-password'];
  const isPublic = publicPages.includes(to.path);

  if (!isPublic && !isLogin) {
    next('/login');
  } //else if (to.meta.requiresAdmin && !isUserAdmin()) {
    // 如果需要管理员权限但用户不是管理员，重定向到主页
   // next('/main/all');
  //}
   else {
    next();
  }
});

// 简单的检查用户是否为管理员的方法（实际项目中可能需要更复杂的逻辑）
function isUserAdmin() {
  // 这里可以检查用户角色信息，比如从token或存储中获取
  const userInfo = localStorage.getItem('userInfo');
  if (userInfo) {
    try {
      const parsed = JSON.parse(userInfo);
      // 假设用户角色信息存储在role字段中
      return parsed.role === 'admin' || parsed.roleName === 'admin' || parsed.authorities?.includes('ROLE_ADMIN');
    } catch (e) {
      return false;
    }
  }
  return false;
}

export default router;