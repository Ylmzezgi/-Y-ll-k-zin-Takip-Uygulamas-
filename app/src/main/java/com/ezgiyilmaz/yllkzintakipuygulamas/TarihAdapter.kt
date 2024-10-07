package com.ezgiyilmaz.yllkzintakipuygulamas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezgiyilmaz.yllkzintakipuygulamas.databinding.ReclerRowBinding

class TarihAdapter(private val tarihListesi : List<String>):RecyclerView.Adapter<TarihAdapter.TarihViewHolder>(){

   class TarihViewHolder(val binding: ReclerRowBinding): RecyclerView.ViewHolder(binding.root){

   }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarihViewHolder {
        val binding =ReclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TarihViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TarihViewHolder, position: Int) {
        holder.binding.tarihText.text=tarihListesi[position]
    }

    override fun getItemCount(): Int {
        return tarihListesi.size
    }
}