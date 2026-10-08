package com.example.referencedata.view;

import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.security.SecurityUtils;
import com.example.referencedata.service.DictionaryItemService;
import com.example.referencedata.service.DictionaryPdfService;
import com.example.referencedata.service.DictionaryService;
import com.example.referencedata.view.components.ActiveBadge;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dictionary/:code", layout = MainLayout.class)
@PageTitle("Сервис справочников")
@PermitAll
public class DictionaryView extends VerticalLayout implements BeforeEnterObserver {

    private static final int COMPACT_WIDTH = 768;
    private static final String PDF_LABEL = "Сформировать PDF";

    private final DictionaryService dictionaryService;
    private final DictionaryItemService itemService;
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

    private final TextField search = new TextField();
    private final Grid<DictionaryItem> grid = new Grid<>(DictionaryItem.class, false);
    private final Div gridCard = new Div();
    private final EmptyState emptyState = new EmptyState();

    private final Column<DictionaryItem> descriptionColumn;
    private final Column<DictionaryItem> orderColumn;

    private ListDataProvider<DictionaryItem> dataProvider = new ListDataProvider<>(List.of());
    private Registration resizeRegistration;
    private Dictionary current;

    public DictionaryView(DictionaryService dictionaryService, DictionaryItemService itemService,
                          DictionaryPdfService pdfService) {
        this.dictionaryService = dictionaryService;
        this.itemService = itemService;
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

        // Поиск и таблица
        search.setPlaceholder("Поиск по элементам");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> applyFilter(e.getValue()));

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addColumn(DictionaryItem::getCode).setHeader("Код").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(DictionaryItem::getName).setHeader("Название").setFlexGrow(1);
        descriptionColumn = grid.addColumn(DictionaryItem::getDescription).setHeader("Описание").setFlexGrow(2);
        orderColumn = grid.addColumn(DictionaryItem::getSortOrder).setHeader("Порядок").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(new ComponentRenderer<>(item -> new ActiveBadge(item.isActive())))
                .setHeader("Статус").setAutoWidth(true).setFlexGrow(0);
        if (editor) {
            grid.addColumn(new ComponentRenderer<>(this::itemActions))
                    .setHeader("").setAutoWidth(true).setFlexGrow(0);
        }
        grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            Span details = new Span(item.getDescription() == null ? "" : item.getDescription());
            details.addClassName("muted");
            return details;
        }));
        grid.setAllRowsVisible(true);

        gridCard.addClassName("card");
        gridCard.add(search, grid);
        gridCard.setWidthFull();

        add(header, gridCard, emptyState);
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
        search.clear();
        gridCard.setVisible(false);
        emptyState.set("Выберите справочник", "Откройте справочник в меню слева или выгрузите все справочники в PDF");
        emptyState.setVisible(true);
        setDownload("all-dictionaries.pdf", pdfService::generateAll);
    }

    private void showDictionary(Dictionary dictionary) {
        current = dictionary;
        title.setText(dictionary.getName());
        description.setText(dictionary.getDescription() == null || dictionary.getDescription().isBlank()
                ? "Код: " + dictionary.getCode()
                : dictionary.getDescription() + " · Код: " + dictionary.getCode());
        badge.setActive(dictionary.isActive());
        badge.setVisible(true);
        editorActions.setVisible(editor);
        search.clear();

        List<DictionaryItem> items = itemService.findByDictionary(dictionary.getId(), false);
        dataProvider = new ListDataProvider<>(items);
        grid.setDataProvider(dataProvider);
        boolean empty = items.isEmpty();
        gridCard.setVisible(!empty);
        emptyState.setVisible(empty);
        emptyState.set("Нет элементов", editor
                ? "Добавьте первый элемент кнопкой «Добавить элемент»"
                : "В этом справочнике пока нет записей");
        setDownload(dictionary.getCode() + ".pdf", () -> pdfService.generate(dictionary));
    }

    private void showNotFound() {
        current = null;
        title.setText("Справочник не найден");
        description.setText("");
        badge.setVisible(false);
        editorActions.setVisible(false);
        gridCard.setVisible(false);
        download.setVisible(false);
        emptyState.set("Справочник не найден", "Проверьте ссылку или выберите справочник в меню");
        emptyState.setVisible(true);
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

    // --- Редактирование справочника ---

    private void openDictionaryForm() {
        new DictionaryFormDialog(current, dictionaryService, saved -> {
            success("Справочник сохранён");
            DictionaryChangedEvent.fire(UI.getCurrent());
            if (saved.getCode().equals(current.getCode())) {
                showDictionary(saved);
            } else {
                UI.getCurrent().navigate("dictionary/" + saved.getCode());
            }
        }).open();
    }

    private void confirmDeleteDictionary() {
        int count = dataProvider.getItems().size();
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
        new ItemFormDialog(current.getId(), item, itemService, saved -> {
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

    // --- Вспомогательное ---

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (NotFoundException e) {
            error("Запись уже удалена или не найдена");
            reload();
        } catch (RuntimeException e) {
            error("Не удалось выполнить операцию: " + e.getMessage());
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

    private void applyFilter(String text) {
        String filter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            dataProvider.clearFilters();
        } else {
            dataProvider.setFilter(item -> contains(item.getCode(), filter)
                    || contains(item.getName(), filter) || contains(item.getDescription(), filter));
        }
    }

    private static boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private void applyWidth(int width) {
        boolean compact = width < COMPACT_WIDTH;
        descriptionColumn.setVisible(!compact);
        orderColumn.setVisible(!compact);
        grid.setDetailsVisibleOnClick(compact);
        pdfButton.setText(compact ? "" : PDF_LABEL);
    }
}
