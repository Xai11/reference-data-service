package com.example.referencedata.view.components;

import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.service.DictionaryItemService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.function.Consumer;

public class ItemFormDialog extends Dialog {

    private final TextField code = new TextField("Код");
    private final TextField name = new TextField("Название");
    private final TextArea description = new TextArea("Описание");
    private final IntegerField sortOrder = new IntegerField("Порядок");
    private final Checkbox active = new Checkbox("Активен");

    private final BeanValidationBinder<DictionaryItem> binder = new BeanValidationBinder<>(DictionaryItem.class);
    private final DictionaryItem bean = new DictionaryItem();

    /** @param existing редактируемый элемент или {@code null} для создания нового */
    public ItemFormDialog(Long dictionaryId, DictionaryItem existing, DictionaryItemService service,
                          Consumer<DictionaryItem> onSaved) {
        setHeaderTitle(existing == null ? "Новый элемент" : "Редактирование элемента");
        setWidth("32rem");
        setMaxWidth("100%");

        if (existing != null) {
            bean.setId(existing.getId());
            bean.setCode(existing.getCode());
            bean.setName(existing.getName());
            bean.setDescription(existing.getDescription());
            bean.setSortOrder(existing.getSortOrder());
            bean.setActive(existing.isActive());
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
        binder.readBean(bean);

        FormLayout form = new FormLayout(code, name, description, sortOrder, active);
        form.setColspan(description, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("30rem", 2));
        add(form);

        Button save = new Button("Сохранить", e -> save(dictionaryId, service, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    private void save(Long dictionaryId, DictionaryItemService service, Consumer<DictionaryItem> onSaved) {
        try {
            binder.writeBean(bean);
        } catch (ValidationException e) {
            return;
        }
        try {
            DictionaryItem saved = bean.getId() == null
                    ? service.create(dictionaryId, bean)
                    : service.update(bean.getId(), bean);
            close();
            onSaved.accept(saved);
        } catch (DuplicateCodeException e) {
            code.setInvalid(true);
            code.setErrorMessage("Элемент с таким кодом уже есть в справочнике");
        }
    }
}
