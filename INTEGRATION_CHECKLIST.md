# Integration Checklist

## Status: ✅ Files Created & Ready for Integration

सभी नई files GitHub branch पर push हो चुकी हैं:
- `feature/low-stock-inward-autofill`

## What's Done ✅

| Task | File(s) | Status |
|---|---|---|
| Low-stock warning popup | `LowStockWarningDialog.kt` | ✅ Complete |
| Dashboard button integration | `DashboardWithLowStockButton.kt` | ✅ Complete |
| Common UI components | `CommonUi.kt` | ✅ Complete |
| Split payment UI | `PaymentSection.kt` | ✅ Complete |
| Variety add/edit dialog | `VarietyEditorDialog.kt` | ✅ Complete |
| Auto-fill & validation logic | `InwardStockAutoFillHelper.kt` | ✅ Complete |
| Helper functions | `InwardStockUiHelpers.kt` | ✅ Complete (existing) |
| Documentation | `FEATURE_README.md`, `IMPLEMENTATION_GUIDE.md` | ✅ Complete |

## What Remains ⏳

### Manual Integration with ShopScreens.kt

`ShopScreens.kt` में manually 5 changes करने हैं:

**1. Category Filtering** (Line ~8200)
```kotlin
// FIND:
val inwardCategories = categories

// REPLACE:
val inwardCategories = getInwardOnlyCategories(categories)
```

**2. Add State Variables** (Line ~8150)
```kotlin
var cashAmountText by remember { mutableStateOf("") }
var onlineAmountText by remember { mutableStateOf("") }
var showAddVariety by remember { mutableStateOf(false) }
var editVarietyName by remember { mutableStateOf("") }
```

**3. Payment Section** (Line ~8500)
```kotlin
// REMOVE old payment mode selector

// ADD:
if (editPurchase == null) {
    PaymentSection(
        paymentStatus = paymentStatus,
        onPaymentStatusChange = { paymentStatus = it },
        cashAmount = cashAmountText,
        onCashAmountChange = { cashAmountText = it },
        onlineAmount = onlineAmountText,
        onOnlineAmountChange = { onlineAmountText = it },
        billTotal = billTotal,
        isEditingPurchase = editPurchase != null
    )
}
```

**4. Item Auto-Fill** (Line ~8650 - inside items(draftItems) loop)
```kotlin
// Add after bestMatch selection:
if (bestMatch?.first != null) {
    val selectedStock = varietyStocks.firstOrNull {
        it.categoryName.equals(selectedCategory, ignoreCase = true) &&
        it.companyName.equals(selectedBrand, ignoreCase = true) &&
        it.varietyName.equals(bestMatch.first, ignoreCase = true)
    }
    if (selectedStock != null) {
        draftItems[idx] = draft.autoFillFromSavedVariety(
            bestMatch.first!!,
            selectedStock,
            draft.boxes
        )
        lineAmountTexts[draft.id] = 
            (draft.boxes * selectedStock.lastCostPrice).toString()
    }
}
```

**5. Payment Save** (Line ~8900)
```kotlin
// FIND:
paidAmount = paid,
paymentMode = paymentMode,

// REPLACE:
val cashVal = cashAmountText.toDoubleOrNull() ?: 0.0
val onlineVal = onlineAmountText.toDoubleOrNull() ?: 0.0
paidAmount = cashVal + onlineVal,
paymentMode = when {
    cashVal > 0 && onlineVal > 0 -> "CASH+UPI"
    cashVal > 0 -> "CASH"
    onlineVal > 0 -> "UPI"
    else -> paymentStatus
},
```

### Add Imports to ShopScreens.kt

```kotlin
import com.refreshsoda.app.ui.screens.getInwardOnlyCategories
import com.refreshsoda.app.ui.screens.PaymentSection
import com.refreshsoda.app.ui.screens.VarietyEditorDialog
import com.refreshsoda.app.ui.screens.autoFillFromSavedVariety
import com.refreshsoda.app.ui.screens.calculatePaymentStatus
import com.refreshsoda.app.ui.screens.calculateDueAmount
```

## Build & Test

```bash
# Gradle build check
./gradlew build

# Run tests
./gradlew test

# Build APK
./gradlew assembleDebug
```

## Verification

**Dashboard:**
- [ ] Low-stock button दिखता है
- [ ] Button tap पर popup आता है
- [ ] Items correct दिखते हैं

**Inward Stock:**
- [ ] सिर्फ ice-cream categories दिखते हैं
- [ ] Item select पर cost/MRP auto-fill होते हैं
- [ ] Amount automatically calculate होता है
- [ ] Manual edit possible है

**Payment:**
- [ ] Cash amount field दिखता है
- [ ] Online amount field दिखता है
- [ ] Total paid auto-calculate होता है
- [ ] Due amount दिखता है
- [ ] Validation काम करती है

## Files Reference

| File | Location |
|---|---|
| LowStockWarningDialog.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| DashboardWithLowStockButton.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| CommonUi.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| PaymentSection.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| VarietyEditorDialog.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| InwardStockAutoFillHelper.kt | `app/src/main/java/com/refreshsoda/app/ui/screens/` |
| FEATURE_README.md | Root directory |
| IMPLEMENTATION_GUIDE.md | Root directory |
| INTEGRATION_CHECKLIST.md | Root directory (this file) |

## Git Commit History

```
85a5736 - Extract common UI components: AppTextField and WhatsApp utility
3acb5ec - Add payment section with cash and online split amount entry
99b0a67 - Add variety editor dialog with name, cost, and MRP fields
5756df6 - Add inward stock auto-fill and validation helpers
93a11da - Add implementation guide for InwardBillDialog integration
eede7fd - Add comprehensive README for inward stock enhancement feature
```

## Next Steps

1. **Read `FEATURE_README.md`** - समझें कि کیا features add हो रहे हैं
2. **Read `IMPLEMENTATION_GUIDE.md`** - Exact line numbers और code snippets देखें
3. **Make 5 manual changes** in `ShopScreens.kt` as listed above
4. **Add imports** as listed
5. **Run Gradle build**
6. **Test on device/emulator**
7. **Create pull request** and merge after review

---

## Support Files

- `FEATURE_README.md` - Complete feature overview and usage
- `IMPLEMENTATION_GUIDE.md` - Step-by-step integration instructions
- `INTEGRATION_CHECKLIST.md` - This file

---

**Branch:** `feature/low-stock-inward-autofill`
**Status:** Ready for integration
**Last Updated:** 2026-11-01
