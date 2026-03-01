<template>
    <div class="p-6">
        <div class="border-b pb-4 mb-6">
            <h2 class="text-xl font-bold mt-2">编辑文档</h2>
            <!-- 在线用户显示 -->
            <div class="flex items-center mt-2 text-sm text-gray-500">
                <span>在线编辑者：</span>
                <div class="flex -space-x-2 ml-2">
                    <div 
                        v-for="(user, index) in onlineUsers" 
                        :key="user.id || index"
                        :title="user.name"
                        class="w-6 h-6 rounded-full border-2 border-white flex items-center justify-center text-white text-xs"
                        :style="{ backgroundColor: user.color || '#3498db' }"
                    >
                        {{ (user.name || user.username || 'U').charAt(0) }}
                    </div>
                </div>
                <span class="ml-2">({{ onlineUsers.length }}人在线)</span>
            </div>
        </div>

        <el-form 
            ref="docFormRef" 
            :model="docForm" 
            label-width="80px"
            class="max-w-3xl mx-auto"
            :disabled="loading"
        >
            <!-- 文档标题 -->
            <el-form-item label="标题" prop="title">
                <el-input 
                    v-model="docForm.title" 
                    placeholder="请输入文档标题（必填）" 
                    class="text-lg"
                    maxlength="100"
                    show-word-limit
                    @input="handleTitleChange"
                />
            </el-form-item>

            <!-- 封面图上传 -->
            <el-form-item label="封面图">
                <el-upload
                    class="avatar-uploader"
                    action=""
                    :show-file-list="false"
                    :http-request="uploadCover"
                    :before-upload="beforeUpload"
                    :disabled="loading"
                >
                    <img 
                        v-if="docForm.coverImage" 
                        :src="docForm.coverImage" 
                        class="w-48 h-32 object-cover rounded border"
                    >
                    <div v-else class="w-48 h-32 bg-gray-100 flex items-center justify-center rounded border border-dashed">
                        <i class="el-icon-plus text-xl text-gray-400"></i>
                        <span class="ml-2 text-xs text-gray-400">点击上传封面</span>
                    </div>
                </el-upload>
                <div class="text-xs text-gray-500 mt-1">支持JPG/PNG格式，大小不超过2MB</div>
            </el-form-item>

            <el-form-item label="权限">
                <el-select 
                    v-model="docForm.visibility" 
                    placeholder="设置权限"
                    clearable  
                    @change="handlePermissionChange"
                >
                    <el-option label="仅自己可见" value="0"></el-option>
                    <el-option label="仅编辑者可见" value="1"></el-option>
                    <el-option label="所有人可见" value="2"></el-option>
                </el-select>
            </el-form-item>

            <!-- 文件夹选择（仅显示当前用户创建的） -->
            <el-form-item label="文件夹">
                <el-select 
                    v-model="docForm.folderId" 
                    placeholder="选择文件夹（可选）"
                    clearable  
                    @change="handleFolderChange"
                >
                    <el-option label="未分类" value=""></el-option>
                    <el-option 
                        v-for="folder in folders" 
                        :key="folder.id" 
                        :label="folder.folderName" 
                        :value="folder.id"
                    ></el-option>
                </el-select>
            </el-form-item>

            <!-- 标签选择（支持多选/取消） -->
            <el-form-item label="标签">
                <el-select 
                    v-model="docForm.tagIds" 
                    placeholder="选择标签（可选，可多选）" 
                    multiple
                    clearable  
                    collapse-tags  
                    @change="handleTagChange"
                >
                    <el-option 
                        v-for="tag in tags" 
                        :key="tag.id" 
                        :label="tag.name" 
                        :value="tag.id"
                    ></el-option>
                </el-select>
                <div class="text-xs text-gray-500 mt-1">可选择多个标签，点击已选标签可取消</div>
            </el-form-item>

            <!-- 文档内容 -->
            <el-form-item label="内容" prop="content">
                <div class="quill-wrapper" style="height: 400px; border: 1px solid #dcdfe6; border-radius: 4px; overflow: hidden;">
                    <QuillEditor
                        v-model:content="docForm.content"
                        contentType="html"
                        class="w-full h-full"
                        :options="editorOptions"
                        :style="{ height: '100%' }"
                        @ready="handleEditorReady"
                    />
                </div>
            </el-form-item>

            <!-- 提交按钮 -->
            <el-form-item>
                <el-button 
                    type="primary" 
                    @click="submitForm"
                    :loading="loading"
                    icon="el-icon-check"
                >
                    保存修改
                </el-button>
                <el-button 
                    type="default" 
                    @click="resetForm"
                    icon="el-icon-refresh"
                >
                    重置
                </el-button>
                <el-button 
                    type="text" 
                    @click="$router.go(-1)"
                    icon="el-icon-arrow-left"
                >
                    返回
                </el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import api from "../api/document";
import * as Y from 'yjs';
import { onMounted, ref, reactive, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
// 导入协作相关模块
import { initCollaboration, destroyCollaboration, syncDocContentToYjs } from '../utils/websocket';
import he from 'he';
// 导入Quill以便获取实例
import Quill from 'quill';
import QuillCursors from 'quill-cursors';

export default {
    components: { QuillEditor },
    setup() {
        // 表单引用
        const docFormRef = ref(null);
        // 加载状态
        const loading = ref(false);
        // 路由相关
        const route = useRoute();
        const router = useRouter();
        const docId = ref(route.params.id || '');
        // 编辑器与协作实例
        const quillInstance = ref(null);
        const collabInstance = ref(null);
        // 在线用户列表
        const onlineUsers = ref([]);
        // 防抖计时器
        const titleDebounceTimer = ref(null);
        const syncTimer = ref(null);
        // 编辑器就绪标记
        const editorReady = ref(false);

        // 注册Quill光标模块
        Quill.register('modules/cursors', QuillCursors);

        // 表单数据（含乐观锁版本号）
        const docForm = reactive({
            id: '',
            title: '',
            content: '',
            coverImage: '',
            folderId: '',
            tagIds: [],
            visibility: '0',
            version: null,
            edit_count: 0
        });

        // 文件夹与标签列表
        const folders = ref([]);
        const tags = ref([]);

        // 富文本编辑器配置
        const editorOptions = {
            placeholder: '请输入文档内容（必填）',
            modules: {
                toolbar: [
                    ['bold', 'italic', 'underline', 'strike'],
                    ['blockquote', 'code-block'],
                    [{ 'header': 1 }, { 'header': 2 }],
                    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
                    [{ 'align': [] }],
                    ['link', 'image'],
                    ['clean']
                ],
                cursors: true
            },
            theme: 'snow',
            bounds: document.body,
            style: {
                height: '100%'
            }
        };

        // 加载文件夹和标签数据
        const loadCategories = async () => {
            loading.value = true;
            try {
                const [foldersRes, tagsRes] = await Promise.all([
                    api.getAllFolders(),
                    api.getAllTags()
                ]);

                folders.value = (foldersRes.data || []).map(folder => ({
                    id: folder.id,
                    folderName: folder.creatorName ? `${folder.folderName} (${folder.creatorName})` : folder.folderName
                }));

                tags.value = (tagsRes.data || []).map(tag => ({
                    id: tag.id || '',
                    name: tag.tagName || '未命名标签'
                }));

                if (folders.value.length === 0) {
                    ElMessage.info('暂无已创建的文件夹，默认选择「未分类」');
                }
            } catch (error) {
                ElMessage.error('加载分类数据失败：' + (error.message || '服务器错误'));
                console.error('加载分类失败', error);
            } finally {
                loading.value = false;
            }
        };

        // 加载文档详情（含版本号）
        const loadDocDetail = async () => {
            if (!docId.value) {
                ElMessage.error('文档ID不存在！');
                router.go(-1);
                return;
            }

            loading.value = true;
            try {
                const res = await api.get(docId.value);
                const docData = res.data || {};

                // 回显文档数据
                docForm.id = docData.id || '';
                docForm.title = docData.title || '';
                const decodedContent = he.decode(docData.content || '');
                docForm.content = decodedContent;
                docForm.coverImage = docData.coverImage || '';
                docForm.folderId = docData.folderId || '';
                docForm.tagIds = docData.tagIds
                    ? (typeof docData.tagIds === 'string' ? docData.tagIds.split(',') : docData.tagIds)
                    : [];
                docForm.visibility = (docData.visibility || '0').toString();
                docForm.version = docData.version;
                docForm.edit_count = docData.edit_count || 0;
            } catch (error) {
                ElMessage.error('加载文档详情失败：' + (error.message || '服务器错误'));
                console.error('加载文档详情失败', error);
                router.go(-1);
            } finally {
                loading.value = false;
                // 编辑器就绪后初始化协作
                if (editorReady.value) {
                    initCollaborationAfterEditorReady();
                }
            }
        };

        // Uint8Array转Base64
        const uint8ArrayToBase64 = (uint8Array) => {
            try {
                return btoa(String.fromCharCode(...uint8Array));
            } catch (e) {
                let binary = '';
                const len = uint8Array.byteLength;
                for (let i = 0; i < len; i++) {
                    binary += String.fromCharCode(uint8Array[i]);
                }
                return btoa(binary);
            }
        };

        // 封面上传前校验
        const beforeUpload = (file) => {
            const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg';
            const isLt2M = file.size / 1024 / 1024 < 2;

            if (!isImage) {
                ElMessage.error('封面图仅支持 JPG/PNG 格式！');
                return false;
            }
            if (!isLt2M) {
                ElMessage.error('封面图大小不能超过 2MB！');
                return false;
            }
            return true;
        };

        // 封面上传处理
        const uploadCover = async (options) => {
            loading.value = true;
            try {
                const formData = new FormData();
                formData.append('file', options.file);
                const res = await api.uploadCover(formData);

                if (res.data && res.data.url) {
                    docForm.coverImage = res.data.url;
                    ElMessage.success('封面上传成功');
                    options.onSuccess();
                } else {
                    throw new Error('上传结果异常');
                }
            } catch (error) {
                ElMessage.error('封面上传失败：' + (error.message || '请重试'));
                options.onError();
            } finally {
                loading.value = false;
            }
        };

        // 文件夹变更回调
        const handleFolderChange = (val) => {
            console.log('选中文件夹：', val || '未分类');
            docForm.folderId = val || '';
        };

        // 标签变更回调
        const handleTagChange = (val) => {
            console.log('选中标签ID：', val);
            docForm.tagIds = val || [];
        };

        // 权限变更回调
        const handlePermissionChange = (val) => {
            console.log('权限变更为：', val);
        };

        // 标题变更防抖处理
        const handleTitleChange = (val) => {
            if (titleDebounceTimer.value) {
                clearTimeout(titleDebounceTimer.value);
            }
            titleDebounceTimer.value = setTimeout(() => {
                console.log('标题变更为：', val);
            }, 500);
        };

        // 重置表单
        const resetForm = () => {
            if (docFormRef.value) {
                docFormRef.value.resetFields();
            }
            loadDocDetail();
            ElMessage.info('表单已重置为原始数据');
        };

        // 提交表单（保存修改）
        const submitForm = async () => {
            // 获取最新编辑器内容
            if (quillInstance.value) {
                docForm.content = quillInstance.value.root.innerHTML;
            } else if (collabInstance.value?.ydoc) {
                const ytext = collabInstance.value.ydoc.getText('quill');
                docForm.content = ytext.toString();
            }

            // 前端校验
            if (!docForm.title.trim()) {
                ElMessage.warning('请输入文档标题！');
                return;
            }
            if (!docForm.content.replace(/<[^>]+>/g, '').trim()) {
                ElMessage.warning('请输入文档内容！');
                return;
            }
            if (!docForm.visibility) {
                ElMessage.warning('请选择文档可见范围！');
                return;
            }
            if (!docForm.id) {
                ElMessage.error('文档ID不存在，无法保存！');
                return;
            }
            if (docForm.version === null || docForm.version === undefined) {
                ElMessage.error('文档版本号异常，无法保存！');
                return;
            }

            loading.value = true;
            try {
                // 准备Yjs状态数据
                let contentState = '';
                if (collabInstance.value && collabInstance.value.ydoc) {
                    const update = Y.encodeStateAsUpdate(collabInstance.value.ydoc);
                    contentState = uint8ArrayToBase64(update);
                }

                // 格式化提交数据
                const submitData = {
                    ...docForm,
                    tagIds: docForm.tagIds.length > 0 ? docForm.tagIds.join(',') : '',
                    folderId: docForm.folderId ? Number(docForm.folderId) : null,
                    visibility: Number(docForm.visibility),
                    version: docForm.version,
                    coverImage: docForm.coverImage,
                    contentState: contentState,
                    content: docForm.content
                };

                console.log('提交的更新数据：', submitData);
                const response = await api.edit(docForm.id, submitData);
                console.log('API响应:', response);

                ElMessage.success('文档修改保存成功！');
                // 更新本地版本号（用于乐观锁）
                docForm.version += 1;
                
                // 自动创建版本快照
                try {
                    await api.createVersion(docForm.id, {
                        changeDescription: '自动保存快照'
                    });
                    console.log('版本快照创建成功');
                } catch (versionError) {
                    console.error('创建版本快照失败:', versionError);
                    // 快照创建失败不影响文档保存，只记录日志
                }
            } catch (error) {
                // 乐观锁冲突提示
                if (error.response?.data?.message?.includes('version') || 
                    error.response?.data?.message?.includes('已被修改')) {
                    ElMessage.error('文档已被其他人修改，请刷新后重试！');
                    // 重新加载文档获取最新版本
                    await loadDocDetail();
                } else {
                    const errMsg = error.response?.data?.message || error.message || '保存失败';
                    ElMessage.error(`文档保存失败：${errMsg}`);
                }
                console.error('保存失败', error);
            } finally {
                loading.value = false;
            }
        };

        // 编辑器就绪后初始化协作
        const initCollaborationAfterEditorReady = () => {
            const token = localStorage.getItem('token') || '';
            const username = localStorage.getItem('username') || '未知用户';    
            // 防止重复初始化
            if (!collabInstance.value && docId.value && quillInstance.value) {
                // 注册在线用户去重回调
                window.updateOnlineUsers = (users) => {
                    console.log('原始用户列表：', users);
                    const uniqueUsers = Array.from(new Map(users.map(user => [user.id, user])).values());
                    onlineUsers.value = uniqueUsers;
                };

                // 初始化协作
                collabInstance.value = initCollaboration(
                    docId.value,
                    quillInstance.value,
                    token,
                    username
                );

                // 同步文档内容到Yjs
                if (collabInstance.value?.ytext) {
                    syncDocContentToYjs(collabInstance.value.ytext, docForm.content);
                }
            }
        };

        // 清理协作资源
        const cleanupCollaboration = () => {
            // 清理全局回调
            if (window.updateOnlineUsers) {
                delete window.updateOnlineUsers;
            }

            // 销毁协作实例
            if (collabInstance.value) {
                destroyCollaboration(collabInstance.value);
                collabInstance.value = null;
            }

            // 清除防抖计时器
            if (titleDebounceTimer.value) {
                clearTimeout(titleDebounceTimer.value);
            }

            // 4. 清理Quill编辑器事件监听和实例
            if (quillInstance.value) {
                quillInstance.value.off('selection-change'); // 取消事件监听
                quillInstance.value = null;
            }
        };

        // 编辑器就绪回调
        const handleEditorReady = (editor) => {
            quillInstance.value = editor;
            editorReady.value = true;

            const token = localStorage.getItem('token') || '';
            const username = localStorage.getItem('username') || '未知用户';
            const userId = localStorage.getItem('userId') || '';

            // 初始化协作实例
            if (!collabInstance.value) {
                collabInstance.value = initCollaboration(
                    docId.value,
                    quillInstance.value,
                    token,
                    username
                );
            }

            if (collabInstance.value) {
                // 绑定在线用户更新回调
                window.updateOnlineUsers = (users) => {
                    console.log('收到在线用户列表：', users);
                    const uniqueUsers = Array.from(new Map(users.map(user => [user.id, user])).values());
                    onlineUsers.value = uniqueUsers;
                };

                const awareness = collabInstance.value.provider.awareness;

                try {
                    // 初始化光标模块
                    if (!Quill.imports['modules/cursors']) {
                        Quill.register('modules/cursors', QuillCursors);
                    }

                    let cursors = quillInstance.value.getModule('cursors');
                    if (!cursors) {
                        cursors = new QuillCursors(quillInstance.value, {
                            template: `
                                <span class="ql-cursor-selections"></span>
                                <span class="ql-cursor-caret-container">
                                    <span class="ql-cursor-caret"></span>
                                </span>
                                <div class="ql-cursor-flag">
                                    <span class="ql-cursor-name"></span>
                                    <span class="ql-cursor-flag-flap"></span>
                                </div>
                            `
                        });
                    }

                    // 监听用户状态变化更新光标
                    awareness.on('change', () => {
                        awareness.getStates().forEach((state, clientId) => {
                            if (state && state.user) {
                                console.log(`用户 ${state.user.name} 状态更新`);
                                cursors.createCursor(
                                    clientId,
                                    state.user.name,
                                    state.user.color
                                );

                                if (state.selection) {
                                    cursors.moveCursor(clientId, state.selection);
                                }
                            }
                        });

                        // 移除无效光标
                        const clientIds = Array.from(awareness.getStates().keys());
                        const currentCursors = cursors.cursors();
                        currentCursors.forEach(cursor => {
                            if (!clientIds.includes(parseInt(cursor.id))) {
                                cursors.removeCursor(cursor.id);
                            }
                        });
                    });

                    // 本地光标变化同步到Awareness
                    quillInstance.value.on('selection-change', (range, oldRange, source) => {
                        if (source === 'user') {
                            awareness.setLocalStateField('selection', range);
                        }
                    });

                    // 初始化触发一次用户列表更新
                    setTimeout(() => {
                        const states = awareness.getStates();
                        const users = [];
                        states.forEach((state, clientID) => {
                            if (state && state.user) {
                                users.push(state.user);
                            }
                        });
                        window.updateOnlineUsers(users);
                    }, 200);
                } catch (error) {
                    console.error('初始化光标模块失败:', error);
                }
            }

            // 初始化协作
            if (editorReady.value) {
                initCollaborationAfterEditorReady();
            }
        };

        // 全量清理资源（页面刷新/关闭）
        const cleanupAllResources = () => {
            cleanupCollaboration();
        };

        // 组件挂载时加载数据
        onMounted(async () => {
            await loadCategories();
            await loadDocDetail();
            // 新增：监听页面刷新/关闭事件，主动清理资源
            window.addEventListener('beforeunload', cleanupAllResources);
        });

        // 组件卸载前清理资源
        onBeforeUnmount(() => {
            // 1. 移除页面刷新监听
            window.removeEventListener('beforeunload', cleanupAllResources);
            cleanupCollaboration();
        });

        // 暴露模板所需属性和方法
        return {
            docFormRef,
            docForm,
            folders,
            tags,
            loading,
            editorOptions,
            onlineUsers,
            beforeUpload,
            uploadCover,
            handleFolderChange,
            handleTagChange,
            handlePermissionChange,
            handleTitleChange,
            resetForm,
            submitForm,
            handleEditorReady
        };
    }
};
</script>

<style scoped>
.avatar-uploader {
    display: inline-block;
    cursor: pointer;
}

/* 富文本编辑器样式覆盖 */
:deep(.ql-container) {
    height: calc(100% - 42px) !important;
    min-height: calc(100% - 42px) !important;
}

:deep(.ql-editor) {
    min-height: 100% !important;
    height: 100% !important;
    line-height: 1.5;
}

:deep(.ql-toolbar) {
    border-bottom: 1px solid #dcdfe6 !important;
}

/* 下拉选择框宽度 */
:deep(.el-select) {
    width: 100%;
}

/* 隐藏原始光标 */
:deep(.ql-cursor) {
    display: none !important;
}

/* 自定义光标样式 */
:deep(.ql-editor) {
    position: relative;
}

:deep(.ql-editor .ql-blank::after) {
    display: none !important;
}

/* 光标样式 */
:deep(.ql-cursor-caret) {
    display: none;
}

/* 自定义光标容器样式 */
:deep(.ql-cursor-container) {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    overflow: visible;
    z-index: 1000;
}

/* 自定义光标样式 */
:deep(.ql-user-cursor) {
    position: absolute;
    height: 1.2em;
    top: 0;
    z-index: 1001;
}

:deep(.ql-user-cursor .ql-cursor-caret-container) {
    position: absolute;
    top: 0;
    left: 0;
    width: 2px;
    height: 1.2em;
    z-index: 1001;
}

:deep(.ql-user-cursor .ql-cursor-caret) {
    position: absolute;
    top: 0;
    left: -1px;
    width: 4px;
    height: 1.2em;
    background-color: currentColor;
    opacity: 0.8;
}

:deep(.ql-user-cursor .ql-cursor-flag) {
    position: absolute;
    top: -1.5em;
    left: 0;
    background-color: currentColor;
    color: white;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 12px;
    white-space: nowrap;
    z-index: 1002;
    opacity: 0.9;
}

:deep(.ql-user-cursor .ql-cursor-flag-flap) {
    position: absolute;
    top: 100%;
    left: 0;
    width: 0;
    height: 0;
    border-top: 5px solid currentColor;
    border-left: 5px solid transparent;
    border-right: 5px solid transparent;
}

:deep(.ql-cursor-selections) {
    position: absolute;
    background-color: rgba(0, 127, 255, 0.2);
    z-index: 1000;
}
</style>