// Temporarily replace SupabaseConfig.kt with this:

package com.Aman.myapplication

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    private const val SUPABASE_URL = "https://vqhmuwjizefxahczixxx.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZxaG11d2ppemVmeGFoY3ppeHh4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk2NzIzODYsImV4cCI6MjA2NTI0ODM4Nn0.JJKfWjHfhl4OWeOqsyJzjL0Hk5iFbjNl6YOI4BFcHoE"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        // Temporarily removed Realtime due to version conflict
    }
}