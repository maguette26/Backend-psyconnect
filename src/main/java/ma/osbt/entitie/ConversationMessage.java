package ma.osbt.entitie;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "psybot_messages")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⚠️ Remplace "Utilisateur" par le nom exact de ton entité User existante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 10)
    private String sender; // "user" ou "bot"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}