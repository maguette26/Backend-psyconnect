package ma.osbt.service;

import ma.osbt.entitie.Personne;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    // Optionnel: pour une logique métier plus complexe
    public boolean checkAdminAccess(Personne personne) {
        return "ADMIN".equals(personne.getRole().name());
    }
}