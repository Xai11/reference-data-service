package com.example.referencedata.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dictionary")
public class Dictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean hierarchical;

    @OneToMany(mappedBy = "dictionary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, name ASC")
    private List<DictionaryItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "dictionary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchFetch(BatchFetchType.IN)
    @OrderBy("sortOrder ASC, name ASC")
    private List<DictionaryAttribute> attributes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isHierarchical() { return hierarchical; }
    public void setHierarchical(boolean hierarchical) { this.hierarchical = hierarchical; }

    public List<DictionaryAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<DictionaryAttribute> attributes) { this.attributes = attributes; }

    public List<DictionaryItem> getItems() { return items; }
    public void setItems(List<DictionaryItem> items) { this.items = items; }

    public void addItem(DictionaryItem item) {
        item.setDictionary(this);
        items.add(item);
    }

    public void removeItem(DictionaryItem item) {
        items.remove(item);
        item.setDictionary(null);
    }
}
