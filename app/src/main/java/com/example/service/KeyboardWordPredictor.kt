package com.example.service

import java.util.Locale

data class WordSuggestionResult(
    val suggestions: List<String>,
    val autoCorrect: String? = null
)

/**
 * Fast, fully offline Gboard-style word predictor & auto-correct engine for
 * English and Thai built directly into NexusPad's virtual keyboard.
 */
object KeyboardWordPredictor {

    private val englishTypoMap: Map<String, String> = mapOf(
        "teh" to "the",
        "hte" to "the",
        "adn" to "and",
        "nad" to "and",
        "taht" to "that",
        "thta" to "that",
        "waht" to "what",
        "whta" to "what",
        "wiht" to "with",
        "wtih" to "with",
        "becuase" to "because",
        "beacuse" to "because",
        "bcuz" to "because",
        "recieve" to "receive",
        "freind" to "friend",
        "frend" to "friend",
        "definately" to "definitely",
        "definitly" to "definitely",
        "seperate" to "separate",
        "thier" to "their",
        "youre" to "you're",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "im" to "I'm",
        "ive" to "I've",
        "id" to "I'd",
        "ill" to "I'll",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "wasnt" to "wasn't",
        "arent" to "aren't",
        "thats" to "that's",
        "whats" to "what's",
        "hes" to "he's",
        "shes" to "she's",
        "theres" to "there's",
        "lets" to "let's",
        "pls" to "please",
        "plz" to "please",
        "thx" to "thanks",
        "thnks" to "thanks",
        "thnaks" to "thanks",
        "helo" to "hello",
        "hllo" to "hello",
        "gmae" to "game",
        "gaem" to "game",
        "paly" to "play",
        "palying" to "playing",
        "playng" to "playing",
        "helpp" to "help",
        "wating" to "waiting",
        "comming" to "coming",
        "sorrry" to "sorry",
        "sory" to "sorry",
        "tomorow" to "tomorrow",
        "tommorow" to "tomorrow",
        "awsome" to "awesome",
        "awseome" to "awesome",
        "realy" to "really",
        "relly" to "really",
        "alredy" to "already",
        "messaeg" to "message",
        "keybord" to "keyboard",
        "contorller" to "controller",
        "controler" to "controller",
        "mincraft" to "minecraft",
        "robolx" to "roblox",
        "fortntie" to "fortnite",
        "valornt" to "valorant",
        "gefroce" to "geforce",
        "overaly" to "overlay",
        "setings" to "settings",
        "upadte" to "update",
        "recoridng" to "recording",
        "screnshot" to "screenshot",
        "yuo" to "you",
        "yuor" to "your",
        "jsut" to "just",
        "kwno" to "know",
        "konw" to "know",
        "liek" to "like",
        "tiem" to "time",
        "godo" to "good",
        "goood" to "good",
        "raedy" to "ready",
        "redy" to "ready",
        "sqaud" to "squad",
        "temmate" to "teammate",
        "enmey" to "enemy",
        "atack" to "attack",
        "defned" to "defend",
        "sheild" to "shield",
        "healt" to "health",
        "wepon" to "weapon",
        "buidl" to "build",
        "mvoe" to "move",
        "jmup" to "jump",
        "cruch" to "crouch",
        "sprnit" to "sprint",
        "relod" to "reload",
        "serach" to "search",
        "connet" to "connect",
        "servre" to "server"
    )

    // Ordered roughly by conversational + gaming frequency
    private val englishDictionary: List<String> = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I", "it", "for", "not", "on",
        "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we",
        "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their",
        "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when",
        "make", "can", "like", "time", "no", "just", "him", "know", "take", "people", "into",
        "year", "your", "good", "some", "could", "them", "see", "other", "than", "then", "now",
        "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two",
        "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any",
        "these", "give", "day", "most", "us", "hello", "hey", "hi", "thanks", "thank", "please",
        "sorry", "okay", "yes", "yeah", "nope", "sure", "maybe", "really", "actually", "already",
        "always", "never", "still", "again", "right", "left", "here", "where", "why", "going",
        "coming", "waiting", "wait", "help", "need", "ready", "start", "stop", "play", "playing",
        "player", "players", "game", "games", "gaming", "win", "winning", "lose", "gg", "nice", "great", "awesome",
        "amazing", "cool", "best", "better", "fast", "slow", "lag", "ping", "fps", "stream",
        "cloud", "server", "lobby", "match", "round", "team", "squad", "party", "friend", "friends",
        "invite", "join", "follow", "behind", "front", "cover", "push", "hold", "defend", "attack",
        "enemy", "enemies", "boss", "loot", "ammo", "heal", "health", "shield", "weapon", "item",
        "build", "craft", "mine", "jump", "crouch", "sprint", "reload", "aim", "shoot", "scope",
        "map", "zone", "safe", "drop", "spawn", "respawn", "checkpoint", "save", "load", "menu",
        "settings", "controls", "layout", "profile", "overlay", "filter", "crosshair", "gallery",
        "record", "recording", "replay", "screenshot", "capture", "update", "version", "download",
        "install", "keyboard", "mouse", "trackpad", "controller", "gamepad", "button", "touch",
        "screen", "color", "theme", "font", "bubble", "custom", "default", "reset", "change",
        "switch", "toggle", "enable", "disable", "open", "close", "show", "hide", "move", "drag",
        "geforce", "nvidia", "minecraft", "roblox", "fortnite", "valorant", "apex", "cyberpunk",
        "genshin", "steam", "epic", "discord", "youtube", "github", "android", "phone", "tablet",
        "today", "tonight", "tomorrow", "morning", "night", "later", "soon", "minute", "second",
        "hour", "before", "while", "during", "around", "through", "between", "under", "above",
        "something", "nothing", "everything", "someone", "everyone", "anyone", "somewhere",
        "probably", "definitely", "finally", "almost", "enough", "check", "watch", "listen",
        "send", "message", "chat", "voice", "mic", "mute", "audio", "sound", "video", "photo",
        "picture", "image", "background", "smooth", "clean", "sharp", "bright", "dark", "light",
        "receive", "separate", "remember", "forget", "understand", "explain", "question", "answer"
    )

    private val englishDictSet: Set<String> = englishDictionary.map { it.lowercase(Locale.ROOT) }.toSet()

    private val englishBigrams: Map<String, List<String>> = mapOf(
        "i" to listOf("am", "have", "need"),
        "i'm" to listOf("ready", "coming", "playing"),
        "how" to listOf("are", "do", "to"),
        "are" to listOf("you", "we", "they"),
        "you" to listOf("are", "can", "ready"),
        "thank" to listOf("you", "so", "god"),
        "thanks" to listOf("for", "a", "bro"),
        "good" to listOf("game", "luck", "job"),
        "let's" to listOf("go", "play", "win"),
        "want" to listOf("to", "some", "more"),
        "need" to listOf("help", "to", "ammo"),
        "going" to listOf("to", "in", "back"),
        "wait" to listOf("for", "a", "here"),
        "come" to listOf("here", "with", "on"),
        "see" to listOf("you", "that", "him"),
        "on" to listOf("my", "the", "it"),
        "in" to listOf("the", "a", "game"),
        "ready" to listOf("to", "for", "now"),
        "can" to listOf("you", "we", "I"),
        "do" to listOf("you", "not", "it"),
        "don't" to listOf("worry", "know", "push"),
        "please" to listOf("wait", "help", "join"),
        "nice" to listOf("shot", "job", "play"),
        "my" to listOf("bad", "team", "game"),
        "no" to listOf("problem", "way", "worries"),
        "gg" to listOf("well", "nice", "everyone")
    )

    private val thaiDictionary: List<String> = listOf(
        "สวัสดี", "ขอบคุณ", "ครับ", "ค่ะ", "ได้", "ไม่ได้", "เล่น", "เกม", "รอ", "เดี๋ยว", "แป๊บ",
        "มา", "ไป", "ช่วย", "ด้วย", "หน่อย", "โอเค", "เก่ง", "มาก", "เลย", "นะ", "จ้า", "เพื่อน",
        "ทีม", "ชนะ", "แพ้", "พร้อม", "แล้ว", "กำลัง", "อยู่", "ไหน", "อะไร", "ทำไม", "เมื่อไหร่",
        "วันนี้", "พรุ่งนี้", "สนุก", "สวย", "ดี", "เยี่ยม", "สุดยอด", "ขอโทษ", "ไม่เป็นไร", "เข้าใจ",
        "ตกลง", "เริ่ม", "หยุด", "เปิด", "ปิด", "ตั้งค่า", "หน้าจอ", "บันทึก", "ภาพ", "วิดีโอ",
        "คีย์บอร์ด", "เมาส์", "จอย", "ปุ่ม", "สี", "ฟอนต์", "ภาษา", "ไทย", "อังกฤษ", "อัปเดต",
        "เวอร์ชัน", "ใหม่", "เก่า", "ระบบ", "ทดสอบ", "พื้นหลัง", "เลือกรูป", "ขนาด", "ความชัด",
        "ลื่น", "กระตุก", "เน็ต", "ปิง", "เซิร์ฟเวอร์", "ห้อง", "ชวน", "เข้า", "ออก", "บุก", "กัน",
        "ระวัง", "ศัตรู", "ข้างหลัง", "ข้างหน้า", "ซ้าย", "ขวา", "บน", "ล่าง", "ตรงนี้", "ตรงนั้น",
        "เก็บ", "ของ", "ยา", "เกราะ", "ปืน", "กระสุน", "กระโดด", "วิ่ง", "ยิง", "เล็ง", "รีโหลด",
        "สร้าง", "ขุด", "บ้าน", "แผนที่", "ภารกิจ", "ชนะแล้ว", "สู้ๆ", "ยินดี", "รัก", "คิดถึง",
        "กินข้าว", "นอน", "ทำงาน", "เรียน", "กลับบ้าน", "เจอ", "คุย", "ส่ง", "ข้อความ", "แชท"
    )

    private val thaiBigrams: Map<String, List<String>> = mapOf(
        "สวัสดี" to listOf("ครับ", "ค่ะ", "เพื่อน"),
        "ขอบคุณ" to listOf("ครับ", "ค่ะ", "มาก"),
        "ขอโทษ" to listOf("ครับ", "ค่ะ", "นะ"),
        "ช่วย" to listOf("ด้วย", "หน่อย", "กัน"),
        "รอ" to listOf("แป๊บ", "เดี๋ยว", "ด้วย"),
        "เล่น" to listOf("เกม", "ด้วย", "กัน"),
        "พร้อม" to listOf("แล้ว", "ลุย", "ยัง"),
        "กำลัง" to listOf("ไป", "มา", "เล่น"),
        "อยู่" to listOf("ไหน", "ตรงนี้", "บ้าน"),
        "ไม่เป็นไร" to listOf("ครับ", "ค่ะ", "นะ"),
        "เก่ง" to listOf("มาก", "เลย", "สุดยอด"),
        "สนุก" to listOf("มาก", "เลย", "ดี")
    )

    fun predict(
        currentWord: String,
        previousWord: String = "",
        isThai: Boolean = false
    ): WordSuggestionResult {
        val trimmed = currentWord.trim()
        val prevLower = previousWord.trim().lowercase(Locale.ROOT)

        if (trimmed.isEmpty()) {
            val nextWords = if (isThai) {
                thaiBigrams[previousWord.trim()] ?: listOf("สวัสดี", "ขอบคุณ", "พร้อม")
            } else {
                englishBigrams[prevLower] ?: listOf("I", "Hey", "Ready")
            }
            return WordSuggestionResult(suggestions = nextWords.take(3), autoCorrect = null)
        }

        return if (isThai) {
            predictThai(trimmed)
        } else {
            predictEnglish(trimmed)
        }
    }

    private fun predictThai(query: String): WordSuggestionResult {
        val exact = thaiDictionary.firstOrNull { it == query }
        val prefixMatches = thaiDictionary.filter { it.startsWith(query) && it != query }
        val containsMatches = if (prefixMatches.size < 3) {
            thaiDictionary.filter { it.contains(query) && !it.startsWith(query) }
        } else {
            emptyList()
        }
        val combined = buildList {
            if (exact != null) add(exact)
            addAll(prefixMatches)
            addAll(containsMatches)
        }.distinct().take(3)

        val fallback = if (combined.isEmpty()) listOf(query, "ครับ", "ค่ะ") else combined
        return WordSuggestionResult(suggestions = fallback.take(3), autoCorrect = null)
    }

    private fun predictEnglish(query: String): WordSuggestionResult {
        val lower = query.lowercase(Locale.ROOT)
        val isAllCaps = query.length > 1 && query.all { !it.isLetter() || it.isUpperCase() }
        val isCapitalized = query.firstOrNull()?.isUpperCase() == true

        fun applyCase(word: String): String = when {
            isAllCaps -> word.uppercase(Locale.ROOT)
            isCapitalized -> word.replaceFirstChar { it.uppercase(Locale.ROOT) }
            else -> word
        }

        // 1. Direct high-confidence typo match
        val mappedTypo = englishTypoMap[lower]
        if (mappedTypo != null) {
            val corrected = if (mappedTypo.startsWith("I'")) mappedTypo else applyCase(mappedTypo)
            val prefixExtras = englishDictionary
                .filter { it.lowercase(Locale.ROOT).startsWith(lower) && !it.equals(mappedTypo, ignoreCase = true) }
                .take(2)
                .map(::applyCase)
            val list = (listOf(corrected) + prefixExtras + listOf(query)).distinct().take(3)
            return WordSuggestionResult(suggestions = list, autoCorrect = corrected)
        }

        // 2. Prefix completion from dictionary
        val exactInDict = lower in englishDictSet
        val prefixMatches = englishDictionary
            .filter { it.lowercase(Locale.ROOT).startsWith(lower) }
            .map(::applyCase)

        if (prefixMatches.isNotEmpty()) {
            val ordered = if (exactInDict) {
                (listOf(applyCase(lower)) + prefixMatches).distinct().take(3)
            } else {
                prefixMatches.distinct().take(3)
            }
            return WordSuggestionResult(suggestions = ordered, autoCorrect = null)
        }

        // 3. Fuzzy edit-distance-1 or adjacent transposition auto-correction for >= 3-letter words
        if (lower.length >= 3) {
            val fuzzyMatches = englishDictionary
                .asSequence()
                .filter { kotlin.math.abs(it.length - lower.length) <= 1 }
                .mapNotNull { candidate ->
                    val dist = damerauDistance1(lower, candidate.lowercase(Locale.ROOT))
                    if (dist <= 1) candidate to dist else null
                }
                .sortedBy { it.second }
                .map { applyCase(it.first) }
                .distinct()
                .take(3)
                .toList()

            if (fuzzyMatches.isNotEmpty()) {
                val topCorrection = fuzzyMatches.first()
                val suggestions = (fuzzyMatches + query).distinct().take(3)
                return WordSuggestionResult(suggestions = suggestions, autoCorrect = topCorrection)
            }
        }

        return WordSuggestionResult(suggestions = listOf(query), autoCorrect = null)
    }

    /**
     * Fast optimal-string-alignment distance capped at 2 (returns 0, 1, or 2+).
     * Detects single substitution, insertion, deletion, or adjacent character transposition.
     */
    private fun damerauDistance1(a: String, b: String): Int {
        if (a == b) return 0
        val lenA = a.length
        val lenB = b.length
        if (kotlin.math.abs(lenA - lenB) > 1) return 2

        if (lenA == lenB) {
            var firstDiff = -1
            var diffs = 0
            for (i in 0 until lenA) {
                if (a[i] != b[i]) {
                    diffs++
                    if (diffs == 1) {
                        firstDiff = i
                    } else if (diffs == 2) {
                        // Adjacent transposition check
                        if (i == firstDiff + 1 && a[firstDiff] == b[i] && a[i] == b[firstDiff]) {
                            continue
                        }
                        return 2
                    } else {
                        return 2
                    }
                }
            }
            return if (diffs in 1..2) 1 else 0
        }

        // Length differs by 1: single insertion or deletion
        val shorter = if (lenA < lenB) a else b
        val longer = if (lenA < lenB) b else a
        var i = 0
        var j = 0
        var skipped = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else if (!skipped) {
                skipped = true
                j++
            } else {
                return 2
            }
        }
        return 1
    }
}
