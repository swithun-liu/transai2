config.devServer = config.devServer || {};
config.devServer.headers = {
  ...(config.devServer.headers || {}),
  "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate",
  "Pragma": "no-cache",
  "Expires": "0",
  "Surrogate-Control": "no-store",
};

config.cache = false;
