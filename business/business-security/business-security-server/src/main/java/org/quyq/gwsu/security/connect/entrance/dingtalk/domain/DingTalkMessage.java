package org.quyq.gwsu.security.connect.entrance.dingtalk.domain;

import com.google.gson.Gson;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/1/8
 * @description
 */
@Getter
public class DingTalkMessage {

    private MessageType type;

    private String content;

    private DingTalkMessage(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public static SampleTextBuilder sampleText() {
        return new SampleTextBuilder();
    }

    public static SampleMarkdownBuilder sampleMarkdown() {
        return new SampleMarkdownBuilder();
    }

    public static SampleImageMsgBuilder sampleImageMsg() {
        return new SampleImageMsgBuilder();
    }

    public static SampleLinkBuilder sampleLink() {
        return new SampleLinkBuilder();
    }

    public static SampleActionCardBuilder sampleActionCard() {
        return new SampleActionCardBuilder();
    }

    public static SampleActionCard2Builder sampleActionCard2() {
        return new SampleActionCard2Builder(MessageType.sampleActionCard2);
    }

    public static SampleActionCard3Builder sampleActionCard3() {
        return new SampleActionCard3Builder();
    }

    public static SampleActionCard4Builder sampleActionCard4() {
        return new SampleActionCard4Builder();
    }

    public static SampleActionCard5Builder sampleActionCard5() {
        return new SampleActionCard5Builder();
    }

    public static SampleActionCard6Builder sampleActionCard6() {
        return new SampleActionCard6Builder();
    }

    public static SampleAudioBuilder sampleAudio() {
        return new SampleAudioBuilder();
    }

    public static SampleFileBuilder sampleFile() {
        return new SampleFileBuilder();
    }

    public static SampleVideoBuilder sampleVideo() {
        return new SampleVideoBuilder();
    }

    public static class SampleVideoBuilder extends BaseBuilder {
        private SampleVideoBuilder() {
            super(MessageType.sampleVideo);
        }

        public SampleVideoBuilder duration(String duration) {
            this.content.put("duration", duration);
            return this;
        }

        public SampleVideoBuilder videoMediaId(String videoMediaId) {
            this.content.put("videoMediaId", videoMediaId);
            return this;
        }

        public SampleVideoBuilder videoType(String videoType) {
            this.content.put("videoType", videoType);
            return this;
        }

        public SampleVideoBuilder picMediaId(String picMediaId) {
            this.content.put("picMediaId", picMediaId);
            return this;
        }
    }


    public static class SampleFileBuilder extends BaseBuilder {
        private SampleFileBuilder() {
            super(MessageType.sampleFile);
        }

        public SampleFileBuilder mediaId(String mediaId) {
            this.content.put("mediaId", mediaId);
            return this;
        }

        public SampleFileBuilder fileName(String fileName) {
            this.content.put("fileName", fileName);
            return this;
        }

        public SampleFileBuilder fileType(String fileType) {
            this.content.put("fileType", fileType);
            return this;
        }
    }


    public static class SampleAudioBuilder extends BaseBuilder {
        private SampleAudioBuilder() {
            super(MessageType.sampleAudio);
        }

        public SampleAudioBuilder mediaId(String mediaId) {
            this.content.put("mediaId", mediaId);
            return this;
        }

        public SampleAudioBuilder duration(String duration) {
            this.content.put("duration", duration);
            return this;
        }


    }

    public static class SampleActionCard6Builder extends BaseBuilder {
        private SampleActionCard6Builder() {
            super(MessageType.sampleActionCard6);
        }

        public SampleActionCard6Builder title(String title) {
            this.content.put("title", title);
            return this;
        }

        public SampleActionCard6Builder text(String text) {
            this.content.put("text", text);
            return this;
        }

        public SampleActionCard6Builder buttonTitle1(String buttonTitle1) {
            this.content.put("buttonTitle1", buttonTitle1);
            return this;
        }

        public SampleActionCard6Builder buttonTitle2(String buttonTitle2) {
            this.content.put("buttonTitle2", buttonTitle2);
            return this;
        }

        public SampleActionCard6Builder buttonUrl1(String buttonUrl1) {
            this.content.put("buttonUrl1", buttonUrl1);
            return this;
        }

        public SampleActionCard6Builder buttonUrl2(String buttonUrl2) {
            this.content.put("buttonUrl2", buttonUrl2);
            return this;
        }

    }


    public static class SampleActionCard5Builder extends SampleActionCard2Builder<SampleActionCard5Builder> {
        private SampleActionCard5Builder() {
            super(MessageType.sampleActionCard5);
        }

        public SampleActionCard5Builder actionTitle3(String actionTitle3) {
            this.content.put("actionTitle3", actionTitle3);
            return this;
        }

        public SampleActionCard5Builder actionURL3(String actionURL3) {
            this.content.put("actionURL3", actionURL3);
            return this;
        }

        public SampleActionCard5Builder actionTitle4(String actionTitle4) {
            this.content.put("actionTitle4", actionTitle4);
            return this;
        }

        public SampleActionCard5Builder actionURL4(String actionURL4) {
            this.content.put("actionURL4", actionURL4);
            return this;
        }

        public SampleActionCard5Builder actionTitle5(String actionTitle5) {
            this.content.put("actionTitle5", actionTitle5);
            return this;
        }

        public SampleActionCard5Builder actionURL5(String actionURL5) {
            this.content.put("actionURL5", actionURL5);
            return this;
        }

    }

    public static class SampleActionCard4Builder extends SampleActionCard2Builder<SampleActionCard4Builder> {

        private SampleActionCard4Builder() {
            super(MessageType.sampleActionCard4);
        }

        public SampleActionCard4Builder actionTitle3(String actionTitle3) {
            this.content.put("actionTitle3", actionTitle3);
            return this;
        }

        public SampleActionCard4Builder actionURL3(String actionURL3) {
            this.content.put("actionURL3", actionURL3);
            return this;
        }

        public SampleActionCard4Builder actionTitle4(String actionTitle4) {
            this.content.put("actionTitle4", actionTitle4);
            return this;
        }

        public SampleActionCard4Builder actionURL4(String actionURL4) {
            this.content.put("actionURL4", actionURL4);
            return this;
        }

    }

    public static class SampleActionCard3Builder extends SampleActionCard2Builder<SampleActionCard3Builder> {

        private SampleActionCard3Builder() {
            super(MessageType.sampleActionCard3);
        }


        public SampleActionCard3Builder actionTitle3(String actionTitle3) {
            this.content.put("actionTitle3", actionTitle3);
            return this;
        }

        public SampleActionCard3Builder actionURL3(String actionURL3) {
            this.content.put("actionURL3", actionURL3);
            return this;
        }

    }

    public static class SampleActionCard2Builder<T extends SampleActionCard2Builder<T>> extends BaseBuilder {


        private SampleActionCard2Builder(MessageType type) {
            super(type);
        }

        public T title(String title) {
            this.content.put("title", title);
            return (T) this;
        }

        public T text(String text) {
            this.content.put("text", text);
            return (T) this;
        }

        public T actionTitle1(String actionTitle1) {
            this.content.put("actionTitle1", actionTitle1);
            return (T) this;
        }

        public T actionURL1(String actionURL1) {
            this.content.put("actionURL1", actionURL1);
            return (T) this;
        }

        public T actionTitle2(String actionTitle2) {
            this.content.put("actionTitle2", actionTitle2);
            return (T) this;
        }

        public T actionURL2(String actionURL2) {
            this.content.put("actionURL2", actionURL2);
            return (T) this;
        }

    }

    public static class SampleActionCardBuilder extends BaseBuilder {

        private SampleActionCardBuilder() {
            super(MessageType.sampleActionCard);
        }

        public SampleActionCardBuilder title(String title) {
            this.content.put("title", title);
            return this;
        }

        public SampleActionCardBuilder text(String text) {
            this.content.put("text", text);
            return this;
        }

        public SampleActionCardBuilder singleTitle(String singleTitle) {
            this.content.put("singleTitle", singleTitle);
            return this;
        }

        public SampleActionCardBuilder singleURL(String singleURL) {
            this.content.put("singleURL", singleURL);
            return this;
        }

    }


    public static class SampleLinkBuilder extends BaseBuilder {

        private SampleLinkBuilder() {
            super(MessageType.sampleLink);
        }

        public SampleLinkBuilder title(String title) {
            this.content.put("title", title);
            return this;
        }

        public SampleLinkBuilder text(String text) {
            this.content.put("text", text);
            return this;
        }

        public SampleLinkBuilder picUrl(String picUrl) {
            this.content.put("picUrl", picUrl);
            return this;
        }

        public SampleLinkBuilder messageUrl(String messageUrl) {
            this.content.put("messageUrl", messageUrl);
            return this;
        }

    }


    public static class SampleImageMsgBuilder extends BaseBuilder {

        private SampleImageMsgBuilder() {
            super(MessageType.sampleImageMsg);
        }

        public SampleImageMsgBuilder photoURL(String photoURL) {
            this.content.put("photoURL", photoURL);
            return this;
        }

    }

    public static class SampleMarkdownBuilder extends BaseBuilder {

        private SampleMarkdownBuilder() {
            super(MessageType.sampleMarkdown);
        }

        public SampleMarkdownBuilder title(String title) {
            this.content.put("title", title);
            return this;
        }

        public SampleMarkdownBuilder text(String text) {
            this.content.put("text", text);
            return this;
        }

    }

    public static class SampleTextBuilder extends BaseBuilder {

        private SampleTextBuilder() {
            super(MessageType.sampleText);
        }

        public SampleTextBuilder content(String content) {
            this.content.put("content", content);
            return this;
        }
    }


    public abstract static class BaseBuilder {
        private static final Gson GSON = new Gson();

        protected final Map<String, Object> content = new LinkedHashMap<>();
        private MessageType type;

        private BaseBuilder(MessageType type) {
            this.type = type;
        }

        public DingTalkMessage build() {
            return new DingTalkMessage(type, GSON.toJson(content));

        }

    }

    enum MessageType {
        sampleText, sampleMarkdown, sampleImageMsg, sampleLink,
        sampleActionCard, sampleActionCard2, sampleActionCard3,
        sampleActionCard4, sampleActionCard5, sampleActionCard6,
        sampleAudio, sampleFile, sampleVideo
    }

}
