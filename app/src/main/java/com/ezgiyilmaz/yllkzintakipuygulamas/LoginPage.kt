package com.ezgiyilmaz.yllkzintakipuygulamas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityLoginPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginPage : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginPageBinding.inflate(layoutInflater)
        var view=binding.root
        setContentView(view)
        enableEdgeToEdge()

        auth=FirebaseAuth.getInstance()
        val curentUser=auth.currentUser
        if(curentUser!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    fun loginOnClick(view: View) {
       try {
           val email = binding.loginEmailText.text.toString()
           val password = binding.loginPasswordText.text.toString()

           if(email.equals("") || password.equals("")){
               Toast.makeText(this, "Email ve şifre giriniz", Toast.LENGTH_LONG).show()
           }else{
               auth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
                   startActivity(Intent(this,MainActivity::class.java))
               }.addOnFailureListener{
                   Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
               }
           }
       } catch (e:Exception) {
           e.localizedMessage
       }
    }
}