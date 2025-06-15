package com.example.yunclass;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yunclass.adapter.OrderAdapter;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivityOrdersBinding;
import com.example.yunclass.model.Order;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends AppCompatActivity {

    private ActivityOrdersBinding binding;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的订单");
        }

        // 初始化订单列表
        setupOrdersList();

        // 加载订单数据
        loadOrders();
    }

    private void setupOrdersList() {
        adapter = new OrderAdapter(this, new ArrayList<>());
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.ordersRecyclerView.setAdapter(adapter);
    }

    private void loadOrders() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);

        Call<ApiResponse<List<Order>>> call = ApiClient.getApiService().getOrders();
        call.enqueue(new Callback<ApiResponse<List<Order>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Order>>> call, Response<ApiResponse<List<Order>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Order>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getOrders() != null) {
                        List<Order> orders = apiResponse.getOrders();
                        adapter.setOrders(orders);
                        
                        // 显示空视图
                        if (orders.isEmpty()) {
                            binding.emptyView.setVisibility(View.VISIBLE);
                        } else {
                            binding.emptyView.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Toast.makeText(OrdersActivity.this, "获取订单失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Order>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OrdersActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 