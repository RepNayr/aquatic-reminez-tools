package com.repp.aquaticremineztools.data

/**
 * Kumpulan semua tools (108) di Aquatic Reminez Tools.
 */
object ToolData {

    enum class Category(val displayName: String, val emoji: String) {
        UTILITY("Utility", "🔧"),
        SECURITY_GEN("Security & Generate", "🔐"),
        FILE("File & System", "📁"),
        NETWORK("Network", "🌐"),
        MEDIA("Media", "🎨"),
        DEV("Dev Tools", "🧰"),
        FORENSIC("Forensic", "🔍"),
        SECURITY("Security", "🛡️"),
        OFFENSIVE("Offensive", "🔴"),
        DEFENSIVE("Defensive", "🛡️"),
        GAMES("Games", "🎮"),
    }

    data class Tool(
        val id: String,
        val name: String,
        val category: Category,
        val emoji: String,
        val description: String,
    )

    val ALL_TOOLS: List<Tool> = listOf(
        // ============ UTILITY DASAR (8) ============
        Tool("util_calculator", "Calculator Scientific", Category.UTILITY, "🧮", "Advanced scientific calculator"),
        Tool("util_unit_converter", "Unit Converter", Category.UTILITY, "📏", "Convert various units"),
        Tool("util_notes", "Notes", Category.UTILITY, "📝", "Notes + save to local"),
        Tool("util_todo", "To-Do List", Category.UTILITY, "✅", "Task list + reminder"),
        Tool("util_stopwatch", "Stopwatch & Timer", Category.UTILITY, "⏱️", "Stopwatch + countdown"),
        Tool("util_compass", "Compass", Category.UTILITY, "🧭", "Direction using magnetic sensor"),
        Tool("util_currency", "Currency Converter", Category.UTILITY, "💱", "Realtime currency conversion"),
        Tool("util_world_clock", "World Clock", Category.UTILITY, "🕐", "Multiple timezone clock"),

        // ============ SECURITY & GENERATE (10) ============
        Tool("secgen_password", "Password Generator", Category.SECURITY_GEN, "🔑", "Strong password generator"),
        Tool("secgen_text_enc", "Text Encryptor", Category.SECURITY_GEN, "🔒", "Encrypt/decrypt text AES"),
        Tool("secgen_hash", "Hash Generator", Category.SECURITY_GEN, "🔐", "MD5, SHA-1, SHA-256"),
        Tool("secgen_qr_gen", "QR Generator", Category.SECURITY_GEN, "📱", "Create QR code"),
        Tool("secgen_qr_scan", "QR Scanner", Category.SECURITY_GEN, "📷", "Scan QR code"),
        Tool("secgen_file_enc", "File Encryptor", Category.SECURITY_GEN, "🗄️", "Encrypt files"),
        Tool("secgen_file_dec", "File Decryptor", Category.SECURITY_GEN, "🔓", "Decrypt files"),
        Tool("secgen_barcode_gen", "Barcode Generator", Category.SECURITY_GEN, "▮▯", "Create barcode"),
        Tool("secgen_barcode_scan", "Barcode Scanner", Category.SECURITY_GEN, "📸", "Scan barcode"),
        Tool("secgen_secure_notes", "Secure Notes", Category.SECURITY_GEN, "📓", "Encrypted notes"),

        // ============ FILE & SYSTEM (9) ============
        Tool("file_manager", "File Manager", Category.FILE, "📂", "Browse & manage files"),
        Tool("file_shredder", "File Shredder", Category.FILE, "🗑️", "Permanent delete"),
        Tool("file_app_info", "App Info", Category.FILE, "📱", "Installed apps info"),
        Tool("file_storage_analyzer", "Storage Analyzer", Category.FILE, "💾", "Storage usage per folder"),
        Tool("file_compressor", "File Compressor", Category.FILE, "📦", "Compress ZIP/RAR/7z"),
        Tool("file_extractor", "File Extractor", Category.FILE, "📤", "Extract archive"),
        Tool("file_splitter", "File Splitter", Category.FILE, "✂️", "Split large files"),
        Tool("file_joiner", "File Joiner", Category.FILE, "🔗", "Join split files"),
        Tool("file_device_info", "Device Info", Category.FILE, "ℹ️", "Device specifications"),

        // ============ NETWORK (16) ============
        Tool("net_scanner", "Network Scanner", Category.NETWORK, "🔎", "Scan local network"),
        Tool("net_ping", "Ping Tool", Category.NETWORK, "📡", "Ping host + latency"),
        Tool("net_dns", "DNS Lookup", Category.NETWORK, "🌐", "DNS record lookup"),
        Tool("net_wifi_info", "WiFi Info", Category.NETWORK, "📶", "Connected WiFi details"),
        Tool("net_ip_info", "IP Info", Category.NETWORK, "🌍", "Public & local IP"),
        Tool("net_port_scanner", "Port Scanner", Category.NETWORK, "🚪", "Scan open ports"),
        Tool("net_whois", "WHOIS Lookup", Category.NETWORK, "📖", "Domain registration info"),
        Tool("net_subdomain", "Subdomain Finder", Category.NETWORK, "🔍", "Find subdomains"),
        Tool("net_http_header", "HTTP Header Grabber", Category.NETWORK, "📋", "Response header viewer"),
        Tool("net_traceroute", "Traceroute", Category.NETWORK, "🛤️", "Trace packet route"),
        Tool("net_speed_test", "Speed Test", Category.NETWORK, "⚡", "Download/upload speed"),
        Tool("net_wifi_scanner", "WiFi Scanner", Category.NETWORK, "📡", "Scan nearby WiFi"),
        Tool("net_wifi_analyzer", "WiFi Analyzer", Category.NETWORK, "📊", "WiFi channel analysis"),
        Tool("net_reverse_ip", "Reverse IP Lookup", Category.NETWORK, "🔄", "Find domain from IP"),
        Tool("net_http_request", "HTTP Request", Category.NETWORK, "🌐", "Custom GET/POST request"),
        Tool("net_bandwidth", "Bandwidth Monitor", Category.NETWORK, "📈", "Monitor data usage"),

        // ============ MEDIA (31) ============
        Tool("media_color_picker", "Color Picker", Category.MEDIA, "🎨", "Pick color from image"),
        Tool("media_image_compressor", "Image Compressor", Category.MEDIA, "🗜️", "Reduce image size"),
        Tool("media_image_converter", "Image Converter", Category.MEDIA, "🖼️", "PNG ↔ JPG ↔ WEBP"),
        Tool("media_image_hd", "Image HD Enhancer", Category.MEDIA, "✨", "Upscale 2x/4x/8x"),
        Tool("media_bg_remover", "Background Remover", Category.MEDIA, "🎭", "Remove background"),
        Tool("media_image_crop", "Image Cropper", Category.MEDIA, "✂️", "Crop image"),
        Tool("media_image_resize", "Image Resizer", Category.MEDIA, "📐", "Change dimensions"),
        Tool("media_image_rotate", "Image Rotator", Category.MEDIA, "🔄", "Rotate + flip"),
        Tool("media_watermark_add", "Watermark Adder", Category.MEDIA, "💧", "Add watermark"),
        Tool("media_ai_image_gen", "AI Image Generator", Category.MEDIA, "🤖", "Text to image"),
        Tool("media_ai_obj_remove", "AI Object Remover", Category.MEDIA, "🧹", "Remove objects"),
        Tool("media_image_filters", "Image Filters", Category.MEDIA, "🌈", "Grayscale, sepia, blur"),
        Tool("media_video_converter", "Video Converter", Category.MEDIA, "🎬", "MP4 ↔ MKV ↔ AVI"),
        Tool("media_video_hd", "Video HD Enhancer", Category.MEDIA, "✨", "Upscale video"),
        Tool("media_watermark_remove_video", "Watermark Remover Video", Category.MEDIA, "🎥", "Remove video watermark"),
        Tool("media_video_compressor", "Video Compressor", Category.MEDIA, "🗜️", "Reduce video size"),
        Tool("media_video_to_gif", "Video to GIF", Category.MEDIA, "🎞️", "Video → GIF"),
        Tool("media_video_trim", "Video Trimmer", Category.MEDIA, "✂️", "Trim video"),
        Tool("media_audio_extractor", "Audio Extractor", Category.MEDIA, "🎵", "Extract audio from video"),
        Tool("media_info", "Media Info", Category.MEDIA, "ℹ️", "Video/audio details"),
        Tool("media_player", "Media Player", Category.MEDIA, "▶️", "Lightweight player"),
        Tool("media_audio_converter", "Audio Converter", Category.MEDIA, "🎵", "MP3 ↔ WAV ↔ FLAC"),
        Tool("media_doc_converter", "Document Converter", Category.MEDIA, "📄", "TXT/DOCX → PDF"),
        Tool("media_tiktok_dl", "TikTok Downloader", Category.MEDIA, "🎵", "Download TikTok no watermark"),
        Tool("media_instagram_dl", "Instagram Downloader", Category.MEDIA, "📷", "Download Reels/Post"),
        Tool("media_facebook_dl", "Facebook Downloader", Category.MEDIA, "📘", "Download FB video"),
        Tool("media_youtube_dl", "YouTube Downloader", Category.MEDIA, "▶️", "Download YouTube"),
        Tool("media_spotify_dl", "Spotify Downloader", Category.MEDIA, "🎧", "Download Spotify"),
        Tool("media_twitter_dl", "Twitter Downloader", Category.MEDIA, "🐦", "Download X video"),
        Tool("media_pinterest_dl", "Pinterest Downloader", Category.MEDIA, "📌", "Download Pinterest"),
        Tool("media_yt_thumbnail", "YouTube Thumbnail Grabber", Category.MEDIA, "🖼️", "Get YT thumbnail"),

        // ============ DEV TOOLS (3) ============
        Tool("dev_terminal", "Terminal", Category.DEV, "💻", "Simple shell"),
        Tool("dev_regex", "Regex Tester", Category.DEV, "🔤", "Test regex pattern"),
        Tool("dev_lorem", "Lorem Ipsum Generator", Category.DEV, "📝", "Dummy text"),

        // ============ FORENSIC (2) ============
        Tool("forensic_apk_analyzer", "APK Analyzer", Category.FORENSIC, "📦", "APK manifest & permission"),
        Tool("forensic_archive", "Archive Inspector", Category.FORENSIC, "🗂️", "Inspect archive without extract"),

        // ============ SECURITY (4) ============
        Tool("security_xss", "XSS Payload Tester", Category.SECURITY, "🕷️", "XSS test (lab)"),
        Tool("security_sqli", "SQL Injection Tester", Category.SECURITY, "💉", "SQLi test (lab)"),
        Tool("security_enc_vault", "Encryption Vault", Category.SECURITY, "🔐", "Encrypted file vault"),
        Tool("security_steganography", "Steganography Tool", Category.SECURITY, "🖼️", "Hide message in image"),

        // ============ OFFENSIVE (6) ============
        Tool("off_ddos", "DDoS Tool", Category.OFFENSIVE, "💥", "HTTP flood (lab)"),
        Tool("off_rat", "RAT", Category.OFFENSIVE, "🖥️", "Remote access (lab)"),
        Tool("off_bruteforce", "Brute Force", Category.OFFENSIVE, "🔨", "Brute force (lab)"),
        Tool("off_exploit", "Exploit Framework", Category.OFFENSIVE, "💣", "Exploit modules"),
        Tool("off_bug_wa", "Bug WhatsApp", Category.OFFENSIVE, "📱", "WA bug info"),
        Tool("off_injection", "Injection Suite", Category.OFFENSIVE, "💉", "SQLi, CMD, NoSQL"),

        // ============ DEFENSIVE (11) ============
        Tool("def_intrusion", "Intrusion Detector", Category.DEFENSIVE, "🕵️", "Detect suspicious access"),
        Tool("def_breach", "Breach Checker", Category.DEFENSIVE, "🚨", "Email/password breach"),
        Tool("def_vuln_scan", "Vulnerability Scanner", Category.DEFENSIVE, "🔍", "Self vulnerability scan"),
        Tool("def_perm_audit", "App Permission Auditor", Category.DEFENSIVE, "🔐", "Audit app permissions"),
        Tool("def_malware", "Malware Scanner", Category.DEFENSIVE, "🦠", "Scan suspicious files"),
        Tool("def_secure_dns", "Secure DNS", Category.DEFENSIVE, "🌐", "Set secure DNS"),
        Tool("def_wifi_sec", "WiFi Security Checker", Category.DEFENSIVE, "📶", "Check WiFi safety"),
        Tool("def_phishing", "Phishing Link Checker", Category.DEFENSIVE, "🎣", "Check phishing link"),
        Tool("def_fake_gps", "Fake GPS", Category.DEFENSIVE, "📍", "Hide real location"),
        Tool("def_call_block", "Call/SMS Blocker", Category.DEFENSIVE, "📵", "Block spam calls"),
        Tool("def_screenshot_detect", "Screenshot Detector", Category.DEFENSIVE, "📸", "Detect screenshots"),

        // ============ GAMES (8) ============
        Tool("game_snake", "Snake", Category.GAMES, "🐍", "Classic snake game"),
        Tool("game_tetris", "Tetris", Category.GAMES, "🧱", "Block puzzle"),
        Tool("game_flappy", "Flappy Bird", Category.GAMES, "🐦", "Flappy bird clone"),
        Tool("game_tictactoe", "Tic Tac Toe", Category.GAMES, "⭕", "X vs O"),
        Tool("game_memory", "Memory Game", Category.GAMES, "🧠", "Card matching"),
        Tool("game_sudoku", "Sudoku", Category.GAMES, "🔢", "Number puzzle"),
        Tool("game_chess", "Chess", Category.GAMES, "♟️", "Chess game"),
        Tool("game_block_blast", "Block Blast", Category.GAMES, "🟦", "Block puzzle"),
    )

    fun searchTools(query: String): List<Tool> {
        if (query.isBlank()) return ALL_TOOLS
        val q = query.lowercase().trim()
        return ALL_TOOLS.filter {
            it.name.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.category.displayName.lowercase().contains(q)
        }
    }

    fun getByCategory(category: Category): List<Tool> {
        return ALL_TOOLS.filter { it.category == category }
    }
}