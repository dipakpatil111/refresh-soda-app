package com.refreshsoda.app.ui.i18n

enum class AppLanguage(val code: String, val displayName: String, val subtitle: String) {
    HINDI("hi", "हिंदी", "Hindi"),
    ENGLISH("en", "English", "English"),
    MARATHI("mr", "मराठी", "Marathi")
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
            AppLanguage.ENGLISH to "Box Stock",
            AppLanguage.MARATHI to "गोदाम साठा"
        ),
        "tab_khata" to mapOf(
            AppLanguage.HINDI to "उधार खाता",
            AppLanguage.ENGLISH to "Udhaar Khata",
            AppLanguage.MARATHI to "उधारी खाते"
        ),
        "tab_hisaab" to mapOf(
            AppLanguage.HINDI to "हिसाब & खर्चे",
            AppLanguage.ENGLISH to "Accounts & Expenses",
            AppLanguage.MARATHI to "हिशोब आणि खर्च"
        ),
        "tab_settings" to mapOf(
            AppLanguage.HINDI to "सेटिंग्स",
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.MARATHI to "सेटिंग्ज"
        ),

        // Financial & Dashboard
        "today_total_sales" to mapOf(
            AppLanguage.HINDI to "आज का कुल धंधा",
            AppLanguage.ENGLISH to "Today's Total Sales",
            AppLanguage.MARATHI to "आजची एकूण विक्री"
        ),
        "net_profit" to mapOf(
            AppLanguage.HINDI to "आज का शुद्ध मुनाफ़ा",
            AppLanguage.ENGLISH to "Today's Net Profit",
            AppLanguage.MARATHI to "आजचा निव्वळ नफा"
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
            AppLanguage.HINDI to "बिक्री",
            AppLanguage.ENGLISH to "Sale",
            AppLanguage.MARATHI to "विक्री"
        ),
        "quick_new_stock" to mapOf(
            AppLanguage.HINDI to "+ माल",
            AppLanguage.ENGLISH to "+ Stock",
            AppLanguage.MARATHI to "+ साठा"
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
            AppLanguage.HINDI to "कैटेगरी-वाइज़ बिक्री",
            AppLanguage.ENGLISH to "Category-wise Sales",
            AppLanguage.MARATHI to "कॅटेगरीनुसार विक्री"
        ),
        "top_items" to mapOf(
            AppLanguage.HINDI to "आज के टॉप बिकने वाले आइटम",
            AppLanguage.ENGLISH to "Today's Top Selling Items",
            AppLanguage.MARATHI to "आजच्या सर्वाधिक विक्री वस्तू"
        ),
        "godown_box_status" to mapOf(
            AppLanguage.HINDI to "गोदाम सीलबंद पेटी स्टॉक",
            AppLanguage.ENGLISH to "Godown Sealed Box Stock",
            AppLanguage.MARATHI to "गोदाम सीलबंद पेटी साठा"
        ),
        "recent_sales" to mapOf(
            AppLanguage.HINDI to "हाल की बिक्री व रसीदें",
            AppLanguage.ENGLISH to "Recent Sales & Receipts",
            AppLanguage.MARATHI to "नुकतीच झालेली विक्री आणि पावत्या"
        ),
        "share_receipt" to mapOf(
            AppLanguage.HINDI to "व्हाट्सएप पर्ची",
            AppLanguage.ENGLISH to "Share Receipt",
            AppLanguage.MARATHI to "पावती पाठवा"
        ),

        // POS
        "live_counter_order" to mapOf(
            AppLanguage.HINDI to "लाइव काउंटर ऑर्डर (Cart)",
            AppLanguage.ENGLISH to "Live Counter Order (Cart)",
            AppLanguage.MARATHI to "लाईव्ह काउंटर ऑर्डर (Cart)"
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
            AppLanguage.MARATHI to "अतिरिक्त:"
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
        "smart_import_btn" to mapOf(
            AppLanguage.HINDI to "📄 इमेज / लिस्ट से इम्पोर्ट",
            AppLanguage.ENGLISH to "📄 Import from Image / Text",
            AppLanguage.MARATHI to "📄 इमेज / यादीवरून इम्पोर्ट"
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

        // Expenses
        "daily_expenses" to mapOf(
            AppLanguage.HINDI to "दुकान खर्चे (Daily & Monthly)",
            AppLanguage.ENGLISH to "Shop Expenses (Daily & Monthly)",
            AppLanguage.MARATHI to "दुकानाचे खर्च (दैनिक व मासिक)"
        ),
        "add_expense" to mapOf(
            AppLanguage.HINDI to "+ नया खर्चा जोड़ें",
            AppLanguage.ENGLISH to "+ Add Expense",
            AppLanguage.MARATHI to "+ नवीन खर्च जोडा"
        ),
        "exp_ice" to mapOf(
            AppLanguage.HINDI to "❄️ बर्फ (Ice)",
            AppLanguage.ENGLISH to "❄️ Ice",
            AppLanguage.MARATHI to "❄️ बर्फ"
        ),
        "exp_lemon" to mapOf(
            AppLanguage.HINDI to "🍋 नींबू (Lemons)",
            AppLanguage.ENGLISH to "🍋 Lemons",
            AppLanguage.MARATHI to "🍋 लिंबू"
        ),
        "exp_soda_syrup" to mapOf(
            AppLanguage.HINDI to "🥤 सोडे का माल व गैस (Syrup, Masala, CO2 Gas)",
            AppLanguage.ENGLISH to "🥤 Soda Syrup & CO2 Gas",
            AppLanguage.MARATHI to "🥤 सोड्याचे साहित्य व गॅस"
        ),
        "exp_milk" to mapOf(
            AppLanguage.HINDI to "🥛 दूध व शेक सामग्री (Milk & Shake)",
            AppLanguage.ENGLISH to "🥛 Milk & Shake Items",
            AppLanguage.MARATHI to "🥛 दूध आणि शेक साहित्य"
        ),
        "exp_glasses" to mapOf(
            AppLanguage.HINDI to "🧻 ग्लास, स्ट्रॉ व डिस्पोजेबल (Glasses & Straws)",
            AppLanguage.ENGLISH to "🧻 Disposable Glasses & Straws",
            AppLanguage.MARATHI to "🧻 डिस्पोजेबल ग्लास आणि स्ट्रॉ"
        ),
        "exp_electricity" to mapOf(
            AppLanguage.HINDI to "💡 बिजली बिल (Electricity Bill)",
            AppLanguage.ENGLISH to "💡 Electricity Bill",
            AppLanguage.MARATHI to "💡 वीज बिल"
        ),
        "exp_rent" to mapOf(
            AppLanguage.HINDI to "🏪 दुकान का किराया (Shop Rent)",
            AppLanguage.ENGLISH to "🏪 Shop Rent",
            AppLanguage.MARATHI to "🏪 दुकानाचे भाडे"
        ),
        "exp_salary" to mapOf(
            AppLanguage.HINDI to "👷 कारीगर / मज़दूरी (Staff Salary / Labor)",
            AppLanguage.ENGLISH to "👷 Staff Salary / Labor",
            AppLanguage.MARATHI to "👷 पगार / मजुरी"
        ),
        "exp_repair" to mapOf(
            AppLanguage.HINDI to "🔧 मशीन मेंटेनेंस व सर्विस (Machine Repair)",
            AppLanguage.ENGLISH to "🔧 Machine Repair & Service",
            AppLanguage.MARATHI to "🔧 मशीन दुरुस्ती व देखभाल"
        ),
        "exp_other" to mapOf(
            AppLanguage.HINDI to "📦 अन्य विविध खर्च (Miscellaneous)",
            AppLanguage.ENGLISH to "📦 Miscellaneous",
            AppLanguage.MARATHI to "📦 इतर खर्च"
        ),

        // Khata
        "total_market_outstanding" to mapOf(
            AppLanguage.HINDI to "मार्केट में कुल बाकी (Total Udhaar)",
            AppLanguage.ENGLISH to "Total Market Outstanding",
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
            AppLanguage.ENGLISH to "Receive Payment",
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
            AppLanguage.ENGLISH to "Day-End Cash Closing",
            AppLanguage.MARATHI to "दुकान बंद करताना गल्ला क्लोजिंग"
        ),
        "opening_cash" to mapOf(
            AppLanguage.HINDI to "सुबह का कैश (Opening Cash):",
            AppLanguage.ENGLISH to "Morning Opening Cash:",
            AppLanguage.MARATHI to "सकाळची रोख रक्कम:"
        ),
        "expected_cash" to mapOf(
            AppLanguage.HINDI to "सिस्टम के हिसाब से कैश (Expected):",
            AppLanguage.ENGLISH to "System Expected Cash:",
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

        // Paytm Settlement
        "paytm_settlement_title" to mapOf(
            AppLanguage.HINDI to "Paytm / UPI मर्चेंट सेटलमेंट",
            AppLanguage.ENGLISH to "Paytm / UPI Settlement",
            AppLanguage.MARATHI to "पेटीएम / यूपीआय सेटलमेंट"
        ),
        "net_bank_amount" to mapOf(
            AppLanguage.HINDI to "कल बैंक में आने वाली शुद्ध रकम:",
            AppLanguage.ENGLISH to "Net Expected in Bank Tomorrow:",
            AppLanguage.MARATHI to "उद्या बँकेत येणारी निव्वळ रक्कम:"
        ),
        "settled_in_bank" to mapOf(
            AppLanguage.HINDI to "हाँ, बैंक में आ गया (Settled in Bank)",
            AppLanguage.ENGLISH to "Yes, Received in Bank",
            AppLanguage.MARATHI to "होय, बँकेत जमा झाले"
        ),

        // Settings Screen
        "settings_title" to mapOf(
            AppLanguage.HINDI to "ऐप सेटिंग्स व प्रोफाइल",
            AppLanguage.ENGLISH to "App Settings & Profile",
            AppLanguage.MARATHI to "ॲप सेटिंग्ज आणि प्रोफाइल"
        ),
        "user_profile_title" to mapOf(
            AppLanguage.HINDI to "दुकान व मालिक प्रोफाइल",
            AppLanguage.ENGLISH to "Shop & Owner Profile",
            AppLanguage.MARATHI to "दुकान आणि मालक प्रोफाइल"
        ),
        "shop_name" to mapOf(
            AppLanguage.HINDI to "दुकान का नाम",
            AppLanguage.ENGLISH to "Shop Name",
            AppLanguage.MARATHI to "दुकानाचे नाव"
        ),
        "owner_name" to mapOf(
            AppLanguage.HINDI to "मालिक का नाम",
            AppLanguage.ENGLISH to "Owner Name",
            AppLanguage.MARATHI to "मालकाचे नाव"
        ),
        "mobile_number" to mapOf(
            AppLanguage.HINDI to "मोबाइल नंबर",
            AppLanguage.ENGLISH to "Mobile Number",
            AppLanguage.MARATHI to "मोबाईल नंबर"
        ),
        "upi_id" to mapOf(
            AppLanguage.HINDI to "दुकान UPI ID (पेमेंट के लिए)",
            AppLanguage.ENGLISH to "Shop UPI ID (For Payments)",
            AppLanguage.MARATHI to "दुकान UPI ID (पेमेंटसाठी)"
        ),
        "shop_address" to mapOf(
            AppLanguage.HINDI to "दुकान का पता / शहर",
            AppLanguage.ENGLISH to "Shop Address / City",
            AppLanguage.MARATHI to "दुकानाचा पत्ता / शहर"
        ),
        "edit_profile_btn" to mapOf(
            AppLanguage.HINDI to "प्रोफाइल बदलें",
            AppLanguage.ENGLISH to "Edit Profile",
            AppLanguage.MARATHI to "प्रोफाइल बदला"
        ),
        "save_profile_btn" to mapOf(
            AppLanguage.HINDI to "प्रोफाइल सेव करें",
            AppLanguage.ENGLISH to "Save Profile",
            AppLanguage.MARATHI to "प्रोफाइल सेव्ह करा"
        ),
        "theme_selector_title" to mapOf(
            AppLanguage.HINDI to "ऐप थीम मोड (Theme Mode)",
            AppLanguage.ENGLISH to "App Theme Mode",
            AppLanguage.MARATHI to "ॲप थीम मोड"
        ),
        "theme_system" to mapOf(
            AppLanguage.HINDI to "सिस्टम डिफ़ॉल्ट",
            AppLanguage.ENGLISH to "System Default",
            AppLanguage.MARATHI to "सिस्टम डिफॉल्ट"
        ),
        "theme_light" to mapOf(
            AppLanguage.HINDI to "लाइट मोड (Light)",
            AppLanguage.ENGLISH to "Light Mode",
            AppLanguage.MARATHI to "लाईट मोड"
        ),
        "theme_dark" to mapOf(
            AppLanguage.HINDI to "डार्क मोड (Dark)",
            AppLanguage.ENGLISH to "Dark Mode",
            AppLanguage.MARATHI to "डार्क मोड"
        ),
        "language_selector_title" to mapOf(
            AppLanguage.HINDI to "भाषा चुनें (Select Language)",
            AppLanguage.ENGLISH to "Select App Language",
            AppLanguage.MARATHI to "भाषा निवडा (Select Language)"
        ),
        "app_info_title" to mapOf(
            AppLanguage.HINDI to "ऐप जानकारी व डेटा",
            AppLanguage.ENGLISH to "App Info & Data Sync",
            AppLanguage.MARATHI to "ॲप माहिती आणि डेटा"
        ),
        "offline_ready" to mapOf(
            AppLanguage.HINDI to "✅ ऑफलाइन पूर्ण सुरक्षित (Room Database)",
            AppLanguage.ENGLISH to "✅ Fully Offline Ready (Room DB)",
            AppLanguage.MARATHI to "✅ पूर्ण ऑफलाइन सुरक्षित (Room DB)"
        ),
        "cloud_sync_status" to mapOf(
            AppLanguage.HINDI to "☁️ Firebase क्लाउड बैकअप सिंक सक्रिय",
            AppLanguage.ENGLISH to "☁️ Firebase Cloud Sync Active",
            AppLanguage.MARATHI to "☁️ Firebase क्लाउड बॅकअप सक्रिय"
        ),

        // Authentication & Multi-User Management
        "auth_welcome" to mapOf(
            AppLanguage.HINDI to "स्वागत है! कृपया लॉग इन या रजिस्टर करें",
            AppLanguage.ENGLISH to "Welcome! Please Sign In or Register",
            AppLanguage.MARATHI to "स्वागत आहे! कृपया लॉग इन किंवा नोंदणी करा"
        ),
        "login_tab" to mapOf(
            AppLanguage.HINDI to "लॉग इन (Sign In)",
            AppLanguage.ENGLISH to "Sign In",
            AppLanguage.MARATHI to "लॉग इन (Sign In)"
        ),
        "register_tab" to mapOf(
            AppLanguage.HINDI to "नया खाता (Register)",
            AppLanguage.ENGLISH to "Register",
            AppLanguage.MARATHI to "नवीन खाते (Register)"
        ),
        "email_or_phone_label" to mapOf(
            AppLanguage.HINDI to "ईमेल या मोबाइल नंबर (Email/Phone)",
            AppLanguage.ENGLISH to "Email or Phone Number",
            AppLanguage.MARATHI to "ईमेल किंवा मोबाईल नंबर"
        ),
        "password_label" to mapOf(
            AppLanguage.HINDI to "पासवर्ड (Password)",
            AppLanguage.ENGLISH to "Password",
            AppLanguage.MARATHI to "पासवर्ड"
        ),
        "confirm_password_label" to mapOf(
            AppLanguage.HINDI to "पासवर्ड दोबारा डालें (Confirm Password)",
            AppLanguage.ENGLISH to "Confirm Password",
            AppLanguage.MARATHI to "पासवर्ड पुन्हा प्रविष्ट करा"
        ),
        "full_name_label" to mapOf(
            AppLanguage.HINDI to "आपका पूरा नाम (Full Name)",
            AppLanguage.ENGLISH to "Full Name",
            AppLanguage.MARATHI to "पूर्ण नाव"
        ),
        "role_label" to mapOf(
            AppLanguage.HINDI to "खाता प्रकार / भूमिका (Role)",
            AppLanguage.ENGLISH to "User Role",
            AppLanguage.MARATHI to "खाते प्रकार / भूमिका"
        ),
        "role_owner" to mapOf(
            AppLanguage.HINDI to "👑 मालिक (Shop Owner - All Access)",
            AppLanguage.ENGLISH to "👑 Shop Owner (All Access)",
            AppLanguage.MARATHI to "👑 मालक (सर्व परवानग्या)"
        ),
        "role_staff" to mapOf(
            AppLanguage.HINDI to "👤 स्टाफ / कैशियर (Staff / Cashier)",
            AppLanguage.ENGLISH to "👤 Staff / Cashier (Counter Sales)",
            AppLanguage.MARATHI to "👤 कर्मचारी / कॅशियर (विक्री व स्टॉक)"
        ),
        "shop_name_label" to mapOf(
            AppLanguage.HINDI to "दुकान का नाम (Shop Name)",
            AppLanguage.ENGLISH to "Shop Name",
            AppLanguage.MARATHI to "दुकानाचे नाव"
        ),
        "login_btn" to mapOf(
            AppLanguage.HINDI to "लॉग इन करें (Login)",
            AppLanguage.ENGLISH to "Login to Shop",
            AppLanguage.MARATHI to "लॉग इन करा"
        ),
        "register_btn" to mapOf(
            AppLanguage.HINDI to "खाता बनाएं (Create Account)",
            AppLanguage.ENGLISH to "Create Account",
            AppLanguage.MARATHI to "खाते तयार करा"
        ),
        "quick_login_title" to mapOf(
            AppLanguage.HINDI to "त्वरित डेमो लॉगिन (Quick 1-Tap)",
            AppLanguage.ENGLISH to "Quick 1-Tap Login",
            AppLanguage.MARATHI to "त्वरित 1-टॅप लॉगिन"
        ),
        "logged_in_as" to mapOf(
            AppLanguage.HINDI to "सक्रिय यूजर:",
            AppLanguage.ENGLISH to "Logged In As:",
            AppLanguage.MARATHI to "सक्रिय वापरकर्ता:"
        ),
        "logout_btn" to mapOf(
            AppLanguage.HINDI to "लॉगआउट (Logout)",
            AppLanguage.ENGLISH to "Logout",
            AppLanguage.MARATHI to "लॉगआउट (Logout)"
        ),
        "switch_user_btn" to mapOf(
            AppLanguage.HINDI to "यूजर बदलें (Switch User)",
            AppLanguage.ENGLISH to "Switch User",
            AppLanguage.MARATHI to "वापरकर्ता बदला"
        ),
        "manage_users_title" to mapOf(
            AppLanguage.HINDI to "यूजर व स्टाफ प्रबंधन (Multi-User Accounts)",
            AppLanguage.ENGLISH to "Multi-User & Staff Accounts",
            AppLanguage.MARATHI to "वापरकर्ते आणि कर्मचारी व्यवस्थापन"
        ),
        "add_staff_member" to mapOf(
            AppLanguage.HINDI to "+ नया स्टाफ जोड़ें",
            AppLanguage.ENGLISH to "+ Add New Staff",
            AppLanguage.MARATHI to "+ नवीन कर्मचारी जोडा"
        ),
        "err_fill_all" to mapOf(
            AppLanguage.HINDI to "कृपया सभी जानकारी भरें!",
            AppLanguage.ENGLISH to "Please fill in all fields!",
            AppLanguage.MARATHI to "कृपया सर्व माहिती भरा!"
        ),
        "err_user_not_found" to mapOf(
            AppLanguage.HINDI to "यह यूजर नहीं मिला, कृपया रजिस्टर करें!",
            AppLanguage.ENGLISH to "User not found, please register!",
            AppLanguage.MARATHI to "हा वापरकर्ता सापडला नाही, कृपया नोंदणी करा!"
        ),
        "err_wrong_password" to mapOf(
            AppLanguage.HINDI to "पासवर्ड गलत है!",
            AppLanguage.ENGLISH to "Incorrect password!",
            AppLanguage.MARATHI to "पासवर्ड चुकीचा आहे!"
        ),
        "err_user_exists" to mapOf(
            AppLanguage.HINDI to "यह मोबाइल या ईमेल पहले से दर्ज है!",
            AppLanguage.ENGLISH to "This email or phone is already registered!",
            AppLanguage.MARATHI to "हा मोबाईल किंवा ईमेल आधीच नोंदणीकृत आहे!"
        ),
        "err_password_mismatch" to mapOf(
            AppLanguage.HINDI to "दोनों पासवर्ड मेल नहीं खा रहे हैं!",
            AppLanguage.ENGLISH to "Passwords do not match!",
            AppLanguage.MARATHI to "दोन्ही पासवर्ड जुळत नाहीत!"
        ),
        "staff_restricted_msg" to mapOf(
            AppLanguage.HINDI to "यह सुविधा केवल दुकान मालिक (Owner) के लिए है।",
            AppLanguage.ENGLISH to "This section is restricted to Shop Owner only.",
            AppLanguage.MARATHI to "ही सुविधा फक्त दुकानाच्या मालकासाठी आहे."
        ),
        "delete_user_confirm" to mapOf(
            AppLanguage.HINDI to "क्या आप इस स्टाफ खाते को हटाना चाहते हैं?",
            AppLanguage.ENGLISH to "Are you sure you want to delete this staff account?",
            AppLanguage.MARATHI to "तुम्हाला हे कर्मचारी खाते हटवायचे आहे का?"
        ),

        // Inward Stock & Distributor Bill Tracking
        "inward_stock_title" to mapOf(
            AppLanguage.HINDI to "📦 नया माल आवक व सप्लायर बिल",
            AppLanguage.ENGLISH to "📦 Inward Stock & Supplier Invoice",
            AppLanguage.MARATHI to "📦 नवीन माल आवक आणि पुरवठादार बिल"
        ),
        "inward_stock_btn" to mapOf(
            AppLanguage.HINDI to "माल आवक",
            AppLanguage.ENGLISH to "Inward",
            AppLanguage.MARATHI to "माल आवक"
        ),
        "select_brand_label" to mapOf(
            AppLanguage.HINDI to "सप्लायर / ब्रांड",
            AppLanguage.ENGLISH to "Supplier / Brand",
            AppLanguage.MARATHI to "पुरवठादार / ब्रँड"
        ),
        "select_category_label" to mapOf(
            AppLanguage.HINDI to "कैटेगरी (Category)",
            AppLanguage.ENGLISH to "Category",
            AppLanguage.MARATHI to "श्रेणी (Category)"
        ),
        "bill_number_label" to mapOf(
            AppLanguage.HINDI to "बिल / इनवॉइस नंबर (Invoice #)",
            AppLanguage.ENGLISH to "Bill / Invoice Number",
            AppLanguage.MARATHI to "बिल / इनव्हॉइस क्रमांक"
        ),
        "bill_date_label" to mapOf(
            AppLanguage.HINDI to "माल आने की तारीख (Date)",
            AppLanguage.ENGLISH to "Inward Date",
            AppLanguage.MARATHI to "माल आल्याची तारीख"
        ),
        "payment_mode_label" to mapOf(
            AppLanguage.HINDI to "भुगतान का तरीका",
            AppLanguage.ENGLISH to "Payment mode",
            AppLanguage.MARATHI to "पेमेंट पद्धत"
        ),
        "total_bill_amount" to mapOf(
            AppLanguage.HINDI to "कुल बिल रकम (Total Amount)",
            AppLanguage.ENGLISH to "Total Bill Amount",
            AppLanguage.MARATHI to "एकूण बिल रक्कम"
        ),
        "paid_amount_label" to mapOf(
            AppLanguage.HINDI to "चुकाई गई रकम (Paid Amount)",
            AppLanguage.ENGLISH to "Paid Amount",
            AppLanguage.MARATHI to "भरलेली रक्कम"
        ),
        "payment_status_label" to mapOf(
            AppLanguage.HINDI to "पेमेंट स्थिति (Payment Status)",
            AppLanguage.ENGLISH to "Payment Status",
            AppLanguage.MARATHI to "पेमेंट स्थिती"
        ),
        "payment_status_paid" to mapOf(
            AppLanguage.HINDI to "✅ पूरा भुगतान कर दिया (Full Paid)",
            AppLanguage.ENGLISH to "✅ Full Paid",
            AppLanguage.MARATHI to "✅ पूर्ण भरले (Full Paid)"
        ),
        "payment_status_partial" to mapOf(
            AppLanguage.HINDI to "⚠️ आधा दिया, आधा बाकी (Partial)",
            AppLanguage.ENGLISH to "⚠️ Partially Paid",
            AppLanguage.MARATHI to "⚠️ अंशतः भरले (Partial)"
        ),
        "payment_status_unpaid" to mapOf(
            AppLanguage.HINDI to "❌ पूरा बिल उधार है (Unpaid)",
            AppLanguage.ENGLISH to "❌ Unpaid / Credit",
            AppLanguage.MARATHI to "❌ सर्व बाकी (Unpaid)"
        ),
        "upload_bill_btn" to mapOf(
            AppLanguage.HINDI to "📷 बिल फोटो / PDF अपलोड करें",
            AppLanguage.ENGLISH to "📷 Upload Bill Image or PDF",
            AppLanguage.MARATHI to "📷 बिलाचा फोटो किंवा PDF अपलोड करा"
        ),
        "icecream_profit_title" to mapOf(
            AppLanguage.HINDI to "🍨 आइसक्रीम बिक्री व मुनाफ़ा (Profit Analytics)",
            AppLanguage.ENGLISH to "🍨 Ice-Cream Sales & Profit Analytics",
            AppLanguage.MARATHI to "🍨 आईस्क्रीम विक्री आणि नफा विश्लेषण"
        ),
        "cost_price_label" to mapOf(
            AppLanguage.HINDI to "खरीद भाव (Cost Price/Box)",
            AppLanguage.ENGLISH to "Cost Price / Box",
            AppLanguage.MARATHI to "खरेदी किंमत (Cost Price/Box)"
        ),
        "selling_price_label" to mapOf(
            AppLanguage.HINDI to "बिक्री भाव (Selling/MRP)",
            AppLanguage.ENGLISH to "Selling Price / MRP",
            AppLanguage.MARATHI to "विक्री किंमत (Selling/MRP)"
        ),
        "profit_per_box" to mapOf(
            AppLanguage.HINDI to "प्रति बॉक्स मुनाफ़ा",
            AppLanguage.ENGLISH to "Profit per Box",
            AppLanguage.MARATHI to "प्रति बॉक्स नफा"
        ),
        "profit_margin_label" to mapOf(
            AppLanguage.HINDI to "मुनाफ़ा मार्जिन (Margin %)",
            AppLanguage.ENGLISH to "Profit Margin %",
            AppLanguage.MARATHI to "नफा मार्जिन %"
        ),
        "supplier_bills_title" to mapOf(
            AppLanguage.HINDI to "सप्लायर बिल व उधारी (Distributor Ledger)",
            AppLanguage.ENGLISH to "Distributor Bills & Outstanding",
            AppLanguage.MARATHI to "पुरवठादार बिल आणि उधारी"
        ),
        "mark_as_paid_btn" to mapOf(
            AppLanguage.HINDI to "पूर्ण भुगतान करें (Paid)",
            AppLanguage.ENGLISH to "Mark as Paid",
            AppLanguage.MARATHI to "पूर्ण भरणा नोंदवा"
        ),
        "inward_success_msg" to mapOf(
            AppLanguage.HINDI to "माल गोदाम में जुड़ गया और सप्लायर बिल सेव हुआ!",
            AppLanguage.ENGLISH to "Inward stock added and invoice recorded successfully!",
            AppLanguage.MARATHI to "माल गोदामात जोडला गेला आणि पुरवठादार बिल जतन केले!"
        )
    )

    fun get(key: String, language: AppLanguage): String {
        val entry = translations[key]
        return entry?.get(language) ?: entry?.get(AppLanguage.HINDI) ?: key
    }
}
