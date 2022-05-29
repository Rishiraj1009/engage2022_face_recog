package com.example.engage2022_face_recog

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class RecyclerViewAdapter(context: Context, infoModelArrayList: ArrayList<MissingInfo>) :
    RecyclerView.Adapter<RecyclerViewAdapter.Viewholder>() {
    private val context: Context = context
    private val infoModelArrayList: ArrayList<MissingInfo> = infoModelArrayList

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): Viewholder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_layout, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(@NonNull holder: Viewholder, position: Int) {
        //load data into recycler view components
        val model: MissingInfo = infoModelArrayList[position]
        holder.name.text = model.name
        val urls = model.images!!.split(",")
        Picasso.get()
            .load(urls[0])
            .into(holder.image)
        holder.age.text = "Age:" + model.age.toString()
        holder.date.text = "Missing Since: " + model.date

        //on click on recycler view go to details page of that particular person
        holder.itemView.setOnClickListener {
            val i = Intent(context, DetailActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.putExtra("name", model.name)
            context.startActivity(i)
        }

    }

    inner class Viewholder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val name: TextView = itemView.findViewById(R.id.name)
        val age: TextView = itemView.findViewById(R.id.age)
        val date: TextView = itemView.findViewById(R.id.date)

    }

    override fun getItemCount(): Int {
        return infoModelArrayList.size
    }

}