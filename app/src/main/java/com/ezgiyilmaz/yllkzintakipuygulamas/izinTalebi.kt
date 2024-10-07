package com.ezgiyilmaz.yllkzintakipuygulamas

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityIzinBilgiBinding
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityIzinTalebiBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class izinTalebi : AppCompatActivity() {
    private lateinit var binding: ActivityIzinTalebiBinding
    private lateinit var recyclerViewAdapter: TarihAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    val tarihListesi: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIzinTalebiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // RecyclerView'u ayarla
        recyclerViewAdapter = TarihAdapter(tarihListesi) // Adapter'ı başlat
        binding.reclerView.layoutManager = LinearLayoutManager(this) // LayoutManager'ı ayarla
        binding.reclerView.adapter = recyclerViewAdapter // Adapter'ı RecyclerView'a ata

        // Veri çekme
        fetchIzinTalepleri()

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchIzinTalepleri() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid // Mevcut kullanıcının ID'si

            db.collection("users")
                .document(userId)
                .collection("izinTalebi")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.d("Firestore", "Hiç izin talebi yok.")
                    } else {
                        for (document in documents) {

                            val startDate = document.getString("startDate")
                            val endDate = document.getString("endDate")
                            val timestamp = document.getTimestamp("timestamp")

                            // Eğer timestamp kullanacaksan
                            val timestampDate = timestamp?.toDate() // Timestamp'ı Date nesnesine çevir

                            if (startDate != null && endDate != null) {
                                tarihListesi.add("Talep edilen tarih aralığı: $startDate - $endDate")
                                Log.d("TAG", "fetchIzinTalepleri: $startDate - $endDate")

                            }
                        }
                        recyclerViewAdapter.notifyDataSetChanged() // RecyclerView'u güncelle
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Veri alınamadı: ${exception.message}")
                }
        } else {
            Toast.makeText(this, "Kullanıcı oturumu açmamış", Toast.LENGTH_LONG).show()
        }
    }


}
