package com.example.referencedata.service;

import com.example.referencedata.model.DictionaryItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Разворачивает плоский список элементов в порядок обхода дерева (в глубину). */
public final class ItemTree {

    public record Node(DictionaryItem item, int level) {
    }

    private ItemTree() {
    }

    /**
     * Порядок сиблингов сохраняется как во входном списке. Элементы, чей родитель отсутствует в списке
     * (например, отфильтрован по дате), считаются корневыми.
     */
    public static List<Node> flatten(List<DictionaryItem> items) {
        Set<Long> ids = new HashSet<>();
        items.forEach(i -> ids.add(i.getId()));

        Map<Long, List<DictionaryItem>> children = new HashMap<>();
        List<DictionaryItem> roots = new ArrayList<>();
        for (DictionaryItem item : items) {
            Long parentId = item.getParentId();
            if (parentId == null || !ids.contains(parentId)) {
                roots.add(item);
            } else {
                children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
            }
        }

        List<Node> result = new ArrayList<>(items.size());
        Deque<Node> stack = new ArrayDeque<>();
        for (int i = roots.size() - 1; i >= 0; i--) {
            stack.push(new Node(roots.get(i), 0));
        }
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            result.add(node);
            List<DictionaryItem> kids = children.getOrDefault(node.item().getId(), List.of());
            for (int i = kids.size() - 1; i >= 0; i--) {
                stack.push(new Node(kids.get(i), node.level() + 1));
            }
        }
        return result;
    }
}
