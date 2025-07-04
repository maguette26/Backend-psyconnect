package ma.osbt.service.implementation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.SujetForum;
import ma.osbt.repository.SujetForumRepository;
import ma.osbt.service.SujetForumService;
@Service
public class SujetForumServiceImpl implements SujetForumService {
    @Autowired
    private SujetForumRepository sujetRepo;

    @Override
    public List<SujetForum> listerSujets() {
        return sujetRepo.findAllByOrderByDateCreationDesc();
    }

    @Override
    public SujetForum creerSujet(SujetForum sujet) {
        return sujetRepo.save(sujet);
    }

    @Override
    public Optional<SujetForum> getSujet(Long id) {
        return sujetRepo.findById(id);
    }
    
    @Override
    public void supprimerSujet(Long id) {
        sujetRepo.deleteById(id);
    }

}
