package com.pd.im.common.tools;

import com.pd.im.common.util.EncryptionUtil;

import java.util.Scanner;

/**
 * 应用 SQL 生成工具
 * 生成应用插入 SQL，包含自动生成的 privateKey
 *
 * @author Parker
 * @date 12/26/25
 */
public class AppSqlGenerator {

    /**
     * 重复字符串（Java 8 兼容）
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 生成64位16进制私钥字符串
     */
    private static String generateHexPrivateKey() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[32]; // 32 bytes * 2 hex chars/byte = 64 chars
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║          应用 SQL 生成工具 v1.0                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("说明：");
        System.out.println("  ✓ 输入应用ID和名称");
        System.out.println("  ✓ 输入 Master Secret (回车使用默认值)");
        System.out.println("  ✓ 自动生成 64位 Hex PrivateKey");
        System.out.println("  ✓ 自动生成加密后的 PrivateKey (基于 MasterSecret + AppId)");
        System.out.println("  ✓ 生成 SQL 插入语句");
        System.out.println();


        // 循环生成
        while (true) {
            System.out.println(repeat("─", 60));
            System.out.print("\n请输入应用ID (输入 'q' 退出): ");
            String appIdStr = scanner.nextLine().trim();

            if ("q".equalsIgnoreCase(appIdStr)) {
                System.out.println("\n感谢使用！再见。");
                break;
            }

            Integer appId;
            try {
                appId = Integer.parseInt(appIdStr);
            } catch (NumberFormatException e) {
                System.out.println("✗ 应用ID必须是数字，请重新输入");
                continue;
            }

            if (appId < 10000) {
                System.out.println("✗ 应用ID必须大于等于 10000 (保留ID段)");
                continue;
            }

            System.out.print("请输入应用名称: ");
            String appName = scanner.nextLine().trim();

            if (appName.isEmpty()) {
                System.out.println("✗ 应用名称不能为空，请重新输入");
                continue;
            }

            try {
                // 生成 privateKey (UserSig 签名私钥)
                String privateKey = generateHexPrivateKey();

                // 生成应用专属的 Random Key
                String appKey = EncryptionUtil.generateKey();

                // 使用 AppKey 加密 privateKey
                String encryptedPrivateKey = EncryptionUtil.encrypt(privateKey, appKey);

                // 使用 MasterSecret 保护（Wrapper） AppKey (使用内部硬编码 Secret)
                String encodedAppKey = EncryptionUtil.encodeKey(appKey);
                
                System.out.println("\n✓ 生成成功！");
                System.out.println("┌────────────────────────────────────────────────────────┐");
                System.out.println("│ 应用ID: " + appId);
                System.out.println("│ 应用名称: " + appName);
                System.out.println("│ 明文 PrivateKey: " + privateKey);
                System.out.println("│ App Key (Wrapped): " + encodedAppKey.substring(0, 10) + "...");
                System.out.println("│ (请务必保存好明文 PrivateKey，用于客户端配置)");
                System.out.println("└────────────────────────────────────────────────────────┘");

                // 生成 SQL
                System.out.println("\nSQL 插入语句（复制到数据库执行）:");
                System.out.println("┌────────────────────────────────────────────────────────┐");
                System.out.println("INSERT INTO `im_app` (");
                System.out.println("  `app_id`, `app_name`, `encryption_key`,");
                System.out.println("  `private_key`, `app_status`, `create_time`");
                System.out.println(") VALUES (");
                System.out.println("  " + appId + ",");
                System.out.println("  '" + appName + "',");
                System.out.println("  '" + encodedAppKey + "',");
                System.out.println("  '" + encryptedPrivateKey + "',");
                System.out.println("  1,");
                System.out.println("  UNIX_TIMESTAMP() * 1000");
                System.out.println(");");
                System.out.println("└────────────────────────────────────────────────────────┘");

            } catch (Exception e) {
                System.err.println("\n✗ 生成失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
    }
}
