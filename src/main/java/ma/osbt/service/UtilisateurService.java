package ma.osbt.service;

import java.util.List;


import java.util.Optional;

import ma.osbt.entitie.Utilisateur;

public interface UtilisateurService {
 public	List<Utilisateur> getAllUtilisateurs();
 public   Optional<Utilisateur> getUtilisateurById(Long id);
 public  Utilisateur saveUtilisateur(Utilisateur utilisateur);
 public  void deleteUtilisateur(Long id);
 public Optional<Utilisateur> modifierUtilisateur(Utilisateur utilisateur);  
 //public Personne enregistrerPersonne(Personne personne);

}
