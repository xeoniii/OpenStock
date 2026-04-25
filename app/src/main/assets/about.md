# OpenStock 📦

A complete Android inventory & sales management app built with Kotlin, Room, and Material Design.

**Version: 1.30.1**  
*Made by [xeoniii.dev](https://github.com/xeoniii)*

---

## 🌟 New in v1.30.1
- **PDF Receipt Generation**: Generate 80mm thermal-style PDF receipts for any sale group.
- **Dual Modes (Personal vs. Sales)**: Switch between a full-access "Personal Mode" and a restricted "Sales Mode" for shop floor use.
- **Password Protection**: Protect sensitive actions and mode switching with a customizable password.
- **Shop Customization**: Set your shop name to appear in the app drawer and on all generated receipts.
- **Improved Navigation**: Dynamic bottom navigation and drawer that adapts to the active mode.

---

## Features

### 📦 Products (Personal Mode Only)
- Add, edit, and delete products from the master catalogue.
- Scan barcodes using the device camera (ZXing).
- Store wholesale price, retail price, unit (e.g., pcs, kg), and description.
- Search products by name or barcode.
- Per-product profit margin shown on cards.

### 🏪 Inventory
- Add any product to inventory with a stock quantity.
- Edit existing stock levels.
- **Auto-sync**: Verified sales automatically decrease stock levels.
- **Auto-clean**: Items with zero stock can be automatically managed.
- Search inventory by product name.

### 🧾 Sales & Billing
- Create named sale groups (e.g., "Sale for ABC Supermarket").
- Add multiple products with quantities to a group.
- Prices auto-filled from product catalogue (editable per sale).
- **Override Totals**: Manually adjust the final retail price for discounts or fees.
- **Bill Generation**: One-tap PDF generation for receipts.
- **Verification**: Formally "verify" a sale to deduct items from inventory permanently.
- **Dashboard banner**: Shows all-time totals across all sales (Personal Mode).

### 📱 Navigation & UI
- **Swipeable Tabs**: Smoothly transition between Products, Inventory, and Sales.
- **Dynamic Drawer**: Access mode switching, generated bills, and shop settings.
- **Modern Search**: Unified search bar that adapts to each section.
- **Material Design 3**: Modern, clean, and responsive interface.
