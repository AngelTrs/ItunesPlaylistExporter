package com.projectpremier.itunesplaylistexporter;

/**
 * Created by user on 5/9/17.
 */
public class Song {

    private final String mId;
    private final String mTitle;
    private final String mArtist;
    private final int mDurationSeconds;
    private final String mPath;

    public Song(String id, String name, String artist, int durationSeconds, String path) {
        mId = id;
        mTitle = name;
        mArtist = artist;
        mDurationSeconds = durationSeconds;
        mPath = path;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public int getDurationSeconds() {
        return mDurationSeconds;
    }

    public String getPath() {
        return mPath;
    }
}
