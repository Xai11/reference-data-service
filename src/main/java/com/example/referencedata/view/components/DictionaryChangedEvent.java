package com.example.referencedata.view.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

/** Сообщает интерфейсу, что состав или названия справочников изменились. */
public class DictionaryChangedEvent extends ComponentEvent<UI> {

    public DictionaryChangedEvent(UI source) {
        super(source, false);
    }

    public static void fire(UI ui) {
        ComponentUtil.fireEvent(ui, new DictionaryChangedEvent(ui));
    }
}
