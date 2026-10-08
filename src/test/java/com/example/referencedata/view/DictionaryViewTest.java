package com.example.referencedata.view;

import com.example.referencedata.AbstractIntegrationTest;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.service.DictionaryAttributeService;
import com.example.referencedata.service.DictionaryItemService;
import com.example.referencedata.service.DictionaryService;
import com.github.mvysny.fakeservlet.FakeRequest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.UtilsKt;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.server.VaadinRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка серверной части интерфейса без браузера (Karibu Testing) на реальной БД. */
class DictionaryViewTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext ctx;
    @Autowired
    DictionaryService dictionaries;
    @Autowired
    DictionaryAttributeService attributes;
    @Autowired
    DictionaryItemService items;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("editor", "x", "ROLE_EDITOR"));
        Routes routes = new Routes().autoDiscoverViews("com.example.referencedata");
        MockVaadin.setup(UI::new, new MockSpringServlet(routes, ctx, UI::new));

        // Vaadin проверяет доступ к @PermitAll по принципалу запроса — подставляем редактора
        FakeRequest request = UtilsKt.getFake(VaadinRequest.getCurrent());
        request.setUserPrincipalInt(SecurityContextHolder.getContext().getAuthentication());
        request.setUserInRole((principal, role) -> "EDITOR".equals(role));
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rendersHierarchicalDictionaryWithAttributes() {
        Dictionary region = new Dictionary();
        region.setCode("UI_REGION");
        region.setName("UI Регионы");
        region.setHierarchical(true);
        region = dictionaries.create(region);

        DictionaryAttribute population = new DictionaryAttribute();
        population.setCode("population");
        population.setName("Население");
        population.setType(AttributeType.NUMBER);
        attributes.create(region.getId(), population, null);

        DictionaryItem root = new DictionaryItem();
        root.setCode("ROOT");
        root.setName("Корень");
        DictionaryItem savedRoot = items.create(region.getId(), root, null, Map.of("population", 100));
        DictionaryItem child = new DictionaryItem();
        child.setCode("CHILD");
        child.setName("Потомок");
        items.create(region.getId(), child, savedRoot.getId(), Map.of());

        UI.getCurrent().navigate("dictionary/UI_REGION");

        assertEquals("UI Регионы", _get(H2.class).getText());
        assertTrue(_get(Tabs.class).isVisible());
        TreeGrid<?> grid = _get(TreeGrid.class);
        // колонки: код, название, описание, порядок, население, период, статус, действия
        assertEquals(8, grid.getColumns().size());
    }

    /** Создаёт иерархический справочник с обязательным числовым атрибутом и корневым элементом. */
    private Dictionary regionWithRoot(String code) {
        Dictionary region = new Dictionary();
        region.setCode(code);
        region.setName(code + " name");
        region.setHierarchical(true);
        region = dictionaries.create(region);

        DictionaryAttribute population = new DictionaryAttribute();
        population.setCode("population");
        population.setName("Население");
        population.setType(AttributeType.NUMBER);
        population.setRequired(true);
        attributes.create(region.getId(), population, null);

        DictionaryItem root = new DictionaryItem();
        root.setCode("ROOT");
        root.setName("Корень");
        items.create(region.getId(), root, null, Map.of("population", 100));
        return region;
    }

    @Test
    void addItemDialogPersistsItemWithParentAndAttribute() {
        Dictionary region = regionWithRoot("UI_ADD");
        UI.getCurrent().navigate("dictionary/UI_ADD");

        _click(_get(Button.class, spec -> spec.withText("Добавить элемент")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Код")), "NEW");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Название")), "Новый");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Порядок")), 5);
        _setValue(_get(BigDecimalField.class, spec -> spec.withLabel("Население")), new BigDecimal("42"));
        @SuppressWarnings("unchecked")
        ComboBox<DictionaryItem> parent = _get(ComboBox.class, spec -> spec.withLabel("Родитель"));
        _setValue(parent, parent.getListDataView().getItems()
                .filter(i -> "ROOT".equals(i.getCode())).findFirst().orElseThrow());
        _click(_get(Button.class, spec -> spec.withText("Сохранить")));

        DictionaryItem saved = items.getByCode(region.getId(), "NEW");
        assertEquals("Новый", saved.getName());
        assertEquals("ROOT", saved.getParent().getCode());
        assertEquals(0, new BigDecimal("42").compareTo(saved.getValues().get(0).getValueNumber()));
    }

    @Test
    void dialogKeepsOpenWhenRequiredAttributeIsMissing() {
        Dictionary region = regionWithRoot("UI_REQ");
        UI.getCurrent().navigate("dictionary/UI_REQ");

        _click(_get(Button.class, spec -> spec.withText("Добавить элемент")));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Код")), "NOATTR");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Название")), "Без атрибута");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Порядок")), 1);
        _click(_get(Button.class, spec -> spec.withText("Сохранить")));

        assertTrue(_get(BigDecimalField.class, spec -> spec.withLabel("Население")).isInvalid());
        assertTrue(items.findByDictionary(region.getId(), false).stream()
                .noneMatch(i -> "NOATTR".equals(i.getCode())));
    }

    @Test
    void attributesTabListsAttributes() {
        regionWithRoot("UI_ATTR");
        UI.getCurrent().navigate("dictionary/UI_ATTR");

        _get(Tabs.class).setSelectedIndex(1);

        @SuppressWarnings("rawtypes")
        List<Grid> visibleGrids = _find(Grid.class);
        assertEquals(1, visibleGrids.size());
        assertEquals(1, visibleGrids.get(0).getListDataView().getItemCount());
    }

    @Test
    void viewerDoesNotSeeEditControls() {
        regionWithRoot("UI_VIEW");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("viewer", "x", "ROLE_VIEWER"));
        UI.getCurrent().navigate("dictionary/UI_VIEW");

        assertEquals("UI_VIEW name", _get(H2.class).getText());
        assertTrue(_find(Button.class, spec -> spec.withText("Добавить элемент")).isEmpty());
        assertTrue(_find(Button.class, spec -> spec.withText("Новый справочник")).isEmpty());
    }

    @Test
    void showsStubForUnknownDictionary() {
        UI.getCurrent().navigate("dictionary/NO_SUCH");

        assertEquals("Справочник не найден", _get(H2.class).getText());
    }
}
