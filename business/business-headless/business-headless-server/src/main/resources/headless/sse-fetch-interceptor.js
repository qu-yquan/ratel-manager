(() => {
  const stateKey = '__GWSU_HEADLESS_SSE_INTERCEPTOR__';
  if (window[stateKey]) {
    return;
  }

  const targetPath = '/brain/run/copilotKit';
  const originalFetch = window.fetch;
  let streamSequence = 0;

  Object.defineProperty(window, stateKey, {
    configurable: false,
    enumerable: false,
    writable: false,
    value: true,
  });

  window.fetch = async function (input, init) {
    const operationId = stringValue(window.__GWSU_HEADLESS_SSE_OPERATION_ID__);
    const metadataPromise = readRequestMetadata(input, init, operationId);
    const response = await Reflect.apply(originalFetch, this, [input, init]);
    const metadata = await metadataPromise;

    if (!metadata.isAgentRun || !metadata.url.includes(targetPath)) {
      return response;
    }

    const streamId = [
      metadata.operationId,
      metadata.threadId,
      metadata.runId,
      Date.now(),
      ++streamSequence,
    ].join(':');

    if (!response.ok) {
      void emitSignal(
        streamId,
        'ERROR',
        metadata,
        response.status,
        `SSE 请求失败: HTTP ${response.status}`,
      );
      return response;
    }

    const contentType = response.headers.get('content-type') || '';
    if (!response.body || !contentType.toLowerCase().includes('text/event-stream')) {
      void emitSignal(
        streamId,
        'ERROR',
        metadata,
        response.status,
        `SSE 响应类型无效: ${contentType || 'unknown'}`,
      );
      return response;
    }

    let mirroredResponse;
    try {
      mirroredResponse = response.clone();
    } catch (error) {
      void emitSignal(
        streamId,
        'ERROR',
        metadata,
        response.status,
        normalizeError(error),
      );
      return response;
    }

    void consumeSseStream(streamId, mirroredResponse, metadata);
    return response;
  };

  async function readRequestMetadata(input, init, operationId) {
    const url = resolveUrl(input);
    let bodyText = '';

    try {
      if (init && typeof init.body === 'string') {
        bodyText = init.body;
      } else if (typeof Request !== 'undefined' && input instanceof Request) {
        bodyText = await input.clone().text();
      }
    } catch {
      return emptyMetadata(url, operationId);
    }

    try {
      const payload = JSON.parse(bodyText);
      const body = payload && typeof payload.body === 'object' ? payload.body : payload;
      return {
        url,
        operationId,
        isAgentRun: payload?.method === 'agent/run',
        threadId: stringValue(body?.threadId),
        runId: stringValue(body?.runId),
      };
    } catch {
      return emptyMetadata(url, operationId);
    }
  }

  function resolveUrl(input) {
    if (typeof input === 'string') {
      return input;
    }
    if (typeof URL !== 'undefined' && input instanceof URL) {
      return input.href;
    }
    return input?.url || '';
  }

  function emptyMetadata(url, operationId) {
    return { url, operationId, isAgentRun: false, threadId: '', runId: '' };
  }

  async function consumeSseStream(streamId, response, metadata) {
    const decoder = new TextDecoder('utf-8', { fatal: false });
    const reader = response.body.getReader();
    let buffer = '';

    try {
      await emitSignal(streamId, 'OPEN', metadata, response.status, '');

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }
        buffer += decoder.decode(value, { stream: true });
        buffer = await dispatchCompleteFrames(streamId, buffer);
      }

      buffer += decoder.decode();
      if (buffer.trim()) {
        await dispatchFrame(streamId, buffer);
      }
      await emitSignal(streamId, 'END', metadata, response.status, '');
    } catch (error) {
      await emitSignal(
        streamId,
        'ERROR',
        metadata,
        response.status,
        normalizeError(error),
      );
    } finally {
      reader.releaseLock();
    }
  }

  async function dispatchCompleteFrames(streamId, buffer) {
    const delimiter = /\r\n\r\n|\n\n|\r\r/;
    while (true) {
      const match = delimiter.exec(buffer);
      if (!match) {
        return buffer;
      }
      const frame = buffer.slice(0, match.index);
      buffer = buffer.slice(match.index + match[0].length);
      await dispatchFrame(streamId, frame);
    }
  }

  async function dispatchFrame(streamId, frame) {
    const dataLines = [];
    for (const line of frame.split(/\r\n|\n|\r/)) {
      if (!line || line.startsWith(':')) {
        continue;
      }
      const separator = line.indexOf(':');
      const field = separator < 0 ? line : line.slice(0, separator);
      if (field !== 'data') {
        continue;
      }
      let value = separator < 0 ? '' : line.slice(separator + 1);
      if (value.startsWith(' ')) {
        value = value.slice(1);
      }
      dataLines.push(value);
    }

    if (dataLines.length === 0) {
      return;
    }

    const eventJson = dataLines.join('\n');
    if (eventJson === '[DONE]') {
      return;
    }

    const callback = window.__gwsuHeadlessSseEvent;
    if (typeof callback !== 'function') {
      throw new Error('Playwright SSE event callback is unavailable');
    }
    await callback(streamId, eventJson);
  }

  async function emitSignal(streamId, type, metadata, status, message) {
    const callback = window.__gwsuHeadlessSseSignal;
    if (typeof callback !== 'function') {
      throw new Error('Playwright SSE lifecycle callback is unavailable');
    }
    await callback(
      streamId,
      type,
      metadata.threadId,
      metadata.runId,
      status,
      message,
    );
  }

  function normalizeError(error) {
    if (error instanceof Error) {
      return error.message;
    }
    return String(error || 'unknown SSE stream error');
  }

  function stringValue(value) {
    return value == null ? '' : String(value);
  }
})();
