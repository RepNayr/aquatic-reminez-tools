package com.repp.aquaticremineztools.data

import androidx.annotation.DrawableRes
import com.repp.aquaticremineztools.R

/**
 * Registry pemetaan terpusat antara tool.id/game.id dengan aset ikon 3D lokal.
 * Setiap tool dan game memiliki satu identitas ikon permanen di seluruh aplikasi.
 */
object ToolIconRegistry {

    private val iconMap: Map<String, Int> = mapOf(
        // ============ UTILITY (8) ============
        "util_calculator" to R.drawable.ic_tool_util_calculator,
        "util_unit_converter" to R.drawable.ic_tool_util_unit_converter,
        "util_notes" to R.drawable.ic_tool_util_notes,
        "util_todo" to R.drawable.ic_tool_util_todo,
        "util_stopwatch" to R.drawable.ic_tool_util_stopwatch,
        "util_compass" to R.drawable.ic_tool_util_compass,
        "util_currency" to R.drawable.ic_tool_util_currency,
        "util_world_clock" to R.drawable.ic_tool_util_world_clock,

        // ============ SECURITY & GENERATE (10) ============
        "secgen_password" to R.drawable.ic_tool_secgen_password,
        "secgen_text_enc" to R.drawable.ic_tool_secgen_text_enc,
        "secgen_hash" to R.drawable.ic_tool_secgen_hash,
        "secgen_qr_gen" to R.drawable.ic_tool_secgen_qr_gen,
        "secgen_qr_scan" to R.drawable.ic_tool_secgen_qr_scan,
        "secgen_file_enc" to R.drawable.ic_tool_secgen_file_enc,
        "secgen_file_dec" to R.drawable.ic_tool_secgen_file_dec,
        "secgen_barcode_gen" to R.drawable.ic_tool_secgen_barcode_gen,
        "secgen_barcode_scan" to R.drawable.ic_tool_secgen_barcode_scan,
        "secgen_secure_notes" to R.drawable.ic_tool_secgen_secure_notes,

        // ============ FILE & SYSTEM (9) ============
        "file_manager" to R.drawable.ic_tool_file_manager,
        "file_shredder" to R.drawable.ic_tool_file_shredder,
        "file_app_info" to R.drawable.ic_tool_file_app_info,
        "file_storage_analyzer" to R.drawable.ic_tool_file_storage_analyzer,
        "file_compressor" to R.drawable.ic_tool_file_compressor,
        "file_extractor" to R.drawable.ic_tool_file_extractor,
        "file_splitter" to R.drawable.ic_tool_file_splitter,
        "file_joiner" to R.drawable.ic_tool_file_joiner,
        "file_device_info" to R.drawable.ic_tool_file_device_info,

        // ============ NETWORK (16) ============
        "net_scanner" to R.drawable.ic_tool_net_scanner,
        "net_ping" to R.drawable.ic_tool_net_ping,
        "net_dns" to R.drawable.ic_tool_net_dns,
        "net_wifi_info" to R.drawable.ic_tool_net_wifi_info,
        "net_ip_info" to R.drawable.ic_tool_net_ip_info,
        "net_port_scanner" to R.drawable.ic_tool_net_port_scanner,
        "net_whois" to R.drawable.ic_tool_net_whois,
        "net_subdomain" to R.drawable.ic_tool_net_subdomain,
        "net_http_header" to R.drawable.ic_tool_net_http_header,
        "net_traceroute" to R.drawable.ic_tool_net_traceroute,
        "net_speed_test" to R.drawable.ic_tool_net_speed_test,
        "net_wifi_scanner" to R.drawable.ic_tool_net_wifi_scanner,
        "net_wifi_analyzer" to R.drawable.ic_tool_net_wifi_analyzer,
        "net_reverse_ip" to R.drawable.ic_tool_net_reverse_ip,
        "net_http_request" to R.drawable.ic_tool_net_http_request,
        "net_bandwidth" to R.drawable.ic_tool_net_bandwidth,

        // ============ MEDIA (31) ============
        "media_color_picker" to R.drawable.ic_tool_media_color_picker,
        "media_image_compressor" to R.drawable.ic_tool_media_image_compressor,
        "media_image_converter" to R.drawable.ic_tool_media_image_converter,
        "media_image_hd" to R.drawable.ic_tool_media_image_hd,
        "media_bg_remover" to R.drawable.ic_tool_media_bg_remover,
        "media_image_crop" to R.drawable.ic_tool_media_image_crop,
        "media_image_resize" to R.drawable.ic_tool_media_image_resize,
        "media_image_rotate" to R.drawable.ic_tool_media_image_rotate,
        "media_watermark_add" to R.drawable.ic_tool_media_watermark_add,
        "media_ai_image_gen" to R.drawable.ic_tool_media_ai_image_gen,
        "media_ai_obj_remove" to R.drawable.ic_tool_media_ai_obj_remove,
        "media_image_filters" to R.drawable.ic_tool_media_image_filters,
        "media_video_converter" to R.drawable.ic_tool_media_video_converter,
        "media_video_hd" to R.drawable.ic_tool_media_video_hd,
        "media_watermark_remove_video" to R.drawable.ic_tool_media_watermark_remove_video,
        "media_video_compressor" to R.drawable.ic_tool_media_video_compressor,
        "media_video_to_gif" to R.drawable.ic_tool_media_video_to_gif,
        "media_video_trim" to R.drawable.ic_tool_media_video_trim,
        "media_audio_extractor" to R.drawable.ic_tool_media_audio_extractor,
        "media_info" to R.drawable.ic_tool_media_info,
        "media_player" to R.drawable.ic_tool_media_player,
        "media_audio_converter" to R.drawable.ic_tool_media_audio_converter,
        "media_doc_converter" to R.drawable.ic_tool_media_doc_converter,
        "media_tiktok_dl" to R.drawable.ic_tool_media_tiktok_dl,
        "media_instagram_dl" to R.drawable.ic_tool_media_instagram_dl,
        "media_facebook_dl" to R.drawable.ic_tool_media_facebook_dl,
        "media_youtube_dl" to R.drawable.ic_tool_media_youtube_dl,
        "media_spotify_dl" to R.drawable.ic_tool_media_spotify_dl,
        "media_twitter_dl" to R.drawable.ic_tool_media_twitter_dl,
        "media_pinterest_dl" to R.drawable.ic_tool_media_pinterest_dl,
        "media_yt_thumbnail" to R.drawable.ic_tool_media_yt_thumbnail,

        // ============ DEV TOOLS (3) ============
        "dev_terminal" to R.drawable.ic_tool_dev_terminal,
        "dev_regex" to R.drawable.ic_tool_dev_regex,
        "dev_lorem" to R.drawable.ic_tool_dev_lorem,

        // ============ FORENSIC (2) ============
        "forensic_apk_analyzer" to R.drawable.ic_tool_forensic_apk_analyzer,
        "forensic_archive" to R.drawable.ic_tool_forensic_archive,

        // ============ SECURITY (4) ============
        "security_xss" to R.drawable.ic_tool_security_xss,
        "security_sqli" to R.drawable.ic_tool_security_sqli,
        "security_enc_vault" to R.drawable.ic_tool_security_enc_vault,
        "security_steganography" to R.drawable.ic_tool_security_steganography,

        // ============ OFFENSIVE (6) ============
        "off_ddos" to R.drawable.ic_tool_off_ddos,
        "off_rat" to R.drawable.ic_tool_off_rat,
        "off_bruteforce" to R.drawable.ic_tool_off_bruteforce,
        "off_exploit" to R.drawable.ic_tool_off_exploit,
        "off_bug_wa" to R.drawable.ic_tool_off_bug_wa,
        "off_injection" to R.drawable.ic_tool_off_injection,

        // ============ DEFENSIVE (11) ============
        "def_intrusion" to R.drawable.ic_tool_def_intrusion,
        "def_breach" to R.drawable.ic_tool_def_breach,
        "def_vuln_scan" to R.drawable.ic_tool_def_vuln_scan,
        "def_perm_audit" to R.drawable.ic_tool_def_perm_audit,
        "def_malware" to R.drawable.ic_tool_def_malware,
        "def_secure_dns" to R.drawable.ic_tool_def_secure_dns,
        "def_wifi_sec" to R.drawable.ic_tool_def_wifi_sec,
        "def_phishing" to R.drawable.ic_tool_def_phishing,
        "def_fake_gps" to R.drawable.ic_tool_def_fake_gps,
        "def_call_block" to R.drawable.ic_tool_def_call_block,
        "def_screenshot_detect" to R.drawable.ic_tool_def_screenshot_detect,

        // ============ GAMES (8) ============
        "game_snake" to R.drawable.ic_tool_game_snake,
        "game_tetris" to R.drawable.ic_tool_game_tetris,
        "game_flappy" to R.drawable.ic_tool_game_flappy,
        "game_tictactoe" to R.drawable.ic_tool_game_tictactoe,
        "game_memory" to R.drawable.ic_tool_game_memory,
        "game_sudoku" to R.drawable.ic_tool_game_sudoku,
        "game_chess" to R.drawable.ic_tool_game_chess,
        "game_block_blast" to R.drawable.ic_tool_game_block_blast,

        // ============ DUMMY / LEGACY SEED ALIASES ============
        "net_diag" to R.drawable.ic_tool_net_ping,
        "wifi_scanner" to R.drawable.ic_tool_net_wifi_scanner,
        "port_scan" to R.drawable.ic_tool_net_port_scanner,
        "hash_calc" to R.drawable.ic_tool_secgen_hash,
        "dns_lookup" to R.drawable.ic_tool_net_dns,
        "subdomain_enum" to R.drawable.ic_tool_net_subdomain,
        "packet_sniffer" to R.drawable.ic_tool_net_traceroute,
        "qr_gen" to R.drawable.ic_tool_secgen_qr_gen,
    )

    @DrawableRes
    fun getIconRes(toolId: String?): Int {
        if (toolId == null) return R.drawable.ic_file
        return iconMap[toolId] ?: R.drawable.ic_file
    }

    @DrawableRes
    fun getIconRes(tool: ToolData.Tool): Int {
        return getIconRes(tool.id)
    }

    fun hasIcon(toolId: String): Boolean {
        return iconMap.containsKey(toolId)
    }
}
