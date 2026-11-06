package com.omnicast.core.domain.config

object SupabaseCloudConfig {
    const val BASE_URL = "https://pfhjrplnfuupftgzdnop.supabase.co"
    const val REST_ENDPOINT = "$BASE_URL/rest/v1"
    const val LOGS_URL = "$REST_ENDPOINT/omnicast_system_logs"
    const val RULES_URL = "$REST_ENDPOINT/omnicast_stream_rules"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"
}
