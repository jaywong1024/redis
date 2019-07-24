package com.good.word.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * @SuppressWarnings 该批注的作用是给编译器一条指令，告诉它对被批注的代码元素内部的某些警告保持静默
     *
     *    关键字         用途
     *    deprecation   使用了不赞成使用的类或方法时的警告
     *    unchecked     执行了未检查的转换时的警告，例如当使用集合时没有用泛型 (Generics) 来指定集合保存的类型。
     *    fallthrough   当 Switch 程序块直接通往下一种情况而没有 Break 时的警告。
     *    path          在类路径、源文件路径等中有不存在的路径时的警告。
     *    serial        当在可序列化的类上缺少 serialVersionUID 定义时的警告。
     *    finally       任何 finally 子句不能正常完成时的警告。
     *    all           关于以上所有情况的警告。
     */

//    注入 Spring 容器中，忽略所有警告
    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//        key 采用 String 的序列化方式
        template.setKeySerializer(stringRedisSerializer);
//        hash 的 key 也采用 String 的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
//        value 的序列化方式采用 JSON
        template.setValueSerializer(jackson2JsonRedisSerializer);
//        hash value 的序列化方式也采用 JSON
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
