package com.example.referencedata.service;

import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.model.DictionaryItemValue;
import com.example.referencedata.model.ItemStatus;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DictionaryPdfService {

    private static final int MAX_PORTRAIT_COLUMNS = 6;
    private static final float INDENT_PER_LEVEL = 10f;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        // Размер первой страницы задаётся при создании документа: после открытия он уже не меняется.
        com.lowagie.text.Rectangle firstPage = dictionaries.isEmpty() ? PageSize.A4 : pageSizeFor(dictionaries.get(0));
        Document document = new Document(firstPage, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            boolean first = true;
            for (Dictionary dictionary : dictionaries) {
                if (!first) {
                    document.setPageSize(pageSizeFor(dictionary));
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

    /** Колонки таблицы справочника: описание и период выводятся, только если они есть хотя бы у одного элемента. */
    private record Layout(boolean description, boolean period, List<DictionaryAttribute> attributes) {
        int columns() {
            return 3 + (description ? 1 : 0) + (period ? 1 : 0) + attributes.size();
        }
    }

    private Layout layoutFor(Dictionary dictionary, List<DictionaryItem> items) {
        boolean description = items.stream().anyMatch(i -> i.getDescription() != null && !i.getDescription().isBlank());
        boolean period = items.stream().anyMatch(i -> i.getValidFrom() != null || i.getValidTo() != null);
        return new Layout(description, period, dictionary.getAttributes());
    }

    private com.lowagie.text.Rectangle pageSizeFor(Dictionary dictionary) {
        int columns = 3 + dictionary.getAttributes().size() + 2;
        // rotate() лишь ставит флаг /Rotate на портретной странице, поэтому берём явный альбомный размер
        return columns > MAX_PORTRAIT_COLUMNS
                ? new com.lowagie.text.Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth())
                : PageSize.A4;
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

        Layout layout = layoutFor(dictionary, items);
        List<String> headers = new ArrayList<>(List.of("Код", "Название"));
        List<Float> widths = new ArrayList<>(List.of(2.2f, 3.2f));
        if (layout.description()) {
            headers.add("Описание");
            widths.add(4f);
        }
        for (DictionaryAttribute attribute : layout.attributes()) {
            headers.add(attribute.getName());
            widths.add(2.6f);
        }
        if (layout.period()) {
            headers.add("Период действия");
            widths.add(3f);
        }
        headers.add("Статус");
        widths.add(2f);

        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        float[] widthArray = new float[widths.size()];
        for (int i = 0; i < widthArray.length; i++) {
            widthArray[i] = widths.get(i);
        }
        table.setWidths(widthArray);
        table.setHeaderRows(1);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, new Font(bold, 10)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }

        Font body = new Font(regular, 10);
        LocalDate today = LocalDate.now();
        List<ItemTree.Node> nodes = dictionary.isHierarchical()
                ? ItemTree.flatten(items)
                : items.stream().map(i -> new ItemTree.Node(i, 0)).toList();
        for (ItemTree.Node node : nodes) {
            DictionaryItem item = node.item();
            PdfPCell codeCell = cell(item.getCode(), body);
            codeCell.setPaddingLeft(4 + node.level() * INDENT_PER_LEVEL);
            table.addCell(codeCell);
            table.addCell(cell(item.getName(), body));
            if (layout.description()) {
                table.addCell(cell(item.getDescription(), body));
            }
            for (DictionaryAttribute attribute : layout.attributes()) {
                table.addCell(cell(valueText(item, attribute), body));
            }
            if (layout.period()) {
                table.addCell(cell(periodText(item), body));
            }
            table.addCell(cell(ItemStatus.of(item, today).getLabel(), body));
        }
        document.add(table);
    }

    private String valueText(DictionaryItem item, DictionaryAttribute attribute) {
        return item.getValues().stream()
                .filter(v -> v.getAttribute().getId().equals(attribute.getId()))
                .map(DictionaryItemValue::displayValue)
                .findFirst().orElse("");
    }

    private String periodText(DictionaryItem item) {
        LocalDate from = item.getValidFrom();
        LocalDate to = item.getValidTo();
        if (from == null && to == null) {
            return "";
        }
        if (to == null) {
            return "с " + DATE_FORMAT.format(from);
        }
        if (from == null) {
            return "до " + DATE_FORMAT.format(to);
        }
        return DATE_FORMAT.format(from) + " — " + DATE_FORMAT.format(to);
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
