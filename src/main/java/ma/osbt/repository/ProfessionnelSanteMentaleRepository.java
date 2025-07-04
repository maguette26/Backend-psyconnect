package ma.osbt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.StatutValidation;
@Repository

public interface ProfessionnelSanteMentaleRepository extends JpaRepository<ProfessionnelSanteMentale,Long> {
	   @Query("SELECT p FROM ProfessionnelSanteMentale p WHERE p.statutValidation = :statut")
	List<ProfessionnelSanteMentale> findByStatutValidation(StatutValidation statut);
    List<ProfessionnelSanteMentale> findBySpecialiteContainingIgnoreCase(String specialite);
    List<ProfessionnelSanteMentale> findByNomContainingIgnoreCase(String nom);
    Optional<ProfessionnelSanteMentale> findByEmail(String email);
	boolean existsByEmail(String email);
    
}
