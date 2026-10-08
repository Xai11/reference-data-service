package com.example.referencedata.view;

import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.model.DictionaryItemValue;
import com.example.referencedata.security.SecurityUtils;
import com.example.referencedata.service.DictionaryAttributeService;
import com.example.referencedata.service.DictionaryItemService;
import com.example.referencedata.service.DictionaryPdfService;
import com.example.referencedata.service.DictionaryService;
import com.example.referencedata.service.ItemTree;
import com.example.referencedata.view.components.ActiveBadge;
import com.example.referencedata.view.components.AttributeFormDialog;
import com.example.referencedata.view.components.DictionaryChangedEvent;
import com.example.referencedata.view.components.DictionaryFormDialog;
import com.example.referencedata.view.components.EmptyState;
import com.example.referencedata.view.components.ItemFormDialog;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dictionary/:code", layout = MainLayout.class)
@PageTitle("Сервис справочников")
@PermitAll
public class DictionaryView extends VerticalLayout implements BeforeEnterObserver {

    private static final int COMPACT_WIDTH = 768;
    private static final String PDF_LABEL = "Сформировать PDF";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DictionaryService dictionaryService;
    private final DictionaryItemService itemService;
    private final DictionaryAttributeService attributeService;
    private final DictionaryPdfService pdfService;
    private final boolean editor = SecurityUtils.isEditor();

    private final H2 title = new H2();
    private final Span description = new Span();
    private final ActiveBadge badge = new ActiveBadge(true);
    private final Button pdfButton = new Button(PDF_LABEL, VaadinIcon.DOWNLOAD_ALT.create());
    private final Anchor download = new Anchor();

    private final Button editButton = new Button(VaadinIcon.EDIT.create());
    private final Button deleteButton = new Button(VaadinIcon.TRASH.create());
    private final Button addItemButton = new Button("Добавить элемент", VaadinIcon.PLUS.create());
    private final HorizontalLayout editorActions = new HorizontalLayout(editButton, deleteButton, addItemButton);

    private final Tab itemsTab = new Tab("Элементы");
    private final Tab attributesTab = new Tab("Атрибуты");
    private final Tabs tabs = new Tabs(itemsTab, attributesTab);

    // Элементы
    private final TextField search = new TextField();
    private final DatePicker asOfPicker = new DatePicker();
    private final TreeGrid<DictionaryItem> grid = new TreeGrid<>();
    private final Div gridCard = new Div();
    private final EmptyState emptyState = new EmptyState();
    private final List<Column<DictionaryItem>> compactHidden = new ArrayList<>();

    // Атрибуты
    private final Grid<DictionaryAttribute> attributesGrid = new Grid<>(DictionaryAttribute.class, false);
    private final Button addAttributeButton = new Button("Добавить атрибут", VaadinIcon.PLUS.create());
    private final Div attributesCard = new Div();

    private List<DictionaryItem> items = List.of();
    private Registration resizeRegistration;
    private Dictionary current;
    private boolean compact;

    public DictionaryView(DictionaryService dictionaryService, DictionaryItemService itemService,
                          DictionaryAttributeService attributeService, DictionaryPdfService pdfService) {
        this.dictionaryService = dictionaryService;
        this.itemService = itemService;
        this.attributeService = attributeService;
        this.pdfService = pdfService;

        addClassName("page");
        setPadding(false);

        // Шапка
        Div titleBlock = new Div(title, description);
        titleBlock.addClassName("grow");
        description.addClassName("muted");

        pdfButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        pdfButton.getElement().setAttribute("aria-label", PDF_LABEL);
        download.add(pdfButton);
        download.getElement().setAttribute("download", true);

        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("aria-label", "Изменить справочник");
        editButton.addClickListener(e -> openDictionaryForm());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("aria-label", "Удалить справочник");
        deleteButton.addClickListener(e -> confirmDeleteDictionary());
        addItemButton.addClickListener(e -> openItemForm(null));
        editorActions.setPadding(false);
        editorActions.setSpacing(false);
        editorActions.setVisible(false);

        FlexLayout header = new FlexLayout(titleBlock, badge, editorActions, download);
        header.addClassName("page-header");

        tabs.addSelectedChangeListener(e -> showTab());
        tabs.setVisible(false);

        // Элементы: поиск, дата актуальности, дерево
        search.setPlaceholder("Поиск по элементам");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> refreshTree());

        asOfPicker.setLabel("Актуально на дату");
        asOfPicker.setClearButtonVisible(true);
        asOfPicker.addValueChangeListener(e -> {
            if (current != null) {
                loadItems();
            }
        });

        FlexLayout toolbar = new FlexLayout(search, asOfPicker);
        toolbar.addClassName("items-toolbar");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setAllRowsVisible(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::itemDetails));

        gridCard.addClassName("card");
        gridCard.add(toolbar, grid);
        gridCard.setWidthFull();

        // Атрибуты
        attributesGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        attributesGrid.addColumn(DictionaryAttribute::getCode).setHeader("Код").setAutoWidth(true).setFlexGrow(0);
        attributesGrid.addColumn(DictionaryAttribute::getName).setHeader("Название").setFlexGrow(1);
        attributesGrid.addColumn(this::typeText).setHeader("Тип").setFlexGrow(1);
        attributesGrid.addColumn(a -> a.isRequired() ? "Да" : "Нет").setHeader("Обязательный")
                .setAutoWidth(true).setFlexGrow(0);
        attributesGrid.addColumn(DictionaryAttribute::getSortOrder).setHeader("Порядок")
                .setAutoWidth(true).setFlexGrow(0);
        if (editor) {
            attributesGrid.addColumn(new ComponentRenderer<>(this::attributeActions))
                    .setHeader("").setAutoWidth(true).setFlexGrow(0);
        }
        attributesGrid.setAllRowsVisible(true);
        addAttributeButton.addClickListener(e -> openAttributeForm(null));
        addAttributeButton.setVisible(editor);
        attributesCard.addClassName("card");
        attributesCard.add(addAttributeButton, attributesGrid);
        attributesCard.setWidthFull();
        attributesCard.setVisible(false);

        add(header, tabs, gridCard, attributesCard, emptyState);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String code = event.getRouteParameters().get("code").orElse(null);
        if (code == null) {
            showAll();
            return;
        }
        try {
            showDictionary(dictionaryService.getByCode(code));
        } catch (NotFoundException e) {
            showNotFound();
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UI ui = event.getUI();
        ui.getPage().executeJs("return window.innerWidth").then(Integer.class, this::applyWidth);
        resizeRegistration = ui.getPage().addBrowserWindowResizeListener(e -> applyWidth(e.getWidth()));
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (resizeRegistration != null) {
            resizeRegistration.remove();
        }
        super.onDetach(event);
    }

    // --- Режимы страницы ---

    private void showAll() {
        current = null;
        title.setText("Все справочники");
        description.setText("Содержимое всех справочников выгружается одним PDF-файлом");
        badge.setVisible(false);
        editorActions.setVisible(false);
        tabs.setVisible(false);
        gridCard.setVisible(false);
        attributesCard.setVisible(false);
        emptyState.set("Выберите справочник", "Откройте справочник в меню слева или выгрузите все справочники в PDF");
        emptyState.setVisible(true);
        setDownload("all-dictionaries.pdf", pdfService::generateAll);
    }

    private void showDictionary(Dictionary dictionary) {
        boolean sameDictionary = current != null && current.getId().equals(dictionary.getId());
        current = dictionary;
        title.setText(dictionary.getName());
        description.setText(describe(dictionary));
        badge.setActive(dictionary.isActive());
        badge.setVisible(true);
        editorActions.setVisible(editor);
        tabs.setVisible(true);
        if (!sameDictionary) {
            search.clear();
            asOfPicker.clear();
            tabs.setSelectedTab(itemsTab);
        }
        buildItemColumns();
        loadItems();
        attributesGrid.setItems(dictionary.getAttributes());
        showTab();
        setDownload(dictionary.getCode() + ".pdf", () -> pdfService.generate(dictionary));
    }

    private void showNotFound() {
        current = null;
        title.setText("Справочник не найден");
        description.setText("");
        badge.setVisible(false);
        editorActions.setVisible(false);
        tabs.setVisible(false);
        gridCard.setVisible(false);
        attributesCard.setVisible(false);
        download.setVisible(false);
        emptyState.set("Справочник не найден", "Проверьте ссылку или выберите справочник в меню");
        emptyState.setVisible(true);
    }

    private void showTab() {
        if (current == null) {
            return;
        }
        boolean attributes = tabs.getSelectedTab() == attributesTab;
        addItemButton.setVisible(!attributes);
        attributesCard.setVisible(attributes);
        boolean empty = items.isEmpty() && asOfPicker.isEmpty();
        gridCard.setVisible(!attributes && !empty);
        emptyState.setVisible(!attributes && empty);
        emptyState.set("Нет элементов", editor
                ? "Добавьте первый элемент кнопкой «Добавить элемент»"
                : "В этом справочнике пока нет записей");
    }

    private String describe(Dictionary dictionary) {
        String base = dictionary.getDescription() == null || dictionary.getDescription().isBlank()
                ? "Код: " + dictionary.getCode()
                : dictionary.getDescription() + " · Код: " + dictionary.getCode();
        return dictionary.isHierarchical() ? base + " · Иерархический" : base;
    }

    private void setDownload(String fileName, Supplier<byte[]> pdf) {
        download.setVisible(true);
        download.setHref(new StreamResource(fileName, () -> new ByteArrayInputStream(pdf.get())));
    }

    private void reload() {
        if (current != null) {
            showDictionary(dictionaryService.getById(current.getId()));
        }
    }

    // --- Таблица элементов ---

    private void buildItemColumns() {
        grid.removeAllColumns();
        compactHidden.clear();

        grid.addHierarchyColumn(DictionaryItem::getCode).setHeader("Код").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(DictionaryItem::getName).setHeader("Название").setFlexGrow(1);
        compactHidden.add(grid.addColumn(DictionaryItem::getDescription).setHeader("Описание").setFlexGrow(2));
        compactHidden.add(grid.addColumn(DictionaryItem::getSortOrder).setHeader("Порядок")
                .setAutoWidth(true).setFlexGrow(0));
        for (DictionaryAttribute attribute : current.getAttributes()) {
            compactHidden.add(grid.addColumn(item -> attributeText(item, attribute))
                    .setHeader(attribute.getName()).setFlexGrow(1));
        }
        compactHidden.add(grid.addColumn(this::periodText).setHeader("Период действия").setAutoWidth(true).setFlexGrow(0));
        grid.addColumn(new ComponentRenderer<>(item -> new ActiveBadge(item.statusOn(statusDate()))))
                .setHeader("Статус").setAutoWidth(true).setFlexGrow(0);
        if (editor) {
            grid.addColumn(new ComponentRenderer<>(this::itemActions))
                    .setHeader("").setAutoWidth(true).setFlexGrow(0);
        }
        applyCompact();
    }

    private void loadItems() {
        items = itemService.findByDictionary(current.getId(), false, asOfPicker.getValue());
        refreshTree();
        showTab();
    }

    /** Строит данные дерева: иерархия — только без поискового фильтра, иначе плоский список совпадений. */
    private void refreshTree() {
        if (current == null) {
            return;
        }
        String filter = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        TreeData<DictionaryItem> data = new TreeData<>();
        if (filter.isEmpty() && current.isHierarchical()) {
            Map<Long, DictionaryItem> byId = new HashMap<>();
            items.forEach(i -> byId.put(i.getId(), i));
            for (ItemTree.Node node : ItemTree.flatten(items)) {
                DictionaryItem item = node.item();
                DictionaryItem parent = item.getParentId() == null ? null : byId.get(item.getParentId());
                data.addItem(parent, item);
            }
        } else {
            data.addItems(null, items.stream().filter(i -> filter.isEmpty() || matches(i, filter)).toList());
        }
        grid.setDataProvider(new TreeDataProvider<>(data));
        if (filter.isEmpty() && current.isHierarchical()) {
            grid.expandRecursively(data.getRootItems(), 20);
        }
    }

    private boolean matches(DictionaryItem item, String filter) {
        return contains(item.getCode(), filter) || contains(item.getName(), filter)
                || contains(item.getDescription(), filter)
                || item.getValues().stream().anyMatch(v -> contains(v.displayValue(), filter));
    }

    private static boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private LocalDate statusDate() {
        return asOfPicker.getValue() != null ? asOfPicker.getValue() : LocalDate.now();
    }

    private String attributeText(DictionaryItem item, DictionaryAttribute attribute) {
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

    private Div itemDetails(DictionaryItem item) {
        Div details = new Div();
        details.addClassName("muted");
        List<String> lines = new ArrayList<>();
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            lines.add(item.getDescription());
        }
        String period = periodText(item);
        if (!period.isEmpty()) {
            lines.add("Период действия: " + period);
        }
        for (DictionaryAttribute attribute : current.getAttributes()) {
            String value = attributeText(item, attribute);
            if (!value.isEmpty()) {
                lines.add(attribute.getName() + ": " + value);
            }
        }
        lines.forEach(line -> details.add(new Div(new Span(line))));
        return details;
    }

    // --- Редактирование справочника ---

    private void openDictionaryForm() {
        new DictionaryFormDialog(current, dictionaryService, saved -> {
            success("Справочник сохранён");
            DictionaryChangedEvent.fire(UI.getCurrent());
            if (saved.getCode().equals(current.getCode())) {
                reload();
            } else {
                UI.getCurrent().navigate("dictionary/" + saved.getCode());
            }
        }).open();
    }

    private void confirmDeleteDictionary() {
        int count = items.size();
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить справочник?");
        dialog.setText("Справочник «" + current.getName() + "»"
                + (count > 0 ? " и все его элементы (" + count + ")" : "") + " будет удалён безвозвратно.");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.setConfirmText("Удалить");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> runSafely(() -> {
            dictionaryService.delete(current.getId());
            success("Справочник удалён");
            DictionaryChangedEvent.fire(UI.getCurrent());
            UI.getCurrent().navigate("");
        }));
        dialog.open();
    }

    // --- Редактирование элементов ---

    private HorizontalLayout itemActions(DictionaryItem item) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openItemForm(item));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", "Изменить элемент");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDeleteItem(item));
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        delete.getElement().setAttribute("aria-label", "Удалить элемент");
        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setPadding(false);
        actions.setSpacing(false);
        return actions;
    }

    private void openItemForm(DictionaryItem item) {
        List<DictionaryItem> all = itemService.findByDictionary(current.getId(), false);
        new ItemFormDialog(current, item, all, itemService, saved -> {
            success("Элемент сохранён");
            reload();
        }).open();
    }

    private void confirmDeleteItem(DictionaryItem item) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить элемент?");
        dialog.setText("Элемент «" + item.getName() + "» будет удалён.");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.setConfirmText("Удалить");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> runSafely(() -> {
            itemService.delete(item.getId());
            success("Элемент удалён");
            reload();
        }));
        dialog.open();
    }

    // --- Редактирование атрибутов ---

    private String typeText(DictionaryAttribute attribute) {
        if (attribute.getType() == AttributeType.REFERENCE && attribute.getRefDictionary() != null) {
            return attribute.getType().getLabel() + " → " + attribute.getRefDictionary().getName();
        }
        return attribute.getType().getLabel();
    }

    private HorizontalLayout attributeActions(DictionaryAttribute attribute) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openAttributeForm(attribute));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", "Изменить атрибут");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDeleteAttribute(attribute));
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        delete.getElement().setAttribute("aria-label", "Удалить атрибут");
        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setPadding(false);
        actions.setSpacing(false);
        return actions;
    }

    private void openAttributeForm(DictionaryAttribute attribute) {
        new AttributeFormDialog(current.getId(), attribute, dictionaryService.findAll(), attributeService, saved -> {
            success("Атрибут сохранён");
            reload();
        }).open();
    }

    private void confirmDeleteAttribute(DictionaryAttribute attribute) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить атрибут?");
        dialog.setText("Атрибут «" + attribute.getName() + "» и его значения у всех элементов будут удалены.");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.setConfirmText("Удалить");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> runSafely(() -> {
            attributeService.delete(attribute.getId());
            success("Атрибут удалён");
            reload();
        }));
        dialog.open();
    }

    // --- Вспомогательное ---

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (NotFoundException e) {
            error("Запись уже удалена или не найдена");
            reload();
        } catch (RuntimeException e) {
            error(e.getMessage() == null ? "Не удалось выполнить операцию" : e.getMessage());
        }
    }

    private static void success(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void error(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void applyWidth(int width) {
        compact = width < COMPACT_WIDTH;
        applyCompact();
        pdfButton.setText(compact ? "" : PDF_LABEL);
    }

    private void applyCompact() {
        compactHidden.forEach(column -> column.setVisible(!compact));
        grid.setDetailsVisibleOnClick(compact);
    }
}
