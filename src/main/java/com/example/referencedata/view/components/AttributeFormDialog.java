package com.example.referencedata.view.components;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.service.DictionaryAttributeService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AttributeFormDialog extends Dialog {

    private final TextField code = new TextField("Код");
    private final TextField name = new TextField("Название");
    private final ComboBox<AttributeType> type = new ComboBox<>("Тип");
    private final ComboBox<Dictionary> refDictionary = new ComboBox<>("Справочник-цель");
    private final IntegerField sortOrder = new IntegerField("Порядок");
    private final Checkbox required = new Checkbox("Обязательный");

    private final BeanValidationBinder<DictionaryAttribute> binder = new BeanValidationBinder<>(DictionaryAttribute.class);
    private final DictionaryAttribute bean = new DictionaryAttribute();

    /** @param existing редактируемый атрибут или {@code null} для создания нового */
    public AttributeFormDialog(Long dictionaryId, DictionaryAttribute existing, List<Dictionary> dictionaries,
                               DictionaryAttributeService service, Consumer<DictionaryAttribute> onSaved) {
        setHeaderTitle(existing == null ? "Новый атрибут" : "Редактирование атрибута");
        setWidth("32rem");
        setMaxWidth("100%");

        if (existing != null) {
            bean.setId(existing.getId());
            bean.setCode(existing.getCode());
            bean.setName(existing.getName());
            bean.setType(existing.getType());
            bean.setRequired(existing.isRequired());
            bean.setSortOrder(existing.getSortOrder());
            refDictionary.setValue(dictionaries.stream()
                    .filter(d -> existing.getRefDictionary() != null
                            && d.getId().equals(existing.getRefDictionary().getId()))
                    .findFirst().orElse(null));
        }

        code.setMaxLength(100);
        name.setMaxLength(255);
        type.setItems(Arrays.asList(AttributeType.values()));
        type.setItemLabelGenerator(AttributeType::getLabel);
        type.setAllowCustomValue(false);
        refDictionary.setItems(dictionaries);
        refDictionary.setItemLabelGenerator(Dictionary::getName);
        type.addValueChangeListener(e -> refDictionary.setVisible(e.getValue() == AttributeType.REFERENCE));
        sortOrder.setStepButtonsVisible(true);

        binder.forField(code).asRequired("Укажите код").bind("code");
        binder.forField(name).asRequired("Укажите название").bind("name");
        binder.forField(type).asRequired("Выберите тип").bind("type");
        binder.forField(required).bind("required");
        binder.forField(sortOrder).asRequired("Укажите порядок")
                .bind(DictionaryAttribute::getSortOrder, DictionaryAttribute::setSortOrder);
        binder.readBean(bean);
        refDictionary.setVisible(bean.getType() == AttributeType.REFERENCE);

        FormLayout form = new FormLayout(code, name, type, refDictionary, sortOrder, required);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("30rem", 2));
        add(form);

        Button save = new Button("Сохранить", e -> save(dictionaryId, service, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    private void save(Long dictionaryId, DictionaryAttributeService service, Consumer<DictionaryAttribute> onSaved) {
        try {
            binder.writeBean(bean);
        } catch (ValidationException e) {
            return;
        }
        Long refId = null;
        if (bean.getType() == AttributeType.REFERENCE) {
            if (refDictionary.getValue() == null) {
                refDictionary.setInvalid(true);
                refDictionary.setErrorMessage("Выберите справочник");
                return;
            }
            refId = refDictionary.getValue().getId();
        }
        try {
            DictionaryAttribute saved = bean.getId() == null
                    ? service.create(dictionaryId, bean, refId)
                    : service.update(bean.getId(), bean, refId);
            close();
            onSaved.accept(saved);
        } catch (DuplicateCodeException e) {
            code.setInvalid(true);
            code.setErrorMessage("Атрибут с таким кодом уже существует");
        } catch (ConflictException | InvalidDataException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
