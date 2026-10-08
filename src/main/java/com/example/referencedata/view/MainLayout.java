package com.example.referencedata.view;

import com.example.referencedata.model.Dictionary;
import com.example.referencedata.service.DictionaryService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.theme.lumo.Lumo;

import java.util.Locale;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private static final String THEME_KEY = "reference-data-theme";

    private final DictionaryService dictionaryService;
    private final SideNav nav = new SideNav();
    private final TextField search = new TextField();
    private final Button themeToggle = new Button();

    public MainLayout(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;

        setPrimarySection(Section.DRAWER);

        H1 title = new H1("Справочники");
        title.addClassName("app-title");

        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getElement().setAttribute("aria-label", "Переключить тему");
        themeToggle.addClickListener(e -> applyTheme(!isDark(), true));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, themeToggle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.setPadding(false);
        header.getStyle().set("padding-inline", "var(--lumo-space-s)");
        addToNavbar(header);

        search.setPlaceholder("Поиск справочника");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> refreshNav());

        VerticalLayout drawer = new VerticalLayout(search, nav);
        drawer.addClassName("app-drawer");
        drawer.setPadding(false);
        addToDrawer(drawer);

        refreshNav();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UI.getCurrent().getPage()
                .executeJs("return localStorage.getItem($0) || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')",
                        THEME_KEY)
                .then(String.class, theme -> applyTheme("dark".equals(theme), false));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshNav();
    }

    private void refreshNav() {
        nav.removeAll();
        nav.addItem(new SideNavItem("Все", "", VaadinIcon.LIST.create()));
        String filter = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Dictionary d : dictionaryService.findAll()) {
            if (filter.isEmpty() || d.getName().toLowerCase(Locale.ROOT).contains(filter)) {
                nav.addItem(new SideNavItem(d.getName(), "dictionary/" + d.getCode(), VaadinIcon.BOOK.create()));
            }
        }
    }

    private boolean isDark() {
        return UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
    }

    private void applyTheme(boolean dark, boolean persist) {
        var themes = UI.getCurrent().getElement().getThemeList();
        if (dark) {
            themes.add(Lumo.DARK);
        } else {
            themes.remove(Lumo.DARK);
        }
        themeToggle.setIcon((dark ? VaadinIcon.SUN_O : VaadinIcon.MOON_O).create());
        if (persist) {
            UI.getCurrent().getPage().executeJs("localStorage.setItem($0, $1)", THEME_KEY, dark ? "dark" : "light");
        }
    }
}
