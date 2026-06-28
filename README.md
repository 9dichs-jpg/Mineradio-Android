# Mineradio Android

> 把 Mineradio 桌面版音乐播放器完整移植到 Android！
> 原项目：[XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio)

Mineradio 是一款沉浸式音乐播放器，融合天气电台、歌词舞台、粒子视觉和 3D 歌单架。

---

## 下载 APK

[点此下载最新 APK](https://github.com/bolaosi/Mineradio-Android/releases/latest)

要求：Android 8.0+，建议 4GB 内存以上，需要开启"允许安装未知来源应用"。

---

## 功能状态

| 功能               | 状态   |
| ------------------ | ------ |
| UI / 3D 粒子 / 歌词 | 正常   |
| 网易云搜索/播放     | 正常   |
| QQ 音乐搜索/播放    | 正常   |
| 扫码登录            | 正常   |
| 天气电台            | 正常   |
| 均衡器              | 正常   |
| 桌面歌词            | 不可用 |

---

## 自行构建

1. Fork 本仓库
2. 进入 Actions → Build Mineradio APK → Run workflow
3. 等待 5-10 分钟，在 Artifacts 下载 APK

---

## 部署后端（必须，免费）

本 App 依赖 Node.js 后端服务器提供音乐搜索/登录/播放接口。推荐使用 Railway 免费部署。

### 一键部署到 Railway

1. 打开 [railway.app](https://railway.app) → 用 GitHub 登录
2. 点击 **New Project** → **Deploy from GitHub**
3. 选择 `bolaosi/Mineradio-Android` 仓库
4. **关键**：Root Directory 设为 `app/src/main/assets/nodejs-project`
5. 点击 **Deploy**
6. 等待 2-3 分钟，复制生成的域名（类似 `https://xxx.up.railway.app`）
7. 把域名告诉开发者，更新到 APK 中

---

## 项目结构

```
app/src/main/
  assets/
    index.html          Web 前端
    vendor/             第三方库 (Three.js, GSAP)
    nodejs-project/     后端服务器
      server.js         API 服务 + NeteaseCloudMusicApi
      package.json
  java/.../MainActivity.java  WebView 容器 + API 路由
```

---

## 许可

遵循原项目 [XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio) 的许可协议。