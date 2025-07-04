package ma.osbt.excel;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.Fonctionnalite;
import ma.osbt.repository.FonctionnaliteRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FonctionnaliteExcelImporter {

    private final FonctionnaliteRepository fonctionnaliteRepository;

    public void importerFonctionnalitesDepuisExcel(MultipartFile file) throws Exception {
        List<Fonctionnalite> fonctionnalites = new ArrayList<>();
        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // ligne 0 = entête
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Fonctionnalite f = new Fonctionnalite();
            f.setNom(getStringCell(row, 0));
            f.setType(getStringCell(row, 1));
            f.setDescription(getStringCell(row, 2));
            f.setPremium(Boolean.parseBoolean(getStringCell(row, 3)));
            f.setStatut(Boolean.parseBoolean(getStringCell(row, 4)));
            f.setLienFichier(getStringCell(row, 5));
            f.setCategorie(getStringCell(row, 6));

            fonctionnalites.add(f);
        }

        workbook.close();
        fonctionnaliteRepository.saveAll(fonctionnalites);
    }

    private String getStringCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        return cell != null ? cell.toString().trim() : "";
    }
}

