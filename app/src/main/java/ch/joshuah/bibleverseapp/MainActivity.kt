package ch.joshuah.bibleverseapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import ch.joshuah.bibleverseapp.fragments.CreditFragment
import ch.joshuah.bibleverseapp.fragments.SettingFragment
import ch.joshuah.bibleverseapp.fragments.VerseFragment
import ch.joshuah.bibleverseapp.widgets.BibleVerseWidget
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle window insets to prevent content from being behind system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (intent.action == "ch.joshuah.bibleverseapp.UPDATE_WIDGETS") {
            BibleVerseWidget.updateWidgets(this)
        }

        if (savedInstanceState == null) {
            loadFragment(VerseFragment())
        }

        bottomNav = findViewById(R.id.activity_main_bottomNav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment(VerseFragment())
                    true
                }
                R.id.settings -> {
                    loadFragment(SettingFragment())
                    true
                }
                R.id.credits -> {
                    loadFragment(CreditFragment())
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.activity_main_container, fragment)
        transaction.commit()
    }
}
