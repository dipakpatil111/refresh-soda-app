# Refresh Soda & Ice-cream - Native Android App (Kotlin + Jetpack Compose)

This is the pure Native Android project for **Refresh Soda & Ice-cream** (Dindoli, Surat) built strictly with:
- **Core Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Modern declarative UI)
- **Local Database**: Room Database (SQLite abstraction with offline-first caching)
- **Cloud Database**: Firebase Firestore (Realtime sync)
- **Concurrency**: Kotlin Coroutines & StateFlow / Flow
- **Architecture**: Android Architecture Components (ViewModel, StateFlow, Room DAO pattern)

---

## Folder Structure
```text
RefreshSodaAndroidApp/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/refreshsoda/app/
│       │   ├── MainActivity.kt
│       │   ├── RefreshApp.kt
│       │   ├── data/
│       │   │   ├── model/Product.kt, SaleTransaction.kt, CustomerKhata.kt
│       │   │   ├── local/AppDatabase.kt, ShopDao.kt
│       │   │   └── firebase/FirebaseSyncService.kt
│       │   └── ui/
│       │       ├── screens/DashboardScreen.kt, POSScreen.kt, InventoryScreen.kt, KhataScreen.kt, MenuSetupScreen.kt
│       │       ├── theme/Theme.kt
│       │       └── viewmodel/ShopViewModel.kt
│       └── res/values/strings.xml
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Features Implemented:
1. **Dashboard**: Aaj ka kul dhandha, Cash (Galla), Paytm/UPI, Udhaar split, live Godown box overview, and recent sales.
2. **Quick POS Billing**: Rapid item entry, user-managed category filters, and Cash/UPI/Udhaar payment recording.
3. **Owner Menu Setup**: Add and manage custom brands, categories, items, and rates; no sample catalog is preloaded.
4. **Godown Box Stock**: Sealed box tracker with low-stock warnings limited to ice-cream products.
5. **Customer Udhaar Khata**: Ledger management with quick repayment (`Jama Karein`).
6. **Room Database & Firebase**: Automatic local persistence in Room DB with Firestore cloud sync.

---

## How to Open & Run:
1. Extract `RefreshSodaAndroidApp.zip`.
2. Open **Android Studio** (Koala / Ladybug or newer).
3. Select **Open** and choose the `RefreshSodaAndroidApp` folder.
4. Android Studio will automatically sync Gradle and download dependencies.
5. Click **Run (Shift + F10)** on your connected Android phone or emulator.
