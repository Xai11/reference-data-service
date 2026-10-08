package com.example.referencedata.view.components;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class EmptyState extends VerticalLayout {

    private final H3 title = new H3();
    private final Span hint = new Span();

    public EmptyState() {
        addClassNames("card", "empty-state");
        setPadding(false);
        add(VaadinIcon.FOLDER_OPEN_O.create(), title, hint);
    }

    public void set(String titleText, String hintText) {
        title.setText(titleText);
        hint.setText(hintText);
    }
}
