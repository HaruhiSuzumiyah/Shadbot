package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;
import reactor.util.annotation.Nullable;

public class DBMemberBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("coins")
    private Long coins;
    @Nullable
    @JsonProperty("mutes")
    private Long mutes;
    @Nullable
    @JsonProperty("weirdchamps")
    private Long weirdchamps;
    @Nullable
    @JsonProperty("freecoins")
    private Boolean freecoins;

    public DBMemberBean(String id, @Nullable Long coins, @Nullable Long mutes, @Nullable Long weirdchamps,
                        @Nullable Boolean freecoins) {
        this.id = id;
        this.coins = coins;
        this.mutes = mutes;
        this.weirdchamps = weirdchamps;
        this.freecoins = freecoins;
    }

    public DBMemberBean(String id) {
        this(id, null, null, null, true);
    }

    public DBMemberBean() {
    }

    public String getId() {
        return this.id;
    }

    public long getCoins() {
        return this.coins == null ? 0 : this.coins;
    }

    public long getMutes() {
        return this.mutes == null ? 0 : this.mutes;
    }

    public long getWeirdchamps() {
        return this.weirdchamps == null ? 0 : this.weirdchamps;
    }

    public boolean getFreecoins() {
        return this.freecoins == null || this.freecoins;
    }

    @Override
    public String toString() {
        return "DBMemberBean{" +
                "id=" + this.id +
                ", coins=" + this.coins +
                ", mutes=" + this.mutes +
                ", weirdchamps=" + this.weirdchamps +
                ", weirdchamps=" + this.freecoins +
                '}';
    }
}
