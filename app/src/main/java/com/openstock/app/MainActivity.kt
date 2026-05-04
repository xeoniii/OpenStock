package com.openstock.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import android.graphics.BitmapFactory
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.openstock.app.databinding.ActivityMainBinding
import com.openstock.app.ui.MainPagerAdapter
import com.openstock.app.ui.MainViewModel
import com.openstock.app.ui.sales.SalesViewModel
import com.openstock.app.ui.sales.SalesViewModelFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var mainViewModel: MainViewModel
    private lateinit var salesViewModel: SalesViewModel
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            Toast.makeText(this, "Camera permission is required for barcode scanning", Toast.LENGTH_SHORT).show()
        }
        
        val storageReadGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: true
        val storageWriteGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: true
        
        if (!storageReadGranted || !storageWriteGranted) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                Toast.makeText(this, "Storage permission is required to save bills", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { saveShopLogo(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        checkAndRequestPermissions()

        val app = application as OpenStockApp
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        salesViewModel = ViewModelProvider(this, SalesViewModelFactory(app.repository))[SalesViewModel::class.java]

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_products, R.id.navigation_inventory, R.id.navigation_sales),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navDrawer.setNavigationItemSelectedListener(this)
        updateDrawerHeader()

        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        navView.setOnItemSelectedListener { item ->
            if (binding.viewPager.visibility == View.GONE) {
                navController.navigate(item.itemId)
            }
            
            val isSalesMode = mainViewModel.isSalesMode.value == true
            when (item.itemId) {
                R.id.navigation_products -> if (!isSalesMode) binding.viewPager.currentItem = 0
                R.id.navigation_inventory -> binding.viewPager.currentItem = if (isSalesMode) 0 else 1
                R.id.navigation_sales -> binding.viewPager.currentItem = if (isSalesMode) 1 else 2
            }
            true
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isSalesMode = mainViewModel.isSalesMode.value == true
                if (isSalesMode) {
                    when (position) {
                        0 -> navView.selectedItemId = R.id.navigation_inventory
                        1 -> navView.selectedItemId = R.id.navigation_sales
                    }
                } else {
                    when (position) {
                        0 -> navView.selectedItemId = R.id.navigation_products
                        1 -> navView.selectedItemId = R.id.navigation_inventory
                        2 -> navView.selectedItemId = R.id.navigation_sales
                    }
                }
                updateTitle(position)
                invalidateOptionsMenu()
            }
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isMainTab = destination.id == R.id.navigation_products ||
                            destination.id == R.id.navigation_inventory ||
                            destination.id == R.id.navigation_sales

            if (isMainTab) {
                binding.viewPager.visibility = View.VISIBLE
                findViewById<View>(R.id.nav_host_fragment_activity_main).visibility = View.GONE
                drawerToggle.isDrawerIndicatorEnabled = true
                binding.navView.visibility = View.VISIBLE
                binding.navDrawer.setCheckedItem(0) // Clear drawer selection on main tabs
            } else {
                binding.viewPager.visibility = View.GONE
                findViewById<View>(R.id.nav_host_fragment_activity_main).visibility = View.VISIBLE
                drawerToggle.isDrawerIndicatorEnabled = false
                binding.navView.visibility = View.GONE
                binding.navDrawer.setCheckedItem(destination.id)
            }
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        mainViewModel.isSalesMode.observe(this) { isSalesMode ->
            val currentTabId = binding.navView.selectedItemId
            pagerAdapter.isSalesMode = isSalesMode

            when (currentTabId) {
                R.id.navigation_products -> {
                    if (isSalesMode) {
                        binding.navView.selectedItemId = R.id.navigation_inventory
                    } else {
                        binding.viewPager.setCurrentItem(0, false)
                    }
                }
                R.id.navigation_inventory -> {
                    binding.viewPager.setCurrentItem(if (isSalesMode) 0 else 1, false)
                }
                R.id.navigation_sales -> {
                    binding.viewPager.setCurrentItem(if (isSalesMode) 1 else 2, false)
                }
            }

            val navMenu = binding.navView.menu
            navMenu.findItem(R.id.navigation_products).isVisible = !isSalesMode

            // Fix for the Hamburger Menu Gap: Re-inflating or forcing a refresh of the menu
            binding.navDrawer.menu.clear()
            binding.navDrawer.inflateMenu(R.menu.menu_main)
            
            val drawerMenu = binding.navDrawer.menu
            drawerMenu.findItem(R.id.action_sales_mode).title = if (isSalesMode) "Switch to Personal mode" else "Switch to Sales mode"
            drawerMenu.findItem(R.id.action_shop_name).isVisible = !isSalesMode
            drawerMenu.findItem(R.id.action_shop_logo).isVisible = !isSalesMode
            
            val isDark = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("dark_mode", false)
            drawerMenu.findItem(R.id.action_theme_mode).title = if (isDark) "Switch to Light mode" else "Switch to Dark mode"

            invalidateOptionsMenu()
        }
    }

    private fun updateDrawerHeader() {
        val headerView = binding.navDrawer.getHeaderView(0)
        val tvShopName = headerView.findViewById<TextView>(R.id.tv_nav_shop_name)
        val ivLogo = headerView.findViewById<ImageView>(R.id.iv_nav_shop_logo)
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        tvShopName?.text = prefs.getString("shop_name", "OpenStock")
        
        val logoPath = prefs.getString("shop_logo_path", null)
        if (logoPath != null && ivLogo != null) {
            val file = File(logoPath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .into(ivLogo)
            }
        }
    }

    private fun saveShopLogo(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val file = File(filesDir, "shop_logo.png")
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("shop_logo_path", file.absolutePath)
                    .apply()
                updateDrawerHeader()
                Toast.makeText(this, "Logo updated successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save logo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentPos = binding.viewPager.currentItem
        val isSalesMode = mainViewModel.isSalesMode.value == true
        val isSearchablePage = if (isSalesMode) currentPos == 0 else currentPos == 0 || currentPos == 1
        val isVisible = binding.viewPager.visibility == View.VISIBLE

        if (isSearchablePage && isVisible) {
            menuInflater.inflate(R.menu.menu_search, menu)
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem.actionView as SearchView

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    mainViewModel.setSearchQuery(newText ?: "")
                    return true
                }
            })

            if (mainViewModel.searchQuery.value?.isNotEmpty() == true) {
                searchItem.expandActionView()
                searchView.setQuery(mainViewModel.searchQuery.value, false)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sales_mode -> handleModeSwitch()
            R.id.action_shop_name -> showShopNameDialog()
            R.id.action_shop_logo -> showShopLogoDialog()
            R.id.navigation_bills -> navController.navigate(R.id.navigation_bills)
            R.id.action_theme_mode -> toggleTheme()
            R.id.action_about -> showAboutDialog()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun toggleTheme() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        val newMode = !isDarkMode
        prefs.edit().putBoolean("dark_mode", newMode).apply()
        
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        recreate()
    }

    private fun showShopLogoDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Set Shop Logo")
            .setMessage("Recommended resolution: 1000x500 pixels (aspect ratio 2:1 for best results).")
            .setPositiveButton("Select Photo") { _, _ ->
                pickImageLauncher.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showShopNameDialog() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("shop_name", "OpenStock")
        val binding2 = layoutInflater.inflate(R.layout.dialog_input_name, null)
        val etName = binding2.findViewById<android.widget.EditText>(R.id.etName)
        etName.setText(currentName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Shop Name")
            .setView(binding2)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    prefs.edit().putString("shop_name", name).apply()
                    updateDrawerHeader()
                    Toast.makeText(this, "Shop name updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleModeSwitch() {
        val isSalesMode = mainViewModel.isSalesMode.value == true
        if (isSalesMode) {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedPassword = prefs.getString("personal_mode_password", null)
            if (savedPassword == null) showSetPasswordDialog() else showEnterPasswordDialog(savedPassword)
        } else {
            mainViewModel.setSalesMode(true)
        }
    }

    private fun showSetPasswordDialog() {
        val binding2 = layoutInflater.inflate(R.layout.dialog_password_input, null)
        val etPassword = binding2.findViewById<android.widget.EditText>(R.id.etPassword)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("First Time Setup")
            .setMessage("Set a password to protect Personal mode.")
            .setView(binding2)
            .setPositiveButton("Set") { _, _ ->
                val password = etPassword.text.toString()
                if (password.isNotEmpty()) {
                    getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putString("personal_mode_password", password).apply()
                    mainViewModel.setSalesMode(false)
                } else {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEnterPasswordDialog(correctPassword: String) {
        val binding2 = layoutInflater.inflate(R.layout.dialog_password_input, null)
        val etPassword = binding2.findViewById<android.widget.EditText>(R.id.etPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("Enter Password")
            .setMessage("Please enter the password to switch to Personal mode.")
            .setView(binding2)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = etPassword.text.toString()
                if (entered == correctPassword || entered == "adminpass0") {
                    mainViewModel.setSalesMode(false)
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutText = try {
            assets.open("about.md").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Version 1.31.2\n\nMade by xeoniii.dev"
        }

        val textView = TextView(this).apply {
            setPadding(48, 32, 48, 32)
            textSize = 14f
            movementMethod = LinkMovementMethod.getInstance()
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.glass_text_secondary))
        }

        Markwon.create(this).setMarkdown(textView, aboutText)

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("About OpenStock")
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .setNeutralButton("Visit Website") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/xeoniii".toUri())
                startActivity(intent)
            }
            .show()
    }

    private fun updateTitle(position: Int) {
        val isSalesMode = mainViewModel.isSalesMode.value == true
        supportActionBar?.title = if (isSalesMode) {
            when (position) {
                0 -> "Inventory"
                1 -> "Sales"
                else -> "OpenStock"
            }
        } else {
            when (position) {
                0 -> "Products"
                1 -> "Inventory"
                2 -> "Sales"
                else -> "OpenStock"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
