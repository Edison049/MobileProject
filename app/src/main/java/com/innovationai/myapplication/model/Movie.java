package com.innovationai.myapplication.model;

/**
 * 电影模型类
 * 用于表示系统中的电影信息
 * 对应Firebase Firestore中的movies集合
 */
public class Movie {
    private String id;              // 电影ID (Firestore文档ID)
    private String title;           // 电影标题
    private String description;     // 电影描述/剧情简介
    private int price;              // 租赁价格（积分）
    private String posterUrl;       // 海报图片 URL
    private int posterResourceId;   // 本地海报图片资源 ID（如果是本地图片）
    private boolean isLocalImage;   // 是否使用本地图片资源
    private String previewVideoUrl; // 预告片视频 URL
    private String genre;           // 电影类型
    private float rating;           // 评分 (0.0 - 10.0)
    private String director;        // 导演
    private String cast;            // 主演列表

    // 无参构造函数（Firebase序列化需要）
    public Movie() {
    }

    // 完整构造函数（用于网络图片）
    public Movie(String id, String title, String description, int price, 
                 String posterUrl, String previewVideoUrl, String genre, 
                 float rating, String director, String cast) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.posterUrl = posterUrl;
        this.posterResourceId = 0;
        this.isLocalImage = false;
        this.previewVideoUrl = previewVideoUrl;
        this.genre = genre;
        this.rating = rating;
        this.director = director;
        this.cast = cast;
    }
    
    // 完整构造函数（用于本地图片）
    public Movie(String id, String title, String description, int price, 
                 int posterResourceId, String previewVideoUrl, String genre, 
                 float rating, String director, String cast) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.posterResourceId = posterResourceId;
        this.isLocalImage = true;
        this.posterUrl = null;
        this.previewVideoUrl = previewVideoUrl;
        this.genre = genre;
        this.rating = rating;
        this.director = director;
        this.cast = cast;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
        this.isLocalImage = false;
    }
    
    public int getPosterResourceId() {
        return posterResourceId;
    }
    
    public void setPosterResourceId(int posterResourceId) {
        this.posterResourceId = posterResourceId;
        this.isLocalImage = true;
    }
    
    public boolean isLocalImage() {
        return isLocalImage;
    }

    public String getPreviewVideoUrl() {
        return previewVideoUrl;
    }

    public void setPreviewVideoUrl(String previewVideoUrl) {
        this.previewVideoUrl = previewVideoUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", genre='" + genre + '\'' +
                ", rating=" + rating +
                ", director='" + director + '\'' +
                '}';
    }
}