package com.example.referencedata.view.components;

import com.vaadin.flow.component.html.Span;

import java.util.List;

public class ActiveBadge extends Span {

    public ActiveBadge(boolean active) {
        setActive(active);
    }

    public void setActive(boolean active) {
        setText(active ? "Активен" : "Неактивен");
        getElement().getThemeList().clear();
        getElement().getThemeList().addAll(List.of("badge", active ? "success" : "contrast"));
    }
}
