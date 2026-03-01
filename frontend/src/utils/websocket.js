import * as Y from 'yjs';
import { WebsocketProvider } from 'y-websocket';
import { QuillBinding } from 'y-quill';
import { ElMessage } from 'element-plus';

// Uint8Array转Base64
export const uint8ArrayToBase64 = (uint8Array) => {
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

// Base64转Uint8Array（用于反向解析）
export const base64ToUint8Array = (base64) => {
    const binary = atob(base64);
    const uint8Array = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        uint8Array[i] = binary.charCodeAt(i);
    }
    return uint8Array;
};

// 生成随机用户颜色
export function getRandomColor() {
    const colors = [
        '#3498db', '#e74c3c', '#2ecc71', '#f39c12',
        '#9b59b6', '#1abc9c', '#e67e22', '#34495e'
    ];
    return colors[Math.floor(Math.random() * colors.length)];
}

// 同步Yjs状态到数据库
export const syncYjsToDatabase = async (docId, contentState, htmlContent) => {
    try {
        const token = localStorage.getItem('token');
        if (!token || !docId) {
            console.error('未找到用户token或文档ID');
            return;
        }

        const response = await fetch(`http://localhost:8080/api/doc/update-yjs-state/${docId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                contentState: contentState,
                content: htmlContent
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP错误：${response.status}`);
        }

         console.log(`文档${docId} Yjs状态同步成功`);

          // 同步成功后检查内容是否发生变化，如果变化则触发内容刷新
        const lastContent = sessionStorage.getItem(`lastDocContent_${docId}`);
        if (lastContent !== htmlContent) {
            // 保存当前内容作为最后内容
            sessionStorage.setItem(`lastDocContent_${docId}`, htmlContent);
            
           // 触发内容框刷新事件
            setTimeout(() => {
                window.dispatchEvent(new CustomEvent('contentUpdated', { 
                    detail: { docId, content: htmlContent } 
                }));
            }, 100);
        }
    } catch (error) {
        console.error('同步Yjs状态到数据库失败:', error.message);
    }
};

// 初始化实时协作
export const initCollaboration = (docId, quill, token, username) => {
    if (!quill || !docId) {
        ElMessage.error('编辑器实例或文档ID不存在！');
        return null;
    }

    // 初始化Yjs文档
    const ydoc = new Y.Doc();
    const wsUrl = `ws://localhost:1234/yjs-ws`; // 匹配服务端WS路径

    // 获取本地用户信息
    const userId = localStorage.getItem('userId') || 'unknown';
    username = username || localStorage.getItem('username') || `用户_${Math.floor(Math.random() * 1000)}`;
    const color = getRandomColor();

    // 初始化WebSocket提供者（优化参数）
    const provider = new WebsocketProvider(
        wsUrl,
        docId,
        ydoc,
        {
            params: {
                token: token,
                docId: docId,
                username: username,
                color: color
            },
            connect: true,
            maxBackoffTime: 2500,
            minBackoffTime: 250,
            resyncInterval: 5000 // 自动重同步间隔
        }
    );

    // 绑定Quill编辑器与Yjs文本
    const ytext = ydoc.getText('quill');
    const binding = new QuillBinding(ytext, quill, provider.awareness);

    // 实时同步到数据库（4秒防抖）
    let syncToDatabaseTimer = null;
    const SYNC_INTERVAL = 4000;

     // 记录上次内容用于变化检测
    let lastContent = quill.root.innerHTML;
    // 监听Yjs文档变化（确保所有修改都触发同步）
    const ytextObserver = () => {
        if (syncToDatabaseTimer) {
            clearTimeout(syncToDatabaseTimer);
        }

        syncToDatabaseTimer = setTimeout(async () => {
            try {
                const update = Y.encodeStateAsUpdate(ydoc);
                const contentState = uint8ArrayToBase64(update);
                const htmlContent = quill.root.innerHTML;

                // 检测内容是否发生变化
                if (htmlContent !== lastContent) {
                    console.log('检测到内容变化，准备同步...');
                    
                    // 同步到数据库
                    await syncYjsToDatabase(docId, contentState, htmlContent);
                    
                    // 更新最后内容
                    lastContent = htmlContent;
                }
            } catch (error) {
                console.error('同步Yjs状态到数据库失败:', error);
            }
        }, SYNC_INTERVAL);
    };
    ytext.observe(ytextObserver);

     // 监听Quill内容变化，用于更及时的内容变化检测
    quill.on('text-change', () => {
        if (syncToDatabaseTimer) {
            clearTimeout(syncToDatabaseTimer);
        }

        syncToDatabaseTimer = setTimeout(async () => {
            try {
                const update = Y.encodeStateAsUpdate(ydoc);
                const contentState = uint8ArrayToBase64(update);
                const htmlContent = quill.root.innerHTML;

                // 检测内容是否发生变化
                if (htmlContent !== lastContent) {
                    console.log('检测到内容变化，准备同步...');
                    
                    // 同步到数据库
                    await syncYjsToDatabase(docId, contentState, htmlContent);
                    
                    // 更新最后内容
                    lastContent = htmlContent;
                }
            } catch (error) {
                console.error('同步Yjs状态到数据库失败:', error);
            }
        }, SYNC_INTERVAL);
    });

     // 监听Yjs文档的update事件，这会在接收到其他用户更新时触发
    ydoc.on('update', (update, origin, doc) => {
        // 确保不是本地更新导致的事件
        if (origin !== provider) {
            // 延迟执行以确保内容已经更新到Quill
            setTimeout(() => {
                const currentContent = quill.root.innerHTML;
                // 触发内容框刷新事件
                window.dispatchEvent(new CustomEvent('contentUpdated', { 
                    detail: { docId, content: currentContent } 
                }));
            }, 100);
        }
    });

    // 监听连接状态变化
    provider.on('status', (event) => {
        console.log('协作连接状态变更：', event.status);
        if (event.status === 'connected') {
            ElMessage.success('进入多人编辑');
        } else if (event.status === 'disconnected') {
            ElMessage.warning('退出合作编辑');
            console.warn('连接断开原因：', event.reason || '未知原因');
        }
    });

    // 延迟设置本地状态
    const updateLocalState = () => {
        provider.awareness.setLocalState({
            user: {
                id: userId,
                name: username,
                color: color
            }
        });
    };
    setTimeout(updateLocalState, 300);

    // 监听在线用户变化
    provider.awareness.on('change', () => {
        const states = provider.awareness.getStates();
        const users = [];

        states.forEach((state, clientID) => {
            if (state && state.user) {
                users.push(state.user);
            }
        });

        // 通知前端更新在线用户列表
        if (window.updateOnlineUsers) {
            window.updateOnlineUsers(users);
        }
    });

    // 监听连接错误
    provider.on('error', (error) => {
        console.error('协作连接错误:', error);
        ElMessage.error('协作服务异常：' + (error.message || '未知错误'));
    });

    // 返回协作实例
    return { 
        ydoc, 
        provider, 
        binding,
        syncToDatabaseTimer, 
        ytext
    };
};

// 销毁实时协作资源
export const destroyCollaboration = (collabInstance) => {
    if (!collabInstance) return;
    
    const { provider, binding, ytext, ytextObserver, syncToDatabaseTimer } = collabInstance;

    // 1. 清理同步定时器
    if (syncToDatabaseTimer) {
        clearTimeout(syncToDatabaseTimer);
    }

    // 2. 移除Yjs文本观察者
    if (ytext && ytextObserver) {
        ytext.unobserve(ytextObserver);
    }

    // 3. 主动清空本地Awareness状态，告知服务端用户已离线
    if (provider && provider.awareness) {
        provider.awareness.setLocalState(null);
        provider.awareness.off('change');
        provider.awareness.destroy();
    }

    // 4. 销毁Quill绑定
    if (binding) {
        binding.destroy();
    }

    // 5. 主动断开WebSocket连接并销毁provider
    if (provider) {
        provider.off('status');
        provider.off('error');
        provider.disconnect();
        provider.destroy();
    }

    console.log('协作资源已彻底销毁，用户已标记为离线');
};

// 同步文档内容到Yjs
export const syncDocContentToYjs = (ytext, htmlContent) => {
    // 1. 避免重复同步（Yjs已有内容则返回）
    if (ytext.length > 0) {
        console.log('Yjs文本已有内容，跳过重复同步');
        return;
    }

    // 2. 无HTML内容则返回
    if (!htmlContent || htmlContent.trim() === '') {
        console.log('无有效HTML内容，跳过Yjs同步');
        return;
    }

    try {
        // 3. 正确将HTML转换为Quill内容，再提取纯文本（Yjs text支持）
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = htmlContent.trim();
        const plainText = tempDiv.textContent || tempDiv.innerText || '';

        // 4. 正确插入Yjs text
        if (plainText) {
            ytext.insert(0, plainText);
            console.log('文档纯文本内容已同步到Yjs');
        }
    } catch (error) {
        console.error('同步文档内容到Yjs失败:', error.message);
    }
};