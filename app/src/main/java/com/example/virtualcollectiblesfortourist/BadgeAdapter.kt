package com.example.virtualcollectiblesfortourist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
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
        private val badgeRibbon: TextView = itemView.findViewById(R.id.badgeRibbon)
        private val badgeImage: ImageView = itemView.findViewById(R.id.badgeImage)
        private val badgeDate: TextView = itemView.findViewById(R.id.badgeDate)
        private val badgeTitle: TextView = itemView.findViewById(R.id.badgeTitle)

        fun bind(place: Place) {
            badgeTitle.text = place.title
            badgeDate.text = place.dateOfVisit ?: "Unknown"

            val rarityColor = when (place.rarity) {
                "legendary" -> R.color.legendary
                "epic" -> R.color.epic
                "rare" -> R.color.rare
                "common" -> R.color.common
                else -> R.color.light_gray
            }
            badgeRibbon.setBackgroundColor(itemView.context.getColor(rarityColor))

            Glide.with(itemView.context)
                .load(place.imageUrl)
                .placeholder(R.drawable.sample_image)
                .transform(CircleCrop())
                .into(badgeImage)
        }
    }
}