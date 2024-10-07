package com.ezgiyilmaz.yllkzintakipuygulamas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewDebug.IntToString
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ActivityIzinBilgiBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class izinBilgi : AppCompatActivity() {
    private lateinit var binding: ActivityIzinBilgiBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: FirebaseStorage
    private val tarihListesi: MutableList<String> = mutableListOf()
    private lateinit var recyclerViewAdapter: TarihAdapter
    val PICK_PDF_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIzinBilgiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance()
        // RecyclerView ve Adapter'ı başlat
        recyclerViewAdapter = TarihAdapter(tarihListesi)

        getDataFromFirestore()

        binding.datePickerTextView.setOnClickListener {
            showDateRangePicker()
        }

        binding.dosyaYukle.setOnClickListener {
            openFileChooser()
        }
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // firebase kaydettiğim bilgileri alıp textlerde gösterdim
    private fun getDataFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid  // Mevcut kullanıcının ID'si
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("FirestoreData", "Veri: ${document.data}")
                    val startDateString = document.getString("datapicker")
                    if (startDateString != null) {
                        val user = Firebase.auth.currentUser
                        user?.let {
                            val name = it.displayName
                            val email = it.email
                            binding.nameText.text = name
                            binding.emailText.text = email
                            binding.izinBilgiText.text = "İşe Başlama Tarihi: $startDateString"
                            // İzin günlerini hesapla
                            val hakedilenIzin = calculateAnnualLeave(startDateString)
                            // binding.izitext.text = "Hak edilen izin günü: $hakedilenIzin"

                            // Hesaplanan izin gününü veritabanına kaydet
                            saveLeaveDaysToFirestore(userId, hakedilenIzin)
                        }

                    } else {
                        Toast.makeText(this, "İşe başlama tarihi bulunamadı", Toast.LENGTH_LONG)
                            .show()
                        Log.e("FirestoreData", "datapicker boş.")
                    }
                } else {
                    Toast.makeText(this, "Belge bulunamadı", Toast.LENGTH_LONG).show()
                    Log.e("FirestoreData", "Belge yok.")
                }
            }.addOnFailureListener { exception ->
                Log.e("FirestoreError", "Veri alınamadı: ${exception.message}")
                Toast.makeText(this, "Veri alınamadı", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Kullanıcı oturumu açmamış", Toast.LENGTH_LONG).show()
        }


    }

    // Hesaplanan izin gününü Firestore'a kaydeden fonksiyon
    private fun saveLeaveDaysToFirestore(userId: String, leaveDays: Int) {
        val leaveData = hashMapOf(
            "hakedilenIzinGunu" to leaveDays,
            "timestamp" to FieldValue.serverTimestamp()  // Kaydedilme zamanı
        )

        db.collection("users").document(userId)
            .collection("izinTalebi").document("izinHakkı")  // Kullanıcıya özel alt koleksiyon
            .set(leaveData)
            .addOnSuccessListener {
                Log.d("Firestore", "Hakedilen izin gün sayısı başarıyla kaydedildi.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Hakedilen izin gün sayısı kaydedilemedi: $e")
            }
    }

    //tarih formatına çevirip çalışma yılına göre izin hesapladım
    private fun calculateAnnualLeave(startDateString: String): Int {
        try {
            //SimpleDateFormat tarih formatı belirler,
            //Locale.getDefault ise cihazın varsayılan tarih ayarına uygun hale getirir
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            //firestore a kaydedilen datapickerı alır ve date nesnesine dönüştürür
            val startDate = sdf.parse(startDateString)
            // güncel tarih
            val currentDate = Calendar.getInstance().time
            //işte geçirilen yılı iki tarih arasındaki farkı  getDifferenceInYears ile buluruz
            val yearsOfService = getDifferenceInYears(startDate, currentDate)
            return when {
                //////////////////farka göre izin hesapla////////////////////////////////
                yearsOfService in 1..5 -> 14
                yearsOfService in 6..15 -> 20
                yearsOfService > 15 -> 26
                else -> 1
            }
            /*
            // Eğer mevcut bir kullanıcı varsa yıllık izin gün sayısını kaydet
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userId = currentUser.uid  // Mevcut kullanıcının ID'si
                val leaveData = hashMapOf(
                    "hakedilenIzin" to yearsOfService,
                )
                db.collection("users").document(userId)
                    .collection("izinTalebi")// Kullanıcıya özel alt koleksiyon
                    .add(leaveData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Yıllık izin bilgileri başarıyla kaydedildi")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Yıllık izin bilgileri kaydedilemedi: $e")
                    }
            }

            return leaveDays


 */
        } catch (e: Exception) {
            Log.e("DateParsing", "Tarih parse edilemedi: ${e.message}")
            return 0
        }
    }


    //iki tarih arası fark hesapalma
    private fun getDifferenceInYears(startDate: Date, currentDate: Date): Int {
        //iki tarih arasındaki farkı yıl olarak alabilmek için 2 tane Calender nesnesi oluşturur
        val startCalendar = Calendar.getInstance()
        val nowCalender = Calendar.getInstance()
        startCalendar.time =
            startDate // kullanıcının işe başlama tarihini startCalender nesnesine atar
        nowCalender.time = currentDate // şu anki tarih

        // fark hesaplama
        val diffYear = nowCalender.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)
        return if (nowCalender.get(Calendar.DAY_OF_YEAR) < startCalendar.get(Calendar.DAY_OF_YEAR)) {
            diffYear - 1
        } else {
            diffYear
        }
    }

    //tarih aralığı seçme
    private fun showDateRangePicker() {
        // Tarih aralığı seçici ayarları
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now()) // Geçmiş tarihleri devre dışı bırakır

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Tarih Aralığı Seç")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        dateRangePicker.show(supportFragmentManager, "date_range_picker")

        // Tarih seçildikten sonra
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val startDateString = sdf.format(Date(startDate))
            val endDateString = sdf.format(Date(endDate))
            val talepEdilenIzinGünü = izinTalebiIcinHesaplama(startDateString, endDateString)

            // binding.izitext.text = "Hak edilen izin günü: $hakedilenIzin"
            //tarihListesi.add("Talep edilen tarih aralığı: $startDateString - $endDateString ")
            //recyclerViewAdapter.notifyDataSetChanged()

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid  // Mevcut kullanıcının ID'si

                // İzin talebi için veriyi oluşturuyoruz
                val izinTalebiData = hashMapOf(
                    "startDate" to startDateString,
                    "endDate" to endDateString,
                    "talepEdilenIzinGünü" to talepEdilenIzinGünü,
                    "timestamp" to FieldValue.serverTimestamp() // Verinin ne zaman oluşturulduğunu kaydetmek için
                )
                //kalanGünHesapla(userId,leaveDays,talepEdilenIzinGünü)

                ///////////////////////////////////////// Firestore'da her kullanıcı için 'izinTalebi' koleksiyonu oluşturulacak
                db.collection("users").document(userId)
                    .collection("izinTalebi")  // Her kullanıcı için 'izinTalebi' alt koleksiyonu oluşturulur
                    .add(izinTalebiData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(
                            "Firestore",
                            "İzin talebi başarıyla kaydedildi: ${documentReference.id}"
                        )
                        Toast.makeText(this, "İzin talebi kaydedildi", Toast.LENGTH_LONG).show()
                        //kalanGünHesapla(userId,hakedilenIzin,talepEdilenIzinGünü)
                        getHakedilenIzinFromFirestore()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "İzin talebi kaydedilirken hata oluştu: $e")
                        Toast.makeText(this, "İzin talebi kaydedilemedi", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Kullanıcı oturumu açmamış", Toast.LENGTH_LONG).show()
            }
            binding.datePickerTextView.text =
                "Seçilen tarih aralığı: $startDateString - $endDateString"

        }
    }

    private fun izinTalebiIcinHesaplama(startDateString: String, end: String): Int {
        return try {
            // Tarih formatını belirliyoruz
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            // Tarihleri parse ediyoruz
            val startDate = sdf.parse(startDateString)
            val endDate = sdf.parse(end)

            // Null kontrolü
            if (startDate != null && endDate != null) {
                // İki tarih arasındaki gün farkını hesaplayan fonksiyon
                val dayOfService = tarihlerArasiHesaplama(startDate, endDate)
                binding.textView8.text = "Talep edilen izin gün sayısı: $dayOfService"

                if (dayOfService == 0) {
                    Toast.makeText(
                        this,
                        "İzin Talebinde bulunabilmeniz için en az 1 yıl çalışmanız gerekmektedir.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                dayOfService // Fonksiyonun dönüş değeri
            } else {
                // Tarih parse edilemezse 0 döndür
                0
            }
        } catch (e: Exception) {
            Log.e("DateParsing", "Tarih parse edilemedi: ${e.message}")
            0 // Hata durumunda da 0 döndür
        }
    }


    private fun tarihlerArasiHesaplama(startDate: Date, enDate: Date): Int {
        //iki tarih arasındaki farkı gün olarak alabilmek için 2 tane Calender nesnesi oluşturur
        val startCalendar = Calendar.getInstance()
        val endCalender = Calendar.getInstance()
        startCalendar.time =
            startDate // kullanıcının işe başlama tarihini startCalender nesnesine atar
        endCalender.time = enDate // şu anki tarih

        // fark hesaplama
        val diffYear =
            endCalender.get(Calendar.DAY_OF_YEAR) - startCalendar.get(Calendar.DAY_OF_YEAR)
        return if (endCalender.get(Calendar.DAY_OF_YEAR) < startCalendar.get(Calendar.DAY_OF_YEAR)) {
            diffYear - 1
        } else {
            diffYear
        }


    }

    fun izinTalepOnClick(view: View) {
        val user = auth.currentUser
        if (user != null) {
            /*
            val izinTarihSec=binding.datePickerTextView.text.toString()
            val userId=intent.getIntExtra("USER_ID",-1)

            val izinList= mapOf(
                "id" to userId,
                "izinTarihSec" to izinTarihSec
            )

             */
        }
    }


    private fun getHakedilenIzinFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid  // Mevcut kullanıcının ID'si
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            val izinTalebiRef = userRef.collection("izinTalebi")

            // Kullanıcının izin talebi koleksiyonuna erişiyoruz
            db.collection("users").document(userId)
                .collection("izinTalebi").document("izinHakkı")
                // Sadece en güncel talebi almak için
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        // Hakedilen izin gününü kontrol et
                        val hakedilenIzinGunu = document.getLong("hakedilenIzinGunu")?.toInt() ?: 0
                        Log.d("FirestoreData", "Hakedilen izin günü: $hakedilenIzinGunu")

                        // Talep edilen izin gününü kontrol et
                        izinTalebiRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val document = documents.first() // İlk belge (en son eklenen)
                                    val talepEdilenIzinGunu =
                                        document.getLong("talepEdilenIzinGünü")?.toInt() ?: 0
                                    recyclerViewAdapter.notifyDataSetChanged()
                                    Log.d(
                                        "FirestoreData",
                                        "Talep edilen izin günü: $talepEdilenIzinGunu"
                                    )


                                    // Kalan izin gününü hesaplayın
                                    val kalanIzinHakki = hakedilenIzinGunu - talepEdilenIzinGunu
                                    Log.d("FirestoreData", "Kalan izin hakkı: $kalanIzinHakki")
                                    kalanGünHesapla(userId, kalanIzinHakki)

                                    // Ekranda göster
                                    binding.textView.text = "Kalan izin günü: $kalanIzinHakki"
                                    binding.izitext.text =
                                        "Hak edilen izin günü: $hakedilenIzinGunu"

                                } else {
                                    Toast.makeText(
                                        this,
                                        "İzin talebi bulunamadı",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.e("FirestoreData", "İzin talebi boş.")
                                }
                            }
                    }
                }
        }

    }

    private fun kalanGünHesapla(userId: String, kalanIzin: Int) {
        // Kalan izin hakkını Firestore'a kaydetmek için
        val kalanIzinData: Map<String, Any> = hashMapOf(
            "kalanIzinGünü" to kalanIzin
        ) // Kalan izin hakkını burada ekliyoruz
        db.collection("users").document(userId)
            .update(kalanIzinData)
            .addOnSuccessListener {
                Log.d("Firestore", "Kalan izin hakkı başarıyla güncellendi.")
                Toast.makeText(this, "Kalan izin hakkı güncellendi", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Kalan izin hakkı güncellenirken hata oluştu: $e")
                Toast.makeText(this, "Kalan izin hakkı güncellenemedi", Toast.LENGTH_LONG).show()
            }

    }

    /*
    private fun guncelleHakedilenIzinHakki2(userId: String, yeniKalanIzinHakki: Long) {
        // Kullanıcının izin hakkı belgesine erişim
        val izinHakkıRef = db.collection("users").document(userId)
            .collection("izinTalebi").document("izinHakkı")

        // Güncellenmesi gereken alanı belirtiyoruz
        izinHakkıRef.update("hakedilenIzinHunu", yeniKalanIzinHakki)

            .addOnSuccessListener {
                Log.d("TAG", "Hakedilen izin hakkı başarıyla güncellendi!")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Belgeyi güncellerken hata oluştu", e)
            }
    }

    fun guncelleKalanIzinGunu(userId: String, talepEdilenIzinGunu: Int) {
        // Kullanıcının izin talebi koleksiyonuna erişiyoruz
        db.collection("users").document(userId)
            .collection("izinTalebi").document("izinHakkı")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Daha önce kalan izin gününü al
                    val kalanIzinGunu = document.getLong("kalanIzinGunu")?.toInt() ?: 0
                    Log.d("FirestoreData", "Kalan izin günü: $kalanIzinGunu")

                    // Yeni kalan izin gününü hesapla
                    val yeniKalanIzinGunu = kalanIzinGunu - talepEdilenIzinGunu
                    Log.d("FirestoreData", "Yeni kalan izin günü: $yeniKalanIzinGunu")

                    // Yeni kalan izin gününü veritabanına kaydet
                    db.collection("users").document(userId)
                        .collection("izinTalebi").document("izinHakkı")
                        .update("kalanIzinGunu", yeniKalanIzinGunu)
                        .addOnSuccessListener {
                            Log.d("FirestoreData", "Yeni kalan izin günü başarıyla güncellendi.")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FirestoreData", "Yeni kalan izin günü güncellenirken hata oluştu", e)
                        }
                } else {
                    Log.w("FirestoreData", "Belge bulunamadı.")
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreData", "Belge alınırken hata oluştu", e)
            }
    }


 */
    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf" // Sadece PDF dosyaları için
        Log.d("TAG", "Dosya seçici açılıyor")
        startActivityForResult(intent, PICK_PDF_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val fileUri = data.data // Dosyanın URI'sini al
            Log.d("TAG", "Dosya seçildi: $fileUri")
            uploadFileToFirebase(fileUri)
        } else {
            Log.d("TAG", "Dosya seçimi başarısız")
        }
    }

    private fun uploadFileToFirebase(fileUri: Uri?) {
        if (fileUri != null) {
            val storageReference = FirebaseStorage.getInstance()
                .getReference("uploads/${System.currentTimeMillis()}.pdf")
            Log.d("TAG", "Dosya yükleme başlıyor: $fileUri")

            storageReference.putFile(fileUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Dosya başarıyla yüklendiğinde yapılacak işlemler
                    Log.d("TAG", "Dosya başarıyla yüklendi!")
                    Toast.makeText(this, "Dosya yüklendi!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Hata olduğunda yapılacak işlemler
                    Log.e("TAG", "Yükleme hatası: ${e.message}", e)
                    Toast.makeText(this, "Yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("FirebaseUpload", "Dosya URI boş, yükleme yapılmadı")
        }
    }
}
