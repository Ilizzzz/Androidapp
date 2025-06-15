package com.example.yunclass.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yunclass.R;
import com.example.yunclass.model.Website;

import java.util.List;

public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder> {

    private List<Website> websites;
    private Context context;

    public WebsiteAdapter(Context context, List<Website> websites) {
        this.context = context;
        this.websites = websites;
    }

    @NonNull
    @Override
    public WebsiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_website, parent, false);
        return new WebsiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WebsiteViewHolder holder, int position) {
        Website website = websites.get(position);
        holder.nameTextView.setText(website.getName());
        holder.descriptionTextView.setText(website.getDescription());

        // 加载图片
        Glide.with(context)
                .load(website.getImage())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.imageView);

        // 点击事件，打开网站
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website.getUrl()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return websites != null ? websites.size() : 0;
    }

    public void setWebsites(List<Website> websites) {
        this.websites = websites;
        notifyDataSetChanged();
    }

    static class WebsiteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView;
        TextView descriptionTextView;

        public WebsiteViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.websiteImageView);
            nameTextView = itemView.findViewById(R.id.websiteNameTextView);
            descriptionTextView = itemView.findViewById(R.id.websiteDescriptionTextView);
        }
    }
} 