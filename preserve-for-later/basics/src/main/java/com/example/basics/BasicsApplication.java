package com.example.basics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class BasicsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasicsApplication.class, args);
	}


	@Bean
	RouterFunction<ServerResponse> routes(CustomerRepository repository) {
		return route()
			.GET("/customers", request -> ok().body(repository.findAll(), Customer.class))
			.build();
	}

	/*
	// spring cloud stream
		// redis
		// google cloud pub sub
		// azure service bus
		// kafka
		// kafka streams
		// rabbitmq
		// solace
	*/

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(
		DatabaseClient dbc,
		CustomerRepository cr) {
		return event -> {
			var ddl = dbc.sql("create table customer(id serial primary key, name varchar(255) not null)").fetch().rowsUpdated();
			var names = Flux.just("Brian", "Josh").map(name -> new Customer(null, name)).flatMap(cr::save);
			var all = cr.findAll();
			ddl.thenMany(names).thenMany(all).subscribe(System.out::println);
		};
	}
}

@Configuration
class GreetingsWebSocketConfiguration {

	@Bean
	WebSocketHandler webSocketHandler() {
		return session -> {
			var replies = session
				.receive()
				.map(WebSocketMessage::getPayloadAsText)
				.flatMap(txt -> Flux.fromStream(Stream.generate(() -> "Hello, " + txt + " @ " + new Date() + " !")).delayElements(Duration.ofSeconds(1)))
				.map(session::textMessage);
			return session.send(replies);
		};
	}

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
		return new SimpleUrlHandlerMapping(Map.of("/ws/greetings", wsh), 10);
	}
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

	@Id
	private Integer id;

	private String name;
}

