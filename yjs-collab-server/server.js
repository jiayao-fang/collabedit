const WebSocket = require('ws');
const Y = require('yjs');
const { RedisPersistence } = require('y-redis');
const jwt = require('jsonwebtoken');
const { parse } = require('url');
const awarenessProtocol = require('y-protocols/awareness');
const syncProtocol = require('y-protocols/sync');
const encoding = require('lib0/encoding');
const decoding = require('lib0/decoding');
const http = require('http');
const url = require('url');
const express = require('express');
const app = express();



const WS_PORT = 1234;
const REDIS_CONFIG = { 
  host: 'localhost', 
  port: 6379,
  //配置Redis持久化的前缀，避免冲突
  prefix: 'yjs_doc_' 
};
const JWT_SECRET = 'VGhpcyIsYXRoaW5ncyBmb3IgdGVzdGluZyBqd3Qgc2VjcmV0LCBub3QgZm9yIHVzZSBpbiByZWFsIHByaW1lbnRzIQ==';

// 初始化Redis持久化
const persistence = new RedisPersistence(REDIS_CONFIG);

// 文档映射：docId -> { ydoc, awareness, clients: Map<clientID: number, ws> }
const docs = new Map();
// 并发创建锁：避免重复创建文档
const creatingDocs = new Map(); // docId -> Promise
// 自增客户端ID
let nextClientID = 1;

// 延迟销毁定时器，销毁内存实例
let destroyTimers = new Map(); // docId -> 定时器ID

const server = http.createServer(app);

/**
 * 延迟销毁内存中的文档实例（保留Redis持久化）
 * @param {string} docId 文档ID
 * @param {Object} doc 文档实例
 */
function scheduleDocDestroy(docId, doc) {
  // 清除旧定时器（避免重复销毁）
  if (destroyTimers.has(docId)) {
    clearTimeout(destroyTimers.get(docId));
  }
  // 延长销毁时间（比如30分钟，根据业务调整）
  const DESTROY_DELAY = 30 * 60 * 1000; // 30分钟
  const timer = setTimeout(() => {
    if (doc.clients.size === 0) {
      console.log(`文档${docId}内存实例销毁（${DESTROY_DELAY/1000}秒无连接）`);
      // 销毁内存中的Yjs实例，不解绑Redis
      doc.ydoc.destroy();
      docs.delete(docId); // 从内存映射中移除
    }
    destroyTimers.delete(docId);
  }, DESTROY_DELAY);
  destroyTimers.set(docId, timer);
}

// 创建WS服务器（允许跨域）
const wss = new WebSocket.Server({
  port: WS_PORT,
  cors: { origin: '*' }
});
console.log(`Yjs WS server running at ws://localhost:${WS_PORT}`);

/**
 * 广播消息给文档的所有客户端（排除发送者）
 * @param {Object} doc 文档实例
 * @param {number} senderClientID 发送者ID
 * @param {Uint8Array} message 二进制消息
 */
function broadcast(doc, senderClientID, message) {
   // 收集需要清理的断开连接
  const disconnectedClients = [];

    doc.clients.forEach((ws, clientID) => {

    if (clientID !== senderClientID && ws) {
      // 检查连接状态
      if (ws.readyState === WebSocket.OPEN) {
        try {
          ws.send(message, (err) => {
            if (err) {
              // 发送失败，标记为断开
              console.warn(`广播消息给客户端${clientID}失败：`, err.message);
              disconnectedClients.push(clientID);
            }
          });
        } catch (err) {
          console.warn(`广播消息给客户端${clientID}异常：`, err.message);
          disconnectedClients.push(clientID);
        }
      } else {

        disconnectedClients.push(clientID);
      }
    }
  });
  
  disconnectedClients.forEach(clientID => {
    if (doc.clients.has(clientID)) {
      console.log(`清理断开的客户端${clientID}连接`);
      doc.clients.delete(clientID);
      // 清理awareness状态
      if (typeof doc.awareness.removeState === 'function') {
        doc.awareness.removeState(clientID);
      } else {
        doc.awareness.setLocalStateField(clientID, null);
      }
    }
  });
}

/**
 * 获取或创建文档实例（核心：从Redis加载持久化状态）
 * @param {string} docId 文档ID
 */
async function getDoc(docId) {
  docId = String(docId);

  // 1. 内存中有文档：直接返回
  if (docs.has(docId)) {
    return docs.get(docId);
  }

  // 2. 正在创建：等待
  if (creatingDocs.has(docId)) {
    return await creatingDocs.get(docId);
  }

  // 3. 从Redis加载/创建文档（核心：保留Redis持久化）
  const createPromise = (async () => {
    let ydoc = new Y.Doc();
    try {
      // 从Redis加载已持久化的文档状态
      await persistence.bindState(docId, ydoc);
      console.log(`从Redis加载文档${docId}状态（持久化保留）`);
    } catch (err) {
      // 仅捕获非重复绑定的致命错误
      if (!err.message.includes('already bound')) {
        console.error(`Redis加载文档${docId}失败:`, err.message);
        throw err;
      } else {
        // 已绑定则直接复用
        console.log(`文档${docId}已绑定Redis，复用状态`);
      }
    }

    // 初始化Awareness（自动清理旧状态）
    const awareness = new awarenessProtocol.Awareness(ydoc);
    awareness.options = {
      autoDelete: true, // 自动清理超时的用户状态
      timeout: 10000,   // 10秒清理无心跳的用户
    };

    // 构造文档实例（仅存于内存）
    const doc = {
      ydoc,
      awareness,
      clients: new Map()
    };
    docs.set(docId, doc);

    // 文档销毁时：仅清理内存，不解绑Redis！
    const destroyHandler = () => {
      console.log(`文档${docId}内存销毁，Redis持久化保留`);
      docs.delete(docId);
      ydoc.off('destroy', destroyHandler); // 移除监听
    };
    ydoc.on('destroy', destroyHandler);

    return doc;
  })();

  creatingDocs.set(docId, createPromise);
  try {
    return await createPromise;
  } finally {
    creatingDocs.delete(docId);
  }
}

// 处理WS连接
wss.on('connection', async (ws, req) => {
  const { query } = parse(req.url, true);
  const docId = String(query.docId);
  const token = query.token;
  const username = query.username;
  const color = query.color;

  // 基础校验
  if (!docId || !token) {
    ws.close(4001, 'missing docId or token');
    return;
  }

  // JWT验证
  let user;
  try {
    user = jwt.verify(token, Buffer.from(JWT_SECRET, 'base64'));
  } catch (err) {
    console.error(`Token验证失败：${err.message}`);
    ws.close(4002, 'invalid token');
    return;
  }

  // 获取文档实例（从Redis加载）
  let doc;
  try {
    doc = await getDoc(docId);
    // 重连时清除延迟销毁定时器（关键！）
    if (destroyTimers.has(docId)) {
      clearTimeout(destroyTimers.get(docId));
      destroyTimers.delete(docId);
      console.log(`文档${docId}重连，取消延迟销毁`);
    }
  } catch (err) {
    console.error(`获取文档${docId}失败:`, err.message);
    ws.close(1011, 'failed to get document');
    return;
  }

  // 分配客户端ID
  const clientID = nextClientID++;
  doc.clients.set(clientID, ws);
  console.log(`客户端${clientID}连接文档${docId}，在线数：${doc.clients.size}`);

  // 初始化用户状态
  doc.awareness.setLocalStateField(clientID, {
    user: {
      id: String(user.id),
      name: username || user.username || `用户${clientID}`,
      color: color || `#${Math.floor(Math.random()*16777215).toString(16)}`
    }
  });
  if (!doc.awareness.meta.has(clientID)) {
    doc.awareness.meta.set(clientID, { clock: 0 });
  }

  // 发送Sync Step1（同步Redis中的最新状态）
  const syncEncoder = encoding.createEncoder();
  encoding.writeVarUint(syncEncoder, 0);
  syncProtocol.writeSyncStep1(syncEncoder, doc.ydoc);
  ws.send(encoding.toUint8Array(syncEncoder));

  // 发送Awareness初始化
  const awarenessEncoder = encoding.createEncoder();
  encoding.writeVarUint(awarenessEncoder, 1);
  const awarenessUpdate = awarenessProtocol.encodeAwarenessUpdate(
    doc.awareness,
    Array.from(doc.awareness.getStates().keys())
  );
  encoding.writeVarUint8Array(awarenessEncoder, awarenessUpdate);
  ws.send(encoding.toUint8Array(awarenessEncoder));

  // 处理消息
  ws.on('message', (data) => {
    try {
      const decoder = decoding.createDecoder(new Uint8Array(data));
      const encoder = encoding.createEncoder();
      const messageType = decoding.readVarUint(decoder);
        console.log('[YSJ-SERVER] recv message:', new TextDecoder().decode(new Uint8Array(data)));

      switch (messageType) {
        case 0: // Sync消息
          syncProtocol.readSyncMessage(decoder, encoder, doc.ydoc, clientID);
          if (encoding.length(encoder) > 1) {
            ws.send(encoding.toUint8Array(encoder));
          }
          break;
        case 1: // Awareness消息
          const update = decoding.readVarUint8Array(decoder);
          awarenessProtocol.applyAwarenessUpdate(doc.awareness, update, clientID);
          const broadcastEncoder = encoding.createEncoder();
          encoding.writeVarUint(broadcastEncoder, 1);
          encoding.writeVarUint8Array(broadcastEncoder, update);
          broadcast(doc, clientID, encoding.toUint8Array(broadcastEncoder));
          break;
        default:
          console.warn(`未知消息类型${messageType}，客户端${clientID}`);
      }
    } catch (err) {
      console.error(`客户端${clientID}消息处理失败:`, err.message);
      ws.close(1011, 'invalid message');
    }
  });

  // 连接关闭：仅触发延迟销毁，不解绑Redis
  ws.on('close', (code, reason) => {
    console.log(`客户端${clientID}断开文档${docId}，原因：${reason.toString()}`);
    doc.clients.delete(clientID);

    // 清理用户状态
    if (typeof doc.awareness.removeState === 'function') {
      doc.awareness.removeState(clientID);
    } else {
      doc.awareness.setLocalStateField(clientID, null);
    }

    // 广播离线状态
    const offlineEncoder = encoding.createEncoder();
    encoding.writeVarUint(offlineEncoder, 1);
    const offlineUpdate = awarenessProtocol.encodeAwarenessUpdate(
      doc.awareness,
      [clientID]
    );
    encoding.writeVarUint8Array(offlineEncoder, offlineUpdate);
    broadcast(doc, clientID, encoding.toUint8Array(offlineEncoder));

    // 无连接时触发延迟销毁（仅内存）
    if (doc.clients.size === 0) {
      console.log(`文档${docId}无在线客户端，${scheduleDocDestroy.DESTROY_DELAY/60000}分钟后销毁内存实例`);
      scheduleDocDestroy(docId, doc);
    }
  });

  // 错误处理
  ws.on('error', (err) => {
    console.error(`客户端${clientID}错误:`, err.message);
    doc.clients.delete(clientID);
    if (doc.awareness.removeState) doc.awareness.removeState(clientID);
    else doc.awareness.setLocalStateField(clientID, null);
  });
});

// 全局错误
wss.on('error', (err) => {
  console.error('WS服务器全局错误:', err.message);
});

// 进程退出：仅此时解绑Redis（避免数据丢失）
process.on('exit', () => {
  console.log('服务器退出，清理所有资源');
  // 清理定时器
  destroyTimers.forEach(timer => clearTimeout(timer));
  destroyTimers.clear();
  // 解绑Redis（仅服务器退出时）
  docs.forEach((doc, docId) => {
    persistence.unbindState(docId, doc.ydoc);
    console.log(`服务器退出，解绑文档${docId}的Redis绑定`);
    doc.ydoc.destroy();
  });
});

// 未捕获异常
process.on('uncaughtException', (err) => {
  console.error('未捕获异常:', err.message);
  destroyTimers.forEach(timer => clearTimeout(timer));
  destroyTimers.clear();
  docs.forEach((doc, docId) => {
    persistence.unbindState(docId, doc.ydoc);
    doc.ydoc.destroy();
  });
  process.exit(1);
});