package com.lin.rin1aiagent.demo.invoke;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class HttpAiInvoke {
    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (StrUtil.isBlank(apiKey)) {
            throw new IllegalStateException("请先设置环境变量 DASHSCOPE_API_KEY");
        }

        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        JSONArray messages = JSONUtil.createArray();
        messages.put(JSONUtil.createObj()
                .set("role", "system")
                .set("content", "You are a helpful assistant."));
        messages.put(JSONUtil.createObj()
                .set("role", "user")
                .set("content", "你是谁？"));

        JSONObject body = JSONUtil.createObj()
                .set("model", "qwen-plus")
                .set("input", JSONUtil.createObj().set("messages", messages))
                .set("parameters", JSONUtil.createObj().set("result_format", "message"));

        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(30_000)
                .execute()) {

            System.out.println("HTTP状态码: " + response.getStatus());
            System.out.println("响应体: " + response.body());
        }
    }
}