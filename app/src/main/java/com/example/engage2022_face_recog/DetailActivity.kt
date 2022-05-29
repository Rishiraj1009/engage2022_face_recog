package com.example.engage2022_face_recog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.squareup.picasso.Picasso


class DetailActivity : AppCompatActivity() {

    private lateinit var dbReference: DatabaseReference
    private lateinit var image: ImageView
    private lateinit var name: TextView
    private lateinit var age: TextView
    private lateinit var gender: TextView
    private lateinit var contact: TextView
    private lateinit var date: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val n:String = intent.getStringExtra("name").toString()

        image = findViewById(R.id.image)
        name = findViewById(R.id.name)
        age = findViewById(R.id.age)
        gender = findViewById(R.id.gender)
        contact = findViewById(R.id.contact)
        date = findViewById(R.id.dates)

        //intent to launch the phone app with the contact number
        contact.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:"+contact.text)
            startActivity(intent)
        }

        //Fetches data from the realtime database and displays the data for the corresponding person
        dbReference = FirebaseDatabase.getInstance().getReference("1MPU39ZefbUAoYX-k_ethP-0CKpqaFRETiOHwdooFo_0/missing")
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (noteSnapshot in dataSnapshot.children) {
                    val note: MissingInfo? = noteSnapshot.getValue(MissingInfo::class.java)
                    if (note!!.name == n) {
                        val urls = note.images!!.split(",")
                        Picasso.get().load(urls[0]).into(image)
                        name.text = note.name
                        age.text = note.age.toString()
                        gender.text = note.gender
                        contact.text = note.contact.toString()
                        date.text = note.date
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // ...
            }
        })
    }
}