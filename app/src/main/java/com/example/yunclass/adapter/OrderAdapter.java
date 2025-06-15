package com.example.yunclass.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yunclass.R;
import com.example.yunclass.model.Order;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orders;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;
    private static final String TAG = "OrderAdapter";

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        
        holder.titleTextView.setText(order.getCourseTitle());
        holder.idTextView.setText(String.valueOf(order.getId()));
        
        if (order.getCreatedAt() != null) {
            Log.d(TAG, "Order date: " + order.getCreatedAt());
            holder.timeTextView.setText(dateFormat.format(order.getCreatedAt()));
        } else {
            // 如果日期为空，使用当前时间
            Log.d(TAG, "Order date is null, using current time");
            Date currentDate = new Date();
            holder.timeTextView.setText(dateFormat.format(currentDate));
        }
        
        holder.priceTextView.setText(currencyFormat.format(order.getPrice()));
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView idTextView;
        TextView timeTextView;
        TextView priceTextView;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.orderTitleTextView);
            idTextView = itemView.findViewById(R.id.orderIdTextView);
            timeTextView = itemView.findViewById(R.id.orderTimeTextView);
            priceTextView = itemView.findViewById(R.id.orderPriceTextView);
        }
    }
} 