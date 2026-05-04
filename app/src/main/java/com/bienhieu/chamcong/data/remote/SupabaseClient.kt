package com.bienhieu.chamcong.data.remote

import com.bienhieu.chamcong.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton object for Supabase Client.
 *
 * Credentials are injected at build time from local.properties → BuildConfig.
 * This prevents API keys from being hardcoded in source code.
 */
object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }
}
