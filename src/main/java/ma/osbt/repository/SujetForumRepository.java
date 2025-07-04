package ma.osbt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.osbt.entitie.SujetForum;

public interface SujetForumRepository extends JpaRepository<SujetForum, Long> {
    List<SujetForum> findAllByOrderByDateCreationDesc();
}



