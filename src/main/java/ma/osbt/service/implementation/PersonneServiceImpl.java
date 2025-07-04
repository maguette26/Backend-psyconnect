package ma.osbt.service.implementation;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.Personne;
import ma.osbt.service.PersonneService;
 
@Service
@RequiredArgsConstructor
public class PersonneServiceImpl implements PersonneService {@Override
	public Personne findByEmail(String email) {
		// TODO Auto-generated method stub
		return null;
	}
 
}
