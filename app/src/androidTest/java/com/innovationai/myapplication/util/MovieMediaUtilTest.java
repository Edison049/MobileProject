package com.innovationai.myapplication.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MovieMediaUtilTest {

    @Test
    public void shouldExtractYouTubeVideoIdFromShortUrl() {
        String url = "https://youtu.be/FXJZ3n6x7Nw?si=PR5NpsNeZsTezXhK";

        assertTrue(MovieMediaUtil.isYouTubeUrl(url));
        assertEquals("FXJZ3n6x7Nw", MovieMediaUtil.extractYouTubeVideoId(url));
        assertEquals(
                "https://www.youtube-nocookie.com/embed/FXJZ3n6x7Nw?autoplay=1&mute=1&playsinline=1&controls=1&rel=0&modestbranding=1&iv_load_policy=3&fs=0&enablejsapi=1",
                MovieMediaUtil.buildYouTubeEmbedUrl(url)
        );
    }

    @Test
    public void shouldRecognizeRemotePosterUrls() {
        assertTrue(MovieMediaUtil.isRemoteUrl("https://example.com/poster.jpg"));
        assertTrue(MovieMediaUtil.isRemoteUrl("http://example.com/poster.jpg"));
        assertTrue(!MovieMediaUtil.isRemoteUrl("file:///tmp/poster.jpg"));
    }
}
