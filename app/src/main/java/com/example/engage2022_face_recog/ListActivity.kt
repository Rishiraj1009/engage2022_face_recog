package com.example.engage2022_face_recog

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.View.DRAWING_CACHE_QUALITY_HIGH
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*


class ListActivity : AppCompatActivity() {

    private lateinit var dbReference: DatabaseReference
    private lateinit var recycler: RecyclerView
    private lateinit var searchField: TextInputLayout
    private lateinit var results: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        recycler = findViewById(R.id.rv)
        searchField = findViewById(R.id.searchField)
        results = findViewById(R.id.results)

        (searchField.editText)!!.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) searchField.hint = ""
        }

        val listMissing: ArrayList<MissingInfo> = ArrayList()
        val searchMissing: ArrayList<MissingInfo> = ArrayList()
        dbReference = FirebaseDatabase.getInstance()
            .getReference("1MPU39ZefbUAoYX-k_ethP-0CKpqaFRETiOHwdooFo_0/missing")

        val adapter = RecyclerViewAdapter(this, listMissing)
        val searchAdapter = RecyclerViewAdapter(this, searchMissing)
        recycler.adapter = adapter


        //get data from the realtime database and display it in a recyclerview
        dbReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (noteSnapshot in dataSnapshot.children) {
                    val note: MissingInfo? = noteSnapshot.getValue(MissingInfo::class.java)
                    listMissing.add(note!!)
                }

                //notify adpater about the change/addition of items to list
                adapter.notifyDataSetChanged();
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // ...
            }
        })

        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.isNestedScrollingEnabled = false

        //search functionality
        (searchField.editText)!!.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode === KeyEvent.KEYCODE_ENTER || actionId === EditorInfo.IME_ACTION_SEARCH) {
                val search = searchField.editText!!.text.toString()

                //check if the search query is empty
                if (search == "") {
                    Toast.makeText(this, "Search Field Cannot Be Empty", Toast.LENGTH_LONG).show()
                }
                else {
                    //load data into new list with search result
                    searchMissing.clear()
                    results.visibility = View.GONE
                    for(missing in listMissing)
                        for( query in missing.name!!.split(" "))
                            if( search.equals(query, ignoreCase = true))
                                searchMissing.add(missing)
                    recycler.adapter = searchAdapter
                    searchAdapter.notifyDataSetChanged()
                    if(searchMissing.size==0)
                        results.visibility = View.VISIBLE
                    val imm: InputMethodManager =
                        (this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    imm.hideSoftInputFromWindow(searchField.editText!!.windowToken, 0)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }
}