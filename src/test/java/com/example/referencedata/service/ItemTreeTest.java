package com.example.referencedata.service;

import com.example.referencedata.model.DictionaryItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTreeTest {

    private static DictionaryItem item(long id, Integer parentId) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setCode("C" + id);
        if (parentId != null) {
            DictionaryItem parent = new DictionaryItem();
            parent.setId(parentId.longValue());
            item.setParent(parent);
        }
        return item;
    }

    private static List<String> render(List<ItemTree.Node> nodes) {
        return nodes.stream().map(n -> n.level() + ":" + n.item().getCode()).toList();
    }

    @Test
    void walksDepthFirstKeepingSiblingOrder() {
        List<DictionaryItem> items = List.of(
                item(1, null), item(2, 1), item(3, null), item(4, 2), item(5, 1));

        assertEquals(List.of("0:C1", "1:C2", "2:C4", "1:C5", "0:C3"), render(ItemTree.flatten(items)));
    }

    @Test
    void itemsWithMissingParentBecomeRoots() {
        List<DictionaryItem> items = List.of(item(2, 1), item(3, 2));

        assertEquals(List.of("0:C2", "1:C3"), render(ItemTree.flatten(items)));
    }

    @Test
    void emptyListGivesEmptyResult() {
        assertEquals(List.of(), ItemTree.flatten(List.of()));
    }
}
