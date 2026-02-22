package com.catalog.repository.elasticsearch;

import com.catalog.domain.model.ProductDocument;
import com.catalog.dto.request.ProductSearchFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductSearchRepository {

    private final ElasticsearchOperations esOperations;

    public Page<ProductDocument> search(ProductSearchFilter filter) {
        Pageable pageable = filter.toPageable();
        List<Query> mustClauses   = new ArrayList<>();
        List<Query> filterClauses = new ArrayList<>();

        filterClauses.add(TermQuery.of(t -> t.field("deleted").value(false))._toQuery());

        if (isNotBlank(filter.getQ())) {
            mustClauses.add(MultiMatchQuery.of(m -> m
                    .query(filter.getQ())
                    .fields("name^2", "description")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))._toQuery());
        }

        if (isNotBlank(filter.getCategory())) {
            filterClauses.add(TermQuery.of(t -> t.field("category").value(filter.getCategory()))._toQuery());
        }

        if (filter.getStatus() != null) {
            filterClauses.add(TermQuery.of(t -> t.field("status").value(filter.getStatus().name()))._toQuery());
        }

        if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
            filterClauses.add(RangeQuery.of(r -> {
                var b = r.field("price");
                if (filter.getMinPrice() != null) b.gte(JsonData.of(filter.getMinPrice().doubleValue()));
                if (filter.getMaxPrice() != null) b.lte(JsonData.of(filter.getMaxPrice().doubleValue()));
                return b;
            })._toQuery());
        }

        Query boolQuery = BoolQuery.of(b -> b.must(mustClauses).filter(filterClauses))._toQuery();

        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(boolQuery).withPageable(pageable).build();

        SearchHits<ProductDocument> hits = esOperations.search(nativeQuery, ProductDocument.class);
        List<ProductDocument> docs = hits.getSearchHits().stream().map(SearchHit::getContent).toList();

        log.debug("ES search: {} hits (total: {})", docs.size(), hits.getTotalHits());
        return new PageImpl<>(docs, pageable, hits.getTotalHits());
    }

    private boolean isNotBlank(String v) { return v != null && !v.isBlank(); }
}
