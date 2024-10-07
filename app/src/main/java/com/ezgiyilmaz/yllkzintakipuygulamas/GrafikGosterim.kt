package com.ezgiyilmaz.yllkzintakipuygulamas

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityGrafikGosterimBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GrafikGosterim : AppCompatActivity() {
    private lateinit var binding: ActivityGrafikGosterimBinding
    lateinit var pieChart: PieChart
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityGrafikGosterimBinding.inflate(layoutInflater)
        var view=binding.root
        setContentView(view)

        pieChart=binding.pieChart
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentUser=auth.currentUser
        if(currentUser != null){
            val userId=currentUser.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null) {
                    // Kullanıcıdan kalan izin gününü al
                    val kalanIzin = document.getLong("kalanIzinGünü")?.toInt() ?: 0
                    Log.d("TAG", "Kalan izin: $kalanIzin")

                    // Kullanıcının izin hakkı verilerini almak için izinHakkı dokümanını kontrol et
                    db.collection("users").document(userId).collection("izinTalebi")
                        .document("izinHakkı").get().addOnSuccessListener { izinDocument ->
                            if (izinDocument != null) {
                                // Hak edilen izin ve talep edilen izin verilerini çek
                                val toplamIzin = izinDocument.getLong("hakedilenIzinGunu")?.toInt() ?: 0
                                Log.d("TAG", "Hak edilen izin: $toplamIzin")

                                // İzin talebi verilerini almak için alt koleksiyonu kontrol et
                                db.collection("users").document(userId).collection("izinTalebi")
                                    .get().addOnSuccessListener { talepDocuments ->
                                        var talepEdilenIzin = 0
                                        for (talepDoc in talepDocuments) {
                                            talepEdilenIzin += talepDoc.getLong("talepEdilenIzinGünü")?.toInt() ?: 0
                                        }
                                        Log.d("TAG", "Talep edilen izin: $talepEdilenIzin")

                                        // Verileri grafik üzerinde göster
                                        goster(talepEdilenIzin, toplamIzin, kalanIzin)
                                    }
                            }
                        }
                }
            }

        }
    }
    private fun goster(toplamIzin: Int, talepEdilenIzin: Int, kalanIzin: Int) {
        val pieEntries = arrayListOf<PieEntry>()

        // Pie chart için verileri ekleme
        pieEntries.add(PieEntry(toplamIzin.toFloat(), "Hakedilen İzin"))

        pieEntries.add(PieEntry(talepEdilenIzin.toFloat(), "Talep Edilen İzin"))
        pieEntries.add(PieEntry(kalanIzin.toFloat(), "Kalan İzin"))

        val pieDataSet = PieDataSet(pieEntries, "İzin Durumu")


        val colors = arrayListOf<Int>(
            Color.rgb(76, 175, 80),   // Hakedilen İzin için yeşil
            Color.rgb(255, 193, 7),   // Talep Edilen İzin için sarı
            Color.rgb(244, 67, 54)    // Kalan İzin için kırmızı
        )
        pieDataSet.colors = colors

        val pieData = PieData(pieDataSet)

        pieChart.data = pieData
        pieChart.invalidate()
    }

}