package icet.koco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KocoApplication {

	public static void main(String[] args) {
		SpringApplication.run(KocoApplication.class, args);
	}

}
