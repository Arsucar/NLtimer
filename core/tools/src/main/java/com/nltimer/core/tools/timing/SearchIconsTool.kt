package com.nltimer.core.tools.timing

import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ParameterType
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolDocumentation
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolParameter
import com.nltimer.core.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class SearchIconsTool @Inject constructor(
    private val missLog: IconSearchMissLog,
) : ToolDefinition {

    override val name: String = "searchIcons"
    override val description: String = "搜索可用图标，支持中英文关键词，返回 iconKey 列表供 createActivity/createTag/bulkUpdateActivities 使用"
    override val category: ToolCategory = ToolCategory.SEARCH
    override val accessLevel: AccessLevel = AccessLevel.NONE
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "query",
            description = "搜索关键词（中英文均可，如 睡觉/sleep/时间/time/工作/work）",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "library",
            description = "图标库过滤：hi (HugeIcons) / mi (Material Icons) / emoji；默认返回所有",
            type = ParameterType.STRING,
            required = false,
            constraints = null,
        ),
        ToolParameter(
            name = "limit",
            description = "返回数量上限，默认 20，最大 50",
            type = ParameterType.NUMBER,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val query = (args["query"] as? String)?.trim()
            ?: return ToolResult.Error(name, ToolError.ValidationError("query 必填"))
        val library = (args["library"] as? String)?.lowercase()?.trim()
        val limit = ((args["limit"] as? Number)?.toInt() ?: 20).coerceIn(1, 50)

        val results = JSONArray()
        val q = query.lowercase()

        if (library == null || library == "hi") {
            searchHugeIcons(q, limit).forEach { results.put(it) }
        }
        if (library == null || library == "mi") {
            searchMaterialIcons(q, limit).forEach { results.put(it) }
        }
        if (library == null || library == "emoji") {
            searchEmoji(q, limit).forEach { results.put(it) }
        }

        if (results.length() == 0) {
            missLog.record(query, library)
            return ToolResult.Success(name, """{"matches":[],"missLogged":true,"placeholderIconKey":"⁉","placeholderNote":"未找到匹配图标，已记录此需求（累计未满足${missLog.count()}条）。建议先使用 ⁉ 占位，后续版本更新图标库后可批量替换","suggestion":"请尝试其他关键词或缩短查询"}""")
        }

        val capped = JSONArray()
        val total = minOf(results.length(), limit)
        for (i in 0 until total) {
            capped.put(results.getJSONObject(i))
        }

        val result = JSONObject().apply {
            put("query", query)
            put("totalMatches", results.length())
            put("returned", total)
            put("matches", capped)
        }
        return ToolResult.Success(name, result.toString())
    }

    private fun searchHugeIcons(query: String, limit: Int): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        for ((name, keywords) in HUGE_ICON_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(JSONObject().apply {
                    put("iconKey", "hi:$name")
                    put("name", name)
                    put("library", "hi")
                    put("keywords", keywords.joinToString(","))
                })
                if (results.size >= limit) break
            }
        }
        return results
    }

    private fun searchMaterialIcons(query: String, limit: Int): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        for ((name, keywords) in MATERIAL_ICON_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(JSONObject().apply {
                    put("iconKey", "mi:filled:$name")
                    put("name", name)
                    put("library", "mi")
                    put("keywords", keywords.joinToString(","))
                })
                if (results.size >= limit) break
            }
        }
        return results
    }

    private fun searchEmoji(query: String, limit: Int): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        for ((emoji, name, keywords) in EMOJI_DATA) {
            if (name.contains(query, true) || keywords.any { it.contains(query, true) }) {
                results.add(JSONObject().apply {
                    put("iconKey", emoji)
                    put("name", name)
                    put("library", "emoji")
                    put("keywords", keywords.joinToString(","))
                })
                if (results.size >= limit) break
            }
        }
        return results
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "query": "sleep",
              "totalMatches": 3,
              "returned": 3,
              "matches": [
                {"iconKey":"hi:Moon","name":"Moon","library":"hi","keywords":"moon,night,月亮,夜晚"},
                {"iconKey":"mi:filled:NightsStay","name":"NightsStay","library":"mi","keywords":"night,dark"},
                {"iconKey":"😴","name":"sleeping","library":"emoji","keywords":"睡觉,困,sleep,zzz"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "query 必填", "未传搜索关键词"),
        ),
        usageExamples = listOf(
            """searchIcons(query="睡觉")""",
            """searchIcons(query="work", library="hi")""",
            """searchIcons(query="时间", limit=10)""",
        ),
    )

    companion object {
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
            "Notification01" to listOf("notification", "bell", "通知", "提醒"),
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
            "AccessAlarms" to listOf("alarm", "clock", "time"),
            "AccessTime" to listOf("time", "clock", "hour"),
            "AccountCircle" to listOf("account", "user", "profile"),
            "Add" to listOf("add", "plus", "new"),
            "AirplanemodeActive" to listOf("airplane", "flight", "travel"),
            "Apartment" to listOf("apartment", "building", "home"),
            "Archive" to listOf("archive", "box", "save"),
            "Bookmark" to listOf("bookmark", "save", "favorite"),
            "Brush" to listOf("brush", "paint", "draw"),
            "Business" to listOf("business", "work", "office"),
            "Calculate" to listOf("calculate", "math", "compute"),
            "CalendarMonth" to listOf("calendar", "month", "date"),
            "Call" to listOf("call", "phone"),
            "CameraAlt" to listOf("camera", "photo"),
            "Check" to listOf("check", "done", "ok"),
            "CheckCircle" to listOf("check", "circle", "success"),
            "Close" to listOf("close", "dismiss", "x"),
            "Code" to listOf("code", "programming", "developer"),
            "Computer" to listOf("computer", "desktop", "pc"),
            "DarkMode" to listOf("dark", "mode", "night"),
            "Delete" to listOf("delete", "remove", "trash"),
            "Done" to listOf("done", "check", "complete"),
            "Download" to listOf("download", "save"),
            "Edit" to listOf("edit", "modify"),
            "Email" to listOf("email", "mail"),
            "Event" to listOf("event", "calendar"),
            "Explore" to listOf("explore", "compass", "discover"),
            "Favorite" to listOf("favorite", "heart", "like"),
            "FitnessCenter" to listOf("fitness", "gym", "exercise", "运动", "健身"),
            "Flight" to listOf("flight", "airplane", "travel"),
            "Folder" to listOf("folder", "directory"),
            "Group" to listOf("group", "people", "team"),
            "Headphones" to listOf("headphones", "audio", "music"),
            "Home" to listOf("home", "house"),
            "Image" to listOf("image", "photo", "picture"),
            "Info" to listOf("info", "information"),
            "Key" to listOf("key", "password"),
            "Laptop" to listOf("laptop", "computer"),
            "LightMode" to listOf("light", "mode", "sun"),
            "Link" to listOf("link", "url"),
            "LocalCafe" to listOf("cafe", "coffee", "咖啡"),
            "LocalDining" to listOf("restaurant", "food", "dining", "餐厅", "吃饭"),
            "Lock" to listOf("lock", "secure"),
            "Map" to listOf("map"),
            "Menu" to listOf("menu", "hamburger"),
            "Mic" to listOf("mic", "microphone"),
            "MusicNote" to listOf("music", "note"),
            "Notifications" to listOf("notification", "bell"),
            "Person" to listOf("person", "user"),
            "Phone" to listOf("phone", "call"),
            "Photo" to listOf("photo", "image"),
            "PlayArrow" to listOf("play", "start"),
            "Public" to listOf("public", "world", "globe"),
            "Refresh" to listOf("refresh", "reload"),
            "Restaurant" to listOf("restaurant", "food", "dining", "餐厅", "吃饭"),
            "School" to listOf("school", "education", "学校", "学习"),
            "Search" to listOf("search", "find"),
            "Security" to listOf("security", "shield", "安全"),
            "Settings" to listOf("settings", "gear", "设置"),
            "Share" to listOf("share", "分享"),
            "ShoppingCart" to listOf("shopping", "cart", "购物"),
            "Smartphone" to listOf("smartphone", "phone", "手机"),
            "Star" to listOf("star", "favorite", "rating"),
            "Stop" to listOf("stop", "halt"),
            "Store" to listOf("store", "shop"),
            "Sync" to listOf("sync", "refresh", "同步"),
            "Timer" to listOf("timer", "clock", "计时器", "时间"),
            "Work" to listOf("work", "briefcase", "工作"),
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
            Triple("📚", "books", listOf("书", "学习", "books", "study")),
            Triple("💻", "computer", listOf("电脑", "computer", "laptop")),
            Triple("🎵", "musical_note", listOf("音乐", "music", "note")),
            Triple("🏃", "runner", listOf("跑步", "运动", "run", "exercise")),
            Triple("🧘", "meditation", listOf("冥想", "瑜伽", "meditation", "yoga")),
            Triple("🎮", "game", listOf("游戏", "game", "play")),
            Triple("🎬", "movie", listOf("电影", "movie", "film")),
            Triple("📝", "memo", listOf("备忘", "笔记", "memo", "note", "write")),
            Triple("☕", "coffee", listOf("咖啡", "coffee", "tea")),
            Triple("🍕", "pizza", listOf("披萨", "pizza", "food")),
            Triple("🏠", "house", listOf("家", "house", "home")),
            Triple("🚗", "car", listOf("车", "汽车", "car")),
            Triple("✈️", "airplane", listOf("飞机", "airplane", "travel")),
            Triple("🌙", "crescent_moon", listOf("月亮", "夜晚", "moon", "night")),
            Triple("☀️", "sun", listOf("太阳", "晴天", "sun", "sunny")),
            Triple("🌈", "rainbow", listOf("彩虹", "rainbow")),
        )
    }
}
