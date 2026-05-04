# OpenStock 📦

A complete Android inventory & sales management app built with Kotlin, Room, and Material Design.

**Version: 1.32.0**  
*Made by [xeoniii.dev](https://xeoniii.github.io/openstocks)*

---

## 🌟 New in v1.32.0
- **Overhauled UI**: Asynchronous image loading in the navigation drawer and improved list rendering in the Sales Gallery.
- **Performance Boost**: Moved search filtering to the database for faster results and reduced memory usage.
- **Enhanced Database**: Added indexing to sales history for near-instant reporting as your data grows.
- **Release Stability**: Improved build pipeline for more reliable APK installations and secure release signing.

---

## Features

### 📦 Products (Personal Mode Only)
- Add, edit, and delete products from the master catalogue.
- Scan barcodes using the device camera (ZXing integration).
- Store wholesale/retail prices, units (pcs, kg, etc.), and descriptions.
- Search products instantly by name or barcode.
- Real-time profit margin visualization on product cards.

### 🏪 Inventory
- Manage stock levels for all catalogued products.
- **Smart Sync**: Stock is automatically deducted when sales are verified.
- **Stock Tracking**: Easily identify low-stock items.
- Unified search for quick inventory lookups.

### 🧾 Sales & Billing
- Create organized sale groups for different customers or sessions.
- Adjustable pricing per sale item (defaults from catalogue).
- **Price Overrides**: Add custom discounts or service fees to the final total.
- **One-Tap Billing**: Generate PDF receipts saved directly to your device.
- **Verification System**: Formally close sales to update inventory and financial records.
- **Financial Dashboard**: View total revenue, wholesale costs, and net profit at a glance.

### 📱 Navigation & UI
- **Dynamic Drawer**: Access settings, bills, theme toggles, and mode switching.
- **Adaptive UI**: The interface (tabs and menus) morphs based on whether you are in Personal or Sales mode.
- **Dark Theme**: Eye-friendly interface designed for low-light environments.
- **Modern Search**: Persistent, context-aware search bar across all main tabs.
- **Material 3 Design**: Clean, modern, and highly responsive user experience.
