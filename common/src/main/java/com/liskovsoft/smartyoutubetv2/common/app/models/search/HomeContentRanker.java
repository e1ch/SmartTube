package com.liskovsoft.smartyoutubetv2.common.app.models.search;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;

import java.util.*;

/**
 * Re-ranks MediaGroup items to maximize diversity and freshness.
 * Uses heuristic scoring (no ML) suitable for low-power TV hardware.
 *
 * Scoring: baseScore + freshnessBoost + noveltyBoost - channelPenalty - topicPenalty
 */
public class HomeContentRanker {

    private static final int MIN_GROUP_SIZE = 3;
    private static final int HIGH_CHANNEL_THRESHOLD = 4;
    private static final float HIGH_CHANNEL_PENALTY = -0.6f;
    private static final float MEDIUM_CHANNEL_PENALTY = -0.3f;
    private static final float TOPIC_SATURATION_RATIO = 0.25f;
    private static final float TOPIC_PENALTY = -0.4f;
    private static final float NOVELTY_BOOST = 0.5f;

    private final Map<String, Integer> channelCounts = new HashMap<>();
    private final Map<String, Integer> topicCounts = new HashMap<>();
    private final Set<String> knownChannels = new HashSet<>();
    private int totalItems = 0;

    public void rankGroup(MediaGroup group) {
        if (group == null || group.getMediaItems() == null || group.getMediaItems().size() <= MIN_GROUP_SIZE) {
            return;
        }

        List<MediaItem> items = group.getMediaItems();
        List<ScoredItem> scored = new ArrayList<>();

        for (MediaItem item : items) {
            if (item == null) continue;
            scored.add(new ScoredItem(item, calculateScore(item)));
        }

        // Sort by score descending
        Collections.sort(scored, (a, b) -> Float.compare(b.score, a.score));

        // Update tracking
        for (ScoredItem si : scored) {
            String chId = si.item.getChannelId();
            if (chId != null && !chId.isEmpty()) {
                channelCounts.merge(chId, 1, Integer::sum);
                knownChannels.add(chId);
            }
            String topic = inferTopic(si.item);
            topicCounts.merge(topic, 1, Integer::sum);
            totalItems++;
        }

        // Replace items
        items.clear();
        for (ScoredItem si : scored) {
            items.add(si.item);
        }
    }

    private float calculateScore(MediaItem item) {
        float score = 1.0f; // base

        String channelId = item.getChannelId() != null ? item.getChannelId() : "";
        String topic = inferTopic(item);

        int chCount = channelCounts.getOrDefault(channelId, 0);
        if (chCount >= HIGH_CHANNEL_THRESHOLD) score += HIGH_CHANNEL_PENALTY;
        else if (chCount >= 2) score += MEDIUM_CHANNEL_PENALTY;

        int topCount = topicCounts.getOrDefault(topic, 0);
        if (totalItems > 0 && topCount > 0) {
            float ratio = (float) topCount / totalItems;
            if (ratio > TOPIC_SATURATION_RATIO) score += TOPIC_PENALTY;
        }

        if (!channelId.isEmpty() && !knownChannels.contains(channelId)) {
            score += NOVELTY_BOOST;
        }

        return score;
    }

    /**
     * Infer topic from item metadata using cheap heuristics.
     * No ML needed — uses title keywords + duration bucket.
     */
    private String inferTopic(MediaItem item) {
        String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";

        // Music
        if (title.contains("mv") || title.contains("official music") ||
            title.contains("music video") || title.contains("歌") ||
            title.contains("曲") || title.contains("뮤직") || title.contains("mV")) {
            return "music";
        }

        // Gaming
        if (title.contains("game") || title.contains("gaming") || title.contains("遊戲") ||
            title.contains("実況") || title.contains("게임") || title.contains("minecraft") ||
            title.contains("roblox") || title.contains("fortnite")) {
            return "gaming";
        }

        // News
        if (title.contains("news") || title.contains("新聞") || title.contains("ニュース") ||
            title.contains("뉴스") || title.contains("breaking") || title.contains("live")) {
            return "news";
        }

        // Tech
        if (title.contains("tech") || title.contains("review") || title.contains("開箱") ||
            title.contains("レビュー") || title.contains("unboxing")) {
            return "tech";
        }

        // Food
        if (title.contains("food") || title.contains("cook") || title.contains("美食") ||
            title.contains("먹방") || title.contains("料理")) {
            return "food";
        }

        // Vlog / Travel
        if (title.contains("vlog") || title.contains("travel") || title.contains("旅遊") ||
            title.contains("旅行")) {
            return "vlog";
        }

        // Entertainment / Variety
        if (title.contains("variety") || title.contains("综艺") || title.contains("綜藝") ||
            title.contains("バラエティ") || title.contains("예능")) {
            return "entertainment";
        }

        return "other";
    }

    /** Reset for new session */
    public void reset() {
        channelCounts.clear();
        topicCounts.clear();
        knownChannels.clear();
        totalItems = 0;
    }

    private static class ScoredItem {
        final MediaItem item;
        final float score;

        ScoredItem(MediaItem item, float score) {
            this.item = item;
            this.score = score;
        }
    }
}
