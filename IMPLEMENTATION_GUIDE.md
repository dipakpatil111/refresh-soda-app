// ============================================================================
// IMPLEMENTATION GUIDE: Inward Stock Dialog Updates
// ============================================================================
// 
// This document describes the exact changes to make to InwardBillDialog() 
// in ShopScreens.kt to integrate the new features:
// 
// 1. Ice-cream only category filtering
// 2. Cost + MRP auto-fill on item selection  
// 3. Split cash + online payment entry
// 4. Variety add/edit with pricing
//
// ============================================================================

// STEP 1: Replace category filtering at line ~8200
// 
// FIND THIS:
//    val inwardCategories = categories
//
// REPLACE WITH:
//    val inwardCategories = getInwardOnlyCategories(categories)
//
// This ensures only box-tracked ice-cream categories appear in inward dialog.

// ============================================================================

// STEP 2: Replace payment section at line ~8500 
//
// FIND THIS:
//    if (editPurchase == null && paymentStatus != "UNPAID") {
//        item {
//            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                Text(viewModel.getString("payment_mode_label"), ...)
//                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
//                    items(listOf("UPI", "CASH", "BANK")) { mode ->
//                        FilterChip(...)
//                    }
//                }
//                ...
//            }
//        }
//    }
//
// REPLACE WITH:
//    if (editPurchase == null) {
//        PaymentSection(
//            paymentStatus = paymentStatus,
//            onPaymentStatusChange = { paymentStatus = it },
//            cashAmount = cashAmountText,
//            onCashAmountChange = { cashAmountText = it },
//            onlineAmount = onlineAmountText,
//            onOnlineAmountChange = { onlineAmountText = it },
//            billTotal = billTotal,
//            isEditingPurchase = editPurchase != null
//        )
//    }
//
// Add two new state variables at the beginning of InwardBillDialog:
//    var cashAmountText by remember { mutableStateOf("") }
//    var onlineAmountText by remember { mutableStateOf("") }

// ============================================================================

// STEP 3: Auto-fill on variety selection at line ~8650
//
// FIND THIS section where draft items are listed:
//    items(draftItems, key = { it.id }) { draft ->
//        val idx = draftItems.indexOfFirst { it.id == draft.id }
//        val bestMatch = remember(...) { ... }
//        ...
//        if (bestMatch?.first != null) {
//            Surface(...) {
//                Row(...) {
//                    Text("💡 Match:", ...)
//                    Text("${bestMatch.first} (tap to select)", ...)
//                }
//            }
//        }
//
// ADD after the bestMatch click handler:
//    val selectedStock = varietyStocks.firstOrNull {
//        it.categoryName.equals(selectedCategory, ignoreCase = true) &&
//        it.companyName.equals(selectedBrand, ignoreCase = true) &&
//        it.varietyName.equals(bestMatch.first, ignoreCase = true)
//    }
//    if (selectedStock != null) {
//        draftItems[idx] = draft.autoFillFromSavedVariety(
//            bestMatch.first!!,
//            selectedStock,
//            draft.boxes
//        )
//        lineAmountTexts[draft.id] = (draft.boxes * selectedStock.lastCostPrice).toString()
//    }

// ============================================================================

// STEP 4: Add variety editor trigger at line ~8350
//
// In the "+ Add company" button section, ADD a button:
//    TextButton(onClick = { showAddVariety = true }) {
//        Text("+ Add/Edit Variety")
//    }
//
// Add state at beginning:
//    var showAddVariety by remember { mutableStateOf(false) }
//    var editVarietyName by remember { mutableStateOf("") }

// ============================================================================

// STEP 5: Save payment amounts at line ~8900
//
// In viewModel.saveInwardPurchase call, FIND:
//    paidAmount = paid,
//    paymentMode = paymentMode,
//
// REPLACE WITH:
//    val cashVal = cashAmountText.toDoubleOrNull() ?: 0.0
//    val onlineVal = onlineAmountText.toDoubleOrNull() ?: 0.0
//    paidAmount = cashVal + onlineVal,
//    paymentMode = when {
//        cashVal > 0 && onlineVal > 0 -> "CASH+UPI"
//        cashVal > 0 -> "CASH"
//        onlineVal > 0 -> "UPI"
//        else -> paymentStatus
//    },

// ============================================================================

// IMPORTS to add at top of ShopScreens.kt:
//
// import com.refreshsoda.app.ui.screens.getInwardOnlyCategories
// import com.refreshsoda.app.ui.screens.PaymentSection
// import com.refreshsoda.app.ui.screens.VarietyEditorDialog
// import com.refreshsoda.app.ui.screens.InwardStockAutoFillHelper
// import com.refreshsoda.app.ui.screens.autoFillFromSavedVariety
// import com.refreshsoda.app.ui.screens.recalculateAmount
// import com.refreshsoda.app.ui.screens.updateCostAndAmount
// import com.refreshsoda.app.ui.screens.calculatePaymentStatus
// import com.refreshsoda.app.ui.screens.calculateDueAmount

// ============================================================================
