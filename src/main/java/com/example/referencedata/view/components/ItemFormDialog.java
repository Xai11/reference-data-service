package com.example.referencedata.view.components;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.model.DictionaryItemValue;
import com.example.referencedata.service.DictionaryItemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemFormDialog extends Dialog {

    /** Поле атрибута: компонент, чтение значения и пометка ошибки. */
    private record AttributeField(DictionaryAttribute attribute, Component component,
                                  Supplier<Object> value, Consumer<String> markInvalid) {
    }

    private final TextField code = new TextField("Код");
    private final TextField name = new TextField("Название");
    private final TextArea description = new TextArea("Описание");
    private final IntegerField sortOrder = new IntegerField("Порядок");
    private final Checkbox active = new Checkbox("Активен");
    private final DatePicker validFrom = new DatePicker("Действует с");
    private final DatePicker validTo = new DatePicker("Действует по");
    private final ComboBox<DictionaryItem> parent = new ComboBox<>("Родитель");

    private final BeanValidationBinder<DictionaryItem> binder = new BeanValidationBinder<>(DictionaryItem.class);
    private final DictionaryItem bean = new DictionaryItem();
    private final List<AttributeField> attributeFields = new ArrayList<>();

    /**
     * @param dictionary справочник элемента (с загруженными атрибутами)
     * @param existing   редактируемый элемент или {@code null} для создания нового
     * @param allItems   все элементы справочника (для выбора родителя)
     */
    public ItemFormDialog(Dictionary dictionary, DictionaryItem existing, List<DictionaryItem> allItems,
                          DictionaryItemService service, Consumer<DictionaryItem> onSaved) {
        setHeaderTitle(existing == null ? "Новый элемент" : "Редактирование элемента");
        setWidth("36rem");
        setMaxWidth("100%");

        if (existing != null) {
            bean.setId(existing.getId());
            bean.setCode(existing.getCode());
            bean.setName(existing.getName());
            bean.setDescription(existing.getDescription());
            bean.setSortOrder(existing.getSortOrder());
            bean.setActive(existing.isActive());
            bean.setValidFrom(existing.getValidFrom());
            bean.setValidTo(existing.getValidTo());
        }

        code.setMaxLength(100);
        name.setMaxLength(255);
        description.setMaxLength(1000);
        description.setMinHeight("6rem");
        sortOrder.setStepButtonsVisible(true);

        binder.forField(code).asRequired("Укажите код").bind("code");
        binder.forField(name).asRequired("Укажите название").bind("name");
        binder.forField(description).bind("description");
        binder.forField(sortOrder).asRequired("Укажите порядок")
                .bind(DictionaryItem::getSortOrder, DictionaryItem::setSortOrder);
        binder.forField(active).bind("active");
        binder.forField(validFrom).bind("validFrom");
        binder.forField(validTo)
                .withValidator(to -> to == null || validFrom.getValue() == null || !to.isBefore(validFrom.getValue()),
                        "Дата окончания раньше даты начала")
                .bind("validTo");
        validFrom.addValueChangeListener(e -> binder.validate());
        binder.readBean(bean);

        List<Component> components = new ArrayList<>(List.<Component>of(code, name, description, sortOrder, active));

        if (dictionary.isHierarchical()) {
            parent.setItems(candidatesForParent(allItems, existing));
            parent.setItemLabelGenerator(i -> i.getCode() + " — " + i.getName());
            parent.setClearButtonVisible(true);
            if (existing != null && existing.getParentId() != null) {
                allItems.stream().filter(i -> i.getId().equals(existing.getParentId()))
                        .findFirst().ifPresent(parent::setValue);
            }
            components.add(parent);
        }
        components.add(validFrom);
        components.add(validTo);

        Map<Long, DictionaryItemValue> current = new LinkedHashMap<>();
        if (existing != null) {
            existing.getValues().forEach(v -> current.put(v.getAttribute().getId(), v));
        }
        for (DictionaryAttribute attribute : dictionary.getAttributes()) {
            AttributeField field = createAttributeField(attribute, current.get(attribute.getId()), service);
            attributeFields.add(field);
            components.add(field.component());
        }

        FormLayout form = new FormLayout(components.toArray(Component[]::new));
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("32rem", 2));
        form.setColspan(description, 2);
        if (dictionary.isHierarchical()) {
            form.setColspan(parent, 2);
        }
        add(form);

        Button save = new Button("Сохранить", e -> save(dictionary, service, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    /** Все элементы, кроме самого редактируемого и его потомков (иначе получится цикл). */
    private static List<DictionaryItem> candidatesForParent(List<DictionaryItem> allItems, DictionaryItem existing) {
        if (existing == null) {
            return allItems;
        }
        Set<Long> excluded = new HashSet<>();
        excluded.add(existing.getId());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (DictionaryItem item : allItems) {
                if (item.getParentId() != null && excluded.contains(item.getParentId())
                        && excluded.add(item.getId())) {
                    changed = true;
                }
            }
        }
        return allItems.stream().filter(i -> !excluded.contains(i.getId())).toList();
    }

    private AttributeField createAttributeField(DictionaryAttribute attribute, DictionaryItemValue value,
                                                DictionaryItemService service) {
        String label = attribute.getName();
        switch (attribute.getType()) {
            case NUMBER -> {
                BigDecimalField field = new BigDecimalField(label);
                field.setRequiredIndicatorVisible(attribute.isRequired());
                if (value != null) {
                    field.setValue(value.getValueNumber());
                }
                return new AttributeField(attribute, field, field::getValue, msg -> invalid(field, msg));
            }
            case BOOLEAN -> {
                Checkbox field = new Checkbox(label);
                field.setValue(value != null && Boolean.TRUE.equals(value.getValueBoolean()));
                return new AttributeField(attribute, field, field::getValue, msg -> { });
            }
            case DATE -> {
                DatePicker field = new DatePicker(label);
                field.setRequiredIndicatorVisible(attribute.isRequired());
                if (value != null) {
                    field.setValue(value.getValueDate());
                }
                return new AttributeField(attribute, field, field::getValue, msg -> invalid(field, msg));
            }
            case REFERENCE -> {
                ComboBox<DictionaryItem> field = new ComboBox<>(label);
                field.setRequiredIndicatorVisible(attribute.isRequired());
                field.setClearButtonVisible(true);
                List<DictionaryItem> targets = service.findByDictionary(attribute.getRefDictionary().getId(), false);
                field.setItems(targets);
                field.setItemLabelGenerator(i -> i.getCode() + " — " + i.getName());
                if (value != null && value.getValueItem() != null) {
                    targets.stream().filter(i -> i.getId().equals(value.getValueItem().getId()))
                            .findFirst().ifPresent(field::setValue);
                }
                return new AttributeField(attribute, field,
                        () -> field.getValue() == null ? null : field.getValue().getCode(),
                        msg -> invalid(field, msg));
            }
            default -> {
                TextField field = new TextField(label);
                field.setRequiredIndicatorVisible(attribute.isRequired());
                field.setMaxLength(2000);
                if (value != null) {
                    field.setValue(value.getValueString() == null ? "" : value.getValueString());
                }
                return new AttributeField(attribute, field, field::getValue, msg -> invalid(field, msg));
            }
        }
    }

    private static void invalid(com.vaadin.flow.component.HasValidation field, String message) {
        field.setInvalid(true);
        field.setErrorMessage(message);
    }

    private void save(Dictionary dictionary, DictionaryItemService service, Consumer<DictionaryItem> onSaved) {
        boolean valid = binder.validate().isOk();

        Map<String, Object> attributes = new LinkedHashMap<>();
        for (AttributeField field : attributeFields) {
            Object value = field.value().get();
            boolean absent = value == null || (value instanceof String s && s.isBlank());
            if (absent && field.attribute().isRequired()) {
                field.markInvalid().accept("Обязательное поле");
                valid = false;
            }
            attributes.put(field.attribute().getCode(), value);
        }
        if (!valid) {
            return;
        }
        try {
            binder.writeBean(bean);
        } catch (ValidationException e) {
            return;
        }

        Long parentId = dictionary.isHierarchical() && parent.getValue() != null ? parent.getValue().getId() : null;
        try {
            DictionaryItem saved = bean.getId() == null
                    ? service.create(dictionary.getId(), bean, parentId, attributes)
                    : service.update(bean.getId(), bean, parentId, attributes);
            close();
            onSaved.accept(saved);
        } catch (DuplicateCodeException e) {
            code.setInvalid(true);
            code.setErrorMessage("Элемент с таким кодом уже есть в справочнике");
        } catch (InvalidDataException | ConflictException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
