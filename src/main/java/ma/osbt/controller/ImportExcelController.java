package ma.osbt.controller;

import lombok.RequiredArgsConstructor;
import ma.osbt.excel.FonctionnaliteExcelImporter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/import")
public class ImportExcelController {

    private final FonctionnaliteExcelImporter importer;

    @PostMapping("/fonctionnalites")
    public ResponseEntity<?> importerFonctionnalites(@RequestParam("file") MultipartFile file) {
        try {
            importer.importerFonctionnalitesDepuisExcel(file);
            return ResponseEntity.ok("Importation réussie !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }
}
