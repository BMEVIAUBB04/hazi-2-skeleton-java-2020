package hu.bme.aut.logistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import hu.bme.aut.logistics.repository.AddressRepository;

@SpringBootApplication
public class LogisticsApplication implements CommandLineRunner {

    @Autowired
    AddressRepository addressRepository;
    
	public static void main(String[] args) {
		SpringApplication.run(LogisticsApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Ide írhattok próba kódokat, de alapvetően nem szükséges, 
		// mert a tesztek futtatása révén kipróbálhatók a megoldás
	}

}
