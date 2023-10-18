package ch.joshuah.bibleverseapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ch.joshuah.bibleverseapp.fragments.CreditFragment
import ch.joshuah.bibleverseapp.fragments.SettingFragment
import ch.joshuah.bibleverseapp.fragments.VerseFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadFragment(VerseFragment())
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

    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.activity_main_container,fragment)
        transaction.commit()
    }

}