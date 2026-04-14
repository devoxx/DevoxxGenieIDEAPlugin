package com.devoxx.genie.service.blog;

/**
 * A blog post entry shown on the welcome screen.
 *
 * @param slug        the post slug, used to build the URL (https://genie.devoxx.com/blog/{slug})
 * @param title       the post title
 * @param date        the publication date as ISO string (yyyy-MM-dd)
 * @param description short description / excerpt
 */
public record BlogPost(String slug, String title, String date, String description) {

    public String url() {
        return "https://genie.devoxx.com/blog/" + slug;
    }
}
