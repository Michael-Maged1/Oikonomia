package com.example.presentation.screens

import java.util.Locale

object Localization {
    private val ar = mapOf(
        "app_name" to "أويكونوميا",
        "welcome" to "مرحباً بك في أويكونوميا",
        "what_should_i_call_you" to "ماذا يجب أن أناديك؟",
        "nickname_placeholder" to "أدخل اسم الشهرة هنا...",
        "select_language" to "اختر اللغة المفضلّة:",
        "start_now" to "ابدأ الآن",
        "ask_notification_permission" to "سنقوم بطلب إذن التنبيهات لتذكيرك بمهامك في الوقت المحدد.",
        "voice_prompt" to "بماذا تريد أن أذكرك اليوم؟",
        "my_tasks" to "مهامي المجدولة",
        "my_tasks_title" to "قائمة المهام",
        "statistics" to "الإحصائيات والتقدم",
        "calendar" to "التقويم المالي",
        "settings" to "الإعدادات العامة",
        "voice_input_listening" to "جاري الاستماع إليك...",
        "voice_input_speak" to "تحدث الآن بطلبك (مثال: تذكير بالاجتماع غداً الساعة 5 مساءً)",
        "stop_recording" to "إيقاف التسجيل",
        "processing_voice" to "جاري تحليل صوتك بالذكاء الاصطناعي...",
        "preview_task" to "مراجعة المهمة المستخرجة",
        "title" to "العنوان",
        "description" to "الوصف",
        "date" to "التاريخ",
        "time" to "الوقت",
        "priority" to "الأهمية",
        "category" to "التصنيف",
        "repeat" to "التكرار",
        "save" to "حفظ المهمة",
        "cancel" to "إلغاء",
        "edit" to "تعديل",
        "delete" to "حذف",
        "no_tasks" to "لا يوجد مهام حالياً",
        "search_placeholder" to "البحث عن مهمة...",
        "create_task_manual" to "إنشاء مهمة يدوياً",
        "update_task_manual" to "تعديل المهمة",
        "voice_autofill" to "إدخال صوتي ذكي",
        "critical" to "حرج للغاية",
        "important" to "مهم جداً",
        "normal" to "عادي",
        "can_wait" to "يمكنه الانتظار",
        "completed_tasks" to "المهام المكتملة",
        "remaining_tasks" to "المهام المتبقية",
        "todays_tasks" to "مهام اليوم",
        "completion_rate" to "نسبة الإنجاز",
        "weekly_progress" to "التقدم الأسبوعي",
        "monthly_progress" to "التقدم الشهري",
        "daily_reminder_time" to "وقت التذكير اليومي التلقائي",
        "nickname" to "الاسم المستعار",
        "account" to "الحساب والمزامنة",
        "backup_restore" to "النسخ الاحتياطي والاستعادة",
        "about" to "حول التطبيق",
        "create_account" to "إنشاء حساب جديد",
        "email" to "البريد الإلكتروني",
        "password" to "كلمة المرور",
        "login" to "تسجيل الدخول",
        "logout" to "تسجيل الخروج",
        "register" to "تسجيل حساب",
        "anonymous_mode" to "حساب محلي (مجهول)",
        "not_logged_in" to "غير متصل بالسحابة",
        "sync_now" to "مزامنة الآن",
        "cloud_synced" to "متصل ومزامن بالسحابة",
        "offline_notice" to "يعمل دون اتصال بالإنترنت",
        "all" to "الكل",
        "none" to "لا يوجد",
        "daily" to "يومي",
        "weekly" to "أسبوعي",
        "monthly" to "شهري",
        "personal" to "شخصي",
        "work" to "عمل",
        "health" to "صحة وعافية",
        "shopping" to "تسوق",
        "other" to "أخرى",
        "voice_test_hint" to "اختبار بالصوت يدوياً (اكتب هنا للتجربة دون مكبر صوت):",
        "submit_test" to "تحليل ذكي"
    )

    private val en = mapOf(
        "app_name" to "أويكونوميا",
        "welcome" to "Welcome to أويكونوميا",
        "what_should_i_call_you" to "What should I call you?",
        "nickname_placeholder" to "Enter your nickname...",
        "select_language" to "Select Language:",
        "start_now" to "Get Started",
        "ask_notification_permission" to "We will request notification permission to remind you of your tasks on time.",
        "voice_prompt" to "What would you like me to remind you about?",
        "my_tasks" to "My Tasks",
        "my_tasks_title" to "Tasks List",
        "statistics" to "Statistics",
        "calendar" to "Calendar",
        "settings" to "Settings",
        "voice_input_listening" to "Listening to you...",
        "voice_input_speak" to "Speak now (e.g., Remind me to call John tomorrow at 5 PM)",
        "stop_recording" to "Stop Recording",
        "processing_voice" to "AI is analyzing your request...",
        "preview_task" to "Review Extracted Task",
        "title" to "Title",
        "description" to "Description",
        "date" to "Date",
        "time" to "Time",
        "priority" to "Priority",
        "category" to "Category",
        "repeat" to "Repeat",
        "save" to "Save Task",
        "cancel" to "Cancel",
        "edit" to "Edit",
        "delete" to "Delete",
        "no_tasks" to "No tasks yet",
        "search_placeholder" to "Search tasks...",
        "create_task_manual" to "Create Task",
        "update_task_manual" to "Update Task",
        "voice_autofill" to "Smart Voice Input",
        "critical" to "Critical",
        "important" to "Important",
        "normal" to "Normal",
        "can_wait" to "Can Wait",
        "completed_tasks" to "Completed Tasks",
        "remaining_tasks" to "Remaining Tasks",
        "todays_tasks" to "Today's Tasks",
        "completion_rate" to "Completion Rate",
        "weekly_progress" to "Weekly Progress",
        "monthly_progress" to "Monthly Progress",
        "daily_reminder_time" to "Default Daily Reminder Time",
        "nickname" to "Nickname",
        "account" to "Account & Sync",
        "backup_restore" to "Backup & Restore",
        "about" to "About",
        "create_account" to "Create Account",
        "email" to "Email Address",
        "password" to "Password",
        "login" to "Log In",
        "logout" to "Log Out",
        "register" to "Register",
        "anonymous_mode" to "Anonymous Mode",
        "not_logged_in" to "Not Logged In",
        "sync_now" to "Sync Now",
        "cloud_synced" to "Cloud Synced",
        "offline_notice" to "Offline Mode",
        "all" to "All",
        "none" to "None",
        "daily" to "Daily",
        "weekly" to "Weekly",
        "monthly" to "Monthly",
        "personal" to "Personal",
        "work" to "Work",
        "health" to "Health",
        "shopping" to "Shopping",
        "other" to "Other",
        "voice_test_hint" to "Emulator Voice Input (Type what you would say):",
        "submit_test" to "AI Parse"
    )

    fun translate(key: String, lang: String): String {
        return if (lang == "ar") {
            ar[key] ?: en[key] ?: key
        } else {
            en[key] ?: ar[key] ?: key
        }
    }

    fun formatTimeTo12Hour(timeStr: String, language: String): String {
        if (timeStr.isEmpty()) return ""
        try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                // Extract only digits in case there is trailing/leading text
                val hrStr = parts[0].filter { it.isDigit() }
                val minStr = parts[1].filter { it.isDigit() }
                val hour24 = hrStr.toIntOrNull() ?: return timeStr
                val minute = minStr.toIntOrNull() ?: return timeStr
                val suffix = if (hour24 >= 12) {
                    if (language == "ar") "مساءً" else "PM"
                } else {
                    if (language == "ar") "صباحاً" else "AM"
                }
                val hour12 = when {
                    hour24 == 0 -> 12
                    hour24 > 12 -> hour24 - 12
                    else -> hour24
                }
                return String.format(Locale.US, "%02d:%02d %s", hour12, minute, suffix)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return timeStr
    }
}
