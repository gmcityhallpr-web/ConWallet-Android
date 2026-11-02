"""Run the library's tts_zh.py (edge-tts) through this environment's HTTPS proxy.
edge-tts opens its own aiohttp session (no proxy from the environment) and trusts certifi's CA list, so here we pass
the proxy to every Communicate() and point certifi at the environment's CA bundle. Usage: same arguments as tts_zh.py."""
import os, runpy, sys

LIB = os.environ.get('LIB', '/root/lemo-opuscar')
proxy = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
ca = os.environ.get('SSL_CERT_FILE') or '/root/.ccr/ca-bundle.crt'

import certifi  # noqa: E402
if os.path.exists(ca):
    certifi.where = lambda: ca
import edge_tts  # noqa: E402
_Comm = edge_tts.Communicate
class Communicate(_Comm):
    def __init__(self, *a, **k):
        k.setdefault('proxy', proxy)
        super().__init__(*a, **k)
edge_tts.Communicate = Communicate

sys.argv = [os.path.join(LIB, 'core/tts/tts_zh.py')] + sys.argv[1:]
runpy.run_path(sys.argv[0], run_name='__main__')
