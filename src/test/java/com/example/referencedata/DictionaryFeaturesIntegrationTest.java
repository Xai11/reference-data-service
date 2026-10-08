package com.example.referencedata;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Сквозная проверка миграций, маппинга EclipseLink и бизнес-правил иерархии, периода, атрибутов и ссылок. */
class DictionaryFeaturesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        var builder = request.with(user("editor").roles("EDITOR"));
        if (body != null) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(builder);
    }

    private long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void hierarchyPeriodAttributesAndReferencesWorkEndToEnd() throws Exception {
        long countries = id(send(post("/api/dictionaries"),
                "{\"code\":\"COUNTRY\",\"name\":\"Страны\",\"active\":true}").andExpect(status().isCreated()));
        long regions = id(send(post("/api/dictionaries"),
                "{\"code\":\"REGION\",\"name\":\"Регионы\",\"active\":true,\"hierarchical\":true}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hierarchical").value(true)));

        long russia = id(send(post("/api/dictionaries/" + countries + "/items"),
                "{\"code\":\"RU\",\"name\":\"Россия\",\"sortOrder\":1,\"active\":true}").andExpect(status().isCreated()));

        // атрибуты: число и ссылка на справочник стран
        long population = id(send(post("/api/dictionaries/" + regions + "/attributes"),
                "{\"code\":\"population\",\"name\":\"Население\",\"type\":\"NUMBER\",\"required\":true,\"sortOrder\":1}")
                .andExpect(status().isCreated()));
        send(post("/api/dictionaries/" + regions + "/attributes"),
                "{\"code\":\"country\",\"name\":\"Страна\",\"type\":\"REFERENCE\",\"required\":false,\"sortOrder\":2,"
                        + "\"refDictionaryId\":" + countries + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.refDictionaryId").value(countries));
        send(post("/api/dictionaries/" + regions + "/attributes"),
                "{\"code\":\"bad\",\"name\":\"Без цели\",\"type\":\"REFERENCE\",\"sortOrder\":3}")
                .andExpect(status().isBadRequest());

        // корневой элемент: обязательный атрибут обязателен
        send(post("/api/dictionaries/" + regions + "/items"),
                "{\"code\":\"ROOT\",\"name\":\"Корень\",\"sortOrder\":1,\"active\":true}")
                .andExpect(status().isBadRequest());
        long root = id(send(post("/api/dictionaries/" + regions + "/items"),
                "{\"code\":\"ROOT\",\"name\":\"Корень\",\"sortOrder\":1,\"active\":true,"
                        + "\"attributes\":{\"population\":1500000.5,\"country\":\"RU\"}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attributes.population").value(1500000.5))
                .andExpect(jsonPath("$.attributes.country").value("RU")));

        // потомок с периодом действия
        long child = id(send(post("/api/dictionaries/" + regions + "/items"),
                "{\"code\":\"CHILD\",\"name\":\"Потомок\",\"sortOrder\":2,\"active\":true,\"parentId\":" + root
                        + ",\"validFrom\":\"2026-01-01\",\"validTo\":\"2026-12-31\","
                        + "\"attributes\":{\"population\":10}}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.parentId").value(root)));

        // дерево и фильтр по дате
        send(get("/api/dictionaries/" + regions + "/items?tree=true"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("ROOT", "CHILD")))
                .andExpect(jsonPath("$[1].level").value(1));
        send(get("/api/dictionaries/" + regions + "/items?asOf=2026-06-01"), null)
                .andExpect(jsonPath("$", hasSize(2)));
        send(get("/api/dictionaries/" + regions + "/items?asOf=2027-06-01"), null)
                .andExpect(jsonPath("$[*].code", contains("ROOT")));

        // правила: цикл, период, родитель в плоском справочнике
        send(put("/api/items/" + root),
                "{\"code\":\"ROOT\",\"name\":\"Корень\",\"sortOrder\":1,\"active\":true,\"parentId\":" + child + "}")
                .andExpect(status().isBadRequest());
        send(post("/api/dictionaries/" + regions + "/items"),
                "{\"code\":\"X\",\"name\":\"X\",\"sortOrder\":1,\"active\":true,\"validFrom\":\"2026-05-01\","
                        + "\"validTo\":\"2026-04-01\",\"attributes\":{\"population\":1}}")
                .andExpect(status().isBadRequest());
        send(post("/api/dictionaries/" + countries + "/items"),
                "{\"code\":\"BY\",\"name\":\"Беларусь\",\"sortOrder\":2,\"active\":true,\"parentId\":" + russia + "}")
                .andExpect(status().isBadRequest());

        // защита удаления и смены типа
        send(delete("/api/items/" + root), null).andExpect(status().isConflict());
        send(delete("/api/items/" + russia), null).andExpect(status().isConflict());
        send(delete("/api/dictionaries/" + countries), null).andExpect(status().isConflict());
        send(put("/api/attributes/" + population),
                "{\"code\":\"population\",\"name\":\"Население\",\"type\":\"STRING\",\"required\":true,\"sortOrder\":1}")
                .andExpect(status().isConflict());

        // нельзя отключить иерархию, пока есть вложенность
        send(put("/api/dictionaries/" + regions),
                "{\"code\":\"REGION\",\"name\":\"Регионы\",\"active\":true,\"hierarchical\":false}")
                .andExpect(status().isConflict());

        // обновление значений атрибутов
        send(put("/api/items/" + child),
                "{\"code\":\"CHILD\",\"name\":\"Потомок\",\"sortOrder\":2,\"active\":true,\"parentId\":" + root
                        + ",\"attributes\":{\"population\":20}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.attributes.population").value(20));

        // удаление: сначала потомок, затем корень, затем справочники
        send(delete("/api/items/" + child), null).andExpect(status().isNoContent());
        send(delete("/api/items/" + root), null).andExpect(status().isNoContent());
        // «Страны» нельзя удалить, пока на них ссылается атрибут справочника «Регионы»
        send(delete("/api/dictionaries/" + countries), null).andExpect(status().isConflict());
        send(delete("/api/dictionaries/" + regions), null).andExpect(status().isNoContent());
        send(delete("/api/dictionaries/" + countries), null).andExpect(status().isNoContent());
    }

    @Test
    void deletingDictionaryRemovesItemsWithParentsAndInternalReferences() throws Exception {
        long dictionary = id(send(post("/api/dictionaries"),
                "{\"code\":\"TREE\",\"name\":\"Дерево\",\"active\":true,\"hierarchical\":true}")
                .andExpect(status().isCreated()));
        send(post("/api/dictionaries/" + dictionary + "/attributes"),
                "{\"code\":\"related\",\"name\":\"Связанный\",\"type\":\"REFERENCE\",\"sortOrder\":1,"
                        + "\"refDictionaryId\":" + dictionary + "}").andExpect(status().isCreated());
        long a = id(send(post("/api/dictionaries/" + dictionary + "/items"),
                "{\"code\":\"A\",\"name\":\"A\",\"sortOrder\":1,\"active\":true}").andExpect(status().isCreated()));
        send(post("/api/dictionaries/" + dictionary + "/items"),
                "{\"code\":\"B\",\"name\":\"B\",\"sortOrder\":2,\"active\":true,\"parentId\":" + a
                        + ",\"attributes\":{\"related\":\"A\"}}").andExpect(status().isCreated());

        send(delete("/api/dictionaries/" + dictionary), null).andExpect(status().isNoContent());
        send(get("/api/dictionaries/" + dictionary), null).andExpect(status().isNotFound());
    }
}
