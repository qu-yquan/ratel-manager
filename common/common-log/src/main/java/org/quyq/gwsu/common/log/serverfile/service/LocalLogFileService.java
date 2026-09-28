package org.quyq.gwsu.common.log.serverfile.service;

import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextResult;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchResult;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public interface LocalLogFileService {

    LocalLogSearchResult search(LocalLogSearchRequest request);

    LocalLogContextResult getContext(LocalLogContextRequest request);

    Resource getDownloadResource(String fileId);

    MediaType getMediaType(String fileId);
}
