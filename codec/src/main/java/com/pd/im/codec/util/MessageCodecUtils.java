package com.pd.im.codec.util;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.MessageLite;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessageHeader;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.enums.command.SystemCommand;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.MessageType;
import com.pd.im.common.utils.JsonUtils;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

/**
 * 消息编解码工具类
 * <p>
 * 提供消息的编码和解码功能，支持非对称协议： - decode(): 客户端 → 服务端（28字节头 + imei + body） - encode(): 服务端 → 客户端（8字节头 +
 * body）
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class MessageCodecUtils {

  // ==================== 协议常量 ====================

  /**
   * 解码协议头长度（客户端 → 服务端） 7个int字段：command + version + clientType + appId + messageType + imeiLength +
   * bodyLen 7 * 4 = 28 字节
   */
  public static final int DECODE_HEADER_LENGTH = 28;

  /**
   * 编码协议头长度（服务端 → 客户端） 2个int字段：command + bodyLen 2 * 4 = 8 字节
   */
  public static final int ENCODE_HEADER_LENGTH = 8;

  /**
   * 默认协议版本
   */
  public static final int DEFAULT_VERSION = 1;

  /**
   * 最大 IMEI 长度 (防护 OOM)
   */
  public static final int MAX_IMEI_LENGTH = 256;

  /**
   * 最大消息体长度 (防护 OOM，1MB)
   */
  public static final int MAX_BODY_LENGTH = 1024 * 1024;

  // ==================== 缓存容器 ====================

  /**
   * 类名 -> Class 对象缓存
   */
  private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

  /**
   * 类名 -> parseFrom 方法缓存
   */
  private static final Map<String, Method> PARSE_METHOD_CACHE = new ConcurrentHashMap<>();

  // ==================== 解码方法（客户端 → 服务端） ====================

  /**
   * 将 ByteBuf 解码为 Message 对象
   * <p>
   * 协议格式：28字节头 + imei + body - command(4) + version(4) + clientType(4) + messageType(4) - appId(4)
   * + imeiLength(4) + bodyLen(4) + imei(变长) + body(变长)
   * <p>
   * 注意：调用此方法前，调用方应确保 ByteBuf 中有足够的数据 此方法不做长度检查，不做 mark/reset 操作，只负责纯粹的解析
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
      int appId = in.readInt();
      int messageType = in.readInt();
      int imeiLength = in.readInt();
      int bodyLen = in.readInt();

      // 1. 验证数据长度，避免负数或过大的长度 (防护 OOM)
      if (imeiLength < 0 || imeiLength > MAX_IMEI_LENGTH || bodyLen < 0
          || bodyLen > MAX_BODY_LENGTH) {
        String errorMsg = String.format("Invalid protocol length: imeiLength=%d, bodyLen=%d",
            imeiLength, bodyLen);
        log.error(errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      // 2. 验证剩余数据是否足够 (使用 long 防止整数溢出)
      if (in.readableBytes() < (long) imeiLength + bodyLen) {
        // 这里返回 null，调用方 MessageDecoderHandler 会进行 mark/reset 操作
        return null;
      }

      // 3. 优先处理心跳包 (0x270f = 9999)
      if (SystemCommand.PING.isMe(command)) {
        // 心跳包通常不需要后续复杂的解析逻辑，直接构造简易 Message 返回
        MessageHeader header = new MessageHeader();
        header.setCommand(command);
        header.setVersion(version);
        header.setClientType(clientType);
        header.setAppId(appId);
        header.setMessageType(messageType);
        header.setLength(bodyLen);

        // 已在上方验证过长度，此处 skipBytes 是安全的
        if (imeiLength > 0) {
          in.skipBytes(imeiLength);
        }
        if (bodyLen > 0) {
          in.skipBytes(bodyLen);
        }

        Message message = new Message();
        message.setMessageHeader(header);
        return message;
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
   * 协议格式：8字节头 + body - command(4) + bodyLen(4) + body(变长)
   *
   * @param msg MessagePack 对象
   * @param out 输出 ByteBuf
   */
  public static void encode(MessagePack<?> msg, ByteBuf out) {
    try {
      byte[] bodyBytes;
      Object data = msg.getData();

      if (data instanceof MessageLite) {
        // 如果已经是 Protobuf 对象
        bodyBytes = ((MessageLite) data).toByteArray();
      } else if (data instanceof JSONObject || data instanceof java.util.Map) {
        // 如果是 JSONObject 或 Map（来自 MQ 或 业务层），还原为 Proto
        MessageLite proto = convertJSONObjectToProto(msg.getCommand(),
            JsonUtils.toJSONObject(data));
        if (proto != null) {
          bodyBytes = proto.toByteArray();
        } else {
          log.error("无法将数据还原为 Protobuf 对象: command={}", msg.getCommand());
          bodyBytes = new byte[0];
        }
      } else {
        log.error("不支持的消息体类型，期待 MessageLite, JSONObject 或 Map，实际为: {}",
            data != null ? data.getClass().getName() : "null");
        bodyBytes = new byte[0];
      }

      // 写入超轻量协议头（8字节）
      // 注意：这里为了兼容，暂不改变协议头长度，但可以通过 command 或其他逻辑标记消息类型
      // 实际上，服务端发往客户端的 8 字节头里没有 messageType，
      // 我们可以考虑扩展协议头或通过特定的 command 范围来区分。
      // 但如果客户端知道某个 command 必须是 Protobuf，也可以。

      out.writeInt(msg.getCommand()); // command (4字节)
      out.writeInt(bodyBytes.length); // bodyLen (4字节)

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
  public static int calculateEncodedSize(MessagePack<?> msg) {
    try {
      int bodyLen = 0;
      Object data = msg.getData();
      if (data instanceof MessageLite) {
        bodyLen = ((MessageLite) data).getSerializedSize();
      } else if (data instanceof JSONObject || data instanceof java.util.Map) {
        MessageLite proto = convertJSONObjectToProto(msg.getCommand(),
            JsonUtils.toJSONObject(data));
        if (proto != null) {
          bodyLen = proto.getSerializedSize();
        }
      }
      // 协议头(8字节) + body长度
      return ENCODE_HEADER_LENGTH + bodyLen;
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
  private static void parseMessageBody(Message message, int messageType, byte[] bodyData,
      String imei, int bodyLen) {
    if (MessageType.DATA_TYPE_PROTOBUF.isMe(messageType)) {
      try {
        int command = message.getMessageHeader().getCommand();
        MessageLite pack = convertBytesToProto(command, bodyData);
        if (pack == null) {
          log.warn("Unknown command for Protobuf decoding: {}", command);
          message.setMessagePack(bodyData);
        } else {
          message.setMessagePack(pack);
        }
      } catch (Exception e) {
        log.error("Failed to parse Protobuf message body, imei={}, bodyLen={}", imei, bodyLen, e);
      }
    } else {
      log.warn("Unknown message type: {}, expected Protobuf=1, imei={}", messageType, imei);
      message.setMessagePack(bodyData);
    }
  }

  /**
   * 根据命令号将字节数组解析为对应的 Protobuf 对象
   */
  private static MessageLite convertBytesToProto(int command, byte[] data) throws Exception {
    String className = null;
    if (SystemCommand.LOGIN.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.LoginPack";
    } else if (MessageCommand.MSG_P2P.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.ChatMessagePack";
    } else if (MessageCommand.MSG_READ.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.MessageReadPack";
    } else if (MessageCommand.MSG_ACK.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.ChatMessageAck";
    } else if (MessageCommand.MSG_RECALL_NOTIFY.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.RecallMessageNotifyPack";
    } else if (FriendshipEventCommand.FRIEND_REQUEST.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.FriendRequestPack";
    } else if (FriendshipEventCommand.FRIEND_REQUEST_APPROVE.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.FriendRequestPack";
    } else if (FriendshipEventCommand.FRIEND_REQUEST_READ.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.ReadAllFriendRequestPack";
    } else if (FriendshipEventCommand.FRIEND_ADD.isMe(command)
        || FriendshipEventCommand.FRIEND_UPDATE.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.FriendInfoPack";
    } else if (FriendshipEventCommand.FRIEND_DELETE.isMe(command)
        || FriendshipEventCommand.FRIEND_ALL_DELETE.isMe(command)) {
      className = "com.pd.im.codec.proto.generated.DeleteFriendPack";
    } else if (FriendshipEventCommand.FRIEND_BLACK_ADD.isMe(command)
        || FriendshipEventCommand.FRIEND_BLACK_DELETE.isMe(command)) {
      // 黑名单通常复用 FriendInfoPack 或类似的 Pack，视 proto 定义而定
      className = "com.pd.im.codec.proto.generated.FriendInfoPack";
    } else if (FriendshipEventCommand.FRIEND_GROUP_ADD.isMe(command)
        || FriendshipEventCommand.FRIEND_GROUP_DELETE.isMe(command)
        || FriendshipEventCommand.FRIEND_GROUP_MEMBER_ADD.isMe(command)
        || FriendshipEventCommand.FRIEND_GROUP_MEMBER_DELETE.isMe(command)) {
      // 目前使用通用的自定义 JSON/Map 下发，或者后续补全对应的 GroupMemberPack
      // 这里为了流程通顺，暂不拦截，让它走外部 JsonUtils 链路或补全对应的类名
    }

    if (className != null) {
      Class<?> clazz = CLASS_CACHE.get(className);
      if (clazz == null) {
        clazz = Class.forName(className);
        CLASS_CACHE.put(className, clazz);
      }

      Method method = PARSE_METHOD_CACHE.get(className);
      if (method == null) {
        method = clazz.getMethod("parseFrom", byte[].class);
        PARSE_METHOD_CACHE.put(className, method);
      }

      return (MessageLite) method.invoke(null, (Object) data);
    }
    return null;
  }

  /**
   * 根据命令号将 JSONObject 还原为对应的 Protobuf 对象
   */
  private static MessageLite convertJSONObjectToProto(Integer command, JSONObject data) {
    if (command == null || data == null) {
      return null;
    }

    try {
      String className = null;
      if (SystemCommand.LOGINACK.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.LoginAckPack";
      } else if (MessageCommand.MSG_P2P.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.ChatMessagePack";
      } else if (MessageCommand.MSG_READ_NOTIFY.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.MessageReadPack";
      } else if (MessageCommand.MSG_ACK.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.ChatMessageAck";
      } else if (MessageCommand.MSG_RECALL_NOTIFY.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.RecallMessageNotifyPack";
      } else if (FriendshipEventCommand.FRIEND_ADD.isMe(command)
          || FriendshipEventCommand.FRIEND_UPDATE.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.FriendInfoPack";
      } else if (FriendshipEventCommand.FRIEND_DELETE.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.DeleteFriendPack";
      } else if (FriendshipEventCommand.FRIEND_REQUEST.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.FriendRequestPack";
      } else if (FriendshipEventCommand.FRIEND_REQUEST_APPROVE.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.FriendRequestPack";
      } else if (FriendshipEventCommand.FRIEND_REQUEST_READ.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.ReadAllFriendRequestPack";
      } else if (SystemCommand.MUTALOGIN.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.LoginAckPack"; // 互斥登录通常复用 LoginAck 或简单通知
      } else if (FriendshipEventCommand.FRIEND_ALL_DELETE.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.DeleteFriendPack";
      } else if (FriendshipEventCommand.FRIEND_BLACK_ADD.isMe(command)
          || FriendshipEventCommand.FRIEND_BLACK_DELETE.isMe(command)) {
        className = "com.pd.im.codec.proto.generated.FriendInfoPack";
      }

      if (className != null) {
        // 统一动态加载生成的 Protobuf 类 (注意：这里加载的是生成的 MessageLite 类)
        Class<? extends MessageLite> protoClazz = (Class<? extends MessageLite>) Class.forName(
            className);

        // 将 JSONObject (可能是从 POJO 序列化来的) 转换为 Protobuf 对象
        return JsonUtils.anyToProto(data, protoClazz);
      }
    } catch (Exception e) {
      log.error("Failed to convert JSONObject to Proto: command={}", command, e);
    }
    return null;
  }

  private MessageCodecUtils() {
    // 工具类，禁止实例化
  }
}
