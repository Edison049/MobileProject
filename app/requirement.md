1. 项目角色与目标

角色：你是一位资深 Android 开发专家和教育顾问。
目标：协助我完成一个大学小组作业项目 "Movie/DVD Online Order System"。
核心要求：
IDE: Android Studio (最新稳定版)。
编程语言: Java (必须使用 Java，不要用 Kotlin)。
数据库: Firebase (Firestore 数据库 + Firebase Authentication + Firebase Storage)。
注意：学校原始要求是 SQLite，但我们已决定升级为 Firebase。请确保数据结构清晰，方便我在文档中解释这一变更（为了云同步和扩展性）。
UI 风格: 类似 Netflix (深色模式、横向滚动列表、大海报图、简洁现代)。
功能模块: 必须覆盖学校要求的 6 个核心模块 + 3 个高级功能。
2. 技术栈与依赖

请在项目初始化时配置以下依赖：
Firebase BoM: 用于管理 Firebase 库版本。
Firebase Auth: 用于用户注册和登录。
Firebase Firestore: 用于存储用户信息、电影数据、订单记录。
Firebase Storage: 用于存储电影海报和预览视频。
Glide 或 Picasso: 用于高效加载图片（高级功能之一）。
ExoPlayer (Media3): 用于电影预览播放（高级功能之二）。
RecyclerView & CardView: 用于列表展示。
Material Design Components: 用于现代化 UI 组件。
3. 详细功能模块实现指南

模块 1: 注册模块 (Registration Module)

功能: 允许新用户注册。
数据字段: Member ID (Email), Password, Name, Age, Credits (初始积分，例如 1000)。
实现: 使用 Firebase Authentication 创建账户，并在 Firestore users 集合中存储额外信息 (name, age, credits)。
UI: 简洁的表单，包含姓名、年龄、邮箱、密码输入框。
模块 2: 登录模块 (Login Module)

功能: 用户登录并显示信息。
特殊要求 (学校硬性规定): 系统必须支持以下两个测试账户登录（请在 Firebase 中预先创建或代码中确保兼容）：
User1: name: mary, password: mary123
User2: name: john, password: john123
逻辑:
登录成功后，进入 "Main Screen"。
首页需显示用户信息包括拥有的 "Credits"。
注意: 虽然学校要求“只有这两个用户可以登录”，但为了符合注册模块要求，请实现完整的 Firebase 登录逻辑，但确保这两个账户一定可用。请在代码注释中标记此逻辑，以便我在文档中说明。
模块 3: 产品摘要 (Product Summary - Main Menu)

功能: 入口页面，展示电影目录。
UI (Netflix 风格):
顶部：用户问候语 + 当前 Credits 显示。
主体：使用 RecyclerView 横向滚动展示不同分类的电影（如 "Action", "Drama"）。
列表项：每行显示最小但关键信息（电影海报、标题、评分、上映日期）。不要显示过多文字。
导航：底部导航栏或侧边菜单，可返回主菜单。
数据: 电影数据必须从 Firebase Firestore 读取，严禁硬编码在 Java 代码中。
模块 4: 产品详情 (Product Detail)

功能: 点击电影后显示详细信息。
UI: 干净整洁的布局。
大尺寸海报。
标题、导演、演员、剧情简介、价格、时长。
高级功能: 包含一个视频预览播放器 (ExoPlayer)。
按钮："Add to Cart"。
导航: 必须有返回主菜单的按钮。
模块 5: 购物车模块 (Cart Module)

功能: 管理购买意向。
逻辑:
允许添加多部电影到购物车。
显示购物车内所有电影列表及单项价格。
显示总金额 (Total Amount)。
核心逻辑: 结账时，检查用户 Credits 是否足够。如果足够，扣除相应 Credits，生成订单，清空购物车。
数据同步：更新 Firestore 中用户的 credits 字段。
模块 6: 购买产品摘要 (Purchased Product Summary)

功能: 显示发票/订单历史。
UI: 类似收据的界面。
列出已购买电影、单价、总金额。
显示购买后的剩余 Credits。
数据: 从 Firestore orders 集合读取。
4. 高级功能 (Advanced Features - 必选 3 项)

请在代码中明确实现以下三项，以便我获得 Part B 的分数：
搜索功能 (Search): 在主菜单顶部添加搜索栏，支持按电影名称、演员、类型搜索 (Firestore Query)。
图片加载优化 (Image Loading): 使用 Glide 库加载海报，支持占位图和缓存。
视频预览 (Video Preview): 在详情页使用 ExoPlayer 播放电影预告片。
5. 数据库设计 (Firebase Firestore Schema)

请按照以下结构设计数据库，确保数据不硬编码：
Collection: users

Document ID: uid (from Auth)
Fields:
name (String)
age (int)
email (String)
credits (int)
createdAt (Timestamp)
Collection: movies

Document ID: auto_id
Fields:
title (String)
description (String)
price (int)
posterUrl (String)
previewVideoUrl (String)
genre (String)
rating (float)
director (String)
cast (String)
Collection: orders

Document ID: auto_id
Fields:
userId (String)
movies (Array of Objects)
totalAmount (int)
timestamp (Timestamp)
status (String)
6. 代码质量与文档支持

注释: 关键逻辑必须添加中文注释，方便我编写文档 (Part C)。
结构: 采用 MVC 或 MVVM 模式，保持代码清晰。
安全性: Firebase Rules 需配置为仅允许认证用户读写数据。
异常处理: 网络错误、积分不足等情况需有 Toast 提示。
7. 执行步骤

请分步骤引导我完成项目，不要一次性生成所有代码。
第一步: 指导我如何在 Android Studio 创建项目并配置 Firebase 连接 (google-services.json)。
第二步: 生成数据库模型类 (Java Model Classes)。
第三步: 实现登录与注册界面及逻辑。
第四步: 实现主菜单 (Netflix 风格列表) 及数据读取。
第五步: 实现详情页、购物车及结账逻辑。
第六步: 实现高级功能 (搜索、视频播放)。
第七步: 提供测试指南，确保 mary/john 账户可用。
