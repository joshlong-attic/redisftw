package com.example.reactiveredisftw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class ReactiveredisftwApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveredisftwApplication.class, args);
	}

	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(3,1);
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		return http
			.httpBasic(Customizer.withDefaults())
			.cors(ServerHttpSecurity.CorsSpec::disable)
			.authorizeExchange(ae -> ae
				.pathMatchers("/proxy").authenticated()
				.anyExchange().permitAll())
			.build();
	}

	@Bean
	MapReactiveUserDetailsService authentication() {
		return new MapReactiveUserDetailsService(
			User.withDefaultPasswordEncoder().username("bsbodden").roles("ADMIN").password("pw").build()
		);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(routeSpec -> routeSpec
				.path("/proxy").and().host("*.spring.io")
				.filters(filterSpec -> filterSpec
					.setPath("/guides")
					.requestRateLimiter(rlc ->
						rlc
							.setRateLimiter(redisRateLimiter())
					)
				)
				.uri("https://spring.io/")

			)
			.build();
	}

}
