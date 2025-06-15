package com.example.yunclass.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yunclass.R;

public class CustomerServiceDialog {
    private Context context;
    private AlertDialog dialog;
    private TextView tvChatHistory;
    private EditText etUserInput;
    private Button btnSend;
    private Button btnRetry;
    private DeepSeekAIClient aiClient;
    private boolean isServiceAvailable = true;
    
    public CustomerServiceDialog(Context context) {
        this.context = context;
        aiClient = new DeepSeekAIClient();
    }
    
    public void show() {
        try {
            // 创建对话框视图
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_customer_service, null);
            tvChatHistory = dialogView.findViewById(R.id.tvChatHistory);
            etUserInput = dialogView.findViewById(R.id.etUserInput);
            btnSend = dialogView.findViewById(R.id.btnSend);
            btnRetry = dialogView.findViewById(R.id.btnRetry);
            
            if (btnRetry != null) {
                btnRetry.setVisibility(View.GONE);
                btnRetry.setOnClickListener(v -> {
                    // 重试连接
                    btnRetry.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    etUserInput.setEnabled(true);
                    isServiceAvailable = true;
                    appendToChat("系统", "正在重新连接DeepSeek客服服务...");
                    new CheckServiceTask().execute();
                });
            }
            
            // 使聊天记录可滚动
            tvChatHistory.setMovementMethod(new ScrollingMovementMethod());
            
            // 添加初始欢迎消息
            appendToChat("客服", "您好！我是基于DeepSeek AI的云课程顾问，很高兴为您服务。请问有什么课程需求我可以帮助您？");
            
            // 创建对话框
            dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setTitle("DeepSeek AI客服")
                    .setPositiveButton("结束对话", (dialog, which) -> {
                        // 关闭对话框并清空对话历史
                        aiClient.resetConversation();
                    })
                    .setNegativeButton("取消", null)
                    .create();
            
            // 设置发送按钮点击事件
            btnSend.setOnClickListener(v -> {
                String userMessage = etUserInput.getText().toString().trim();
                if (userMessage.isEmpty()) {
                    Toast.makeText(context, "请输入您的问题", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 显示用户消息
                appendToChat("我", userMessage);
                
                // 清空输入框
                etUserInput.setText("");
                
                // 显示加载中消息
                appendToChat("客服", "正在思考中...");
                
                // 使用DeepSeek AI客服
                new AIResponseTask().execute(userMessage);
            });
            
            // 显示对话框
            dialog.show();
        } catch (Exception e) {
            // 捕获任何异常，确保对话框能够显示
            Toast.makeText(context, "客服服务初始化失败，请稍后再试", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    /**
     * 向聊天记录添加消息
     */
    private void appendToChat(String speaker, String message) {
        try {
            String formattedMessage = "<b>" + speaker + ":</b> " + message + "<br/><br/>";
            
            // 如果是加载中消息，替换最后一条消息
            if (message.equals("正在思考中...") && tvChatHistory.getText().toString().contains("客服: 正在思考中...")) {
                String currentChat = tvChatHistory.getText().toString();
                currentChat = currentChat.replace("客服: 正在思考中...<br/><br/>", "");
                tvChatHistory.setText(Html.fromHtml(currentChat + formattedMessage));
            } else {
                tvChatHistory.append(Html.fromHtml(formattedMessage));
            }
            
            // 滚动到底部
            final int scrollAmount = tvChatHistory.getLayout().getLineTop(tvChatHistory.getLineCount()) - tvChatHistory.getHeight();
            if (scrollAmount > 0) {
                tvChatHistory.scrollTo(0, scrollAmount);
            } else {
                tvChatHistory.scrollTo(0, 0);
            }
        } catch (Exception e) {
            // 捕获异常，但不中断对话
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
                // 检查在线服务可用性
                return aiClient.sendMessage("您好，我想测试连接");
            } catch (Exception e) {
                e.printStackTrace();
                return "连接DeepSeek服务失败，请检查网络后重试。";
            }
        }
        
        @Override
        protected void onPostExecute(String response) {
            try {
                // 移除"正在思考中..."或"正在尝试重新连接客服服务..."消息
                String currentChat = tvChatHistory.getText().toString();
                currentChat = currentChat.replace("客服: 正在思考中...<br/><br/>", "");
                currentChat = currentChat.replace("系统: 正在重新连接DeepSeek客服服务...<br/><br/>", "");
                tvChatHistory.setText(Html.fromHtml(currentChat));
                
                // 检查响应是否包含错误信息
                if (response.contains("连接DeepSeek服务失败") || response.contains("网络连接异常")) {
                    isServiceAvailable = false;
                    appendToChat("系统", response);
                    
                    if (btnRetry != null) {
                        btnRetry.setVisibility(View.VISIBLE);
                    }
                } else {
                    isServiceAvailable = true;
                    // 将测试连接的回复显示为正常的客服消息
                    appendToChat("客服", "连接已恢复。我是DeepSeek AI智能客服助手，很高兴为您提供帮助。请问有什么问题我可以解答？");
                    
                    if (btnRetry != null) {
                        btnRetry.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 发送消息给AI并获取回复
     */
    private class AIResponseTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            // 禁用发送按钮，防止重复发送
            btnSend.setEnabled(false);
        }
        
        @Override
        protected String doInBackground(String... strings) {
            try {
                // 检查服务是否可用
                if (!isServiceAvailable) {
                    return "客服服务当前不可用，请点击重试或稍后再试。";
                }
                
                // 通过AI客户端发送消息
                return aiClient.sendMessage(strings[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return "抱歉，客服服务暂时无法使用，请稍后再试。";
            }
        }
        
        @Override
        protected void onPostExecute(String response) {
            // 重新启用发送按钮
            btnSend.setEnabled(true);
            
            try {
                // 删除"正在思考中..."
                String currentChat = tvChatHistory.getText().toString();
                currentChat = currentChat.replace("客服: 正在思考中...<br/><br/>", "");
                tvChatHistory.setText(Html.fromHtml(currentChat));
                
                // 检查是否是错误消息
                if (response.contains("无法使用") || response.contains("不可用") || response.contains("失败")) {
                    isServiceAvailable = false;
                    appendToChat("系统", response);
                    
                    if (btnRetry != null) {
                        btnRetry.setVisibility(View.VISIBLE);
                    }
                } else {
                    // 显示AI回复
                    appendToChat("客服", response);
                }
            } catch (Exception e) {
                e.printStackTrace();
                appendToChat("系统", "显示回复时出错，请重试。");
            }
        }
    }
} 