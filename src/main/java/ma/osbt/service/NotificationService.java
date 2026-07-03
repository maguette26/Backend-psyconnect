package ma.osbt.service;

import ma.osbt.entitie.Personne;

public interface NotificationService {
	 	 void notifier(Personne destinataire, String message);
	 	void notifierAvecPieceJointe(Personne destinataire, String messageHtml, byte[] pdfData, String fileName);
		void notifierAdmin(String string);
		void envoyerMailReinitialisation(Personne personne,String lien);

}
