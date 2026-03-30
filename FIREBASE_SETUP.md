# Firebase 接入说明

这个项目现在支持两种运行模式：

1. 本地模式  
   没有 `app/google-services.json` 时自动启用，用于开发和演示。

2. Firebase 模式  
   放入 `app/google-services.json` 后自动启用，电影、用户、订单、积分会改为使用 Firestore。

## 1. 放置配置文件

把你在 Firebase Console 下载的 `google-services.json` 放到这里：

`app/google-services.json`

项目里的 [app/build.gradle.kts](/Users/edisonkwan/Desktop/UOW/Year2/Sem2/Mobile%20Application/Project/app/build.gradle.kts#L5) 已经做了自动检测，有这个文件时会自动启用 Google Services 插件。

## 2. 创建 Firestore 数据库

在 Firebase Console 中：

1. 打开你的项目
2. 进入 `Firestore Database`
3. 创建数据库
4. 开发阶段建议先使用测试环境

## 3. 建议的集合结构

### `users`

文档 ID：用户 ID

字段：
- `uid` String
- `name` String
- `age` Number
- `email` String
- `passwordHash` String
- `credits` Number
- `role` String，值为 `admin` 或 `user`
- `active` Boolean
- `createdAt` Timestamp

### `movies`

文档 ID：电影 ID

字段：
- `id` String
- `title` String
- `description` String
- `price` Number
- `posterUrl` String
- `previewVideoUrl` String
- `genre` String
- `rating` Number
- `director` String
- `cast` String
- `active` Boolean

### `orders`

文档 ID：订单 ID

字段：
- `orderId` String
- `userId` String
- `movies` Array
- `totalAmount` Number
- `timestamp` Timestamp
- `status` String

## 4. 默认测试账号

应用初始化时会自动补默认数据：

- `admin@example.com / admin123`
- `mary@example.com / mary123`
- `john@example.com / john123`

## 5. 本地资源写法

为了让电影海报和视频在 Firebase 中也能继续用你项目里的本地资源，这个项目支持下面两种写法：

- 海报：`drawable://avengers4`
- 视频：`raw://avenger_trailer`

仓库会在运行时自动解析成 Android 资源。

## 6. 规则说明

当前项目为了支持“管理员在 App 内新增/删除用户”这一需求，用户数据是由客户端直接写入 Firestore 的。  
这意味着如果你想完全依赖 Firestore 存用户名和密码哈希，开发阶段规则需要放宽。

仓库里已经提供了一个演示用规则文件：

`firestore.rules`

注意：

- 这个规则适合课程演示和开发测试
- 不适合生产环境
- 如果你想做成真正安全的线上系统，下一步应该改成 Firebase Authentication + Cloud Functions / Admin SDK

## 7. 当前已减少的 Firebase 配置负担

我已经把电影、用户、订单查询改成客户端排序，不再依赖额外的复合索引，所以你接 Firestore 时通常不需要再手动创建索引。
