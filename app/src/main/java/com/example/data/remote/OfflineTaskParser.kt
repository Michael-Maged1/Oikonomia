package com.example.data.remote

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object OfflineTaskParser {
    private const val TAG = "OfflineTaskParser"

    fun normalizeDigits(input: String): String {
        var output = input
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            output = output.replace(arabicDigits[i].toString(), i.toString())
        }
        return output
    }

    fun normalizeArabic(input: String): String {
        var str = input.trim()
        str = str.replace("[أإآ]".toRegex(), "ا")
        str = str.replace("ة".toRegex(), "ه")
        str = str.replace("ى".toRegex(), "ي")
        str = str.replace("[ًٌٍَُِّْ]".toRegex(), "") // remove diacritics
        return str
    }

    fun parse(userText: String): ParsedTaskResult {
        val originalText = userText.trim()
        if (originalText.isEmpty()) {
            return ParsedTaskResult(
                title = "",
                description = "",
                dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                dueTime = "12:00",
                priority = "NORMAL",
                repeat = "NONE",
                category = "OTHER"
            )
        }

        var normalizedText = normalizeDigits(originalText)
        var cleanText = normalizedText

        // List of conversational prefixes to strip from the start of the sentence
        val prefixes = listOf(
            "فكرني بـ", "فكرني ب", "فكرني اني", "فكرني ان", "فكرني أن", "فكرني",
            "ذكرني بـ", "ذكرني ب", "ذكرني اني", "ذكرني ان", "ذكرني أن", "ذكرني",
            "تذكير بـ", "تذكير ب", "تذكير اني", "تذكير ان", "تذكير أن", "تذكير",
            "remind me to", "remind me", "create a task to", "create task", "add task", "todo", "please remind me to",
            "عايزك تفكرني", "ابغاك تفكرني", "ودي تسوي", "ابغي تذكير", "أبغاك تفكرني"
        )

        for (prefix in prefixes) {
            val normPrefix = normalizeArabic(normalizeDigits(prefix)).lowercase(Locale.ROOT)
            val normClean = normalizeArabic(cleanText).lowercase(Locale.ROOT)
            if (normClean.startsWith(normPrefix)) {
                cleanText = cleanText.substring(prefix.length).trim()
                // If it starts with "بـ" or "ب" or "ان" after removing prefix, strip it too
                if (cleanText.startsWith("بـ")) cleanText = cleanText.substring(2).trim()
                else if (cleanText.startsWith("ب")) cleanText = cleanText.substring(1).trim()
                else if (cleanText.startsWith("اني")) cleanText = cleanText.substring(3).trim()
                else if (cleanText.startsWith("ان")) cleanText = cleanText.substring(2).trim()
                break
            }
        }

        val calendar = Calendar.getInstance()
        var dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        var dueTime = "" // Default will be set at the end if not resolved
        var priority = "NORMAL"
        var category = "OTHER"
        var repeat = "NONE"

        val normalizedForMatch = normalizeArabic(cleanText).lowercase(Locale.ROOT)

        // Extract priority
        if (normalizedForMatch.contains("عاجل") || normalizedForMatch.contains("ضروري") || 
            normalizedForMatch.contains("اهم شيء") || normalizedForMatch.contains("فورا") || 
            normalizedForMatch.contains("urgent") || normalizedForMatch.contains("asap")) {
            priority = "CRITICAL"
        } else if (normalizedForMatch.contains("مهم") || normalizedForMatch.contains("لا تنسي") || 
                   normalizedForMatch.contains("important") || normalizedForMatch.contains("crucial")) {
            priority = "IMPORTANT"
        } else if (normalizedForMatch.contains("بسيط") || normalizedForMatch.contains("عادي") || 
                   normalizedForMatch.contains("براحتك") || normalizedForMatch.contains("can wait") || 
                   normalizedForMatch.contains("someday")) {
            priority = "CAN_WAIT"
        }

        // Extract category
        if (normalizedForMatch.contains("دكتور") || normalizedForMatch.contains("طبيب") || 
            normalizedForMatch.contains("صحه") || normalizedForMatch.contains("مستشفي") || 
            normalizedForMatch.contains("دواء") || normalizedForMatch.contains("علاج") || 
            normalizedForMatch.contains("رياضه") || normalizedForMatch.contains("تمرين") || 
            normalizedForMatch.contains("جيم") || normalizedForMatch.contains("health") || 
            normalizedForMatch.contains("doctor") || normalizedForMatch.contains("medicine") || 
            normalizedForMatch.contains("gym") || normalizedForMatch.contains("workout")) {
            category = "HEALTH"
        } else if (normalizedForMatch.contains("شغل") || normalizedForMatch.contains("عمل") || 
                   normalizedForMatch.contains("مكتب") || normalizedForMatch.contains("اجتماع") || 
                   normalizedForMatch.contains("مذاكره") || normalizedForMatch.contains("دراسه") || 
                   normalizedForMatch.contains("وظيفه") || normalizedForMatch.contains("work") || 
                   normalizedForMatch.contains("office") || normalizedForMatch.contains("meeting") || 
                   normalizedForMatch.contains("study")) {
            category = "WORK"
        } else if (normalizedForMatch.contains("شراء") || normalizedForMatch.contains("سوبرماركت") || 
                   normalizedForMatch.contains("سوبر ماركت") || normalizedForMatch.contains("بقاله") || 
                   normalizedForMatch.contains("اشتري") || normalizedForMatch.contains("شوبينج") || 
                   normalizedForMatch.contains("shopping") || normalizedForMatch.contains("buy") || 
                   normalizedForMatch.contains("groceries") || normalizedForMatch.contains("store")) {
            category = "SHOPPING"
        } else if (normalizedForMatch.contains("بيت") || normalizedForMatch.contains("منزل") || 
                   normalizedForMatch.contains("عائلي") || normalizedForMatch.contains("اهل") || 
                   normalizedForMatch.contains("اصدقاء") || normalizedForMatch.contains("منزلي") || 
                   normalizedForMatch.contains("personal") || normalizedForMatch.contains("family") || 
                   normalizedForMatch.contains("home") || normalizedForMatch.contains("friends")) {
            category = "PERSONAL"
        }

        // --- Extracted Temporal Phrases to remove from cleanText ---
        val phrasesToRemove = mutableListOf<String>()

        // 1. Repeat Pattern
        var repeatPhraseFound = ""
        val repeatMappings = listOf(
            Pair("كل يوم", "DAILY"),
            Pair("يوميا", "DAILY"),
            Pair("every day", "DAILY"),
            Pair("daily", "DAILY"),
            Pair("كل اسبوع", "WEEKLY"),
            Pair("اسبوعيا", "WEEKLY"),
            Pair("every week", "WEEKLY"),
            Pair("weekly", "WEEKLY"),
            Pair("كل شهر", "MONTHLY"),
            Pair("شهريا", "MONTHLY"),
            Pair("every month", "MONTHLY"),
            Pair("monthly", "MONTHLY"),
            Pair("كل سنه", "MONTHLY"),
            Pair("سنويا", "MONTHLY"),
            Pair("every year", "MONTHLY"),
            Pair("yearly", "MONTHLY")
        )

        for (mapping in repeatMappings) {
            val target = normalizeArabic(mapping.first)
            if (normalizedForMatch.contains(target)) {
                repeat = mapping.second
                repeatPhraseFound = mapping.first
                break
            }
        }

        // Day of week repeating
        val dayRepeatMappings = listOf(
            Pair("كل سبت", Calendar.SATURDAY),
            Pair("كل احد", Calendar.SUNDAY),
            Pair("كل اثنين", Calendar.MONDAY),
            Pair("كل ثلاثاء", Calendar.TUESDAY),
            Pair("كل اربعاء", Calendar.WEDNESDAY),
            Pair("كل خميس", Calendar.THURSDAY),
            Pair("كل جمعه", Calendar.FRIDAY)
        )
        for (mapping in dayRepeatMappings) {
            val target = normalizeArabic(mapping.first)
            if (normalizedForMatch.contains(target)) {
                repeat = "WEEKLY"
                repeatPhraseFound = mapping.first
                // Set initial due date to next weekday
                val nextDate = getNextWeekday(mapping.second)
                calendar.time = nextDate
                dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                break
            }
        }

        if (repeatPhraseFound.isNotEmpty()) {
            phrasesToRemove.add(repeatPhraseFound)
            // also remove variation in case it had diacritics or normalized form in cleanText
            val pattern = Pattern.compile(Pattern.quote(repeatPhraseFound), Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                cleanText = cleanText.replace(matcher.group(0) ?: "", " ")
            } else {
                // remove normalized form or parts
                cleanText = cleanText.replace("كل يوم", " ").replace("يومياً", " ").replace("يوميا", " ")
                    .replace("كل أسبوع", " ").replace("كل اسبوع", " ")
                    .replace("كل شهر", " ").replace("كل سنة", " ").replace("كل سنه", " ")
                    .replace("كل خميس", " ").replace("كل جمعة", " ").replace("كل جمعه", " ")
                    .replace("كل سبت", " ").replace("كل أحد", " ").replace("كل احد", " ")
                    .replace("كل إثنين", " ").replace("كل اثنين", " ")
                    .replace("كل ثلاثاء", " ").replace("كل أربعاء", " ").replace("كل اربعاء", " ")
            }
        }

        // 2. Date parsing
        var datePhraseFound = ""
        var dateTarget = calendar.time

        if (normalizedForMatch.contains("بعد بكره") || normalizedForMatch.contains("بعد بكرة") || 
            normalizedForMatch.contains("بعد غد") || normalizedForMatch.contains("day after tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            dateTarget = calendar.time
            datePhraseFound = "after_tomorrow"
            // Find what to remove from cleanText
            cleanText = cleanText.replace("بعد بكرة", " ").replace("بعد بكره", " ").replace("بعد غد", " ")
                .replace("day after tomorrow", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("بكره") || normalizedForMatch.contains("بكرة") || 
                   normalizedForMatch.contains("غدا") || normalizedForMatch.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateTarget = calendar.time
            datePhraseFound = "tomorrow"
            cleanText = cleanText.replace("بكرة", " ").replace("بكره", " ").replace("غداً", " ").replace("غدا", " ")
                .replace("tomorrow", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("النهارده") || normalizedForMatch.contains("النهاردة") || 
                   normalizedForMatch.contains("اليوم") || normalizedForMatch.contains("today")) {
            dateTarget = calendar.time
            datePhraseFound = "today"
            cleanText = cleanText.replace("النهاردة", " ").replace("النهارده", " ").replace("اليوم", " ")
                .replace("today", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("الاسبوع القادم") || normalizedForMatch.contains("الاسبوع الجاي") || 
                   normalizedForMatch.contains("next week")) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            dateTarget = calendar.time
            datePhraseFound = "next_week"
            cleanText = cleanText.replace("الأسبوع القادم", " ").replace("الاسبوع القادم", " ").replace("الاسبوع الجاي", " ")
                .replace("next week", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("اخر الشهر") || normalizedForMatch.contains("آخر الشهر") || 
                   normalizedForMatch.contains("end of month")) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            dateTarget = calendar.time
            datePhraseFound = "end_of_month"
            cleanText = cleanText.replace("آخر الشهر", " ").replace("اخر الشهر", " ")
                .replace("end of month", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("اول الشهر") || normalizedForMatch.contains("أول الشهر") || 
                   normalizedForMatch.contains("start of month")) {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            dateTarget = calendar.time
            datePhraseFound = "start_of_month"
            cleanText = cleanText.replace("أول الشهر", " ").replace("اول الشهر", " ")
                .replace("start of month", " ", ignoreCase = true)
        } else {
            // Specific weekdays
            val dayOfWeekMappings = listOf(
                Pair("السبت", Calendar.SATURDAY),
                Pair("الأحد", Calendar.SUNDAY),
                Pair("الاحد", Calendar.SUNDAY),
                Pair("الإثنين", Calendar.MONDAY),
                Pair("الاثنين", Calendar.MONDAY),
                Pair("الثلاثاء", Calendar.TUESDAY),
                Pair("الأربعاء", Calendar.WEDNESDAY),
                Pair("الاربعاء", Calendar.WEDNESDAY),
                Pair("الخميس", Calendar.THURSDAY),
                Pair("الجمعة", Calendar.FRIDAY),
                Pair("الجمعه", Calendar.FRIDAY),
                Pair("saturday", Calendar.SATURDAY),
                Pair("sunday", Calendar.SUNDAY),
                Pair("monday", Calendar.MONDAY),
                Pair("tuesday", Calendar.TUESDAY),
                Pair("wednesday", Calendar.WEDNESDAY),
                Pair("thursday", Calendar.THURSDAY),
                Pair("friday", Calendar.FRIDAY)
            )
            for (mapping in dayOfWeekMappings) {
                if (normalizedForMatch.contains(mapping.first)) {
                    val nextDate = getNextWeekday(mapping.second)
                    dateTarget = nextDate
                    datePhraseFound = mapping.first
                    cleanText = cleanText.replace(mapping.first, " ", ignoreCase = true)
                    break
                }
            }
        }

        dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateTarget)

        // Reset calendar to now for calculating time offsets correctly
        calendar.time = Date()

        // 3. Time Offsets
        var timeOffsetFound = false
        if (normalizedForMatch.contains("بعد ساعتين") || normalizedForMatch.contains("in 2 hours") || normalizedForMatch.contains("after 2 hours")) {
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
            timeOffsetFound = true
            cleanText = cleanText.replace("بعد ساعتين", " ").replace("in 2 hours", " ", ignoreCase = true).replace("after 2 hours", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("بعد ساعه") || normalizedForMatch.contains("بعد ساعة") || 
                   normalizedForMatch.contains("in an hour") || normalizedForMatch.contains("after an hour") || 
                   normalizedForMatch.contains("بعد ساعه واحدة") || normalizedForMatch.contains("بعد ساعة واحدة")) {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
            timeOffsetFound = true
            cleanText = cleanText.replace("بعد ساعة واحدة", " ").replace("بعد ساعه واحدة", " ")
                .replace("بعد ساعة", " ").replace("بعد ساعه", " ")
                .replace("in an hour", " ", ignoreCase = true).replace("after an hour", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("بعد نصف ساعه") || normalizedForMatch.contains("بعد نصف ساعة") || 
                   normalizedForMatch.contains("بعد نص ساعه") || normalizedForMatch.contains("بعد نص ساعة") || 
                   normalizedForMatch.contains("in half an hour") || normalizedForMatch.contains("after half an hour")) {
            calendar.add(Calendar.MINUTE, 30)
            dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
            timeOffsetFound = true
            cleanText = cleanText.replace("بعد نصف ساعة", " ").replace("بعد نصف ساعه", " ")
                .replace("بعد نص ساعة", " ").replace("بعد نص ساعه", " ")
                .replace("in half an hour", " ", ignoreCase = true).replace("after half an hour", " ", ignoreCase = true)
        } else if (normalizedForMatch.contains("بعد ربع ساعه") || normalizedForMatch.contains("بعد ربع ساعة") || 
                   normalizedForMatch.contains("in a quarter of an hour") || normalizedForMatch.contains("after a quarter of an hour")) {
            calendar.add(Calendar.MINUTE, 15)
            dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
            timeOffsetFound = true
            cleanText = cleanText.replace("بعد ربع ساعة", " ").replace("بعد ربع ساعه", " ")
                .replace("in a quarter of an hour", " ", ignoreCase = true).replace("after a quarter of an hour", " ", ignoreCase = true)
        } else {
            // General "بعد X ساعة" or "بعد X دقيقة" using regex
            val arHourPattern = Pattern.compile("بعد\\s+(\\d+)\\s+(ساعة|ساعات|ساعتين|ساعه)")
            val arMinPattern = Pattern.compile("بعد\\s+(\\d+)\\s+(دقيقة|دقائق|دقيقه)")
            val enHourPattern = Pattern.compile("(in|after)\\s+(\\d+)\\s*(hours?|hr|hrs)", Pattern.CASE_INSENSITIVE)
            val enMinPattern = Pattern.compile("(in|after)\\s+(\\d+)\\s*(minutes?|mins?|m)", Pattern.CASE_INSENSITIVE)

            val matcherArHr = arHourPattern.matcher(cleanText)
            val matcherArMin = arMinPattern.matcher(cleanText)
            val matcherEnHr = enHourPattern.matcher(cleanText)
            val matcherEnMin = enMinPattern.matcher(cleanText)

            if (matcherArHr.find()) {
                val num = matcherArHr.group(1)?.toIntOrNull() ?: 1
                calendar.add(Calendar.HOUR_OF_DAY, num)
                dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
                timeOffsetFound = true
                cleanText = cleanText.replace(matcherArHr.group(0) ?: "", " ")
            } else if (matcherArMin.find()) {
                val num = matcherArMin.group(1)?.toIntOrNull() ?: 15
                calendar.add(Calendar.MINUTE, num)
                dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
                timeOffsetFound = true
                cleanText = cleanText.replace(matcherArMin.group(0) ?: "", " ")
            } else if (matcherEnHr.find()) {
                val num = matcherEnHr.group(2)?.toIntOrNull() ?: 1
                calendar.add(Calendar.HOUR_OF_DAY, num)
                dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
                timeOffsetFound = true
                cleanText = cleanText.replace(matcherEnHr.group(0) ?: "", " ")
            } else if (matcherEnMin.find()) {
                val num = matcherEnMin.group(2)?.toIntOrNull() ?: 15
                calendar.add(Calendar.MINUTE, num)
                dueTime = SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
                timeOffsetFound = true
                cleanText = cleanText.replace(matcherEnMin.group(0) ?: "", " ")
            }
        }

        // 4. Absolute Times if no relative time offset found
        if (!timeOffsetFound) {
            val arAbsoluteTimePatternColon = Pattern.compile("الساعة\\s+(\\d+)[:：](\\d+)")
            val arAbsoluteTimePatternSimple = Pattern.compile("الساعة\\s+(\\d+)")
            val enTimePatternColon = Pattern.compile("at\\s+(\\d+):(\\d+)\\s*(pm|am)?", Pattern.CASE_INSENSITIVE)
            val enTimePatternSimple = Pattern.compile("at\\s+(\\d+)\\s*(pm|am)", Pattern.CASE_INSENSITIVE)
            val simpleEnPmAmPattern = Pattern.compile("(\\d+)\\s*(pm|am)", Pattern.CASE_INSENSITIVE)

            val matcherColon = arAbsoluteTimePatternColon.matcher(cleanText)
            val matcherSimple = arAbsoluteTimePatternSimple.matcher(cleanText)
            val matcherEnColon = enTimePatternColon.matcher(cleanText)
            val matcherEnSimple = enTimePatternSimple.matcher(cleanText)
            val matcherSimplePmAm = simpleEnPmAmPattern.matcher(cleanText)

            var hourResolved = -1
            var minResolved = 0
            var resolvedMatchText = ""

            if (matcherColon.find()) {
                hourResolved = matcherColon.group(1)?.toIntOrNull() ?: 12
                minResolved = matcherColon.group(2)?.toIntOrNull() ?: 0
                resolvedMatchText = matcherColon.group(0) ?: ""
            } else if (matcherSimple.find()) {
                hourResolved = matcherSimple.group(1)?.toIntOrNull() ?: 12
                minResolved = 0
                resolvedMatchText = matcherSimple.group(0) ?: ""
            } else if (matcherEnColon.find()) {
                hourResolved = matcherEnColon.group(1)?.toIntOrNull() ?: 12
                minResolved = matcherEnColon.group(2)?.toIntOrNull() ?: 0
                val ampm = matcherEnColon.group(3)
                if (ampm != null) {
                    if (ampm.lowercase(Locale.ROOT) == "pm" && hourResolved < 12) {
                        hourResolved += 12
                    } else if (ampm.lowercase(Locale.ROOT) == "am" && hourResolved == 12) {
                        hourResolved = 0
                    }
                }
                resolvedMatchText = matcherEnColon.group(0) ?: ""
            } else if (matcherEnSimple.find()) {
                hourResolved = matcherEnSimple.group(1)?.toIntOrNull() ?: 12
                minResolved = 0
                val ampm = matcherEnSimple.group(2)
                if (ampm.lowercase(Locale.ROOT) == "pm" && hourResolved < 12) {
                    hourResolved += 12
                } else if (ampm.lowercase(Locale.ROOT) == "am" && hourResolved == 12) {
                    hourResolved = 0
                }
                resolvedMatchText = matcherEnSimple.group(0) ?: ""
            } else if (matcherSimplePmAm.find()) {
                hourResolved = matcherSimplePmAm.group(1)?.toIntOrNull() ?: 12
                minResolved = 0
                val ampm = matcherSimplePmAm.group(2)
                if (ampm.lowercase(Locale.ROOT) == "pm" && hourResolved < 12) {
                    hourResolved += 12
                } else if (ampm.lowercase(Locale.ROOT) == "am" && hourResolved == 12) {
                    hourResolved = 0
                }
                resolvedMatchText = matcherSimplePmAm.group(0) ?: ""
            }

            if (hourResolved != -1) {
                // Determine PM/AM for Arabic absolute times
                val matchesForArabicPm = cleanText.contains("مساء") || cleanText.contains("مساءً") || 
                        cleanText.contains("عصر") || cleanText.contains("ليل") || cleanText.contains("ظهر")
                val matchesForArabicAm = cleanText.contains("صباح") || cleanText.contains("صباحاً") || 
                        cleanText.contains("فجر")

                if (matchesForArabicPm && hourResolved < 12) {
                    hourResolved += 12
                } else if (matchesForArabicAm && hourResolved == 12) {
                    hourResolved = 0
                }

                dueTime = String.format(Locale.US, "%02d:%02d", hourResolved, minResolved)
                cleanText = cleanText.replace(resolvedMatchText, " ")
                    .replace("مساءً", " ").replace("مساء", " ").replace("صباحاً", " ").replace("صباح", " ")
                    .replace("ليلاً", " ").replace("ليلا", " ").replace("عصراً", " ").replace("عصر", " ")
                    .replace("ظهراً", " ").replace("ظهر", " ")
            }
        }

        // 5. Day Parts (Morning, Noon, Afternoon, Night)
        if (dueTime.isEmpty()) {
            if (normalizedForMatch.contains("بعد الفجر")) {
                dueTime = "05:00"
                cleanText = cleanText.replace("بعد الفجر", " ")
            } else if (normalizedForMatch.contains("بعد المغرب")) {
                dueTime = "18:30"
                cleanText = cleanText.replace("بعد المغرب", " ")
            } else if (normalizedForMatch.contains("بعد العشاء") || normalizedForMatch.contains("بعد العشا")) {
                dueTime = "20:30"
                cleanText = cleanText.replace("بعد العشاء", " ").replace("بعد العشا", " ")
            } else if (normalizedForMatch.contains("الصبح") || normalizedForMatch.contains("صباحا") || 
                       normalizedForMatch.contains("صباحا") || normalizedForMatch.contains("morning")) {
                dueTime = "09:00"
                cleanText = cleanText.replace("الصبح", " ").replace("صباحاً", " ").replace("صباحا", " ")
                    .replace("morning", " ", ignoreCase = true)
            } else if (normalizedForMatch.contains("الظهر") || normalizedForMatch.contains("ظهرا") || 
                       normalizedForMatch.contains("noon")) {
                dueTime = "12:00"
                cleanText = cleanText.replace("الظهر", " ").replace("ظهراً", " ").replace("ظهرا", " ")
                    .replace("noon", " ", ignoreCase = true)
            } else if (normalizedForMatch.contains("العصر") || normalizedForMatch.contains("عصرا") || 
                       normalizedForMatch.contains("afternoon")) {
                dueTime = "16:00"
                cleanText = cleanText.replace("العصر", " ").replace("عصراً", " ").replace("عصرا", " ")
                    .replace("afternoon", " ", ignoreCase = true)
            } else if (normalizedForMatch.contains("المساء") || normalizedForMatch.contains("مساء") || 
                       normalizedForMatch.contains("بالليل") || normalizedForMatch.contains("بليل") || 
                       normalizedForMatch.contains("evening") || normalizedForMatch.contains("night")) {
                dueTime = "20:00"
                cleanText = cleanText.replace("المساء", " ").replace("مساءً", " ").replace("مساء", " ")
                    .replace("بالليل", " ").replace("بليل", " ")
                    .replace("evening", " ", ignoreCase = true).replace("night", " ", ignoreCase = true)
            } else {
                dueTime = "12:00" // Default if absolutely nothing matched
            }
        }

        // --- Post-processing Title & Description Extraction ---
        // Clean up text double spaces, trailing periods/spaces, commas
        cleanText = cleanText.replace("\\s+".toRegex(), " ").trim()
        cleanText = cleanText.removeSuffix(".").removeSuffix(",").trim()

        var title = ""
        var description = ""

        if (cleanText.isEmpty()) {
            title = originalText
        } else {
            val words = cleanText.split(" ")
            if (words.size <= 6) {
                title = cleanText
            } else {
                // Look for conjunction/preposition split words
                val splitWords = listOf(
                    "علشان", "عشان", "لان", "لانني", "لأن", "بسبب", "لكي", "كي", "حتي", "حتى",
                    "من", "في", "بـ", "مع", "وأرسل", "وأعمل", "وانا", "وأنا", "و ", "ثم",
                    "because", "so that", "in order to", "for", "due to", "from", "at", "in", "with", "and", "then"
                )

                var splitIndex = -1
                var selectedSplitWord = ""

                for (word in words) {
                    val normalizedWord = normalizeArabic(word).lowercase(Locale.ROOT)
                    if (splitWords.any { normalizeArabic(it).lowercase(Locale.ROOT) == normalizedWord }) {
                        val idx = words.indexOf(word)
                        // Make sure we leave at least 2 words on the left, and at least 1 word on the right
                        if (idx >= 2 && idx < words.size - 1) {
                            splitIndex = idx
                            selectedSplitWord = word
                            break
                        }
                    }
                }

                if (splitIndex != -1) {
                    val leftSide = words.subList(0, splitIndex).joinToString(" ")
                    val rightSide = words.subList(splitIndex, words.size).joinToString(" ")

                    val leftWords = leftSide.split(" ")
                    if (leftWords.size <= 6) {
                        title = leftSide
                        description = rightSide
                    } else {
                        // Left side is too long (> 6 words), so take first 5 words as Title, rest goes to Description
                        title = leftWords.subList(0, 5).joinToString(" ")
                        description = leftWords.subList(5, leftWords.size).joinToString(" ") + " " + rightSide
                    }
                } else {
                    // No split word found, take first 5 words as Title, remaining as Description
                    title = words.subList(0, 5).joinToString(" ")
                    description = words.subList(5, words.size).joinToString(" ")
                }
            }
        }

        // Strip leading split words or punctuation from description
        description = description.trim()
        val descPrefixes = listOf("علشان", "عشان", "لان", "لأن", "بسبب", "because", "due to", "and", "ثم")
        for (dp in descPrefixes) {
            if (description.lowercase(Locale.ROOT).startsWith(dp)) {
                description = description.substring(dp.length).trim()
                break
            }
        }
        if (description.startsWith("و") && !description.startsWith("واحد")) {
            description = description.substring(1).trim()
        }

        // Clean up title and description first letters/capitalization
        title = title.trim()
        if (title.isNotEmpty() && title[0].isLowerCase()) {
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        description = description.trim()
        if (description.isNotEmpty() && description[0].isLowerCase()) {
            description = description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        Log.d(TAG, "Parsed user text: '$userText'")
        Log.d(TAG, "Output -> Title: '$title', Description: '$description', DueDate: '$dueDate', DueTime: '$dueTime', Priority: '$priority', Repeat: '$repeat', Category: '$category'")

        return ParsedTaskResult(
            title = title,
            description = description,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority,
            repeat = repeat,
            category = category
        )
    }

    private fun getNextWeekday(targetDayOfWeek: Int): Date {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var daysDiff = targetDayOfWeek - today
        if (daysDiff < 0) {
            daysDiff += 7
        } else if (daysDiff == 0) {
            // If it's today, we schedule it for today if time is future, but generally "Thursday" on a Thursday implies today or next Thursday. Let's default to today if daysDiff == 0.
        }
        cal.add(Calendar.DAY_OF_YEAR, daysDiff)
        return cal.time
    }
}
