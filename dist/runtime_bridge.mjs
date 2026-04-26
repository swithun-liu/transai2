export function currentHostname() {
  return window.location.hostname;
}

export function configuredAiProxyEndpoint() {
  const config = window.TRANSAI_RUNTIME_CONFIG;
  return config && typeof config.aiProxyEndpoint === 'string'
    ? config.aiProxyEndpoint
    : '';
}
