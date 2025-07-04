package ma.osbt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Personne;
import ma.osbt.entitie.Role;

@Repository
public interface PersonneRepository extends JpaRepository<Personne,Long> {

	List<Personne> findByRoleIn(List<Role> roles);
	Optional<Personne> findByEmail(String email);
	boolean existsByTelephone(String nouveauTelephone);


}