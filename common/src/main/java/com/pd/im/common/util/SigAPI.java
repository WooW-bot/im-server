package com.pd.im.common.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import lombok.extern.slf4j.Slf4j;

/**
 * UserSig签名生成和验证工具类
 * 用于IM服务的用户鉴权
 *
 * @author Parker
 * @date 12/9/25
 */
@Slf4j
public class SigAPI {
    private static final int COMPRESS_BUFFER_SIZE = 2048;
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final long appId;
    private final String key;

    public SigAPI(long appId, String key) {
        this.appId = appId;
        this.key = key;
    }

    public static void main(String[] args) {
        SigAPI asd = new SigAPI(10000, "123456");
        String sign = asd.genUserSig("lld", 100000000);
        //        Thread.sleep(2000L);
        JSONObject jsonObject = decodeUserSig(sign);
        System.out.println("sign:" + sign);
        System.out.println("decoder:" + jsonObject.toString());
    }

    /**
     * 解密UserSig
     *
     * @param userSig 待解密的UserSig
     * @return 解密后的JSON对象
     */
    public static JSONObject decodeUserSig(String userSig) {
        JSONObject sigDoc = new JSONObject(true);
        try {
            byte[] decodeUrlByte = Base64URL.base64DecodeUrlNotReplace(userSig.getBytes(StandardCharsets.UTF_8));
            byte[] decompressByte = decompress(decodeUrlByte);
            String decodeText = new String(decompressByte, StandardCharsets.UTF_8);

            if (StringUtils.isNotBlank(decodeText)) {
                sigDoc = JSONObject.parseObject(decodeText);
            }
        } catch (Exception ex) {
            log.error("Failed to decode userSig", ex);
        }
        return sigDoc;
    }

    /**
     * 解压缩数据
     *
     * @param data 待解压缩的数据
     * @return 解压缩后的数据
     */
    public static byte[] decompress(byte[] data) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
                byte[] buffer = new byte[1024];
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    outputStream.write(buffer, 0, count);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("Failed to decompress data", e);
            return data;
        } finally {
            inflater.end();
        }
    }


    /**
     * 生成UserSig鉴权票据
     *
     * @param userid 用户ID
     * @param expire 有效期（秒）
     * @return UserSig字符串
     */
    public String genUserSig(String userid, long expire) {
        return genUserSig(userid, expire, null);
    }

    /**
     * 使用HMAC-SHA256算法生成签名
     *
     * @param identifier    用户标识
     * @param currTime      当前时间戳
     * @param expire        过期时间
     * @param base64Userbuf Base64编码的用户缓冲区
     * @return 签名字符串，失败时返回null
     */
    private String hmacsha256(String identifier, long currTime, long expire, String base64Userbuf) {
        StringBuilder contentToBeSigned = new StringBuilder()
                .append("TLS.identifier:").append(identifier).append("\n")
                .append("TLS.appId:").append(appId).append("\n")
                .append("TLS.time:").append(currTime).append("\n")
                .append("TLS.expire:").append(expire).append("\n");

        if (base64Userbuf != null) {
            contentToBeSigned.append("TLS.userbuf:").append(base64Userbuf).append("\n");
        }

        try {
            byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
            Mac hmac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA256_ALGORITHM);
            hmac.init(keySpec);
            byte[] byteSig = hmac.doFinal(contentToBeSigned.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(byteSig).replaceAll("\\s*", "");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC-SHA256 signature", e);
            return null;
        }
    }

    /**
     * 生成UserSig鉴权票据（使用当前时间戳）
     *
     * @param userid  用户ID
     * @param expire  有效期（秒）
     * @param userbuf 用户缓冲区数据
     * @return UserSig字符串
     */
    private String genUserSig(String userid, long expire, byte[] userbuf) {
        long currTime = System.currentTimeMillis() / 1000;
        return genUserSig(userid, expire, currTime, userbuf);
    }

    /**
     * 生成UserSig鉴权票据（指定时间戳）
     *
     * @param userid  用户ID
     * @param expire  有效期（秒）
     * @param time    时间戳（秒）
     * @param userbuf 用户缓冲区数据
     * @return UserSig字符串
     */
    public String genUserSig(String userid, long expire, long time, byte[] userbuf) {
        JSONObject sigDoc = new JSONObject();
        sigDoc.put("TLS.identifier", userid);
        sigDoc.put("TLS.appId", appId);
        sigDoc.put("TLS.expire", expire);
        sigDoc.put("TLS.time", time);

        String base64UserBuf = null;
        if (userbuf != null) {
            base64UserBuf = Base64.getEncoder().encodeToString(userbuf).replaceAll("\\s*", "");
            sigDoc.put("TLS.userbuf", base64UserBuf);
        }

        String sig = hmacsha256(userid, time, expire, base64UserBuf);
        if (sig == null || sig.isEmpty()) {
            log.error("Failed to generate signature for userid: {}", userid);
            return "";
        }
        sigDoc.put("TLS.sig", sig);

        return compressAndEncode(sigDoc.toString());
    }

    /**
     * 压缩并Base64编码数据
     *
     * @param data 待压缩的字符串
     * @return 压缩并编码后的字符串
     */
    private String compressAndEncode(String data) {
        Deflater compressor = new Deflater();
        try {
            compressor.setInput(data.getBytes(StandardCharsets.UTF_8));
            compressor.finish();

            byte[] compressedBytes = new byte[COMPRESS_BUFFER_SIZE];
            int compressedBytesLength = compressor.deflate(compressedBytes);

            byte[] result = Arrays.copyOfRange(compressedBytes, 0, compressedBytesLength);
            return new String(Base64URL.base64EncodeUrl(result)).replaceAll("\\s*", "");
        } finally {
            compressor.end();
        }
    }
}
