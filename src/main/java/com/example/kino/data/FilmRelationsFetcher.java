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
        String sql = "SELECT filmid, genreid FROM public.filmgenres WHERE filmid IN (:filmIds)";
        return fetchRelation(sql, filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmTags(Set<Integer> filmIds) {
        String sql = "SELECT filmid, tagid FROM public.filmtags WHERE filmid IN (:filmIds)";
        return fetchRelation(sql, filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmActors(Set<Integer> filmIds) {
        String sql = "SELECT filmid, actorid FROM public.filmactors WHERE filmid IN (:filmIds)";
        return fetchRelation(sql, filmIds);
    }

    public Map<Integer, Set<Integer>> fetchFilmDirectors(Set<Integer> filmIds) {
        String sql = "SELECT filmid, directorid FROM public.filmdirectors WHERE filmid IN (:filmIds)";
        return fetchRelation(sql, filmIds);
    }

    private Map<Integer, Set<Integer>> fetchRelation(String sql, Set<Integer> filmIds) {
        if (filmIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("filmIds", filmIds)
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
