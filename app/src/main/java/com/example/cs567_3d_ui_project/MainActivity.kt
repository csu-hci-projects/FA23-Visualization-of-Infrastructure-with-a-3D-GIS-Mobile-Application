package com.example.cs567_3d_ui_project

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.example.cs567_3d_ui_project.ui.theme.CS567_3D_UI_ProjectTheme

class MainActivity : AppCompatActivity() {

//    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolBar: Toolbar
//    private lateinit var navigationView: NavigationView
//    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
            setContentView(R.layout.activity_main)
            val navHostFragment = supportFragmentManager.findFragmentById(
                R.id.nav_host_container) as NavHostFragment

//            drawerLayout = findViewById(R.id.drawer_layout)

            toolBar = findViewById(R.id.toolbar)
            navController = navHostFragment.navController
            appBarConfiguration = AppBarConfiguration(navController.graph)
            //findViewById<NavigationView>(R.id.navigationView).setupWithNavController(navController)
            toolBar.setupWithNavController(navController, appBarConfiguration)
            toolBar.inflateMenu(R.menu.drawer_view)

//
//            drawerLayout = findViewById(R.id.drawer_layout)
//            drawerToggle = setupDrawerToggle()
//            drawerToggle.isDrawerIndicatorEnabled = true
//            drawerToggle.syncState()
//            drawerLayout.addDrawerListener(drawerToggle)

        }
        catch(e: Exception){
            Log.e("Error During onCreate", e.message, e)
            showError(e.message.toString())
            throw e
        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_container)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.drawer_view, menu)
        return true
//        return super.onCreateOptionsMenu(menu)
    }


    private fun showError(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        Log.e(localClassName, message)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CS567_3D_UI_ProjectTheme {
        Greeting("Android")
    }
}