/**
 * i18n.js - Global Translation System
 * Safe Trip Planner
 * 
 * Usage:
 * 1. Include <script src="/js/i18n.js"></script> after navbar.js in each HTML page
 * 2. Add data-i18n="key" attributes to translatable text elements
 * 3. The system auto-loads and applies translations on DOMContentLoaded
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'appLanguage';
    const DEFAULT_LANG = 'en';

    const translations = {
        en: {
            // Navbar
            home: "Home",
            plan_trip: "Plan Trip",
            my_trip: "My Trip",
            about: "About",
            login: "Login",
            logout: "Logout",
            logo_sub: "Plan smart · travel safe",

            // Home page - Hero
            hero_tag: "Trusted digital travel co-pilot",
            home_title: "Explore the world Safely",
            hero_subtitle: "Plan smart, travel safe, and discover unforgettable destinations with a home page designed for structured, stress‑free trip planning.",
            start_planning: "Start Planning",
            hero_meta_1: "Curated itineraries in minutes",
            hero_meta_2: "Safety first on every route",

            // Home page - About
            why_stp: "Why Safe Trip Planner?",
            about_kicker: "About the platform",
            about_title: "Structured journeys. Safer adventures.",
            about_subtitle: "Safe Trip Planner helps tourists discover destinations, plan trips efficiently, explore nearby attractions, and travel with confidence using smart, safety‑aware planning tools.",
            about_text: "From weekend getaways to once‑in‑a‑lifetime journeys, we organise every step – routes, highlights, buffers, and safety checkpoints – so you can focus on enjoying the world rather than worrying about logistics.",
            discover_confidently: "Discover confidently",
            discover_confidently_desc: "Hand‑picked locations with practical tips, transport insights, and safety guidance baked in.",
            plan_with_clarity: "Plan with clarity",
            plan_with_clarity_desc: "Timeline‑friendly itineraries that balance must‑see highlights with time to breathe and explore.",
            avg_planning_time: "Average planning time",
            trips_organized: "Trips organised safely",
            traveler_satisfaction: "Traveler satisfaction",
            live_trip_overview: "Live trip overview",

            // Home page - Features
            features_kicker: "Plan your next journey",
            features_title: "Everything you need for smarter, safer trips",
            features_subtitle: "Move from inspiration to itinerary with a simple home page that showcases how Safe Trip Planner supports every stage of your travel.",
            discover_destinations: "Discover Destinations",
            discover_destinations_desc: "Browse inspiring cities, coastlines, and hidden gems matched to your style of travel, time frame, and comfort level.",
            smart_trip_planning: "Smart Trip Planning",
            smart_trip_planning_desc: "Turn ideas into a clear, day‑by‑day itinerary with balanced routes, realistic timing, and built‑in buffers for the unexpected.",
            safe_secure_travel: "Safe & Secure Travel",
            safe_secure_travel_desc: "Travel with confidence using guidance that highlights safer options, smarter timings, and local considerations along your route.",

            // Home page - CTA
            cta_title: "Ready to design your next safe adventure?",
            cta_subtitle: "Start with a destination in mind or simply tell Safe Trip Planner how you like to travel. Your itinerary begins here.",
            no_downloads: "No downloads · plan from any device",

            // Plan Trip page
            back_to_home: "Back to Home",
            brand_sub: "Plan trip · day-by-day",
            plan_form_title: "Plan a new trip",
            plan_form_subtitle: "Choose your city, travel mood, and trip length. Safe Trip Planner will generate a simple, structured itinerary.",
            city_label: "City / Region / Place",
            city_hint: "Works with Cities, Districts, regions, tourist areas.",
            city_placeholder: "e.g. Kashmir, Leh, Paris, Bali",
            mood_label: "Travel Mood",
            mood_select: "Select mood",
            tourist: "🗺 Tourist",
            adventure: "🏔 Adventure",
            historical: "🏛 Historical",
            romantic: "💕 Romantic",
            nature: "🌿 Nature",
            relaxation: "☀ Relaxation",
            spiritual: "🕌 Spiritual",
            family_friendly: "👨‍👩‍👧 Family Friendly",
            days_label: "Number of days",
            generate_plan: "Generate Plan",
            searching_places: "Searching places nearby…",
            error_fill_fields: "Please fill all required fields with valid values.",
            error_generate_plan: "Could not generate a trip plan.",
            error_no_data: "No itinerary data returned.",
            plan_for: "Your plan for",
            trip_plan: "Your trip plan",
            generated_by: "generated by Safe Trip Planner",
            no_places_assigned: "No places assigned to this day.",
            view_on_map: "View on OpenStreetMap",
            add_to_trip: "Add to My Trip",
            remove_from_trip: "Remove from My Trip",
            added_to_trip: "added to My Trip!",
            plan_generated_success: "Trip plan generated successfully.",

            // My Trip page
            bucket_title: "🧳 My Trip Bucket List",
            bucket_subtitle: "Places you've saved from your trip plans. Click Remove to take a place off your list.",
            no_places_saved: "No places saved yet",
            no_places_desc: "Go to Plan Trip, generate your itinerary, and tap the + button on any place to add it here.",
            plan_a_trip: "Plan a Trip →",
            remove: "✕ Remove",

            // About page
            about_hero_tag: "About Safe Trip Planner",
            about_hero_title: "Your trusted travel co-pilot",
            about_hero_subtitle: "Helping you discover, plan, and travel with confidence — one safe adventure at a time.",
            platform_kicker: "About the platform",
            platform_title: "Structured journeys. Safer adventures.",
            platform_body: "Safe Trip Planner helps tourists discover destinations, plan trips efficiently, explore nearby attractions, and travel with confidence using smart, safety-aware planning tools. From weekend getaways to once-in-a-lifetime journeys, we organise every step – routes, highlights, buffers, and safety checkpoints – so you can focus on enjoying the world rather than worrying about logistics.",
            travel_safely: "Travel safely",
            travel_safely_desc: "Guidance that highlights safer options, smarter timings, and local considerations along your route.",

            // Contact/Footer
            contact_title: "Stay in touch with Safe Trip Planner",
            contact_text: "Have a question, partnership idea, or feedback about your journeys? Reach out and help us shape the future of safer travel planning.",
            email_label: "Email",
            support_hours: "Support hours: 9:00 – 18:00 (local time)",
            copyright: "© 2026 Safe Trip Planner. All rights reserved.",
            footer_meta: "Designed for modern, safety‑first explorers.",

            // Language switcher
            language: "Language"
        },
        hi: {
            // Navbar
            home: "होम",
            plan_trip: "यात्रा योजना",
            my_trip: "मेरी यात्रा",
            about: "हमारे बारे में",
            login: "लॉगिन",
            logout: "लॉगआउट",
            logo_sub: "स्मार्ट योजना · सुरक्षित यात्रा",

            // Home page - Hero
            hero_tag: "विश्वसनीय डिजिटल ट्रैवल सहयोगी",
            home_title: "दुनिया को सुरक्षित रूप से खोजें",
            hero_subtitle: "स्मार्ट योजना, सुरक्षित यात्रा, और अविस्मरणीय गंतव्यों की खोज करें — संरचित, तनाव-मुक्त यात्रा योजना के लिए डिज़ाइन किया गया।",
            start_planning: "योजना शुरू करें",
            hero_meta_1: "मिनटों में क्यूरेटेड यात्रा कार्यक्रम",
            hero_meta_2: "हर मार्ग पर सुरक्षा पहले",

            // Home page - About
            why_stp: "सेफ ट्रिप प्लानर क्यों?",
            about_kicker: "प्लेटफार्म के बारे में",
            about_title: "संरचित यात्राएं। सुरक्षित रोमांच।",
            about_subtitle: "सेफ ट्रिप प्लानर पर्यटकों को गंतव्यों की खोज करने, यात्राओं की कुशलता से योजना बनाने, आस-पास के आकर्षणों का पता लगाने और स्मार्ट, सुरक्षा-जागरूक योजना उपकरणों का उपयोग करके आत्मविश्वास के साथ यात्रा करने में मदद करता है।",
            about_text: "सप्ताहांत की छुट्टियों से लेकर जीवनकाल की यात्राओं तक, हम हर कदम को व्यवस्थित करते हैं — मार्ग, आकर्षण, बफर और सुरक्षा चौकियां — ताकि आप रसद की चिंता करने के बजाय दुनिया का आनंद लेने पर ध्यान केंद्रित कर सकें।",
            discover_confidently: "आत्मविश्वास से खोजें",
            discover_confidently_desc: "व्यावहारिक सुझावों, परिवहन अंतर्दृष्टि और सुरक्षा मार्गदर्शन के साथ हाथ से चुने गए स्थान।",
            plan_with_clarity: "स्पष्टता के साथ योजना बनाएं",
            plan_with_clarity_desc: "समयरेखा-अनुकूल यात्रा कार्यक्रम जो देखने योग्य आकर्षणों के साथ सांस लेने और खोजने के लिए समय को संतुलित करते हैं।",
            avg_planning_time: "औसत योजना समय",
            trips_organized: "सुरक्षित रूप से आयोजित यात्राएं",
            traveler_satisfaction: "यात्री संतुष्टि",
            live_trip_overview: "लाइव यात्रा अवलोकन",

            // Home page - Features
            features_kicker: "अपनी अगली यात्रा की योजना बनाएं",
            features_title: "स्मार्टर, सुरक्षित यात्राओं के लिए सब कुछ",
            features_subtitle: "प्रेरणा से लेकर यात्रा कार्यक्रम तक — सेफ ट्रिप प्लानर आपकी यात्रा के हर चरण में कैसे सहायता करता है, यह दर्शाने वाला एक सरल होम पेज।",
            discover_destinations: "गंतव्यों की खोज करें",
            discover_destinations_desc: "अपनी यात्रा शैली, समय सीमा और आराम स्तर के अनुरूप प्रेरित करने वाले शहरों, तटरेखाओं और छिपे हुए रत्नों को ब्राउज़ करें।",
            smart_trip_planning: "स्मार्ट यात्रा योजना",
            smart_trip_planning_desc: "विचारों को एक स्पष्ट, दिन-दर-दिन यात्रा कार्यक्रम में बदलें जिसमें संतुलित मार्ग, यथार्थवादी समय और अप्रत्याशित के लिए बिल्ट-इन बफर हों।",
            safe_secure_travel: "सुरक्षित यात्रा",
            safe_secure_travel_desc: "आत्मविश्वास के साथ यात्रा करें जो सुरक्षित विकल्पों, स्मार्ट समय और अपने मार्ग पर स्थानीय विचारों को उजागर करता है।",

            // Home page - CTA
            cta_title: "अपना अगला सुरक्षित रोमांच डिजाइन करने के लिए तैयार हैं?",
            cta_subtitle: "एक गंतव्य के साथ शुरू करें या बस सेफ ट्रिप प्लानर को बताएं कि आप कैसे यात्रा करना पसंद करते हैं। आपकी यात्रा कार्यक्रम यहीं से शुरू होता है।",
            no_downloads: "कोई डाउनलोड नहीं · किसी भी डिवाइस से योजना बनाएं",

            // Plan Trip page
            back_to_home: "होम पर वापस",
            brand_sub: "यात्रा योजना · दिन-दर-दिन",
            plan_form_title: "नई यात्रा की योजना बनाएं",
            plan_form_subtitle: "अपना शहर, यात्रा मूड और यात्रा की अवधि चुनें। सेफ ट्रिप प्लानर एक सरल, संरचित यात्रा कार्यक्रम तैयार करेगा।",
            city_label: "शहर / क्षेत्र / स्थान",
            city_hint: "शहरों, जिलों, क्षेत्रों, पर्यटन क्षेत्रों के साथ काम करता है।",
            city_placeholder: "जैसे कश्मीर, लेह, पेरिस, बाली",
            mood_label: "यात्रा मूड",
            mood_select: "मूड चुनें",
            tourist: "🗺 पर्यटक",
            adventure: "🏔 साहसिक",
            historical: "🏐 ऐतिहासिक",
            romantic: "💕 रोमांटिक",
            nature: "🌿 प्रकृति",
            relaxation: "☀ आराम",
            spiritual: "🕌 आध्यात्मिक",
            family_friendly: "👨‍👩‍👧 पारिवारिक",
            days_label: "दिनों की संख्या",
            generate_plan: "योजना तैयार करें",
            searching_places: "पास के स्थान खोज रहे हैं…",
            error_fill_fields: "कृपया सभी आवश्यक फ़ील्ड मान्य मानों के साथ भरें।",
            error_generate_plan: "यात्रा योजना तैयार नहीं कर सका।",
            error_no_data: "कोई यात्रा कार्यक्रम डेटा प्राप्त नहीं हुआ।",
            plan_for: "के लिए आपकी योजना",
            trip_plan: "आपकी यात्रा योजना",
            generated_by: "सेफ ट्रिप प्लानर द्वारा तैयार",
            no_places_assigned: "इस दिन के लिए कोई स्थान निर्धारित नहीं किया गया।",
            view_on_map: "ओपनस्ट्रीटमैप पर देखें",
            add_to_trip: "यात्रा में जोड़ें",
            remove_from_trip: "यात्रा से हटाएं",
            added_to_trip: "मेरी यात्रा में जोड़ा गया!",
            plan_generated_success: "यात्रा योजना सफलतापूर्वक तैयार की गई।",

            // My Trip page
            bucket_title: "🧳 मेरी यात्रा बकेट लिस्ट",
            bucket_subtitle: "आपकी यात्रा योजनाओं से सहेजे गए स्थान। किसी स्थान को अपनी सूची से हटाने के लिए हटाएं पर क्लिक करें।",
            no_places_saved: "अभी तक कोई स्थान सहेजा नहीं गया",
            no_places_desc: "यात्रा योजना पर जाएं, अपना यात्रा कार्यक्रम तैयार करें, और किसी भी स्थान को यहां जोड़ने के लिए + बटन दबाएं।",
            plan_a_trip: "यात्रा योजना बनाएं →",
            remove: "✕ हटाएं",

            // About page
            about_hero_tag: "सेफ ट्रिप प्लानर के बारे में",
            about_hero_title: "आपका विश्वसनीय ट्रैवल सहयोगी",
            about_hero_subtitle: "आपको खोजने, योजना बनाने और आत्मविश्वास के साथ यात्रा करने में मदद करना — एक सुरक्षित रोमांच एक बार में।",
            platform_kicker: "प्लेटफार्म के बारे में",
            platform_title: "संरचित यात्राएं। सुरक्षित रोमांच।",
            platform_body: "सेफ ट्रिप प्लानर पर्यटकों को गंतव्यों की खोज करने, यात्राओं की कुशलता से योजना बनाने, आस-पास के आकर्षणों का पता लगाने और स्मार्ट, सुरक्षा-जागरूक योजना उपकरणों का उपयोग करके आत्मविश्वास के साथ यात्रा करने में मदद करता है। सप्ताहांत की छुट्टियों से लेकर जीवनकाल की यात्राओं तक, हम हर कदम को व्यवस्थित करते हैं — मार्ग, आकर्षण, बफर और सुरक्षा चौकियां — ताकि आप रसद की चिंता करने के बजाय दुनिया का आनंद लेने पर ध्यान केंद्रित कर सकें।",
            travel_safely: "सुरक्षित रूप से यात्रा करें",
            travel_safely_desc: "मार्गदर्शन जो आपके मार्ग पर सुरक्षित विकल्पों, स्मार्ट समय और स्थानीय विचारों को उजागर करता है।",

            // Contact/Footer
            contact_title: "सेफ ट्रिप प्लानर के संपर्क में रहें",
            contact_text: "कोई प्रश्न, साझेदारी का विचार, या आपकी यात्राओं के बारे में प्रतिक्रिया है? पहुंचें और सुरक्षित यात्रा योजना के भविष्य को आकार देने में हमारी मदद करें।",
            email_label: "ईमेल",
            support_hours: "समर्थन समय: 9:00 – 18:00 (स्थानीय समय)",
            copyright: "© 2026 सेफ ट्रिप प्लानर। सर्वाधिकार सुरक्षित।",
            footer_meta: "आधुनिक, सुरक्षा-प्रथम खोजकर्ताओं के लिए डिज़ाइन किया गया।",

            // Language switcher
            language: "भाषा"
        },
        zh: {
            // Navbar
            home: "首页",
            plan_trip: "规划行程",
            my_trip: "我的行程",
            about: "关于",
            login: "登录",
            logout: "退出",
            logo_sub: "智能规划 · 安全出行",

            // Home page - Hero
            hero_tag: "值得信赖的数字旅行助手",
            home_title: "安全探索世界",
            hero_subtitle: "智能规划、安全出行、探索难忘的目的地 — 专为有条理、无压力的旅行规划而设计的主页。",
            start_planning: "开始规划",
            hero_meta_1: "几分钟内获得精选行程",
            hero_meta_2: "每条路线的安全第一",

            // Home page - About
            why_stp: "为什么选择安全行程规划师？",
            about_kicker: "关于平台",
            about_title: "有组织的旅程。更安全的冒险。",
            about_subtitle: "安全行程规划师帮助游客发现目的地、高效规划行程、探索附近景点，并使用智能、注重安全的规划工具自信旅行。",
            about_text: "从周末短途到一生一次的旅程，我们组织每一步 — 路线、亮点、缓冲时间和安全检查点 — 让您专注于享受世界，而不用担心后勤问题。",
            discover_confidently: "自信地发现",
            discover_confidently_desc: "精心挑选的地点，包含实用提示、交通见解和安全指导。",
            plan_with_clarity: "清晰地规划",
            plan_with_clarity_desc: "时间安排友好的行程，平衡必看景点与休息和探索的时间。",
            avg_planning_time: "平均规划时间",
            trips_organized: "安全组织的旅行",
            traveler_satisfaction: "旅行者满意度",
            live_trip_overview: "实时行程概览",

            // Home page - Features
            features_kicker: "规划您的下一次旅程",
            features_title: "您需要的更智能、更安全的旅行的一切",
            features_subtitle: "从灵感变为行程 — 一个简单的首页，展示安全行程规划师如何支持您旅行的每个阶段。",
            discover_destinations: "发现目的地",
            discover_destinations_desc: "浏览与您旅行风格、时间范围和舒适程度相匹配的灵感城市、海岸线和隐藏的宝石。",
            smart_trip_planning: "智能行程规划",
            smart_trip_planning_desc: "将想法转化为清晰的每日行程，路线平衡、时间现实，并为意外情况内置缓冲时间。",
            safe_secure_travel: "安全可靠的旅行",
            safe_secure_travel_desc: "使用突出更安全选项、更智能时间和沿途当地注意事项的指导，自信旅行。",

            // Home page - CTA
            cta_title: "准备好设计您的下一次安全冒险了吗？",
            cta_subtitle: "从心仪的目的地开始，或者简单地告诉安全行程规划师您喜欢如何旅行。您的行程从这里开始。",
            no_downloads: "无需下载 · 从任何设备规划",

            // Plan Trip page
            back_to_home: "返回首页",
            brand_sub: "规划行程 · 逐日安排",
            plan_form_title: "规划新行程",
            plan_form_subtitle: "选择您的城市、旅行风格和行程天数。安全行程规划师将生成一个简单、有条理的行程。",
            city_label: "城市 / 地区 / 地点",
            city_hint: "适用于城市、区县、地区、旅游区。",
            city_placeholder: "例如：喀什米尔、列城、巴黎、巴厘岛",
            mood_label: "旅行风格",
            mood_select: "选择风格",
            tourist: "🗺 观光",
            adventure: "🏔 冒险",
            historical: "🏛 历史",
            romantic: "💕 浪漫",
            nature: "🌿 自然",
            relaxation: "☀ 休闲",
            spiritual: "🕌 精神",
            family_friendly: "👨‍👩‍👧 家庭友好",
            days_label: "天数",
            generate_plan: "生成行程",
            searching_places: "正在搜索附近地点…",
            error_fill_fields: "请填写所有必填字段并输入有效值。",
            error_generate_plan: "无法生成行程计划。",
            error_no_data: "未返回行程数据。",
            plan_for: "您的行程",
            trip_plan: "您的行程计划",
            generated_by: "由安全行程规划师生成",
            no_places_assigned: "这一天没有分配地点。",
            view_on_map: "在 OpenStreetMap 上查看",
            add_to_trip: "添加到行程",
            remove_from_trip: "从行程移除",
            added_to_trip: "已添加到我的行程！",
            plan_generated_success: "行程计划生成成功。",

            // My Trip page
            bucket_title: "🧳 我的行程收藏清单",
            bucket_subtitle: "您从行程计划中保存的地点。点击移除可从列表中删除地点。",
            no_places_saved: "尚未保存任何地点",
            no_places_desc: "前往规划行程，生成您的行程，然后点击任意地点的 + 按钮将其添加到这里。",
            plan_a_trip: "规划行程 →",
            remove: "✕ 移除",

            // About page
            about_hero_tag: "关于安全行程规划师",
            about_hero_title: "您值得信赖的旅行助手",
            about_hero_subtitle: "帮助您发现、规划并自信旅行 — 一次安全的冒险。",
            platform_kicker: "关于平台",
            platform_title: "有组织的旅程。更安全的冒险。",
            platform_body: "安全行程规划师帮助游客发现目的地、高效规划行程、探索附近景点，并使用智能、注重安全的规划工具自信旅行。从周末短途到一生一次的旅程，我们组织每一步 — 路线、亮点、缓冲时间和安全检查点 — 让您专注于享受世界，而不用担心后勤问题。",
            travel_safely: "安全旅行",
            travel_safely_desc: "突出更安全选项、更智能时间和沿途当地注意事项的指导。",

            // Contact/Footer
            contact_title: "与安全行程规划师保持联系",
            contact_text: "有问题、合作想法或对旅程的反馈？联系我们，帮助塑造更安全旅行规划的未来。",
            email_label: "电子邮件",
            support_hours: "支持时间：9:00 – 18:00（当地时间）",
            copyright: "© 2026 安全行程规划师。保留所有权利。",
            footer_meta: "专为现代注重安全的探索者设计。",

            // Language switcher
            language: "语言"
        }
    };

    function getCurrentLang() {
        return localStorage.getItem(STORAGE_KEY) || DEFAULT_LANG;
    }

    function setLang(lang) {
        if (translations[lang]) {
            localStorage.setItem(STORAGE_KEY, lang);
            applyTranslations();
            updateLangSwitcher();
        }
    }

    function applyTranslations() {
        const lang = getCurrentLang();
        const t = translations[lang];
        if (!t) return;

        // Apply translations to elements with data-i18n attribute
        const elements = document.querySelectorAll('[data-i18n]');
        elements.forEach(function (el) {
            const key = el.getAttribute('data-i18n');
            if (t[key]) {
                // Check if element has child elements (like the hero title with span)
                // For elements with HTML children, we need to handle carefully
                if (el.children.length > 0 && el.getAttribute('data-i18n-html') !== 'true') {
                    // Only replace text nodes, preserve HTML children
                    const textNodes = [];
                    for (let i = 0; i < el.childNodes.length; i++) {
                        if (el.childNodes[i].nodeType === Node.TEXT_NODE && el.childNodes[i].textContent.trim()) {
                            textNodes.push(el.childNodes[i]);
                        }
                    }
                    // If there's only one text node at the beginning, replace it
                    if (textNodes.length > 0) {
                        textNodes[0].textContent = ' ' + t[key] + ' ';
                    } else {
                        // Check if first child is a text node
                        if (el.childNodes.length > 0 && el.childNodes[0].nodeType === Node.TEXT_NODE) {
                            el.childNodes[0].textContent = t[key];
                        } else {
                            // Prepend text before first child
                            el.insertBefore(document.createTextNode(t[key] + ' '), el.firstChild);
                        }
                    }
                } else {
                    el.textContent = t[key];
                }
            }
        });

        // Update HTML lang attribute
        document.documentElement.setAttribute('lang', lang);
    }

    function updateLangSwitcher() {
        const select = document.getElementById('lang-switcher');
        if (select) {
            select.value = getCurrentLang();
        }
    }

    function init() {
        // Apply translations on page load
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', applyTranslations);
        } else {
            applyTranslations();
        }
    }

    // Expose global API
    window.I18n = {
        setLang: setLang,
        getLang: getCurrentLang,
        t: function (key) {
            const lang = getCurrentLang();
            return translations[lang] && translations[lang][key] ? translations[lang][key] : key;
        },
        apply: applyTranslations,
        updateSwitcher: updateLangSwitcher
    };

    // Auto-init
    init();
})();
