package ma.osbt.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.osbt.entitie.Disponibilite;

public interface DisponibiliteRepository extends JpaRepository<Disponibilite, Long> {

    List<Disponibilite> findByProfessionnelId(Long professionnelId);

    List<Disponibilite> findByProfessionnelIdAndDate(Long professionnelId, LocalDate date);

    // Charge les disponibilités avec leurs réservations associées pour éviter le lazy loading N+1
    @Query("SELECT DISTINCT d FROM Disponibilite d LEFT JOIN FETCH d.reservations WHERE d.professionnel.id = :proId")
    List<Disponibilite> findByProfessionnelIdWithReservations(@Param("proId") Long proId);
}

 
