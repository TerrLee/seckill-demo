package com.example.seckilldemo.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/ai")
public class AIController {
    private final ObjectMapper objectMapper;

    public AIController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping(value="/chat",produces ="text/event-stream;charset=UTF-8")
    public SseEmitter chatWithPython(@RequestBody Map<String,String> request){
        String prompt = request.get("prompt");

        //1,创建SssEmitter，超时时间设置长一点（比如3分钟），因为大模型有时候吐字慢
        SseEmitter emitter = new SseEmitter(180000L);
        //2.异步执行，防止阻塞tomcat的主线程


        CompletableFuture.runAsync(()->{
            try {
                //构建请求发给python端
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(java.time.Duration.ofSeconds(10)) // 🌟 连接超时
                        .build();

                //用jackson的objectMapper
                //核心改造：使用Map+ObjectMapper来构建JSON
                Map<String,Object> bodyMap = new HashMap<>();
                bodyMap.put("prompt",prompt);
                bodyMap.put("model","qwen2.5:1.5b");
                bodyMap.put("temperature",0.7);
                //自动序列化成合法的JSON字符串，无论prompt里有什么引号都不怕

                String jsonBody = objectMapper.writeValueAsString(bodyMap);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:8000/api/chat/stream"))//确保python已经改成流式
                        .header("Content-Type","application/json")
                        .timeout(java.time.Duration.ofSeconds(120))//请求超时，防止死等
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();
                //3.发送请求并流式读取响应
                HttpResponse<java.io.InputStream>response =client.send(
                        httpRequest,
                        HttpResponse.BodyHandlers.ofInputStream()
                );
                //4.逐行读取Python吐出来的数据，再转发给前端
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body(),StandardCharsets.UTF_8)
                );
                String line;
                while((line = reader.readLine())!=null){
                    if(line.startsWith("data:")){
                        String text = line.substring(5).trim();
                        if(!text.isEmpty()){
                            emitter.send(SseEmitter.event().data(text));
                        }
                    }
                }


            }catch(Exception e){
                emitter.completeWithError(e);
            }

        });
        return emitter;
    }
}
