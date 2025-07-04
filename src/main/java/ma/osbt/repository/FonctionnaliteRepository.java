package ma.osbt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Fonctionnalite;
@Repository

public interface FonctionnaliteRepository extends JpaRepository<Fonctionnalite,Long> {
	List<Fonctionnalite> findByTypeAndPremiumFalseAndStatutTrue(String type);
	List<Fonctionnalite> findByCategorieAndTypeAndPremiumFalseAndStatutTrue(String categorie, String type);
	List<Fonctionnalite> findByTypeAndPremiumTrueAndStatutTrue(String type);

}
