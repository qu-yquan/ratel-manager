package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceContext;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogEntry;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceContentVO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceReadDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRuntimeContextDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillProviderIdentityResolver;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillRegistryProperties;
import org.quyq.gwsu.common.ai.skill.dynamic.support.DynamicSkillRestClientFactory;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillDigestUtils;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillResourcePathValidator;

import org.quyq.gwsu.common.core.domain.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public final class DynamicSkillResourceService {

    private static final ParameterizedTypeReference<R<SkillResourceContentVO>> RESOURCE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final SkillProviderIdentity identity;
    private final LocalSkillResourceRegistry localRegistry;
    private final RestClient.Builder restClientBuilder;
    private final SkillRegistryProperties properties;
    private final BoundedSkillResourceCache cache;

    public DynamicSkillResourceService(
            SkillProviderIdentityResolver identityResolver,
            LocalSkillResourceRegistry localRegistry,
            RestClient.Builder restClientBuilder,
            SkillRegistryProperties properties) {
        this.identity = identityResolver.identity();
        this.localRegistry = localRegistry;
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.cache = new BoundedSkillResourceCache(
                properties.resourceCacheMaxEntries(), properties.resourceCacheTtl());
    }

    Optional<String> readText(
            SkillCatalogEntry entry,
            String path,
            SkillResourceContext context) {
        SkillResourceManifest resource = requiredManifest(entry, path, false);
        return load(entry, resource, context)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    Optional<byte[]> readBinary(
            SkillCatalogEntry entry,
            String path,
            SkillResourceContext context) {
        SkillResourceManifest resource = requiredManifest(entry, path, true);
        return load(entry, resource, context);
    }

    private Optional<byte[]> load(
            SkillCatalogEntry entry,
            SkillResourceManifest resource,
            SkillResourceContext context) {
        SkillRegistrationManifest manifest = entry.manifest();
        String cacheKey = "%s:%s:%s:%s:%s:%s".formatted(
                manifest.applicationName(), manifest.skillId(), manifest.version(),
                resource.path(), resource.digest(), contextKey(context));
        byte[] cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<byte[]> loaded = isLocal(manifest)
                ? loadLocal(manifest, resource, context)
                : loadRemote(manifest, resource, context);
        loaded.ifPresent(content -> {
            if (content.length > properties.resourceMaxBytes()) {
                throw new IllegalStateException("动态技能资源超过最大限制：" + resource.path());
            }
            verifyDigest(resource, content);
            cache.put(cacheKey, content);
        });
        return loaded;
    }

    private Optional<byte[]> loadLocal(
            SkillRegistrationManifest manifest,
            SkillResourceManifest resource,
            SkillResourceContext context) {
        SkillResourceProvider provider = localRegistry.requiredProvider(manifest.skillId());
        if (resource.binary()) {
            return provider.readBinary(resource.path(), context);
        }
        return provider.readText(resource.path(), context)
                .map(value -> value.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<byte[]> loadRemote(
            SkillRegistrationManifest manifest,
            SkillResourceManifest resource,
            SkillResourceContext context) {
        R<SkillResourceContentVO> response = DynamicSkillRestClientFactory.create(
                        restClientBuilder, manifest.applicationName(), properties)
                .post()
                .uri("/internal/ai/skills/resources/read")
                .body(new SkillResourceReadDTO(
                        manifest.skillId(), resource.path(),
                        SkillRuntimeContextDTO.from(
                                context == null ? null : context.runtimeContext())))
                .retrieve()
                .body(RESOURCE_TYPE);
        if (response == null || !response.isSuccess() || response.data() == null) {
            return Optional.empty();
        }
        SkillResourceContentVO value = response.data();
        if (!Objects.equals(resource.digest(), value.digest())) {
            throw new IllegalStateException("远程动态技能资源摘要与 Manifest 不一致：" + resource.path());
        }
        return Optional.of(value.binary()
                ? Base64.getDecoder().decode(value.content())
                : value.content().getBytes(StandardCharsets.UTF_8));
    }

    private SkillResourceManifest requiredManifest(
            SkillCatalogEntry entry,
            String path,
            boolean binary) {
        SkillResourcePathValidator.validate(path);
        SkillResourceManifest manifest = entry.manifest().resources().stream()
                .filter(value -> value.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("资源未在 Manifest 中注册：" + path));
        if (manifest.binary() != binary) {
            throw new IllegalArgumentException("资源读取类型不匹配：" + path);
        }
        return manifest;
    }

    private boolean isLocal(SkillRegistrationManifest manifest) {
        return Objects.equals(manifest.applicationName(), identity.applicationName());
    }

    private String contextKey(SkillResourceContext context) {
        if (context == null || context.runtimeContext() == null) {
            return "anonymous";
        }
        String userId = context.runtimeContext().getUserId();
        String sessionId = context.runtimeContext().getSessionId();
        return SkillDigestUtils.sha256(
                Objects.toString(userId, "") + ':' + Objects.toString(sessionId, ""));
    }

    private void verifyDigest(SkillResourceManifest resource, byte[] content) {
        try {
            String actual = "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content));
            if (!Objects.equals(resource.digest(), actual)) {
                throw new IllegalStateException("动态技能资源摘要校验失败：" + resource.path());
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JRE 不支持 SHA-256", ex);
        }
    }
}
