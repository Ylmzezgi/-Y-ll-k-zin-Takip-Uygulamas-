package com.ezgiyilmaz.yllkzintakipuygulamas

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityMainBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        var view=binding.root
        setContentView(view)
        enableEdgeToEdge()

        auth=FirebaseAuth.getInstance()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun butonOnClick(view: View){
        startActivity(Intent(this,izinBilgi::class.java))
    }
    fun girişOnClick(view: View){
        startActivity(Intent(this,LoginPage::class.java))
    }
    fun kayıtOnClick(view: View){
        startActivity(Intent(this,signUpPage::class.java))

    }
    fun reclerOnclick(view: View){
        startActivity(Intent(this,izinTalebi::class.java))

    }
    fun butonOnClick2(view: View){
        auth.signOut()
        Toast.makeText(this, "Başarıyla çıkış yapıldı", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }
    fun pieChartOnClick(view: View){
        startActivity(Intent(this,GrafikGosterim::class.java))
    }

}