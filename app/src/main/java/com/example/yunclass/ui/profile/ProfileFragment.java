package com.example.yunclass.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yunclass.LoginActivity;
import com.example.yunclass.MyCoursesActivity;
import com.example.yunclass.MyQuestionsActivity;
import com.example.yunclass.OrdersActivity;
import com.example.yunclass.SettingsActivity;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.FragmentProfileBinding;
import com.example.yunclass.model.Account;
import com.example.yunclass.model.User;
import com.example.yunclass.utils.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        
        // 显示用户信息
        displayUserInfo();
        
        // 加载账户信息
        loadAccountInfo();
        
        // 设置点击事件
        setupClickListeners();
    }

    private void displayUserInfo() {
        User user = sessionManager.getUserDetails();
        if (user != null) {
            binding.userNameTextView.setText(user.getName());
            binding.userEmailTextView.setText(user.getEmail());
        }
    }

    private void loadAccountInfo() {
        Call<ApiResponse<Account>> call = ApiClient.getApiService().getAccount();
        call.enqueue(new Callback<ApiResponse<Account>>() {
            @Override
            public void onResponse(Call<ApiResponse<Account>> call, Response<ApiResponse<Account>> response) {
                if (isAdded()) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Account> apiResponse = response.body();
                        if (apiResponse.isSuccess() && apiResponse.getAccount() != null) {
                            Account account = apiResponse.getAccount();
                            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CHINA);
                            binding.balanceTextView.setText(format.format(account.getBalance()));
                        }
                    } else {
                        Toast.makeText(requireContext(), "获取账户信息失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Account>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        // 我的订单
        binding.ordersCardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrdersActivity.class);
            startActivity(intent);
        });

        // 我的课程
        binding.myCoursesCardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyCoursesActivity.class);
            startActivity(intent);
        });

        // 我的问题
        binding.myQuestionsCardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyQuestionsActivity.class);
            startActivity(intent);
        });

        // 设置
        binding.settingsCardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // 退出登录
        binding.logoutButton.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 