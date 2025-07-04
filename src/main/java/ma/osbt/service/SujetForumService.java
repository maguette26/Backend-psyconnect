package ma.osbt.service;

import java.util.List;
import java.util.Optional;

import ma.osbt.entitie.SujetForum;

public interface SujetForumService {
	 public List<SujetForum> listerSujets();
	 public SujetForum creerSujet(SujetForum sujet);
	 public Optional<SujetForum> getSujet(Long id);
	 public void supprimerSujet(Long id);
}
