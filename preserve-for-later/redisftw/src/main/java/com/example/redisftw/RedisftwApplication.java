package com.example.redisftw;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.io.Serializable;

/*
 1. cache
 2. session
 3. regular data access with Redis
 */
@EnableCaching
@EnableRedisHttpSession(saveMode = SaveMode.ALWAYS)
@SpringBootApplication
public class RedisftwApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisftwApplication.class, args);
	}

	@Bean
	CommandLineRunner ops(StringRedisTemplate rt) {
		return event -> {
			rt.opsForValue().set("test", "hello world");
			rt.opsForValue().get("test");


		};
	}

	@Bean
	CommandLineRunner cache(
		Calculator calculator, RedisTemplate<Object, Object> rt) {
		return event -> {
			System.out.println(calculator.add(1, 2));
			System.out.println(calculator.add(1, 2));


//			rt.opsForValue().getOperations().opsForValue().set("value" , 1);


		};
	}

}

@RestController
class RedisSessionController {

	private final String countKey = "count";

	@GetMapping("/doit/{v}")
	int doit(HttpSession session, @PathVariable int v) {
		session.setAttribute("value", v);
		return get(session);
	}

	@GetMapping("/read")
	int get(HttpSession session) {
		return (int) session.getAttribute("value");
	}
}

interface Calculator {
	int add(int a, int b);
}

@Component
class SimpleCalculator implements Calculator {

	@Cacheable("add")
	@SneakyThrows
	public int add(int a, int b) {
		System.out.println("start");
		Thread.sleep(3_000);
		var res = a + b;
		System.out.println("stop");
		return res;
	}

}

@AllArgsConstructor
@NoArgsConstructor
class ShoppingCart implements Serializable {
	private int number;
	private String string;
}
