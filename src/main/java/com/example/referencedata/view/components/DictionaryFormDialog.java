package com.example.referencedata.view.components;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.service.DictionaryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.function.Consumer;

public class DictionaryFormDialog extends Dialog {

    private final TextField code = new TextField("Код");
    private final TextField name = new TextField("Название");
    private final TextArea description = new TextArea("Описание");
    private final Checkbox active = new Checkbox("Активен");
    private final Checkbox hierarchical = new Checkbox("Иерархический (элементы могут иметь родителя)");

    private final BeanValidationBinder<Dictionary> binder = new BeanValidationBinder<>(Dictionary.class);
    private final Dictionary bean = new Dictionary();

    /** @param existing редактируемый справочник или {@code null} для создания нового */
    public DictionaryFormDialog(Dictionary existing, DictionaryService service, Consumer<Dictionary> onSaved) {
        setHeaderTitle(existing == null ? "Новый справочник" : "Редактирование справочника");
        setWidth("32rem");
        setMaxWidth("100%");

        if (existing != null) {
            bean.setId(existing.getId());
            bean.setCode(existing.getCode());
            bean.setName(existing.getName());
            bean.setDescription(existing.getDescription());
            bean.setActive(existing.isActive());
            bean.setHierarchical(existing.isHierarchical());
        }

        code.setMaxLength(100);
        name.setMaxLength(255);
        description.setMaxLength(1000);
        description.setMinHeight("6rem");

        binder.forField(code).asRequired("Укажите код").bind("code");
        binder.forField(name).asRequired("Укажите название").bind("name");
        binder.forField(description).bind("description");
        binder.forField(active).bind("active");
        binder.forField(hierarchical).bind("hierarchical");
        binder.readBean(bean);

        FormLayout form = new FormLayout(code, name, description, active, hierarchical);
        form.setColspan(description, 2);
        form.setColspan(hierarchical, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("30rem", 2));
        add(form);

        Button save = new Button("Сохранить", e -> save(service, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    private void save(DictionaryService service, Consumer<Dictionary> onSaved) {
        try {
            binder.writeBean(bean);
        } catch (ValidationException e) {
            return;
        }
        try {
            Dictionary saved = bean.getId() == null ? service.create(bean) : service.update(bean.getId(), bean);
            close();
            onSaved.accept(saved);
        } catch (DuplicateCodeException e) {
            code.setInvalid(true);
            code.setErrorMessage("Справочник с таким кодом уже существует");
        } catch (ConflictException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
