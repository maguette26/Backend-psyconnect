package ma.osbt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.osbt.entitie.ReponseForum;

public interface ReponseForumRepository extends JpaRepository<ReponseForum, Long> {
    List<ReponseForum> findBySujetIdOrderByDateReponseAsc(Long sujetId);
}