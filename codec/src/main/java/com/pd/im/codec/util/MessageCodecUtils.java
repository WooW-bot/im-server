package com.pd.im.codec.util;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessageHeader;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.common.enums.MessageType;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 消息编解码工具类
 * <p>
 * 提供消息的编码和解码功能，支持非对称协议：
 * - decode(): 客户端 → 服务端（28字节头 + imei + body）
 * - encode(): 服务端 → 客户端（8字节头 + body）
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class MessageCodecUtils {

    // ==================== 协议常量 ====================

    /**
     * 解码协议头长度（客户端 → 服务端）
     * 7个int字段：command + version + clientType + messageType + appId + imeiLength + bodyLen
     * 7 * 4 = 28 字节
     */
    public static final int DECODE_HEADER_LENGTH = 28;

    /**
     * 编码协议头长度（服务端 → 客户端）
     * 2个int字段：command + bodyLen
     * 2 * 4 = 8 字节
     */
    public static final int ENCODE_HEADER_LENGTH = 8;

    /**
     * 默认协议版本
     */
    public static final int DEFAULT_VERSION = 1;

    // ==================== 解码方法（客户端 → 服务端） ====================

    /**
     * 将 ByteBuf 解码为 Message 对象
     * <p>
     * 协议格式：28字节头 + imei + body
     * - command(4) + version(4) + clientType(4) + messageType(4)
     * - appId(4) + imeiLength(4) + bodyLen(4) + imei(变长) + body(变长)
     * <p>
     * 注意：调用此方法前，调用方应确保 ByteBuf 中有足够的数据
     * 此方法不做长度检查，不做 mark/reset 操作，只负责纯粹的解析
     *
     * @param in ByteBuf 输入流
     * @return Message 对象，解析失败返回 null
     */
    public static Message decode(ByteBuf in) {
        try {
            // 读取协议头（7个int字段，每个4字节）
            int command = in.readInt();
            int version = in.readInt();
            int clientType = in.readInt();
            int messageType = in.readInt();
            int appId = in.readInt();
            int imeiLength = in.readInt();
            int bodyLen = in.readInt();

            // 验证数据长度，避免负数或过大的长度
            if (imeiLength < 0 || bodyLen < 0) {
                log.error("Invalid length: imeiLength={}, bodyLen={}", imeiLength, bodyLen);
                return null;
            }

            // 验证剩余数据是否足够
            if (in.readableBytes() < imeiLength + bodyLen) {
                log.error("Insufficient data: expected {} bytes, but only {} bytes available",
                        imeiLength + bodyLen, in.readableBytes());
                return null;
            }

            // 读取 imei
            byte[] imeiData = new byte[imeiLength];
            in.readBytes(imeiData);
            String imei = new String(imeiData, StandardCharsets.UTF_8);

            // 读取消息体
            byte[] bodyData = new byte[bodyLen];
            in.readBytes(bodyData);

            // 构建消息头
            MessageHeader messageHeader = new MessageHeader();
            messageHeader.setCommand(command);
            messageHeader.setVersion(version);
            messageHeader.setClientType(clientType);
            messageHeader.setMessageType(messageType);
            messageHeader.setAppId(appId);
            messageHeader.setImeiLength(imeiLength);
            messageHeader.setLength(bodyLen);
            messageHeader.setImei(imei);

            // 构建消息对象
            Message message = new Message();
            message.setMessageHeader(messageHeader);

            // 根据消息类型解析消息体
            parseMessageBody(message, messageType, bodyData, imei, bodyLen);

            return message;
        } catch (Exception e) {
            log.error("Failed to decode message", e);
            return null;
        }
    }

    // ==================== 编码方法（服务端 → 客户端） ====================

    /**
     * 将 MessagePack 编码为字节流并写入 ByteBuf
     * <p>
     * 协议格式：8字节头 + body
     * - command(4) + bodyLen(4) + body(变长)
     *
     * @param msg MessagePack 对象
     * @param out 输出 ByteBuf
     */
    public static void encode(MessagePack msg, ByteBuf out) {
        try {
            // 序列化消息体数据（统一使用 JSON 格式）
            String bodyJson = JSONObject.toJSONString(msg.getData());
            byte[] bodyBytes = bodyJson.getBytes(StandardCharsets.UTF_8);

            // 写入超轻量协议头（8字节）
            out.writeInt(msg.getCommand());        // command (4字节)
            out.writeInt(bodyBytes.length);        // bodyLen (4字节)

            // 写入消息体
            out.writeBytes(bodyBytes);

            if (log.isDebugEnabled()) {
                log.debug("Encoded message: command={}, bodyLen={}",
                        msg.getCommand(), bodyBytes.length);
            }
        } catch (Exception e) {
            log.error("Failed to encode message: command={}", msg.getCommand(), e);
            throw new RuntimeException("Message encode failed", e);
        }
    }

    /**
     * 计算编码后的消息长度
     *
     * @param msg MessagePack 对象
     * @return 编码后的字节长度
     */
    public static int calculateEncodedSize(MessagePack msg) {
        try {
            String bodyJson = JSONObject.toJSONString(msg.getData());
            byte[] bodyBytes = bodyJson.getBytes(StandardCharsets.UTF_8);

            // 协议头(8字节) + body长度
            return ENCODE_HEADER_LENGTH + bodyBytes.length;
        } catch (Exception e) {
            log.error("Failed to calculate encoded size", e);
            return 512; // 返回一个默认值
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析消息体
     *
     * @param message     消息对象
     * @param messageType 消息类型
     * @param bodyData    消息体字节数组
     * @param imei        设备标识
     * @param bodyLen     消息体长度
     */
    private static void parseMessageBody(Message message, int messageType, byte[] bodyData, String imei, int bodyLen) {
        if (messageType == MessageType.DATA_TYPE_JSON.getCode()) {
            try {
                String body = new String(bodyData, StandardCharsets.UTF_8);
                JSONObject parse = (JSONObject) JSONObject.parse(body);
                message.setMessagePack(parse);
            } catch (Exception e) {
                log.error("Failed to parse JSON message body, imei={}, bodyLen={}", imei, bodyLen, e);
                // JSON 解析失败，仍然返回消息对象，但 messagePack 为 null
            }
        } else if (messageType == MessageType.DATA_TYPE_PROTOBUF.getCode()) {
            log.warn("ProtoBuf message type is not supported yet, imei={}", imei);
            // TODO: 实现 ProtoBuf 解码
            message.setMessagePack(bodyData);
        } else if (messageType == MessageType.DATA_TYPE_XML.getCode()) {
            log.warn("XML message type is not supported yet, imei={}", imei);
            // TODO: 实现 XML 解码
            String body = new String(bodyData, StandardCharsets.UTF_8);
            message.setMessagePack(body);
        } else {
            log.warn("Unknown message type: {}, imei={}", messageType, imei);
            message.setMessagePack(bodyData);
        }
    }

    private MessageCodecUtils() {
        // 工具类，禁止实例化
    }
}
