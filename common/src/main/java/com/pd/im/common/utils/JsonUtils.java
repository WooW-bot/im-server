package com.pd.im.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统用的 JSON 工具类，支持 Protobuf 对象的安全转换 解决 FastJSON 直接序列化 Protobuf 对象导致的 StackOverflowError
 */
@Slf4j
public class JsonUtils {

  /**
   * 类 -> newBuilder 方法缓存
   */
  private static final Map<Class<?>, Method> NEW_BUILDER_CACHE = new ConcurrentHashMap<>();

  /**
   * 将对象转换为 JSON 字符串 自动识别 Protobuf 对象并使用 JsonFormat 处理
   *
   * @param object 待转换对象
   * @return JSON 字符串
   */
  public static String toJSONString(Object object) {
    if (object == null) {
      return null;
    }

    if (object instanceof MessageOrBuilder) {
      try {
        return JsonFormat.printer().print((MessageOrBuilder) object);
      } catch (Exception e) {
        log.error("Protobuf 转 JSON 字符串失败", e);
        return "{}";
      }
    }

    return JSON.toJSONString(object);
  }

  /**
   * 将对象转换为 JSONObject 自动识别 Protobuf 对象并使用 JsonFormat 处理
   *
   * @param object 待转换对象
   * @return JSONObject
   */
  public static JSONObject toJSONObject(Object object) {
    if (object == null) {
      return null;
    }

    if (object instanceof JSONObject) {
      return (JSONObject) object;
    }

    if (object instanceof MessageOrBuilder) {
      try {
        String jsonStr = JsonFormat.printer().print((MessageOrBuilder) object);
        return JSON.parseObject(jsonStr);
      } catch (Exception e) {
        log.error("Protobuf 转 JSONObject 失败", e);
        return new JSONObject();
      }
    }

    Object json = JSON.toJSON(object);
    if (json instanceof JSONObject) {
      return (JSONObject) json;
    }

    // 如果是 byte[] 或 JSONArray 等非对象类型，将其包装在 data 字段中
    JSONObject wrapper = new JSONObject();
    wrapper.put("data", json);
    return wrapper;
  }

  /**
   * 将 JSON 字符串转换为 Protobuf 对象
   *
   * @param json  JSON 字符串
   * @param clazz Protobuf 类
   * @return Protobuf 对象
   */
  public static com.google.protobuf.MessageLite fromJson(String json,
      Class<? extends com.google.protobuf.MessageLite> clazz) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      // 优化反射：缓存 newBuilder 方法
      Method newBuilderMethod = NEW_BUILDER_CACHE.get(clazz);
      if (newBuilderMethod == null) {
        newBuilderMethod = clazz.getMethod("newBuilder");
        NEW_BUILDER_CACHE.put(clazz, newBuilderMethod);
      }

      // 首先尝试使用 Protobuf 标准的 JsonFormat (最严谨)
      Object builderObj = newBuilderMethod.invoke(null);
      if (builderObj instanceof com.google.protobuf.Message.Builder) {
        try {
          com.google.protobuf.util.JsonFormat.parser().ignoringUnknownFields()
              .merge(json, (com.google.protobuf.Message.Builder) builderObj);
          return ((com.google.protobuf.Message.Builder) builderObj).build();
        } catch (Exception e) {
          log.warn("JsonFormat.parser 转换失败，尝试 fallback 方案: class={}, message={}",
              clazz.getName(), e.getMessage());
        }
      }

      // Fallback 方案：利用 FastJSON 的灵活性处理数字字符串等类型转换
      JSONObject jsonObject = JSON.parseObject(json);
      builderObj = newBuilderMethod.invoke(null);

      if (builderObj instanceof com.google.protobuf.MessageLite.Builder) {
        com.google.protobuf.MessageLite.Builder builder = (com.google.protobuf.MessageLite.Builder) builderObj;
        java.lang.reflect.Method[] methods = builder.getClass().getMethods();

        for (String key : jsonObject.keySet()) {
          try {
            String setterName = "set" + key.toLowerCase();
            Object val = jsonObject.get(key);
            if (val == null) {
              continue;
            }

            for (java.lang.reflect.Method method : methods) {
              // 忽略大小写匹配 setter
              if (method.getName().equalsIgnoreCase(setterName)
                  && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType == long.class || paramType == Long.class) {
                  method.invoke(builder, jsonObject.getLong(key));
                } else if (paramType == int.class || paramType == Integer.class) {
                  method.invoke(builder, jsonObject.getInteger(key));
                } else if (paramType == String.class) {
                  method.invoke(builder, jsonObject.getString(key));
                } else if (paramType == boolean.class || paramType == Boolean.class) {
                  method.invoke(builder, jsonObject.getBoolean(key));
                } else if (paramType == double.class || paramType == Double.class) {
                  method.invoke(builder, jsonObject.getDouble(key));
                } else if (paramType == float.class || paramType == Float.class) {
                  method.invoke(builder, jsonObject.getFloat(key));
                }
                break;
              }
            }
          } catch (Exception ex) {
            log.warn("Fallback 填充字段失败: key={}, class={}", key, clazz.getName());
          }
        }
        return builder.build();
      }
      return null;
    } catch (Exception e) {
      log.error("JSON 转换为 Protobuf 彻底失败: class={}", clazz.getName(), e);
      return null;
    }
  }

  /**
   * 将任意对象转换为 Protobuf 对象 逻辑：对象 -> JSON 字符串 -> Protobuf
   *
   * @param object 任意对象 (POJO 或 Map)
   * @param clazz  目标 Protobuf 类
   * @return Protobuf 对象
   */
  public static com.google.protobuf.MessageLite anyToProto(Object object,
      Class<? extends com.google.protobuf.MessageLite> clazz) {
    if (object == null) {
      return null;
    }
    String json = toJSONString(object);
    return fromJson(json, clazz);
  }
}
