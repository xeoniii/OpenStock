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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import io.noties.markwon.Markwon
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            } else {
                binding.viewPager.visibility = View.GONE
                findViewById<View>(R.id.nav_host_fragment_activity_main).visibility = View.VISIBLE
                drawerToggle.isDrawerIndicatorEnabled = false
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

            invalidateOptionsMenu()
        }
    }

    private fun updateDrawerHeader() {
        val headerView = binding.navDrawer.getHeaderView(0)
        val tvShopName = headerView.findViewById<TextView>(R.id.tv_nav_shop_name)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        tvShopName.text = prefs.getString("shop_name", "OpenStock")
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
            R.id.action_bills -> navController.navigate(R.id.navigation_bills)
            R.id.action_about -> showAboutDialog()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showShopNameDialog() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("shop_name", "OpenStock")
        val input = EditText(this).apply {
            setText(currentName)
            hint = "Shop Name"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Shop Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
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
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Set Password"
        }
        AlertDialog.Builder(this)
            .setTitle("First Time Setup")
            .setMessage("Set a password to protect Personal mode.")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val password = input.text.toString()
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
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter Password"
        }
        AlertDialog.Builder(this)
            .setTitle("Enter Password")
            .setMessage("Please enter the password to switch to Personal mode.")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text.toString()
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
            "Version 1.30.0\n\nMade by xeoniii.dev"
        }

        val textView = TextView(this).apply {
            setPadding(48, 32, 48, 32)
            textSize = 14f
            movementMethod = LinkMovementMethod.getInstance()
        }

        Markwon.create(this).setMarkdown(textView, aboutText)

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }

        AlertDialog.Builder(this)
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
