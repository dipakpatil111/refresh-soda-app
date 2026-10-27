package com.refreshsoda.app.ui.i18n

enum class AppLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिंदी"),
    ENGLISH("en", "English"),
    MARATHI("mr", "मराठी")
}

object AppStrings {
    private val translations = mapOf(
        // Navigation Tabs
        "tab_dashboard" to mapOf(
            AppLanguage.HINDI to "डैशबोर्ड",
            AppLanguage.ENGLISH to "Dashboard",
            AppLanguage.MARATHI to "डॅशबोर्ड"
        ),
        "tab_pos" to mapOf(
            AppLanguage.HINDI to "बिक्री POS",
            AppLanguage.ENGLISH to "POS Sale",
            AppLanguage.MARATHI to "विक्री POS"
        ),
        "tab_stock" to mapOf(
            AppLanguage.HINDI to "गोदाम स्टॉक",
            AppLanguage.ENGLISH to "Godown Stock",
            AppLanguage.MARATHI to "गोदाम साठा"
        ),
        "tab_khata" to mapOf(
            AppLanguage.HINDI to "उधार खाता",
            AppLanguage.ENGLISH to "Khata (Credit)",
            AppLanguage.MARATHI to "उधारी खाते"
        ),
        "tab_hisaab" to mapOf(
            AppLanguage.HINDI to "हिसाब & खर्चा",
            AppLanguage.ENGLISH to "Accounts & Closing",
            AppLanguage.MARATHI to "हिशोब आणि खर्च"
        ),

        // Dashboard
        "today_total_sales" to mapOf(
            AppLanguage.HINDI to "आज का कुल धंधा",
            AppLanguage.ENGLISH to "Today's Total Sales",
            AppLanguage.MARATHI to "आजची एकूण विक्री"
        ),
        "cash_galla" to mapOf(
            AppLanguage.HINDI to "Cash (गल्ला)",
            AppLanguage.ENGLISH to "Cash (Till)",
            AppLanguage.MARATHI to "रोख (गल्ला)"
        ),
        "upi_paytm" to mapOf(
            AppLanguage.HINDI to "Paytm / UPI",
            AppLanguage.ENGLISH to "Paytm / UPI",
            AppLanguage.MARATHI to "पेटीएम / यूपीआय"
        ),
        "udhar_khata" to mapOf(
            AppLanguage.HINDI to "उधार (खाता)",
            AppLanguage.ENGLISH to "Credit (Udhaar)",
            AppLanguage.MARATHI to "उधारी (खाते)"
        ),
        "quick_new_sale" to mapOf(
            AppLanguage.HINDI to "+ नई बिक्री",
            AppLanguage.ENGLISH to "+ New Sale",
            AppLanguage.MARATHI to "+ नवीन विक्री"
        ),
        "quick_new_stock" to mapOf(
            AppLanguage.HINDI to "+ नया माल",
            AppLanguage.ENGLISH to "+ New Stock",
            AppLanguage.MARATHI to "+ नवीन साठा"
        ),
        "quick_open_box" to mapOf(
            AppLanguage.HINDI to "- बॉक्स खोलें",
            AppLanguage.ENGLISH to "- Open Box",
            AppLanguage.MARATHI to "- बॉक्स उघडा"
        ),
        "low_stock_warning" to mapOf(
            AppLanguage.HINDI to "कम स्टॉक चेतावनी (2 बॉक्स या कम)",
            AppLanguage.ENGLISH to "Low Stock Alert (2 Boxes or Less)",
            AppLanguage.MARATHI to "कमी साठा इशारा (२ बॉक्स किंवा कमी)"
        ),
        "category_sales" to mapOf(
            AppLanguage.HINDI to "कैटेगरी-वाइज़ कमाई",
            AppLanguage.ENGLISH to "Category-wise Sales",
            AppLanguage.MARATHI to "कॅटेगरीनुसार कमाई"
        ),
        "top_items" to mapOf(
            AppLanguage.HINDI to "आज के टॉप आइटम",
            AppLanguage.ENGLISH to "Today's Top Items",
            AppLanguage.MARATHI to "आजच्या सर्वाधिक विक्री वस्तू"
        ),
        "godown_box_status" to mapOf(
            AppLanguage.HINDI to "गोदाम बॉक्स स्टेटस",
            AppLanguage.ENGLISH to "Godown Box Status",
            AppLanguage.MARATHI to "गोदाम बॉक्स स्थिती"
        ),
        "recent_sales" to mapOf(
            AppLanguage.HINDI to "हाल ही की बिक्री",
            AppLanguage.ENGLISH to "Recent Sales",
            AppLanguage.MARATHI to "नुकतीच झालेली विक्री"
        ),

        // POS
        "live_counter_order" to mapOf(
            AppLanguage.HINDI to "LIVE COUNTER ORDER",
            AppLanguage.ENGLISH to "LIVE COUNTER ORDER",
            AppLanguage.MARATHI to "लाईव्ह काउंटर ऑर्डर"
        ),
        "clear_cart" to mapOf(
            AppLanguage.HINDI to "खाली करें (Clear)",
            AppLanguage.ENGLISH to "Clear Cart",
            AppLanguage.MARATHI to "रिकामे करा (Clear)"
        ),
        "cart_empty_hint" to mapOf(
            AppLanguage.HINDI to "नीचे से आइटम पर टैप करें, बिल अपने आप बनेगा...",
            AppLanguage.ENGLISH to "Tap items below to add to bill...",
            AppLanguage.MARATHI to "खालील वस्तूंवर टॅप करा, बिल आपोआप तयार होईल..."
        ),
        "add_ons" to mapOf(
            AppLanguage.HINDI to "ऐड-ऑन:",
            AppLanguage.ENGLISH to "Add-ons:",
            AppLanguage.MARATHI to "अतिरिक्त (Add-ons):"
        ),
        "add_category_btn" to mapOf(
            AppLanguage.HINDI to "+ नई कैटेगरी",
            AppLanguage.ENGLISH to "+ New Category",
            AppLanguage.MARATHI to "+ नवीन कॅटेगरी"
        ),
        "add_item_btn" to mapOf(
            AppLanguage.HINDI to "+ नया आइटम",
            AppLanguage.ENGLISH to "+ New Item",
            AppLanguage.MARATHI to "+ नवीन वस्तू"
        ),
        "no_categories_hint" to mapOf(
            AppLanguage.HINDI to "कोई कैटेगरी नहीं है। पहले '+ नई कैटेगरी' बनाएं (उदा. Soda, Ice-cream, Milkshake)",
            AppLanguage.ENGLISH to "No categories yet. First add a category (e.g. Soda, Ice-cream, Milkshake)",
            AppLanguage.MARATHI to "अजून कोणतीही कॅटेगरी नाही. आधी '+ नवीन कॅटेगरी' जोडा (उदा. Soda, Ice-cream, Milkshake)"
        ),
        "no_items_in_category" to mapOf(
            AppLanguage.HINDI to "इस कैटेगरी में कोई आइटम नहीं है। '+ नया आइटम' दबाएं।",
            AppLanguage.ENGLISH to "No items in this category. Tap '+ New Item'.",
            AppLanguage.MARATHI to "या कॅटेगरीत कोणतीही वस्तू नाही. '+ नवीन वस्तू' वर टॅप करा."
        ),
        "btn_cash" to mapOf(
            AppLanguage.HINDI to "💵 Cash",
            AppLanguage.ENGLISH to "💵 Cash",
            AppLanguage.MARATHI to "💵 रोख"
        ),
        "btn_upi" to mapOf(
            AppLanguage.HINDI to "📱 UPI",
            AppLanguage.ENGLISH to "📱 UPI",
            AppLanguage.MARATHI to "📱 यूपीआय"
        ),
        "btn_udhar" to mapOf(
            AppLanguage.HINDI to "📒 उधार",
            AppLanguage.ENGLISH to "📒 Credit",
            AppLanguage.MARATHI to "📒 उधारी"
        ),

        // Khata
        "total_market_outstanding" to mapOf(
            AppLanguage.HINDI to "MARKET ME KUL BAAKI",
            AppLanguage.ENGLISH to "TOTAL MARKET OUTSTANDING",
            AppLanguage.MARATHI to "मार्केटमध्ये एकूण बाकी"
        ),
        "search_customer" to mapOf(
            AppLanguage.HINDI to "ग्राहक का नाम या फोन खोजें...",
            AppLanguage.ENGLISH to "Search customer name or phone...",
            AppLanguage.MARATHI to "ग्राहकाचे नाव किंवा फोन शोधा..."
        ),
        "add_customer" to mapOf(
            AppLanguage.HINDI to "+ नया ग्राहक",
            AppLanguage.ENGLISH to "+ New Customer",
            AppLanguage.MARATHI to "+ नवीन ग्राहक"
        ),
        "record_repayment" to mapOf(
            AppLanguage.HINDI to "जमा करें",
            AppLanguage.ENGLISH to "Receive",
            AppLanguage.MARATHI to "जमा करा"
        ),
        "whatsapp_reminder" to mapOf(
            AppLanguage.HINDI to "तकादा",
            AppLanguage.ENGLISH to "Reminder",
            AppLanguage.MARATHI to "आठवण"
        ),

        // Closing & Hisaab
        "day_closing_title" to mapOf(
            AppLanguage.HINDI to "दुकान बंद करते वक्त गल्ला क्लोज़िंग",
            AppLanguage.ENGLISH to "Shop Day-End Cash Closing",
            AppLanguage.MARATHI to "दुकान बंद करताना गल्ला क्लोजिंग"
        ),
        "opening_cash" to mapOf(
            AppLanguage.HINDI to "सुबह का कैश (Opening Cash):",
            AppLanguage.ENGLISH to "Opening Cash (Morning):",
            AppLanguage.MARATHI to "सकाळची रोख रक्कम:"
        ),
        "expected_cash" to mapOf(
            AppLanguage.HINDI to "सिस्टम के हिसाब से कैश (Expected):",
            AppLanguage.ENGLISH to "Expected System Cash:",
            AppLanguage.MARATHI to "सिस्टीमनुसार अपेक्षित कॅश:"
        ),
        "actual_cash_input" to mapOf(
            AppLanguage.HINDI to "रात को गिनकर असली कैश डालें",
            AppLanguage.ENGLISH to "Counted Actual Cash at Night",
            AppLanguage.MARATHI to "रात्री मोजून प्रत्यक्ष रोख रक्कम टाका"
        ),
        "save_closing" to mapOf(
            AppLanguage.HINDI to "क्लोज़िंग सेव करें",
            AppLanguage.ENGLISH to "Save Closing",
            AppLanguage.MARATHI to "क्लोजिंग सेव्ह करा"
        ),
        "whatsapp_summary" to mapOf(
            AppLanguage.HINDI to "📲 WhatsApp समरी भेजें",
            AppLanguage.ENGLISH to "📲 Send WhatsApp Summary",
            AppLanguage.MARATHI to "📲 व्हॉट्सॲप अहवाल पाठवा"
        ),

        // Expenses
        "daily_expenses" to mapOf(
            AppLanguage.HINDI to "आज के खर्चे (Daily Expenses)",
            AppLanguage.ENGLISH to "Today's Daily Expenses",
            AppLanguage.MARATHI to "आजचे दैनंदिन खर्च"
        ),
        "add_expense" to mapOf(
            AppLanguage.HINDI to "+ नया खर्चा",
            AppLanguage.ENGLISH to "+ New Expense",
            AppLanguage.MARATHI to "+ नवीन खर्च"
        ),

        // Paytm Settlement
        "paytm_settlement_title" to mapOf(
            AppLanguage.HINDI to "Paytm / UPI मर्चेंट सेटलमेंट ट्रैकर",
            AppLanguage.ENGLISH to "Paytm / UPI Merchant Settlement",
            AppLanguage.MARATHI to "पेटीएम / यूपीआय मर्चंट सेटलमेंट"
        ),
        "net_bank_amount" to mapOf(
            AppLanguage.HINDI to "कल बैंक में आने वाली शुद्ध रकम:",
            AppLanguage.ENGLISH to "Net Expected in Bank Tomorrow:",
            AppLanguage.MARATHI to "उद्या बँकेत येणारी निव्वळ रक्कम:"
        ),
        "settled_in_bank" to mapOf(
            AppLanguage.HINDI to "हाँ, बैंक में आ गया (Settled in Bank)",
            AppLanguage.ENGLISH to "Yes, Settled in Bank",
            AppLanguage.MARATHI to "होय, बँकेत जमा झाले"
        )
    )

    fun get(key: String, language: AppLanguage): String {
        return translations[key]?.get(language) ?: translations[key]?.get(AppLanguage.HINDI) ?: key
    }
}

