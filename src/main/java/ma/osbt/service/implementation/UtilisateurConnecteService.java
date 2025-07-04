package ma.osbt.service.implementation;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurConnecteService {

	public String getEmailConnecte() {
	    String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    System.out.println("Email connecté récupéré : " + email);
	    return email;
	}

}
