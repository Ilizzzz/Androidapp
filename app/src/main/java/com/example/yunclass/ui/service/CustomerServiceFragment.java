package com.example.yunclass.ui.service;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yunclass.R;
import com.example.yunclass.utils.AppConfig;
import com.example.yunclass.utils.DeepSeekAIClient;

public class CustomerServiceFragment extends Fragment {

    private TextView tvChatHistory;
    private EditText etUserInput;
    private Button btnSend;
    private Button btnRetry;
    private ProgressBar progressBar;
    
    private DeepSeekAIClient aiClient;
    private boolean isServiceAvailable = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化AppConfig，确保能够获取API配置
        try {
            AppConfig.init(requireContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 初始化AI客户端
        aiClient = new DeepSeekAIClient();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_service, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        tvChatHistory = view.findViewById(R.id.tvChatHistory);
        etUserInput = view.findViewById(R.id.etUserInput);
        btnSend = view.findViewById(R.id.btnSend);
        btnRetry = view.findViewById(R.id.btnRetry);
        progressBar = view.findViewById(R.id.progressBar);
        
        // 设置聊天记录可滚动
        tvChatHistory.setMovementMethod(new ScrollingMovementMethod());
        
        // 设置重试按钮
        btnRetry.setOnClickListener(v -> {
            btnRetry.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            etUserInput.setEnabled(false);
            btnSend.setEnabled(false);
            
            // 检查服务是否可用
            new CheckServiceTask().execute();
        });
        
        // 设置发送按钮
        btnSend.setOnClickListener(v -> {
            String userMessage = etUserInput.getText().toString().trim();
            if (userMessage.isEmpty()) {
                Toast.makeText(requireContext(), "请输入您的问题", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 显示用户消息
            appendToChat("我", userMessage);
            
            // 清空输入框
            etUserInput.setText("");
            
            // 显示进度条
            progressBar.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);
            
            // 发送消息到AI
            new AIResponseTask().execute(userMessage);
        });
        
        // 显示欢迎消息
        appendToChat("客服", "您好！我是基于DeepSeek AI的云课程顾问，很高兴为您服务。请问有什么课程需求我可以帮助您？");
    }
    
    /**
     * 向聊天记录添加消息
     */
    private void appendToChat(String speaker, String message) {
        try {
            String formattedMessage = "<b>" + speaker + ":</b> " + message + "<br/><br/>";
            tvChatHistory.append(Html.fromHtml(formattedMessage));
            
            // 滚动到底部
            final int scrollAmount = tvChatHistory.getLayout().getLineTop(tvChatHistory.getLineCount()) - tvChatHistory.getHeight();
            if (scrollAmount > 0) {
                tvChatHistory.scrollTo(0, scrollAmount);
            } else {
                tvChatHistory.scrollTo(0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 检查服务是否可用
     */
    private class CheckServiceTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return aiClient.sendMessage("您好，我想测试连接");
            } catch (Exception e) {
                e.printStackTrace();
                return "连接DeepSeek服务失败，请检查网络后重试。";
            }
        }
        
        @Override
        protected void onPostExecute(String response) {
            if (getContext() == null) return; // Fragment可能已被销毁
            
            progressBar.setVisibility(View.GONE);
            
            if (response.contains("连接DeepSeek服务失败") || response.contains("网络连接异常")) {
                isServiceAvailable = false;
                appendToChat("系统", response);
                btnRetry.setVisibility(View.VISIBLE);
            } else {
                isServiceAvailable = true;
                appendToChat("客服", "连接成功。我是DeepSeek AI智能客服助手，很高兴为您提供帮助。请问有什么问题我可以解答？");
                btnRetry.setVisibility(View.GONE);
                etUserInput.setEnabled(true);
                btnSend.setEnabled(true);
            }
        }
    }
    
    /**
     * 发送消息给AI并获取回复
     */
    private class AIResponseTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                if (!isServiceAvailable) {
                    return "客服服务当前不可用，请点击重试按钮重新连接。";
                }
                
                return aiClient.sendMessage(strings[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return "抱歉，客服服务暂时无法使用，请稍后再试。";
            }
        }
        
        @Override
        protected void onPostExecute(String response) {
            if (getContext() == null) return; // Fragment可能已被销毁
            
            progressBar.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            
            if (response.contains("无法使用") || response.contains("不可用") || response.contains("失败")) {
                isServiceAvailable = false;
                appendToChat("系统", response);
                btnRetry.setVisibility(View.VISIBLE);
            } else {
                appendToChat("客服", response);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 重置对话
        if (aiClient != null) {
            aiClient.resetConversation();
        }
    }
} 