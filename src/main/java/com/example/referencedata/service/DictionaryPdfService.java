package com.example.referencedata.service;

import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class DictionaryPdfService {

    private static final String[] COLUMNS = {"Код", "Название", "Описание", "Порядок", "Активен"};
    private static final float[] WIDTHS = {2f, 3f, 4f, 1.4f, 1.4f};

    private final DictionaryService dictionaryService;
    private final DictionaryItemService itemService;

    private final BaseFont regular;
    private final BaseFont bold;

    public DictionaryPdfService(DictionaryService dictionaryService, DictionaryItemService itemService) {
        this.dictionaryService = dictionaryService;
        this.itemService = itemService;
        this.regular = loadFont("fonts/DejaVuSans.ttf");
        this.bold = loadFont("fonts/DejaVuSans-Bold.ttf");
    }

    public byte[] generate(Dictionary dictionary) {
        return generateAll(List.of(dictionary));
    }

    public byte[] generateAll() {
        return generateAll(dictionaryService.findAll());
    }

    public byte[] generateAll(List<Dictionary> dictionaries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            boolean first = true;
            for (Dictionary dictionary : dictionaries) {
                if (!first) {
                    document.newPage();
                }
                first = false;
                appendDictionary(document, dictionary);
            }
            if (first) {
                document.add(new Paragraph("Справочники отсутствуют", new Font(regular, 12)));
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Не удалось сформировать PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void appendDictionary(Document document, Dictionary dictionary) throws DocumentException {
        Paragraph title = new Paragraph(dictionary.getName(), new Font(bold, 16));
        title.setSpacingAfter(2);
        document.add(title);

        Paragraph code = new Paragraph("Код: " + dictionary.getCode(), new Font(regular, 10));
        code.setSpacingAfter(10);
        document.add(code);

        List<DictionaryItem> items = itemService.findByDictionary(dictionary.getId(), false);
        if (items.isEmpty()) {
            document.add(new Paragraph("Нет элементов", new Font(regular, 11)));
            return;
        }

        PdfPTable table = new PdfPTable(COLUMNS.length);
        table.setWidthPercentage(100);
        table.setWidths(WIDTHS);
        table.setHeaderRows(1);
        for (String column : COLUMNS) {
            PdfPCell cell = new PdfPCell(new Phrase(column, new Font(bold, 10)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
        Font body = new Font(regular, 10);
        for (DictionaryItem item : items) {
            table.addCell(cell(item.getCode(), body));
            table.addCell(cell(item.getName(), body));
            table.addCell(cell(item.getDescription(), body));
            table.addCell(cell(String.valueOf(item.getSortOrder()), body));
            table.addCell(cell(item.isActive() ? "Да" : "Нет", body));
        }
        document.add(table);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        return cell;
    }

    private static BaseFont loadFont(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Не удалось загрузить шрифт " + path, e);
        }
    }
}
