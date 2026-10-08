package com.example.referencedata.view.components;

import com.example.referencedata.model.ItemStatus;
import com.vaadin.flow.component.html.Span;

import java.util.List;

public class ActiveBadge extends Span {

    public ActiveBadge(boolean active) {
        setActive(active);
    }

    public ActiveBadge(ItemStatus status) {
        setStatus(status);
    }

    public void setActive(boolean active) {
        setStatus(active ? ItemStatus.ACTIVE : ItemStatus.INACTIVE);
    }

    public void setStatus(ItemStatus status) {
        setText(status.getLabel());
        getElement().getThemeList().clear();
        getElement().getThemeList().addAll(List.of("badge", switch (status) {
            case ACTIVE -> "success";
            case INACTIVE -> "contrast";
            case NOT_STARTED -> "";
            case EXPIRED -> "error";
        }).stream().filter(t -> !t.isEmpty()).toList());
    }
}
