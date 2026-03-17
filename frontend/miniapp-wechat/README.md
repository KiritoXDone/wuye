# miniapp-wechat

原生微信小程序 + TypeScript 的 Resident 最小 MVP，已接通当前后端已实现的住户链路：

- 微信登录：`/api/v1/auth/login/wechat`
- 我的房间：`/api/v1/me/rooms`
- 全部账单：`/api/v1/me/bills`
- 房间账单：`/api/v1/me/rooms/{roomId}/bills`
- 账单详情：`/api/v1/bills/{billId}`
- 创建支付单：`/api/v1/payments`
- 支付结果轮询：`/api/v1/payments/{payOrderNo}`

## 当前页面

```text
pages/
├─ login            登录页
├─ rooms            我的房间
├─ bills            账单列表（支持全部账单 / 房间账单）
├─ bill-detail      账单详情 + 发起支付
└─ payment-result   支付结果轮询
```

## 本地启动

1. 安装依赖

```bash
npm install
```

2. 用微信开发者工具打开 `frontend/miniapp-wechat`

3. 开发者工具中建议：

- 关闭“请求合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”校验
- 确保后端已启动，默认请求地址为 `http://127.0.0.1:8080`

## 演示登录 code

后端当前是本地开发桩，直接支持以下演示 code：

- `resident-zhangsan`
- `resident-lisi`

登录页也保留了 `wx.login` 获取真实 code 的入口，但在当前本地桩环境下，优先建议使用上述演示 code 联调。

## 支付说明

当前后端支付也是开发桩：

- 创建支付单后会先返回 mock `payParams`
- 小程序会继续进入支付结果页轮询支付状态
- 若没有手动触发后端支付回调，页面会停留在“支付处理中”并在轮询上限后停止

这符合仓库当前 backend-first MVP 的真实状态，便于先完成住户端最小页面闭环。
