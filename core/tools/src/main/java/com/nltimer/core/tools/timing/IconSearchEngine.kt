package com.nltimer.core.tools.timing

import org.json.JSONObject

object IconSearchEngine {

    data class IconMatch(
        val iconKey: String,
        val name: String,
        val library: String,
        val keywords: String,
    )

    private val SYNONYM_MAP: Map<String, List<String>> = mapOf(
        "review" to listOf("check", "assessment", "audit"),
        "评审" to listOf("检查", "审核", "check"),
        "urgent" to listOf("alarm", "priority", "alert"),
        "紧急" to listOf("告警", "提醒", "alarm"),
        "cooking" to listOf("food", "restaurant", "kitchen"),
        "做饭" to listOf("餐厅", "吃饭", "食物", "food"),
        "course" to listOf("school", "education", "study", "book"),
        "课程" to listOf("学习", "学校", "教育", "school"),
        "priority" to listOf("alarm", "flag", "star"),
        "project" to listOf("folder", "briefcase", "work"),
        "项目" to listOf("文件夹", "工作", "folder"),
        "relax" to listOf("spa", "self_improvement", "favorite"),
        "放松" to listOf("冥想", "瑜伽", "meditation"),
        "competition" to listOf("sports", "game", "trophy"),
        "竞技" to listOf("游戏", "运动", "game"),
        "cardio" to listOf("fitness", "exercise", "run"),
        "有氧" to listOf("运动", "健身", "跑步", "fitness"),
        "strength" to listOf("fitness", "muscle", "gym"),
        "力量" to listOf("健身", "肌肉", "fitness"),
        "todo" to listOf("checklist", "task", "memo"),
        "待办" to listOf("清单", "备忘", "任务", "checklist"),
        "低优先级" to listOf("flag", "bookmark"),
        "必买" to listOf("购物", "购物袋", "shopping"),
        "复习" to listOf("书", "学习", "笔记", "book"),
    )

    fun search(
        query: String,
        library: String? = null,
        limit: Int = 20,
    ): List<IconMatch> {
        val q = query.lowercase()
        val results = mutableListOf<IconMatch>()

        if (library == null || library == "hi") {
            searchHugeIcons(q, limit).also { results.addAll(it) }
        }
        if (library == null || library == "mi") {
            searchMaterialIcons(q, limit).also { results.addAll(it) }
        }
        if (library == null || library == "emoji") {
            searchEmoji(q, limit).also { results.addAll(it) }
        }

        return results.take(limit)
    }

    fun searchWithFallback(
        query: String,
        library: String? = null,
        limit: Int = 5,
    ): List<IconMatch> {
        val direct = search(query, library, limit)
        if (direct.isNotEmpty()) return direct

        val synonyms = SYNONYM_MAP[query.lowercase()] ?: SYNONYM_MAP[query] ?: return emptyList()
        for (syn in synonyms) {
            val results = search(syn, library, limit)
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    /**
     * 多关键词搜索，优先 hi 库。
     * 按关键词顺序尝试，每个关键词先搜 hi，hi 无结果再搜 mi/emoji。
     * 返回第一个有结果的关键词的匹配，以及匹配来源的库信息。
     */
    data class MultiSearchResult(
        val match: IconMatch?,
        val matchedQuery: String?,
        val fallbackLibrary: String?,  // 如果使用了 mi/emoji 则记录，hi 匹配为 null
    )

    fun searchMultipleQueries(
        queries: List<String>,
        limit: Int = 1,
    ): MultiSearchResult {
        for (query in queries) {
            // 优先 hi 库
            val hiResults = searchHugeIcons(query.lowercase(), limit)
            if (hiResults.isNotEmpty()) {
                return MultiSearchResult(
                    match = hiResults.first(),
                    matchedQuery = query,
                    fallbackLibrary = null,
                )
            }
            // hi 无结果，搜索 mi 和 emoji
            val miResults = searchMaterialIcons(query.lowercase(), limit)
            if (miResults.isNotEmpty()) {
                return MultiSearchResult(
                    match = miResults.first(),
                    matchedQuery = query,
                    fallbackLibrary = "mi",
                )
            }
            val emojiResults = searchEmoji(query.lowercase(), limit)
            if (emojiResults.isNotEmpty()) {
                return MultiSearchResult(
                    match = emojiResults.first(),
                    matchedQuery = query,
                    fallbackLibrary = "emoji",
                )
            }
        }
        return MultiSearchResult(match = null, matchedQuery = null, fallbackLibrary = null)
    }

    fun toJsonObject(match: IconMatch): JSONObject = JSONObject().apply {
        put("iconKey", match.iconKey)
        put("name", match.name)
        put("library", match.library)
        put("keywords", match.keywords)
    }

    private fun searchHugeIcons(query: String, limit: Int): List<IconMatch> {
        val results = mutableListOf<IconMatch>()
        for ((name, keywords) in HUGE_ICON_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(
                    IconMatch(
                        iconKey = "hi:$name",
                        name = name,
                        library = "hi",
                        keywords = keywords.joinToString(","),
                    )
                )
                if (results.size >= limit) break
            }
        }
        return results
    }

    private fun searchMaterialIcons(query: String, limit: Int): List<IconMatch> {
        val results = mutableListOf<IconMatch>()
        for ((name, keywords) in MATERIAL_ICON_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(
                    IconMatch(
                        iconKey = "mi:filled:$name",
                        name = name,
                        library = "mi",
                        keywords = keywords.joinToString(","),
                    )
                )
                if (results.size >= limit) break
            }
        }
        return results
    }

    private fun searchEmoji(query: String, limit: Int): List<IconMatch> {
        val results = mutableListOf<IconMatch>()
        for ((emoji, name, keywords) in EMOJI_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(
                    IconMatch(
                        iconKey = emoji,
                        name = name,
                        library = "emoji",
                        keywords = keywords.joinToString(","),
                    )
                )
                if (results.size >= limit) break
            }
        }
        return results
    }

    private val HUGE_ICON_DATA: List<Pair<String, List<String>>> = listOf(
        "Add01" to listOf("add", "plus", "新建", "添加"),
        "AddCircle" to listOf("add", "circle", "新建", "添加"),
        "Access" to listOf("access", "enter", "访问", "入口"),
        "Activity01" to listOf("activity", "活动", "动态"),
        "Tick01" to listOf("tick", "check", "done", "完成", "勾"),
        "CheckmarkCircle01" to listOf("check", "circle", "success", "成功", "确认"),
        "CheckmarkSquare01" to listOf("check", "square", "确认"),
        "CheckList" to listOf("checklist", "list", "清单", "待办"),
        "Cancel01" to listOf("cancel", "close", "x", "取消", "关闭"),
        "CancelCircle" to listOf("cancel", "circle", "取消"),
        "Delete01" to listOf("delete", "remove", "trash", "删除"),
        "Edit01" to listOf("edit", "modify", "编辑", "修改"),
        "Copy01" to listOf("copy", "duplicate", "复制"),
        "Clipboard" to listOf("clipboard", "paste", "剪贴板"),
        "Filter" to listOf("filter", "筛选", "过滤"),
        "SortByDown01" to listOf("sort", "排序"),
        "Search01" to listOf("search", "find", "搜索", "查找"),
        "Searching" to listOf("search", "搜索"),
        "Refresh" to listOf("refresh", "reload", "刷新", "重载"),
        "Reload" to listOf("reload", "refresh", "重载"),
        "Favourite" to listOf("favourite", "favorite", "star", "收藏", "喜欢"),
        "Star" to listOf("star", "favourite", "星", "收藏"),
        "ThumbsUp" to listOf("thumbs", "up", "like", "赞", "拇指"),
        "Eye" to listOf("eye", "view", "visible", "眼睛", "查看"),
        "InformationCircle" to listOf("info", "information", "信息"),
        "Question" to listOf("question", "help", "帮助", "问号"),
        "MagicWand01" to listOf("magic", "wand", "auto", "魔法", "自动"),
        "Megaphone01" to listOf("megaphone", "announce", "喇叭", "广播"),
        "Crown" to listOf("crown", "king", "vip", "皇冠"),
        "Award01" to listOf("award", "prize", "奖", "奖励"),
        "Medal01" to listOf("medal", "prize", "奖章"),
        "Gift" to listOf("gift", "present", "礼物"),
        "Rocket01" to listOf("rocket", "launch", "火箭", "发布"),
        "ArrowDown01" to listOf("arrow", "down", "下", "箭头"),
        "ArrowLeft01" to listOf("arrow", "left", "左", "箭头"),
        "ArrowRight01" to listOf("arrow", "right", "右", "箭头"),
        "ArrowUp01" to listOf("arrow", "up", "上", "箭头"),
        "Call" to listOf("call", "phone", "电话", "呼叫"),
        "Chat" to listOf("chat", "message", "聊天", "消息"),
        "Message01" to listOf("message", "chat", "消息", "信息"),
        "Mail01" to listOf("mail", "email", "邮件", "邮箱"),
        "Notification01" to listOf("notification", "bell", "通知", "提醒", "闹钟", "alarm"),
        "Play" to listOf("play", "播放"),
        "Pause" to listOf("pause", "暂停"),
        "Stop" to listOf("stop", "停止"),
        "MusicNote01" to listOf("music", "note", "音乐", "音符"),
        "Headphones" to listOf("headphones", "audio", "耳机", "音乐"),
        "Camera01" to listOf("camera", "photo", "相机", "拍照"),
        "Image01" to listOf("image", "photo", "picture", "图片", "照片"),
        "Video01" to listOf("video", "film", "视频"),
        "File01" to listOf("file", "document", "文件", "文档"),
        "Folder01" to listOf("folder", "directory", "文件夹", "目录"),
        "Bookmark01" to listOf("bookmark", "书签", "收藏"),
        "Download01" to listOf("download", "下载"),
        "Upload01" to listOf("upload", "上传"),
        "Code" to listOf("code", "programming", "代码", "编程"),
        "Briefcase01" to listOf("briefcase", "work", "公文包", "工作"),
        "Chart" to listOf("chart", "analytics", "图表", "分析"),
        "DashboardSquare01" to listOf("dashboard", "仪表盘"),
        "Money01" to listOf("money", "dollar", "钱", "货币"),
        "Calculator" to listOf("calculator", "计算器"),
        "Store01" to listOf("store", "shop", "商店"),
        "ShoppingBag01" to listOf("shopping", "bag", "购物", "购物袋"),
        "Tag01" to listOf("tag", "label", "标签"),
        "User" to listOf("user", "person", "用户", "个人"),
        "UserGroup" to listOf("user", "group", "team", "群组", "团队"),
        "Lock" to listOf("lock", "secure", "锁", "安全"),
        "Shield01" to listOf("shield", "security", "盾牌", "安全"),
        "Clock01" to listOf("clock", "time", "时钟", "时间"),
        "Timer01" to listOf("timer", "countdown", "计时器", "倒计时"),
        "StopWatch" to listOf("stopwatch", "秒表"),
        "AlarmClock" to listOf("alarm", "clock", "闹钟"),
        "Calendar01" to listOf("calendar", "date", "日历", "日期"),
        "Hourglass" to listOf("hourglass", "沙漏"),
        "Timeline" to listOf("timeline", "时间线"),
        "TimeManagement" to listOf("time", "management", "时间管理"),
        "TimeSchedule" to listOf("schedule", "日程", "计划"),
        "Computer" to listOf("computer", "desktop", "电脑", "台式"),
        "Laptop" to listOf("laptop", "笔记本"),
        "SmartPhone01" to listOf("smartphone", "phone", "手机"),
        "Wifi01" to listOf("wifi", "wireless", "无线"),
        "Energy" to listOf("energy", "power", "能源", "电量"),
        "Earth" to listOf("earth", "globe", "world", "地球", "世界"),
        "Globe" to listOf("globe", "world", "全球"),
        "Maps" to listOf("map", "地图"),
        "Compass" to listOf("compass", "direction", "指南针", "方向"),
        "Home01" to listOf("home", "house", "家", "首页"),
        "Car01" to listOf("car", "auto", "汽车", "车"),
        "Bicycle" to listOf("bicycle", "bike", "自行车"),
        "Airplane01" to listOf("airplane", "flight", "飞机", "航班"),
        "Sun01" to listOf("sun", "太阳"),
        "Moon" to listOf("moon", "night", "月亮", "夜晚", "睡觉", "sleep"),
        "Cloud" to listOf("cloud", "云"),
        "Rain" to listOf("rain", "雨"),
        "Snow" to listOf("snow", "雪"),
        "BookOpen01" to listOf("book", "open", "read", "书", "阅读"),
        "GraduationScroll" to listOf("graduation", "diploma", "毕业", "学位"),
        "StudyDesk" to listOf("study", "desk", "学习", "书桌"),
        "Medicine01" to listOf("medicine", "pill", "药", "药物"),
        "Settings01" to listOf("settings", "gear", "设置", "齿轮"),
        "Share01" to listOf("share", "分享"),
        "Link01" to listOf("link", "url", "链接"),
        "Grid" to listOf("grid", "网格"),
    )

    private val MATERIAL_ICON_DATA: List<Pair<String, List<String>>> = listOf(
        "AccessAlarms" to listOf("alarm", "clock", "time", "闹钟", "时间"),
        "AccessTime" to listOf("time", "clock", "hour", "时间", "小时"),
        "AccountCircle" to listOf("account", "user", "profile", "账户", "用户"),
        "Add" to listOf("add", "plus", "new", "添加", "新建"),
        "AirplanemodeActive" to listOf("airplane", "flight", "travel", "飞机", "旅行"),
        "Apartment" to listOf("apartment", "building", "home", "公寓", "建筑"),
        "Archive" to listOf("archive", "box", "save", "归档", "保存"),
        "Bookmark" to listOf("bookmark", "save", "favorite", "书签", "收藏"),
        "Brush" to listOf("brush", "paint", "draw", "画笔", "绘画"),
        "Business" to listOf("business", "work", "office", "商务", "工作", "办公"),
        "Calculate" to listOf("calculate", "math", "compute", "计算", "数学"),
        "CalendarMonth" to listOf("calendar", "month", "date", "日历", "日期"),
        "Call" to listOf("call", "phone", "电话", "呼叫"),
        "CameraAlt" to listOf("camera", "photo", "相机", "拍照"),
        "Check" to listOf("check", "done", "ok", "完成", "确认"),
        "CheckCircle" to listOf("check", "circle", "success", "成功", "确认"),
        "Close" to listOf("close", "dismiss", "x", "关闭"),
        "Code" to listOf("code", "programming", "developer", "代码", "编程", "开发"),
        "Computer" to listOf("computer", "desktop", "pc", "电脑", "台式"),
        "DarkMode" to listOf("dark", "mode", "night", "暗色", "夜晚"),
        "Delete" to listOf("delete", "remove", "trash", "删除", "移除"),
        "Done" to listOf("done", "check", "complete", "完成"),
        "Download" to listOf("download", "save", "下载"),
        "Edit" to listOf("edit", "modify", "编辑", "修改"),
        "Email" to listOf("email", "mail", "邮件", "邮箱"),
        "Event" to listOf("event", "calendar", "事件", "日历"),
        "Explore" to listOf("explore", "compass", "discover", "探索", "发现"),
        "Favorite" to listOf("favorite", "heart", "like", "喜欢", "爱心"),
        "FitnessCenter" to listOf("fitness", "gym", "exercise", "运动", "健身", "力量"),
        "Flag" to listOf("flag", "priority", "标记", "优先级"),
        "Flight" to listOf("flight", "airplane", "travel", "飞机", "旅行"),
        "Folder" to listOf("folder", "directory", "文件夹", "目录", "项目"),
        "Group" to listOf("group", "people", "team", "群组", "团队"),
        "Headphones" to listOf("headphones", "audio", "music", "耳机", "音乐"),
        "Home" to listOf("home", "house", "家", "首页"),
        "Image" to listOf("image", "photo", "picture", "图片", "照片"),
        "Info" to listOf("info", "information", "信息"),
        "Key" to listOf("key", "password", "钥匙", "密码"),
        "Laptop" to listOf("laptop", "computer", "笔记本", "电脑"),
        "LightMode" to listOf("light", "mode", "sun", "亮色", "太阳"),
        "Link" to listOf("link", "url", "链接"),
        "LocalCafe" to listOf("cafe", "coffee", "咖啡"),
        "LocalDining" to listOf("restaurant", "food", "dining", "餐厅", "吃饭", "做饭", "食物"),
        "Lock" to listOf("lock", "secure", "锁", "安全"),
        "Map" to listOf("map", "地图"),
        "Menu" to listOf("menu", "hamburger", "菜单"),
        "Mic" to listOf("mic", "microphone", "麦克风"),
        "MusicNote" to listOf("music", "note", "音乐", "音符"),
        "Notifications" to listOf("notification", "bell", "通知", "提醒", "告警"),
        "Person" to listOf("person", "user", "用户", "个人"),
        "Phone" to listOf("phone", "call", "电话"),
        "Photo" to listOf("photo", "image", "照片", "图片"),
        "PlayArrow" to listOf("play", "start", "播放", "开始"),
        "PriorityHigh" to listOf("priority", "high", "urgent", "优先级", "紧急", "重要"),
        "Public" to listOf("public", "world", "globe", "公共", "世界"),
        "Refresh" to listOf("refresh", "reload", "刷新", "重载", "同步"),
        "Restaurant" to listOf("restaurant", "food", "dining", "餐厅", "吃饭", "做饭", "食物"),
        "School" to listOf("school", "education", "学校", "学习", "教育", "课程"),
        "Search" to listOf("search", "find", "搜索", "查找"),
        "Security" to listOf("security", "shield", "安全"),
        "SelfImprovement" to listOf("self", "improvement", "meditation", "yoga", "冥想", "瑜伽", "放松"),
        "Settings" to listOf("settings", "gear", "设置", "齿轮"),
        "Share" to listOf("share", "分享"),
        "ShoppingCart" to listOf("shopping", "cart", "购物", "必买"),
        "Smartphone" to listOf("smartphone", "phone", "手机"),
        "Spa" to listOf("spa", "relax", "wellness", "放松", "养生"),
        "Star" to listOf("star", "favorite", "rating", "星", "收藏", "评价"),
        "Stop" to listOf("stop", "halt", "停止"),
        "Store" to listOf("store", "shop", "商店", "购物"),
        "Sync" to listOf("sync", "refresh", "同步", "刷新"),
        "Timer" to listOf("timer", "clock", "计时器", "时间"),
        "Work" to listOf("work", "briefcase", "工作", "公文包"),
    )

    private val EMOJI_DATA: List<Triple<String, String, List<String>>> = listOf(
        Triple("😀", "grinning", listOf("笑脸", "开心", "grin", "smile", "happy")),
        Triple("😂", "joy", listOf("笑哭", "笑死了", "laugh", "cry")),
        Triple("😍", "heart_eyes", listOf("花痴", "爱心眼", "love", "heart")),
        Triple("🤔", "thinking", listOf("思考", "想想", "think", "hmm")),
        Triple("👍", "thumbsup", listOf("赞", "好的", "like", "thumbs", "up")),
        Triple("❤️", "red_heart", listOf("爱心", "爱", "love", "heart", "red")),
        Triple("🔥", "fire", listOf("火", "厉害", "fire", "hot")),
        Triple("🎉", "tada", listOf("庆祝", "派对", "party", "celebrate")),
        Triple("✨", "sparkles", listOf("闪亮", "特效", "sparkle", "shine", "magic")),
        Triple("💯", "hundred", listOf("满分", "一百", "perfect", "100")),
        Triple("💪", "muscle", listOf("加油", "力量", "strong", "muscle")),
        Triple("👋", "wave", listOf("你好", "再见", "hi", "bye", "wave")),
        Triple("👏", "clap", listOf("鼓掌", "棒", "clap", "applause")),
        Triple("😴", "sleeping", listOf("睡觉", "困", "sleep", "zzz", "睡眠")),
        Triple("⭐", "star", listOf("星", "星星", "star")),
        Triple("📚", "books", listOf("书", "学习", "books", "study", "复习")),
        Triple("💻", "computer", listOf("电脑", "computer", "laptop", "编程", "代码")),
        Triple("🎵", "musical_note", listOf("音乐", "music", "note")),
        Triple("🏃", "runner", listOf("跑步", "运动", "run", "exercise", "有氧")),
        Triple("🧘", "meditation", listOf("冥想", "瑜伽", "meditation", "yoga", "放松")),
        Triple("🎮", "game", listOf("游戏", "game", "play", "竞技")),
        Triple("🎬", "movie", listOf("电影", "movie", "film")),
        Triple("📝", "memo", listOf("备忘", "笔记", "memo", "note", "write", "待办")),
        Triple("☕", "coffee", listOf("咖啡", "coffee", "tea")),
        Triple("🍕", "pizza", listOf("披萨", "pizza", "food", "食物")),
        Triple("🏠", "house", listOf("家", "house", "home")),
        Triple("🚗", "car", listOf("车", "汽车", "car")),
        Triple("✈️", "airplane", listOf("飞机", "airplane", "travel")),
        Triple("🌙", "crescent_moon", listOf("月亮", "夜晚", "moon", "night")),
        Triple("☀️", "sun", listOf("太阳", "晴天", "sun", "sunny")),
        Triple("🌈", "rainbow", listOf("彩虹", "rainbow")),
        Triple("🔔", "bell", listOf("铃铛", "通知", "bell", "notification", "提醒", "紧急")),
        Triple("⚡", "zap", listOf("闪电", "紧急", "lightning", "urgent", "fast")),
        Triple("🎯", "target", listOf("目标", "靶子", "target", "goal")),
        Triple("📌", "pin", listOf("图钉", "标记", "pin", "mark")),
        Triple("🏋️", "weightlifter", listOf("举重", "健身", "力量", "weightlifting", "gym")),
        Triple("🎨", "art", listOf("艺术", "绘画", "art", "paint")),
        Triple("📱", "mobile", listOf("手机", "mobile", "phone")),
        Triple("🍳", "cooking", listOf("做饭", "烹饪", "cooking", "food", "食物")),
        Triple("🎓", "graduation", listOf("毕业", "学位", "学习", "graduation", "education")),
        Triple("💼", "briefcase", listOf("公文包", "工作", "work", "business")),
        Triple("🏆", "trophy", listOf("奖杯", "胜利", "trophy", "win", "竞技")),
        Triple("📂", "folder", listOf("文件夹", "目录", "folder", "project", "项目")),
        Triple("🛒", "shopping_cart", listOf("购物车", "购物", "shopping", "必买")),
    )
}
