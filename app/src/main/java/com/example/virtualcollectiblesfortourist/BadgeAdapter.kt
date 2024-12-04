package com.example.virtualcollectiblesfortourist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.virtualcollectiblesfortourist.data.Place

class BadgeAdapter(private val places: List<Place>) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.badge_item, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val place = places[position]
        holder.bind(place)
    }

    override fun getItemCount(): Int = places.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val badgeImage: ImageView = itemView.findViewById(R.id.badgeImage)
        private val badgeTitle: TextView = itemView.findViewById(R.id.badgeTitle)
        private val badgeRarity: TextView = itemView.findViewById(R.id.badgeRarity)
        private val badgeDate: TextView = itemView.findViewById(R.id.badgeDate)

        fun bind(place: Place) {
            badgeTitle.text = place.title
            badgeRarity.text = place.rarity
            badgeDate.text = place.dateOfVisit ?: "Unknown"

            Glide.with(itemView.context)
                .load(place.imageUrl)
                .placeholder(R.drawable.sample_image)
                .into(badgeImage)
        }
    }
}