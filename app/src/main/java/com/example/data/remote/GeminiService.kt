package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Moshi request/response models for Gemini
data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(val contents: List<GeminiContent>)

data class GeminiCandidate(val content: GeminiContent)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun parseTaskFromText(userText: String, currentDateTimeStr: String): ParsedTaskResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is placeholder")
            return null
        }

        val prompt = """
            You are أويكونوميا's highly intelligent, multilingual natural language understanding engine.
            The user wants to set a reminder/task.
            Current timestamp context (use this to resolve relative dates and times): $currentDateTimeStr.
            
            Understand the user's intent in any language they speak:
            - Classical Arabic (الفصحى)
            - Colloquial Arabic (العامية بجميع لهجاتها: المصرية، السعودية، الخليجية، الشامية، العراقية، إلخ.)
            - English
            - Mixed Arabic and English (Frarabic / Spanglish)
            
            Extract the following fields from the user's message:
            1. title: Short, active, and clean task title. Strip out any conversational filler words (such as "فكرني بـ", "ذكرني اني", "اريد تذكير", "remind me to", "please", "عايزك تفكرني", "أبغاك تفكرني", "ودّي تسوي"). Use a polished, clear title.
            2. description: Optional details or notes. If none, use an empty string.
            3. dueDate: Format strictly as YYYY-MM-DD. Intelligently calculate relative dates based on the current timestamp context:
               - "اليوم" / "النهاردة" / "اليومين دول" / "today" -> Today's date
               - "بكرة" / "بكره" / "غداً" / "tomorrow" -> Tomorrow's date
               - "بعد بكرة" / "بعد بكره" / "بعد غد" / "day after tomorrow" -> Date in 2 days
               - "الجمعة الجاية" / "الجمعة القادمة" / "next Friday" -> The next upcoming Friday
               - Days of the week like "السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", or "Monday", "Tuesday", etc. -> The exact date of that nearest upcoming weekday.
            4. dueTime: Format strictly as HH:MM (24-hour). Resolve conversational and colloquial time expressions:
               - "الصبح" / "صباحاً" / "morning" -> "09:00"
               - "الظهر" / "ظهراً" / "noon" -> "13:00"
               - "العصر" / "بعد الظهر" / "afternoon" -> "16:00"
               - "المغرب" / "غروب" -> "18:30"
               - "العشا" / "بليل" / "مساءً" / "night" / "evening" -> "20:00"
               - "بعد ساعة" / "in an hour" -> Current hour + 1 hour (calculate from timestamp context)
               - "بعد ساعتين" / "in 2 hours" -> Current hour + 2 hours
               - Explicit times like "الساعة 5 مساءً", "5:30 م", "at 5 PM" -> "17:00", "17:30", "17:00"
               - If no time is specified at all, default to "12:00".
            5. priority: Must be one of: CRITICAL, IMPORTANT, NORMAL, CAN_WAIT.
               - Match words like "عاجل", "ضروري", "أهم شيء", "فورا", "urgent", "asap" -> CRITICAL
               - "مهم", "لا تنسى", "important" -> IMPORTANT
               - "بسيط", "عادي", "براحتك", "can wait", "someday" -> CAN_WAIT
               - Default to NORMAL.
            6. repeat: Must be one of: NONE, DAILY, WEEKLY, MONTHLY.
               - "كل يوم" / "يومياً" / "daily" -> DAILY
               - "كل أسبوع" / "أسبوعياً" / "weekly" -> WEEKLY
               - "كل شهر" / "شهرياً" / "monthly" -> MONTHLY
               - Default to NONE.
            7. category: Must be one of: PERSONAL, WORK, HEALTH, SHOPPING, OTHER.
               - Words related to health, gym, medicine, doctor, دكتور, علاج, رياضة, تمرين -> HEALTH
               - Words related to office, job, meeting, work, شغل, عمل, وظيفة, اجتماع, مذاكرة, دراسة -> WORK
               - Words related to grocery, store, buying, shop, شراء, بقالة, سوبرماركت, اشتري -> SHOPPING
               - Words related to home, family, children, friends, بيت, أهل, أصدقاء, منزلي -> PERSONAL
               - Otherwise -> OTHER.

            Rules:
            - Return ONLY a raw JSON object with these keys: "title", "description", "dueDate", "dueTime", "priority", "repeat", "category".
            - Do NOT include any markdown formatting, no ```json, no backticks, no trailing comments. Return pure JSON string only.

            User message: "$userText"
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "Raw Gemini response: $rawText")

            // Parse clean JSON using Android's built-in JSONObject
            val cleanJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObject = JSONObject(cleanJson)
            ParsedTaskResult(
                title = jsonObject.optString("title", ""),
                description = jsonObject.optString("description", ""),
                dueDate = jsonObject.optString("dueDate", ""),
                dueTime = jsonObject.optString("dueTime", "12:00"),
                priority = jsonObject.optString("priority", "NORMAL"),
                repeat = jsonObject.optString("repeat", "NONE"),
                category = jsonObject.optString("category", "OTHER")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing task from text via Gemini", e)
            null
        }
    }
}

data class ParsedTaskResult(
    val title: String,
    val description: String,
    val dueDate: String,
    val dueTime: String,
    val priority: String,
    val repeat: String,
    val category: String
)
