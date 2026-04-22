package com.innovationai.myapplication.util;

import com.innovationai.myapplication.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical built-in movie catalog used for local defaults and Firebase bootstrap.
 */
public final class DefaultMovieCatalog {
    private static final Map<String, Movie> DEFAULT_MOVIES = createDefaultMovies();

    private DefaultMovieCatalog() {
    }

    public static List<Movie> getDefaultMovies() {
        List<Movie> movies = new ArrayList<>();
        for (Movie movie : DEFAULT_MOVIES.values()) {
            movies.add(copyOf(movie));
        }
        return movies;
    }

    public static Movie normalizeMovie(Movie movie) {
        if (movie == null) {
            return null;
        }

        Movie canonicalMovie = movie.getId() == null ? null : DEFAULT_MOVIES.get(movie.getId());
        if (canonicalMovie != null) {
            movie.setTitle(canonicalMovie.getTitle());
            movie.setDescription(canonicalMovie.getDescription());
            movie.setPrice(canonicalMovie.getPrice());
            movie.setPosterUrl(canonicalMovie.getPosterUrl());
            movie.setPreviewVideoUrl(canonicalMovie.getPreviewVideoUrl());
            movie.setGenre(canonicalMovie.getGenre());
            movie.setRating(canonicalMovie.getRating());
            movie.setDirector(canonicalMovie.getDirector());
            movie.setCast(canonicalMovie.getCast());
            return movie;
        }

        movie.setGenre(normalizeGenre(movie.getGenre()));
        return movie;
    }

    public static String normalizeGenre(String genre) {
        String normalizedGenre = genre == null ? "" : genre.trim();
        String lowerGenre = normalizedGenre.toLowerCase(Locale.ROOT);
        if (lowerGenre.contains("action") || lowerGenre.contains("动作")) {
            return "Action";
        }
        if (lowerGenre.contains("comedy") || lowerGenre.contains("喜剧")) {
            return "Comedy";
        }
        if (lowerGenre.contains("drama") || lowerGenre.contains("剧情")) {
            return "Drama";
        }
        if (lowerGenre.contains("other") || lowerGenre.contains("其他")) {
            return "Other";
        }
        return normalizedGenre.isEmpty() ? "Other" : normalizedGenre;
    }

    public static String normalizeOrderStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim();
        if (normalizedStatus.isEmpty()) {
            return "Completed";
        }

        String lowerStatus = normalizedStatus.toLowerCase(Locale.ROOT);
        if (lowerStatus.contains("completed") || lowerStatus.contains("已完成")) {
            return "Completed";
        }
        return normalizedStatus;
    }

    private static Map<String, Movie> createDefaultMovies() {
        Map<String, Movie> movies = new LinkedHashMap<>();
        movies.put("movie_avengers4", createMovie(
                "movie_avengers4",
                "Avengers: Endgame",
                "Earth's mightiest heroes assemble for one final battle against Thanos.",
                150,
                MovieMediaUtil.drawableRef("avengers4"),
                MovieMediaUtil.rawRef("avenger_trailer"),
                "Action",
                8.5f,
                "Anthony and Joe Russo",
                "Robert Downey Jr., Chris Evans"
        ));
        movies.put("movie_fast9", createMovie(
                "movie_fast9",
                "F9",
                "Dom and his family face a dangerous new threat from the past.",
                120,
                MovieMediaUtil.drawableRef("fast_and_furious"),
                MovieMediaUtil.rawRef("fastandfurious_trailer"),
                "Action",
                7.2f,
                "Justin Lin",
                "Vin Diesel, Michelle Rodriguez"
        ));
        movies.put("movie_hangover", createMovie(
                "movie_hangover",
                "The Hangover",
                "A wild bachelor party in Las Vegas spirals into unforgettable chaos.",
                80,
                MovieMediaUtil.drawableRef("ic_launcher_foreground"),
                MovieMediaUtil.rawRef("seabird1"),
                "Comedy",
                7.8f,
                "Todd Phillips",
                "Bradley Cooper, Ed Helms"
        ));
        movies.put("movie_shawshank", createMovie(
                "movie_shawshank",
                "The Shawshank Redemption",
                "A banker finds hope and friendship while serving a life sentence in prison.",
                100,
                MovieMediaUtil.drawableRef("ic_launcher_foreground"),
                MovieMediaUtil.rawRef("seabird1"),
                "Drama",
                9.7f,
                "Frank Darabont",
                "Tim Robbins, Morgan Freeman"
        ));
        return movies;
    }

    private static Movie createMovie(String id, String title, String description, int price,
                                     String posterUrl, String previewVideoUrl, String genre,
                                     float rating, String director, String cast) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setPrice(price);
        movie.setPosterUrl(posterUrl);
        movie.setPreviewVideoUrl(previewVideoUrl);
        movie.setGenre(genre);
        movie.setRating(rating);
        movie.setDirector(director);
        movie.setCast(cast);
        return movie;
    }

    private static Movie copyOf(Movie source) {
        Movie movie = new Movie();
        movie.setId(source.getId());
        movie.setTitle(source.getTitle());
        movie.setDescription(source.getDescription());
        movie.setPrice(source.getPrice());
        movie.setPosterUrl(source.getPosterUrl());
        movie.setPreviewVideoUrl(source.getPreviewVideoUrl());
        movie.setGenre(source.getGenre());
        movie.setRating(source.getRating());
        movie.setDirector(source.getDirector());
        movie.setCast(source.getCast());
        return movie;
    }
}
