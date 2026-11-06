package com.omnicast.sender.stream

/**
 * Esquema y constantes de base de datos para perfiles de streaming y firmas HLS (ADR-006 / HU-009).
 * Desacoplado de la lógica operativa de base de datos para mantener archivos < 250 líneas.
 */
object StreamProfileDbSchema {
    const val DB_NAME = "omnicast_stream_profiles.db"
    const val DB_VERSION = 2

    const val TABLE_PROFILES = "stream_profiles"
    const val TABLE_SIGS = "stream_format_signatures"

    const val COL_HOST = "host"
    const val COL_HEADERS = "headers_json"
    const val COL_BYPASS_PROXY = "bypass_proxy"
    const val COL_BRANCH = "branch"
    const val COL_SUCCESS_COUNT = "success_count"
    const val COL_LAST_SUCCESS = "last_success_ts"

    const val COL_SIG = "signature"
    const val COL_TYPE = "rule_type"

    const val SQL_CREATE_PROFILES_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_PROFILES (
            $COL_HOST TEXT PRIMARY KEY,
            $COL_HEADERS TEXT NOT NULL,
            $COL_BYPASS_PROXY INTEGER NOT NULL DEFAULT 1,
            $COL_BRANCH TEXT NOT NULL DEFAULT 'VOD_HLS_MP4',
            $COL_SUCCESS_COUNT INTEGER NOT NULL DEFAULT 1,
            $COL_LAST_SUCCESS INTEGER NOT NULL
        )
    """

    const val SQL_CREATE_SIGS_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_SIGS (
            $COL_SIG TEXT PRIMARY KEY,
            $COL_TYPE TEXT NOT NULL
        )
    """

    const val SQL_DROP_PROFILES_TABLE = "DROP TABLE IF EXISTS $TABLE_PROFILES"
    const val SQL_DROP_SIGS_TABLE = "DROP TABLE IF EXISTS $TABLE_SIGS"

    val DEFAULT_SIGNATURES = listOf(
        Triple("#EXT-X-STREAM-INF:", "MASTER", ""),
        Triple("#EXT-X-MEDIA:", "MASTER", ""),
        Triple("#EXT-X-I-FRAME-STREAM-INF:", "MASTER", ""),
        Triple("#EXTINF:", "DISQUALIFIER", ""),
        Triple("#EXT-X-TARGETDURATION:", "DISQUALIFIER", ""),
        Triple("#EXT-X-MEDIA-SEQUENCE:", "DISQUALIFIER", ""),
        Triple(".aac", "RAW_EXT", ""),
        Triple(".ts", "RAW_EXT", ""),
        Triple(".m4s", "RAW_EXT", ""),
        Triple(".m4a", "RAW_EXT", ""),
        Triple(".mp3", "RAW_EXT", "")
    )

    val DEFAULT_MASTER_NAMES = listOf("master.m3u8", "playlist.m3u8", "index.m3u8")
}

internal const val DB_NAME = StreamProfileDbSchema.DB_NAME
internal const val DB_VERSION = StreamProfileDbSchema.DB_VERSION
internal const val TABLE_PROFILES = StreamProfileDbSchema.TABLE_PROFILES
internal const val TABLE_SIGS = StreamProfileDbSchema.TABLE_SIGS
internal const val COL_HOST = StreamProfileDbSchema.COL_HOST
internal const val COL_HEADERS = StreamProfileDbSchema.COL_HEADERS
internal const val COL_BYPASS_PROXY = StreamProfileDbSchema.COL_BYPASS_PROXY
internal const val COL_BRANCH = StreamProfileDbSchema.COL_BRANCH
internal const val COL_SUCCESS_COUNT = StreamProfileDbSchema.COL_SUCCESS_COUNT
internal const val COL_LAST_SUCCESS = StreamProfileDbSchema.COL_LAST_SUCCESS
internal const val COL_SIG = StreamProfileDbSchema.COL_SIG
internal const val COL_TYPE = StreamProfileDbSchema.COL_TYPE
internal val DEFAULT_SIGNATURES = StreamProfileDbSchema.DEFAULT_SIGNATURES
internal val DEFAULT_MASTER_NAMES = StreamProfileDbSchema.DEFAULT_MASTER_NAMES
internal const val SQL_CREATE_PROFILES_TABLE = StreamProfileDbSchema.SQL_CREATE_PROFILES_TABLE
internal const val SQL_CREATE_SIGS_TABLE = StreamProfileDbSchema.SQL_CREATE_SIGS_TABLE
internal const val SQL_DROP_PROFILES_TABLE = StreamProfileDbSchema.SQL_DROP_PROFILES_TABLE
internal const val SQL_DROP_SIGS_TABLE = StreamProfileDbSchema.SQL_DROP_SIGS_TABLE
