# OpenStock 📦

A complete Android inventory & sales management app built with Kotlin, Room, and Material Design.

**Version: 1.31.2**  
*Made by [xeoniii.dev](https://github.com/xeoniii)*

---

## 🌟 New in v1.31.2
- **New Branding**: Modern 📦 box icon applied as the new app icon and across the interface.
- **Enhanced Product Photos**: Three ways to add photos—Select from gallery, take with camera, or download from a web URL.
- **Product Photos**: Add/edit photos for each product from the catalogue.
- **Sales Gallery**: A new "Gallery Mode" for adding items to sales with high-quality swipeable images.
- **Dark Mode Support**: Seamlessly switch between Light and Dark themes from the side menu.
- **Shop Logo Customization**: Personalize your workspace by uploading a custom logo to the navigation drawer.
- **PDF Receipt Generation**: Generate professional 80mm thermal-style PDF receipts for any sale.
- **Dual Modes (Personal vs. Sales)**: Secure your data with a restricted "Sales Mode" for staff use.
- **Rich "About" Dialog**: Integrated Markdown rendering for a clean and detailed app information screen.
- **Password Protection**: Customizable security for mode switching and sensitive deletions.
- **Shop Branding**: Your shop name now appears on all generated bills and app headers.

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
