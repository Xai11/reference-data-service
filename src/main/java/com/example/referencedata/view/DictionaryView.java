package com.example.referencedata.view;

import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.service.DictionaryItemService;
import com.example.referencedata.service.DictionaryPdfService;
import com.example.referencedata.service.DictionaryService;
import com.example.referencedata.view.components.ActiveBadge;
import com.example.referencedata.view.components.EmptyState;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dictionary/:code", layout = MainLayout.class)
@PageTitle("Сервис справочников")
public class DictionaryView extends VerticalLayout implements BeforeEnterObserver {

    private static final int COMPACT_WIDTH = 768;
    private static final String PDF_LABEL = "Сформировать PDF";

    private final DictionaryService dictionaryService;
    private final DictionaryItemService itemService;
    private final DictionaryPdfService pdfService;

    private final H2 title = new H2();
    private final Span description = new Span();
    private final ActiveBadge badge = new ActiveBadge(true);
    private final Button pdfButton = new Button(PDF_LABEL, VaadinIcon.DOWNLOAD_ALT.create());
    private final Anchor download = new Anchor();

    private final TextField search = new TextField();
    private final Grid<DictionaryItem> grid = new Grid<>(DictionaryItem.class, false);
    private final Div gridCard = new Div();
    private final EmptyState emptyState = new EmptyState();

    private final Column<DictionaryItem> descriptionColumn;
    private final Column<DictionaryItem> orderColumn;

    private ListDataProvider<DictionaryItem> dataProvider = new ListDataProvider<>(List.of());
    private Registration resizeRegistration;
    private boolean compact;

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

        FlexLayout header = new FlexLayout(titleBlock, badge, download);
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

    private void showAll() {
        title.setText("Все справочники");
        description.setText("Содержимое всех справочников выгружается одним PDF-файлом");
        badge.setVisible(false);
        search.clear();
        gridCard.setVisible(false);
        emptyState.set("Выберите справочник", "Откройте справочник в меню слева или выгрузите все справочники в PDF");
        emptyState.setVisible(true);
        setDownload("all-dictionaries.pdf", pdfService::generateAll);
    }

    private void showDictionary(Dictionary dictionary) {
        title.setText(dictionary.getName());
        description.setText(dictionary.getDescription() == null || dictionary.getDescription().isBlank()
                ? "Код: " + dictionary.getCode()
                : dictionary.getDescription() + " · Код: " + dictionary.getCode());
        badge.setActive(dictionary.isActive());
        badge.setVisible(true);
        search.clear();

        List<DictionaryItem> items = itemService.findByDictionary(dictionary.getId(), false);
        dataProvider = new ListDataProvider<>(items);
        grid.setDataProvider(dataProvider);
        boolean empty = items.isEmpty();
        gridCard.setVisible(!empty);
        emptyState.setVisible(empty);
        emptyState.set("Нет элементов", "В этом справочнике пока нет записей");
        setDownload(dictionary.getCode() + ".pdf", () -> pdfService.generate(dictionary));
    }

    private void showNotFound() {
        title.setText("Справочник не найден");
        description.setText("");
        badge.setVisible(false);
        gridCard.setVisible(false);
        download.setVisible(false);
        emptyState.set("Справочник не найден", "Проверьте ссылку или выберите справочник в меню");
        emptyState.setVisible(true);
    }

    private void setDownload(String fileName, java.util.function.Supplier<byte[]> pdf) {
        download.setVisible(true);
        download.setHref(new StreamResource(fileName, () -> new ByteArrayInputStream(pdf.get())));
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
        compact = width < COMPACT_WIDTH;
        descriptionColumn.setVisible(!compact);
        orderColumn.setVisible(!compact);
        grid.setDetailsVisibleOnClick(compact);
        pdfButton.setText(compact ? "" : PDF_LABEL);
    }
}
