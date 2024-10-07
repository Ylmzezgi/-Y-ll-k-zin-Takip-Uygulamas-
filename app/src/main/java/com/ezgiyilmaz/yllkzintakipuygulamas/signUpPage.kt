package com.ezgiyilmaz.yllkzintakipuygulamas

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivitySignUpPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class signUpPage : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpPageBinding
    private lateinit var auth: FirebaseAuth
    lateinit var dateEditText:EditText
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySignUpPageBinding.inflate(layoutInflater)
        var view=binding.root
        setContentView(view)
        enableEdgeToEdge()

        auth=Firebase.auth
        storage = FirebaseStorage.getInstance()

        db = FirebaseFirestore.getInstance()

        dateEditText = findViewById(R.id.datePickerEditText)

        showDatePicker()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

        fun showDatePicker() {
            // DatePicker

            dateEditText.setText(SimpleDateFormat("dd/MM/yyyy").format(System.currentTimeMillis()))

            var cal = Calendar.getInstance()

            val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val myFormat = "dd/MM/yyyy"
                val sdf = SimpleDateFormat(myFormat, Locale.US)
                dateEditText.setText(sdf.format(cal.time))
            }

            dateEditText.setOnClickListener {

                Log.d("Clicked", "Interview Date Clicked")

                val dialog = DatePickerDialog(this, dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                dialog.datePicker.maxDate = CalendarHelper.getCurrentDateInMills()
                dialog.show()
            }
        }



    fun loginOnClick(view: View){
        startActivity(Intent(this,LoginPage::class.java))
    }


    fun signupOnClick(view: View){
        val name=binding.nameText.text.toString()
        val surname=binding.surnameText.text.toString()
        val email=binding.emailText.text.toString()
        val password=binding.passwordText.text.toString()
        val dataPicker=binding.datePickerEditText.text.toString()
        auth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {
            val user=auth.currentUser
            if(user !=null){
                val displayName="$name $surname"
                val profileUpdates= userProfileChangeRequest {
                    this.displayName=displayName
                }
                user.updateProfile(profileUpdates).addOnCompleteListener{task->
                    if(task.isSuccessful){

                        Log.d("FirebaseAuth", "Profil güncellendi: İsim - $displayName")

                        val userData= hashMapOf(
                            "name" to displayName,
                            "email" to email,
                            "datapicker" to dataPicker
                        )
                        db.collection("users").document(user.uid).set(userData).addOnSuccessListener {
                            Toast.makeText(this, "Kayıt başarılı", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } .addOnFailureListener { e ->
                            Log.e("Firestore", "Veri kaydedilemedi: ${e.message}")
                            Toast.makeText(this, "Veri kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    } else {
                        Log.e("FirebaseAuth", "Profil güncellenemedi: ${task.exception?.message}")
                        Toast.makeText(this, "Profil güncellenemedi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }.addOnFailureListener { exception ->
            Log.e("FirebaseStorage", "kayıt başarısız: ${exception.localizedMessage}")
            Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
        }

    }
}
