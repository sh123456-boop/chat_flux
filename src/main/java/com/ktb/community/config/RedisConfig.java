package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .value(jsonSerializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    // chat publish 객체
    @Bean
    @Qualifier("chatPubSub")
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory redisConnectionFactory){
        return new ReactiveStringRedisTemplate(redisConnectionFactory);
    }

//    // subscribe 객체
//    @Bean
//    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(
//            ReactiveRedisConnectionFactory redisConnectionFactory,
//            RedisPubSubService redisPubSubService
//    ) {
//        ReactiveRedisMessageListenerContainer container = new ReactiveRedisMessageListenerContainer(redisConnectionFactory);
//        container.receive(ChannelTopic.of("chat"))
//                .flatMap(message -> redisPubSubService.handleMessage(message.getMessage()))
//                .subscribe();
//        return container;
//    }
}
