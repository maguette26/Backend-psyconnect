package ma.osbt.service;

import ma.osbt.entitie.Personne;

public interface PersonneService {
	Personne findByEmail(String email);
}
