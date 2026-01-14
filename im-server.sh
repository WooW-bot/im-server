#!/bin/bash
# IM-Server 快捷启动脚本
# 这是一个便捷的包装脚本，调用 scripts/im-server.sh
exec "$(dirname "$0")/scripts/im-server.sh" "$@"
