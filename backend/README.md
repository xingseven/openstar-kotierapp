# Backend

这里放的是面向所有前端的统一后端边界，不是某个 UI 的实现细节。

目标很简单：前端可以换，协议不换；Android、Qt、以后如果有 Web 前端，都只通过同一套命令和事件和后端交互。

现在这里已经不只是说明文档了，实际的机器可读文件也已经落地：

- `protocol/easytier.rpc.json`：统一后端协议清单。
- `core/easytier.models.json`：请求、响应、事件共用的数据模型。
- `qt-easy-tier-master/SRC/backend/`：Qt 侧复用的薄 backend 工具，已经包含运行态解析、JSON 文件存取（对象/数组）、服务器列表读取和版本比较等纯逻辑。
- `qt-easy-tier-master/SRC/backend/ServerInfo.h`：Qt 侧收藏服务器的共享数据模型，页面直接复用它的 JSON 转换和数组映射。
- `qt-easy-tier-master/SRC/backend/ServerInfoData.h`：Qt 侧服务器对话框的共享数据模型，负责兼容本地收藏和公共服务器两种 JSON 格式。
- `qt-easy-tier-master/SRC/backend/JsonArrayUtils.h`：Qt 侧 JSON 对象数组字段提取 helper，页面直接复用它来收集字符串列表。
- `android/app/src/main/java/com/easytier/backend/`：Android 侧复用的薄 backend 工具，已经包含 TOML 脱敏、节点 JSON 解析和一键联机码编解码。
- `android/app/src/main/java/com/easytier/backend/`：Android 侧复用的薄 backend 工具，已经包含 TOML 脱敏、节点 JSON 解析、一键联机码编解码和逗号分隔字符串解析。

## 分层

- `protocol/`：统一调用协议，定义请求、响应、事件、错误码。
- `adapters/`：不同平台的薄适配层，只负责把平台能力接到协议上。
- `core/`：后续真正收纳网络实例生命周期、状态查询、配置迁移的共享逻辑。

## 当前状态

这一步先立边界，同时补了可机器读取的协议和模型文件，不做大规模搬迁。

现阶段的要求是：

1. 前端不要直接碰底层引擎细节。
2. 平台差异只留在适配层。
3. 所有新能力先写进协议，再接入实现。

Qt 侧的适配层代码目前先落在 `qt-easy-tier-master/SRC/backend/`，由 `RuntimeService` 和 `ETRunWorker` 协作转发到具体 backend client，并逐步收纳运行态 JSON 解析、状态缓存更新规则和 worker 生命周期控制；`QtETNetwork` 与 `QtETOneClick` 现在都通过同一类 service 间接访问运行时后端，一键联机页的运行态 JSON 解释、TOML 配置拼装和联机码生成/解码也都已下沉到 backend 解析器/构建器。
Android 侧的薄共享工具已经落在 `android/app/src/main/java/com/easytier/backend/`，用于复用 TOML 脱敏、节点 JSON 解析和一键联机码编解码。