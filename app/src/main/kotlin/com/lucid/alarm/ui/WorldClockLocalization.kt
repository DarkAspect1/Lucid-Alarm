package com.lucid.alarm.ui

import android.content.Context
import android.os.Build
import com.lucid.alarm.R
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

data class WorldCity(
    val zoneId: String,
    val city: String,
    val region: String,
    val country: String = "",
    val searchTerms: List<String> = emptyList(),
    val normCity: String = "",
    val normCityWithoutAl: String = "",
    val normCityWords: List<String> = emptyList(),
    val normTerms: List<String> = emptyList(),
    val normCountry: String = "",
    val normCountryWithoutAl: String = "",
    val normRegion: String = "",
    val normZoneId: String = ""
)

object WorldClockLocalization {

    private val TASHKEEL_REGEX = Regex("[\u064B-\u065F\u0670\u0640]")
    private val DIACRITICS_REGEX = Regex("\\p{M}+")
    private val PUNCT_REGEX = Regex("[\\-_/,.+`'’\"]+")
    private val SPACES_REGEX = Regex("\\s+")

    private val VALID_REGION_PREFIXES = listOf(
        "Africa/",
        "America/",
        "Antarctica/",
        "Arctic/",
        "Asia/",
        "Atlantic/",
        "Australia/",
        "Europe/",
        "Indian/",
        "Pacific/"
    )

    private val EXCLUDED_ZONES = setOf(
        "Australia/ACT",
        "Australia/NSW",
        "Australia/North",
        "Australia/Queensland",
        "Australia/South",
        "Australia/Tasmania",
        "Australia/Victoria",
        "Australia/West",
        "Australia/LHI",
        "Australia/Canberra",
        "Asia/Ulan_Bator",
        "Asia/Calcutta",
        "Asia/Katmandu",
        "Asia/Dacca",
        "Asia/Saigon",
        "Asia/Thimbu",
        "Asia/Rangoon",
        "Asia/Ashkhabad",
        "Europe/Kiev",
        "Europe/Uzhgorod",
        "Europe/Zaporozhye",
        "Africa/Asmera",
        "Africa/Timbuktu",
        "America/Buenos_Aires",
        "America/Catamarca",
        "America/Cordoba",
        "America/Jujuy",
        "America/Mendoza",
        "America/Rosario",
        "America/Indianapolis",
        "America/Knox_IN",
        "America/Louisville",
        "America/Porto_Acre",
        "America/Atka",
        "America/Virgin",
        "America/Shiprock",
        "America/Coral_Harbour",
        "America/Ensenada",
        "America/Santa_Isabel",
        "Pacific/Ponape",
        "Pacific/Truk",
        "Pacific/Yap",
        "Pacific/Samoa",
        "Atlantic/Faeroe",
        "Asia/Chungking",
        "Asia/Ujung_Pandang",
        "Asia/Macao",
        "Asia/Tel_Aviv"
    )

    fun isValidCityZone(zoneId: String): Boolean {
        if (!zoneId.contains("/")) return false
        var hasValidPrefix = false
        for (prefix in VALID_REGION_PREFIXES) {
            if (zoneId.startsWith(prefix)) {
                hasValidPrefix = true
                break
            }
        }
        if (!hasValidPrefix) return false
        if (zoneId.contains("GMT+") || zoneId.contains("GMT-")) return false
        if (zoneId.endsWith("/UTC") || zoneId.endsWith("/UCT") || zoneId.endsWith("/GMT")) return false
        if (EXCLUDED_ZONES.contains(zoneId)) return false
        return true
    }

    private val REGION_NAMES = mapOf(
        "ar" to mapOf(
            "Africa" to "أفريقيا",
            "America" to "أمريكا",
            "Antarctica" to "القارة القطبية الجنوبية",
            "Arctic" to "المنطقة القطبية الشمالية",
            "Asia" to "آسيا",
            "Atlantic" to "المحيط الأطلسي",
            "Australia" to "أستراليا",
            "Europe" to "أوروبا",
            "Indian" to "المحيط الهندي",
            "Pacific" to "المحيط الهادئ"
        ),
        "de" to mapOf(
            "Africa" to "Afrika",
            "America" to "Amerika",
            "Antarctica" to "Antarktis",
            "Arctic" to "Arktis",
            "Asia" to "Asien",
            "Atlantic" to "Atlantik",
            "Australia" to "Australien",
            "Europe" to "Europa",
            "Indian" to "Indischer Ozean",
            "Pacific" to "Pazifik"
        ),
        "es" to mapOf(
            "Africa" to "África",
            "America" to "América",
            "Antarctica" to "Antártida",
            "Arctic" to "Ártico",
            "Asia" to "Asia",
            "Atlantic" to "Atlántico",
            "Australia" to "Australia",
            "Europe" to "Europa",
            "Indian" to "Océano Índico",
            "Pacific" to "Pacífico"
        ),
        "fr" to mapOf(
            "Africa" to "Afrique",
            "America" to "Amérique",
            "Antarctica" to "Antarctique",
            "Arctic" to "Arctique",
            "Asia" to "Asie",
            "Atlantic" to "Atlantique",
            "Australia" to "Australie",
            "Europe" to "Europe",
            "Indian" to "Océan Indien",
            "Pacific" to "Pacifique"
        ),
        "it" to mapOf(
            "Africa" to "Africa",
            "America" to "America",
            "Antarctica" to "Antartide",
            "Arctic" to "Artide",
            "Asia" to "Asia",
            "Atlantic" to "Atlantico",
            "Australia" to "Australia",
            "Europe" to "Europa",
            "Indian" to "Oceano Indiano",
            "Pacific" to "Pacifico"
        ),
        "tr" to mapOf(
            "Africa" to "Afrika",
            "America" to "Amerika",
            "Antarctica" to "Antarktika",
            "Arctic" to "Kuzey Kutbu",
            "Asia" to "Asya",
            "Atlantic" to "Atlantik",
            "Australia" to "Avustralya",
            "Europe" to "Avrupa",
            "Indian" to "Hint Okyanusu",
            "Pacific" to "Pasifik"
        )
    )

    private val MULTI_LANG_CITIES = mapOf(
        "Asia/Riyadh" to mapOf("de" to "Riad", "es" to "Riad", "fr" to "Riyad", "it" to "Riyad", "tr" to "Riyad", "ar" to "الرياض", "en" to "Riyadh"),
        "Asia/Dubai" to mapOf("de" to "Dubai", "es" to "Dubái", "fr" to "Dubaï", "it" to "Dubai", "tr" to "Dubai", "ar" to "دبي", "en" to "Dubai"),
        "Asia/Kuwait" to mapOf("de" to "Kuwait", "es" to "Kuwait", "fr" to "Koweït", "it" to "Kuwait", "tr" to "Kuveyt", "ar" to "الكويت", "en" to "Kuwait"),
        "Asia/Qatar" to mapOf("de" to "Doha", "es" to "Doha", "fr" to "Doha", "it" to "Doha", "tr" to "Doha", "ar" to "الدوحة", "en" to "Doha"),
        "Asia/Bahrain" to mapOf("de" to "Manama", "es" to "Manama", "fr" to "Manama", "it" to "Manama", "tr" to "Manama", "ar" to "المنامة", "en" to "Manama"),
        "Asia/Muscat" to mapOf("de" to "Maskat", "es" to "Mascate", "fr" to "Mascate", "it" to "Mascate", "tr" to "Maskat", "ar" to "مسقط", "en" to "Muscat"),
        "Asia/Amman" to mapOf("de" to "Amman", "es" to "Amán", "fr" to "Amman", "it" to "Amman", "tr" to "Amman", "ar" to "عمان", "en" to "Amman"),
        "Asia/Baghdad" to mapOf("de" to "Bagdad", "es" to "Bagdad", "fr" to "Bagdad", "it" to "Baghdad", "tr" to "Bağdat", "ar" to "بغداد", "en" to "Baghdad"),
        "Asia/Damascus" to mapOf("de" to "Damaskus", "es" to "Damasco", "fr" to "Damas", "it" to "Damasco", "tr" to "Şam", "ar" to "دمشق", "en" to "Damascus"),
        "Asia/Beirut" to mapOf("de" to "Beirut", "es" to "Beirut", "fr" to "Beyrouth", "it" to "Beirut", "tr" to "Beyrut", "ar" to "بيروت", "en" to "Beirut"),
        "Asia/Jerusalem" to mapOf("de" to "Jerusalem", "es" to "Jerusalén", "fr" to "Jérusalem", "it" to "Gerusalemme", "tr" to "Kudüs", "ar" to "القدس", "en" to "Jerusalem"),
        "Asia/Gaza" to mapOf("de" to "Gaza", "es" to "Gaza", "fr" to "Gaza", "it" to "Gaza", "tr" to "Gazze", "ar" to "غزة", "en" to "Gaza"),
        "Asia/Hebron" to mapOf("de" to "Hebron", "es" to "Hebrón", "fr" to "Hébron", "it" to "Hebron", "tr" to "El Halil", "ar" to "الخليل", "en" to "Hebron"),
        "Asia/Aden" to mapOf("de" to "Aden", "es" to "Adén", "fr" to "Aden", "it" to "Aden", "tr" to "Aden", "ar" to "عدن", "en" to "Aden"),
        "Africa/Cairo" to mapOf("de" to "Kairo", "es" to "El Cairo", "fr" to "Le Caire", "it" to "Il Cairo", "tr" to "Kahire", "ar" to "القاهرة", "en" to "Cairo"),
        "Africa/Tripoli" to mapOf("de" to "Tripolis", "es" to "Trípoli", "fr" to "Tripoli", "it" to "Tripoli", "tr" to "Trablus", "ar" to "طرابلس", "en" to "Tripoli"),
        "Africa/Tunis" to mapOf("de" to "Tunis", "es" to "Túnez", "fr" to "Tunis", "it" to "Tunisi", "tr" to "Tunus", "ar" to "تونس", "en" to "Tunis"),
        "Africa/Algiers" to mapOf("de" to "Algier", "es" to "Argel", "fr" to "Alger", "it" to "Algeri", "tr" to "Cezayir", "ar" to "الجزائر", "en" to "Algiers"),
        "Africa/Casablanca" to mapOf("de" to "Casablanca", "es" to "Casablanca", "fr" to "Casablanca", "it" to "Casablanca", "tr" to "Kazablanka", "ar" to "الدار البيضاء", "en" to "Casablanca"),
        "Africa/Khartoum" to mapOf("de" to "Khartum", "es" to "Jartum", "fr" to "Khartoum", "it" to "Khartoum", "tr" to "Hartum", "ar" to "الخرطوم", "en" to "Khartoum"),
        "Africa/Nouakchott" to mapOf("de" to "Nouakchott", "es" to "Nuakchot", "fr" to "Nouakchott", "it" to "Nouakchott", "tr" to "Nuakşot", "ar" to "نواكشوط", "en" to "Nouakchott"),
        "Africa/Mogadishu" to mapOf("de" to "Mogadischu", "es" to "Mogadiscio", "fr" to "Mogadiscio", "it" to "Mogadiscio", "tr" to "Mogadişu", "ar" to "مقديشو", "en" to "Mogadishu"),
        "Africa/Djibouti" to mapOf("de" to "Dschibuti", "es" to "Yibuti", "fr" to "Djibouti", "it" to "Gibuti", "tr" to "Cibuti", "ar" to "جيبوتي", "en" to "Djibouti"),
        "Africa/El_Aaiun" to mapOf("de" to "El Aaiún", "es" to "El Aaiún", "fr" to "Laâyoune", "it" to "El Aaiún", "tr" to "Layun", "ar" to "العيون", "en" to "El Aaiun"),
        "Indian/Comoro" to mapOf("de" to "Moroni", "es" to "Moroni", "fr" to "Moroni", "it" to "Moroni", "tr" to "Moroni", "ar" to "موروني", "en" to "Moroni"),
        "Asia/Tehran" to mapOf("de" to "Teheran", "es" to "Teherán", "fr" to "Téhéran", "it" to "Teheran", "tr" to "Tahran", "ar" to "طهران", "en" to "Tehran"),
        "Asia/Kabul" to mapOf("de" to "Kabul", "es" to "Kabul", "fr" to "Kaboul", "it" to "Kabul", "tr" to "Kabil", "ar" to "كابول", "en" to "Kabul"),
        "Asia/Karachi" to mapOf("de" to "Karatschi", "es" to "Karachi", "fr" to "Karachi", "it" to "Karachi", "tr" to "Karaçi", "ar" to "كراتشي", "en" to "Karachi"),
        "Asia/Kolkata" to mapOf("de" to "Neu-Delhi", "es" to "Nueva Delhi", "fr" to "New Delhi", "it" to "Nuova Delhi", "tr" to "Yeni Delhi", "ar" to "نيودلهي", "en" to "New Delhi"),
        "Asia/Dhaka" to mapOf("de" to "Dhaka", "es" to "Daca", "fr" to "Dacca", "it" to "Dacca", "tr" to "Dakka", "ar" to "دكا", "en" to "Dhaka"),
        "Asia/Colombo" to mapOf("de" to "Colombo", "es" to "Colombo", "fr" to "Colombo", "it" to "Colombo", "tr" to "Kolombo", "ar" to "كولومبو", "en" to "Colombo"),
        "Asia/Bangkok" to mapOf("de" to "Bangkok", "es" to "Bangkok", "fr" to "Bangkok", "it" to "Bangkok", "tr" to "Bangkok", "ar" to "بانكوك", "en" to "Bangkok"),
        "Asia/Jakarta" to mapOf("de" to "Jakarta", "es" to "Yakarta", "fr" to "Jakarta", "it" to "Giacarta", "tr" to "Cakarta", "ar" to "جاكرتا", "en" to "Jakarta"),
        "Asia/Kuala_Lumpur" to mapOf("de" to "Kuala Lumpur", "es" to "Kuala Lumpur", "fr" to "Kuala Lumpur", "it" to "Kuala Lumpur", "tr" to "Kuala Lumpur", "ar" to "كوالالمبور", "en" to "Kuala Lumpur"),
        "Asia/Singapore" to mapOf("de" to "Singapur", "es" to "Singapur", "fr" to "Singapour", "it" to "Singapore", "tr" to "Singapur", "ar" to "سنغافورة", "en" to "Singapore"),
        "Asia/Manila" to mapOf("de" to "Manila", "es" to "Manila", "fr" to "Manille", "it" to "Manila", "tr" to "Manila", "ar" to "مانيلا", "en" to "Manila"),
        "Asia/Hong_Kong" to mapOf("de" to "Hongkong", "es" to "Hong Kong", "fr" to "Hong Kong", "it" to "Hong Kong", "tr" to "Hong Kong", "ar" to "هونغ كونغ", "en" to "Hong Kong"),
        "Asia/Taipei" to mapOf("de" to "Taipeh", "es" to "Taipéi", "fr" to "Taipei", "it" to "Taipei", "tr" to "Taipei", "ar" to "تايبيه", "en" to "Taipei"),
        "Asia/Seoul" to mapOf("de" to "Seoul", "es" to "Seúl", "fr" to "Séoul", "it" to "Seul", "tr" to "Seul", "ar" to "سيول", "en" to "Seoul"),
        "Asia/Tokyo" to mapOf("de" to "Tokio", "es" to "Tokio", "fr" to "Tokyo", "it" to "Tokyo", "tr" to "Tokyo", "ar" to "طوكيو", "en" to "Tokyo"),
        "Asia/Tashkent" to mapOf("de" to "Taschkent", "es" to "Taskent", "fr" to "Tachkent", "it" to "Tashkent", "tr" to "Taşkent", "ar" to "طشقند", "en" to "Tashkent"),
        "Asia/Baku" to mapOf("de" to "Baku", "es" to "Bakú", "fr" to "Bakou", "it" to "Baku", "tr" to "Bakü", "ar" to "باكو", "en" to "Baku"),
        "Asia/Tbilisi" to mapOf("de" to "Tiflis", "es" to "Tiflis", "fr" to "Tbilissi", "it" to "Tbilisi", "tr" to "Tiflis", "ar" to "تبليسي", "en" to "Tbilisi"),
        "Asia/Yerevan" to mapOf("de" to "Eriwan", "es" to "Ereván", "fr" to "Erevan", "it" to "Erevan", "tr" to "Erivan", "ar" to "يريفان", "en" to "Yerevan"),
        "Asia/Almaty" to mapOf("de" to "Almaty", "es" to "Almaty", "fr" to "Almaty", "it" to "Almaty", "tr" to "Almatı", "ar" to "ألماتي", "en" to "Almaty"),
        "Asia/Bishkek" to mapOf("de" to "Bischkek", "es" to "Biskek", "fr" to "Bichkek", "it" to "Bishkek", "tr" to "Bişkek", "ar" to "بيشكك", "en" to "Bishkek"),
        "Asia/Dushanbe" to mapOf("de" to "Duschanbe", "es" to "Dusambé", "fr" to "Douchanbé", "it" to "Dushanbe", "tr" to "Duşanbe", "ar" to "دوشانبي", "en" to "Dushanbe"),
        "Asia/Ashgabat" to mapOf("de" to "Aschgabat", "es" to "Asjabad", "fr" to "Achgabat", "it" to "Ashgabat", "tr" to "Aşkabat", "ar" to "عشق آباد", "en" to "Ashgabat"),
        "Asia/Ulaanbaatar" to mapOf("de" to "Ulaanbaatar", "es" to "Ulán Bator", "fr" to "Oulan-Bator", "it" to "Ulan Bator", "tr" to "Ulan Batur", "ar" to "أولان باتور", "en" to "Ulaanbaatar"),
        "Asia/Choibalsan" to mapOf("de" to "Tschoibalsan", "es" to "Choybalsan", "fr" to "Tchoibalsan", "it" to "Choibalsan", "tr" to "Çoybalsan", "ar" to "تشويبالسان", "en" to "Choibalsan"),
        "Asia/Hovd" to mapOf("de" to "Chowd", "es" to "Jovd", "fr" to "Khovd", "it" to "Hovd", "tr" to "Hovd", "ar" to "خوفد", "en" to "Hovd"),
        "Asia/Kathmandu" to mapOf("de" to "Kathmandu", "es" to "Katmandú", "fr" to "Katmandou", "it" to "Katmandu", "tr" to "Katmandu", "ar" to "كاتماندو", "en" to "Kathmandu"),
        "Asia/Thimphu" to mapOf("de" to "Thimphu", "es" to "Timbu", "fr" to "Thimphou", "it" to "Thimphu", "tr" to "Thimphu", "ar" to "تيمفو", "en" to "Thimphu"),
        "Asia/Pyongyang" to mapOf("de" to "Pjöngjang", "es" to "Pionyang", "fr" to "Pyongyang", "it" to "Pyongyang", "tr" to "Pyongyang", "ar" to "بيونغ يانغ", "en" to "Pyongyang"),
        "Asia/Shanghai" to mapOf("de" to "Shanghai", "es" to "Shanghái", "fr" to "Shanghai", "it" to "Shanghai", "tr" to "Şanghay", "ar" to "شنغهاي", "en" to "Shanghai"),
        "Asia/Chongqing" to mapOf("de" to "Chongqing", "es" to "Chongqing", "fr" to "Chongqing", "it" to "Chongqing", "tr" to "Çongçing", "ar" to "تشونغتشينغ", "en" to "Chongqing"),
        "Asia/Urumqi" to mapOf("de" to "Ürümqi", "es" to "Urumqi", "fr" to "Ürümqi", "it" to "Urumqi", "tr" to "Urumçi", "ar" to "أورومتشي", "en" to "Urumqi"),
        "Asia/Harbin" to mapOf("de" to "Harbin", "es" to "Harbin", "fr" to "Harbin", "it" to "Harbin", "tr" to "Harbin", "ar" to "هاربين", "en" to "Harbin"),
        "Asia/Macau" to mapOf("de" to "Macao", "es" to "Macao", "fr" to "Macao", "it" to "Macao", "tr" to "Makao", "ar" to "ماكاو", "en" to "Macau"),
        "Asia/Ho_Chi_Minh" to mapOf("de" to "Ho-Chi-Minh-Stadt", "es" to "Ciudad Ho Chi Minh", "fr" to "Hô-Chi-Minh-Ville", "it" to "Ho Chi Minh", "tr" to "Ho Chi Minh", "ar" to "مدينة هو تشي منه", "en" to "Ho Chi Minh City"),
        "Asia/Phnom_Penh" to mapOf("de" to "Phnom Penh", "es" to "Nom Pen", "fr" to "Phnom Penh", "it" to "Phnom Penh", "tr" to "Phnom Penh", "ar" to "بنوم بنه", "en" to "Phnom Penh"),
        "Asia/Vientiane" to mapOf("de" to "Vientiane", "es" to "Vientián", "fr" to "Vientiane", "it" to "Vientiane", "tr" to "Vientiane", "ar" to "فيينتيان", "en" to "Vientiane"),
        "Asia/Yangon" to mapOf("de" to "Rangun", "es" to "Rangún", "fr" to "Rangoun", "it" to "Yangon", "tr" to "Yangon", "ar" to "يانغون", "en" to "Yangon"),
        "Asia/Brunei" to mapOf("de" to "Bandar Seri Begawan", "es" to "Bandar Seri Begawan", "fr" to "Bandar Seri Begawan", "it" to "Bandar Seri Begawan", "tr" to "Bandar Seri Begavan", "ar" to "بندر سري بكاوان", "en" to "Bandar Seri Begawan"),
        "Asia/Nicosia" to mapOf("de" to "Nikosia", "es" to "Nicosia", "fr" to "Nicosie", "it" to "Nicosia", "tr" to "Lefkoşa", "ar" to "نيقوسيا", "en" to "Nicosia"),
        "Asia/Famagusta" to mapOf("de" to "Famagusta", "es" to "Famagusta", "fr" to "Famagouste", "it" to "Famagosta", "tr" to "Gazimağusa", "ar" to "فاماغوستا", "en" to "Famagusta"),
        "Asia/Samarkand" to mapOf("de" to "Samarkand", "es" to "Samarcanda", "fr" to "Samarcande", "it" to "Samarcanda", "tr" to "Semerkant", "ar" to "سمرقند", "en" to "Samarkand"),
        "Asia/Dili" to mapOf("de" to "Dili", "es" to "Dili", "fr" to "Dili", "it" to "Dili", "tr" to "Dili", "ar" to "ديلي", "en" to "Dili"),
        "Asia/Makassar" to mapOf("de" to "Makassar", "es" to "Makasar", "fr" to "Macassar", "it" to "Makassar", "tr" to "Makassar", "ar" to "ماكاسار", "en" to "Makassar"),
        "Asia/Jayapura" to mapOf("de" to "Jayapura", "es" to "Jayapura", "fr" to "Jayapura", "it" to "Jayapura", "tr" to "Jayapura", "ar" to "جايابورا", "en" to "Jayapura"),
        "Asia/Pontianak" to mapOf("de" to "Pontianak", "es" to "Pontianak", "fr" to "Pontianak", "it" to "Pontianak", "tr" to "Pontianak", "ar" to "بونتياناك", "en" to "Pontianak"),
        "Asia/Kuching" to mapOf("de" to "Kuching", "es" to "Kuching", "fr" to "Kuching", "it" to "Kuching", "tr" to "Kuching", "ar" to "كوتشينغ", "en" to "Kuching"),
        "Asia/Vladivostok" to mapOf("de" to "Wladiwostok", "es" to "Vladivostok", "fr" to "Vladivostok", "it" to "Vladivostok", "tr" to "Vladivostok", "ar" to "فلاديفوستوك", "en" to "Vladivostok"),
        "Asia/Novosibirsk" to mapOf("de" to "Nowosibirsk", "es" to "Novosibirsk", "fr" to "Novossibirsk", "it" to "Novosibirsk", "tr" to "Novosibirsk", "ar" to "نوفوسيبيرسك", "en" to "Novosibirsk"),
        "Asia/Yekaterinburg" to mapOf("de" to "Jekaterinburg", "es" to "Ekaterimburgo", "fr" to "Iekaterinbourg", "it" to "Ekaterinburg", "tr" to "Yekaterinburg", "ar" to "يكاترينبورغ", "en" to "Yekaterinburg"),
        "Asia/Irkutsk" to mapOf("de" to "Irkutsk", "es" to "Irkutsk", "fr" to "Irkoutsk", "it" to "Irkutsk", "tr" to "İrkutsk", "ar" to "إيركوتسك", "en" to "Irkutsk"),
        "Asia/Krasnoyarsk" to mapOf("de" to "Krasnojarsk", "es" to "Krasnoyarsk", "fr" to "Krasnoïarsk", "it" to "Krasnojarsk", "tr" to "Krasnoyarsk", "ar" to "كراسنويارسك", "en" to "Krasnoyarsk"),
        "Asia/Omsk" to mapOf("de" to "Omsk", "es" to "Omsk", "fr" to "Omsk", "it" to "Omsk", "tr" to "Omsk", "ar" to "أومسك", "en" to "Omsk"),
        "Asia/Yakutsk" to mapOf("de" to "Jakutsk", "es" to "Yakutsk", "fr" to "Iakoutsk", "it" to "Jakutsk", "tr" to "Yakutsk", "ar" to "ياكوتسك", "en" to "Yakutsk"),
        "Asia/Kamchatka" to mapOf("de" to "Kamtschatka", "es" to "Kamchatka", "fr" to "Kamtchatka", "it" to "Kamchatka", "tr" to "Kamçatka", "ar" to "كامشاتكا", "en" to "Kamchatka"),
        "Asia/Magadan" to mapOf("de" to "Magadan", "es" to "Magadán", "fr" to "Magadan", "it" to "Magadan", "tr" to "Magadan", "ar" to "ماغادان", "en" to "Magadan"),
        "Asia/Sakhalin" to mapOf("de" to "Sachalin", "es" to "Sajalín", "fr" to "Sakhaline", "it" to "Sachalin", "tr" to "Sahalin", "ar" to "ساخالين", "en" to "Sakhalin"),
        "Asia/Aqtobe" to mapOf("de" to "Aktobe", "es" to "Aktobé", "fr" to "Aqtöbe", "it" to "Aktobe", "tr" to "Aktöbe", "ar" to "أكتوبي", "en" to "Aktobe"),
        "Asia/Aqtau" to mapOf("de" to "Aktau", "es" to "Aktau", "fr" to "Aktaou", "it" to "Aktau", "tr" to "Aktav", "ar" to "أكتاو", "en" to "Aktau"),
        "Asia/Atyrau" to mapOf("de" to "Atyrau", "es" to "Atirau", "fr" to "Atyraou", "it" to "Atyrau", "tr" to "Atırav", "ar" to "أتيراو", "en" to "Atyrau"),
        "Asia/Qyzylorda" to mapOf("de" to "Qysylorda", "es" to "Kyzylorda", "fr" to "Kizilorda", "it" to "Kyzylorda", "tr" to "Kızılorda", "ar" to "كيزيلوردا", "en" to "Qyzylorda"),
        "Asia/Qostanay" to mapOf("de" to "Qostanai", "es" to "Kostanái", "fr" to "Kostanaï", "it" to "Kostanay", "tr" to "Kostanay", "ar" to "كوستاناي", "en" to "Qostanay"),
        "Asia/Oral" to mapOf("de" to "Oral", "es" to "Oral", "fr" to "Oural", "it" to "Oral", "tr" to "Oral", "ar" to "أورال", "en" to "Oral"),
        "Europe/Rome" to mapOf("de" to "Rom", "es" to "Roma", "fr" to "Rome", "it" to "Roma", "tr" to "Roma", "ar" to "روما", "en" to "Rome"),
        "Europe/London" to mapOf("de" to "London", "es" to "Londres", "fr" to "Londres", "it" to "Londra", "tr" to "Londra", "ar" to "لندن", "en" to "London"),
        "Europe/Paris" to mapOf("de" to "Paris", "es" to "París", "fr" to "Paris", "it" to "Parigi", "tr" to "Paris", "ar" to "باريس", "en" to "Paris"),
        "Europe/Vienna" to mapOf("de" to "Wien", "es" to "Viena", "fr" to "Vienne", "it" to "Vienna", "tr" to "Viyana", "ar" to "فيينا", "en" to "Vienna"),
        "Europe/Warsaw" to mapOf("de" to "Warschau", "es" to "Varsovia", "fr" to "Varsovie", "it" to "Varsavia", "tr" to "Varşova", "ar" to "وارسو", "en" to "Warsaw"),
        "Europe/Moscow" to mapOf("de" to "Moskau", "es" to "Moscú", "fr" to "Moscou", "it" to "Mosca", "tr" to "Moskova", "ar" to "موسكو", "en" to "Moscow"),
        "Europe/Prague" to mapOf("de" to "Prag", "es" to "Praga", "fr" to "Prague", "it" to "Praga", "tr" to "Prag", "ar" to "براغ", "en" to "Prague"),
        "Europe/Brussels" to mapOf("de" to "Brüssel", "es" to "Bruselas", "fr" to "Bruxelles", "it" to "Bruxelles", "tr" to "Brüksel", "ar" to "بروكسل", "en" to "Brussels"),
        "Europe/Athens" to mapOf("de" to "Athen", "es" to "Atenas", "fr" to "Athènes", "it" to "Atene", "tr" to "Atina", "ar" to "أثينا", "en" to "Athens"),
        "Europe/Lisbon" to mapOf("de" to "Lissabon", "es" to "Lisboa", "fr" to "Lisbonne", "it" to "Lisbona", "tr" to "Lizbon", "ar" to "لشبونة", "en" to "Lisbon"),
        "Europe/Madrid" to mapOf("de" to "Madrid", "es" to "Madrid", "fr" to "Madrid", "it" to "Madrid", "tr" to "Madrid", "ar" to "مدريد", "en" to "Madrid"),
        "Europe/Berlin" to mapOf("de" to "Berlin", "es" to "Berlín", "fr" to "Berlin", "it" to "Berlino", "tr" to "Berlin", "ar" to "برلين", "en" to "Berlin"),
        "Europe/Amsterdam" to mapOf("de" to "Amsterdam", "es" to "Ámsterdam", "fr" to "Amsterdam", "it" to "Amsterdam", "tr" to "Amsterdam", "ar" to "أمستردام", "en" to "Amsterdam"),
        "Europe/Dublin" to mapOf("de" to "Dublin", "es" to "Dublín", "fr" to "Dublin", "it" to "Dublino", "tr" to "Dublin", "ar" to "دبلن", "en" to "Dublin"),
        "Europe/Helsinki" to mapOf("de" to "Helsinki", "es" to "Helsinki", "fr" to "Helsinki", "it" to "Helsinki", "tr" to "Helsinki", "ar" to "هلسنكي", "en" to "Helsinki"),
        "Europe/Stockholm" to mapOf("de" to "Stockholm", "es" to "Estocolmo", "fr" to "Stockholm", "it" to "Stoccolma", "tr" to "Stokholm", "ar" to "ستوكهولم", "en" to "Stockholm"),
        "Europe/Oslo" to mapOf("de" to "Oslo", "es" to "Oslo", "fr" to "Oslo", "it" to "Oslo", "tr" to "Oslo", "ar" to "أوسلو", "en" to "Oslo"),
        "Europe/Copenhagen" to mapOf("de" to "Kopenhagen", "es" to "Copenhague", "fr" to "Copenhague", "it" to "Copenaghen", "tr" to "Kopenhag", "ar" to "كوبنهاغن", "en" to "Copenhagen"),
        "Europe/Budapest" to mapOf("de" to "Budapest", "es" to "Budapest", "fr" to "Budapest", "it" to "Budapest", "tr" to "Budapeşte", "ar" to "بودابست", "en" to "Budapest"),
        "Europe/Bucharest" to mapOf("de" to "Bukarest", "es" to "Bucarest", "fr" to "Bucarest", "it" to "Bucarest", "tr" to "Bükreş", "ar" to "بوخارست", "en" to "Bucharest"),
        "Europe/Belgrade" to mapOf("de" to "Belgrad", "es" to "Belgrado", "fr" to "Belgrade", "it" to "Belgrado", "tr" to "Belgrad", "ar" to "بلغراد", "en" to "Belgrade"),
        "Europe/Sofia" to mapOf("de" to "Sofia", "es" to "Sofía", "fr" to "Sofia", "it" to "Sofia", "tr" to "Sofya", "ar" to "صوفيا", "en" to "Sofia"),
        "Europe/Kyiv" to mapOf("de" to "Kiew", "es" to "Kiev", "fr" to "Kiev", "it" to "Kiev", "tr" to "Kiev", "ar" to "كييف", "en" to "Kyiv"),
        "Europe/Zurich" to mapOf("de" to "Zürich", "es" to "Zúrich", "fr" to "Zurich", "it" to "Zurigo", "tr" to "Zürih", "ar" to "زيورخ", "en" to "Zurich"),
        "Europe/Istanbul" to mapOf("de" to "Istanbul", "es" to "Estambul", "fr" to "Istanbul", "it" to "Istanbul", "tr" to "İstanbul", "ar" to "إسطنبول", "en" to "Istanbul"),
        "Europe/Bratislava" to mapOf("de" to "Bratislava", "es" to "Bratislava", "fr" to "Bratislava", "it" to "Bratislava", "tr" to "Bratislava", "ar" to "براتيسلافا", "en" to "Bratislava"),
        "Europe/Zagreb" to mapOf("de" to "Zagreb", "es" to "Zagreb", "fr" to "Zagreb", "it" to "Zagabria", "tr" to "Zagreb", "ar" to "زغرب", "en" to "Zagreb"),
        "Europe/Ljubljana" to mapOf("de" to "Ljubljana", "es" to "Liubliana", "fr" to "Ljubljana", "it" to "Lubiana", "tr" to "Ljubljana", "ar" to "ليوبليانا", "en" to "Ljubljana"),
        "Europe/Sarajevo" to mapOf("de" to "Sarajevo", "es" to "Sarajevo", "fr" to "Sarajevo", "it" to "Sarajevo", "tr" to "Saraybosna", "ar" to "سراييفو", "en" to "Sarajevo"),
        "Europe/Skopje" to mapOf("de" to "Skopje", "es" to "Skopie", "fr" to "Skopje", "it" to "Skopje", "tr" to "Üsküp", "ar" to "سكوبيه", "en" to "Skopje"),
        "Europe/Tirane" to mapOf("de" to "Tirana", "es" to "Tirana", "fr" to "Tirana", "it" to "Tirana", "tr" to "Tiran", "ar" to "تيرانا", "en" to "Tirane"),
        "Europe/Podgorica" to mapOf("de" to "Podgorica", "es" to "Podgorica", "fr" to "Podgorica", "it" to "Podgorica", "tr" to "Podgoritsa", "ar" to "بودغوريتسا", "en" to "Podgorica"),
        "Europe/Riga" to mapOf("de" to "Riga", "es" to "Riga", "fr" to "Riga", "it" to "Riga", "tr" to "Riga", "ar" to "ريغا", "en" to "Riga"),
        "Europe/Vilnius" to mapOf("de" to "Vilnius", "es" to "Vilna", "fr" to "Vilnius", "it" to "Vilnius", "tr" to "Vilnius", "ar" to "فيلنيوس", "en" to "Vilnius"),
        "Europe/Tallinn" to mapOf("de" to "Tallinn", "es" to "Tallin", "fr" to "Tallinn", "it" to "Tallinn", "tr" to "Tallinn", "ar" to "تالين", "en" to "Tallinn"),
        "Europe/Minsk" to mapOf("de" to "Minsk", "es" to "Minsk", "fr" to "Minsk", "it" to "Minsk", "tr" to "Minsk", "ar" to "مينسك", "en" to "Minsk"),
        "Europe/Chisinau" to mapOf("de" to "Chisinau", "es" to "Chisináu", "fr" to "Chisinau", "it" to "Chisinau", "tr" to "Kişinev", "ar" to "كيشيناو", "en" to "Chisinau"),
        "Europe/Luxembourg" to mapOf("de" to "Luxemburg", "es" to "Luxemburgo", "fr" to "Luxembourg", "it" to "Lussemburgo", "tr" to "Lüksemburg", "ar" to "لوكسمبورغ", "en" to "Luxembourg"),
        "Europe/Monaco" to mapOf("de" to "Monaco", "es" to "Mónaco", "fr" to "Monaco", "it" to "Monaco", "tr" to "Monako", "ar" to "موناكو", "en" to "Monaco"),
        "Europe/Andorra" to mapOf("de" to "Andorra", "es" to "Andorra", "fr" to "Andorre", "it" to "Andorra", "tr" to "Andorra", "ar" to "أندورا", "en" to "Andorra"),
        "Europe/San_Marino" to mapOf("de" to "San Marino", "es" to "San Marino", "fr" to "Saint-Marin", "it" to "San Marino", "tr" to "San Marino", "ar" to "سان مارينو", "en" to "San Marino"),
        "Europe/Vatican" to mapOf("de" to "Vatikan", "es" to "Vaticano", "fr" to "Vatican", "it" to "Vaticano", "tr" to "Vatikan", "ar" to "الفاتيكان", "en" to "Vatican"),
        "Europe/Malta" to mapOf("de" to "Valletta", "es" to "La Valeta", "fr" to "La Valette", "it" to "La Valletta", "tr" to "Valletta", "ar" to "فاليتا", "en" to "Valletta"),
        "Europe/Reykjavik" to mapOf("de" to "Reykjavik", "es" to "Reikiavik", "fr" to "Reykjavik", "it" to "Reykjavik", "tr" to "Reykjavik", "ar" to "ريكيافيك", "en" to "Reykjavik"),
        "Europe/Gibraltar" to mapOf("de" to "Gibraltar", "es" to "Gibraltar", "fr" to "Gibraltar", "it" to "Gibilterra", "tr" to "Cebelitarık", "ar" to "جبل طارق", "en" to "Gibraltar"),
        "Europe/Kaliningrad" to mapOf("de" to "Kaliningrad", "es" to "Kaliningrado", "fr" to "Kaliningrad", "it" to "Kaliningrad", "tr" to "Kaliningrad", "ar" to "كالينينغراد", "en" to "Kaliningrad"),
        "Europe/Samara" to mapOf("de" to "Samara", "es" to "Samara", "fr" to "Samara", "it" to "Samara", "tr" to "Samara", "ar" to "سامارا", "en" to "Samara"),
        "Europe/Volgograd" to mapOf("de" to "Wolgograd", "es" to "Volgogrado", "fr" to "Volgograd", "it" to "Volgograd", "tr" to "Volgograd", "ar" to "فولغوغراد", "en" to "Volgograd"),
        "Europe/Saratov" to mapOf("de" to "Saratow", "es" to "Sarátov", "fr" to "Saratov", "it" to "Saratov", "tr" to "Saratov", "ar" to "ساراتوف", "en" to "Saratov"),
        "Europe/Ulyanovsk" to mapOf("de" to "Uljanowsk", "es" to "Uliánovsk", "fr" to "Oulianovsk", "it" to "Ul'janovsk", "tr" to "Ulyanovsk", "ar" to "أوليانوفسك", "en" to "Ulyanovsk"),
        "Europe/Astrakhan" to mapOf("de" to "Astrachan", "es" to "Astracán", "fr" to "Astrakhan", "it" to "Astrachan'", "tr" to "Astrahan", "ar" to "أستراخان", "en" to "Astrakhan"),
        "Europe/Kirov" to mapOf("de" to "Kirow", "es" to "Kírov", "fr" to "Kirov", "it" to "Kirov", "tr" to "Kirov", "ar" to "كيروف", "en" to "Kirov"),
        "America/New_York" to mapOf("de" to "New York", "es" to "Nueva York", "fr" to "New York", "it" to "New York", "tr" to "New York", "ar" to "نيويورك", "en" to "New York"),
        "America/Chicago" to mapOf("de" to "Chicago", "es" to "Chicago", "fr" to "Chicago", "it" to "Chicago", "tr" to "Şikago", "ar" to "شيكاغو", "en" to "Chicago"),
        "America/Los_Angeles" to mapOf("de" to "Los Angeles", "es" to "Los Ángeles", "fr" to "Los Angeles", "it" to "Los Angeles", "tr" to "Los Angeles", "ar" to "لوس أنجلوس", "en" to "Los Angeles"),
        "America/Denver" to mapOf("de" to "Denver", "es" to "Denver", "fr" to "Denver", "it" to "Denver", "tr" to "Denver", "ar" to "دنفر", "en" to "Denver"),
        "America/Phoenix" to mapOf("de" to "Phoenix", "es" to "Phoenix", "fr" to "Phoenix", "it" to "Phoenix", "tr" to "Phoenix", "ar" to "فينيكس", "en" to "Phoenix"),
        "America/Detroit" to mapOf("de" to "Detroit", "es" to "Detroit", "fr" to "Détroit", "it" to "Detroit", "tr" to "Detroit", "ar" to "ديترويت", "en" to "Detroit"),
        "America/Anchorage" to mapOf("de" to "Anchorage", "es" to "Anchorage", "fr" to "Anchorage", "it" to "Anchorage", "tr" to "Anchorage", "ar" to "أنكوريج", "en" to "Anchorage"),
        "Pacific/Honolulu" to mapOf("de" to "Honolulu", "es" to "Honolulu", "fr" to "Honolulu", "it" to "Honolulu", "tr" to "Honolulu", "ar" to "هونولولو", "en" to "Honolulu"),
        "America/Toronto" to mapOf("de" to "Toronto", "es" to "Toronto", "fr" to "Toronto", "it" to "Toronto", "tr" to "Toronto", "ar" to "تورونتو", "en" to "Toronto"),
        "America/Montreal" to mapOf("de" to "Montreal", "es" to "Montreal", "fr" to "Montréal", "it" to "Montréal", "tr" to "Montreal", "ar" to "مونتريال", "en" to "Montreal"),
        "America/Vancouver" to mapOf("de" to "Vancouver", "es" to "Vancouver", "fr" to "Vancouver", "it" to "Vancouver", "tr" to "Vancouver", "ar" to "فانكوفر", "en" to "Vancouver"),
        "America/Edmonton" to mapOf("de" to "Edmonton", "es" to "Edmonton", "fr" to "Edmonton", "it" to "Edmonton", "tr" to "Edmonton", "ar" to "إدمونتون", "en" to "Edmonton"),
        "America/Winnipeg" to mapOf("de" to "Winnipeg", "es" to "Winnipeg", "fr" to "Winnipeg", "it" to "Winnipeg", "tr" to "Winnipeg", "ar" to "وينيبيغ", "en" to "Winnipeg"),
        "America/Halifax" to mapOf("de" to "Halifax", "es" to "Halifax", "fr" to "Halifax", "it" to "Halifax", "tr" to "Halifax", "ar" to "هاليفاكس", "en" to "Halifax"),
        "America/St_Johns" to mapOf("de" to "St. John's", "es" to "San Juan de Terranova", "fr" to "Saint-Jean de Terre-Neuve", "it" to "St. John's", "tr" to "St. John's", "ar" to "سانت جونز", "en" to "St. John's"),
        "America/Mexico_City" to mapOf("de" to "Mexiko-Stadt", "es" to "Ciudad de México", "fr" to "Mexico", "it" to "Città del Messico", "tr" to "Meksiko", "ar" to "مكسيكو سيتي", "en" to "Mexico City"),
        "America/Cancun" to mapOf("de" to "Cancún", "es" to "Cancún", "fr" to "Cancún", "it" to "Cancún", "tr" to "Cancun", "ar" to "كانكون", "en" to "Cancun"),
        "America/Monterrey" to mapOf("de" to "Monterrey", "es" to "Monterrey", "fr" to "Monterrey", "it" to "Monterrey", "tr" to "Monterrey", "ar" to "مونتيري", "en" to "Monterrey"),
        "America/Tijuana" to mapOf("de" to "Tijuana", "es" to "Tijuana", "fr" to "Tijuana", "it" to "Tijuana", "tr" to "Tijuana", "ar" to "تيخوانا", "en" to "Tijuana"),
        "America/Havana" to mapOf("de" to "Havanna", "es" to "La Habana", "fr" to "La Havane", "it" to "L'Avana", "tr" to "Havana", "ar" to "هافانا", "en" to "Havana"),
        "America/Bogota" to mapOf("de" to "Bogota", "es" to "Bogotá", "fr" to "Bogota", "it" to "Bogotà", "tr" to "Bogota", "ar" to "بوغوتا", "en" to "Bogota"),
        "America/Lima" to mapOf("de" to "Lima", "es" to "Lima", "fr" to "Lima", "it" to "Lima", "tr" to "Lima", "ar" to "ليما", "en" to "Lima"),
        "America/Santiago" to mapOf("de" to "Santiago", "es" to "Santiago", "fr" to "Santiago", "it" to "Santiago", "tr" to "Santiago", "ar" to "سانتياغو", "en" to "Santiago"),
        "America/Sao_Paulo" to mapOf("de" to "São Paulo", "es" to "São Paulo", "fr" to "São Paulo", "it" to "San Paolo", "tr" to "São Paulo", "ar" to "ساو باولو", "en" to "Sao Paulo"),
        "America/Argentina/Buenos_Aires" to mapOf("de" to "Buenos Aires", "es" to "Buenos Aires", "fr" to "Buenos Aires", "it" to "Buenos Aires", "tr" to "Buenos Aires", "ar" to "بوينس آيرس", "en" to "Buenos Aires"),
        "America/Caracas" to mapOf("de" to "Caracas", "es" to "Caracas", "fr" to "Caracas", "it" to "Caracas", "tr" to "Karakas", "ar" to "كاراكاس", "en" to "Caracas"),
        "America/Panama" to mapOf("de" to "Panama-Stadt", "es" to "Ciudad de Panamá", "fr" to "Panama", "it" to "Panama", "tr" to "Panama", "ar" to "بنما", "en" to "Panama City"),
        "America/Costa_Rica" to mapOf("de" to "San José", "es" to "San José", "fr" to "San José", "it" to "San José", "tr" to "San Jose", "ar" to "سان خوسيه", "en" to "San Jose"),
        "America/Guatemala" to mapOf("de" to "Guatemala-Stadt", "es" to "Ciudad de Guatemala", "fr" to "Guatemala", "it" to "Guatemala", "tr" to "Guatemala", "ar" to "غواتيمالا", "en" to "Guatemala City"),
        "America/El_Salvador" to mapOf("de" to "San Salvador", "es" to "San Salvador", "fr" to "San Salvador", "it" to "San Salvador", "tr" to "San Salvador", "ar" to "سان سلفادور", "en" to "San Salvador"),
        "America/Tegucigalpa" to mapOf("de" to "Tegucigalpa", "es" to "Tegucigalpa", "fr" to "Tegucigalpa", "it" to "Tegucigalpa", "tr" to "Tegucigalpa", "ar" to "تيغوسيغالبا", "en" to "Tegucigalpa"),
        "America/Managua" to mapOf("de" to "Managua", "es" to "Managua", "fr" to "Managua", "it" to "Managua", "tr" to "Managua", "ar" to "ماناغوا", "en" to "Managua"),
        "America/Asuncion" to mapOf("de" to "Asunción", "es" to "Asunción", "fr" to "Asunción", "it" to "Asunción", "tr" to "Asuncion", "ar" to "أسونسيون", "en" to "Asuncion"),
        "America/Montevideo" to mapOf("de" to "Montevideo", "es" to "Montevideo", "fr" to "Montevideo", "it" to "Montevideo", "tr" to "Montevideo", "ar" to "مونتيفيديو", "en" to "Montevideo"),
        "America/La_Paz" to mapOf("de" to "La Paz", "es" to "La Paz", "fr" to "La Paz", "it" to "La Paz", "tr" to "La Paz", "ar" to "لاباز", "en" to "La Paz"),
        "America/Guayaquil" to mapOf("de" to "Guayaquil", "es" to "Guayaquil", "fr" to "Guayaquil", "it" to "Guayaquil", "tr" to "Guayaquil", "ar" to "غواياكيل", "en" to "Guayaquil"),
        "America/Puerto_Rico" to mapOf("de" to "San Juan", "es" to "San Juan", "fr" to "San Juan", "it" to "San Juan", "tr" to "San Juan", "ar" to "سان خوان", "en" to "San Juan"),
        "America/Santo_Domingo" to mapOf("de" to "Santo Domingo", "es" to "Santo Domingo", "fr" to "Saint-Domingue", "it" to "Santo Domingo", "tr" to "Santo Domingo", "ar" to "سانتو دومينغو", "en" to "Santo Domingo"),
        "America/Port-au-Prince" to mapOf("de" to "Port-au-Prince", "es" to "Puerto Príncipe", "fr" to "Port-au-Prince", "it" to "Port-au-Prince", "tr" to "Port-au-Prince", "ar" to "بورت أو برنس", "en" to "Port-au-Prince"),
        "America/Jamaica" to mapOf("de" to "Kingston", "es" to "Kingston", "fr" to "Kingston", "it" to "Kingston", "tr" to "Kingston", "ar" to "كينغستون", "en" to "Kingston"),
        "America/Nassau" to mapOf("de" to "Nassau", "es" to "Nasáu", "fr" to "Nassau", "it" to "Nassau", "tr" to "Nassau", "ar" to "ناسو", "en" to "Nassau"),
        "America/Belize" to mapOf("de" to "Belize City", "es" to "Ciudad de Belice", "fr" to "Belize City", "it" to "Belize City", "tr" to "Belize City", "ar" to "مدينة بليز", "en" to "Belize City"),
        "America/Barbados" to mapOf("de" to "Bridgetown", "es" to "Bridgetown", "fr" to "Bridgetown", "it" to "Bridgetown", "tr" to "Bridgetown", "ar" to "بريدج تاون", "en" to "Bridgetown"),
        "America/Port_of_Spain" to mapOf("de" to "Port of Spain", "es" to "Puerto España", "fr" to "Port-d'Espagne", "it" to "Port of Spain", "tr" to "Port of Spain", "ar" to "بورت أوف سبين", "en" to "Port of Spain"),
        "America/Curacao" to mapOf("de" to "Willemstad", "es" to "Willemstad", "fr" to "Willemstad", "it" to "Willemstad", "tr" to "Willemstad", "ar" to "ويلمستاد", "en" to "Willemstad"),
        "America/Aruba" to mapOf("de" to "Oranjestad", "es" to "Oranjestad", "fr" to "Oranjestad", "it" to "Oranjestad", "tr" to "Oranjestad", "ar" to "أورنجستاد", "en" to "Oranjestad"),
        "America/Cayenne" to mapOf("de" to "Cayenne", "es" to "Cayena", "fr" to "Cayenne", "it" to "Caienna", "tr" to "Cayenne", "ar" to "كايين", "en" to "Cayenne"),
        "America/Paramaribo" to mapOf("de" to "Paramaribo", "es" to "Paramaribo", "fr" to "Paramaribo", "it" to "Paramaribo", "tr" to "Paramaribo", "ar" to "باراماريبو", "en" to "Paramaribo"),
        "America/Guyana" to mapOf("de" to "Georgetown", "es" to "Georgetown", "fr" to "Georgetown", "it" to "Georgetown", "tr" to "Georgetown", "ar" to "جورج تاون", "en" to "Georgetown"),
        "America/Fortaleza" to mapOf("de" to "Fortaleza", "es" to "Fortaleza", "fr" to "Fortaleza", "it" to "Fortaleza", "tr" to "Fortaleza", "ar" to "فورتاليزا", "en" to "Fortaleza"),
        "America/Recife" to mapOf("de" to "Recife", "es" to "Recife", "fr" to "Recife", "it" to "Recife", "tr" to "Recife", "ar" to "ريسيفي", "en" to "Recife"),
        "America/Bahia" to mapOf("de" to "Salvador", "es" to "Salvador", "fr" to "Salvador", "it" to "Salvador", "tr" to "Salvador", "ar" to "سلفادور", "en" to "Salvador"),
        "America/Belem" to mapOf("de" to "Belém", "es" to "Belém", "fr" to "Belém", "it" to "Belém", "tr" to "Belem", "ar" to "بيليم", "en" to "Belem"),
        "America/Manaus" to mapOf("de" to "Manaus", "es" to "Manaos", "fr" to "Manaus", "it" to "Manaus", "tr" to "Manaus", "ar" to "ماناوس", "en" to "Manaus"),
        "America/Cuiaba" to mapOf("de" to "Cuiabá", "es" to "Cuiabá", "fr" to "Cuiabá", "it" to "Cuiabá", "tr" to "Cuiaba", "ar" to "كويابا", "en" to "Cuiaba"),
        "America/Campo_Grande" to mapOf("de" to "Campo Grande", "es" to "Campo Grande", "fr" to "Campo Grande", "it" to "Campo Grande", "tr" to "Campo Grande", "ar" to "كامبو غراندي", "en" to "Campo Grande"),
        "America/Porto_Velho" to mapOf("de" to "Porto Velho", "es" to "Porto Velho", "fr" to "Porto Velho", "it" to "Porto Velho", "tr" to "Porto Velho", "ar" to "بورتو فيلهو", "en" to "Porto Velho"),
        "America/Boa_Vista" to mapOf("de" to "Boa Vista", "es" to "Boa Vista", "fr" to "Boa Vista", "it" to "Boa Vista", "tr" to "Boa Vista", "ar" to "بوا فيستا", "en" to "Boa Vista"),
        "America/Rio_Branco" to mapOf("de" to "Rio Branco", "es" to "Rio Branco", "fr" to "Rio Branco", "it" to "Rio Branco", "tr" to "Rio Branco", "ar" to "ريو برانكو", "en" to "Rio Branco"),
        "America/Argentina/Cordoba" to mapOf("de" to "Córdoba", "es" to "Córdoba", "fr" to "Córdoba", "it" to "Córdoba", "tr" to "Cordoba", "ar" to "قرطبة", "en" to "Cordoba"),
        "America/Argentina/Mendoza" to mapOf("de" to "Mendoza", "es" to "Mendoza", "fr" to "Mendoza", "it" to "Mendoza", "tr" to "Mendoza", "ar" to "مندوزا", "en" to "Mendoza"),
        "America/Argentina/Salta" to mapOf("de" to "Salta", "es" to "Salta", "fr" to "Salta", "it" to "Salta", "tr" to "Salta", "ar" to "سالتا", "en" to "Salta"),
        "America/Argentina/Tucuman" to mapOf("de" to "Tucumán", "es" to "Tucumán", "fr" to "Tucumán", "it" to "Tucumán", "tr" to "Tucuman", "ar" to "توكومان", "en" to "Tucuman"),
        "America/Argentina/Ushuaia" to mapOf("de" to "Ushuaia", "es" to "Ushuaia", "fr" to "Ushuaïa", "it" to "Ushuaia", "tr" to "Ushuaia", "ar" to "أوشوايا", "en" to "Ushuaia"),
        "America/Punta_Arenas" to mapOf("de" to "Punta Arenas", "es" to "Punta Arenas", "fr" to "Punta Arenas", "it" to "Punta Arenas", "tr" to "Punta Arenas", "ar" to "بونتا أريناس", "en" to "Punta Arenas"),
        "Africa/Johannesburg" to mapOf("de" to "Johannesburg", "es" to "Johannesburgo", "fr" to "Johannesbourg", "it" to "Johannesburg", "tr" to "Johannesburg", "ar" to "جوهانسبرغ", "en" to "Johannesburg"),
        "Africa/Nairobi" to mapOf("de" to "Nairobi", "es" to "Nairobi", "fr" to "Nairobi", "it" to "Nairobi", "tr" to "Nairobi", "ar" to "نيروبي", "en" to "Nairobi"),
        "Africa/Addis_Ababa" to mapOf("de" to "Addis Abeba", "es" to "Adís Abeba", "fr" to "Addis-Abeba", "it" to "Addis Abeba", "tr" to "Addis Ababa", "ar" to "أديس أبابا", "en" to "Addis Ababa"),
        "Africa/Lagos" to mapOf("de" to "Lagos", "es" to "Lagos", "fr" to "Lagos", "it" to "Lagos", "tr" to "Lagos", "ar" to "لاغوس", "en" to "Lagos"),
        "Africa/Dakar" to mapOf("de" to "Dakar", "es" to "Dakar", "fr" to "Dakar", "it" to "Dakar", "tr" to "Dakar", "ar" to "داكار", "en" to "Dakar"),
        "Africa/Accra" to mapOf("de" to "Accra", "es" to "Acra", "fr" to "Accra", "it" to "Accra", "tr" to "Akra", "ar" to "أكرا", "en" to "Accra"),
        "Africa/Abidjan" to mapOf("de" to "Abidjan", "es" to "Abiyán", "fr" to "Abidjan", "it" to "Abidjan", "tr" to "Abidjan", "ar" to "أبيدجان", "en" to "Abidjan"),
        "Africa/Dar_es_Salaam" to mapOf("de" to "Daressalam", "es" to "Dar es Salaam", "fr" to "Dar es Salam", "it" to "Dar es Salaam", "tr" to "Darüsselam", "ar" to "دار السلام", "en" to "Dar es Salaam"),
        "Africa/Kampala" to mapOf("de" to "Kampala", "es" to "Kampala", "fr" to "Kampala", "it" to "Kampala", "tr" to "Kampala", "ar" to "كمبالا", "en" to "Kampala"),
        "Africa/Kigali" to mapOf("de" to "Kigali", "es" to "Kigali", "fr" to "Kigali", "it" to "Kigali", "tr" to "Kigali", "ar" to "كيغالي", "en" to "Kigali"),
        "Africa/Luanda" to mapOf("de" to "Luanda", "es" to "Luanda", "fr" to "Luanda", "it" to "Luanda", "tr" to "Luanda", "ar" to "لواندا", "en" to "Luanda"),
        "Africa/Kinshasa" to mapOf("de" to "Kinshasa", "es" to "Kinshasa", "fr" to "Kinshasa", "it" to "Kinshasa", "tr" to "Kinşasa", "ar" to "كينشاسا", "en" to "Kinshasa"),
        "Africa/Brazzaville" to mapOf("de" to "Brazzaville", "es" to "Brazzaville", "fr" to "Brazzaville", "it" to "Brazzaville", "tr" to "Brazzaville", "ar" to "برازافيل", "en" to "Brazzaville"),
        "Africa/Harare" to mapOf("de" to "Harare", "es" to "Harare", "fr" to "Harare", "it" to "Harare", "tr" to "Harare", "ar" to "هراري", "en" to "Harare"),
        "Africa/Lusaka" to mapOf("de" to "Lusaka", "es" to "Lusaka", "fr" to "Lusaka", "it" to "Lusaka", "tr" to "Lusaka", "ar" to "لوساكا", "en" to "Lusaka"),
        "Africa/Maputo" to mapOf("de" to "Maputo", "es" to "Maputo", "fr" to "Maputo", "it" to "Maputo", "tr" to "Maputo", "ar" to "مابوتو", "en" to "Maputo"),
        "Africa/Windhoek" to mapOf("de" to "Windhoek", "es" to "Windhoek", "fr" to "Windhoek", "it" to "Windhoek", "tr" to "Windhoek", "ar" to "ويندهوك", "en" to "Windhoek"),
        "Africa/Gaborone" to mapOf("de" to "Gaborone", "es" to "Gaborone", "fr" to "Gaborone", "it" to "Gaborone", "tr" to "Gaborone", "ar" to "غابورون", "en" to "Gaborone"),
        "Africa/Bamako" to mapOf("de" to "Bamako", "es" to "Bamako", "fr" to "Bamako", "it" to "Bamako", "tr" to "Bamako", "ar" to "باماكو", "en" to "Bamako"),
        "Africa/Ouagadougou" to mapOf("de" to "Ouagadougou", "es" to "Uagadugú", "fr" to "Ouagadougou", "it" to "Ouagadougou", "tr" to "Ouagadougou", "ar" to "واغادوغو", "en" to "Ouagadougou"),
        "Africa/Niamey" to mapOf("de" to "Niamey", "es" to "Niamey", "fr" to "Niamey", "it" to "Niamey", "tr" to "Niamey", "ar" to "نيامي", "en" to "Niamey"),
        "Africa/Ndjamena" to mapOf("de" to "N'Djamena", "es" to "Yamena", "fr" to "N'Djaména", "it" to "N'Djamena", "tr" to "Encemine", "ar" to "انجمينا", "en" to "N'Djamena"),
        "Africa/Bangui" to mapOf("de" to "Bangui", "es" to "Bangui", "fr" to "Bangui", "it" to "Bangui", "tr" to "Bangui", "ar" to "بانغي", "en" to "Bangui"),
        "Africa/Libreville" to mapOf("de" to "Libreville", "es" to "Libreville", "fr" to "Libreville", "it" to "Libreville", "tr" to "Libreville", "ar" to "ليبرفيل", "en" to "Libreville"),
        "Africa/Douala" to mapOf("de" to "Douala", "es" to "Duala", "fr" to "Douala", "it" to "Douala", "tr" to "Douala", "ar" to "دوالا", "en" to "Douala"),
        "Africa/Malabo" to mapOf("de" to "Malabo", "es" to "Malabo", "fr" to "Malabo", "it" to "Malabo", "tr" to "Malabo", "ar" to "مالابو", "en" to "Malabo"),
        "Africa/Conakry" to mapOf("de" to "Conakry", "es" to "Conakri", "fr" to "Conakry", "it" to "Conakry", "tr" to "Konakri", "ar" to "كوناكري", "en" to "Conakry"),
        "Africa/Freetown" to mapOf("de" to "Freetown", "es" to "Freetown", "fr" to "Freetown", "it" to "Freetown", "tr" to "Freetown", "ar" to "فريتاون", "en" to "Freetown"),
        "Africa/Monrovia" to mapOf("de" to "Monrovia", "es" to "Monrovia", "fr" to "Monrovia", "it" to "Monrovia", "tr" to "Monrovia", "ar" to "منروفيا", "en" to "Monrovia"),
        "Africa/Lome" to mapOf("de" to "Lomé", "es" to "Lomé", "fr" to "Lomé", "it" to "Lomé", "tr" to "Lome", "ar" to "لومي", "en" to "Lome"),
        "Africa/Porto-Novo" to mapOf("de" to "Porto-Novo", "es" to "Porto Novo", "fr" to "Porto-Novo", "it" to "Porto-Novo", "tr" to "Porto-Novo", "ar" to "بورتو نوفو", "en" to "Porto-Novo"),
        "Africa/Bissau" to mapOf("de" to "Bissau", "es" to "Bisáu", "fr" to "Bissau", "it" to "Bissau", "tr" to "Bissau", "ar" to "بيساو", "en" to "Bissau"),
        "Africa/Banjul" to mapOf("de" to "Banjul", "es" to "Banjul", "fr" to "Banjul", "it" to "Banjul", "tr" to "Banjul", "ar" to "بانجول", "en" to "Banjul"),
        "Africa/Asmara" to mapOf("de" to "Asmara", "es" to "Asmara", "fr" to "Asmara", "it" to "Asmara", "tr" to "Asmara", "ar" to "أسمرة", "en" to "Asmara"),
        "Africa/Juba" to mapOf("de" to "Juba", "es" to "Yuba", "fr" to "Djouba", "it" to "Giuba", "tr" to "Cuba", "ar" to "جوبا", "en" to "Juba"),
        "Australia/Sydney" to mapOf("de" to "Sydney", "es" to "Sídney", "fr" to "Sydney", "it" to "Sydney", "tr" to "Sidney", "ar" to "سيدني", "en" to "Sydney"),
        "Australia/Melbourne" to mapOf("de" to "Melbourne", "es" to "Melbourne", "fr" to "Melbourne", "it" to "Melbourne", "tr" to "Melbourne", "ar" to "ملبورن", "en" to "Melbourne"),
        "Australia/Brisbane" to mapOf("de" to "Brisbane", "es" to "Brisbane", "fr" to "Brisbane", "it" to "Brisbane", "tr" to "Brisbane", "ar" to "بريزبان", "en" to "Brisbane"),
        "Australia/Perth" to mapOf("de" to "Perth", "es" to "Perth", "fr" to "Perth", "it" to "Perth", "tr" to "Perth", "ar" to "بيرث", "en" to "Perth"),
        "Australia/Adelaide" to mapOf("de" to "Adelaide", "es" to "Adelaida", "fr" to "Adélaïde", "it" to "Adelaide", "tr" to "Adelaide", "ar" to "أديليد", "en" to "Adelaide"),
        "Australia/Darwin" to mapOf("de" to "Darwin", "es" to "Darwin", "fr" to "Darwin", "it" to "Darwin", "tr" to "Darwin", "ar" to "داروين", "en" to "Darwin"),
        "Australia/Hobart" to mapOf("de" to "Hobart", "es" to "Hobart", "fr" to "Hobart", "it" to "Hobart", "tr" to "Hobart", "ar" to "هوبارت", "en" to "Hobart"),
        "Pacific/Auckland" to mapOf("de" to "Auckland", "es" to "Auckland", "fr" to "Auckland", "it" to "Auckland", "tr" to "Auckland", "ar" to "أوكلاند", "en" to "Auckland"),
        "Pacific/Fiji" to mapOf("de" to "Fidschi", "es" to "Fiyi", "fr" to "Fidji", "it" to "Figi", "tr" to "Fiji", "ar" to "فيجي", "en" to "Fiji"),
        "Pacific/Guam" to mapOf("de" to "Guam", "es" to "Guam", "fr" to "Guam", "it" to "Guam", "tr" to "Guam", "ar" to "غوام", "en" to "Guam"),
        "Pacific/Tahiti" to mapOf("de" to "Tahiti", "es" to "Tahití", "fr" to "Tahiti", "it" to "Tahiti", "tr" to "Tahiti", "ar" to "تاهيتي", "en" to "Tahiti"),
        "Pacific/Noumea" to mapOf("de" to "Nouméa", "es" to "Numea", "fr" to "Nouméa", "it" to "Nouméa", "tr" to "Noumea", "ar" to "نوميا", "en" to "Noumea"),
        "Pacific/Port_Moresby" to mapOf("de" to "Port Moresby", "es" to "Port Moresby", "fr" to "Port Moresby", "it" to "Port Moresby", "tr" to "Port Moresby", "ar" to "بورت مورسبي", "en" to "Port Moresby"),
        "Indian/Maldives" to mapOf("de" to "Malediven", "es" to "Maldivas", "fr" to "Maldives", "it" to "Maldive", "tr" to "Maldivler", "ar" to "المالديف", "en" to "Maldives"),
        "Indian/Mauritius" to mapOf("de" to "Mauritius", "es" to "Mauricio", "fr" to "Maurice", "it" to "Mauritius", "tr" to "Mauritius", "ar" to "موريشيوس", "en" to "Mauritius"),
        "Indian/Antananarivo" to mapOf("de" to "Antananarivo", "es" to "Antananarivo", "fr" to "Antananarivo", "it" to "Antananarivo", "tr" to "Antananarivo", "ar" to "أنتاناناريفو", "en" to "Antananarivo"),
        "Indian/Reunion" to mapOf("de" to "Réunion", "es" to "Reunión", "fr" to "La Réunion", "it" to "Riunione", "tr" to "Reunion", "ar" to "ريونيون", "en" to "Reunion"),
        "Indian/Mayotte" to mapOf("de" to "Mayotte", "es" to "Mayotte", "fr" to "Mayotte", "it" to "Mayotte", "tr" to "Mayotte", "ar" to "مايوت", "en" to "Mayotte"),
        "Indian/Mahe" to mapOf("de" to "Mahé", "es" to "Mahé", "fr" to "Mahé", "it" to "Mahé", "tr" to "Mahe", "ar" to "ماهيه", "en" to "Mahe"),
        "Atlantic/Canary" to mapOf("de" to "Kanarische Inseln", "es" to "Islas Canarias", "fr" to "Îles Canaries", "it" to "Isole Canarie", "tr" to "Kanarya Adaları", "ar" to "جزر الكناري", "en" to "Canary Islands"),
        "Atlantic/Madeira" to mapOf("de" to "Madeira", "es" to "Madeira", "fr" to "Madère", "it" to "Madera", "tr" to "Madeira", "ar" to "ماديرا", "en" to "Madeira"),
        "Atlantic/Azores" to mapOf("de" to "Azoren", "es" to "Azores", "fr" to "Açores", "it" to "Azorre", "tr" to "Azorlar", "ar" to "الأزور", "en" to "Azores"),
        "Atlantic/Bermuda" to mapOf("de" to "Bermuda", "es" to "Bermudas", "fr" to "Bermudes", "it" to "Bermuda", "tr" to "Bermuda", "ar" to "برمودا", "en" to "Bermuda"),
        "Atlantic/Cape_Verde" to mapOf("de" to "Kap Verde", "es" to "Cabo Verde", "fr" to "Cap-Vert", "it" to "Capo Verde", "tr" to "Yeşil Burun", "ar" to "الرأس الأخضر", "en" to "Cape Verde"),
        "Atlantic/Faroe" to mapOf("de" to "Färöer", "es" to "Islas Feroe", "fr" to "Îles Féroé", "it" to "Isole Fær Øer", "tr" to "Faroe Adaları", "ar" to "جزر فارو", "en" to "Faroe Islands"),
        "Arctic/Longyearbyen" to mapOf("de" to "Longyearbyen", "es" to "Longyearbyen", "fr" to "Longyearbyen", "it" to "Longyearbyen", "tr" to "Longyearbyen", "ar" to "لونغياربيين", "en" to "Longyearbyen")
    )

    private val CITY_ALIASES = mapOf(
        "Asia/Riyadh" to listOf("الرياض", "رياض", "Riyadh", "السعودية", "Saudi Arabia", "KSA"),
        "Africa/Cairo" to listOf("القاهرة", "قاهرة", "Cairo", "مصر", "Egypt", "Le Caire", "El Cairo", "Kahire"),
        "Asia/Dubai" to listOf("دبي", "Dubai", "الإمارات", "UAE", "United Arab Emirates"),
        "Asia/Jerusalem" to listOf("القدس", "قدس", "Jerusalem", "فلسطين", "Palestine", "Al-Quds", "Kudüs"),
        "Asia/Kuwait" to listOf("الكويت", "كويت", "Kuwait", "Kuveyt"),
        "Asia/Qatar" to listOf("الدوحة", "دوحة", "Doha", "قطر", "Qatar"),
        "Asia/Bahrain" to listOf("المنامة", "منامة", "Manama", "البحرين", "Bahrain"),
        "Asia/Muscat" to listOf("مسقط", "Muscat", "عمان", "سلطنة عمان", "Oman"),
        "Asia/Amman" to listOf("عمان", "Amman", "الأردن", "Jordan"),
        "Asia/Baghdad" to listOf("بغداد", "Baghdad", "العراق", "Iraq"),
        "Asia/Damascus" to listOf("دمشق", "Damascus", "الشام", "سوريا", "Syria"),
        "Asia/Beirut" to listOf("بيروت", "Beirut", "لبنان", "Lebanon"),
        "Asia/Aden" to listOf("عدن", "Aden", "اليمن", "Yemen"),
        "Africa/Tripoli" to listOf("طرابلس", "Tripoli", "ليبيا", "Libya"),
        "Africa/Tunis" to listOf("تونس", "Tunis", "تونس الخضراء", "Tunisia"),
        "Africa/Algiers" to listOf("الجزائر", "Algiers", "Alger", "Algeria"),
        "Africa/Casablanca" to listOf("الدار البيضاء", "كازابلانكا", "Casablanca", "المغرب", "Morocco"),
        "Africa/Khartoum" to listOf("الخرطوم", "خرطوم", "Khartoum", "السودان", "Sudan"),
        "Africa/Nouakchott" to listOf("نواكشوط", "Nouakchott", "موريتانيا", "Mauritania"),
        "Africa/Mogadishu" to listOf("مقديشو", "Mogadishu", "الصومال", "Somalia"),
        "Africa/Djibouti" to listOf("جيبوتي", "Djibouti"),
        "Europe/Istanbul" to listOf("إسطنبول", "اسطنبول", "Istanbul", "قسطنطينية", "Constantinople", "تركيا", "Turkey", "Türkiye"),
        "Europe/London" to listOf("لندن", "London", "بريطانيا", "المملكة المتحدة", "UK", "United Kingdom", "England"),
        "Europe/Paris" to listOf("باريس", "Paris", "فرنسا", "France"),
        "America/New_York" to listOf("نيويورك", "New York", "NYC", "أمريكا", "الولايات المتحدة", "USA", "United States"),
        "America/Los_Angeles" to listOf("لوس أنجلوس", "Los Angeles", "LA"),
        "America/Chicago" to listOf("شيكاغو", "Chicago"),
        "Asia/Tokyo" to listOf("طوكيو", "Tokyo", "اليابان", "Japan"),
        "Asia/Seoul" to listOf("سيول", "Seoul", "كوريا", "Korea"),
        "Asia/Ulaanbaatar" to listOf("أولان باتور", "اولان باتور", "أولانباتار", "اولانباتار", "الانباتار", "اولان باطور", "Ulaanbaatar", "Ulan Bator", "Ulan-Bator", "منغوليا", "Mongolia", "Ulan Batur"),
        "Europe/Berlin" to listOf("برلين", "Berlin", "ألمانيا", "Germany"),
        "Europe/Rome" to listOf("روما", "Rome", "Roma", "إيطاليا", "Italy"),
        "Europe/Madrid" to listOf("مدريد", "Madrid", "إسبانيا", "Spain"),
        "Europe/Moscow" to listOf("موسكو", "Moscow", "روسيا", "Russia"),
        "Europe/Zurich" to listOf("زيورخ", "Zurich", "Zürich", "سويسرا", "Switzerland")
    )

    fun rawCityName(zoneId: String): String =
        zoneId.substringAfter("/").substringAfterLast("/").replace("_", " ")

    fun getLocalizedCity(zoneId: String, context: Context): String {
        val locale = resolveLocale(context)
        val lang = locale.language

        MULTI_LANG_CITIES[zoneId]?.get(lang)?.let { return it }

        val icuCity = runCatching {
            val canonical = android.icu.util.TimeZone.getCanonicalID(zoneId) ?: zoneId
            val tzNames = android.icu.text.TimeZoneNames.getInstance(locale)
            val exemplar = tzNames.getExemplarLocationName(canonical)
            if (!exemplar.isNullOrBlank()) exemplar else tzNames.getExemplarLocationName(zoneId)
        }.getOrNull()

        if (!icuCity.isNullOrBlank()) {
            if (lang == "ar") {
                if (icuCity.any { it in '\u0600'..'\u06FF' }) return icuCity
                return transliterateLatinToArabic(icuCity)
            }
            return icuCity
        }

        val raw = rawCityName(zoneId)
        if (lang == "ar") {
            return transliterateLatinToArabic(raw)
        }
        return raw
    }

    fun getLocalizedCountry(zoneId: String, context: Context): String {
        val locale = resolveLocale(context)
        val countryCode = runCatching {
            android.icu.util.TimeZone.getRegion(zoneId)
        }.getOrNull()
        if (!countryCode.isNullOrBlank() && countryCode != "001") {
            val displayCountry = Locale("", countryCode).getDisplayCountry(locale)
            if (displayCountry.isNotBlank() && displayCountry != countryCode) {
                return displayCountry
            }
        }
        return getLocalizedRegion(zoneId, context)
    }

    fun getLocalizedRegion(zoneId: String, context: Context): String {
        val rawRegion = zoneId.substringBefore("/").replace("_", " ")
        val locale = resolveLocale(context)
        val map = REGION_NAMES[locale.language]
        if (map != null && map.containsKey(rawRegion)) {
            return map[rawRegion] ?: rawRegion
        }
        return rawRegion
    }

    fun getSearchTerms(zoneId: String, context: Context): List<String> {
        val terms = mutableListOf<String>()
        terms += zoneId
        val rawCity = rawCityName(zoneId)
        terms += rawCity
        val localizedCity = getLocalizedCity(zoneId, context)
        terms += localizedCity
        val localizedCountry = getLocalizedCountry(zoneId, context)
        terms += localizedCountry
        val rawRegion = zoneId.substringBefore("/").replace("_", " ")
        terms += rawRegion
        val localizedRegion = getLocalizedRegion(zoneId, context)
        terms += localizedRegion

        MULTI_LANG_CITIES[zoneId]?.values?.forEach { terms += it }

        CITY_ALIASES[zoneId]?.forEach { terms += it }

        val countryCode = runCatching { android.icu.util.TimeZone.getRegion(zoneId) }.getOrNull()
        if (!countryCode.isNullOrBlank() && countryCode != "001") {
            terms += countryCode
            val targetLanguages = listOf("en", "ar", "de", "es", "fr", "it", "tr")
            targetLanguages.forEach { lang ->
                val countryName = Locale("", countryCode).getDisplayCountry(Locale(lang))
                if (countryName.isNotBlank() && countryName != countryCode) {
                    terms += countryName
                }
            }
        }
        return terms.distinct()
    }

    fun normalizeForSearch(text: String): String {
        if (text.isBlank()) return ""
        var s = text.trim()
        s = TASHKEEL_REGEX.replace(s, "")
        val sb = StringBuilder(s.length)
        for (ch in s) {
            val mapped = when (ch) {
                'أ', 'إ', 'آ', 'ٱ', 'ا' -> 'ا'
                'ة' -> 'ه'
                'ى', 'ئ' -> 'ي'
                'ؤ' -> 'و'
                else -> ch
            }
            sb.append(mapped)
        }
        s = sb.toString().lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = DIACRITICS_REGEX.replace(decomposed, "")
        s = s.replace("ß", "ss")
            .replace("æ", "ae")
            .replace("œ", "oe")
            .replace("ø", "o")
            .replace("đ", "d")
        s = PUNCT_REGEX.replace(s, " ")
        s = SPACES_REGEX.replace(s, " ").trim()
        return s
    }

    fun createWorldCity(zoneId: String, context: Context): WorldCity {
        val city = getLocalizedCity(zoneId, context)
        val country = getLocalizedCountry(zoneId, context)
        val region = getLocalizedRegion(zoneId, context)
        val terms = getSearchTerms(zoneId, context)

        val normCity = normalizeForSearch(city)
        val normCityWithoutAl = if (normCity.startsWith("ال") && normCity.length > 2) normCity.substring(2).trim() else normCity
        val normCityWords = normCity.split(" ").filter { it.isNotBlank() }
        val normTerms = terms.map { normalizeForSearch(it) }.filter { it.isNotBlank() }
        val normCountry = normalizeForSearch(country)
        val normCountryWithoutAl = if (normCountry.startsWith("ال") && normCountry.length > 2) normCountry.substring(2).trim() else normCountry
        val normRegion = normalizeForSearch(region)
        val normZoneId = normalizeForSearch(zoneId)

        return WorldCity(
            zoneId = zoneId,
            city = city,
            region = region,
            country = country,
            searchTerms = terms,
            normCity = normCity,
            normCityWithoutAl = normCityWithoutAl,
            normCityWords = normCityWords,
            normTerms = normTerms,
            normCountry = normCountry,
            normCountryWithoutAl = normCountryWithoutAl,
            normRegion = normRegion,
            normZoneId = normZoneId
        )
    }

    fun isSubsequence(sub: String, target: String): Boolean {
        if (sub.isEmpty()) return true
        if (sub.length > target.length) return false
        var i = 0
        var j = 0
        while (i < sub.length && j < target.length) {
            if (sub[i] == target[j]) {
                i++
            }
            j++
        }
        return i == sub.length
    }

    fun boundedLevenshtein(s1: String, s2: String, maxDist: Int): Int {
        if (s1 == s2) return 0
        val lenDiff = kotlin.math.abs(s1.length - s2.length)
        if (lenDiff > maxDist) return maxDist + 1
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var prev = IntArray(s2.length + 1) { it }
        var curr = IntArray(s2.length + 1)

        for (i in s1.indices) {
            curr[0] = i + 1
            var minInRow = curr[0]
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = kotlin.math.min(
                    kotlin.math.min(curr[j] + 1, prev[j + 1] + 1),
                    prev[j] + cost
                )
                if (curr[j + 1] < minInRow) minInRow = curr[j + 1]
            }
            if (minInRow > maxDist) return maxDist + 1
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[s2.length]
    }

    fun calculateSearchScore(city: WorldCity, q: String, qWithoutAl: String): Int {
        if (q.isEmpty()) return 0

        if (city.normCity == q || city.normCityWithoutAl == q || city.normCity == qWithoutAl || city.normCityWithoutAl == qWithoutAl) {
            return 10000
        }

        if (city.normCity.startsWith(q) || city.normCityWithoutAl.startsWith(q) || city.normCity.startsWith(qWithoutAl) || city.normCityWithoutAl.startsWith(qWithoutAl)) {
            val lenDiff = kotlin.math.abs(city.normCity.length - q.length)
            return 8000 - lenDiff * 10
        }

        for (word in city.normCityWords) {
            val wordWithoutAl = if (word.startsWith("ال") && word.length > 2) word.substring(2) else word
            if (word == q || wordWithoutAl == q || word == qWithoutAl || wordWithoutAl == qWithoutAl) {
                return 7000
            }
            if (word.startsWith(q) || wordWithoutAl.startsWith(q) || word.startsWith(qWithoutAl) || wordWithoutAl.startsWith(qWithoutAl)) {
                return 6500
            }
        }

        if (city.normCity.contains(q) || city.normCity.contains(qWithoutAl)) {
            return 6000
        }

        for (term in city.normTerms) {
            val termWithoutAl = if (term.startsWith("ال") && term.length > 2) term.substring(2) else term
            if (term == q || termWithoutAl == q || term == qWithoutAl || termWithoutAl == qWithoutAl) {
                return 5000
            }
            if (term.startsWith(q) || termWithoutAl.startsWith(q) || term.startsWith(qWithoutAl) || termWithoutAl.startsWith(qWithoutAl)) {
                return 4500
            }
            if (term.contains(q) || term.contains(qWithoutAl)) {
                return 4000
            }
        }

        if (city.normCountry == q || city.normCountryWithoutAl == q || city.normCountry == qWithoutAl || city.normCountryWithoutAl == qWithoutAl) {
            return 3500
        }
        if (city.normCountry.startsWith(q) || city.normCountryWithoutAl.startsWith(q) || city.normCountry.startsWith(qWithoutAl) || city.normCountryWithoutAl.startsWith(qWithoutAl)) {
            return 3000
        }
        if (city.normCountry.contains(q) || city.normCountry.contains(qWithoutAl)) {
            return 2500
        }

        if (q.length >= 2) {
            if (isSubsequence(q, city.normCity) || isSubsequence(qWithoutAl, city.normCityWithoutAl)) {
                val lenDiff = kotlin.math.abs(city.normCity.length - q.length)
                return 2000 - lenDiff * 10
            }
            for (term in city.normTerms) {
                if (isSubsequence(q, term) || isSubsequence(qWithoutAl, term)) {
                    return 1800
                }
            }
        }

        if (q.length >= 3) {
            val d1 = boundedLevenshtein(q, city.normCity, 2)
            val d2 = boundedLevenshtein(qWithoutAl, city.normCityWithoutAl, 2)
            val minDist = kotlin.math.min(d1, d2)
            if (minDist == 1) return 1500
            if (minDist == 2 && q.length >= 4) return 1000

            for (term in city.normTerms) {
                if (boundedLevenshtein(q, term, 1) <= 1 || boundedLevenshtein(qWithoutAl, term, 1) <= 1) {
                    return 900
                }
            }
        }

        if (city.normRegion.contains(q)) {
            return 500
        }

        if (city.normZoneId.contains(q)) {
            return 400
        }

        return 0
    }

    fun sectionHeaderFor(cityName: String): Char {
        val trimmed = cityName.trim()
        val first = trimmed.firstOrNull() ?: return '#'
        return when (first) {
            'أ', 'إ', 'آ', 'ٱ', 'ا' -> 'أ'
            'ة', 'ه' -> 'ه'
            'ى', 'ي', 'ئ' -> 'ي'
            'ؤ', 'و' -> 'و'
            in 'a'..'z', in 'A'..'Z' -> first.uppercaseChar()
            'Á', 'À', 'Â', 'Ä', 'Ã', 'Å', 'á', 'à', 'â', 'ä', 'ã', 'å' -> 'A'
            'É', 'È', 'Ê', 'Ë', 'é', 'è', 'ê', 'ë' -> 'E'
            'Í', 'Ì', 'Î', 'Ï', 'í', 'ì', 'î', 'ï' -> 'I'
            'Ó', 'Ò', 'Ô', 'Ö', 'Õ', 'ó', 'ò', 'ô', 'ö', 'õ' -> 'O'
            'Ú', 'Ù', 'Û', 'Ü', 'ú', 'ù', 'û', 'ü' -> 'U'
            'Ç', 'ç' -> 'C'
            'Ñ', 'ñ' -> 'N'
            'Ş', 'ş' -> 'S'
            'Ğ', 'ğ' -> 'G'
            else -> first.uppercaseChar()
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        if (kotlin.math.abs(s1.length - s2.length) > 3) return 99

        var prev = IntArray(s2.length + 1) { it }
        var curr = IntArray(s2.length + 1)

        for (i in s1.indices) {
            curr[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = kotlin.math.min(
                    kotlin.math.min(curr[j] + 1, prev[j + 1] + 1),
                    prev[j] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[s2.length]
    }

    fun formatWorldClockDifference(diffSec: Long, context: Context): String {
        val diffHours = diffSec / 3600
        val diffMinutes = (kotlin.math.abs(diffSec) % 3600) / 60
        val offsetSign = if (diffSec >= 0) "+" else "-"
        val absHours = kotlin.math.abs(diffHours)

        return when {
            diffHours == 0L && diffMinutes == 0L -> context.getString(R.string.world_clock_diff_zero)
            diffMinutes > 0L -> String.format(Locale.US, context.getString(R.string.world_clock_diff_hm), offsetSign, absHours, diffMinutes)
            else -> String.format(Locale.US, context.getString(R.string.world_clock_diff_hours), offsetSign, absHours)
        }
    }

    fun formatWorldClockSubtitle(nowMillis: Long, zone: ZoneId, localZone: ZoneId, context: Context): String {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val localDate = Instant.ofEpochMilli(nowMillis).atZone(localZone).toLocalDate()
        val targetDate = now.toLocalDate()
        val dayText = when {
            targetDate.isEqual(localDate) -> context.getString(R.string.today)
            targetDate.isAfter(localDate) -> context.getString(R.string.tomorrow)
            else -> context.getString(R.string.yesterday)
        }
        val targetOffsetSec = now.offset.totalSeconds.toLong()
        val localOffsetSec = localZone.rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds.toLong()
        val diffSec = targetOffsetSec - localOffsetSec
        val diffText = formatWorldClockDifference(diffSec, context)
        return String.format(context.getString(R.string.world_clock_subtitle), dayText, diffText)
    }

    fun resolveLocale(context: Context): Locale =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }.getOrNull() ?: Locale.getDefault()

    private fun transliterateLatinToArabic(input: String): String {
        val sb = StringBuilder()
        var i = 0
        val text = input.trim()
        while (i < text.length) {
            val remaining = text.substring(i)
            val upper = remaining.uppercase(Locale.ROOT)
            when {
                upper.startsWith("SH") -> { sb.append("ش"); i += 2 }
                upper.startsWith("CH") -> { sb.append("تش"); i += 2 }
                upper.startsWith("TH") -> { sb.append("ث"); i += 2 }
                upper.startsWith("KH") -> { sb.append("خ"); i += 2 }
                upper.startsWith("PH") -> { sb.append("ف"); i += 2 }
                upper.startsWith("GH") -> { sb.append("غ"); i += 2 }
                upper.startsWith("OU") -> { sb.append("و"); i += 2 }
                upper.startsWith("OO") -> { sb.append("و"); i += 2 }
                upper.startsWith("EE") -> { sb.append("ي"); i += 2 }
                upper.startsWith("EA") -> { sb.append("ي"); i += 2 }
                upper.startsWith("AI") -> { sb.append("اي"); i += 2 }
                upper.startsWith("AY") -> { sb.append("اي"); i += 2 }
                upper.startsWith("AU") -> { sb.append(if (sb.isEmpty()) "أو" else "او"); i += 2 }
                upper.startsWith("AW") -> { sb.append(if (sb.isEmpty()) "أو" else "او"); i += 2 }
                upper.startsWith("AL_") || upper.startsWith("AL-") -> { sb.append("ال"); i += 3 }
                upper.startsWith("SAN_") || upper.startsWith("SAN ") -> { sb.append("سان "); i += 4 }
                upper.startsWith("SANTA_") || upper.startsWith("SANTA ") -> { sb.append("سانتا "); i += 6 }
                upper.startsWith("SAINT_") || upper.startsWith("SAINT ") -> { sb.append("سانت "); i += 6 }
                upper.startsWith("ST_") || upper.startsWith("ST ") -> { sb.append("سانت "); i += 3 }
                upper.startsWith("NEW_") || upper.startsWith("NEW ") -> { sb.append("نيو "); i += 4 }
                upper.startsWith("PORT_") || upper.startsWith("PORT ") -> { sb.append("بورت "); i += 5 }
                upper.startsWith("FORT_") || upper.startsWith("FORT ") -> { sb.append("فورت "); i += 5 }
                else -> {
                    val c = text[i].uppercaseChar()
                    val nextChar = if (i + 1 < text.length) text[i + 1].uppercaseChar() else ' '
                    val ar = when (c) {
                        'A' -> if (sb.isEmpty()) "أ" else "ا"
                        'B' -> "ب"
                        'C' -> if (nextChar == 'E' || nextChar == 'I' || nextChar == 'Y') "س" else "ك"
                        'D' -> "د"
                        'E' -> if (sb.isEmpty()) "إ" else "ي"
                        'F' -> "ف"
                        'G' -> if (nextChar == 'E' || nextChar == 'I' || nextChar == 'Y') "ج" else "غ"
                        'H' -> "ه"
                        'I' -> if (sb.isEmpty()) "إ" else "ي"
                        'J' -> "ج"
                        'K' -> "ك"
                        'L' -> "ل"
                        'M' -> "م"
                        'N' -> "ن"
                        'O' -> if (sb.isEmpty()) "أو" else "و"
                        'P' -> "ب"
                        'Q' -> "ق"
                        'R' -> "ر"
                        'S' -> "س"
                        'T' -> "ت"
                        'U' -> if (sb.isEmpty()) "أو" else "و"
                        'V' -> "ف"
                        'W' -> "و"
                        'X' -> "كس"
                        'Y' -> "ي"
                        'Z' -> "ز"
                        ' ' -> " "
                        '-' -> " "
                        '_' -> " "
                        else -> c.toString()
                    }
                    sb.append(ar)
                    i++
                }
            }
        }
        return sb.toString()
    }
}
