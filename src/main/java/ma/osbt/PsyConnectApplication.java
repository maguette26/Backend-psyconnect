package ma.osbt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
 public class PsyConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(PsyConnectApplication.class, args);
	}
}
