package org.quyq.gwsu.log.serverfile.service;

import jakarta.servlet.http.HttpServletResponse;
import org.quyq.gwsu.log.api.dto.ServerLogContextDTO;
import org.quyq.gwsu.log.api.dto.ServerLogSearchDTO;
import org.quyq.gwsu.log.api.vo.ServerLogContextVO;
import org.quyq.gwsu.log.api.vo.ServerLogOptionsVO;
import org.quyq.gwsu.log.api.vo.ServerLogSearchVO;

import java.io.IOException;

public interface IServerLogService {

    ServerLogOptionsVO getOptions();

    ServerLogSearchVO search(ServerLogSearchDTO query);

    ServerLogContextVO getContext(ServerLogContextDTO query);

    void download(String targetId, String fileId, HttpServletResponse response) throws IOException;
}
