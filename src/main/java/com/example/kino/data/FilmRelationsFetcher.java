package com.example.kino.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FilmRelationsFetcher {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<Integer, Set<Integer>> fetchFilmGenres(Set<Integer> filmIds) {
        return fetchRelation("SELECT filmid, genreid FROM public.filmgenres WHERE filmid = ANY (?1)", filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmTags(Set<Integer> filmIds) {
        return fetchRelation("SELECT filmid, tagid FROM public.filmtags WHERE filmid = ANY (?1)", filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmActors(Set<Integer> filmIds) {
        return fetchRelation("SELECT filmid, actorid FROM public.filmactors WHERE filmid = ANY (?1)", filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmDirectors(Set<Integer> filmIds) {
        return fetchRelation("SELECT filmid, directorid FROM public.filmdirectors WHERE filmid = ANY (?1)", filmIds);
    }

    private Map<Integer, Set<Integer>> fetchRelation(String sql, Set<Integer> filmIds) {
        if (filmIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, filmIds.toArray())
                .getResultList();

        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (Object[] row : results) {
            Integer filmId = ((Number) row[0]).intValue();
            Integer relatedId = ((Number) row[1]).intValue();
            map.computeIfAbsent(filmId, k -> new HashSet<>()).add(relatedId);
        }
        return map;
    }
}
