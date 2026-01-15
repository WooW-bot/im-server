package com.pd.im.common.tools;

import com.pd.im.common.util.SigAPI;

/**
 * UserSign 生成工具
 * 用于生成测试用的 userSign
 */
public class UserSignGenerator {
    
    public static void main(String[] args) {
        // 配置
        long appId = 10001;
        String privateKey = "dc32d51f8d2f957cfc9f1c197f917a54ad207b5dfb30ab9ca5799b5e273a69dd";
        String userId = "10001";
        long expireSeconds = 86400 * 7; // 7天
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║          UserSign 生成工具                             ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("配置信息:");
        System.out.println("  App ID: " + appId);
        System.out.println("  User ID: " + userId);
        System.out.println("  Private Key: " + privateKey);
        System.out.println("  有效期: " + expireSeconds + " 秒 (" + (expireSeconds / 86400) + " 天)");
        System.out.println();
        
        try {
            // 生成 userSign
            SigAPI sigAPI = new SigAPI(appId, privateKey);
            String userSign = sigAPI.genUserSig(userId, expireSeconds);
            
            System.out.println("✓ 生成成功！");
            System.out.println("┌────────────────────────────────────────────────────────┐");
            System.out.println("│ UserSign:");
            System.out.println("│ " + userSign);
            System.out.println("└────────────────────────────────────────────────────────┘");
            System.out.println();
            System.out.println("使用示例:");
            System.out.println("curl -X POST \"http://localhost:8000/v1/user/data/getUserInfo?appId=10001&identifier=10001&userSign=" + userSign + "\" \\");
            System.out.println("  -H \"Content-Type: application/json\" \\");
            System.out.println("  -d '{\"userId\": \"10001\"}'");
            
        } catch (Exception e) {
            System.err.println("✗ 生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
